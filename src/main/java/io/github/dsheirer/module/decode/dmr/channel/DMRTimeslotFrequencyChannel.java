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

package io.github.dsheirer.module.decode.dmr.channel;

import io.github.dsheirer.module.decode.dmr.TimeslotFrequency;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;

/**
 * DMR Channel Identifier for MotoTRBO Logical Slot Numbers (LSN).
 *
 * Logical Slot Numbers are 1-based index (1, 2, 3, 4, etc.) where the odd values are timeslot 0 and even values are 1.
 */
public class DMRTimeslotFrequencyChannel extends DMRChannel
{
    private TimeslotFrequency mTimeslotFrequency;

    /**
     * Constructs an instance
     * @param logicalSlotNumber
     */
    public DMRTimeslotFrequencyChannel(int logicalSlotNumber)
    {
        super(logicalSlotNumber);
    }

    /**
     * Logical slot number for this channel
     * @return logical slot number, a 1-based index value
     */
    public int getLogicalSlotNumber()
    {
        return getValue();
    }

    /**
     * Returns a array of length 1 containing this channel's logical slot number
     */
    public int[] getLSNArray()
    {
        int[] lsns = new int[1];
        lsns[0] = getLogicalSlotNumber();
        return lsns;
    }

    /**
     * Downlink frequency
     * @return value in Hertz, or 0 if this channel doesn't have a timeslot frequency mapping
     */
    @Override
    public long getDownlinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getDownlinkFrequency();
        }

        return 0;
    }

    /**
     * Uplink frequency
     * @return value in Hertz, or 0 if this channel doesn't have a timeslot frequency mapping
     */
    @Override
    public long getUplinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getUplinkFrequency();
        }

        return 0;
    }

    /**
     * Not implemented
     */
    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return new int[0];
    }

    /**
     * Sets the timeslot frequency mapping
     * @param timeslotFrequency
     */
    public void setTimeslotFrequency(TimeslotFrequency timeslotFrequency)
    {
        mTimeslotFrequency = timeslotFrequency;
    }

    /**
     * Number of timeslots for the DMR channel.
     * @return 2 always.
     */
    @Override
    public int getTimeslotCount()
    {
        return 2;
    }

    /**
     * Indicates (true) that this is a TDMA channel.
     */
    @Override
    public boolean isTDMAChannel()
    {
        return true;
    }

    /**
     * Zero-based index timeslot for a DMR Logical Slot Number
     * @return timeslot, 0 or 1
     */
    public int getTimeslot()
    {
        return (getLogicalSlotNumber() + 1) % 2;
    }

    /**
     * Not implemented.
     */
    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        throw new IllegalArgumentException("This method is not supported");
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("LSN:").append(getLogicalSlotNumber());
        sb.append(" TS:").append(getTimeslot());

        return sb.toString();
    }
}
