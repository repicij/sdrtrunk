/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the network configuration details of a P25 Phase 1 network from the broadcast messages
 */
public class DMRNetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRNetworkConfigurationMonitor.class);
    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new HashMap<>();

    /**
     * Constructs a network configuration monitor.
     */
    public DMRNetworkConfigurationMonitor()
    {
    }

    public void reset()
    {
        mFrequencyBandMap.clear();
    }

    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder:DMR");
        sb.append("\nFrequency Bands\n");
        if(mFrequencyBandMap.isEmpty())
        {
            sb.append("  UNKNOWN");
        }
        else
        {
            List<Integer> ids = new ArrayList<>(mFrequencyBandMap.keySet());
            Collections.sort(ids);
            {
                for(Integer id : ids)
                {
                    sb.append("  ").append(formatFrequencyBand(mFrequencyBandMap.get(id))).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Formats a frequency band
     */
    private String formatFrequencyBand(IFrequencyBand band)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("BAND:").append(band.getIdentifier());
        sb.append(" ").append(band.isTDMA() ? "TDMA" : "FDMA");
        sb.append(" BASE:").append(band.getBaseFrequency());
        sb.append(" BANDWIDTH:").append(band.getBandwidth());
        sb.append(" SPACING:").append(band.getChannelSpacing());
        sb.append(" TRANSMIT OFFSET:").append(band.getTransmitOffset());

        if(band.isTDMA())
        {
            sb.append(" TIMESLOTS:").append(band.getTimeslotCount());
        }

        return sb.toString();
    }

}
