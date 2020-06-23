/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.symbol.QPSKCarrierLock;

/**
 * Listener interface to be notified each time a sync pattern has been detected
 */
public interface IDMRSyncDetectListener
{
    /**
     * Indicates that a sync pattern has been detected.
     *
     * @param pattern detected
     * @param carrier lock indication that conveys any PLL lock correction action that may be necessary.
     * @param bitErrors count for soft sync matching to indicate the number of bit positions
     * of the sequence that didn't fully match the sync pattern
     */
    void syncDetected(DMRSyncPattern pattern, QPSKCarrierLock carrierLock, int bitErrors);
}
