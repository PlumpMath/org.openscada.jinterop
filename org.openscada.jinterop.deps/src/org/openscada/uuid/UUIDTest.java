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

public class UUIDTest
{

    public static java.util.UUID createDCE ()
    {
        return UUIDGenerator.generateID ();
    }

    public static void main ( final String[] args )
    {
        /*
        final UUID u1 = UUIDFactory.createUUID ();
        System.out.println ( "UUID: " + u1 );
        System.out.println ( "GUID: " + GUIDUtil.guidStringFromHexString ( u1.toHexString () ) );

        final java.util.UUID u2 = java.util.UUID.fromString ( GUIDUtil.guidStringFromHexString ( u1.toHexString () ) );
        System.out.println ( "UUID: " + u2 );
        System.out.println ( "Version: " + u2.version () + " - Variant: " + u2.variant () );
        
        final java.util.UUID u3 = u1.toJavaUUID ();
        System.out.println ( "UUID: " + u3 );
        */

        for ( int i = 0; i < 5; i++ )
        {
            /*
             * final java.util.UUID uu1 = UUIDFactory.createJavaUUID ();
             * dumpUUID ( "GUID", uu1 );
            */
            final java.util.UUID uu = createDCE ();
            dumpUUID ( "UUID", uu );
            System.out.println ();
        }

        final long l = 0x01B21DD213814000L;
        final long n = System.currentTimeMillis () * ( 1000 * 10 );

        System.out.println ( l );
        System.out.println ( n );
        System.out.println ( l + n );
    }

    public static void dumpUUID ( final String prefix, final java.util.UUID uuid )
    {
        System.out.print ( prefix );
        System.out.print ( ": " );
        System.out.print ( uuid );
        System.out.print ( " VV: " + uuid.version () + "/" + uuid.variant () );

        if ( uuid.variant () == 1 )
        {
            System.err.print ( " Timestamp: " + uuid.timestamp () );
        }
        System.out.println ();
    }
}
