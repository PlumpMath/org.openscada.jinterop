/* Donated by Jarapac (http://jarapac.sourceforge.net/)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110, USA
 */

package rpc.core;

import java.util.StringTokenizer;

import ndr.NdrBuffer;
import ndr.NdrException;
import ndr.NdrObject;
import ndr.NetworkDataRepresentation;

public class UUID extends NdrObject
{

    public static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";

    private static final int TIMELOW_INDEX = 0;

    private static final int TIMEMID_INDEX = 1;

    private static final int TIMEHIGHANDVERSION_INDEX = 2;

    private static final int CLOCKSEQHIGHANDRESERVED_INDEX = 3;

    private static final int CLOCKSEQLOW_INDEX = 4;

    private static final int NODE_INDEX = 5;

    int timeLow, timeMid, timeHighAndVersion, clockSeqHighAndReserved, clockSeqLow;

    byte[] node = new byte[6];

    public UUID ()
    {
    }

    public UUID ( final String uuid )
    {
        parse ( uuid );
    }

    @Override
    public void encode ( final NetworkDataRepresentation ndr, final NdrBuffer dst ) throws NdrException
    {
        dst.enc_ndr_long ( this.timeLow );
        dst.enc_ndr_short ( this.timeMid );
        dst.enc_ndr_short ( this.timeHighAndVersion );
        dst.enc_ndr_small ( this.clockSeqHighAndReserved );
        dst.enc_ndr_small ( this.clockSeqLow );
        System.arraycopy ( this.node, 0, dst.buf, dst.index, 6 );
        dst.index += 6;
    }

    @Override
    public void decode ( final NetworkDataRepresentation ndr, final NdrBuffer src ) throws NdrException
    {
        this.timeLow = src.dec_ndr_long ();
        this.timeMid = src.dec_ndr_short ();
        this.timeHighAndVersion = src.dec_ndr_short ();
        this.clockSeqHighAndReserved = src.dec_ndr_small ();
        this.clockSeqLow = src.dec_ndr_small ();
        System.arraycopy ( src.buf, src.index, this.node, 0, 6 );
        src.index += 6;
    }

    /*
        public long getTimeLow() {
            return ((UnsignedLongHolder)
                    structure.get(TIMELOW_INDEX)).getUnsignedLong();
        }

        public void setTimeLow(long timeLow) {
            ((UnsignedLongHolder) structure.get(TIMELOW_INDEX)).setUnsignedLong(
                    timeLow);
        }

        public int getTimeMid() {
            return ((UnsignedShortHolder)
                    structure.get(TIMEMID_INDEX)).getUnsignedShort();
        }

        public void setTimeMid(int timeMid) {
            ((UnsignedShortHolder) structure.get(TIMEMID_INDEX)).setUnsignedShort(
                    timeMid);
        }

        public int getTimeHighAndVersion() {
            return ((UnsignedShortHolder)
                    structure.get(TIMEHIGHANDVERSION_INDEX)).getUnsignedShort();
        }

        public void setTimeHighAndVersion(int timeHighAndVersion) {
            ((UnsignedShortHolder)
                    structure.get(TIMEHIGHANDVERSION_INDEX)).setUnsignedShort(
                            timeHighAndVersion);
        }

        public short getClockSeqHighAndReserved() {
            return ((UnsignedSmallHolder) structure.get(
                    CLOCKSEQHIGHANDRESERVED_INDEX)).getUnsignedSmall();
        }

        public void setClockSeqHighAndReserved(short clockSeqHighAndReserved) {
            ((UnsignedSmallHolder) structure.get(
                    CLOCKSEQHIGHANDRESERVED_INDEX)).setUnsignedSmall(
                            clockSeqHighAndReserved);
        }

        public short getClockSeqLow() {
            return ((UnsignedSmallHolder) structure.get(
                    CLOCKSEQLOW_INDEX)).getUnsignedSmall();
        }

        public void setClockSeqLow(short clockSeqLow) {
            ((UnsignedSmallHolder) structure.get(
                    CLOCKSEQLOW_INDEX)).setUnsignedSmall(clockSeqLow);
        }

        public byte[] getNode() {
            return (byte[]) ((FixedArray) structure.get(NODE_INDEX)).getArray();
        }

        public void setNode(byte[] node) {
            ((FixedArray) structure.get(NODE_INDEX)).setArray(node);
        }
    */

    @Override
    public String toString ()
    {
        final StringBuffer buffer = new StringBuffer ();
        //        int timeLow = (int) (getTimeLow() & 0xffffffffl);
        buffer.append ( Integer.toHexString ( this.timeLow >> 28 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow >> 24 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow >> 20 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow >> 16 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow >> 12 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow >> 8 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow >> 4 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeLow & 0x0f ) );
        buffer.append ( '-' );
        //        int timeMid = getTimeMid();
        buffer.append ( Integer.toHexString ( this.timeMid >> 12 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeMid >> 8 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeMid >> 4 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeMid & 0x0f ) );
        buffer.append ( '-' );
        //        int timeHighAndVersion = getTimeHighAndVersion();
        buffer.append ( Integer.toHexString ( this.timeHighAndVersion >> 12 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeHighAndVersion >> 8 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeHighAndVersion >> 4 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.timeHighAndVersion & 0x0f ) );
        buffer.append ( '-' );
        //        short clockSeqHighAndReserved = getClockSeqHighAndReserved();
        buffer.append ( Integer.toHexString ( this.clockSeqHighAndReserved >> 4 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.clockSeqHighAndReserved & 0x0f ) );
        //        short clockSeqLow = getClockSeqLow();
        buffer.append ( Integer.toHexString ( this.clockSeqLow >> 4 & 0x0f ) );
        buffer.append ( Integer.toHexString ( this.clockSeqLow & 0x0f ) );
        buffer.append ( '-' );
        //        byte[] node = getNode();
        for ( int i = 0; i < 6; i++ )
        {
            buffer.append ( Integer.toHexString ( this.node[i] >> 4 & 0x0f ) );
            buffer.append ( Integer.toHexString ( this.node[i] & 0x0f ) );
        }
        return buffer.toString ();
    }

    public void parse ( final String uuid )
    {
        final StringTokenizer tokenizer = new StringTokenizer ( uuid, "-" );
        this.timeLow = (int)Long.parseLong ( tokenizer.nextToken (), 16 );
        this.timeMid = Integer.parseInt ( tokenizer.nextToken (), 16 );
        this.timeHighAndVersion = Integer.parseInt ( tokenizer.nextToken (), 16 );
        String token = tokenizer.nextToken ();
        this.clockSeqHighAndReserved = Integer.parseInt ( token.substring ( 0, 2 ), 16 );
        this.clockSeqLow = Integer.parseInt ( token.substring ( 2 ), 16 );
        token = tokenizer.nextToken ();
        this.node = new byte[6];
        for ( int i = 0; i < 6; i++ )
        {
            final int offset = i * 2;
            this.node[i] = (byte) ( Character.digit ( token.charAt ( offset ), 16 ) << 4 | Character.digit ( token.charAt ( offset + 1 ), 16 ) );
        }
        /*
                setTimeLow(timeLow);
                setTimeMid(timeMid);
                setTimeHighAndVersion(timeHighAndVersion);
                setClockSeqHighAndReserved(clockSeqHighAndReserved);
                setClockSeqLow(clockSeqLow);
                setNode(node);
        */
    }
}
