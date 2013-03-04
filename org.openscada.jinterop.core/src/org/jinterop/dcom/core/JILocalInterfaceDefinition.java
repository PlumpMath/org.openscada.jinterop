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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JISystem;

/**
 * <p>
 * Forms the definition of a COM interface to be used in callbacks. Method
 * overloads are <b>not</b> allowed.
 * <p>
 * <i>Please refer to MSInternetExplorer, Test_ITestServer2_Impl,
 * SampleTestServer and MSShell examples for more details on how to use this
 * class.</i><br>
 * 
 * @since 2.0 (formerly JIInterfaceDefinition)
 */
public final class JILocalInterfaceDefinition implements Serializable
{
    private static final long serialVersionUID = 7683984211902254797L;

    private String interfaceIdentifier = null;

    private final Map opnumVsMethodInfo = new HashMap ();

    private final Map dispIdVsMethodInfo = new HashMap ();

    private final Map nameVsMethodInfo = new HashMap ();

    private int nextNum = 0;

    Object instance = null;

    Class clazz = null;

    private boolean dispInterface = true;

    /**
     * Creates an Interface definition. By default, the
     * <code>dispinterface</code> property is <code>true</code>.
     * 
     * @param interfaceIdentifier
     *            <code>IID</code> of the COM interface being implemented.
     */
    public JILocalInterfaceDefinition ( final String interfaceIdentifier )
    {
        this.interfaceIdentifier = interfaceIdentifier;
    }

    /**
     * Creates an Interface definition. Set <code>isDispInterface</code>
     * interface to <code>false</code> if this interface does not support
     * <code>IDispatch</code> based calls.
     * 
     * @param interfaceIdentifier
     *            <code>IID</code> of the COM interface being implemented.
     * @param isDispInterface
     *            <code>true</code> if <code>IDispatch</code> ("
     *            <code>dispinterface</code>")
     *            is supported , <code>false</code> otherwise.
     */
    public JILocalInterfaceDefinition ( final String interfaceIdentifier, final boolean isDispInterface )
    {
        this.interfaceIdentifier = interfaceIdentifier;
        this.dispInterface = isDispInterface;
    }

    /**
     * Adds a Method Descriptor. Methods <b>must</b> be added in the same order
     * as they appear in the IDL.
     * <p>
     * Please note that overloaded methods are not allowed.
     * 
     * @param methodDescriptor
     * @throws IllegalArgumentException
     *             if a method by the same name already exists.
     */
    public void addMethodDescriptor ( final JILocalMethodDescriptor methodDescriptor )
    {
        if ( this.nameVsMethodInfo.containsKey ( methodDescriptor.getMethodName () ) )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_CALLBACK_OVERLOADS_NOTALLOWED ) );
        }

        methodDescriptor.setMethodNum ( this.nextNum );
        this.nextNum++;

        this.opnumVsMethodInfo.put ( new Integer ( methodDescriptor.getMethodNum () ), methodDescriptor );
        if ( this.dispInterface )
        {
            if ( methodDescriptor.getMethodDispID () == -1 )
            {
                throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_METHODDESC_DISPID_MISSING ) );
            }
            this.dispIdVsMethodInfo.put ( new Integer ( methodDescriptor.getMethodDispID () ), methodDescriptor );
        }

        this.nameVsMethodInfo.put ( methodDescriptor.getMethodName (), methodDescriptor );

    }

    /**
     * Returns the method descriptor identified by it's number. <br>
     * 
     * @param opnum
     * @return <code>null</code> if no method by this <code>opnum</code> was
     *         found.
     */
    public JILocalMethodDescriptor getMethodDescriptor ( final int opnum )
    {
        return (JILocalMethodDescriptor)this.opnumVsMethodInfo.get ( new Integer ( opnum ) );
    }

    /**
     * Returns the method descriptor identified by it's dispId. <br>
     * 
     * @param dispId
     * @return <code>null</code> if no method by this <code>dispId</code> was
     *         found.
     */
    public JILocalMethodDescriptor getMethodDescriptorForDispId ( final int dispId )
    {
        return (JILocalMethodDescriptor)this.dispIdVsMethodInfo.get ( new Integer ( dispId ) );
    }

    /**
     * Returns the method descriptor identified by it's name. <br>
     * 
     * @param name
     * @return <code>null</code> if no method by this <code>name</code> was
     *         found.
     */
    public JILocalMethodDescriptor getMethodDescriptor ( final String name )
    {
        return (JILocalMethodDescriptor)this.nameVsMethodInfo.get ( name );
    }

    /**
     * Returns all method descriptors. <br>
     * 
     * @return
     */
    public JILocalMethodDescriptor[] getMethodDescriptors ()
    {
        return (JILocalMethodDescriptor[])this.opnumVsMethodInfo.values ().toArray ( new JILocalMethodDescriptor[this.opnumVsMethodInfo.values ().size ()] );
    }

    /**
     * Returns the interface identifier (<code>IID</code>) of this definition. <br>
     * 
     * @return
     */
    public String getInterfaceIdentifier ()
    {
        return this.interfaceIdentifier;
    }

    /**
     * Removes the method descriptor identified by it's number.
     * <p>
     * Please note that removal of a sequential method can have unpredictable
     * results during a call. <br>
     * 
     * @param opnum
     * @see #addMethodDescriptor(JILocalMethodDescriptor)
     */
    public void removeMethodDescriptor ( final int opnum )
    {
        final JILocalMethodDescriptor methodDescriptor = (JILocalMethodDescriptor)this.opnumVsMethodInfo.remove ( new Integer ( opnum ) );
        if ( methodDescriptor != null )
        {
            this.nameVsMethodInfo.remove ( methodDescriptor.getMethodName () );
        }
    }

    /**
     * Removes the method descriptor identified by it's name.
     * <p>
     * <p>
     * Please note that removal of a sequential method can have unpredictable
     * results during a call. <br>
     * 
     * @param methodName
     * @see #addMethodDescriptor(JILocalMethodDescriptor)
     */
    public void removeMethodDescriptor ( final String methodName )
    {
        final JILocalMethodDescriptor methodDescriptor = (JILocalMethodDescriptor)this.nameVsMethodInfo.remove ( methodName );
        if ( methodDescriptor != null )
        {
            this.nameVsMethodInfo.remove ( new Integer ( methodDescriptor.getMethodNum () ) );
        }
    }

    /**
     * Returns status whether this interface supports <code>IDispatch</code> or
     * not.
     * 
     * @return <code>true</code> if <code>IDispatch</code> is supported.
     */
    public boolean isDispInterface ()
    {
        return this.dispInterface;
    }
}
