/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer, Zhenyu Mao
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr;


import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Listener interface to be notified each time a DMR burst has been detected or when sync is lost.
 */
public interface IDMRBurstDetectListener
{
    /**
     * Indicates that a DMR sync has been detected and binary message with the burst contents are ready for processing
     * @param binaryMessage burst Binary Message and any bit errors detected in the message
     * @param pattern SyncPattern
     */
    void burstDetectedWithSync(CorrectedBinaryMessage binaryMessage, DMRSyncPattern pattern);

    /**
     * Indicates that sync has been lost on the dibit stream
     *
     * @param bitsProcessed since the last sync detect
     */
    void syncLost(int bitsProcessed);
}
