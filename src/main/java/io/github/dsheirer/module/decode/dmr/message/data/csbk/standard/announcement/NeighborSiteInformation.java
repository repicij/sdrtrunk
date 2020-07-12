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
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;

import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Announcement Message
 */
public class NeighborSiteInformation extends Announcement
{
    private static final int NEIGHBOR_SYSTEM_IDENTITY_CODE_OFFSET = 21;
    private static final int NETWORK_CONNECTION_STATUS_AVAILABLE_FLAG = 56;
    private static final int ACTIVE_NETWORK_CONNECTION_FLAG = 57;
    private static final int[] CONFIRMED_CHANNEL_PRIORITY = new int[]{58, 59, 60};
    private static final int[] ADJACENT_CHANNEL_PRIORITY = new int[]{61, 62, 63};
    private static final int[] RESERVED = new int[]{64, 65, 66, 67};
    private static final int[] NEIGHBOR_CHANNEL_NUMBER = new int[]{68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private SystemIdentityCode mNeighborSystemIdentityCode;
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
    public NeighborSiteInformation(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" ").append(getSystemIdentityCode().getModel());
        sb.append(" NEIGHBOR NETWORK:").append(getNeighborSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getNeighborSystemIdentityCode().getSite());
        sb.append(" CHAN:").append(getNeighborChannelNumber());
        sb.append(" THIS NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());

        return sb.toString();
    }

    /**
     * Indicates if the active network connection status is available.
     *
     * See also: isActiveNetworkConnection()
     */
    public boolean hasNetworkConnectionStatus()
    {
        return getMessage().get(NETWORK_CONNECTION_STATUS_AVAILABLE_FLAG);
    }

    /**
     * Indicates if this site has an active network connection
     *
     * See also: hasNetworkConnectionStatus()
     */
    public boolean isActiveNetworkConnection()
    {
        return getMessage().get(ACTIVE_NETWORK_CONNECTION_FLAG);
    }

    /**
     * Confirmed channel priority
     */
    public int getConfirmedChannelPriority()
    {
        return getMessage().getInt(CONFIRMED_CHANNEL_PRIORITY);
    }

    /**
     * Adjacent channel priority
     */
    public int getAdjacentChannelPriority()
    {
        return getMessage().getInt(ADJACENT_CHANNEL_PRIORITY);
    }

    /**
     * Control Channel Number for the neighbor site
     */
    public int getNeighborChannelNumber()
    {
        return getMessage().getInt(NEIGHBOR_CHANNEL_NUMBER);
    }

    /**
     * Neighbor Site System Identity Code structure
     */
    public SystemIdentityCode getNeighborSystemIdentityCode()
    {
        if(mNeighborSystemIdentityCode == null)
        {
            mNeighborSystemIdentityCode = new SystemIdentityCode(getMessage(),
                NEIGHBOR_SYSTEM_IDENTITY_CODE_OFFSET, false);
        }

        return mNeighborSystemIdentityCode;
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
