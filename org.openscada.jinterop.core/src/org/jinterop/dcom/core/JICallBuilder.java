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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ndr.NdrObject;
import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JIRuntimeException;
import org.jinterop.dcom.common.JISystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.core.UUID;

/**
 * <p>
 * Class used for setting up information such as <code>[in]</code> ,
 * <code>[out]</code> parameters and the method number for executing a call to
 * the COM server.
 * <p>
 * Sample Usage :- <code>
 *  <br>
 *  JICallBuilder obj = new JICallBuilder(); <br>
 *  obj.reInit(); <br>
 * 	obj.setOpnum(0); //0 based index, can be obtained from the IDL or the Type Library of COM server.
 * 	<br>
 * 	obj.addInParamAsString(new JIString("j-Interop Rocks !"), JIFlags.FLAG_NULL); <br>
 * 	obj.addInParamAsInt(100, JIFlags.FLAG_NULL); <br>
 * 	//handle is previously obtained {@link IJIComObject} <br>
 * 	Object[] result = comObject.call(obj); 
 * <br>
 * </code> <br>
 * <code>[out]</code> parameters can be added in a similar way.<br>
 * <code>
 *  obj.addOutParamAsType(JIVariant.class,JIFlags.FLAG_NULL); <br>
 *  obj.addOutParamAsObject(new JIPointer(Short.class,true),JIFlags.FLAG_NULL); <br>
 * </code> <br>
 * </p>
 * 
 * @since 2.0 (formerly <code>JICallObject</code>)
 */
public class JICallBuilder extends NdrObject implements Serializable
{
    private final static Logger logger = LoggerFactory.getLogger ( JICallBuilder.class );

    static final String CURRENTSESSION = "CURRENTSESSION";

    static final String COMOBJECTS = "COMOBJECTS";

    private static final long serialVersionUID = -2939657500731135110L;

    private int opnum = -1;

    private Object[] outparams = null;

    private boolean dispatchNotSupported = false;

    private String enclosingParentsIPID = null;

    private ArrayList inparamFlags = new ArrayList ();

    private ArrayList outparamFlags = new ArrayList ();

    private ArrayList inParams = new ArrayList ();

    private ArrayList outParams = new ArrayList ();

    private int hresult = 0;

    private boolean executed = false;

    private Object[] resultsOfException = null;

    private JISession session = null;

    boolean fromDestroySession = false;

    /**
     * Constructs a builder object.
     * 
     * @param dispatchNotSupported
     *            <code>true</code> if <code>IDispatch</code> is
     *            not supported by the <code>IJIComObject</code> on which this
     *            builder would
     *            act. Use {@link IJIComObject#isDispatchSupported()} to find
     *            out if
     *            dispatch is supported on the COM Object.
     */
    public JICallBuilder ( final boolean dispatchNotSupported )
    {
        this ();
        this.dispatchNotSupported = dispatchNotSupported;
    }

    /**
     * <p>
     * Constructs a builder object. It is assumed that <code>IDispatch</code>
     * interface is supported by the <code>IJIComObject</code> on which this
     * builder would act.
     */
    public JICallBuilder ()
    {
        //		enclosingParentsIPID = IPIDofParent;
    }

    /**
     * Reinitializes all members of this object. It is ready to be used again on
     * a
     * fresh <code>{@link IJIComObject#call}</code> after this step.
     */
    //after reinit, except parent, nothing is available.
    public void reInit ()
    {
        this.opnum = -1;
        this.inParams = new ArrayList ();
        this.inparamFlags = new ArrayList ();
        this.outParams = new ArrayList ();
        this.outparamFlags = new ArrayList ();
        this.hresult = -1;
        this.outparams = null;
        this.executed = false;
    }

    void setParentIpid ( final String IPIDofParent )
    {
        this.enclosingParentsIPID = IPIDofParent;
    }

    String getParentIpid ()
    {
        return this.enclosingParentsIPID;
    }

    //	/**Add IN parameter as <code>JIInterfacePointer</code> at the end of the Parameter list.
    //	 * 
    //	 * @param value
    //	 * @param FLAGS from JIFlags (if need be)
    //	 */
    //	public void addInParamAsInterfacePointer(JIInterfacePointer interfacePointer, int FLAGS)
    //	{
    //		insertInParamAsInterfacePointerAt(inParams.size(),interfacePointer,FLAGS);
    //	}

    /**
     * Add <code>[in]</code> parameter as <code>IJIComObject</code> at the end
     * of the Parameter list.
     * 
     * @param comObject
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsComObject ( final IJIComObject comObject, final int FLAGS )
    {
        insertInParamAsComObjectAt ( this.inParams.size (), comObject, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>int</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsInt ( final int value, final int FLAGS )
    {
        insertInParamAsIntAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>IJIUnsigned</code> at the end of
     * the Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsUnsigned ( final IJIUnsigned value, final int FLAGS )
    {
        insertInParamAsUnsignedAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>float</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsFloat ( final float value, final int FLAGS )
    {
        insertInParamAsFloatAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>boolean</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsBoolean ( final boolean value, final int FLAGS )
    {
        insertInParamAsBooleanAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>short</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsShort ( final short value, final int FLAGS )
    {
        insertInParamAsShortAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>double</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsDouble ( final double value, final int FLAGS )
    {
        insertInParamAsDoubleAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>char</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be)
     */
    public void addInParamAsCharacter ( final char value, final int FLAGS )
    {
        insertInParamAsCharacterAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>String</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (These <i>HAVE</i> to be the <b>String</b>
     *            Flags).
     */
    //flags have to be String flags
    public void addInParamAsString ( final String value, final int FLAGS )
    {
        insertInParamAsStringAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIVariant</code> at the end of
     * the Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void addInParamAsVariant ( final JIVariant value, final int FLAGS )
    {
        insertInParamAsVariantAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>Object</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void addInParamAsObject ( final Object value, final int FLAGS )
    {
        insertInParamAsObjectAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as
     * <code>String representation of UUID</code> at the end of the Parameter
     * list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void addInParamAsUUID ( final String value, final int FLAGS )
    {
        insertInParamAsUUIDAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIPointer</code> at the end of
     * the Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void addInParamAsPointer ( final JIPointer value, final int FLAGS )
    {
        insertInParamAsPointerAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIStruct</code> at the end of
     * the Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void addInParamAsStruct ( final JIStruct value, final int FLAGS )
    {
        insertInParamAsStructAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIArray</code> at the end of the
     * Parameter list.
     * 
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void addInParamAsArray ( final JIArray value, final int FLAGS )
    {
        insertInParamAsArrayAt ( this.inParams.size (), value, FLAGS );
    }

    /**
     * Add <code>[in]</code> parameter as <code>Object[]</code> at the end of
     * the Parameter list.The array is iterated and
     * all members appended to the list.
     * 
     * @param values
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void setInParams ( final Object[] values, final int FLAGS )
    {
        for ( int i = 0; i < values.length; i++ )
        {
            this.inParams.add ( values[i] );
            this.inparamFlags.add ( new Integer ( FLAGS ) ); //quite useless but do not want to change logic elsewhere
        }

    }

    /**
     * Add <code>[in]</code> parameter as <code>IJIComObject</code> at the
     * specified index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsComObjectAt ( final int index, final IJIComObject value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>int</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsIntAt ( final int index, final int value, final int FLAGS )
    {
        this.inParams.add ( index, new Integer ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>IJIUnsigned</code> at the
     * specified index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsUnsignedAt ( final int index, final IJIUnsigned value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>float</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsFloatAt ( final int index, final float value, final int FLAGS )
    {
        this.inParams.add ( index, new Float ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>boolean</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsBooleanAt ( final int index, final boolean value, final int FLAGS )
    {
        this.inParams.add ( index, Boolean.valueOf ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>short</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsShortAt ( final int index, final short value, final int FLAGS )
    {
        this.inParams.add ( index, new Short ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>double</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsDoubleAt ( final int index, final double value, final int FLAGS )
    {
        this.inParams.add ( index, new Double ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>char</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsCharacterAt ( final int index, final char value, final int FLAGS )
    {
        this.inParams.add ( index, new Character ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>String</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (These <i>HAVE</i> to be the <b>String</b>
     *            Flags).
     */
    //flags have to be String flags
    public void insertInParamAsStringAt ( final int index, final String value, final int FLAGS )
    {
        this.inParams.add ( index, new JIString ( value, FLAGS ) );
        this.inparamFlags.add ( index, new Integer ( JIFlags.FLAG_NULL ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIVariant</code> at the
     * specified index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsVariantAt ( final int index, final JIVariant value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( JIFlags.FLAG_NULL ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>Object</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    //this is for dispatch, etc...more or less will never be used.
    public void insertInParamAsObjectAt ( final int index, final Object value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as
     * <code>String representation of UUID</code> at the specified index in the
     * Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsUUIDAt ( final int index, final String value, final int FLAGS )
    {
        this.inParams.add ( index, new rpc.core.UUID ( value ) );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIPointer</code> at the
     * specified index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsPointerAt ( final int index, final JIPointer value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIStruct</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsStructAt ( final int index, final JIStruct value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Add <code>[in]</code> parameter as <code>JIArray</code> at the specified
     * index in the Parameter list.
     * 
     * @param index
     *            0 based index
     * @param value
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void insertInParamAsArrayAt ( final int index, final JIArray value, final int FLAGS )
    {
        this.inParams.add ( index, value );
        this.inparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Removes <code>[in]</code> parameter at the specified index from the
     * Parameter list.
     * 
     * @param index
     *            0 based index
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void removeInParamAt ( final int index, final int FLAGS )
    {
        final Object value = this.inParams.remove ( index );
        this.inparamFlags.remove ( index );
    }

    /**
     * Returns <code>[in]</code> parameter at the specified index from the
     * Parameter list.
     * 
     * @param index
     *            0 based index
     * @return Primitives are returned as there Derieved types.
     */
    //Will just provide 1 getter, for outParams there would be overloads like inParam setters.
    public Object getInParamAt ( final int index )
    {
        return this.inParams.get ( index );
    }

    /**
     * Add <code>[out]</code> parameter of the type <code>clazz</code> at the
     * end of the out parameter list.
     * 
     * @param clazz
     * @param FLAGS
     */
    public void addOutParamAsType ( final Class clazz, final int FLAGS )
    {
        insertOutParamAt ( this.outParams.size (), clazz, FLAGS );
    }

    /**
     * Add <code>[out]</code> parameter at the end of the out parameter list.
     * Typically callers are <br>
     * composite in nature JIStruct, JIUnions, JIPointer and JIString .
     * 
     * @param outparam
     * @param FLAGS
     */
    public void addOutParamAsObject ( final Object outparam, final int FLAGS )
    {
        insertOutParamAt ( this.outParams.size (), outparam, FLAGS );
    }

    /**
     * insert an <code>[out]</code> parameter at the specified index in the out
     * parameter list.
     * 
     * @param index
     *            0 based index
     * @param classOrInstance
     *            can be either a Class or an Object
     * @param FLAGS
     */
    public void insertOutParamAt ( final int index, final Object classOrInstance, final int FLAGS )
    {
        this.outParams.add ( index, classOrInstance );
        this.outparamFlags.add ( index, new Integer ( FLAGS ) );
    }

    /**
     * Retrieves the <code>[out]</code> param at the index in the out parameters
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public Object getOutParamAt ( final int index )
    {
        return this.outParams.get ( index );
    }

    /**
     * Removes <code>[out]</code> parameter at the specified index from the out
     * parameters list.
     * 
     * @param index
     *            0 based index
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void removeOutParamAt ( final int index, final int FLAGS )
    {
        this.outParams.remove ( index );
        this.outparamFlags.remove ( index );
    }

    /**
     * Add <code>[out]</code> parameter as <code>Object[]</code> at the end of
     * the Parameter list. The array is iterated and
     * all members appended to the list.
     * 
     * @param values
     * @param FLAGS
     *            from JIFlags (if need be).
     */
    public void setOutParams ( final Object[] values, final int FLAGS )
    {
        for ( int i = 0; i < values.length; i++ )
        {
            this.outParams.add ( values[i] );
            this.outparamFlags.add ( new Integer ( FLAGS ) );
        }

    }

    //now for the results

    /**
     * Returns the results as an <code>Object[]</code>. This array has to be
     * iterated over to get the individual values.
     */
    //	only valid before the interpretation of read, after that has actual values
    public Object[] getResults ()
    {
        //checkIfCalled();
        return this.outparams;
    }

    /**
     * Returns the value as <code>int</code> at the index from the result list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public int getResultAsIntAt ( final int index )
    {
        checkIfCalled ();
        return ( (Integer)this.outparams[index] ).intValue ();
    }

    /**
     * Returns the value as <code>float</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public float getResultAsFloatAt ( final int index )
    {
        checkIfCalled ();
        return ( (Float)this.outparams[index] ).floatValue ();
    }

    /**
     * Returns the value as <code>boolean</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public boolean getResultAsBooleanAt ( final int index )
    {
        checkIfCalled ();
        return ( (Boolean)this.outparams[index] ).booleanValue ();
    }

    /**
     * Returns the value as <code>short</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public short getResultAsShortAt ( final int index )
    {
        checkIfCalled ();
        return ( (Short)this.outparams[index] ).shortValue ();
    }

    /**
     * Returns the value as <code>double</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public double getResultAsDoubleAt ( final int index )
    {
        checkIfCalled ();
        return ( (Double)this.outparams[index] ).doubleValue ();
    }

    /**
     * Returns the value as <code>char</code> at the index from the result list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public char getResultAsCharacterAt ( final int index )
    {
        checkIfCalled ();
        return ( (Character)this.outparams[index] ).charValue ();
    }

    /**
     * Returns the value as <code>JIString</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public JIString getResultAsStringAt ( final int index )
    {
        checkIfCalled ();
        return (JIString)this.outparams[index];
    }

    /**
     * Returns the value as <code>JIVariant</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public JIVariant getResultAsVariantAt ( final int index )
    {
        checkIfCalled ();
        return (JIVariant)this.outparams[index];
    }

    /**
     * Returns the value as <code>String representation of the UUID</code> at
     * the index from the result list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public String getResultAsUUIDStrAt ( final int index )
    {
        checkIfCalled ();
        return ( (UUID)this.outparams[index] ).toString ();
    }

    /**
     * Returns the value as <code>JIPointer</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public JIPointer getResultAsPointerAt ( final int index )
    {
        checkIfCalled ();
        return (JIPointer)this.outparams[index];
    }

    /**
     * Returns the value as <code>JIStruct</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public JIStruct getResultAsStructAt ( final int index )
    {
        checkIfCalled ();
        return (JIStruct)this.outparams[index];
    }

    /**
     * Returns the value as <code>JIArray</code> at the index from the result
     * list.
     * 
     * @param index
     *            0 based index
     * @return
     */
    public JIArray getResultAsArrayAt ( final int index )
    {
        checkIfCalled ();
        return (JIArray)this.outparams[index];
    }

    /**
     * Returns the results incase an exception occured.
     * 
     * @return
     */
    public Object[] getResultsInCaseOfException ()
    {
        //checkIfCalled();
        return this.resultsOfException;
    }

    /**
     * Returns the <code>HRESULT</code> of this operation. This should be zero
     * for successful calls and
     * non-zero for failures.
     * 
     * @return
     */
    public int getHRESULT ()
    {
        return this.hresult;
    }

    private void checkIfCalled ()
    {
        if ( !this.executed )
        {
            throw new IllegalStateException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_API_INCORRECTLY_CALLED ) );
        }
    }

    /**
     * Returns the entire <code>[in]</code> parameters list.
     * 
     * @return
     */
    public Object[] getInParams ()
    {
        return this.inParams.toArray ();
    }

    /**
     * Returns the entire <code>[out]</code> parameters list.
     * 
     * @return
     */
    public Object[] getOutParams ()
    {
        return this.outParams.toArray ();
    }

    /**
     * Returns the In Param flag.
     * 
     * @return
     */
    public Integer[] getInparamFlags ()
    {
        return (Integer[])this.inparamFlags.toArray ( new Integer[0] );
    }

    /**
     * Returns the Out Param flag.
     * 
     * @return
     */
    public Integer[] getOutparamFlags ()
    {
        return (Integer[])this.outparamFlags.toArray ( new Integer[0] );
    }

    /**
     * Returns the opnum of the API which will be invoked at the
     * <code>COM</code> server.
     */
    @Override
    public int getOpnum ()
    {
        //opnum is 3 as this is a COM interface and 0,1,2 are occupied by IUnknown
        //TODO remember this for extending com components also.
        return this.opnum;
    }

    //All Methods are 0 index based
    /**
     * Sets the opnum of the API which will be invoked at the <code>COM</code>
     * server. This is a 0 based index.
     * Refer to the IDL of the <code>COM server</code> for this, all APIs are
     * listed in a sequential order starting from 0. Please ignore the
     * <code>"Id"</code> they might be having and count the index of the API
     * being called here from the beginning of the interface starting from
     * 0 as the first index. Also note that if this interface derieves from
     * anything other than <code>IUnknown</code> or <code>IDispatch</code>, your
     * start
     * index will change from 0 to the cumulative(if that interface is also a
     * derieved one) count of the super interface. For e.g if A(3 apis) derieves
     * from B
     * (10 apis), then first API of A is at Opnum of 3, second at 4 and so on.
     * Alternatively, you can use the IJIDispatch interface, if the object
     * supports it.
     */
    public void setOpnum ( final int num )
    {
        int dispatch = 0;
        if ( !this.dispatchNotSupported )
        {
            dispatch = 4; //4 apis.
        }
        this.opnum = dispatch + num + 3; //0,1,2, Q.I
    }

    void write2 ( final NetworkDataRepresentation ndr )
    {
        //reset buffer size here...
        //calculate rough length required length + 16 for the last bytes
        //plus adding 30 more for the verifier etc. 
        ndr.getBuffer ().buf = new byte[bufferLength () + 16 + 30];
        JIOrpcThat.encode ( ndr );
        writePacket ( ndr );
    }

    /**
     * @exclude
     */
    @Override
    public void write ( final NetworkDataRepresentation ndr )
    {

        //reset buffer size here...
        //calculate rough length required length + 16 for the last bytes
        //plus adding 30 more for the verifier etc. 
        ndr.getBuffer ().buf = new byte[bufferLength () + 16];

        final JIOrpcThis orpcthis = new JIOrpcThis ();
        orpcthis.encode ( ndr );

        writePacket ( ndr );

        //when it ends add 16 zeros.
        ndr.writeUnsignedLong ( 0 );
        ndr.writeUnsignedLong ( 0 );
        ndr.writeUnsignedLong ( 0 );
        ndr.writeUnsignedLong ( 0 );

    }

    private void writePacket ( final NetworkDataRepresentation ndr )
    {
        if ( this.session == null )
        {
            throw new IllegalStateException ( "Programming Error ! Session not attached with this call ! ... Please rectify ! " );
        }

        final Object[] inparams = this.inParams.toArray ();

        int index = 0;
        if ( inparams != null )
        {
            //			if (JISystem.getLogger().isLoggable(Level.FINEST))
            //			{
            //				String str = "";
            //				for (int i = 0;i < inparams.length;i++)
            //				{
            //					str = str + "In Param:[" + i + "] " + inparams[i] + "\n";
            //				}
            //				JISystem.getLogger().finest(str);
            //			}
            while ( index < inparams.length )
            {
                final List listOfDefferedPointers = new ArrayList ();
                if ( inparams[index] == null )
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), listOfDefferedPointers, JIFlags.FLAG_NULL );
                }
                else
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, inparams[index].getClass (), inparams[index], listOfDefferedPointers, ( (Integer)this.inparamFlags.get ( index ) ).intValue () );
                }

                int x = 0;

                while ( x < listOfDefferedPointers.size () )
                {
                    //					thought of this today morning...change the logic here...the defeered pointers need to be 
                    //					completely serialized here. If they are also having nested deffered pointers then  those pointers
                    //					should be "inserted" just after the current pointer itself.
                    //					change the logic below to send out a new list and insert that list after the current x.
                    //					consider the case when there is a Struct having a nested pointer to another struct and this struct
                    //					itself having a pointer.
                    //					
                    //					Inparams order:- for 2 params.
                    //					int f,Struct{int i;			 
                    //								 Struct *ptr;
                    //								 Struct *ptr2;
                    //								 int j;
                    //								}
                    //					
                    //					while serializing this struct the pointer 1 will get deffered and so will pointer 2. Now while writing
                    //					the deffered pointers , we will find that the pointer 1 is pointing to a struct which has another deffered pointer (pointer to another struct maybe)
                    //					in such case, the current logic will add the deffered pointer to the end of the listOfDefferedPointers list, effectively serializing it
                    //					after the pointer 2 referent. But that is what is against the rules of DCERPC, in this case the referent of pointer 1 (struct with the pointer to another struct)
                    //					should be serialized in place (following th rules of the struct serialization ofcourse) and should not go to the end of the list.

                    //JIMarshalUnMarshalHelper.serialize(ndr,JIPointer.class,(JIPointer)listOfDefferedPointers.get(x),listOfDefferedPointers,inparamFlags);
                    final ArrayList newList = new ArrayList ();
                    JIMarshalUnMarshalHelper.serialize ( ndr, JIPointer.class, listOfDefferedPointers.get ( x ), newList, ( (Integer)this.inparamFlags.get ( index ) ).intValue () );
                    x++; //incrementing index
                    listOfDefferedPointers.addAll ( x, newList );
                }
                index++;
            }

        }
    }

    /**
     * @exclude
     */
    @Override
    public void read ( final NetworkDataRepresentation ndr )
    {
        //		if (opnum == 10) FOR TESTING ONLY
        //		{
        //			byte[] buffer = new byte[360];
        //			FileInputStream inputStream;
        //			try {
        //				inputStream = new FileInputStream("c:/temp/ONEEVENTSTRUCT");
        //				inputStream.read(buffer,0,360);
        //			} catch (Exception e) {
        //				// TODO Auto-generated catch block
        //				e.printStackTrace();
        //			}
        //			
        //			NdrBuffer ndrBuffer = new NdrBuffer(buffer,0);
        //			ndr.setBuffer(ndrBuffer);
        //			NetworkDataRepresentation ndr2 = new NetworkDataRepresentation();
        //			ndr2.setBuffer(ndrBuffer);
        //			read2(ndr2);
        //		}
        //interpret based on the out params flags
        final JIOrpcThat orpcThat = JIOrpcThat.decode ( ndr );
        readPacket ( ndr, false );
        readResult ( ndr );
    }

    /**
     * called by only COMRuntime and NO ONE ELSE.
     * 
     * @exclude
     * @param ndr
     */
    void read2 ( final NetworkDataRepresentation ndr )
    {
        JIOrpcThis.decode ( ndr );
        readPacket ( ndr, true );
        //readResult(ndr);
        //hresult = 0;
    }

    private void readPacket ( final NetworkDataRepresentation ndr, final boolean fromCallback )
    {

        if ( this.session == null )
        {
            throw new IllegalStateException ( "Programming Error ! Session not attached with this call ! ... Please rectify ! " );
        }

        int index = 0;

        this.outparams = this.outParams.toArray ();

        if ( logger.isTraceEnabled () )
        {
            final StringBuilder str = new StringBuilder ();
            for ( int i = 0; i < this.outparams.length; i++ )
            {
                str.append ( "Out Param:[" + i + "]" + this.outparams[i] + "\n" );
            }

            logger.trace ( str.toString () );
        }

        final ArrayList comObjects = new ArrayList ();
        final Map additionalData = new HashMap ();
        additionalData.put ( CURRENTSESSION, this.session );
        additionalData.put ( COMOBJECTS, comObjects );
        final ArrayList results = new ArrayList ();
        //user has nothing to return.
        if ( this.outparams != null && this.outparams.length > 0 )
        {
            while ( index < this.outparams.length )
            {
                final List listOfDefferedPointers = new ArrayList ();
                results.add ( JIMarshalUnMarshalHelper.deSerialize ( ndr, this.outparams[index], listOfDefferedPointers, ( (Integer)this.outparamFlags.get ( index ) ).intValue (), additionalData ) );
                int x = 0;

                while ( x < listOfDefferedPointers.size () )
                {

                    final ArrayList newList = new ArrayList ();
                    final JIPointer replacement = (JIPointer)JIMarshalUnMarshalHelper.deSerialize ( ndr, listOfDefferedPointers.get ( x ), newList, ( (Integer)this.outparamFlags.get ( index ) ).intValue (), additionalData );
                    ( (JIPointer)listOfDefferedPointers.get ( x ) ).replaceSelfWithNewPointer ( replacement ); //this should replace the value in the original place.	
                    x++;
                    listOfDefferedPointers.addAll ( x, newList );
                }
                index++;
            }

            //now create the right COM Objects, it is required here only and no place else. 
            for ( int i = 0; i < comObjects.size (); i++ )
            {
                final JIComObjectImpl comObjectImpl = (JIComObjectImpl)comObjects.get ( i );
                try
                {
                    IJIComObject comObject = null;
                    if ( fromCallback )
                    {
                        //this is a new IP , so make a new JIComServer for this.
                        final JISession newsession = JISession.createSession ( this.session );
                        newsession.setGlobalSocketTimeout ( this.session.getGlobalSocketTimeout () );
                        newsession.useSessionSecurity ( this.session.isSessionSecurityEnabled () );
                        newsession.useNTLMv2 ( this.session.isNTLMv2Enabled () );
                        final JIComServer comServer = new JIComServer ( newsession, comObjectImpl.internal_getInterfacePointer (), null );
                        comObject = comServer.getInstance ();
                        JIFrameworkHelper.link2Sessions ( this.session, newsession );
                    }
                    else
                    {
                        comObject = JIFrameworkHelper.instantiateComObject2 ( this.session, comObjectImpl.internal_getInterfacePointer () );
                    }

                    comObjectImpl.replaceMembers ( comObject );
                    JIFrameworkHelper.addComObjectToSession ( comObjectImpl.getAssociatedSession (), comObjectImpl );
                    //Why did I put this here. We should do an addRef regardless of whether we give a pointer to COM or it gives us one.
                    //					if (!fromCallback)
                    {
                        comObjectImpl.addRef ();
                    }

                }
                catch ( final JIException e )
                {
                    logger.error ( "readPacket", e );
                    throw new JIRuntimeException ( e.getErrorCode () );
                }
                //replace the members of the original com objects by the completed ones.
            }

            comObjects.clear ();
        }

        this.outparams = results.toArray ();
        this.executed = true;
    }

    private void readResult ( final NetworkDataRepresentation ndr )
    {
        //last has to be the result.
        this.hresult = ndr.readUnsignedLong ();

        if ( this.hresult != 0 )
        {
            //something exception occured at server, set up results
            this.resultsOfException = this.outparams;
            this.outparams = null;
            throw new JIRuntimeException ( this.hresult );
        }
    }

    private int bufferLength ()
    {
        int length = 0;
        final Object[] inparams = this.inParams.toArray ();
        for ( int i = 0; i < inparams.length; i++ )
        {
            if ( inparams[i] == null )
            {
                length = length + 4;
                continue;
            }
            final int length2 = JIMarshalUnMarshalHelper.getLengthInBytes ( inparams[i].getClass (), inparams[i], JIFlags.FLAG_NULL );
            length = length + length2;
        }

        return length + 2048; //2K extra for alignments, if any.
    }

    /**
     * Returns true incase the Call resulted in an exception, use getHRESULT to
     * get the error code.
     * 
     * @return
     */
    public boolean isError ()
    {
        checkIfCalled ();
        return this.hresult != 0;
    }

    void attachSession ( final JISession session )
    {
        this.session = session;
    }

    JISession getSession ()
    {
        return this.session;
    }
}
