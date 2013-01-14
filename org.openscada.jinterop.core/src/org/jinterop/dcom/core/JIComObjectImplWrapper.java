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

import org.jinterop.dcom.common.IJIUnreferenced;
import org.jinterop.dcom.common.JIException;

/**
 * Internal Framework class.
 * 
 * @exclude
 * @since 1.0
 **/
public class JIComObjectImplWrapper implements IJIComObject
{

    /**
	 * 
	 */
    private static final long serialVersionUID = 6142976024482507753L;

    protected final IJIComObject comObject;

    protected JIComObjectImplWrapper ( final IJIComObject comObject )
    {
        this.comObject = comObject;
    }

    @Override
    public IJIComObject queryInterface ( final String iid ) throws JIException
    {

        return this.comObject.queryInterface ( iid );
    }

    @Override
    public void addRef () throws JIException
    {
        this.comObject.addRef ();
    }

    @Override
    public void release () throws JIException
    {
        this.comObject.release ();
    }

    @Override
    public String getIpid ()
    {
        return this.comObject.getIpid ();
    }

    @Override
    public Object[] call ( final JICallBuilder obj ) throws JIException
    {
        return this.comObject.call ( obj );
    }

    @Override
    public JIInterfacePointer internal_getInterfacePointer ()
    {
        return this.comObject.internal_getInterfacePointer ();
    }

    @Override
    public JISession getAssociatedSession ()
    {
        return this.comObject.getAssociatedSession ();
    }

    /**
     * Returns the <i>IID</i> of this object
     * 
     * @return String representation of 128 bit uuid.
     */
    @Override
    public String getInterfaceIdentifier ()
    {
        return this.comObject.getInterfaceIdentifier ();
    }

    //	/**
    //	 * @exclude
    //	 */
    //	public JIComServer getAssociatedComServer()
    //	{
    //		return comObject.getAssociatedComServer();
    //	}

    @Override
    public boolean isDispatchSupported ()
    {
        return this.comObject.isDispatchSupported ();
    }

    @Override
    public String internal_setConnectionInfo ( final IJIComObject connectionPoint, final Integer cookie )
    {
        return this.comObject.internal_setConnectionInfo ( connectionPoint, cookie );
    }

    @Override
    public Object[] internal_getConnectionInfo ( final String identifier )
    {
        return this.comObject.internal_getConnectionInfo ( identifier );
    }

    @Override
    public Object[] internal_removeConnectionInfo ( final String identifier )
    {
        return this.comObject.internal_removeConnectionInfo ( identifier );
    }

    @Override
    public IJIUnreferenced getUnreferencedHandler ()
    {
        return this.comObject.getUnreferencedHandler ();
    }

    @Override
    public void registerUnreferencedHandler ( final IJIUnreferenced unreferenced )
    {
        this.comObject.registerUnreferencedHandler ( unreferenced );
    }

    @Override
    public void unregisterUnreferencedHandler ()
    {
        this.comObject.unregisterUnreferencedHandler ();
    }

    @Override
    public Object[] call ( final JICallBuilder obj, final int timeout ) throws JIException
    {
        return this.comObject.call ( obj, timeout );
    }

    @Override
    public int getInstanceLevelSocketTimeout ()
    {
        return this.comObject.getInstanceLevelSocketTimeout ();
    }

    @Override
    public void setInstanceLevelSocketTimeout ( final int timeout )
    {
        this.comObject.setInstanceLevelSocketTimeout ( timeout );
    }

    @Override
    public void internal_setDeffered ( final boolean deffered )
    {
        this.comObject.internal_setDeffered ( deffered );
    }

    @Override
    public boolean isLocalReference ()
    {
        return this.comObject.isLocalReference ();
    }

    @Override
    public String toString ()
    {
        return this.comObject.toString ();
    }
}
