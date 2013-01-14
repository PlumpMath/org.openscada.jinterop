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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JISystem;

/**
 * <p>
 * Represents a C++ array which can display both <i>conformant and standard</i>
 * behaviors. Since this class forms a wrapper on the actual array, the
 * developer is expected to provide complete and final arrays (of Objects) to
 * this class. Modifying the wrapped array afterwards <b>will</b> have
 * unexpected results.
 * </p>
 * <p>
 * <i>Please refer to <b>MSExcel</b> examples for more details on how to use
 * this class.</i>
 * <p>
 * <b>Note</b>: Wrapped Arrays can be at most two dimensional in nature. Above
 * that is not supported by the library.
 * 
 * @since 1.0
 */
public final class JIArray implements Serializable
{

    private static final long serialVersionUID = -8267477025978489665L;

    private Object memberArray = null;

    private Class clazz = null;

    private int[] upperBounds = null;

    private int dimension = -1;

    private int numElementsInAllDimensions = 0;

    private boolean isConformant = false;

    private boolean isVarying = false;

    private boolean isConformantProxy = false;

    private boolean isVaryingProxy = false;

    private List conformantMaxCounts = new ArrayList (); //list of integers

    private Object template = null;

    private int sizeOfNestedArrayInBytes = 0; //used in both encoding and decoding.

    private JIArray ()
    {

    }

    /**
     * <p>
     * Creates an array object of the type specified by <code>clazz</code>. This
     * is used to prepare a template for decoding an array of that type. Used
     * only for setting as an <code>[out]</code> parameter in a JICallBuilder.
     * </p>
     * <p>
     * For example:- <br>
     * This call creates a template for a single dimension Integer array of size
     * 10. <code> 
     * <br>
     * JIArray array = new JIArray(Integer.class,new int[]{10},1,false);
     * </code> <br>
     * </P>
     * 
     * @param clazz
     *            class whose instances will be members of the deserialized
     *            array.
     * @param upperBounds
     *            highest index for each dimension.
     * @param dimension
     *            number of dimensions
     * @param isConformant
     *            declares whether the array is <i>conformant</i> or not.
     * @throws IllegalArgumentException
     *             if <code>upperBounds</code> is supplied and its length
     *             is not equal to the <code>dimension</code> parameter.
     */
    public JIArray ( final Class clazz, final int[] upperBounds, final int dimension, final boolean isConformant )
    {
        this.clazz = clazz;
        init2 ( upperBounds, dimension, isConformant, false );
    }

    /**
     * <P>
     * Refer to {@link #JIArray(Class, int[], int, boolean)}
     * 
     * @param clazz
     *            class whose instances will be members of the deserialized
     *            array.
     * @param upperBounds
     *            highest index for each dimension.
     * @param dimension
     *            number of dimensions
     * @param isConformant
     *            declares whether the array is <i>conformant</i> or not.
     * @param isVarying
     *            declares whether the array is <i>varying</i> or not.
     * @throws IllegalArgumentException
     *             if <code>upperBounds</code> is supplied and its length
     *             is not equal to the <code>dimension</code> parameter.
     */
    public JIArray ( final Class clazz, final int[] upperBounds, final int dimension, final boolean isConformant, final boolean isVarying )
    {
        this.clazz = clazz;
        init2 ( upperBounds, dimension, isConformant, isVarying );
    }

    /**
     * <p>
     * Creates an array object with members of the type <code>template</code>.
     * This constructor is used to prepare a template for decoding an array and
     * is exclusively for composites like <code>JIStruct</code>,
     * <code>JIPointer</code>, <code>JIUnion</code>, <code>JIString</code> where
     * more information on the structure of the composite is required before
     * trying to deserialize it.
     * <p>
     * Sample Usage:- <br>
     * <code>
     *  JIStruct safeArrayBounds = new JIStruct(); <br>
     * 	safeArrayBounds.addMember(Integer.class); <br>
     * 	safeArrayBounds.addMember(Integer.class); <br><br>
     * 	
     * 	//arraydesc <br>
     * 	JIStruct arrayDesc = new JIStruct(); <br>
     * 	//typedesc <br>
     * 	JIStruct typeDesc = new JIStruct(); <br><br>
     * 	
     * 	arrayDesc.addMember(typeDesc);<br>
     * 	arrayDesc.addMember(Short.class);<br>
     * 	arrayDesc.addMember(<b>new JIArray(safeArrayBounds,new int[]{1},1,true)</b>);<br>
     *  </code>
     * </p>
     * 
     * @param template
     *            can be only of the type <code>JIStruct</code>,
     *            <code>JIPointer</code>, <code>JIUnion</code>,
     *            <code>JIString</code>
     * @param upperBounds
     *            highest index for each dimension.
     * @param dimension
     *            number of dimensions
     * @param isConformant
     *            declares whether the array is <i>conformant</i> or not.
     * @throws IllegalArgumentException
     *             if <code>upperBounds</code> is supplied and its length
     *             is not equal to the <code>dimension</code> parameter.
     * @throws IllegalArgumentException
     *             if <code>template</code> is null or is not of the
     *             specified types.
     */
    //for structs, pointers , unions.
    public JIArray ( final Object template, final int[] upperBounds, final int dimension, final boolean isConformant )
    {
        if ( template == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_TEMPLATE_NULL ) );
        }

        if ( !template.getClass ().equals ( JIStruct.class ) && !template.getClass ().equals ( JIUnion.class ) && !template.getClass ().equals ( JIPointer.class ) && !template.getClass ().equals ( JIString.class ) )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_INCORRECT_TEMPLATE_PARAM ) );
        }

        this.template = template;
        this.clazz = template.getClass ();

        init2 ( upperBounds, dimension, isConformant, false );
    }

    /**
     * <p>
     * Refer to {@link #JIArray(Object, int[], int, boolean)} for details.
     * 
     * @param template
     *            can be only of the type <code>JIStruct</code>,
     *            <code>JIPointer</code>, <code>JIUnion</code>,
     *            <code>JIString</code>
     * @param upperBounds
     *            highest index for each dimension.
     * @param dimension
     *            number of dimensions
     * @param isConformant
     *            declares whether the array is <i>conformant</i> or not.
     * @param isVarying
     *            declares whether the array is <i>varying</i> or not.
     * @throws IllegalArgumentException
     *             if <code>upperBounds</code> is supplied and its length
     *             is not equal to the <code>dimension</code> parameter.
     * @throws IllegalArgumentException
     *             if <code>template</code> is null or is not of the
     *             specified types.
     */
    //for structs, pointers , unions.
    public JIArray ( final Object template, final int[] upperBounds, final int dimension, final boolean isConformant, final boolean isVarying )
    {
        if ( template == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_TEMPLATE_NULL ) );
        }

        if ( !template.getClass ().equals ( JIStruct.class ) && !template.getClass ().equals ( JIUnion.class ) && !template.getClass ().equals ( JIPointer.class ) && !template.getClass ().equals ( JIString.class ) )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_INCORRECT_TEMPLATE_PARAM ) );
        }

        this.template = template;
        this.clazz = template.getClass ();

        init2 ( upperBounds, dimension, isConformant, isVarying );
    }

    private void init2 ( final int[] upperBounds, final int dimension, final boolean isConformant, final boolean isVarying )
    {
        this.upperBounds = upperBounds;
        this.dimension = dimension;
        this.isConformant = isConformant;
        this.isConformantProxy = isConformant;
        this.isVarying = isVarying;
        this.isVaryingProxy = isVarying;

        if ( upperBounds != null )
        {
            //have to supply the upperbounds for each dimension , no gaps in between
            if ( upperBounds.length != dimension )
            {
                throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_UPPERBNDS_DIM_NOTMATCH ) );
            }
        }

        for ( int i = 0; upperBounds != null && i < upperBounds.length; i++ )
        {
            this.numElementsInAllDimensions = this.numElementsInAllDimensions + upperBounds[i];
            if ( isConformant )
            {
                this.conformantMaxCounts.add ( new Integer ( upperBounds[i] ) );
            }
        }

        //numElementsInAllDimensions = numElementsInAllDimensions * dimension;
    }

    /**
     * <p>
     * Creates an object with <i>array</i> parameter as the nested Array. This
     * constructor is used when the developer wants to send an array to COM
     * server.
     * <p>
     * Sample Usage :- <br>
     * <code>
     * JIArray array = new JIArray(new JIString[]{new JIString(name)},true); <br>
     * </code>
     * 
     * @param array
     *            Array of any type. Primitive arrays are not allowed.
     * @param isConformant
     *            declares whether the array is <code>conformant</code> or not.
     * @throws IllegalArgumentException
     *             if the <code>array</code> is not an array or
     *             is of primitive type or is an array of
     *             <code>java.lang.Object</code>.
     */
    public JIArray ( final Object array, final boolean isConformant )
    {
        this.isConformant = isConformant;
        this.isConformantProxy = isConformant;
        init ( array );
    }

    /**
     * Refer {@link #JIArray(Object, boolean)}
     * 
     * @param array
     *            Array of any type. Primitive arrays are not allowed.
     * @param isConformant
     *            declares whether the array is <code>conformant</code> or not.
     * @param isVarying
     *            declares whether the array is <code>varying</code> or not.
     * @throws IllegalArgumentException
     *             if the <code>array</code> is not an array or
     *             is of primitive type or is an array of
     *             <code>java.lang.Object</code>.
     */
    public JIArray ( final Object array, final boolean isConformant, final boolean isVarying )
    {
        this.isConformant = isConformant;
        this.isConformantProxy = isConformant;
        this.isVarying = isVarying;
        this.isVaryingProxy = isVarying;
        init ( array );
    }

    /***
     * <p>
     * Creates an object with <i>array</i> parameter as the nested Array. This
     * constructor forms a <code>non-conformant</code> array and is used when
     * the developer wants to send an array to COM server.
     * <p>
     * Sample Usage :- <br>
     * <code>
     * JIArray array = new JIArray(new JIString[]{new JIString(name)},true); <br>
     * </code>
     * 
     * @param array
     *            Array of any type. Primitive arrays are not allowed.
     * @throws IllegalArgumentException
     *             if the <code>array</code> is not an array or
     *             is of primitive type or is an array of
     *             <code>java.lang.Object</code>.
     */
    public JIArray ( final Object array )
    {
        init ( array );
    }

    private void init ( final Object array )
    {
        if ( !array.getClass ().isArray () )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_PARAM_ONLY ) );
        }

        if ( array.getClass ().isPrimitive () )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_PRIMITIVE_NOTACCEPT ) );
        }

        //bad way...but what the heck...
        if ( array.getClass ().toString ().indexOf ( "java.lang.Object" ) != -1 )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_ARRAY_TYPE_INCORRECT ) );
        }

        this.memberArray = array;

        final ArrayList upperBounds2 = new ArrayList ();
        String name = array.getClass ().getName ();
        Object subArray = array;
        this.numElementsInAllDimensions = 1;
        while ( name.startsWith ( "[" ) )
        {
            name = name.substring ( 1 );
            final int x = ( (Object[])subArray ).length;
            upperBounds2.add ( new Integer ( x ) );
            this.numElementsInAllDimensions = this.numElementsInAllDimensions * x;
            if ( this.isConformant )
            {
                this.conformantMaxCounts.add ( new Integer ( x ) );
            }
            this.clazz = subArray.getClass ().getComponentType ();
            if ( x == 0 ) //In which ever index the length is 0 , the array stops there, example Byte[0],Byte[0][10],Byte[10][0]
            {
                break;
            }
            subArray = Array.get ( subArray, 0 );
            this.dimension++;
        }

        if ( this.dimension == -1 )
        {
            this.numElementsInAllDimensions = 0;
            this.dimension++;
        }

        this.upperBounds = new int[upperBounds2.size ()];
        for ( int i = 0; i < upperBounds2.size (); i++ )
        {
            this.upperBounds[i] = ( (Integer)upperBounds2.get ( i ) ).intValue ();
        }
        this.dimension++; //since it starts from -1.
        this.sizeOfNestedArrayInBytes = computeLengthArray ( array );
    }

    private int computeLengthArray ( final Object array )
    {
        int length = 0;
        final String name = array.getClass ().getName ();
        final Object o[] = (Object[])array;
        for ( int i = 0; i < o.length; i++ )
        {
            if ( name.charAt ( 1 ) != '[' )
            {
                final Object o1[] = (Object[])array;
                for ( int j = 0; j < o1.length; j++ )
                {
                    length = length + JIMarshalUnMarshalHelper.getLengthInBytes ( o1.getClass ().getComponentType (), o1[j], JIFlags.FLAG_NULL );
                }
                return length;
            }
            length = length + computeLengthArray ( Array.get ( array, i ) );
        }

        return length;
    }

    /**
     * Returns the nested Array.
     * 
     * @return array Object which can be type casted based on value returned by
     *         {@link #getArrayClass()}.
     */
    public Object getArrayInstance ()
    {
        return this.memberArray;
    }

    /**
     * Class of the nested Array.
     * 
     * @return <code>class</code>
     */
    public Class getArrayClass ()
    {
        return this.clazz;
    }

    /**
     * Array of integers depicting highest index for each dimension.
     * 
     * @return <code>int[]</code>
     */
    public int[] getUpperBounds ()
    {
        return this.upperBounds;
    }

    /**
     * Returns the dimensions of the Array.
     * 
     * @return <code>int</code>
     */
    public int getDimensions ()
    {
        return this.dimension;
    }

    int getSizeOfAllElementsInBytes ()
    {
        //		int length = numElementsInAllDimensions * JIMarshalUnMarshalHelper.getLengthInBytes(clazz,((Object[])memberArray)[0],JIFlags.FLAG_NULL);

        //this means that decode has created this array, and we need to compute the size to stay consistent.
        if ( this.sizeOfNestedArrayInBytes == -1 )
        {
            this.sizeOfNestedArrayInBytes = computeLengthArray ( this.memberArray );
        }

        return this.sizeOfNestedArrayInBytes;
    }

    void encode ( final NetworkDataRepresentation ndr, final Object array, final List defferedPointers, final int FLAG )
    {
        //	ArrayList listofDefferedPointers = new ArrayList();

        if ( this.isConformantProxy )
        {
            //first write the max counts ...First to last dimension.
            int i = 0;
            while ( i < this.conformantMaxCounts.size () )
            {
                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, this.conformantMaxCounts.get ( i ), defferedPointers, FLAG );
                i++;
            }

            this.isConformantProxy = false; //this is since encode is recursive.
        }

        if ( this.isVaryingProxy )
        {
            //write the offset and the actual count
            int i = 0;
            while ( i < this.conformantMaxCounts.size () )
            {
                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, new Integer ( 0 ), defferedPointers, FLAG );//offset
                JIMarshalUnMarshalHelper.serialize ( ndr, Integer.class, this.conformantMaxCounts.get ( i ), defferedPointers, FLAG );//actual count
                i++;
            }

            this.isVaryingProxy = false; //this is since encode is recursive.
        }

        final String name = array.getClass ().getName ();
        final Object o[] = (Object[])array;
        for ( int i = 0; i < o.length; i++ )
        {
            if ( name.charAt ( 1 ) != '[' )
            {
                final Object o1[] = (Object[])array;
                for ( int j = 0; j < o1.length; j++ )
                {
                    JIMarshalUnMarshalHelper.serialize ( ndr, this.clazz, o1[j], defferedPointers, FLAG | JIFlags.FLAG_REPRESENTATION_ARRAY );
                }
                return;
            }
            encode ( ndr, Array.get ( array, i ), defferedPointers, FLAG );
        }

    }

    /**
     * Status whether the array is <code>conformant</code> or not.
     * 
     * @return <code>true</code> is array is <code>conformant</code>.
     */
    public boolean isConformant ()
    {
        return this.isConformant;
    }

    /**
     * Status whether the array is <code>varying</code> or not.
     * 
     * @return <code>true</code> is array is <code>varying</code>.
     */
    public boolean isVarying ()
    {
        return this.isVarying;
    }

    Object decode ( final NetworkDataRepresentation ndr, final Class arrayType, final int dimension, final List defferedPointers, final int FLAG, final Map additionalData )
    {
        final JIArray retVal = new JIArray ();
        retVal.isConformantProxy = this.isConformantProxy;
        retVal.isVaryingProxy = this.isVaryingProxy;
        if ( this.isConformantProxy )
        {

            //first read the max counts ...First to last dimension.
            int i = 0;
            while ( i < dimension )
            {
                retVal.conformantMaxCounts.add ( JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, defferedPointers, FLAG, additionalData ) );
                i++;
            }

            //isConformantProxy = false; //this is since decode is recursive.

            if ( this.upperBounds == null )
            {
                //max elements will come now.
                retVal.numElementsInAllDimensions = 0;
                retVal.upperBounds = new int[retVal.conformantMaxCounts.size ()];
                i = 0;
                while ( i < retVal.conformantMaxCounts.size () )
                {
                    retVal.upperBounds[i] = ( (Integer)retVal.conformantMaxCounts.get ( i ) ).intValue ();
                    retVal.numElementsInAllDimensions = retVal.numElementsInAllDimensions * retVal.upperBounds[i];
                    i++;
                }
                if ( i == 0 )
                {
                    this.numElementsInAllDimensions = 0;
                }
                //retVal.numElementsInAllDimensions = retVal.numElementsInAllDimensions * dimension;
            }
        }
        else
        {//this is the case when it is non conformant or coming from struct.
            retVal.upperBounds = this.upperBounds;
            retVal.conformantMaxCounts = this.conformantMaxCounts;
            retVal.numElementsInAllDimensions = this.numElementsInAllDimensions;
        }

        if ( this.isVaryingProxy )
        {
            //first read the max counts ...First to last dimension.
            int i = 0;
            retVal.conformantMaxCounts.clear ();//can't take the max count size now
            retVal.upperBounds = null;
            retVal.numElementsInAllDimensions = 0;

            while ( i < dimension )
            {
                JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, defferedPointers, FLAG, null );///offset
                retVal.conformantMaxCounts.add ( JIMarshalUnMarshalHelper.deSerialize ( ndr, Integer.class, defferedPointers, FLAG, additionalData ) );//actual count
                i++;
            }

            //isConformantProxy = false; //this is since decode is recursive.

            if ( this.upperBounds == null )
            {
                //max elements will come now.
                retVal.numElementsInAllDimensions = 1;
                retVal.upperBounds = new int[retVal.conformantMaxCounts.size ()];
                i = 0;
                while ( i < retVal.conformantMaxCounts.size () )
                {
                    retVal.upperBounds[i] = ( (Integer)retVal.conformantMaxCounts.get ( i ) ).intValue ();
                    retVal.numElementsInAllDimensions = retVal.numElementsInAllDimensions * retVal.upperBounds[i];
                    i++;
                }
                if ( i == 0 )
                {
                    this.numElementsInAllDimensions = 0;
                }
                //retVal.numElementsInAllDimensions = retVal.numElementsInAllDimensions * dimension;
            }

        }

        retVal.isConformant = this.isConformant;
        retVal.isVarying = this.isVarying;
        retVal.template = this.template;
        retVal.memberArray = recurseDecode ( retVal, ndr, arrayType, dimension, defferedPointers, FLAG, additionalData );
        retVal.clazz = this.clazz;
        retVal.dimension = this.dimension;
        retVal.sizeOfNestedArrayInBytes = -1; // setting here so that when a call actually comes for it's lenght , the getLength will compute. This is required since while decoding many pointers are still not complete and their length cannot be decided.
        return retVal;
    }

    private Object recurseDecode ( final JIArray retVal, final NetworkDataRepresentation ndr, final Class arrayType, final int dimension, final List defferedPointers, final int FLAG, final Map additionalData )
    {
        Object array = null;
        Class c = arrayType;
        for ( int j = 0; j < dimension; j++ )
        {
            array = Array.newInstance ( c, retVal.upperBounds[retVal.upperBounds.length - j - 1] );
            c = array.getClass ();
        }

        for ( int i = 0; i < retVal.upperBounds[retVal.upperBounds.length - dimension]; i++ )
        {
            if ( dimension == 1 )
            {
                //fill value here
                //Array.set(array,i,new Float(i));
                if ( this.template == null )
                {
                    Array.set ( array, i, JIMarshalUnMarshalHelper.deSerialize ( ndr, c.getComponentType () == null ? c : c.getComponentType (), defferedPointers, FLAG | JIFlags.FLAG_REPRESENTATION_ARRAY, additionalData ) );
                }
                else
                {
                    Array.set ( array, i, JIMarshalUnMarshalHelper.deSerialize ( ndr, this.template, defferedPointers, FLAG | JIFlags.FLAG_REPRESENTATION_ARRAY, additionalData ) );
                }
            }
            else
            {
                Array.set ( array, i, recurseDecode ( retVal, ndr, arrayType, dimension - 1, defferedPointers, FLAG, additionalData ) );
            }
        }

        return array;
    }

    /**
     * Reverses Array elements for IJIDispatch.
     * 
     * @return
     */
    int reverseArrayForDispatch ()
    {
        if ( this.memberArray == null )
        {
            return 0;
        }

        int i = 0;
        final Stack stack = new Stack ();
        for ( i = 0; i < ( (Object[])this.memberArray ).length; i++ )
        {
            stack.push ( ( (Object[])this.memberArray )[i] );
        }

        i = 0;
        while ( stack.size () > 0 )
        {
            ( (Object[])this.memberArray )[i++] = stack.pop ();
        }

        return i;
    }

    List getConformantMaxCounts ()
    {
        return this.conformantMaxCounts;
    }

    void setConformant ( final boolean isConformant )
    {
        this.isConformantProxy = isConformant;
    }

    void setVarying ( final boolean isVarying )
    {
        this.isVaryingProxy = isVarying;
    }

    void setMaxCountAndUpperBounds ( final List maxCount )
    {
        this.conformantMaxCounts = maxCount;
        //	if (upperBounds == null) this will always be null since this api will get called from a decode and 
        //in that the upperBounds is always null, since one does not know the dim expected.
        if ( this.conformantMaxCounts.size () > 0 )
        {
            //max elements will come now.
            this.numElementsInAllDimensions = 1;
            this.upperBounds = new int[this.conformantMaxCounts.size ()];
            int i = 0;
            while ( i < this.conformantMaxCounts.size () )
            {
                this.upperBounds[i] = ( (Integer)this.conformantMaxCounts.get ( i ) ).intValue ();
                this.numElementsInAllDimensions = this.numElementsInAllDimensions * this.upperBounds[i];
                i++;
            }
            if ( i == 0 )
            {
                this.numElementsInAllDimensions = 0;
            }
        }
        else
        {
            this.upperBounds = null;
            this.numElementsInAllDimensions = 0;
        }
    }

    int getNumElementsInAllDimensions ()
    {
        return this.numElementsInAllDimensions;
    }

    /**
     * <p>
     * Used only from the JIVariant.getDecodedValueAsArray. It is required when
     * the real class of the array is determined after the SafeArray Struct has
     * been processed. SA in COM can contain these along with normal types as
     * well :- FADF_BSTR 0x0100 An array of BSTRs. <br>
     * FADF_UNKNOWN 0x0200 An array of IUnknown*. <br>
     * FADF_DISPATCH 0x0400 An array of IDispatch*. <br>
     * FADF_VARIANT 0x0800 An array of VARIANTs. <br>
     * I have noticed that the "type" of the array doesn't always convey the
     * right thing, so this "feature" flag of the SA shas to be looked into. As
     * can be seen above except only BSTR require a template others do not. But
     * the logic for the JIString(BSTR) already works fine. So I will use this
     * flag only to set the JIVariant.class , whereever the "type" does not
     * specify it but the "feature" does.
     * </p>
     * 
     * @exclude
     * @param c
     */
    void updateClazz ( final Class c )
    {
        this.clazz = c;
    }

    @Override
    public String toString ()
    {
        String retVal = "[Type: " + this.clazz + " , ";
        if ( this.memberArray == null )
        {
            retVal += "memberArray is null , ";
        }
        else
        {
            retVal += this.memberArray + " , ";
        }

        if ( this.isConformant )
        {
            retVal += " conformant , ";
        }
        if ( this.isVarying )
        {
            retVal += " varying , ";
        }

        return retVal + "]";
    }
}
