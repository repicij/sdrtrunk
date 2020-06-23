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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;

import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Announcement or Widthdraw TSCC
 */
public class AnnounceWithdrawTSCC extends Announcement
{
    //Broadcast Parameters 1: 21-34
    private static final int[] RESERVED = new int[]{21, 22, 23, 24};
    private static final int[] COLOR_CODE_CHAN_1 = new int[]{25, 26, 27, 28};
    private static final int[] COLOR_CODE_CHAN_2 = new int[]{29, 30, 31, 32};
    private static final int ADD_WITHDRAW_CHAN_1_FLAG = 33;
    private static final int ADD_WITHDRAW_CHAN_2_FLAG = 34;

    //Broadcast Parameters 2: 56-79
    private static final int[] CHANNEL_NUMBER_1 = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67};
    private static final int[] CHANNEL_NUMBER_2 = new int[]{68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private DMRLogicalChannel mChannel1;
    private DMRLogicalChannel mChannel2;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public AnnounceWithdrawTSCC(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());

        if(hasChannel1())
        {
            sb.append((isChannel1Add() ? " ADD CHAN:" : " WITHDRAW CHAN:"));
            sb.append(getChannel1()).append(" CC:").append(getChannel1ColorCode());
        }

        if(hasChannel2())
        {
            sb.append((isChannel2Add() ? " ADD CHAN:" : " WITHDRAW CHAN:"));
            sb.append(getChannel2()).append(" CC:").append(getChannel2ColorCode());
        }

        sb.append(" ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());

        return sb.toString();
    }

    /**
     * Indicates if this message has channel 1 information
     */
    public boolean hasChannel1()
    {
        return getMessage().getInt(CHANNEL_NUMBER_1) != 0;
    }

    /**
     * Indicates if this message has channel 2 information
     */
    public boolean hasChannel2()
    {
        return getMessage().getInt(CHANNEL_NUMBER_2) != 0;
    }

    /**
     * Color code for channel 1
     */
    public int getChannel1ColorCode()
    {
        return getMessage().getInt(COLOR_CODE_CHAN_1);
    }

    /**
     * Color code for channel 2
     */
    public int getChannel2ColorCode()
    {
        return getMessage().getInt(COLOR_CODE_CHAN_2);
    }

    /**
     * Indicates if channel 1 is being added (true) or withdrawn (false)
     */
    public boolean isChannel1Add()
    {
        return getMessage().get(ADD_WITHDRAW_CHAN_1_FLAG);
    }

    /**
     * Indicates if channel 2 is being added (true) or withdrawn (false)
     */
    public boolean isChannel2Add()
    {
        return getMessage().get(ADD_WITHDRAW_CHAN_2_FLAG);
    }

    /**
     * Channel 1
     */
    public DMRLogicalChannel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = new DMRLogicalChannel(getMessage().getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    /**
     * Channel 2
     */
    public DMRLogicalChannel getChannel2()
    {
        if(mChannel2 == null)
        {
            mChannel2 = new DMRLogicalChannel(getMessage().getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }
}
