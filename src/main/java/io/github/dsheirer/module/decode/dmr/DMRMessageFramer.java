/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.UnknownCSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusUnknown41;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import io.github.dsheirer.module.decode.dmr.message.data.header.DataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ShortLCMessage;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * DMR Sync Detector and Message Framer.  Includes capability to detect PLL out-of-phase lock errors
 * and issue phase corrections.
 */
public class DMRMessageFramer implements Listener<Dibit>, IDMRBurstDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFramer.class);

    private static final double DMR_BIT_RATE = 9600.0;

    /**
     * Provides sync detection and burst framing
     */
    private DMRBurstFramer mBurstFramer;

    /**
     * External listener for messages produced by this framer
     */
    private Listener<IMessage> mMessageListener;

    /**
     * External listener for sync state information
     */
    private ISyncDetectListener mSyncDetectListener;  //TODO: move this into the DMRBurstFramer

    /**
     * Tracks current time for use with framed bursts/messages.  This value is updated externally from sample buffer
     * timestamps and internally as each framed message is received or sync loss is processed.
     */
    private long mCurrentTime = System.currentTimeMillis();

    /**
     * Tracks timeslots (0 or 1) for incoming bursts/messages
     */
    private boolean mIsTimeslot0 = true;

    /**
     * Constructs an instance
     *
     * @param phaseLockedLoop to receive PLL phase lock error corrections that are identified by the DMRBurstFramer
     * when known phase misaligned sync patterns are detected.
     */
    public DMRMessageFramer(IPhaseLockedLoop phaseLockedLoop)
    {
        mBurstFramer = new DMRBurstFramer(this, phaseLockedLoop);
    }

    /**
     * Constructs an instance.
     */
    public DMRMessageFramer()
    {
        this(null);
    }

    /**
     * Registers a sync detect listener to be notified each time a sync pattern and NID are detected.
     */
    public void setSyncDetectListener(ISyncDetectListener syncDetectListener)
    {
        mSyncDetectListener = syncDetectListener;
    }

    /**
     * Current timestamp or timestamp of incoming message buffers that is continuously updated to as
     * close as possible to the bits processed for the expected baud rate.
     *
     * @return
     */
    private long getTimestamp()
    {
        return mCurrentTime;
    }

    /**
     * Sets the current time.  This should be invoked by an incoming message buffer stream.
     *
     * @param currentTime
     */
    public void setCurrentTime(long currentTime)
    {
        mCurrentTime = currentTime;
    }

    /**
     * Updates the current timestamp based on the number of bits processed versus the bit rate per second
     * in order to keep an accurate running timestamp to use for timestamped message creation.
     *
     * @param bitsProcessed thus far
     */
    private void updateBitsProcessed(int bitsProcessed)
    {
        if(bitsProcessed > 0)
        {
            mCurrentTime += (long)((double)bitsProcessed / DMR_BIT_RATE * 1000.0);
        }
    }

    /**
     * Registers the listener for messages produced by this message framer
     *
     * @param messageListener to receive framed and decoded messages
     */
    public void setListener(Listener<IMessage> messageListener)
    {
        mMessageListener = messageListener;
    }

    /**
     * Primary method for streaming decoded symbol dibits for message framing.
     *
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        mBurstFramer.receive(dibit);
    }

    /**
     * Primary method for streaming decoded symbol byte arrays.
     *
     * @param buffer to process into a stream of dibits for processing.
     */
    public void receive(ReusableByteBuffer buffer)
    {
        //Updates current timestamp to the timestamp from the incoming buffer
        setCurrentTime(buffer.getTimestamp());

        for(byte value : buffer.getBytes())
        {
            for(int x = 0; x <= 3; x++)
            {
                receive(Dibit.parse(value, x));
            }
        }

        buffer.decrementUserCount();
    }

    /**
     * DMR burst detection processing.  Processes DMR burst/messages from external burst framer/producer.
     *
     * @param message bits for the burst/message and any bit errors detected in the sync pattern
     * @param syncPattern that was detected for the burst
     */
    @Override
    public void burstDetected(CorrectedBinaryMessage message, DMRSyncPattern syncPattern)
    {
        CACH cach = null;

        if(syncPattern.hasCACH())
        {
            cach = CACH.getCACH(message);
        }

        int timeslot = getTimeslot(syncPattern, cach);

        if(mMessageListener != null)
        {
            DMRMessage dmrMessage = DMRMessageFactory.create(syncPattern, message, cach, getTimestamp(), timeslot);

            if(dmrMessage != null)
            {
                mMessageListener.receive(dmrMessage);
            }
        }
    }

    /**
     * Identifies the timeslot for the current incoming DMR burst/message.  Attempts to identify the timeslot from the
     * CACH when available (and valid), or using Direct Mode sync pattern.  Otherwise, simply toggles between primary
     * and secondary timeslots.
     *
     * @param pattern detected in DMR burst
     * @param cach to identify the timeslot (optional - null)
     * @return current timeslot
     */
    private int getTimeslot(DMRSyncPattern pattern, CACH cach)
    {
        if(cach != null && cach.isValid())
        {
            mIsTimeslot0 = cach.isTimeslot0();
            return cach.getTimeslot();
        }
        else
        {
            switch(pattern)
            {
                case DIRECT_MODE_VOICE_TIMESLOT_0:
                case DIRECT_MODE_DATA_TIMESLOT_0:
                    mIsTimeslot0 = true;
                    return 0;
                case DIRECT_MODE_DATA_TIMESLOT_1:
                case DIRECT_MODE_VOICE_TIMESLOT_1:
                    mIsTimeslot0 = false;
                    return 1;
                default:
                    mIsTimeslot0 = !mIsTimeslot0;
                    return mIsTimeslot0 ? 0 : 1;
            }
        }
    }

    /**
     * Processes a sync loss to track how many bits were processed and update message listeners.
     *
     * @param bitsProcessed since the last sync detect
     */
    @Override
    public void syncLost(int bitsProcessed)
    {
        updateBitsProcessed(bitsProcessed);

        dispatchSyncLoss(bitsProcessed);

        if(mSyncDetectListener != null)
        {
            mSyncDetectListener.syncLost(bitsProcessed);
        }
    }

    /**
     * Creates a sync loss message and dispatches it to the message listener
     *
     * @param bitsProcessed without a sync detection
     */
    private void dispatchSyncLoss(int bitsProcessed)
    {
        if(bitsProcessed > 0 && mMessageListener != null)
        {
            mMessageListener.receive(new SyncLossMessage(getTimestamp(), bitsProcessed, Protocol.DMR));
        }
    }

    public static class MessageListener implements Listener<IMessage>
    {
        private boolean mHasDMRData = false;

        @Override
        public void receive(IMessage message)
        {
            mLog.info("TS:" + message.getTimeslot() + " " + message.toString());

            if(message instanceof DMRMessage)
            {
//                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
                mHasDMRData = true;
            }

            if(message instanceof CapacityPlusUnknown41)
            {
//                mLog.info("TS:" + message.getTimeslot() + " " + message.toString());
            }

            if(message instanceof UnknownCSBKMessage)
            {
//                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
            }

            if(message instanceof Preamble)
            {
//                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
            }

            if(message instanceof DataHeader)
            {
//                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
            }

            if(message instanceof DataBlock)
            {
//                mLog.info("TS:" + ((DMRMessage)message).getTimeslot() + " " + message.toString());
            }

            if(message instanceof ShortLCMessage)
            {
//                mLog.debug(message.toString());
            }
        }

        public boolean hasData()
        {
            return mHasDMRData;
        }

        public void reset()
        {
            mHasDMRData = false;
        }
    }

    public static void main(String[] args)
    {
//        String file = "/home/denny/SDRTrunk/recordings/20200513_143340_9600BPS_DMR_SaiaNet_Onondaga_Control.bits";
        String path = "C:\\Users\\Denny\\SDRTrunk\\recordings\\";
        String file = path + "20200716_223547_9600BPS_DMR_Dallas_Unk_Dallas_Unk.bits";

        MessageListener listener = new MessageListener();
        DecodeConfigDMR config = new DecodeConfigDMR();

        boolean multi = false;

        if(multi)
        {
            try
            {
                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path), "*.bits");

                stream.forEach(new Consumer<Path>()
                {
                    @Override
                    public void accept(Path path)
                    {
                        mLog.info("Processing: " + path.toString());
                        DMRMessageFramer messageFramer = new DMRMessageFramer(null);
                        DMRMessageProcessor messageProcessor = new DMRMessageProcessor(config);
                        messageFramer.setListener(messageProcessor);
                        messageProcessor.setMessageListener(listener);

                        try(BinaryReader reader = new BinaryReader(path, 200))
                        {
                            while(reader.hasNext())
                            {
                                ReusableByteBuffer buffer = reader.next();
                                messageFramer.receive(buffer);
                            }
                        }
                        catch(Exception ioe)
                        {
                            ioe.printStackTrace();
                        }

                        if(!listener.hasData())
                        {
                            mLog.info("Has Data: " + listener.hasData() + " File:" + path.toString());
//                            try
//                            {
//                                Files.delete(path);
//                            }
//                            catch(IOException ioe)
//                            {
//                                ioe.printStackTrace();
//                            }
                        }

                        listener.reset();

                    }
                });
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
        else
        {
            DMRMessageFramer messageFramer = new DMRMessageFramer(null);
            DMRMessageProcessor messageProcessor = new DMRMessageProcessor(config);
            messageFramer.setListener(messageProcessor);
            messageProcessor.setMessageListener(listener);

            try(BinaryReader reader = new BinaryReader(Path.of(file), 200))
            {
                while(reader.hasNext())
                {
                    ReusableByteBuffer buffer = reader.next();
                    messageFramer.receive(buffer);
                }
            }
            catch(Exception ioe)
            {
                ioe.printStackTrace();
            }
        }
    }
}
