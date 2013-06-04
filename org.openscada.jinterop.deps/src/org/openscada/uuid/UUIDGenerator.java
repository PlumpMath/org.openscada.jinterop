/**j-Interop (Pure Java implementation of DCOM protocol)  
 * Copyright (C) 2013  Jens Reimann (ctron@dentrassi.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Though a sincere effort has been made to deliver a professional, 
 * quality product,the library itself is distributed WITHOUT ANY WARRANTY; 
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110, USA
 */
package org.openscada.uuid;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UUIDGenerator
{

    private final static Logger logger = LoggerFactory.getLogger ( UUIDGenerator.class );

    private static byte[] node;

    private static long lastTime;

    private static int sequence;

    private static final Random random;

    private static final boolean useDummyMac = Boolean.getBoolean ( "jinterop.useDummyMac" );

    private static final int MAX_SEQUENCE_NUMBER = 0x4000;

    static
    {
        random = new SecureRandom ();
        sequence = random.nextInt ();

        UUIDGenerator.node = getMacAddress ();
    }

    private static byte[] getMacAddress ()
    {
        final byte[] dummy = new byte[6];

        if ( !useDummyMac )
        {
            try
            {
                final Enumeration<NetworkInterface> i = NetworkInterface.getNetworkInterfaces ();
                while ( i.hasMoreElements () )
                {
                    final NetworkInterface ni = i.nextElement ();
                    try
                    {
                        if ( ni.isLoopback () )
                        {
                            continue;
                        }
                        final byte[] mac = ni.getHardwareAddress ();
                        if ( mac == null )
                        {
                            continue;
                        }

                        if ( mac.length == dummy.length )
                        {
                            return mac;
                        }
                        else if ( mac.length > dummy.length )
                        {
                            System.arraycopy ( mac, 0, dummy, 0, Math.min ( dummy.length, mac.length ) );
                            return dummy;
                        }
                        else
                        {
                            System.arraycopy ( mac, 0, dummy, 0, Math.min ( dummy.length, mac.length ) );
                            return dummy;
                        }

                    }
                    catch ( final SocketException e )
                    {
                        continue;
                    }

                }
            }
            catch ( final Exception e )
            {
                logger.warn ( "Failed to generate node id. Using dummy.", e );
            }
        }

        // reached last interface or failed miserably, use a random node id
        random.nextBytes ( dummy );

        /* setting the broadcast/multicast bit for generated node addresses
         * according to RFC4122 Section 4.5 - http://www.ietf.org/rfc/rfc4122.txt 
         */
        dummy[0] |= (byte)0x80;
        return dummy;
    }

    public synchronized static java.util.UUID generateID ()
    {
        updateTime ( makeTime () );

        final byte[] data = new byte[8];

        // encode time

        data[3] = (byte) ( ( lastTime & 0x00000000000000FFL ) >> 0 & 0xff );
        data[2] = (byte) ( ( lastTime & 0x000000000000FF00L ) >> 8 & 0xff );
        data[1] = (byte) ( ( lastTime & 0x0000000000FF0000L ) >> 16 & 0xff );
        data[0] = (byte) ( ( lastTime & 0x00000000FF000000L ) >> 24 & 0xff );

        data[5] = (byte) ( ( lastTime & 0x000000FF00000000L ) >> 32 & 0xff );
        data[4] = (byte) ( ( lastTime & 0x0000FF0000000000L ) >> 40 & 0xff );

        data[7] = (byte) ( ( lastTime & 0x00FF000000000000L ) >> 48 & 0xff );
        data[6] = (byte) ( ( lastTime & 0xFF00000000000000L ) >> 56 & 0xff );

        // encode version 2

        data[6] &= 0x0f;
        data[6] |= 0x10;

        long l1 = 0;
        for ( int i = 0; i < 8; i++ )
        {
            l1 = l1 << 8 | data[i] & 0xff;
        }

        // lsb

        long l2 = 0;

        // set clock sequence

        data[1] = (byte) ( sequence & 0x00FF & 0xff );
        data[0] = (byte) ( ( sequence & 0xFF00 ) >> 8 & 0xff );

        // set variant

        data[0] &= 0x3f;
        data[0] |= 0x80;

        for ( int i = 0; i < 2; i++ )
        {
            l2 = l2 << 8 | data[i] & 0xff;
        }

        // append node

        for ( int i = 0; i < 6; i++ )
        {
            l2 = l2 << 8 | node[i] & 0xff;
        }

        return new java.util.UUID ( l1, l2 );
    }

    /**
     * Set the time, increase the clock counter instead
     * if the time did not change
     * 
     * @param time
     *            the time to set
     */
    private static void updateTime ( final long time )
    {
        if ( lastTime >= time )
        {
            sequence++;
            sequence = sequence % MAX_SEQUENCE_NUMBER;
        }
        else
        {
            lastTime = time;
        }
    }

    private static long TIME_DIFF = 0x01B21DD213814000L;

    private static long makeTime ()
    {
        final long time = System.currentTimeMillis () * ( 1000 * 10 );

        // FIXME: we could fill the nanoseconds in that passed since the last call

        return TIME_DIFF + time;
    }
}
