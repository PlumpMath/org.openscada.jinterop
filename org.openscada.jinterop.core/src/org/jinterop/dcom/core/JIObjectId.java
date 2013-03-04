/**j-Interop (Pure Java implementation of DCOM protocol)
 * Copyright (C) 2006  Vikram Roopchand
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

package org.jinterop.dcom.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JIObjectId implements Serializable
{
    private final static Logger logger = LoggerFactory.getLogger ( JIObjectId.class );

    private static final long serialVersionUID = -4335536047242439700L;

    private final byte[] oid;

    private int refcountofIPID = 0;

    private long lastPingTime = System.currentTimeMillis ();

    final boolean dontping;

    int getIPIDRefCount ()
    {
        return this.refcountofIPID;
    }

    boolean hasExpired ()
    {
        //8 minutes interval...giving COM Client some grace period.
        if ( System.currentTimeMillis () - this.lastPingTime > 8 * 60 * 1000 )
        {
            return true;
        }
        else
        {
            //			lastPingTime = System.currentTimeMillis();
            return false;
        }
    }

    void updateLastPingTime ()
    {
        this.lastPingTime = System.currentTimeMillis ();
    }

    void setIPIDRefCountTo0 ()
    {
        this.refcountofIPID = 0;
    }

    void decrementIPIDRefCountBy1 ()
    {
        this.refcountofIPID--;
    }

    void incrementIPIDRefCountBy1 ()
    {
        this.refcountofIPID++;
    }

    JIObjectId ( final byte[] oid, final boolean dontping )
    {
        this.oid = oid;
        this.dontping = dontping;
        if ( dontping )
        {
            logger.info ( "DONT PING is true for OID: {}", this );
        }
    }

    byte[] getOID ()
    {
        return this.oid;
    }

    @Override
    public int hashCode ()
    {
        int result = 1;
        //from SUN
        for ( int i = 0; i < this.oid.length; i++ )
        {
            result = 31 * result + this.oid[i];
        }
        return result;

        //return Arrays.hashCode(oid);
    }

    @Override
    public boolean equals ( final Object obj )
    {
        if ( ! ( obj instanceof JIObjectId ) )
        {
            return false;
        }

        return Arrays.equals ( this.oid, ( (JIObjectId)obj ).getOID () );
    }

    @Override
    public String toString ()
    {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
        jcifs.util.Hexdump.hexdump ( new PrintStream ( byteArrayOutputStream ), this.oid, 0, this.oid.length );
        return "{ IPID ref count is " + this.refcountofIPID + " } and OID in bytes[] " + byteArrayOutputStream.toString () + " , hasExpired " + hasExpired () + " } ";
    }

    //	void addIpid(String IPID)
    //	{
    //		listOfIpids.add(IPID);
    //	}
    //
    //	List getIpidList()
    //	{
    //		return listOfIpids;
    //	}
}
