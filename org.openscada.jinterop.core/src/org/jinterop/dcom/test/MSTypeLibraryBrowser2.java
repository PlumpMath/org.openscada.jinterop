package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.ElemDesc;
import org.jinterop.dcom.impls.automation.FuncDesc;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJITypeInfo;
import org.jinterop.dcom.impls.automation.IJITypeLib;
import org.jinterop.dcom.impls.automation.ImplTypeFlags;
import org.jinterop.dcom.impls.automation.TypeAttr;
import org.jinterop.dcom.impls.automation.TypeDesc;
import org.jinterop.dcom.impls.automation.TypeKind;
import org.jinterop.dcom.impls.automation.VarDesc;

public class MSTypeLibraryBrowser2
{

    private JIComServer comServer = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public MSTypeLibraryBrowser2 ( final String address, final String args[] ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );
        session.useSessionSecurity ( true );
        this.comServer = new JIComServer ( JIProgId.valueOf ( args[4] ), address, session );
    }

    public void start () throws JIException
    {
        this.unknown = this.comServer.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
        final IJITypeLib typeLib = (IJITypeLib)this.dispatch.getTypeInfo ( 0 ).getContainingTypeLib ()[0];
        Object[] result = typeLib.getDocumentation ( -1 );
        System.out.println ( "Name: " + ( (JIString)result[0] ).getString () );
        System.out.println ( "Library Name: " + ( (JIString)result[1] ).getString () );
        System.out.println ( "Full path to help file: " + ( (JIString)result[3] ).getString () );
        System.out.println ( "\n------------------------Library Members---------------------" );
        final int typeInfoCount = typeLib.getTypeInfoCount ();
        final String g_arrClassification[] = { "Enum", "Struct", "Module", "Interface", "Dispinterface", "Coclass", "Typedef", "Union" };
        for ( int l = 0; l < typeInfoCount; l++ )
        {
            System.out.println ( "\n\n-----------------------Member Description--------------------------" );
            result = typeLib.getDocumentation ( l );
            final int k = typeLib.getTypeInfoType ( l );

            System.out.println ( "Name: " + ( (JIString)result[0] ).getString () );
            System.out.println ( "Type: " + g_arrClassification[k] );

            IJITypeInfo typeInfo = typeLib.getTypeInfo ( l );
            TypeAttr typeAttr = typeInfo.getTypeAttr ();
            IJITypeInfo ptempInfo = null;
            TypeAttr pTempAttr = null;
            if ( typeAttr.typekind != TypeKind.TKIND_DISPATCH.intValue () && typeAttr.typekind != TypeKind.TKIND_COCLASS.intValue () )
            {
                int p = 0;
                p++;
            }

            if ( typeAttr.typekind == TypeKind.TKIND_COCLASS.intValue () )
            {

                for ( int i = 0; i < typeAttr.cImplTypes; i++ )
                {
                    int nFlags = -1;
                    try
                    {
                        nFlags = typeInfo.getImplTypeFlags ( i );
                    }
                    catch ( final JIException e )
                    {
                        continue;
                    }

                    if ( ( nFlags & ImplTypeFlags.IMPLTYPEFLAG_FDEFAULT ) == ImplTypeFlags.IMPLTYPEFLAG_FDEFAULT )
                    {
                        int hRefType = -1;
                        try
                        {
                            hRefType = typeInfo.getRefTypeOfImplType ( i );
                        }
                        catch ( final JIException e )
                        {
                            break;
                        }

                        try
                        {
                            ptempInfo = typeInfo.getRefTypeInfo ( hRefType );
                        }
                        catch ( final JIException e )
                        {
                            break;
                        }

                        try
                        {
                            pTempAttr = ptempInfo.getTypeAttr ();
                        }
                        catch ( final JIException e )
                        {
                            System.out.println ( "Failed to get reference type info." );
                            return;
                        }
                    }
                }

            }

            if ( pTempAttr != null )
            {
                typeInfo = ptempInfo;
                typeAttr = pTempAttr;
            }

            final int m_nMethodCount = typeAttr.cFuncs;
            final int m_nVarCount = typeAttr.cVars;
            final int m_nDispInfoCount = m_nMethodCount + 2 * m_nVarCount;
            System.out.println ( "Method and variable count = " + m_nMethodCount + m_nVarCount + "\n\n" );

            for ( int i = 0; i < m_nMethodCount; i++ )
            {
                System.out.println ( "************Method Seperator*****************" );
                FuncDesc pFuncDesc;

                try
                {
                    pFuncDesc = typeInfo.getFuncDesc ( i );
                }
                catch ( final JIException e )
                {
                    e.printStackTrace ();
                    return;
                }

                System.out.println ( i + ": DispID = " + pFuncDesc.memberId );

                int nCount;
                try
                {
                    final Object[] ret = typeInfo.getNames ( pFuncDesc.memberId, 1 );
                    System.out.println ( "MethodName = " + ( (JIString) ( (Object[]) ( (JIArray)ret[0] ).getArrayInstance () )[0] ).getString () );
                    nCount = ( (Integer)ret[1] ).intValue ();
                }
                catch ( final JIException e )
                {
                    System.out.println ( "GetNames failed." );
                    return;
                }

                switch ( pFuncDesc.invokeKind )
                {

                    case 2://InvokeKind.INVOKE_PROPERTYGET.intValue():
                        System.out.println ( "PropertyGet" );
                        break;
                    case 4://InvokeKind.INVOKE_PROPERTYPUT.intValue():
                        System.out.println ( "PropertyPut" );
                        break;
                    case 8://InvokeKind.INVOKE_PROPERTYPUTREF.intValue():
                        System.out.println ( "PropertyPutRef" );
                        break;
                    case 1://InvokeKind.INVOKE_FUNC.intValue():
                        System.out.println ( "DispatchMethod" );
                        break;
                    default:
                        break;
                }

                System.out.println ( "VTable offset: " + pFuncDesc.oVft );
                System.out.println ( "Calling convention: " + pFuncDesc.callConv );
                //TODO need to return a string representation of this.
                System.out.println ( "Return type = " + pFuncDesc.elemdescFunc.typeDesc.vt );
                System.out.println ( "ParamCount = " + pFuncDesc.cParams );
                final JIArray array = (JIArray)pFuncDesc.lprgelemdescParam.getReferent ();
                ElemDesc[] types = null;
                if ( array != null )
                {
                    final Object[] temp = (Object[])array.getArrayInstance ();
                    types = new ElemDesc[temp.length];
                    for ( int k1 = 0; k1 < temp.length; k1++ )
                    {
                        types[k1] = new ElemDesc ( (JIStruct)temp[k1] );
                    }
                }

                for ( int j = 0; j < pFuncDesc.cParams; j++ )
                {

                    if ( types[j].typeDesc.vt == TypeDesc.VT_SAFEARRAY.shortValue () )
                    {
                        System.out.println ( "Param(" + j + ") type = SafeArray" );
                    }
                    else if ( types[j].typeDesc.vt == TypeDesc.VT_PTR.shortValue () )
                    {
                        System.out.println ( "Param(" + j + ") type = Pointer" );
                    }
                    else
                    {
                        System.out.println ( "Param(" + j + ") type = UserDefined" );
                    }
                }
            }

            for ( int i = m_nMethodCount; i < m_nMethodCount + m_nVarCount; i++ )
            {
                System.out.println ( "************Variable Seperator*****************" );
                VarDesc pVarDesc;
                try
                {
                    pVarDesc = typeInfo.getVarDesc ( i - m_nMethodCount );
                }
                catch ( final JIException e )
                {
                    System.out.println ( "GetVarDesc failed." );
                    return;
                }

                System.out.println ( i + ": DispID = " + pVarDesc.memberId );

                int nCount;
                try
                {
                    final Object[] ret = typeInfo.getNames ( pVarDesc.memberId, 1 );
                    System.out.println ( "VarName = " + ( (JIString) ( (Object[]) ( (JIArray)ret[0] ).getArrayInstance () )[0] ).getString () );
                    nCount = ( (Integer)ret[1] ).intValue ();
                }
                catch ( final JIException e )
                {
                    System.out.println ( "GetNames failed." );
                    return;
                }

                switch ( pVarDesc.varkind )
                {
                    case VarDesc.VAR_DISPATCH:
                        System.out.println ( "VarKind = VAR_DISPATCH" );
                        System.out.println ( "VarType = " + pVarDesc.elemdescVar.typeDesc.vt );
                        break;
                    default:
                        //TODO resolve to it's string representation
                        System.out.println ( "VarKind = " + pVarDesc.varkind );
                        break;
                }
            }
        }

        System.out.println ( "########################Execution complete#########################" );
        JISession.destroySession ( this.dispatch.getAssociatedSession () );
    }

    public static void main ( final String[] args )
    {
        try
        {
            if ( args.length < 5 )
            {
                System.out.println ( "Please provide address domain username password progIdOfApplication" );
                return;
            }
            // JR: JISystem.getLogger ().setLevel ( Level.OFF );
            // JR: configure using slf4j now
            JISystem.setInBuiltLogHandler ( false );
            final MSTypeLibraryBrowser2 typeLibraryBrowser = new MSTypeLibraryBrowser2 ( args[0], args );
            typeLibraryBrowser.start ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }

    }

}
