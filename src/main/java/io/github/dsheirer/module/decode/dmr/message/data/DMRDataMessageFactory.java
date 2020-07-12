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

package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.BPTC_196_96;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_3_4_DMR;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock1Rate;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock1_2Rate;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock3_4Rate;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.header.ConfirmedDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.DataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.DefinedShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.HeaderMessage;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.RawShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ResponseDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.StatusDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.UnconfirmedDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.UnifiedDataTransportHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.motorola.MNISProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.motorola.MotorolaProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.TerminatorMessage;
import io.github.dsheirer.module.decode.dmr.message.type.DataPacketFormat;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating data messages that contain a 196-bit BPTC protected message.
 */
public class DMRDataMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDataMessageFactory.class);
    private static final ViterbiDecoder_3_4_DMR VITERBI_DECODER = new ViterbiDecoder_3_4_DMR();

    /**
     * Creates a data message class
     * @param pattern for the DMR burst
     * @param message DMR burst as transmitted
     * @param cach from the DMR burst
     * @param timestamp for the message
     * @param timeslot for the message
     * @return data message instance
     */
    public static DataMessage create(DMRSyncPattern pattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                                     int timeslot)
    {
        SlotType slotType = SlotType.getSlotType(message);

        if(slotType.isValid())
        {
            switch(slotType.getDataType())
            {
                case SLOT_IDLE:
                    return new IDLEMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case CSBK:
                    return CSBKMessageFactory.create(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case CHANNEL_CONTROL_ENC_HEADER:
                case CSBK_ENC_HEADER:
                case DATA_ENC_HEADER:
                case MBC_ENC_HEADER:
                case MBC_HEADER:
                case PI_HEADER:
                case VOICE_HEADER:
                    return new HeaderMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case DATA_HEADER:
                    CorrectedBinaryMessage payload = getPayload(message);
                    DataPacketFormat dpf = DataHeader.getDataPacketFormat(payload);

                    switch(dpf)
                    {
                        case CONFIRMED_DATA_PACKET:
                            return new ConfirmedDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                        case PROPRIETARY_DATA_PACKET:
                            Vendor vendor = ProprietaryDataHeader.getVendor(payload);

                            switch(vendor)
                            {
                                case MOTOROLA_CAPACITY_PLUS:
                                case MOTOROLA_CONNECT_PLUS:
                                    ServiceAccessPoint sap = ProprietaryDataHeader.getServiceAccessPoint(payload);

                                    if(sap == ServiceAccessPoint.SAP_1)
                                    {
                                        return new MNISProprietaryDataHeader(pattern, payload, cach, slotType,
                                            timestamp, timeslot);
                                    }
                                    else
                                    {
                                        return new MotorolaProprietaryDataHeader(pattern, payload, cach, slotType,
                                            timestamp, timeslot);
                                    }
                                default:
                                    return new ProprietaryDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                            }
                        case RAW_OR_STATUS_SHORT_DATA:
                            int appendedBlockCount = ShortDataHeader.getAppendedBlocks(payload);

                            if(appendedBlockCount == 0)
                            {
                                return new StatusDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                            }
                            else
                            {
                                return new RawShortDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                            }
                        case RESPONSE_PACKET:
                            return new ResponseDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                        case DEFINED_SHORT_DATA:
                            return new DefinedShortDataHeader(pattern, payload, cach,slotType, timestamp, timeslot);
                        case UNCONFIRMED_DATA_PACKET:
                            return new UnconfirmedDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                        case UNIFIED_DATA_TRANSPORT:
                            //TODO: this will eventually need its own factory
                            return new UnifiedDataTransportHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                        default:
                            return new DataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                    }
                case TLC:
                    return new TerminatorMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_1_OF_2_DATA:
                    return new DataBlock1_2Rate(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_3_OF_4_DATA:
                    return new DataBlock3_4Rate(pattern, getTrellisPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_1_DATA:
                    return new DataBlock1Rate(pattern, message, cach, slotType, timestamp, timeslot);
                case MBC:
                case RESERVED_15:
                case UNKNOWN:
                    return new UnknownDataMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
            }
        }

        return new UnknownDataMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
    }

    /**
     * Decodes a 3/4 rate trellis coded message
     * @param message containing trellis coded message
     * @return decoded message
     */
    private static CorrectedBinaryMessage getTrellisPayload(BinaryMessage message)
    {
        return VITERBI_DECODER.decode(message);
    }

    /**
     * De-scramble, decode and error check a BPTC protected raw message and return a 96-bit error corrected
     * payload or null.
     * @param _message
     * @return
     */
    private static CorrectedBinaryMessage getPayload(CorrectedBinaryMessage _message)
    {
        CorrectedBinaryMessage bm1 = new CorrectedBinaryMessage(196);

        try
        {
            for(int i = 24; i < 122; i++)
            {
                bm1.add(_message.get(i));
            }
            for(int i = 190; i < 190 + 98; i++)
            {
                bm1.add(_message.get(i));
            }
        }
        catch(BitSetFullException ex)
        {
            mLog.error("Error decoding DMR BPTC 196 structure");
        }

//        CorrectedBinaryMessage message = BPTC_196_96.deinterleave(bm1);
//
//        //TODO: detect the quantity of bits that were repaired and insert that value into the corrected binary message result
//        if(bptc_196_96_check_and_repair(message))
//        {
//            message = bptc_196_96_extractdata(message);
//        }
//        else
//        {
//            //TODO: the above should always return the payload, even if it fails error correction which should cause
//            //TODO: the wrapping message class to setValid(false).  That way you're never having to deal with null values.
//            message = null;
//        }
//
//        return message;

        return BPTC_196_96.extract(bm1);
    }
}
