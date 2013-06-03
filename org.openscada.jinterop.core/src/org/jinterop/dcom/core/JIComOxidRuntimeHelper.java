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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import ndr.NdrBuffer;
import ndr.NdrException;
import ndr.NdrObject;
import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.common.IJICOMRuntimeWorker;
import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JIRuntimeException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.common.UUIDGenerator;
import org.jinterop.dcom.transport.JIComRuntimeEndpoint;
import org.jinterop.dcom.transport.JIComRuntimeTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.Stub;
import rpc.core.UUID;

/**
 * Used to manipulate Oxid details. one instance is created per binding
 * call to the oxid resolver.
 * 
 * @since 1.0
 */
final class JIComOxidRuntimeHelper extends Stub
{
    private final static Logger logger = LoggerFactory.getLogger ( JIComOxidRuntimeHelper.class );

    JIComOxidRuntimeHelper ( final Properties properties )
    {
        super.setTransportFactory ( JIComRuntimeTransportFactory.getSingleTon () );
        super.setProperties ( properties );
        super.setAddress ( "127.0.0.1[135]" );//this is never consulted so , putting localhost here.
    }

    @Override
    protected String getSyntax ()
    {
        //return "99fcfec4-5260-101b-bbcb-00aa0021347a:0.0";//IOxidResolver IID
        return UUID.NIL_UUID + ":0.0"; //returning nothing
    }

    void startOxid ( final int portNumLocal, final int portNumRemote ) throws IOException
    {
        final Thread oxidResolverThread = new Thread ( new Runnable () {
            @Override
            public void run ()
            {
                try
                {
                    logger.info ( "started startOxid thread: {}", Thread.currentThread ().getName () );
                    attach ();
                    ( (JIComRuntimeEndpoint)getEndpoint () ).processRequests ( new OxidResolverImpl ( getProperties () ), null, new ArrayList () );
                }
                catch ( final Exception e )
                {
                    logger.warn ( "Oxid Resolver Thread/run", e );
                }
                finally
                {
                    try
                    {
                        ( (JIComRuntimeEndpoint)getEndpoint () ).detach ();
                    }
                    catch ( final IOException e )
                    {
                    }
                }
                logger.info ( "terminating startOxid thread: {}", Thread.currentThread ().getName () );
            }
        }, "jI_OxidResolver_Client[" + portNumLocal + " , " + portNumRemote + "]" );
        oxidResolverThread.setDaemon ( true );
        oxidResolverThread.start ();
    }

    //returns the port to which the server is listening.
    Object[] startRemUnknown ( final String baseIID, final String ipidOfRemUnknown, final String ipidOfComponent, final List listOfSupportedInterfaces ) throws IOException
    {
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open ();
        final ServerSocket serverSocket = serverSocketChannel.socket ();//new ServerSocket(0);
        //	    serverSocket.setSoTimeout(120*1000); //2 min timeout.
        serverSocket.bind ( null );
        final int remUnknownPort = serverSocket.getLocalPort ();
        //have to pick up a random name so adding the ipid of remunknown this is a uuid so the string is quite random.
        final ThreadGroup remUnknownForThisListener = new ThreadGroup ( "ThreadGroup - " + baseIID + "[" + ipidOfRemUnknown + "]" );
        remUnknownForThisListener.setDaemon ( true );
        final Thread remUnknownThread = new Thread ( remUnknownForThisListener, new Runnable () {
            @Override
            public void run ()
            {
                logger.info ( "started RemUnknown listener thread for : {}", Thread.currentThread ().getName () );
                try
                {

                    while ( true )
                    {
                        final Socket socket = serverSocket.accept ();
                        logger.info ( "RemUnknown listener: Got Connection from {}", socket.getPort () );

                        //now create the JIComOxidRuntimeHelper Object and start it. We need a new one since the old one is already attached to the listener.
                        final JIComOxidRuntimeHelper remUnknownHelper = new JIComOxidRuntimeHelper ( getProperties () );
                        synchronized ( JIComOxidRuntime.mutex )
                        {
                            JISystem.internal_setSocket ( socket );
                            remUnknownHelper.attach ();
                        }

                        //now start a new thread with this socket 
                        final Thread remUnknown = new Thread ( remUnknownForThisListener, new Runnable () {
                            @Override
                            public void run ()
                            {
                                try
                                {
                                    ( (JIComRuntimeEndpoint)remUnknownHelper.getEndpoint () ).processRequests ( new RemUnknownObject ( ipidOfRemUnknown, ipidOfComponent ), baseIID, listOfSupportedInterfaces );
                                }
                                catch ( final SmbAuthException e )
                                {
                                    logger.warn ( "JIComOxidRuntimeHelper RemUnknownThread (not listener)", e );
                                    throw new JIRuntimeException ( JIErrorCodes.JI_CALLBACK_AUTH_FAILURE );
                                }
                                catch ( final SmbException e )
                                {
                                    logger.warn ( "JIComOxidRuntimeHelper RemUnknownThread (not listener)", e );
                                    throw new JIRuntimeException ( JIErrorCodes.JI_CALLBACK_SMB_FAILURE );
                                }
                                catch ( final ClosedByInterruptException e )
                                {
                                    logger.warn ( "JIComOxidRuntimeHelper RemUnknownThread (not listener)" + Thread.currentThread ().getName () + " is purposefully closed by interruption.", e );
                                }
                                catch ( final IOException e )
                                {
                                    logger.warn ( "JIComOxidRuntimeHelper RemUnknownThread (not listener)", e );
                                }
                                finally
                                {
                                    try
                                    {
                                        remUnknownHelper.detach ();
                                    }
                                    catch ( final IOException e )
                                    {
                                    }
                                }

                            }
                        }, "jI_RemUnknown[" + baseIID + " , L(" + socket.getLocalPort () + "):R(" + socket.getPort () + ")]" );
                        remUnknown.setDaemon ( true );
                        remUnknown.start ();
                    }
                }
                catch ( final ClosedByInterruptException e )
                {
                    logger.info ( "JIComOxidRuntimeHelper RemUnknownListener" + Thread.currentThread ().getName () + " is purposefully closed by interruption.", e );
                }
                catch ( final IOException e )
                {
                    logger.warn ( "JIComOxidRuntimeHelper RemUnknownListener on thread Id: " + Thread.currentThread ().getName (), e );
                }
                catch ( final Throwable e )
                {
                    logger.warn ( "JIComOxidRuntimeHelper RemUnknownListener", e );
                }

                logger.info ( "terminating RemUnknownListener thread: {}", Thread.currentThread ().getName () );
            }
        }, "jI_RemUnknownListener[" + baseIID + " , " + remUnknownPort + "]" );

        remUnknownThread.setDaemon ( true );
        remUnknownThread.start ();
        return new Object[] { new Integer ( remUnknownPort ), remUnknownForThisListener };
    }
}

//This object should have serialized access only , i.e at a time only 1 read --> write , cycle should happen
// it is not multithreaded safe.
class OxidResolverImpl extends NdrObject implements IJICOMRuntimeWorker
{

    private final static Logger logger = LoggerFactory.getLogger ( OxidResolverImpl.class );

    //override read\write\opnum etc. here, use the util apis to decompose this.
    private int opnum = -1;

    private NdrBuffer buffer = null;

    private Properties p = null;

    public OxidResolverImpl ( final Properties p )
    {
        super ();
        this.p = p;
    }

    @Override
    public void setCurrentObjectID ( final UUID objectId )
    {
        //does nothing.
    }

    //	public void setCurrentJavaInstanceFromIID(String iid)
    //	{
    //		//does nothing.
    //	}

    @Override
    public void setOpnum ( final int opnum )
    {
        this.opnum = opnum;
    }

    @Override
    public int getOpnum ()
    {
        return this.opnum;
    }

    @Override
    public void write ( final NetworkDataRepresentation ndr )
    {
        ndr.setBuffer ( this.buffer ); //this buffer is prepared via read.
    }

    @Override
    public void read ( final NetworkDataRepresentation ndr )
    {
        //will read according to the opnum. The setOpnum should have been called before this
        //call.	

        switch ( this.opnum )
        {
            case 1:
                this.buffer = SimplePing ( ndr );
                break;
            case 2:
                this.buffer = ComplexPing ( ndr );
                break;
            case 3: //ServerAlive
                this.buffer = ServerAlive ( ndr );
                break;
            case 5: //This is ServerAlive2
                this.buffer = ServerAlive2 ( ndr );
                break;
            case 4: //This is ResolveOxid2
                this.buffer = ResolveOxid2 ( ndr );
                break;
            default: //should not have arrived here.
                logger.warn ( "Oxid Object: DEFAULTED !!!" );
                throw new JIRuntimeException ( JIErrorCodes.RPC_S_PROCNUM_OUT_OF_RANGE );
        }

    }

    private final Random random = new Random ( System.currentTimeMillis () );

    private NdrBuffer SimplePing ( final NetworkDataRepresentation ndr )
    {
        logger.info ( "Oxid Object: SimplePing" );
        final byte b[] = JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 );//setid
        JIComOxidRuntime.addUpdateSets ( new JISetId ( b ), new ArrayList (), new ArrayList () );
        this.buffer = new NdrBuffer ( new byte[16], 0 );
        this.buffer.enc_ndr_long ( 0 );
        this.buffer.enc_ndr_long ( 0 );
        this.buffer.enc_ndr_long ( 0 );
        this.buffer.enc_ndr_long ( 0 );
        return this.buffer;
    }

    private NdrBuffer ComplexPing ( final NetworkDataRepresentation ndr )
    {
        logger.info ( "Oxid Object: ComplexPing" );
        final byte b[] = JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 );//setid
        JIMarshalUnMarshalHelper.deSerialize ( ndr, Short.class, null, JIFlags.FLAG_NULL, null );//seqId.
        final Short lengthAdds = (Short)JIMarshalUnMarshalHelper.deSerialize ( ndr, Short.class, null, JIFlags.FLAG_NULL, null );//
        final Short lengthDels = (Short)JIMarshalUnMarshalHelper.deSerialize ( ndr, Short.class, null, JIFlags.FLAG_NULL, null );//
        JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, null, JIFlags.FLAG_NULL, null );//

        JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, null, JIFlags.FLAG_NULL, null );//length
        final ArrayList listOfAdds = new ArrayList ();
        for ( int i = 0; i < lengthAdds.intValue (); i++ )
        {
            listOfAdds.add ( new JIObjectId ( JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 ), false ) );
        }

        JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, null, JIFlags.FLAG_NULL, null );//length
        final ArrayList listOfDels = new ArrayList ();
        for ( int i = 0; i < lengthDels.intValue (); i++ )
        {
            listOfDels.add ( new JIObjectId ( JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 ), false ) );
        }

        if ( Arrays.equals ( b, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 } ) )
        {
            this.random.nextBytes ( b );
        }

        JIComOxidRuntime.addUpdateSets ( new JISetId ( b ), listOfAdds, listOfDels );

        this.buffer = new NdrBuffer ( new byte[32], 0 );
        final NetworkDataRepresentation ndr2 = new NetworkDataRepresentation ();
        ndr2.setBuffer ( this.buffer );

        JIMarshalUnMarshalHelper.writeOctetArrayLE ( ndr2, b );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Short.class, new Short ( (short)0 ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );//hresult
        return this.buffer;
    }

    private NdrBuffer ServerAlive ( final NetworkDataRepresentation ndr )
    {
        logger.info ( "Oxid Object: ServerAlive" );
        final byte[] buffer = new byte[32]; //16 + 16=just in case
        final NdrBuffer ndrBuffer = new NdrBuffer ( buffer, 0 );
        ndrBuffer.enc_ndr_long ( 0 );
        ndrBuffer.enc_ndr_long ( 0 );
        ndrBuffer.enc_ndr_long ( 0 );
        ndrBuffer.enc_ndr_long ( 0 );
        return ndrBuffer;
    }

    private NdrBuffer ServerAlive2 ( final NetworkDataRepresentation ndr )
    {
        logger.info ( "Oxid Object: ServerAlive2" );
        //there is no in params for this.
        //only out params

        //want no port information associated with this.
        //		byte[] buffer = new byte[120];
        //		FileInputStream inputStream;
        //		try {
        //			inputStream = new FileInputStream("c:/serveralive2");
        //			inputStream.read(buffer,0,120);
        //		} catch (Exception e) {
        //			// TODO Auto-generated catch block
        //			e.printStackTrace();
        //		}
        //		
        //		NdrBuffer ndrBuffer = new NdrBuffer(buffer,0);

        final JIDualStringArray dualStringArray = new JIDualStringArray ( -1 );

        final byte[] buffer = new byte[dualStringArray.getLength () + 4 /*COMVERSION*/+ 16 /*2 unknown 8 bytes*/+ 16/*just in case*/];
        final NdrBuffer ndrBuffer = new NdrBuffer ( buffer, 0 );

        final NetworkDataRepresentation ndr2 = new NetworkDataRepresentation ();
        ndr2.setBuffer ( ndrBuffer );

        //serialize COMVERSION
        JIMarshalUnMarshalHelper.serialize ( ndr2, Short.class, new Short ( (short)JISystem.getCOMVersion ().getMajorVersion () ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Short.class, new Short ( (short)JISystem.getCOMVersion ().getMinorVersion () ), null, JIFlags.FLAG_NULL );

        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( dualStringArray.getLength () ), null, JIFlags.FLAG_NULL );
        dualStringArray.encode ( ndr2 );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL );
        return ndrBuffer;
    }

    //will prepare a NdrBuffer for reply to this call 
    private NdrBuffer ResolveOxid2 ( final NetworkDataRepresentation ndr )
    {
        logger.info ( "Oxid Object: ResolveOxid2" );
        //System.err.println("VIKRAM: resolve oxid thread Id = " + Thread.currentThread().getId());
        //first read the OXID, then consult the oxid master about it's details.
        final JIOxid oxid = new JIOxid ( JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 ) );

        //now get the RequestedProtoSeq length.
        final int length = ( (Short)JIMarshalUnMarshalHelper.deSerialize ( ndr, Short.class, null, JIFlags.FLAG_NULL, null ) ).intValue ();

        //now for the array.
        final JIArray array = (JIArray)JIMarshalUnMarshalHelper.deSerialize ( ndr, new JIArray ( Short.class, null, 1, true ), null, JIFlags.FLAG_REPRESENTATION_ARRAY, null );

        //now query the Resolver master for this data.
        final JIComOxidDetails details = JIComOxidRuntime.getOxidDetails ( oxid );

        if ( details == null )
        {
            //not found, now throw an JIRuntimeException , so that a FaultPdu could be sent.
            throw new JIRuntimeException ( JIErrorCodes.RPC_E_INVALID_OXID );
        }

        //		byte[] buffer = new byte[424];
        //		FileInputStream inputStream;
        //		try {
        //			inputStream = new FileInputStream("c:/resolveoxid2");
        //			inputStream.read(buffer,0,424);
        //		} catch (Exception e) {
        //			// TODO Auto-generated catch block
        //			e.printStackTrace();
        //		}
        //		
        //		try {
        //			details.getCOMRuntimeHelper().startRemUnknown();
        //		} catch (IOException e) {
        //			// TODO Auto-generated catch block
        //			e.printStackTrace();
        //		}
        //		
        //		NdrBuffer ndrBuffer = new NdrBuffer(buffer,0);
        //		

        //randomly create IPID and send, this is the ipid of the remunknown, we store it with remunknown object
        final UUID uuid = details.getRemUnknownIpid () == null ? new UUID ( UUIDGenerator.generateID () ) : new UUID ( details.getRemUnknownIpid () );

        //create the bindings for this Java Object.
        //this port will go in the new bindings sent to the COM client.
        int port = -1;
        try
        {
            //this is so that repeated calls for Oxid resolution return the same rem unknwon.
            port = details.getPortForRemUnknown ();
            if ( port == -1 )
            {
                final String remunknownipid = uuid.toString ();
                final Object[] portandthread = details.getCOMRuntimeHelper ().startRemUnknown ( details.getIID (), remunknownipid, details.getIpid (), details.getReferent ().getSupportedInterfaces () );
                port = ( (Integer)portandthread[0] ).intValue ();
                details.setRemUnknownThreadGroup ( (ThreadGroup)portandthread[1] );
                details.setRemUnknownIpid ( remunknownipid );
            }
            details.setPortForRemUnknown ( port );
        }
        catch ( final IOException e )
        {

            throw new JIRuntimeException ( JIErrorCodes.E_UNEXPECTED );
        }

        //can support only TCP connections
        //JIDualStringArray.test = true;
        final JIDualStringArray dualStringArray = new JIDualStringArray ( port );

        final Integer authnHint = new Integer ( details.getProtectionLevel () );

        final byte[] buffer = new byte[4 + 4 + dualStringArray.getLength () + 16 + 4 + 2 + 2 + 4 + 16];

        //have all data now prepare the response
        //the response expected here is defines the byte array size.
        final NdrBuffer ndrBuffer = new NdrBuffer ( buffer, 0 );

        final NetworkDataRepresentation ndr2 = new NetworkDataRepresentation ();
        ndr2.setBuffer ( ndrBuffer );

        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( new Object ().hashCode () ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( ( dualStringArray.getLength () - 4 ) / 2 ), null, JIFlags.FLAG_NULL );
        dualStringArray.encode ( ndr2 );

        JIMarshalUnMarshalHelper.serialize ( ndr2, UUID.class, uuid, null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, authnHint, null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Short.class, new Short ( (short)JISystem.getCOMVersion ().getMajorVersion () ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Short.class, new Short ( (short)JISystem.getCOMVersion ().getMinorVersion () ), null, JIFlags.FLAG_NULL );
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( 0 ), null, JIFlags.FLAG_NULL ); //hresult

        return ndrBuffer;
    }

    @Override
    public List getQIedIIDs ()
    {
        return null;
    }

    @Override
    public UUID getCurrentObjectID ()
    {
        return null;
    }

    @Override
    public boolean isResolver ()
    {
        return true;
    }

    @Override
    public void setCurrentIID ( final String iid )
    {
        //does nothing
    }

    @Override
    public boolean workerOver ()
    {
        //oxid resolver gets over when the client connected to it releases socket.
        return false;
    }
}

//This object should have serialized access only , i.e at a time only 1 read --> write , cycle should happen
//it is not multithreaded safe.
class RemUnknownObject extends NdrObject implements IJICOMRuntimeWorker
{
    //override read\write\opnum etc. here, use the util apis to decompose this.
    private int opnum = -1;

    private NdrBuffer buffer = null;

    //component tells you the JILocalCoClass to act on , sent via the AlterContext calls
    //for all Altercontexts with IRemUnknown , this will be null.
    private JILocalCoClass component = null; //will hold the current instance to act on.

    /* the component and object id duo work together. 1 component could export many ipids.
     * 
     */
    //ObjectID tells you the IPID to act on, sent via the Request calls
    private UUID objectId = null;

    //this would be the ipid of this RemUnknownObject
    private final String selfIPID;

    private String currentIID = null;

    private final List listOfIIDsQIed = new ArrayList ();

    RemUnknownObject ( final String ipidOfme, final String ipidOfComponent )
    {
        this.selfIPID = ipidOfme;
        this.mapOfIpidsVsRef.put ( ipidOfComponent.toUpperCase (), new Integer ( 5 ) );
    }

    //this list will get cleared after this call.
    @Override
    public List getQIedIIDs ()
    {
        return this.listOfIIDsQIed;
    }

    @Override
    public boolean isResolver ()
    {
        return false;
    }

    @Override
    public void setOpnum ( final int opnum )
    {
        this.opnum = opnum;
    }

    @Override
    public int getOpnum ()
    {
        return this.opnum;
    }

    @Override
    public void write ( final NetworkDataRepresentation ndr )
    {
        ndr.setBuffer ( this.buffer ); //this buffer is prepared via read.
    }

    private final static Logger logger = LoggerFactory.getLogger ( RemUnknownObject.class );

    private static final JIStruct remInterfaceRef = new JIStruct ();
    static
    {
        try
        {
            remInterfaceRef.addMember ( UUID.class );
            remInterfaceRef.addMember ( Integer.class );
            remInterfaceRef.addMember ( Integer.class );
        }
        catch ( final JIException shouldnothappen )
        {
            logger.warn ( "Static Initialiser", shouldnothappen );
        }
    }

    private static final JIArray remInterfaceRefArray = new JIArray ( remInterfaceRef, null, 1, true );

    private final Map mapOfIpidsVsRef = new HashMap ();

    private boolean workerOver = false;

    @Override
    public void read ( final NetworkDataRepresentation ndr )
    {
        //will read according to the opnum. The setOpnum should have been called before this
        //call.	
        final String ipid = this.objectId.toString ();

        //		if (!mapOfIpidsVsRef.containsKey(ipid.toUpperCase()))
        //		{
        //		    System.out.println(Thread.currentThread() + " -->> " + ipid.toUpperCase());
        //		    //we always give 5 references
        //		    mapOfIpidsVsRef.put(ipid.toUpperCase(),new Integer(5));
        //		}

        //this means the call came for IRemUnknown apis, since selfIpid is null or matches the objectID
        //if (selfIPID == null || selfIPID.equalsIgnoreCase(ipid))
        //		if ("00000131-0000-0000-C000-000000000046".equalsIgnoreCase(currentIID))
        if ( this.selfIPID.equalsIgnoreCase ( ipid ) )
        {
            switch ( this.opnum )
            {
                case 3: //IRemUnknown QI.
                    this.buffer = QueryInterface ( ndr );
                    break;
                case 4: //addref
                    JIOrpcThis.decode ( ndr );
                    int length = ndr.readUnsignedShort ();

                    final int[] retvals = new int[length];
                    JIArray array = (JIArray)JIMarshalUnMarshalHelper.deSerialize ( ndr, remInterfaceRefArray, new ArrayList (), JIFlags.FLAG_REPRESENTATION_ARRAY, new HashMap () );
                    //saving the ipids with there references. considering public + private references together for now.
                    JIStruct[] structs = (JIStruct[])array.getArrayInstance ();
                    for ( int i = 0; i < length; i++ )
                    {
                        final String ipidref = ( (UUID)structs[i].getMember ( 0 ) ).toString ().toUpperCase ();
                        final int publicRefs = ( (Integer)structs[i].getMember ( 1 ) ).intValue ();
                        final int privateRefs = ( (Integer)structs[i].getMember ( 2 ) ).intValue ();

                        if ( !this.mapOfIpidsVsRef.containsKey ( ipidref ) )
                        {
                            //this would be strange, since all the ipids we give should be part of the map already.
                            //have to set 0x80000003 (INVALID ARG here)
                            retvals[i] = 0x80000003;
                            continue;
                        }

                        final int total = ( (Integer)this.mapOfIpidsVsRef.get ( ipidref ) ).intValue () + publicRefs + privateRefs;
                        this.mapOfIpidsVsRef.put ( ipidref, new Integer ( total ) );
                    }

                    //preparing the response
                    this.buffer = new NdrBuffer ( new byte[length * 4 + 16], 0 );
                    NetworkDataRepresentation ndr2 = new NetworkDataRepresentation ();
                    ndr2.setBuffer ( this.buffer );
                    JIOrpcThat.encode ( ndr2 );
                    for ( int i = 0; i < length; i++ )
                    {
                        this.buffer.enc_ndr_long ( retvals[i] );
                    }

                    this.buffer.enc_ndr_long ( 0 );
                    this.buffer.enc_ndr_long ( 0 );

                    break;
                case 5: //release

                    JIOrpcThis.decode ( ndr );
                    length = ndr.readUnsignedShort ();
                    array = (JIArray)JIMarshalUnMarshalHelper.deSerialize ( ndr, remInterfaceRefArray, new ArrayList (), JIFlags.FLAG_REPRESENTATION_ARRAY, new HashMap () );
                    //saving the ipids with there references. considering public + private references together for now.
                    structs = (JIStruct[])array.getArrayInstance ();
                    for ( int i = 0; i < length; i++ )
                    {
                        final String ipidref = ( (UUID)structs[i].getMember ( 0 ) ).toString ().toUpperCase ();
                        final int publicRefs = ( (Integer)structs[i].getMember ( 1 ) ).intValue ();
                        final int privateRefs = ( (Integer)structs[i].getMember ( 2 ) ).intValue ();
                        if ( !this.mapOfIpidsVsRef.containsKey ( ipidref ) )
                        {
                            continue;
                        }

                        final int total = ( (Integer)this.mapOfIpidsVsRef.get ( ipidref ) ).intValue () - publicRefs - privateRefs;
                        if ( total == 0 )
                        {
                            this.mapOfIpidsVsRef.remove ( ipidref );
                        }
                        else
                        {
                            this.mapOfIpidsVsRef.put ( ipidref, new Integer ( total ) );
                        }
                    }

                    //all references to all IPIDs exported are over, this is now done.
                    if ( this.mapOfIpidsVsRef.isEmpty () )
                    {
                        this.workerOver = true;
                    }

                    //I have 1 OID == 1 IPID == 1 java instance.
                    this.buffer = new NdrBuffer ( new byte[32], 0 );
                    ndr2 = new NetworkDataRepresentation ();
                    ndr2.setBuffer ( this.buffer );
                    JIOrpcThat.encode ( ndr2 );
                    this.buffer.enc_ndr_long ( 0 );
                    this.buffer.enc_ndr_long ( 0 );
                    break;
                default:
                    throw new JIRuntimeException ( JIErrorCodes.RPC_S_PROCNUM_OUT_OF_RANGE );
            }
        }
        else
        {
            //now use the objectId , just set in before this call to read. That objectId is the IPID on which the
            //call is being made , and was previously exported during Q.I. The component value was filled during an
            //alter context or bind, again made some calls before.
            if ( this.component == null )
            {
                logger.error ( "JIComOxidRuntimeHelper RemUnknownObject read(): component is null , opnum is " + this.opnum + " , IPID is " + ipid + " , selfIpid is " + this.selfIPID );
            }
            final byte b[] = null;
            Object result = null;
            final NetworkDataRepresentation ndr2 = new NetworkDataRepresentation ();
            int hresult = 0;
            Object[] retArray = null;
            try
            {
                result = this.component.invokeMethod ( ipid, this.opnum, ndr );
            }
            catch ( final JIException e )
            {
                hresult = e.getErrorCode ();
                logger.warn ( "read", e );
            }

            //now if opnum was 6 then this is a dispatch call , so response has to be dispatch response
            //not the normal one.
            if ( this.component.getInterfaceDefinitionFromIPID ( ipid ).isDispInterface () && this.opnum == 6 )
            {
                final Object result2 = result;
                //orpcthat
                //[out] VARIANT * pVarResult,
                //[out] EXCEPINFO * pExcepInfo,
                //[out] UINT * pArgErr,
                //[in, out, size_is(cVarRef)] VARIANTARG * rgVarRef
                result = new Object[4]; //orpcthat gets filled outside
                final JIStruct excepInfo = new JIStruct ();
                try
                {
                    excepInfo.addMember ( new Short ( (short)0 ) );
                    excepInfo.addMember ( new Short ( (short)0 ) );
                    excepInfo.addMember ( new JIString ( "" ) );
                    excepInfo.addMember ( new JIString ( "" ) );
                    excepInfo.addMember ( new JIString ( "" ) );
                    excepInfo.addMember ( new Integer ( 0 ) );
                    excepInfo.addMember ( new JIPointer ( null, true ) );
                    excepInfo.addMember ( new JIPointer ( null, true ) );
                    excepInfo.addMember ( new Integer ( 0 ) );
                }
                catch ( final JIException e )
                {//not expecting any here
                    e.printStackTrace ();
                }

                if ( result2 == null )
                {
                    ( (Object[])result )[0] = JIVariant.EMPTY ();
                }
                else
                {
                    //now check whether the variant is by ref or not.
                    final JIVariant variant = (JIVariant) ( (Object[])result2 )[0];

                    try
                    {
                        if ( variant.isByRefFlagSet () )
                        {
                            //add empty inplace of this.
                            ( (Object[])result )[0] = JIVariant.EMPTY ();
                            //now update the array at the end.
                            ( (Object[])result )[3] = new JIArray ( new JIVariant[] { variant }, true );

                        }
                        else
                        {
                            ( (Object[])result )[0] = ( (Object[])result2 )[0]; //will have only a single index.
                            ( (Object[])result )[3] = new Integer ( 0 ); //Array
                        }
                    }
                    catch ( final JIException e )
                    {
                        throw new JIRuntimeException ( e.getErrorCode () );
                    }
                }

                ( (Object[])result )[1] = excepInfo;

                ( (Object[])result )[2] = new Integer ( 0 ); //argErr is null, for now.

                retArray = (Object[])result;

            }

            this.buffer = new NdrBuffer ( b, 0 );
            ndr2.setBuffer ( this.buffer );

            //JIOrpcThat.encode(ndr2);
            //have to create a call Object, since these return types could be structs , unions etc. having deffered pointers 
            final JICallBuilder callObject = new JICallBuilder ();
            callObject.attachSession ( this.component.getSession () );
            if ( result != null )
            {

                if ( retArray != null )
                {
                    //serialize all members sequentially.
                    for ( int i = 0; i < retArray.length; i++ )
                    {
                        callObject.addInParamAsObject ( retArray[i], JIFlags.FLAG_NULL );
                    }
                }
                else
                {
                    //serialize all members sequentially.
                    for ( int i = 0; i < ( (Object[])result ).length; i++ )
                    {
                        callObject.addInParamAsObject ( ( (Object[])result )[i], JIFlags.FLAG_NULL );
                    }

                }

            }
            callObject.write2 ( ndr2 );
            JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( hresult ), null, JIFlags.FLAG_NULL );

        }

    }

    private NdrBuffer QueryInterface ( final NetworkDataRepresentation ndr )
    {
        //now to decompose all

        logger.trace ( "RemUnknownObject: [QI] Before call terminated listOfIIDsQIed are: {}", this.listOfIIDsQIed );
        JIOrpcThis.decode ( ndr );

        //now get the IPID and export the component with a new IPID and IID. 
        final UUID ipid = new rpc.core.UUID ();
        try
        {
            ipid.decode ( ndr, ndr.getBuffer () );
        }
        catch ( final NdrException e )
        {
            logger.warn ( "QueryInterface", e );
        }

        logger.trace ( "RemUnknownObject: [QI] IPID is {}", ipid );

        //set the JILocalCoClass., the ipid should not be null in this call.
        final JIComOxidDetails details = JIComOxidRuntime.getComponentFromIPID ( ipid.toString () );

        if ( details == null )
        {
            //not found, now throw an JIRuntimeException , so that a FaultPdu could be sent.
            throw new JIRuntimeException ( JIErrorCodes.RPC_E_INVALID_OXID );
        }

        final JILocalCoClass component = details.getReferent ();

        logger.trace ( "RemUnknownObject: [QI] JIJavcCoClass is {}", component.getCoClassIID () );

        ( (Integer)JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, null, JIFlags.FLAG_NULL, null ) ).intValue ();//refs , don't really care about this.

        final int length = ( (Short)JIMarshalUnMarshalHelper.deSerialize ( ndr, Short.class, null, JIFlags.FLAG_NULL, null ) ).intValue ();//length of the requested Interfaces

        final JIArray array = (JIArray)JIMarshalUnMarshalHelper.deSerialize ( ndr, new JIArray ( UUID.class, null, 1, true ), null, JIFlags.FLAG_REPRESENTATION_ARRAY, null );

        //now to build the buffer and export the IIDs with new IPIDs
        final byte[] b = new byte[8 + 4 + 4 + length * ( 4 + 4 + 40 ) + 16];
        final NdrBuffer buffer = new NdrBuffer ( b, 0 );

        //start with response
        final NetworkDataRepresentation ndr2 = new NetworkDataRepresentation ();
        ndr2.setBuffer ( buffer );

        JIOrpcThat.encode ( ndr2 );

        //pointer
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( new Object ().hashCode () ), null, JIFlags.FLAG_NULL );
        //length of array
        JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( length ), null, JIFlags.FLAG_NULL );

        final Object[] arrayOfUUIDs = (Object[])array.getArrayInstance ();

        for ( int i = 0; i < arrayOfUUIDs.length; i++ )
        {
            final UUID iid = (UUID)arrayOfUUIDs[i];
            logger.trace ( "RemUnknownObject: [QI] Array iid[{}] is {}", i, iid );
            //now for each QueryResult
            try
            {
                int hresult = 0;
                String ipid2 = UUIDGenerator.generateID ();
                ;
                if ( !component.isPresent ( iid.toString () ) )
                {
                    hresult = JIErrorCodes.E_NOINTERFACE;
                }
                else
                {
                    String tmpIpid = null;
                    try
                    {
                        tmpIpid = component.getIpidFromIID ( iid.toString () );
                    }
                    catch ( final Exception e )
                    {
                        logger.warn ( "QueryInterface", e );
                    }

                    if ( tmpIpid == null )
                    {
                        logger.trace ( "RemUnknownObject: [QI] tmpIpid is null for iid {}", iid );
                        component.exportInstance ( iid.toString (), ipid2 );
                    }
                    else
                    {
                        logger.trace ( "RemUnknownObject: [QI] tmpIpid is NOT null for iid {} and ipid sent back is {}", iid, ipid2 );
                        ipid2 = tmpIpid;
                    }
                }
                //hresult
                JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( hresult ), null, JIFlags.FLAG_NULL );
                JIMarshalUnMarshalHelper.serialize ( ndr2, Integer.class, new Integer ( 0xCCCCCCCC ), null, JIFlags.FLAG_NULL );

                //now generate the IPID and export a java instance with this.
                final JIStdObjRef objRef = new JIStdObjRef ( ipid2, details.getOxid (), details.getOid () );
                objRef.encode ( ndr2 );

                //add it to the exported Ipids map
                this.mapOfIpidsVsRef.put ( ipid2.toUpperCase (), new Integer ( objRef.getPublicRefs () ) );

                logger.trace ( "RemUnknownObject: [QI] for which the stdObjRef is {}", objRef );
            }
            catch ( final IllegalAccessException e )
            {
                logger.warn ( "QueryInterface", e );
            }
            catch ( final InstantiationException e )
            {
                logger.warn ( "QueryInterface", e );
            }

            final String iidtemp = iid.toString ().toUpperCase () + ":0.0";
            if ( !this.listOfIIDsQIed.contains ( iidtemp ) )
            {
                this.listOfIIDsQIed.add ( iidtemp );
            }
        }

        logger.trace ( "RemUnknownObject: [QI] After call terminated listOfIIDsQIed are: {}", this.listOfIIDsQIed );

        return buffer;
    }

    //for all remunknown methods and calls component is null, alter context for IRemUnknown will make this
    //null.
    //	public void setCurrentJavaInstanceFromIID(String  iid)
    //	{
    //		int i = iid.indexOf(":");
    //		if (i != -1)
    //		{
    //			iid = iid.substring(0,i);
    //		}
    //		this.component = JIComOxidRuntime.getJavaComponentForIID(iid);
    //		if (component == null)
    //		{
    //			objectId = null;
    //		}
    //	}

    @Override
    public void setCurrentObjectID ( final UUID objectId )
    {
        this.objectId = objectId;
        this.component = JIComOxidRuntime.getJavaComponentFromIPID ( objectId.toString () );
    }

    @Override
    public UUID getCurrentObjectID ()
    {
        return this.objectId;
    }

    @Override
    public void setCurrentIID ( final String iid )
    {
        this.currentIID = iid;

    }

    @Override
    public boolean workerOver ()
    {
        return this.workerOver;
    }
}