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

package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusAloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusChannelActive;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusNeighborReport;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusTerminateChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Aloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.MoveTSCC;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.AnnounceWithdrawTSCC;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.Announcement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.CallTimerParameters;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.LocalTime;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.MassRegistration;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.NeighborSiteInformation;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.VoteNowAdvice;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.BroadcastTalkgroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.DuplexPrivateDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.DuplexPrivateVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.PrivateDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.PrivateVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.TalkgroupDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.TalkgroupVoiceChannelGrant;

/**
 * Factory for creating DMR CSBK messages
 */
public class CSBKMessageFactory
{
    public static CSBKMessage create(DMRSyncPattern pattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                                     long timestamp, int timeslot)
    {
        if(message != null)
        {
            int corrected = CRCDMR.correctCCITT80(message, 0, 80, 0xA5A5);

            Opcode opcode = CSBKMessage.getOpcode(message);

            switch(opcode)
            {
                case STANDARD_ALOHA:
                    return new Aloha(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_ANNOUNCEMENT:
                    switch(Announcement.getAnnouncementType(message))
                    {
                        case ADJACENT_SITE_INFORMATION:
                            return new NeighborSiteInformation(pattern, message, cach, slotType, timestamp, timeslot);
                        case ANNOUNCE_OR_WITHDRAW_TSCC:
                            return new AnnounceWithdrawTSCC(pattern, message, cach, slotType, timestamp, timeslot);
                        case CALL_TIMER_PARAMETERS:
                            return new CallTimerParameters(pattern, message, cach, slotType, timestamp, timeslot);
                        case LOCAL_TIME:
                            return new LocalTime(pattern, message, cach, slotType, timestamp, timeslot);
                        case MASS_REGISTRATION:
                            return new MassRegistration(pattern, message, cach, slotType, timestamp, timeslot);
                        case VOTE_NOW_ADVICE:
                            return new VoteNowAdvice(pattern, message, cach, slotType, timestamp, timeslot);
                        default:
                            return new Announcement(pattern, message, cach, slotType, timestamp, timeslot);
                    }
                case STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT:
                    return new BroadcastTalkgroupVoiceChannelGrant(pattern, message, cach, slotType, timestamp,
                            timeslot);
                case STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT:
                    return new DuplexPrivateDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT:
                    return new DuplexPrivateVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_MOVE_TSCC:
                    return new MoveTSCC(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_PREAMBLE:
                    return new Preamble(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                    return new PrivateDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_PRIVATE_VOICE_CHANNEL_GRANT:
                    return new PrivateVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                    return new TalkgroupDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT:
                    return new TalkgroupVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);

                case MOTOROLA_CAPPLUS_ALOHA:
                    return new CapacityPlusAloha(pattern, message, cach, slotType, timestamp, timeslot);

                case MOTOROLA_CONPLUS_NEIGHBOR_REPORT:
                    return new ConnectPlusNeighborReport(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_VOICE_CHANNEL_GRANT:
                    return new ConnectPlusVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_DATA_CHANNEL_GRANT:
                    return new ConnectPlusDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_TERMINATE_CHANNEL_GRANT:
                    return new ConnectPlusTerminateChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_CHANNEL_ACTIVE:
                    return new ConnectPlusChannelActive(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_CHANNEL_USER:
                    return new ConnectPlusChannelUser(pattern, message, cach, slotType, timestamp, timeslot);
                default:
                    return new UnknownCSBKMessage(pattern, message, cach, slotType, timestamp, timeslot);
            }
        }

        return null;
    }
}
