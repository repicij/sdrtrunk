package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.Listener;

/**
 * Detector for DMR sync patterns that also includes built-in support for detecting PLL phase lock errors.
 */
public class DMRSyncDetectorNew implements Listener<Dibit>
{
    private static long SYNC_MASK = 0xFFFFFFFFFFFFl;

    private int mMaxBitErrors;
    private long mCurrentSyncValue;
    private long mErrorPattern;
    private int mPatternMatchBitErrorCount;

    /**
     * Constructs an instance
     * @param maxBitErrors allowed for matching any of the sync patterns
     */
    public DMRSyncDetectorNew(int maxBitErrors)
    {
        mMaxBitErrors = maxBitErrors;
    }

    @Override
    public void receive(Dibit dibit)
    {
        Long.rotateLeft(mCurrentSyncValue, 2);
        mCurrentSyncValue &= SYNC_MASK;
        mCurrentSyncValue += dibit.getValue();
        checkSync();
    }

    /**
     * Checks the current sync value against each of the sync patterns to determine if the value matches a pattern.
     */
    private void checkSync()
    {
        for(DMRSyncPattern pattern: DMRSyncPattern.SYNC_PATTERNS)
        {
            mErrorPattern = mCurrentSyncValue ^ pattern.getPattern();

            if(mErrorPattern == 0)
            {
                broadastSyncDetected(pattern, 0);
                return;
            }

            mPatternMatchBitErrorCount = Long.bitCount(mErrorPattern);

            if(mPatternMatchBitErrorCount <= mMaxBitErrors)
            {
                broadastSyncDetected(pattern, mPatternMatchBitErrorCount);
                return;
            }
        }

        //TODO: iterate each of the phase misalignment sync patterns with more stringent max allowable bit errors

    }

    private void broadastSyncDetected(DMRSyncPattern pattern, int bitErrorCount)
    {

    }

    /**
     * Sets the current sync value to the argument value.
     * @param value to load as the current sync value
     */
    public void setCurrentSyncValue(long value)
    {
        mCurrentSyncValue = value;
    }
}
