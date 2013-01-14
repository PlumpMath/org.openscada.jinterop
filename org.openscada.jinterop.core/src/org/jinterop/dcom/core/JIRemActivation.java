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

import java.util.ArrayList;
import java.util.HashMap;

import ndr.NdrException;
import ndr.NdrObject;
import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.common.JIComVersion;
import org.jinterop.dcom.common.JIRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.core.UUID;

final class JIRemActivation extends NdrObject
{

    private final static Logger logger = LoggerFactory.getLogger ( JIRemActivation.class );

    public static final int RPC_C_IMP_LEVEL_IDENTIFY = 2;

    public static final int RPC_C_IMP_LEVEL_IMPERSONATE = 3;

    private int impersonationLevel = RPC_C_IMP_LEVEL_IMPERSONATE;

    private int mode = 0;

    private String monikerName = null;

    private UUID clsid = null;

    private boolean activationSuccessful = false;

    private JIOrpcThat orpcthat = null;

    private byte[] oxid = null;

    private JIDualStringArray dualStringArrayForOxid = null;

    private String ipid = null;

    private int authenticationHint = -1;

    private JIComVersion comVersion = null;

    private int hresult = -1;

    private JIInterfacePointer mInterfacePointer = null;

    boolean isDual = false;

    String dispIpid = null;

    int dispRefs = 5;

    byte[] dispOid = null;

    public JIRemActivation ( final String clsid )
    {
        //10000002-0000-0000-0000-000000000001 Inside DCOM
        this.clsid = new UUID ( clsid );
    }

    public void setMode ( final int mode )
    {
        this.mode = mode;
    }

    public void setClientImpersonationLevel ( final int implLevel )
    {
        this.impersonationLevel = implLevel;
    }

    public void setfileMonikerAtServer ( final String name )
    {
        if ( name != null && !name.equalsIgnoreCase ( "" ) )
        {
            this.monikerName = name;
        }
    }

    @Override
    public int getOpnum ()
    {
        return 0;
    }

    @Override
    public void write ( final NetworkDataRepresentation ndr )
    {

        final JIOrpcThis orpcThis = new JIOrpcThis ();
        orpcThis.encode ( ndr );

        //JIClsid of the component being activated.
        final UUID uuid = new UUID ();
        uuid.parse ( this.clsid.toString () );
        try
        {
            uuid.encode ( ndr, ndr.buf );
        }
        catch ( final NdrException e )
        {
            logger.warn ( "write", e );
        }
        if ( this.monikerName == null )
        {
            ndr.writeUnsignedLong ( 0 );
        }
        else
        {
            ndr.writeCharacterArray ( this.monikerName.toCharArray (), 0, this.monikerName.length () ); // Object Name
        }

        ndr.writeUnsignedLong ( 0 ); // Minterface pointer
        ndr.writeUnsignedLong ( this.impersonationLevel ); // impersonation level
        ndr.writeUnsignedLong ( this.mode ); //mode, when object name , interface pointer are not null , this is passed directly to IPersistFile:Load

        ndr.writeUnsignedLong ( 2 ); //No. of IIDs requested.

        ndr.writeUnsignedLong ( new Object ().hashCode () );

        ndr.writeUnsignedLong ( 2 ); //Array length

        //IID of IUnknown , this is hard coded here, standard way of COM is to first get a handle to the IUnknown
        uuid.parse ( "00000000-0000-0000-c000-000000000046" );
        try
        {
            uuid.encode ( ndr, ndr.buf );
        }
        catch ( final NdrException e )
        {
            logger.warn ( "write", e );
        }

        //checking for IDispatch support
        uuid.parse ( "00020400-0000-0000-c000-000000000046" );
        try
        {
            uuid.encode ( ndr, ndr.buf );
        }
        catch ( final NdrException e )
        {
            logger.warn ( "write", e );
        }

        ndr.writeUnsignedLong ( 1 ); //Protocol Sequences available
        ndr.writeUnsignedLong ( 1 ); //Array length
        ndr.writeUnsignedShort ( 7 ); //TCP

        final byte[] address = JISession.getLocalhostAddressAsIPbytes ();

        ndr.writeUnsignedShort ( address[0] );
        ndr.writeUnsignedShort ( address[1] );
        ndr.writeUnsignedShort ( address[2] );
        ndr.writeUnsignedShort ( address[3] );
        ndr.writeUnsignedShort ( 0 );
    }

    @Override
    public void read ( final NetworkDataRepresentation ndr )
    {

        //first take out JIOrpcThat
        this.orpcthat = JIOrpcThat.decode ( ndr );

        //now fill the oxid
        this.oxid = JIMarshalUnMarshalHelper.readOctetArrayLE ( ndr, 8 );

        final int skipdual = ndr.readUnsignedLong ();

        if ( skipdual != 0 )
        {
            ndr.readUnsignedLong ();
            //now fill the dual string array for oxid bindings, the call to IRemUnknown will be
            //directed to this address and the port in that address.
            this.dualStringArrayForOxid = JIDualStringArray.decode ( ndr );
        }
        //get the IPID which will be the "Object" in the call to IRemUknown. This is the IPID of the
        //component which has been specified as the JIClsid. This may differ in multiple invokations of
        //of remote activation as everytime a new object may be created at the server per call. This is all
        //server implementation dependent.
        try
        {
            final UUID ipid2 = new UUID ();
            ipid2.decode ( ndr, ndr.getBuffer () );
            this.ipid = ipid2.toString ();
        }
        catch ( final NdrException e )
        {
            logger.warn ( "read", e );
        }

        //read the auth hint
        this.authenticationHint = ndr.readUnsignedLong ();

        this.comVersion = new JIComVersion ();
        this.comVersion.setMajorVersion ( ndr.readUnsignedShort () );
        this.comVersion.setMinorVersion ( ndr.readUnsignedShort () );

        this.hresult = ndr.readUnsignedLong ();

        if ( this.hresult != 0 )
        {
            //System.out.println("EXCEPTION FROM SERVER ! --> " + "0x" + Long.toHexString(hresult).substring(8));
            throw new JIRuntimeException ( this.hresult );
        }

        //int numRet = ndr.readUnsignedLong();//Number of interface pointers returned. Currently only 2.

        JIArray array = new JIArray ( JIInterfacePointer.class, null, 1, true );
        final ArrayList listOfDefferedPointers = new ArrayList ();
        array = (JIArray)JIMarshalUnMarshalHelper.deSerialize ( ndr, array, listOfDefferedPointers, JIFlags.FLAG_NULL, new HashMap () );
        int x = 0;

        while ( x < listOfDefferedPointers.size () )
        {

            final ArrayList newList = new ArrayList ();
            final JIPointer replacement = (JIPointer)JIMarshalUnMarshalHelper.deSerialize ( ndr, listOfDefferedPointers.get ( x ), newList, JIFlags.FLAG_NULL, null );
            ( (JIPointer)listOfDefferedPointers.get ( x ) ).replaceSelfWithNewPointer ( replacement ); //this should replace the value in the original place.
            x++;
            listOfDefferedPointers.addAll ( x, newList );
        }
        final JIInterfacePointer[] arrayObjs = (JIInterfacePointer[])array.getArrayInstance ();
        this.mInterfacePointer = arrayObjs[0];

        if ( arrayObjs[1] != null )
        {
            //dual is supported since the IDispatch was obtained
            this.isDual = true;
            //eat this keeping only the IPID for cleanup , let the user perform another queryInterface for this.
            final JIInterfacePointer ptr = arrayObjs[1];
            this.dispIpid = ptr.getIPID ();
            this.dispOid = ptr.getOID ();
            this.dispRefs = ( (JIStdObjRef)ptr.getObjectReference ( JIInterfacePointer.OBJREF_STANDARD ) ).getPublicRefs ();
        }

        array = new JIArray ( Integer.class, null, 1, true );
        //ignore the retvals
        JIMarshalUnMarshalHelper.deSerialize ( ndr, array, null, JIFlags.FLAG_NULL, null );

        this.activationSuccessful = true;

    }

    public boolean isActivationSuccessful ()
    {
        return this.activationSuccessful;
    }

    public JIOrpcThat getORPCThat ()
    {
        return this.orpcthat;
    }

    public byte[] getOxid ()
    {
        return this.oxid;
    }

    public JIDualStringArray getDualStringArrayForOxid ()
    {
        return this.dualStringArrayForOxid;
    }

    public int getAuthenticationHint ()
    {
        return this.authenticationHint;
    }

    public JIComVersion getComVersion ()
    {
        return this.comVersion;
    }

    public int getHresult ()
    {
        return this.hresult;
    }

    public JIInterfacePointer getMInterfacePointer ()
    {
        return this.mInterfacePointer;
    }

    public String getIPID ()
    {
        return this.ipid;
    }
}
