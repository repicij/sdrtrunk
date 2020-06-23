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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.channel.DMRTimeslotFrequencyChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Connect Plus - Data Channel Grant
 */
public class ConnectPlusDataChannelGrant extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] TARGET_ADDRESS = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] LSN = new int[]{40, 41, 42, 43, 44};
    private static final int[] UNKNOWN_FIELD = new int[]{48, 49, 50, 51, 52, 53, 54, 55};

    private RadioIdentifier mTargetRadio;
    private DMRTimeslotFrequencyChannel mDMRTimeslotFrequencyChannel;
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
    public ConnectPlusDataChannelGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK ").append(getVendor());
        sb.append(" DATA CHANNEL GRANT TO:").append(getTargetRadio());
        sb.append(" LSN:").append(getLogicalSlotNumber());
        sb.append(" UNK:").append(getUnknownField());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Target radio address
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetRadio;
    }

    /**
     * Unknown field
     */
    public int getUnknownField()
    {
        return getMessage().getInt(UNKNOWN_FIELD);
    }

    /**
     * Logical Slot Number (LSN)
     */
    public int getLogicalSlotNumber()
    {
        return getMessage().getInt(LSN) - 1;
    }

    /**
     * DMR Channel
     */
    public DMRTimeslotFrequencyChannel getDMRTimeslotFrequencyChannel()
    {
        if(mDMRTimeslotFrequencyChannel == null)
        {
            mDMRTimeslotFrequencyChannel = new DMRTimeslotFrequencyChannel(getLogicalSlotNumber());
        }

        return mDMRTimeslotFrequencyChannel;
    }

    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getDMRTimeslotFrequencyChannel().getLSNArray();
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(timeslotFrequency.getNumber() == getDMRTimeslotFrequencyChannel().getLogicalSlotNumber())
            {
                getDMRTimeslotFrequencyChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetRadio());
            mIdentifiers.add(getDMRTimeslotFrequencyChannel());
        }

        return mIdentifiers;
    }
}
