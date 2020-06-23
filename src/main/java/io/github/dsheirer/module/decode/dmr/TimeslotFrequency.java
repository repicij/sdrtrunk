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

package io.github.dsheirer.module.decode.dmr;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Maps a timeslot number to a pair of channel frequency values
 */
public class TimeslotFrequency
{
    private int mNumber;
    private long mDownlinkFrequency;
    private long mUplinkFrequency;

    /**
     * Constructs an instance
     */
    public TimeslotFrequency()
    {

    }

    /**
     * Logical slot number (LSN) as a 1-index based counter
     */
    @JacksonXmlProperty(isAttribute = true, localName = "lsn")
    public int getNumber()
    {
        return mNumber;
    }

    /**
     * Sets the logical slot number (LSN) as a 1-index start
     * @param number where LSN 1 is the first slot, 2 the second, etc
     */
    public void setNumber(int number)
    {
        mNumber = number;
    }

    /**
     * Downlink frequency
     * @return value in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "downlink")
    public long getDownlinkFrequency()
    {
        return mDownlinkFrequency;
    }

    /**
     * Sets the downlink frequency value
     * @param downlinkFrequency in hertz
     */
    public void setDownlinkFrequency(long downlinkFrequency)
    {
        mDownlinkFrequency = downlinkFrequency;
    }

    /**
     * Uplink frequency
     * @return value in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "uplink")
    public long getUplinkFrequency()
    {
        return mUplinkFrequency;
    }

    /**
     * Sets the uplink frequency value
     * @param uplinkFrequency in hertz
     */
    public void setUplinkFrequency(long uplinkFrequency)
    {
        mUplinkFrequency = uplinkFrequency;
    }
}
