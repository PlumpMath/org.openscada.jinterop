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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;

import ndr.NdrObject;
import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.transport.JIComTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.Endpoint;
import rpc.Stub;

/**
 * Class only used for Oxid ping requests between the Java client and the COM
 * server. This is not for
 * reverse operations i.e COM client and Java server. That is handled at the
 * OxidResolverImpl level in JIComOxidRuntimeHelper,
 * since each of the Oxid Resolver has a separate thread for COM client.
 * 
 * @exclude
 * @since 1.0
 */
final class JIComOxidStub extends Stub
{
    private final static Logger logger = LoggerFactory.getLogger ( JIComOxidStub.class );

    private static Properties defaults = new Properties ();

    static
    {

        defaults.put ( "rpc.ntlm.lanManagerKey", "false" );
        defaults.put ( "rpc.ntlm.sign", "false" );
        defaults.put ( "rpc.ntlm.seal", "false" );
        defaults.put ( "rpc.ntlm.keyExchange", "false" );
        defaults.put ( "rpc.connectionContext", "rpc.security.ntlm.NtlmConnectionContext" );

    }

    @Override
    protected String getSyntax ()
    {
        return "99fcfec4-5260-101b-bbcb-00aa0021347a:0.0";
    }

    public JIComOxidStub ( final String address, final String domain, final String username, final String password )
    {
        super ();
        super.setTransportFactory ( JIComTransportFactory.getSingleTon () );
        super.setProperties ( new Properties ( defaults ) );
        super.getProperties ().setProperty ( "rpc.security.username", username );
        super.getProperties ().setProperty ( "rpc.security.password", password );
        super.getProperties ().setProperty ( "rpc.ntlm.domain", domain );
        super.setAddress ( "ncacn_ip_tcp:" + address + "[135]" );

    }

    public byte[] call ( final boolean isSimplePing, final byte[] setId, final ArrayList listOfAdds, final ArrayList listOfDels, final int seqNum )
    {
        final PingObject pingObject = new PingObject ();
        pingObject.setId = setId;
        pingObject.listOfAdds = listOfAdds;
        pingObject.listOfDels = listOfDels;
        pingObject.seqNum = seqNum;

        if ( isSimplePing )
        {
            pingObject.opnum = 1;
        }
        else
        {
            pingObject.opnum = 2;
        }

        try
        {
            call ( Endpoint.IDEMPOTENT, pingObject );
        }
        catch ( final IOException e )
        {
            logger.warn ( "call", e );
        }

        //returns setId.
        return pingObject.setId;
    }

    public void close ()
    {
        try
        {
            detach ();
        }
        catch ( final Exception e )
        {
            //JISystem.getLogger().throwing("JIComOxidStub","close",e);  
        }
    }

}

class PingObject extends NdrObject
{
    private final static Logger logger = LoggerFactory.getLogger ( PingObject.class );

    int opnum = -1;

    ArrayList listOfAdds = new ArrayList ();

    ArrayList listOfDels = new ArrayList ();

    byte[] setId = null;

    int seqNum = 0;

    @Override
    public int getOpnum ()
    {
        return this.opnum;
    }

    //read follows write...please remember
    @Override
    public void write ( final NetworkDataRepresentation ndr )
    {
        switch ( this.opnum )
        {
            case 2: //complex ping

                final int newlength = 8 + 6 + 8 + this.listOfAdds.size () * 8 + 8 + this.listOfDels.size () * 8 + 16;
                if ( newlength > ndr.getBuffer ().buf.length )
                {
                    ndr.getBuffer ().buf = new byte[newlength + 16];
                }

                if ( this.setId == null )
                {
                    logger.info ( "Complex Ping going for the first time, will get the setId as response of this call" );
                    this.setId = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
                }
                else
                {
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
                    jcifs.util.Hexdump.hexdump ( new PrintStream ( byteArrayOutputStream ), this.setId, 0, this.setId.length );
                    logger.info ( "Complex Ping going for setId: {}", byteArrayOutputStream );
                }

                if ( logger.isInfoEnabled () )
                {
                    logger.info ( "Complex ping going : listOfAdds -> Size : " + this.listOfAdds.size () + " , " + this.listOfAdds );
                    logger.info ( "listOfDels -> Size : " + this.listOfDels.size () + " , " + this.listOfDels );
                }

                JIMarshalUnMarshalHelper.writeOctetArrayLE ( ndr, this.setId );

                JIMarshalUnMarshalHelper.serialize ( ndr, Short.class, new Short ( (short)this.seqNum ), null, JIFlags.FLAG_NULL );//seq
                JIMarshalUnMarshalHelper.serialize ( ndr, Short.class, new Short ( (short)this.listOfAdds.size () ), null, JIFlags.FLAG_NULL );//add
                JIMarshalUnMarshalHelper.serialize ( ndr, Short.class, new Short ( (short)this.listOfDels.size () ), null, JIFlags.FLAG_NULL );//del

                if ( this.listOfAdds.size () > 0 )
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( new Object ().hashCode () ), null, JIFlags.FLAG_NULL );//pointer
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( this.listOfAdds.size () ), null, JIFlags.FLAG_NULL );

                    for ( int i = 0; i < this.listOfAdds.size (); i++ )
                    {
                        final JIObjectId oid = (JIObjectId)this.listOfAdds.get ( i );
                        JIMarshalUnMarshalHelper.writeOctetArrayLE ( ndr, oid.getOID () );
                        //JISystem.getLogger().info("[" + oid.toString() + "]");
                    }
                }
                else
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );//null pointer
                }

                if ( this.listOfDels.size () > 0 )
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( new Object ().hashCode () ), null, JIFlags.FLAG_NULL );//pointer
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( this.listOfDels.size () ), null, JIFlags.FLAG_NULL );

                    //now align for array
                    final double index = new Integer ( ndr.getBuffer ().getIndex () ).doubleValue ();
                    long k = ( k = Math.round ( index % 8.0 ) ) == 0 ? 0 : 8 - k;
                    ndr.writeOctetArray ( new byte[(int)k], 0, (int)k );

                    for ( int i = 0; i < this.listOfDels.size (); i++ )
                    {
                        final JIObjectId oid = (JIObjectId)this.listOfDels.get ( i );
                        JIMarshalUnMarshalHelper.writeOctetArrayLE ( ndr, oid.getOID () );
                        //JISystem.getLogger().info("[" + oid + "]");
                    }
                }
                else
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );//null pointer
                }

                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
                break;

            case 1:// simple ping

                if ( this.setId != null )
                {
                    JIMarshalUnMarshalHelper.writeOctetArrayLE ( ndr, this.setId );//setid
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
                    jcifs.util.Hexdump.hexdump ( new PrintStream ( byteArrayOutputStream ), this.setId, 0, this.setId.length );
                    logger.info ( "Simple Ping going for setId: {}", byteArrayOutputStream );
                }
                else
                {
                    logger.info ( "Some error ! Simple ping requested , but has no setID" );
                }
                break;

            default:
                //nothing.
        }
    }

    @Override
    public void read ( final NetworkDataRepresentation ndr )
    {
        //read response and fill DSs accordingly
        switch ( this.opnum )
        {
            case 2: //complex ping

                this.setId = JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 );
                //ping factor
                JIMarshalUnMarshalHelper.deSerialize ( ndr, Short.class, null, JIFlags.FLAG_NULL, null );

                //hresult
                int hresult = ( (Integer)JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, null, JIFlags.FLAG_NULL, null ) ).intValue ();

                if ( hresult != 0 )
                {
                    logger.error ( "Some error ! Complex ping failed , hresult: {}", hresult );
                }
                else
                {
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
                    jcifs.util.Hexdump.hexdump ( new PrintStream ( byteArrayOutputStream ), this.setId, 0, this.setId.length );
                    logger.info ( "Complex Ping Succeeded,  setId is : {}", byteArrayOutputStream );
                }

                break;
            case 1:// simple ping

                //hresult
                hresult = ( (Integer)JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, null, JIFlags.FLAG_NULL, null ) ).intValue ();

                if ( hresult != 0 )
                {
                    logger.error ( "Some error ! Simple ping failed , hresult: {}", hresult );
                }
                else
                {
                    logger.info ( "Simple Ping Succeeded" );
                }
                break;

            default:
                //nothing.
        }
    }
}
