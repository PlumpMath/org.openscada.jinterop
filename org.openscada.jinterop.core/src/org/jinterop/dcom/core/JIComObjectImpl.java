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

import java.util.HashMap;
import java.util.Map;

import org.jinterop.dcom.common.IJIUnreferenced;
import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iwombat.foundation.IdentifierFactory;
import com.iwombat.util.GUIDUtil;

/**
 * Implementation for IJIComObject. There is a 1 to 1 mapping between this and a
 * <code>COM</code> interface.
 * 
 * @exclude
 * @since 1.0
 */
final class JIComObjectImpl implements IJIComObject
{
    private final static Logger logger = LoggerFactory.getLogger ( JIComObjectImpl.class );

    private static final long serialVersionUID = -1661750453596032089L;

    private boolean isDual = false;

    private boolean dualInfo = false;

    private transient JISession session = null;

    private JIInterfacePointer ptr = null;

    private Map connectionPointInfo = null;

    private int timeout = 0;

    private final boolean isLocal;

    JIComObjectImpl ( final JISession session, final JIInterfacePointer ptr )
    {
        this ( session, ptr, false );
    }

    JIComObjectImpl ( final JISession session, final JIInterfacePointer ptr, final boolean isLocal )
    {
        this.session = session;
        this.ptr = ptr;
        this.isLocal = isLocal;
    }

    void replaceMembers ( final IJIComObject comObject )
    {
        this.session = comObject.getAssociatedSession ();
        this.ptr = comObject.internal_getInterfacePointer ();
    }

    private void checkLocal ()
    {
        if ( this.session == null )
        {
            throw new IllegalStateException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_SESSION_NOT_ATTACHED ) );
        }

        if ( isLocalReference () )
        {
            throw new IllegalStateException ( JISystem.getLocalizedMessage ( JIErrorCodes.E_NOTIMPL ) );
        }
    }

    @Override
    public IJIComObject queryInterface ( final String iid ) throws JIException
    {
        checkLocal ();
        return this.session.getStub ().getInterface ( iid, this.ptr.getIPID () );
    }

    @Override
    public void addRef () throws JIException
    {
        checkLocal ();
        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setParentIpid ( this.ptr.getIPID () );
        obj.setOpnum ( 1 );//addRef

        //length
        obj.addInParamAsShort ( (short)1, JIFlags.FLAG_NULL );
        //ipid to addfref on
        final JIArray array = new JIArray ( new rpc.core.UUID[] { new rpc.core.UUID ( this.ptr.getIPID () ) }, true );
        obj.addInParamAsArray ( array, JIFlags.FLAG_NULL );
        //TODO requesting 5 for now, will later build caching mechnaism to exhaust 5 refs first before asking for more
        // same with release.
        obj.addInParamAsInt ( 5, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0, JIFlags.FLAG_NULL );//private refs = 0

        obj.addOutParamAsType ( Short.class, JIFlags.FLAG_NULL );//size
        obj.addOutParamAsType ( Integer.class, JIFlags.FLAG_NULL );//Hresult for size
        if ( logger.isInfoEnabled () )
        {
            logger.info ( "addRef: Adding 5 references for " + this.ptr.getIPID () + " session: " + this.session.getSessionIdentifier () );
        }

        JISession.debug_addIpids ( this.ptr.getIPID (), 5 );

        this.session.getStub2 ().addRef_ReleaseRef ( obj );

        if ( obj.getResultAsIntAt ( 1 ) != 0 )
        {
            throw new JIException ( obj.getResultAsIntAt ( 1 ), (Throwable)null );
        }
    }

    @Override
    public void release () throws JIException
    {
        checkLocal ();
        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setParentIpid ( this.ptr.getIPID () );
        obj.setOpnum ( 2 );//release
        //length
        obj.addInParamAsShort ( (short)1, JIFlags.FLAG_NULL );
        //ipid to addfref on
        final JIArray array = new JIArray ( new rpc.core.UUID[] { new rpc.core.UUID ( this.ptr.getIPID () ) }, true );
        obj.addInParamAsArray ( array, JIFlags.FLAG_NULL );
        //TODO requesting 5 for now, will later build caching mechnaism to exhaust 5 refs first before asking for more
        // same with release.
        obj.addInParamAsInt ( 5, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0, JIFlags.FLAG_NULL );//private refs = 0
        if ( logger.isInfoEnabled () )
        {
            logger.info ( "RELEASE called directly ! removing 5 references for " + this.ptr.getIPID () + " session: " + this.session.getSessionIdentifier () );
            JISession.debug_delIpids ( this.ptr.getIPID (), 5 );
        }
        this.session.getStub2 ().addRef_ReleaseRef ( obj );
    }

    @Override
    public Object[] call ( final JICallBuilder obj ) throws JIException
    {
        checkLocal ();
        return call ( obj, this.timeout );
    }

    @Override
    public JIInterfacePointer internal_getInterfacePointer ()
    {
        return this.ptr == null ? this.session.getStub ().getServerInterfacePointer () : this.ptr;
    }

    @Override
    public String getIpid ()
    {
        return this.ptr.getIPID ();
    }

    @Override
    public boolean equals ( final Object obj )
    {

        if ( ! ( obj instanceof JIComObjectImpl ) )
        {
            return false;
        }

        return this.ptr.getIPID ().equalsIgnoreCase ( ( (IJIComObject)obj ).getIpid () );
    }

    @Override
    public int hashCode ()
    {
        return this.ptr.getIPID ().hashCode ();
    }

    @Override
    public JISession getAssociatedSession ()
    {
        return this.session;
    }

    @Override
    public String getInterfaceIdentifier ()
    {
        return this.ptr.getIID ();
    }

    //	public JIComServer getAssociatedComServer()
    //	{
    //		checkLocal();
    //		return session.getStub();
    //	}

    @Override
    public synchronized boolean isDispatchSupported ()
    {
        checkLocal ();
        if ( !this.dualInfo )
        {
            //query interface for it and then release it.
            try
            {
                final IJIComObject comObject = queryInterface ( "00020400-0000-0000-c000-000000000046" );
                comObject.release ();
                setIsDual ( true );
            }
            catch ( final JIException e )
            {
                setIsDual ( false );
            }
        }
        return this.isDual;
    }

    @Override
    public synchronized String internal_setConnectionInfo ( final IJIComObject connectionPoint, final Integer cookie )
    {
        checkLocal ();
        if ( this.connectionPointInfo == null ) //lazy creation, since this is used by event callbacks only.
        {
            this.connectionPointInfo = new HashMap ();
        }
        final String uniqueId = GUIDUtil.guidStringFromHexString ( IdentifierFactory.createUniqueIdentifier ().toHexString () );
        this.connectionPointInfo.put ( uniqueId, new Object[] { connectionPoint, cookie } );
        return uniqueId;
    }

    @Override
    public synchronized Object[] internal_getConnectionInfo ( final String identifier )
    {
        checkLocal ();
        return (Object[])this.connectionPointInfo.get ( identifier );
    }

    @Override
    public synchronized Object[] internal_removeConnectionInfo ( final String identifier )
    {
        checkLocal ();
        return (Object[])this.connectionPointInfo.remove ( identifier );
    }

    @Override
    public IJIUnreferenced getUnreferencedHandler ()
    {
        checkLocal ();
        return this.session.getUnreferencedHandler ( getIpid () );
    }

    @Override
    public void registerUnreferencedHandler ( final IJIUnreferenced unreferenced )
    {
        checkLocal ();
        this.session.registerUnreferencedHandler ( getIpid (), unreferenced );
    }

    @Override
    public void unregisterUnreferencedHandler ()
    {
        checkLocal ();
        this.session.unregisterUnreferencedHandler ( getIpid () );
    }

    @Override
    public Object[] call ( final JICallBuilder obj, final int socketTimeout ) throws JIException
    {
        checkLocal ();
        obj.attachSession ( this.session );
        obj.setParentIpid ( this.ptr.getIPID () );
        //Call is always made on your stub.

        if ( socketTimeout != 0 ) //using instance level timeout
        {
            return this.session.getStub ().call ( obj, this.ptr.getIID (), socketTimeout );
        }
        else
        {
            return this.session.getStub ().call ( obj, this.ptr.getIID () );
        }
    }

    @Override
    public int getInstanceLevelSocketTimeout ()
    {
        checkLocal ();
        return this.timeout;
    }

    @Override
    public void setInstanceLevelSocketTimeout ( final int timeout )
    {
        checkLocal ();
        this.timeout = timeout;
    }

    @Override
    public void internal_setDeffered ( final boolean deffered )
    {
        this.ptr.setDeffered ( deffered );
    }

    @Override
    public boolean isLocalReference ()
    {
        return this.isLocal;
    }

    void setIsDual ( final boolean isDual )
    {
        this.dualInfo = true;
        this.isDual = isDual;
    }

    @Override
    public String toString ()
    {
        return "IJIComObject[" + internal_getInterfacePointer () + " , session: " + getAssociatedSession ().getSessionIdentifier () + ", isLocal: " + isLocalReference () + "]";
    }
}
