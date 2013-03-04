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

package org.jinterop.dcom.impls.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIComObjectImplWrapper;
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JIFrameworkHelper;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.core.UUID;

/**
 * @exclude
 * @since 1.0
 */
final class JIDispatchImpl extends JIComObjectImplWrapper implements IJIDispatch
{

    private final static Logger logger = LoggerFactory.getLogger ( JIDispatchImpl.class );

    private static final long serialVersionUID = 4908149252176353846L;

    //IJIComObject comObject = null;
    private final Map cacheOfDispIds = new HashMap ();

    JIDispatchImpl ( final IJIComObject comObject )
    {
        super ( comObject );
        //this.comObject = comObject;
    }

    private final JIExcepInfo lastExcepInfo = new JIExcepInfo ();

    public static final int FLAG_TYPEINFO_SUPPORTED = 1;

    public static final int FLAG_TYPEINFO_NOTSUPPORTED = 0;

    public IJIComObject getCOMObject ()
    {
        return this.comObject;
    }

    @Override
    public int getTypeInfoCount () throws JIException
    {
        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 0 );
        obj.addInParamAsInt ( 0, JIFlags.FLAG_NULL );
        obj.addOutParamAsType ( Integer.class, JIFlags.FLAG_NULL );
        final Object[] result = this.comObject.call ( obj );
        return ( (Integer)result[0] ).intValue ();
    }

    @Override
    public int getIDsOfNames ( final String apiName ) throws JIException
    {
        if ( apiName == null || apiName.trim ().equals ( "" ) )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_DISP_INCORRECT_VALUE_FOR_GETIDNAMES ) );
        }

        Map innerMap = (Map)this.cacheOfDispIds.get ( apiName );
        if ( innerMap != null )
        {
            final Integer dispId = (Integer)innerMap.get ( apiName );
            return dispId.intValue ();
        }

        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 2 ); //size of the array																	//1st is the num elements and second is the actual values

        final JIString name = new JIString ( apiName.trim (), JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR );
        final JIArray array = new JIArray ( new JIPointer[] { new JIPointer ( name ) }, true );
        obj.addInParamAsUUID ( UUID.NIL_UUID, JIFlags.FLAG_NULL );
        obj.addInParamAsArray ( array, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 1, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0x800, JIFlags.FLAG_NULL );
        obj.addOutParamAsObject ( new JIArray ( Integer.class, null, 1, true ), JIFlags.FLAG_NULL );

        final Object[] result = this.comObject.call ( obj );

        if ( result == null && obj.isError () )
        {
            throw new JIException ( obj.getHRESULT () );
        }

        innerMap = new HashMap ();
        innerMap.put ( apiName, ( (Object[]) ( (JIArray)result[0] ).getArrayInstance () )[0] );
        this.cacheOfDispIds.put ( apiName, innerMap );

        //first will be the length , and the next will be the actual value.
        return ( (Integer) ( (Object[]) ( (JIArray)result[0] ).getArrayInstance () )[0] ).intValue ();//will get the dispatch ID.
    }

    @Override
    public int[] getIDsOfNames ( final String[] apiName ) throws JIException
    {
        if ( apiName == null || apiName.length == 0 )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_DISP_INCORRECT_VALUE_FOR_GETIDNAMES ) );
        }

        boolean sendForAll = false;
        //first one will be the method name
        Map innerMap = (Map)this.cacheOfDispIds.get ( apiName[0] );
        if ( innerMap != null ) //if name is not found will not even go in. so it is safe to assume that api name will always be there.
        {
            final int[] values = new int[innerMap.size ()];
            for ( int i = 0; i < apiName.length; i++ )
            {
                final Integer dispId = (Integer)innerMap.get ( apiName[i] );
                if ( dispId == null )
                {
                    sendForAll = true;
                    break;
                }
                else
                {
                    values[i] = dispId.intValue ();
                }
            }

            if ( !sendForAll )
            {
                return values; //all found returning now
            }
        }

        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 2 ); //size of the array																	//1st is the num elements and second is the actual values

        final JIPointer[] pointers = new JIPointer[apiName.length];

        for ( int i = 0; i < apiName.length; i++ )
        {
            if ( apiName[i] == null || apiName[i].trim ().equals ( "" ) )
            {
                throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_DISP_INCORRECT_VALUE_FOR_GETIDNAMES ) );
            }
            pointers[i] = new JIPointer ( new JIString ( apiName[i].trim (), JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR ) );
        }

        final JIArray array = new JIArray ( pointers, true );
        final JIArray arrayOut = new JIArray ( Integer.class, null, 1, true );
        obj.addInParamAsUUID ( UUID.NIL_UUID, JIFlags.FLAG_NULL );
        obj.addInParamAsArray ( array, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( apiName.length, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0x800, JIFlags.FLAG_NULL );

        obj.addOutParamAsObject ( arrayOut, JIFlags.FLAG_NULL );

        final Object[] result = this.comObject.call ( obj );

        if ( obj.getHRESULT () != 0 ) //exception occured
        {
            throw new JIException ( obj.getHRESULT (), JISystem.getLocalizedMessage ( obj.getHRESULT () ) );
        }

        final JIArray arrayOfResults = (JIArray)result[0];
        final Integer[] arrayOfDispIds = (Integer[])arrayOfResults.getArrayInstance ();
        final int[] retVal = new int[apiName.length];

        innerMap = innerMap == null ? new HashMap () : innerMap;
        for ( int i = 0; i < apiName.length; i++ )
        {
            retVal[i] = arrayOfDispIds[i].intValue ();
            innerMap.put ( apiName[i], arrayOfDispIds[i] );
        }

        if ( !this.cacheOfDispIds.containsKey ( apiName[0] ) )
        {
            this.cacheOfDispIds.put ( apiName[0], innerMap );
        }
        return retVal;
    }

    @Override
    public IJITypeInfo getTypeInfo ( final int typeInfo ) throws JIException
    {
        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 1 );
        obj.addInParamAsInt ( typeInfo, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0x400, JIFlags.FLAG_NULL );
        obj.addOutParamAsType ( IJIComObject.class, JIFlags.FLAG_NULL );
        //obj.setUpParams(new Object[]{new Integer(typeInfo),new Integer(0x400)},new Object[]{MInterfacePointer.class},JIFlags.FLAG_NULL,JIFlags.FLAG_NULL);
        final Object[] result = this.comObject.call ( obj );
        return (IJITypeInfo)JIObjectFactory.narrowObject ( (IJIComObject)result[0] );
    }

    //	//First inparams[0] will always be variant and the inparams[1] is expected to be an JIArray
    //	public JIVariant invoke(int dispId,int dispatchFlags,Object[] inparams) throws JIException
    //	{
    //		return invoke(dispId,dispatchFlags,inparams,null);
    //	}

    public JIVariant[] invoke ( final int dispId, final int dispatchFlags, final JIArray arrayOfVariantsInParams, final JIArray arrayOfNamedDispIds, final JIVariant outParamType ) throws JIException
    {
        this.lastExcepInfo.clearAll ();
        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 3 );

        final JIStruct dispParams = new JIStruct ();

        //now check whether any of the variants is representation of a variant ptr, if so replace it with an
        //EMPTY variant and add it to another array.
        final ArrayList listOfVariantPtrs = new ArrayList ();
        final ArrayList listOfPositions = new ArrayList ();
        JIVariant[] variants = null;
        int lengthVar = 0;
        //boolean isLastAptr = false;
        if ( arrayOfVariantsInParams != null )
        {
            lengthVar = JIFrameworkHelper.reverseArrayForDispatch ( arrayOfVariantsInParams );
            variants = (JIVariant[])arrayOfVariantsInParams.getArrayInstance ();
            for ( int i = 0; i < variants.length; i++ )
            {
                final JIVariant variant = variants[i];
                if ( variant.isByRefFlagSet () )
                {
                    listOfVariantPtrs.add ( variant );
                    listOfPositions.add ( new Integer ( i ) );//for position array
                    //now replace with Empty.
                    //variants[i] = new JIVariant(JIVariant.POINTER);
                    variants[i] = JIVariant.EMPTY ();
                }
            }
        }

        int lengthPtr = 0;
        if ( arrayOfNamedDispIds != null )
        {
            lengthPtr = JIFrameworkHelper.reverseArrayForDispatch ( arrayOfNamedDispIds );
        }

        dispParams.addMember ( new JIPointer ( arrayOfVariantsInParams ) ); //should be an array of variants
        dispParams.addMember ( new JIPointer ( arrayOfNamedDispIds ) ); //if there, this should be an array of variants , these too.
        dispParams.addMember ( new Integer ( lengthVar ) );
        dispParams.addMember ( new Integer ( lengthPtr ) );

        obj.addInParamAsInt ( dispId, JIFlags.FLAG_NULL );
        obj.addInParamAsUUID ( UUID.NIL_UUID, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0x800, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( dispatchFlags ^ 0xFFFFFFF0, JIFlags.FLAG_NULL );
        obj.addInParamAsStruct ( dispParams, JIFlags.FLAG_REPRESENTATION_IDISPATCH_INVOKE );

        //now add the extra params if exist.
        if ( listOfVariantPtrs.size () > 0 )
        {
            //write length
            obj.addInParamAsInt ( listOfPositions.size (), JIFlags.FLAG_NULL );
            //then write the array
            obj.addInParamAsArray ( new JIArray ( listOfPositions.toArray ( new Integer[listOfPositions.size ()] ), true ), JIFlags.FLAG_NULL );
            //now write the array of variant ptrs
            obj.addInParamAsArray ( new JIArray ( listOfVariantPtrs.toArray ( new JIVariant[listOfVariantPtrs.size ()] ), true ), JIFlags.FLAG_NULL );
        }

        obj.addInParamAsObject ( null, JIFlags.FLAG_NULL ); //results --> currently all are null and this param is not required as the outparam carries this info.
        obj.addInParamAsObject ( null, JIFlags.FLAG_NULL ); //excepinfo --> currently all are null and this param is not required as the excepinfo is built here.
        obj.addInParamAsObject ( null, JIFlags.FLAG_NULL ); //augerr --> currently all are null and this param is not required as the excepinfo is built here.

        final Object[] outparams = new Object[4];
        if ( outParamType == null )
        {
            outparams[0] = JIVariant.class; //fill ourselves
        }
        else
        {
            outparams[0] = outParamType; //fill from users input
        }

        outparams[1] = excepInfo;
        outparams[2] = new JIPointer ( Integer.class, true );
        outparams[3] = new JIArray ( JIVariant.class, null, 1, true );

        obj.setOutParams ( outparams, JIFlags.FLAG_REPRESENTATION_IDISPATCH_INVOKE );

        Object[] result = null;
        try
        {
            result = this.comObject.call ( obj );
        }
        catch ( final JIException e )
        {
            final Object[] results = obj.getResultsInCaseOfException ();
            if ( results != null )
            {
                //catching here so that an extended message could be sent out
                final JIStruct excepInfoRet = (JIStruct)results[1];
                final String text1 = ( (JIString)excepInfoRet.getMember ( 2 ) ).getString () + " ";
                final String text2 = ( (JIString)excepInfoRet.getMember ( 3 ) ).getString () + " [ ";
                final String text3 = ( (JIString)excepInfoRet.getMember ( 4 ) ).getString () + " ] ";
                this.lastExcepInfo.excepDesc = text2;
                this.lastExcepInfo.excepHelpfile = text3;
                this.lastExcepInfo.excepSource = text1;
                this.lastExcepInfo.errorCode = ( (Short)excepInfoRet.getMember ( 0 ) ).intValue () != 0 ? ( (Short)excepInfoRet.getMember ( 0 ) ).intValue () : ( (Integer)excepInfoRet.getMember ( 8 ) ).intValue ();

                final JIAutomationException automationException = new JIAutomationException ( e );
                automationException.setExcepInfo ( this.lastExcepInfo );
                throw automationException;
                //				throw new JIException(obj.getHRESULT(),JISystem.getLocalizedMessage(obj.getHRESULT()) + " ==> Message from Server: " +
                //				text1 + text2 + text3);
            }
            else
            {
                throw e;
            }
        }

        final JIArray array = (JIArray)result[3];
        final JIVariant[] byrefVariants = (JIVariant[])array.getArrayInstance (); //will be a sinlge dimensional array.

        final JIVariant[] retVal = new JIVariant[1 + byrefVariants.length];
        retVal[0] = (JIVariant)result[0];
        System.arraycopy ( byrefVariants, 0, retVal, 1, byrefVariants.length );

        return retVal;
    }

    private void put ( final int dispId, final Object[] inparams, final boolean isRef ) throws JIException
    {
        final int propertyFlag = isRef ? IJIDispatch.DISPATCH_PROPERTYPUTREF : IJIDispatch.DISPATCH_PROPERTYPUT;
        Object[] objectParams = inparams;
        if ( objectParams == null )
        {
            objectParams = new Object[0];
        }

        final JIVariant[] variants = new JIVariant[objectParams.length];
        for ( int i = 0; i < objectParams.length; i++ )
        {
            JIVariant variant = null;
            final Object obj = objectParams[i];
            if ( ! ( obj instanceof JIVariant ) )
            {
                if ( obj instanceof JIArray )
                {
                    variant = new JIVariant ( (JIArray)obj, isRef );
                }
                else
                {
                    variant = JIVariant.makeVariant ( obj, isRef );
                }

            }
            else
            {
                variant = (JIVariant)obj;
                //variant = new JIVariant((JIVariant)obj);
            }

            variants[i] = variant;
        }

        invoke ( dispId, propertyFlag, new JIArray ( variants, true ), new JIArray ( new Integer[] { new Integer ( DISPATCH_DISPID_PUTPUTREF ) }, true ), null );
        //invoke(dispId,propertyFlag,new JIArray(new JIVariant[]{inparam},true),new JIArray(new Integer[]{new Integer(propertyFlag)},true),null);
    }

    @Override
    public void put ( final int dispId, final JIVariant inparam ) throws JIException
    {
        put ( dispId, new Object[] { inparam }, false );
    }

    @Override
    public void put ( final String name, final JIVariant inparam ) throws JIException
    {
        put ( getIDsOfNames ( name ), inparam );
    }

    @Override
    public void putRef ( final int dispId, final JIVariant inparam ) throws JIException
    {
        put ( dispId, new Object[] { inparam }, true );
    }

    @Override
    public void putRef ( final String name, final JIVariant inparam ) throws JIException
    {
        putRef ( getIDsOfNames ( name ), inparam );
    }

    @Override
    public JIVariant get ( final int dispId ) throws JIException
    {
        //return invoke(dispId,IJIDispatch.DISPATCH_PROPERTYGET,new Object[]{null,null,null,null,null},null);
        return invoke ( dispId, IJIDispatch.DISPATCH_PROPERTYGET, null, null, null )[0];
    }

    @Override
    public JIVariant[] get ( final int dispId, final Object[] inparams ) throws JIException
    {
        return callMethodA ( dispId, inparams, IJIDispatch.DISPATCH_PROPERTYGET );
    }

    @Override
    public JIVariant[] get ( final String name, final Object[] inparams ) throws JIException
    {
        //return invoke(dispId,IJIDispatch.DISPATCH_PROPERTYGET,new Object[]{null,null,null,null,null},null);
        return get ( getIDsOfNames ( name ), inparams );
    }

    @Override
    public JIVariant get ( final String name ) throws JIException
    {
        //return invoke(getIDsOfNames(name),IJIDispatch.DISPATCH_PROPERTYGET,new Object[]{null,null,null,null,null},null);
        return get ( getIDsOfNames ( name ) );
    }

    @Override
    public void callMethod ( final String name ) throws JIException
    {
        //invoke(getIDsOfNames(name),IJIDispatch.DISPATCH_METHOD,null,null,null);
        callMethod ( getIDsOfNames ( name ) );
    }

    @Override
    public void callMethod ( final int dispId ) throws JIException
    {
        //invoke(dispId,IJIDispatch.DISPATCH_METHOD,new Object[]{null,null,null,null,null},null);
        callMethodA ( dispId );
    }

    @Override
    public JIVariant callMethodA ( final String name ) throws JIException
    {
        //return invoke(getIDsOfNames(name),IJIDispatch.DISPATCH_METHOD,new Object[]{null,null,null,null,null},null);
        return callMethodA ( getIDsOfNames ( name ) );
    }

    @Override
    public JIVariant callMethodA ( final int dispId ) throws JIException
    {
        //return invoke(dispId,IJIDispatch.DISPATCH_METHOD,new Object[]{null,null,null,null,null},null);
        return invoke ( dispId, IJIDispatch.DISPATCH_METHOD, null, null, null )[0];
    }

    //Ordinary params, will internally form Variant and the JIArray associated
    @Override
    public void callMethod ( final String name, final Object[] inparams ) throws JIException
    {
        callMethodA ( getIDsOfNames ( name ), inparams );
    }

    //	Ordinary params, will internally form Variant and the JIArray associated
    @Override
    public void callMethod ( final int dispId, final Object[] inparams ) throws JIException
    {
        callMethodA ( dispId, inparams );
    }

    //	Ordinary params, will internally form Variant and the JIArray associated
    @Override
    public JIVariant[] callMethodA ( final String name, final Object[] inparams ) throws JIException
    {
        return callMethodA ( getIDsOfNames ( name ), inparams );
    }

    private JIVariant[] callMethodA ( final int dispId, final Object[] inparams, final int FLAG ) throws JIException
    {
        Object[] objectParams = inparams;
        if ( objectParams == null )
        {
            objectParams = new Object[0];
        }

        final JIVariant[] variants = new JIVariant[objectParams.length];
        for ( int i = 0; i < objectParams.length; i++ )
        {
            JIVariant variant = null;
            final Object obj = objectParams[i];
            if ( ! ( obj instanceof JIVariant ) )
            {
                if ( obj instanceof JIArray )
                {
                    variant = new JIVariant ( (JIArray)obj );
                }
                else
                {
                    variant = JIVariant.makeVariant ( obj );
                }

            }
            else
            {
                variant = (JIVariant)obj;
                //variant = new JIVariant((JIVariant)obj);
            }

            variants[i] = variant;
        }

        //		Integer[] array = new Integer[inparams.length];
        //		//now prepare the JIArray of dispIds.
        //		System.arraycopy(arrayOfDispIds,0,array,0,inparams.length);
        //		JIArray arrayOfValues = new JIArray(array,true);

        return invoke ( dispId, FLAG, new JIArray ( variants, true ), null, null );
    }

    //	Ordinary params, will internally form Variant and the JIArray associated
    @Override
    public JIVariant[] callMethodA ( final int dispId, final Object[] inparams ) throws JIException
    {
        return callMethodA ( dispId, inparams, IJIDispatch.DISPATCH_METHOD );
    }

    @Override
    public void callMethod ( final String name, final Object[] inparams, final int[] dispIds ) throws JIException
    {
        callMethodA ( getIDsOfNames ( name ), inparams, dispIds );
    }

    //inparams.length == dispIds.length.
    @Override
    public void callMethod ( final int dispId, final Object[] inparams, final int[] dispIds ) throws JIException
    {
        callMethodA ( dispId, inparams, dispIds );
    }

    //inparams.length == dispIds.length.
    @Override
    public JIVariant[] callMethodA ( final String name, final Object[] inparams, final int[] dispIds ) throws JIException
    {
        return callMethodA ( getIDsOfNames ( name ), inparams, dispIds );
    }

    //if inparams == null, dispIds is not considered
    @Override
    public JIVariant[] callMethodA ( final int dispId, final Object[] inparams, final int[] dispIds ) throws JIException
    {
        if ( inparams == null || inparams.length == 0 )
        {
            return callMethodA ( dispId, inparams );
        }

        if ( dispIds == null || dispIds.length != inparams.length )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_DISP_INCORRECT_PARAM_LENGTH ) );
        }

        final Integer[] array = new Integer[inparams.length];
        //now prepare the JIArray of dispIds.
        for ( int i = 0; i < inparams.length; i++ )
        {
            array[i] = new Integer ( dispIds[i] );
        }

        final JIArray arrayOfValues = new JIArray ( array, true );

        final JIVariant[] variants = new JIVariant[inparams.length];
        for ( int i = 0; i < inparams.length; i++ )
        {
            JIVariant variant = null;
            final Object obj = inparams[i];
            if ( ! ( obj instanceof JIVariant ) )
            {
                if ( obj instanceof JIArray )
                {
                    variant = new JIVariant ( (JIArray)obj );
                }
                else
                {
                    variant = JIVariant.makeVariant ( obj );
                }
            }
            else
            {
                variant = (JIVariant)obj;
                //variant = new JIVariant((JIVariant)obj);
            }

            variants[i] = variant;
        }

        return invoke ( dispId, IJIDispatch.DISPATCH_METHOD, new JIArray ( variants, true ), arrayOfValues, null );

    }

    @Override
    public void callMethod ( final String name, final Object[] inparams, final String[] paramNames ) throws JIException
    {
        callMethodA ( name, inparams, paramNames );
    }

    @Override
    public JIVariant[] callMethodA ( final String name, final Object[] inparams, final String[] paramNames ) throws JIException
    {
        if ( inparams == null || inparams.length == 0 )
        {
            return callMethodA ( getIDsOfNames ( name ), inparams );
        }

        if ( paramNames == null || paramNames.length != inparams.length )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_DISP_INCORRECT_PARAM_LENGTH ) );
        }

        final String[] names = new String[paramNames.length + 1];
        names[0] = name;
        System.arraycopy ( paramNames, 0, names, 1, paramNames.length );
        final int[] dispIds = getIDsOfNames ( names );

        final int[] newDispIds = new int[dispIds.length - 1];

        for ( int i = 0; i < newDispIds.length; i++ )
        {
            newDispIds[i] = dispIds[i + 1]; //skip the apiname
        }

        return callMethodA ( dispIds[0], inparams, newDispIds );
    }

    private static final Integer[] arrayOfDispIds = new Integer[100];

    private static final JIStruct excepInfo = new JIStruct ();
    static
    {
        try
        {
            excepInfo.addMember ( Short.class );
            excepInfo.addMember ( Short.class );
            excepInfo.addMember ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ) );
            excepInfo.addMember ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ) );
            excepInfo.addMember ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ) );
            excepInfo.addMember ( Integer.class );
            excepInfo.addMember ( new JIPointer ( null, true ) );
            excepInfo.addMember ( new JIPointer ( null, true ) );
            excepInfo.addMember ( Integer.class );
        }
        catch ( final JIException e )
        {
            logger.error ( "static initializer", e );
        }
        for ( int i = 0; i < 100; i++ )
        {
            arrayOfDispIds[i] = new Integer ( i );
        }

    }

    @Override
    public void put ( final int dispId, final Object[] params ) throws JIException
    {
        put ( dispId, params, false );
    }

    @Override
    public void put ( final String name, final Object[] params ) throws JIException
    {
        put ( getIDsOfNames ( name ), params, false );
    }

    @Override
    public void putRef ( final int dispId, final Object[] params ) throws JIException
    {
        put ( dispId, params, true );
    }

    @Override
    public void putRef ( final String name, final Object[] params ) throws JIException
    {
        put ( getIDsOfNames ( name ), params, true );
    }

    @Override
    public JIExcepInfo getLastExcepInfo ()
    {
        return this.lastExcepInfo;
    }

    @Override
    public String toString ()
    {
        return "IJIDispatch[" + super.toString () + "]";
    }
}
