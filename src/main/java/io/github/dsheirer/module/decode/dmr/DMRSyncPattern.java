/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
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

import java.util.EnumSet;

public enum DMRSyncPattern
{
    BASE_STATION_DATA(0xDFF57D75DF5Dl, 0xBAAFEBEFBAFBl, 0x455014104504l, 0x200A828A20A2l, "BS DATA"),
    BASE_STATION_VOICE(0x755FD7DF75F7l, 0xEFFABEBAEFAEl, 0x100541451051l, 0x8AA028208A08l, "BS VOIC"),

    MOBILE_STATION_DATA(0xD5D7F77FD757l, 0xBFBEAEEABEFEl, 0x404151154101l, 0x2A28088028A8l, "MS DATA"),
    MOBILE_STATION_VOICE(0x7F7D5DD57DFDl, 0xEAEBFBBFEBABl, 0x151404401454l, 0x8082A22A8202l, "MS VOIC"),
    DIRECT_MODE_DATA_TIMESLOT_0(0xF7FDD5DDFD55l, 0xAEABBFBBABFFl, 0x515440445400l, 0x08022A2202AAl, "DM DAT0"),
    DIRECT_MODE_DATA_TIMESLOT_1(0xD7557F5FF7F5l, 0xBEFFEAFAAEAFl, 0x410015055150l, 0x28AA80A0080Al, "DM DAT1"),
    DIRECT_MODE_VOICE_TIMESLOT_0(0x5D577F7757FFl, 0xFBFEEAEEFEAAl, 0x040115110155l, 0xA2A88088A800l, "DM VOX0"),
    DIRECT_MODE_VOICE_TIMESLOT_1(0x7DFFD5F55D5Fl, 0xEBAABFAFFBFAl, 0x145540500405l, 0x82002A0AA2A0l, "DM VOX1"),
    MOBILE_STATION_REVERSE_CHANNEL(0x77D55F7DFD77l, 0xEEBFFAEBABEEl, 0x114005145411l, 0x882AA0820288l, "MS RVRS"),

    RESERVED(0xDD7FF5D757DDl, 0xBBEAAFBEFEBBl, 0x441550410144l, 0x22800A28A822l, "RESERVED"),

    //These are used to identify the sync-less sub frames of the voice super frame
    VOICE_FRAME_B(-2, 0,0,0, "VOICE B"),
    VOICE_FRAME_C(-3, 0, 0, 0,"VOICE C"),
    VOICE_FRAME_D(-4, 0,0,0,"VOICE D"),
    VOICE_FRAME_E(-5, 0,0,0,"VOICE E"),
    VOICE_FRAME_F(-6, 0,0,0,"VOICE F"),

    UNKNOWN(-1, 0, 0, 0, "UNKNOWN");

    private long mPattern;
    private long mPlus90Pattern;
    private long mMinus90Pattern;
    private long mInvertedPattern;
    private String mLabel;

    //Base Station sync patterns
    public static final EnumSet<DMRSyncPattern> BASE_STATION_SYNC_PATTERNS = EnumSet.of(BASE_STATION_DATA, BASE_STATION_VOICE);

    //Mobile Subscriber sync patterns
    public static final EnumSet<DMRSyncPattern> MOBILE_SUBSCRIBER_SYNC_PATTERNS = EnumSet.of(MOBILE_STATION_DATA,
            MOBILE_STATION_VOICE, DIRECT_MODE_DATA_TIMESLOT_0, DIRECT_MODE_DATA_TIMESLOT_1,
            DIRECT_MODE_VOICE_TIMESLOT_0, DIRECT_MODE_VOICE_TIMESLOT_1, MOBILE_STATION_REVERSE_CHANNEL);

    //Valid sync patterns (excluding the sync-less voice patterns)
    public static final EnumSet<DMRSyncPattern> SYNC_PATTERNS = EnumSet.range(BASE_STATION_VOICE, RESERVED);

    //Sync patterns containing a Common Announcement Channel (CACH)
    public static final EnumSet<DMRSyncPattern> CACH_PATTERNS = EnumSet.of(BASE_STATION_DATA, BASE_STATION_VOICE);

    //Sync patterns for Data Frames
    public static final EnumSet<DMRSyncPattern> DATA_PATTERNS = EnumSet.of(BASE_STATION_DATA, MOBILE_STATION_DATA);

    //Sync patterns for Voice Frames
    public static final EnumSet<DMRSyncPattern> VOICE_PATTERNS = EnumSet.of(BASE_STATION_VOICE, MOBILE_STATION_VOICE,
            VOICE_FRAME_B, VOICE_FRAME_C, VOICE_FRAME_D, VOICE_FRAME_E, VOICE_FRAME_F);

    /**
     * DMR Sync Patterns.  See TS 102-361-1, paragraph 9.1.1
     */
    DMRSyncPattern(long pattern, long plus90Pattern, long minus90Pattern, long invertedPattern, String label)
    {
        mPattern = pattern;
        mPlus90Pattern = plus90Pattern;
        mMinus90Pattern = minus90Pattern;
        mInvertedPattern = invertedPattern;
        mLabel = label;
    }

    /**
     * Pattern that represents the enum entry
     */
    public long getPattern()
    {
        return mPattern;
    }

    /**
     * Pattern that represents the PLL locking to the carrier misaligned by plus 90 degrees.
     */
    public long getPlus90Pattern()
    {
        return mPlus90Pattern;
    }

    /**
     * Pattern that represents the PLL locking to the carrier misaligned by minus 90 degrees.
     */
    public long getMinus90Pattern()
    {
        return mMinus90Pattern;
    }

    /**
     * Pattern that represents the PLL locking to the carrier misaligned by 180 degrees (inverted).
     */
    public long getInvertedPattern()
    {
        return mInvertedPattern;
    }


    /**
     * String representation of the sync pattern
     */
    public String toString()
    {
        return mLabel;
    }

    /**
     * Indicates if a message using this sync pattern contains a Common Announcement Channel (CACH).
     */
    public boolean hasCACH()
    {
        return CACH_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a data sync pattern
     */
    public boolean isData()
    {
        return DATA_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a voice sync pattern
     */
    public boolean isVoice()
    {
        return VOICE_PATTERNS.contains(this);
    }

    /**
     * Lookup the DMR Sync Pattern from the transmitted value.
     *
     * @param value to match to a pattern
     * @return the matching enum entry or UNKNOWN
     */
    public static DMRSyncPattern fromValue(long value)
    {
        if(value < 0)
        {
            for(DMRSyncPattern pattern : VOICE_PATTERNS)
            {
                if(pattern.getPattern() == value)
                {
                    return pattern;
                }
            }
            return UNKNOWN;
        }
        for(DMRSyncPattern pattern : SYNC_PATTERNS)
        {
            if(pattern.getPattern() == value)
            {
                return pattern;
            }
        }

        return UNKNOWN;
    }
}