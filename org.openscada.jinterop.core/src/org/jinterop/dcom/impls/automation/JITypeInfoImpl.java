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

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIComObjectImplWrapper;
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.core.JIUnion;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;

import rpc.core.UUID;

/**
 * @exclude
 * @since 1.0
 */
final class JITypeInfoImpl extends JIComObjectImplWrapper implements IJITypeInfo
{

    /**
	 *
	 */
    private static final long serialVersionUID = 693590689068822035L;

    //IJIComObject comObject = null;
    //JIRemUnknown unknown = null;
    JITypeInfoImpl ( final IJIComObject comObject/*, JIRemUnknown unknown*/)
    {
        super ( comObject );
        //this.comObject = comObject;
    }

    @Override
    public FuncDesc getFuncDesc ( final int index ) throws JIException
    {

        //prepare the GO here

        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 2 );
        obj.addInParamAsInt ( index, JIFlags.FLAG_NULL );

        //now to prepare out params
        final JIStruct funcDescStruct = new JIStruct ();
        funcDescStruct.addMember ( Integer.class );
        funcDescStruct.addMember ( new JIPointer ( new JIArray ( Integer.class, null, 1, true ) ) );
        //first read the pointer representation. Do not want to use funcdesc but only describe
        //it. This should show the flexibility of the API.
        //TODO have to make a Pointer type which only reads the representation.
        obj.addOutParamAsObject ( new JIPointer ( funcDescStruct ), JIFlags.FLAG_NULL );

        //CLEANLOCALSTORAGE --> this is wrong, since CLEANLOCALSTORAGE is a struct, but it has always
        //come null and even if something comes, I don't know which pointer PVOID stands for.
        final JIStruct cleanlocalstorage = new JIStruct ();
        cleanlocalstorage.addMember ( Integer.class );
        cleanlocalstorage.addMember ( Integer.class );
        cleanlocalstorage.addMember ( Integer.class );
        obj.addOutParamAsObject ( new JIPointer ( cleanlocalstorage ), JIFlags.FLAG_NULL );

        //now for member id
        //obj.addOutParamAsType(Integer.class,JIFlags.FLAG_NULL);

        //now for lprgscode, Pointer to Conformant array of SCODEs (int)
        //obj.addOutParamAsObject(new Pointer(new JIArray(Integer.class,null,1,true)), JIFlags.FLAG_NULL);

        //now for lprgelemdescParam, Pointer to Conformant array of ELEMDESC (struct)
        //define the struct
        final JIStruct elemDesc = new JIStruct ();

        //SAFEARRAYBOUNDS
        final JIStruct safeArrayBounds = new JIStruct ();
        safeArrayBounds.addMember ( Integer.class );
        safeArrayBounds.addMember ( Integer.class );

        //arraydesc
        final JIStruct arrayDesc = new JIStruct ();
        //typedesc
        final JIStruct typeDesc = new JIStruct ();

        arrayDesc.addMember ( typeDesc );
        arrayDesc.addMember ( Short.class );
        arrayDesc.addMember ( new JIArray ( safeArrayBounds, new int[] { 1 }, 1, true ) );

        final JIUnion forTypeDesc = new JIUnion ( Short.class );
        final JIPointer ptrToTypeDesc = new JIPointer ( typeDesc );
        final JIPointer ptrToArrayDesc = new JIPointer ( arrayDesc );

        forTypeDesc.addMember ( TypeDesc.VT_PTR, ptrToTypeDesc );
        forTypeDesc.addMember ( TypeDesc.VT_SAFEARRAY, ptrToTypeDesc );
        forTypeDesc.addMember ( TypeDesc.VT_CARRAY, ptrToArrayDesc );
        forTypeDesc.addMember ( TypeDesc.VT_USERDEFINED, Integer.class );
        typeDesc.addMember ( forTypeDesc );
        typeDesc.addMember ( Short.class );//VARTYPE

        //PARAMDESC
        final JIStruct paramDesc2 = new JIStruct ();
        paramDesc2.addMember ( Integer.class );
        paramDesc2.addMember ( JIVariant.class );
        final JIStruct paramDesc = new JIStruct ();
        paramDesc.addMember ( new JIPointer ( paramDesc2, false ) );
        paramDesc.addMember ( Short.class );

        elemDesc.addMember ( typeDesc );
        elemDesc.addMember ( paramDesc );

        funcDescStruct.addMember ( new JIPointer ( new JIArray ( elemDesc, null, 1, true ) ) );
        //obj.addOutParamAsObject(new Pointer(new JIArray(elemDesc,null,1,true)), JIFlags.FLAG_NULL);

        //		obj.addOutParamAsObject(Integer.class,JIFlags.FLAG_NULL);
        //		obj.addOutParamAsObject(Integer.class,JIFlags.FLAG_NULL);
        //		obj.addOutParamAsObject(Integer.class,JIFlags.FLAG_NULL);
        //
        //		obj.addOutParamAsObject(Short.class,JIFlags.FLAG_NULL);
        //		obj.addOutParamAsObject(Short.class,JIFlags.FLAG_NULL);
        //
        //		obj.addOutParamAsObject(Short.class,JIFlags.FLAG_NULL);
        //		obj.addOutParamAsObject(Short.class,JIFlags.FLAG_NULL);
        //
        //		obj.addOutParamAsObject(elemDesc,JIFlags.FLAG_NULL);
        //		obj.addOutParamAsObject(Short.class,JIFlags.FLAG_NULL);

        funcDescStruct.addMember ( Integer.class );
        funcDescStruct.addMember ( Integer.class );
        funcDescStruct.addMember ( Integer.class );

        funcDescStruct.addMember ( Short.class );
        funcDescStruct.addMember ( Short.class );

        funcDescStruct.addMember ( Short.class );
        funcDescStruct.addMember ( Short.class );

        funcDescStruct.addMember ( elemDesc );
        funcDescStruct.addMember ( Short.class );

        final Object[] result = this.comObject.call ( obj );
        final FuncDesc funcDesc = new FuncDesc ( (JIPointer)result[0] );
        return funcDesc;
    }

    @Override
    public TypeAttr getTypeAttr () throws JIException
    {
        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 0 );

        final JIStruct typeAttr = new JIStruct ();
        final JIPointer mainPtr = new JIPointer ( typeAttr );
        obj.addOutParamAsObject ( mainPtr, JIFlags.FLAG_NULL );

        //CLEANLOCALSTORAGE --> this is wrong, since CLEANLOCALSTORAGE is a struct, but it has always
        //come null and even if something comes, I don't know which pointer PVOID stands for.
        obj.addOutParamAsObject ( new JIPointer ( Integer.class ), JIFlags.FLAG_NULL );

        typeAttr.addMember ( UUID.class );
        typeAttr.addMember ( Integer.class );
        typeAttr.addMember ( Integer.class );

        typeAttr.addMember ( Integer.class );
        typeAttr.addMember ( Integer.class );

        typeAttr.addMember ( new JIPointer ( new JIString ( null, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR ) ) );

        typeAttr.addMember ( Integer.class );

        typeAttr.addMember ( Integer.class );

        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );
        typeAttr.addMember ( Short.class );

        final JIStruct typeDesc = new JIStruct ();
        final JIStruct arrayDesc = new JIStruct ();
        final JIStruct safeArrayBounds = new JIStruct ();

        safeArrayBounds.addMember ( Integer.class );
        safeArrayBounds.addMember ( Integer.class );

        arrayDesc.addMember ( typeDesc );
        arrayDesc.addMember ( Short.class );
        arrayDesc.addMember ( new JIArray ( safeArrayBounds, new int[] { 1 }, 1, true ) );

        final JIUnion forTypeDesc = new JIUnion ( Short.class );
        final JIPointer ptrToTypeDesc = new JIPointer ( typeDesc );
        final JIPointer ptrToArrayDesc = new JIPointer ( arrayDesc );

        forTypeDesc.addMember ( TypeDesc.VT_PTR, ptrToTypeDesc );
        forTypeDesc.addMember ( TypeDesc.VT_SAFEARRAY, ptrToTypeDesc );
        forTypeDesc.addMember ( TypeDesc.VT_CARRAY, ptrToArrayDesc );
        forTypeDesc.addMember ( TypeDesc.VT_USERDEFINED, Integer.class );
        typeDesc.addMember ( forTypeDesc );
        typeDesc.addMember ( Short.class );//VARTYPE

        typeAttr.addMember ( typeDesc );

        final JIStruct paramDesc = new JIStruct ();
        paramDesc.addMember ( new JIPointer ( JIVariant.class, false ) );
        paramDesc.addMember ( Short.class );

        typeAttr.addMember ( paramDesc );

        final Object[] result = this.comObject.call ( obj );
        final TypeAttr attr = new TypeAttr ( (JIPointer)result[0] );
        return attr;
    }

    @Override
    public Object[] getContainingTypeLib () throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.addOutParamAsObject ( IJIComObject.class, JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( Integer.class, JIFlags.FLAG_NULL );
        callObject.setOpnum ( 15 );
        final Object[] result = this.comObject.call ( callObject );
        final Object[] retVal = new Object[2];
        retVal[0] = JIObjectFactory.narrowObject ( (IJIComObject)result[0] );
        retVal[1] = result[1];
        return retVal;
    }

    //	HRESULT GetDllEntry(
    //			  MEMBERID  memid,
    //			  InvokeKind  invKind,
    //			  BSTR FAR*  pBstrDllName,
    //			  BSTR FAR*  pBstrName,
    //			  unsigned short FAR*  pwOrdinal
    //			);
    @Override
    public Object[] getDllEntry ( final int memberId, final int invKind ) throws JIException
    {
        if ( invKind != InvokeKind.INVOKE_FUNC.intValue () && invKind != InvokeKind.INVOKE_PROPERTYGET.intValue () && invKind != InvokeKind.INVOKE_PROPERTYPUTREF.intValue () && invKind != InvokeKind.INVOKE_PROPERTYPUT.intValue () )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.E_INVALIDARG ) );
        }

        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.addInParamAsInt ( memberId, JIFlags.FLAG_NULL );
        callObject.addInParamAsInt ( invKind, JIFlags.FLAG_NULL );
        callObject.addInParamAsInt ( 1, JIFlags.FLAG_NULL );//refPtrFlags , as per the oaidl.idl...
        callObject.addOutParamAsObject ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( Short.class, JIFlags.FLAG_NULL );
        callObject.setOpnum ( 10 );
        return this.comObject.call ( callObject );
    }

    //	HRESULT GetDocumentation(
    //			  MEMBERID  memid,
    //			  BSTR FAR*  pBstrName,
    //			  BSTR FAR*  pBstrDocString,
    //			  unsigned long FAR*  pdwHelpContext,
    //			  BSTR FAR*  pBstrHelpFile
    //			);
    @Override
    public Object[] getDocumentation ( final int memberId ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.addInParamAsInt ( memberId, JIFlags.FLAG_NULL );
        callObject.addInParamAsInt ( 0xb, JIFlags.FLAG_NULL );//refPtrFlags , as per the oaidl.idl...
        callObject.addOutParamAsObject ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( Integer.class, JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), JIFlags.FLAG_NULL );
        callObject.setOpnum ( 9 );
        return this.comObject.call ( callObject );
    }

    @Override
    public VarDesc getVarDesc ( final int index ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 3 );
        callObject.addInParamAsInt ( index, JIFlags.FLAG_NULL );

        //now build the vardesc
        final JIStruct vardesc = new JIStruct ();
        callObject.addOutParamAsObject ( new JIPointer ( vardesc ), JIFlags.FLAG_NULL );
        //CLEANLOCALSTORAGE --> this is wrong, since CLEANLOCALSTORAGE is a struct, but it has always
        //come null and even if something comes, I don't know which pointer PVOID stands for.
        final JIStruct cleanlocalstorage = new JIStruct ();
        cleanlocalstorage.addMember ( Integer.class );
        cleanlocalstorage.addMember ( Integer.class );
        cleanlocalstorage.addMember ( Integer.class );
        callObject.addOutParamAsObject ( new JIPointer ( cleanlocalstorage ), JIFlags.FLAG_NULL );

        vardesc.addMember ( Integer.class );//memberid
        vardesc.addMember ( new JIPointer ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR ) ) );

        final JIUnion union = new JIUnion ( Integer.class );
        union.addMember ( new Integer ( VarDesc.VAR_PERINSTANCE ), Integer.class );
        union.addMember ( new Integer ( VarDesc.VAR_DISPATCH ), Integer.class );
        union.addMember ( new Integer ( VarDesc.VAR_STATIC ), Integer.class );
        union.addMember ( new Integer ( VarDesc.VAR_CONST ), JIVariant.class );
        vardesc.addMember ( union );

        final JIStruct elemDesc = new JIStruct ();

        //SAFEARRAYBOUNDS
        final JIStruct safeArrayBounds = new JIStruct ();
        safeArrayBounds.addMember ( Integer.class );
        safeArrayBounds.addMember ( Integer.class );

        //arraydesc
        final JIStruct arrayDesc = new JIStruct ();
        //typedesc
        final JIStruct typeDesc = new JIStruct ();

        arrayDesc.addMember ( typeDesc );
        arrayDesc.addMember ( Short.class );
        arrayDesc.addMember ( new JIArray ( safeArrayBounds, new int[] { 1 }, 1, true ) );

        final JIUnion forTypeDesc = new JIUnion ( Short.class );
        final JIPointer ptrToTypeDesc = new JIPointer ( typeDesc );
        final JIPointer ptrToArrayDesc = new JIPointer ( arrayDesc );

        forTypeDesc.addMember ( TypeDesc.VT_PTR, ptrToTypeDesc );
        forTypeDesc.addMember ( TypeDesc.VT_SAFEARRAY, ptrToTypeDesc );
        forTypeDesc.addMember ( TypeDesc.VT_CARRAY, ptrToArrayDesc );
        forTypeDesc.addMember ( TypeDesc.VT_USERDEFINED, Integer.class );
        typeDesc.addMember ( forTypeDesc );
        typeDesc.addMember ( Short.class );//VARTYPE

        //PARAMDESC
        final JIStruct paramDesc2 = new JIStruct ();
        paramDesc2.addMember ( Integer.class );
        paramDesc2.addMember ( JIVariant.class );
        final JIStruct paramDesc = new JIStruct ();
        paramDesc.addMember ( new JIPointer ( paramDesc2, false ) );
        paramDesc.addMember ( Short.class );
        //		JIStruct paramDesc = new JIStruct();
        //		paramDesc.addMember(new JIPointer(JIVariant.class,false));
        //		//paramDesc.addMember(JIVariant.class);
        //		paramDesc.addMember(Short.class);

        elemDesc.addMember ( typeDesc );
        elemDesc.addMember ( paramDesc );

        vardesc.addMember ( elemDesc );
        vardesc.addMember ( Short.class );
        vardesc.addMember ( Integer.class );

        final Object[] result = this.comObject.call ( callObject );

        return new VarDesc ( (JIPointer)result[0] );

    }

    @Override
    public Object[] getNames ( final int memberId, final int maxNames ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 4 );

        //for experiment only
        //		JIArray arry = new JIArray(new Integer[]{new Integer(100),new Integer(200)},true);
        //		JIStruct struct = new JIStruct();
        //		struct.addMember(Short.valueOf((short)86));
        //		struct.addMember(arry);
        //		callObject.addInParamAsStruct(struct,JIFlags.FLAG_NULL);

        callObject.addInParamAsInt ( memberId, JIFlags.FLAG_NULL );
        callObject.addInParamAsInt ( maxNames, JIFlags.FLAG_NULL );

        callObject.addOutParamAsObject ( new JIArray ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), null, 1, true, true ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsType ( Integer.class, JIFlags.FLAG_NULL );

        return this.comObject.call ( callObject );
    }

    @Override
    public int getRefTypeOfImplType ( final int index ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 5 );
        callObject.addInParamAsInt ( index, JIFlags.FLAG_NULL );
        callObject.addOutParamAsType ( Integer.class, JIFlags.FLAG_NULL );
        return ( (Integer)this.comObject.call ( callObject )[0] ).intValue ();
    }

    @Override
    public int getImplTypeFlags ( final int index ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 6 );
        callObject.addInParamAsInt ( index, JIFlags.FLAG_NULL );
        callObject.addOutParamAsType ( Integer.class, JIFlags.FLAG_NULL );
        return ( (Integer)this.comObject.call ( callObject )[0] ).intValue ();
    }

    @Override
    public IJITypeInfo getRefTypeInfo ( final int hrefType ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 11 );
        callObject.addInParamAsInt ( hrefType, JIFlags.FLAG_NULL );
        callObject.addOutParamAsType ( IJIComObject.class, JIFlags.FLAG_NULL );
        final Object[] result = this.comObject.call ( callObject );
        return (IJITypeInfo)JIObjectFactory.narrowObject ( (IJIComObject)result[0] );
    }

    //	public int[] getIdOfNames(String[] names) throws JIException
    //	{
    //		JICallBuilder callObject = new JICallBuilder(true);
    //		callObject.setOpnum(7);
    //
    //		JIPointer[] pointers = new JIPointer[names.length];
    //
    //		for (int i = 0;i < names.length;i++)
    //		{
    //			if (names[i] == null || names[i].trim().equals(""))
    //			{
    //				throw new IllegalArgumentException(JISystem.getLocalizedMessage(JIErrorCodes.JI_DISP_INCORRECT_VALUE_FOR_GETIDNAMES));
    //			}
    //			pointers[i] = new JIPointer(new JIString(names[i].trim(),JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR));
    //		}
    //
    //
    //		JIArray array = new JIArray(pointers,true);
    //		JIArray arrayOut = new JIArray(Integer.class,null,1,true);
    //
    //		callObject.addInParamAsArray(new JIArray(pointers,true),JIFlags.FLAG_NULL);
    //		callObject.addInParamAsInt(names.length,JIFlags.FLAG_NULL);
    //		callObject.addOutParamAsObject(arrayOut,JIFlags.FLAG_NULL);
    //
    //		Object[] result = comObject.call(callObject);
    //
    //		JIArray arrayOfResults = (JIArray)result[0];
    //		Integer[] arrayOfDispIds = (Integer[])arrayOfResults.getArrayInstance();
    //		int[] retVal = new int[names.length];
    //
    //		for (int i = 0;i < names.length;i++)
    //		{
    //			retVal[i] = arrayOfDispIds[i].intValue();
    //		}
    //
    //		return retVal;
    //
    //	}

    @Override
    public IJIComObject createInstance ( final String riid ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 13 );

        callObject.addInParamAsUUID ( riid, JIFlags.FLAG_NULL );
        callObject.addOutParamAsType ( IJIComObject.class, JIFlags.FLAG_NULL );
        final Object[] result = this.comObject.call ( callObject );
        return JIObjectFactory.narrowObject ( (IJIComObject)result[0] );
    }

    @Override
    public JIString getMops ( final int memberId ) throws JIException
    {
        final JICallBuilder callObject = new JICallBuilder ( true );
        callObject.setOpnum ( 14 );
        callObject.addInParamAsInt ( memberId, JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject ( new JIString ( JIFlags.FLAG_REPRESENTATION_STRING_BSTR ), JIFlags.FLAG_NULL );
        final Object[] result = this.comObject.call ( callObject );
        return (JIString)result[0];
    }
}
