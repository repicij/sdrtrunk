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

import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;

/**
 * DMR Tier III Logical Physical Channel Number Identifier.
 */
public class DMRLogicalChannel extends DMRChannel
{
    private int mTimeslot = 0;

    public DMRLogicalChannel(int logicalPhysicalChannelNumber, int timeslot)
    {
        super(logicalPhysicalChannelNumber);
        mTimeslot = timeslot;
    }

    /**
     * Constructs an instance with timeslot defaulted to 0
     * @param logicalPhysicalChannelNumber
     */
    public DMRLogicalChannel(int logicalPhysicalChannelNumber)
    {
        this(logicalPhysicalChannelNumber, 0);
    }

    /**
     * Downlink frequency
     * @return value in Hertz
     */
    @Override
    public long getDownlinkFrequency()
    {
        //TODO: figure out a better strategy.
        return 450000000 + ((getValue() - 1) * 12500);
    }

    /**
     * Uplink frequency
     * @return value in Hertz
     */
    @Override
    public long getUplinkFrequency()
    {
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
        return mTimeslot;
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

        sb.append(getValue());
        sb.append(" TS:").append(getTimeslot());

        return sb.toString();
    }
}
