package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayBuffer;
import io.github.dsheirer.dsp.symbol.QPSKCarrierLock;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Processes a stream of Dibit symbols and performs burst detection and timeslot framing.  This framer
 * also detects abnormal PLL phase locks and issues PLL phase lock corrections.
 */
public class DMRBurstFramer implements Listener<Dibit>, IDMRSyncDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRBurstFramer.class);

    private static final int DMR_SYMBOL_RATE = 4800;
    private static final double PLL_PHASE_CORRECTION_90_DEGREES = (double)DMR_SYMBOL_RATE / 4.0;
    private static final double PLL_PHASE_CORRECTION_180_DEGREES = (double)DMR_SYMBOL_RATE / 2.0;

    private static final int BURST_DIBIT_START = 0;
    private static final int BURST_DIBIT_LENGTH = 144;
    private static final int SYNC_DIBIT_OFFSET = 66;
    private static final int SYNC_DIBIT_LENGTH = 24;
    private static final int MAX_UNSYNCHRONIZED_SYNC_DETECT_BIT_ERRORS = 3;
    private static final int MAX_SYNCHRONIZED_SYNC_DETECT_BIT_ERRORS = 6;

    /**
     * Threshold for issuing a sync loss message.  This is set to trigger once the dibit count exceeds one second of
     * dibits (4800 baud/dibits) plus one burst dibit length.  The one burst length padding is so that we don't reset
     * the dibit counter below the burst sync check threshold.
     */
    private static final int SYNC_LOSS_MESSAGE_THRESHOLD = DMR_SYMBOL_RATE + BURST_DIBIT_LENGTH;

    /**
     * The message buffer is sized to hold 144 dibits (288 bits) that include 12 dibits of CACH and 132 dibits of burst
     */
    private DibitDelayBuffer mMessageBuffer = new DibitDelayBuffer(BURST_DIBIT_LENGTH);

    /**
     * The sync delay buffer is sized to align sync detection with a burst being fully loaded into the message buffer.
     * This delay equals the number of dibits in the second half of the burst payload: 54 dibits (108 bits).
     */
    private DibitDelayBuffer mSyncDelayBuffer = new DibitDelayBuffer(54);

    /**
     * Synchronized indicates that either the primary or secondary sync tracker is currently synchronized to
     * one of the timeslot bursts.  Once either sync tracker obtains a valid sync pattern, this flag is set
     * to true.  The synchronized state will remain true as long as at least one of the two sync trackers
     * maintains sync state.  Once both sync trackers lose sync, the framer will fallback into sync search/detection
     * mode to inspect each dibit until a sync match can be (re)discovered.
     */
    private boolean mSynchronized;

    /**
     * Tracks the number of dibits received to trigger burst or sync loss message processing
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

    private DMRSyncDetector mSyncDetector;
    private IDMRBurstDetectListener mBurstDetectListener;
    private IPhaseLockedLoop mPhaseLockedLoop;

    /**
     * Constructs an instance
     * @param listener to be notified of framed burst detections and/or sync loss bits processed
     * @param phaseLockedLoop to receive symbol alignment corrections
     */
    public DMRBurstFramer(IDMRBurstDetectListener listener, IPhaseLockedLoop phaseLockedLoop)
    {
        mBurstDetectListener = listener;
        mPhaseLockedLoop = phaseLockedLoop;
        mSyncDetector = new DMRSyncDetector(this, MAX_UNSYNCHRONIZED_SYNC_DETECT_BIT_ERRORS);
    }

    /**
     * Primary dibit symbol input method
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        mDibitCounter++;

        //Feed the message buffer first to ensure buffer contains the full message when a sync is detected
        mMessageBuffer.put(dibit);

        //Feed the sync delay buffer and reassign the delayed dibit to feed the sync detector if we're not synchronized
        dibit = mSyncDelayBuffer.getAndPut(dibit);

        if(mSynchronized)
        {
            if(mDibitCounter >= BURST_DIBIT_LENGTH)
            {
                if(mCurrentTracker.hasSync())
                {
                    dispatchMessage(mCurrentTracker.getSyncPattern(), mCurrentTracker.getBitErrorCount());
                }
                else
                {
                    if(mSynchronized)
                    {
                        //Even though this timeslot has lost sync, the other timeslot still reflects sync, so we
                        //dispatch a dummy message with sync pattern = UNKNOWN to ensure the receiver is able to
                        //track timeslot state.
                        dispatchMessage(mCurrentTracker.getSyncPattern(), mCurrentTracker.getBitErrorCount());
                    }
                    else
                    {
                        //We were synchronized but now we're not.  Update the sync detector with the current message
                        //sync field contents so that we can restart sync detection from this point
                        mSyncDetector.setCurrentSyncValue(getSyncFieldValue());
                    }
                }

                toggleSyncTracker();
            }
        }
        else
        {
            mSyncDetector.receive(dibit);

            //Issue a sync loss for each full second of dibits that we process
            if(mDibitCounter > SYNC_LOSS_MESSAGE_THRESHOLD)
            {
                processSyncLossDibits(DMR_SYMBOL_RATE);
            }
        }
    }

    /**
     * Processes a sync pattern detection and dispatches the current contents of the message/burst buffer.
     * @param pattern of sync that was detected
     * @param carrierLock indicates if the PLL locked to the signal correctly, or if there are phase mis-alignment
     * detected that needs to be corrected.
     * @param bitErrors count for soft sync matching to indicate the number of bit positions in the sync field of the
     * message that differ from the actual sync pattern.
     */
    @Override
    public void syncDetected(DMRSyncPattern pattern, QPSKCarrierLock carrierLock, int bitErrors)
    {
        mCurrentTracker.setSyncPattern(pattern, bitErrors);

        if(carrierLock == QPSKCarrierLock.NORMAL)
        {
//            mLog.info("*** Sync Detected: " + pattern);
            dispatchMessage(pattern, bitErrors);
        }
        else
        {
//            mLog.error("*** PLL Lock Misalign Detected - Correcting: " + carrierLock);

            if(mPhaseLockedLoop != null)
            {
                switch(carrierLock)
                {
                    case PLUS_90:
                        mPhaseLockedLoop.correctInversion(-PLL_PHASE_CORRECTION_90_DEGREES);
                        break;
                    case MINUS_90:
                        mPhaseLockedLoop.correctInversion(PLL_PHASE_CORRECTION_90_DEGREES);
                        break;
                    case INVERTED:
                        mPhaseLockedLoop.correctInversion(PLL_PHASE_CORRECTION_180_DEGREES);
                        break;
                }
            }

            //Correct the message buffer dibits and dispatch the message
            dispatchMessage(pattern, bitErrors, carrierLock);
        }
    }

    /**
     * Toggles sync tracker between primary and secondary.  Sync trackers are timeslot agnostic and can monitor either
     * timeslot 0 or 1.
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

    /**
     * Resets the message and sync delay buffers and resets the dibit counter to zero.
     */
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
            value = Long.rotateLeft(value, 2);
            value += dibit.getValue();
        }

        return value;
    }

    /**
     * Dispatches the contents of the message buffer to the registered burst detection listener
     * @param syncPattern that was detected for the message
     * @param bitErrors count of bit errors between the detected sync pattern and the actual sync field
     */
    private void dispatchMessage(DMRSyncPattern syncPattern, int bitErrors)
    {
        if(mDibitCounter > BURST_DIBIT_LENGTH)
        {
            processSyncLossDibits(mDibitCounter - BURST_DIBIT_LENGTH);
        }

        CorrectedBinaryMessage message = mMessageBuffer.getMessage(BURST_DIBIT_START, BURST_DIBIT_LENGTH);
        message.incrementCorrectedBitCount(bitErrors);
        mDibitCounter = 0;

        if(mBurstDetectListener != null)
        {
            mBurstDetectListener.burstDetected(message, syncPattern);
        }
    }

    /**
     * Dispatches the contents of the message buffer applying corrections when a carrier lock was detected to be
     * misaligned.
     *
     * @param syncPattern that was detected
     * @param bitErrors when matching the sync pattern
     * @param carrierLock that indicates how the PLL was detected to be locked to the signal along with any phase
     * alignment issues.
     */
    private void dispatchMessage(DMRSyncPattern syncPattern, int bitErrors, QPSKCarrierLock carrierLock)
    {
        switch(carrierLock)
        {
            //Apply correction to the dibits in the message buffer if the PLL locked abnormally
            case PLUS_90:
            case MINUS_90:
            case INVERTED:
                for(int x = 0; x < mMessageBuffer.length(); x++)
                {
                    Dibit misalignedDibit = mMessageBuffer.get(x);
                    mMessageBuffer.set(x, carrierLock.correct(misalignedDibit));
                }
                break;
        }

        dispatchMessage(syncPattern, bitErrors);
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
    protected void updateSynchronizedState()
    {
        mSynchronized = mPrimaryTracker.isSynchronized() | mSecondaryTracker.isSynchronized();
    }

    /**
     * Tracks the sync state for either timeslot to account for voice framing that does not transmit a
     * sync pattern in each frame.
     */
    public class SyncTracker
    {
        private DMRSyncPattern mSyncPattern = DMRSyncPattern.UNKNOWN;
        private int mBitErrorCount;

        /**
         * Sets the sync pattern for the most recently detected burst to the specified pattern and commands the burst
         * framer to update the overall framer synchronization state
         */
        private void setSyncPattern(DMRSyncPattern pattern, int bitErrors)
        {
            mSyncPattern = pattern;
            mBitErrorCount = bitErrors;
            updateSynchronizedState();
        }

        /**
         * Most recently detected sync pattern
         */
        private DMRSyncPattern getSyncPattern()
        {
            return mSyncPattern;
        }

        /**
         * Bit error count for the most recently detected sync pattern
         */
        private int getBitErrorCount()
        {
            return mBitErrorCount;
        }

        /**
         * Checks the contents of the message buffer to determine if the message has a valid sync pattern, or if the
         * message is a continuation voice frame from a voice super frame.  The identified sync pattern and number of
         * bit errors for that pattern are stored in the sync detector so that they can be accessed for dispatching
         * a message.
         */
        public boolean hasSync()
        {
            DMRSyncPattern previousSyncPattern = getSyncPattern();

            //Determine if there is a valid sync pattern in the current message buffer.
            long sync = getSyncFieldValue();

            for(DMRSyncPattern syncPattern: DMRSyncPattern.SYNC_PATTERNS)
            {
                long errorPattern = sync ^ syncPattern.getPattern();

                if(errorPattern == 0)
                {
                    setSyncPattern(syncPattern, 0);
                    return true;
                }
                else
                {
                    int bitErrors = Long.bitCount(errorPattern);

                    if(bitErrors <= MAX_SYNCHRONIZED_SYNC_DETECT_BIT_ERRORS)
                    {
                        setSyncPattern(syncPattern, bitErrors);
                        return true;
                    }
                }
            }

            //Check to see if we're currently in a voice super-frame and auto-advance the sync pattern accordingly.
            switch(previousSyncPattern)
            {
                case BASE_STATION_VOICE:
                    setSyncPattern(DMRSyncPattern.BS_VOICE_FRAME_B, 0);
                    return true;
                case BS_VOICE_FRAME_B:
                    setSyncPattern(DMRSyncPattern.BS_VOICE_FRAME_C, 0);
                    return true;
                case BS_VOICE_FRAME_C:
                    setSyncPattern(DMRSyncPattern.BS_VOICE_FRAME_D, 0);
                    return true;
                case BS_VOICE_FRAME_D:
                    setSyncPattern(DMRSyncPattern.BS_VOICE_FRAME_E, 0);
                    return true;
                case BS_VOICE_FRAME_E:
                    setSyncPattern(DMRSyncPattern.BS_VOICE_FRAME_F, 0);
                    return true;

                case MOBILE_STATION_VOICE:
                case DIRECT_MODE_VOICE_TIMESLOT_0:
                case DIRECT_MODE_VOICE_TIMESLOT_1:
                    setSyncPattern(DMRSyncPattern.MS_VOICE_FRAME_B, 0);
                    return true;
                case MS_VOICE_FRAME_B:
                    setSyncPattern(DMRSyncPattern.MS_VOICE_FRAME_C, 0);
                    return true;
                case MS_VOICE_FRAME_C:
                    setSyncPattern(DMRSyncPattern.MS_VOICE_FRAME_D, 0);
                    return true;
                case MS_VOICE_FRAME_D:
                    setSyncPattern(DMRSyncPattern.MS_VOICE_FRAME_E, 0);
                    return true;
                case MS_VOICE_FRAME_E:
                    setSyncPattern(DMRSyncPattern.MS_VOICE_FRAME_F, 0);
                    return true;
            }

            //We've lost sync on this timeslot
            setSyncPattern(DMRSyncPattern.UNKNOWN, 0);
            return false;
        }

        /**
         * Indicates if this tracker is currently synchronized, meaning that it detected a valid sync pattern
         * in the most recent burst when hasSync() was invoked.
         */
        public boolean isSynchronized()
        {
            return mSyncPattern != DMRSyncPattern.UNKNOWN;
        }
    }

    public static void main(String[] args)
    {
        System.out.println("Starting ...");
        DMRBurstFramer framer = new DMRBurstFramer(new IDMRBurstDetectListener()
        {
            @Override
            public void burstDetected(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern)
            {
//                System.out.println("Burst: " + pattern);
            }

            @Override
            public void syncLost(int bitsProcessed)
            {
                System.out.println("Lost: " + bitsProcessed);
            }
        }, null);

        Path directory = Path.of("/home/denny/SDRTrunk/recordings");
        Path path = directory.resolve("20200628_081304_9600BPS_DMR_SaiaNet_Onondaga_LCN_1.bits");

        try(BinaryReader reader = new BinaryReader(path, 200))
        {
            while(reader.hasNext())
            {
                ReusableByteBuffer buffer = reader.next();

                for(byte b: buffer.getBytes())
                {
                    for(int x = 0; x <= 3; x++)
                    {
                        framer.receive(Dibit.parse(b, x));
                    }
                }

                buffer.decrementUserCount();
            }
        }
        catch(Exception ioe)
        {
            ioe.printStackTrace();
        }

        System.out.println("Finished!");
    }
}
