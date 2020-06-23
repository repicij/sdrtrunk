/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.DefinedDataFormat;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Defined Short Data Header
 */
public class DefinedShortDataHeader extends DataHeader
{
    private static final int RADIO_TALKGROUP_FLAG = 0;
    private static final int[] SERVICE_ACCESS_POINT = new int[]{8, 9, 10, 11};
    private static final int[] APPENDED_BLOCKS = new int[]{2, 3, 12, 13, 14, 15};
    private static final int[] DESTINATION_IDENTIFIER = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SOURCE_RADIO = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54,
        55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] DEFINED_DATA_FORMAT = new int[]{64, 65, 66, 67, 68, 69};
    private static final int RESYNCHRONIZE_FLAG = 70;
    private static final int FULL_MESSAGE_FLAG = 71;
    private static final int[] BIT_PADDING = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private IntegerIdentifier mDestinationLLID;
    private RadioIdentifier mSourceLLID;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     */
    public DefinedShortDataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DEFINED SHORT DATA HEADER");
        sb.append(" FM:").append(getSourceLLID());
        sb.append(" TO:").append(getDestinationLLID());
        sb.append(" FORMAT:").append(getDefinedDataFormat());
        sb.append(" ").append(getServiceAccessPoint());
        if(isFullMessage())
        {
            sb.append(" FULL MESSAGE");
        }
        if(isResynchronize())
        {
            sb.append(" RESYNCHRONIZE");
        }
        sb.append(" BIT PADDING:").append(getBitPadding());
        sb.append(" APPENDED BLOCKS:").append(getAppendedBlocks());
        return sb.toString();
    }

    /**
     * Format for the data payload for this packet sequence.
     */
    public DefinedDataFormat getDefinedDataFormat()
    {
        return DefinedDataFormat.fromValue(getMessage().getInt(DEFINED_DATA_FORMAT));
    }

    /**
     * Indicates if this is a full message
     */
    public boolean isFullMessage()
    {
        return getMessage().get(FULL_MESSAGE_FLAG);
    }

    /**
     * Indicates if this is resynchronized data
     */
    public boolean isResynchronize()
    {
        return getMessage().get((RESYNCHRONIZE_FLAG));
    }

    /**
     * Bit padding value.
     */
    public int getBitPadding()
    {
        return getMessage().getInt(BIT_PADDING);
    }

    /**
     * Number of appended blocks
     */
    public int getAppendedBlocks()
    {
        return getMessage().getInt(APPENDED_BLOCKS);
    }

    /**
     * Service access point for the specified message
     */
    public static ServiceAccessPoint getServiceAccessPoint(CorrectedBinaryMessage message)
    {
        return ServiceAccessPoint.fromValue(message.getInt(SERVICE_ACCESS_POINT));
    }

    /**
     * Service access point for this message
     */
    public ServiceAccessPoint getServiceAccessPoint()
    {
        return getServiceAccessPoint(getMessage());
    }

    /**
     * Destination Logical Link ID
     */
    public IntegerIdentifier getDestinationLLID()
    {
        if(mDestinationLLID == null)
        {
            if(getMessage().get(RADIO_TALKGROUP_FLAG))
            {
                mDestinationLLID = DMRTalkgroup.create(getMessage().getInt(DESTINATION_IDENTIFIER));
            }
            else
            {
                mDestinationLLID = DMRRadio.createTo(getMessage().getInt(DESTINATION_IDENTIFIER));
            }
        }

        return mDestinationLLID;
    }


    /**
     * Source Logical Link ID
     */
    public RadioIdentifier getSourceLLID()
    {
        if(mSourceLLID == null)
        {
            mSourceLLID = DMRRadio.createFrom(getMessage().getInt(SOURCE_RADIO));
        }

        return mSourceLLID;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getDestinationLLID());
            mIdentifiers.add(getSourceLLID());
        }

        return mIdentifiers;
    }
}
