package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayBuffer;
import io.github.dsheirer.sample.Listener;

/**
 * Processes a stream of Dibit symbols and performs burst detection and timeslot framing.  This framer
 * also detects abnormal PLL phase locks and issues PLL phase lock corrections.
 */
public class DMRBurstFramer implements Listener<Dibit>
{
    private static final int BURST_DIBIT_START = 0;
    private static final int BURST_LENGTH = 144;
    private static final int SYNC_DIBIT_OFFSET = 66;
    private static final int SYNC_DIBIT_LENGTH = 24;
    private static final int MAX_SYNC_BIT_ERRORS = 4;

    /**
     * The message buffer is sized to hold 144 dibits (288 bits) that include 12 dibits of CACH and
     * 132 dibits of the burst frame
     */
    private DibitDelayBuffer mMessageBuffer = new DibitDelayBuffer(144);

    /**
     * The sync delay buffer is sized to align sync detection with a burst being fully loaded into the
     * message buffer.  This delay if the number of dibits contained in the second half of the burst
     * payload which is 54 dibits (108 bits).
     */
    private DibitDelayBuffer mSyncDelayBuffer = new DibitDelayBuffer(54);

    /**
     * Synchronized indicates that either the primary or secondary sync tracker is currently synchronized to
     * one of the timeslot bursts.  Once either sync tracker obtains a valid sync pattern, this flag is set
     * to true.  The synchronized state will remain true as long as at least one of the two sync trackers
     * maintains sync state.  Once both sync trackers lose sync, the framer will fallback into sync search
     * mode to inspect each dibit until a sync match can be discovered.
     */
    private boolean mSynchronized;

    /**
     * Tracks the number of dibits received to trigger burst message processing
     */
    private int mDibitCounter = 0;

    /**
     * Timeslot sync trackers track the synchronization state for each of the two timeslots.  These are named primary
     * and secondary for naming purposes only. Each tracker can track either timeslot 0 or timeslot 1.  It doesn't
     * matter which timeslot each tracker locks to because the framer will toggle between the two with each burst frame
     * that is processed.
     */
    private SyncTracker mPrimaryTracker = new SyncTracker();
    private SyncTracker mSecondaryTracker = new SyncTracker();
    private SyncTracker mCurrentTracker = mPrimaryTracker;

    private IDMRBurstDetectListener mBurstDetectListener;

    /**
     * Constructs an instance
     * @param listener to be notified of framed burst detections and/or sync loss bits processed
     */
    public DMRBurstFramer(IDMRBurstDetectListener listener)
    {
        mBurstDetectListener = listener;
    }

    @Override
    public void receive(Dibit dibit)
    {
        mDibitCounter++;

        //Feed the message buffer first to ensure buffer contains the full message when a sync is detected
        mMessageBuffer.put(dibit);

        dibit = mSyncDelayBuffer.getAndPut(dibit);

        if(mSynchronized)
        {
            if(mDibitCounter >= BURST_LENGTH)
            {
                if(!mCurrentTracker.hasSync())
                {
                    //TODO: load sync value into sync detector and run as un-synchronized
                };
            }
        }
        else
        {

            //TODO: feed delayed dibit to sync detector
            //TODO: if an automatic sync is not dete

        }

    }

    /**
     * Toggles sync tracker between primary and secondary
     */
    private void toggleSyncTracker()
    {
        if(mCurrentTracker == mPrimaryTracker)
        {
            mCurrentTracker = mSecondaryTracker;
        }
        else
        {
            mCurrentTracker = mPrimaryTracker;
        }
    }

    public void reset()
    {
        mDibitCounter = 0;
        mMessageBuffer.reset();
        mSyncDelayBuffer.reset();
    }

    /**
     * Calculates the value of the sync field currently in the message buffer.
     */
    protected long getSyncFieldValue()
    {
        Dibit[] syncDibits = mMessageBuffer.getBuffer(SYNC_DIBIT_OFFSET, SYNC_DIBIT_LENGTH);

        long value = 0;

        for(Dibit dibit: syncDibits)
        {
            if(dibit.getBit1())
            {
                value++;
            }

            Long.rotateLeft(value, 1);

            if(dibit.getBit2())
            {
                value++;
            }

            Long.rotateLeft(value, 1);
        }

        return value;
    }

    /**
     * Dispatches the contents of the message buffer to the registered burst detection listener
     * @param syncPattern that was detected for the message
     * @param bitErrors count of bit errors between the detected sync pattern and the actual sync field
     */
    protected void dispatchMessage(DMRSyncPattern syncPattern, int bitErrors)
    {
        if(mDibitCounter > BURST_LENGTH)
        {
            processSyncLossDibits(mDibitCounter - BURST_LENGTH);
        }

        CorrectedBinaryMessage message = mMessageBuffer.getMessage(BURST_DIBIT_START, BURST_LENGTH);
        message.incrementCorrectedBitCount(bitErrors);
        mDibitCounter -= BURST_LENGTH;

        if(mBurstDetectListener != null)
        {
            mBurstDetectListener.burstDetectedWithSync(message, syncPattern);
        }

        toggleSyncTracker();
    }

    /**
     * Processes dibits received without a sync condition
     * @param dibitCount with sync loss
     */
    private void processSyncLossDibits(int dibitCount)
    {
        mDibitCounter -= dibitCount;

        if(mBurstDetectListener != null)
        {
            mBurstDetectListener.syncLost(dibitCount * 2);
        }
    }

    /**
     * Updates the synchronized state for the framer.  The framer will indicate synchronized if either the
     * primary or secondary sync track reflects a synchronized state.  Once both trackers lose sync, the
     * framer will update to a non-synchronized state and revert to using a the sync pattern matcher to regain
     * sync state.
     */
    protected void updateSyncronizedState()
    {
        mSynchronized = mPrimaryTracker.isSynchronized() | mSecondaryTracker.isSynchronized();
    }

    /**
     * Tracks the sync state for a timeslot to account for voice framing that does not transmit a
     * sync pattern in each frame.
     */
    public class SyncTracker
    {
        private DMRSyncPattern mSyncPattern = DMRSyncPattern.UNKNOWN;

        /**
         * Sets the sync pattern for the most recently detected burst to the specified pattern and commands the burst
         * framer to update the overall framer synchronization state
         */
        private void setSyncPattern(DMRSyncPattern pattern)
        {
            mSyncPattern = pattern;
            updateSyncronizedState();
        }

        /**
         * Most recently detected sync pattern
         */
        private DMRSyncPattern getSyncPattern()
        {
            return mSyncPattern;
        }

        /**
         * Verifies that the message buffer contains a message and a valid sync pattern.  When invoked,
         * this method will verify that the message buffer sync is valid and either process as a valid
         * message and continue with the synchronized state, or it will dispatch a sync loss and set the
         * framer to unsynchronized.
         */
        public boolean hasSync()
        {
            //TODO: rework this to:
            //1. Check for a sync value.
            //2. If UNKNOWN, see if previous sync was voice and if so, auto-advance to next voice sync
            //3. If KNOWN, then broadcast as known.
            //4. There's a possibility that a voice superframe will terminate prematurely and that would
            //cause us to mis-classify a data burst as a voice burst if we don't check first.
            if(isSynchronized())
            {
                switch(getSyncPattern())
                {
                    case BASE_STATION_VOICE:
                    case MOBILE_STATION_VOICE:
                    case DIRECT_MODE_VOICE_TIMESLOT_0:
                    case DIRECT_MODE_VOICE_TIMESLOT_1:
                        setSyncPattern(DMRSyncPattern.VOICE_FRAME_B);
                        dispatchMessage(getSyncPattern(), 0);
                        return true;
                    case VOICE_FRAME_B:
                        setSyncPattern(DMRSyncPattern.VOICE_FRAME_C);
                        dispatchMessage(getSyncPattern(), 0);
                        return true;
                    case VOICE_FRAME_C:
                        setSyncPattern(DMRSyncPattern.VOICE_FRAME_D);
                        dispatchMessage(getSyncPattern(), 0);
                        return true;
                    case VOICE_FRAME_D:
                        setSyncPattern(DMRSyncPattern.VOICE_FRAME_E);
                        dispatchMessage(getSyncPattern(), 0);
                        return true;
                    case VOICE_FRAME_E:
                        setSyncPattern(DMRSyncPattern.VOICE_FRAME_F);
                        dispatchMessage(getSyncPattern(), 0);
                        return true;
                }
            }
            else
            {
                //TODO: if we're not synchronized, we need to try every possible combination of sync
                //pattern to find the sync and/or correct PLL lock errors
            }

            long sync = getSyncFieldValue();

            for(DMRSyncPattern syncPattern: DMRSyncPattern.SYNC_PATTERNS)
            {
                long errorPattern = sync ^ syncPattern.getPattern();

                if(errorPattern == 0)
                {
                    dispatchMessage(syncPattern, 0);
                    setSyncPattern(syncPattern);
                    return true;
                }
                else
                {
                    int bitErrors = Long.bitCount(errorPattern);

                    if(bitErrors <= MAX_SYNC_BIT_ERRORS)
                    {
                        dispatchMessage(syncPattern, bitErrors);
                        mSyncPattern = syncPattern;
                        updateSyncronizedState();
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Indicates if this tracker is currently synchronized, meaning that it detected a valid sync pattern
         * in the most recent burst or it is currently processing a voice frame sequence.
         * @return
         */
        public boolean isSynchronized()
        {
            return mSyncPattern != DMRSyncPattern.UNKNOWN;
        }
    }
}
