package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.FuncDesc;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJITypeInfo;
import org.jinterop.dcom.impls.automation.IJITypeLib;
import org.jinterop.dcom.impls.automation.TypeAttr;
import org.jinterop.dcom.impls.automation.VarDesc;

public class MSTypeLibraryBrowser
{

    private JIComServer comServer = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public MSTypeLibraryBrowser ( final String address, final String args[] ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );
        this.comServer = new JIComServer ( JIProgId.valueOf ( "InternetExplorer.Application" ), address, session );
    }

    public void start () throws JIException
    {
        this.unknown = this.comServer.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
        final IJITypeInfo typeInfo = this.dispatch.getTypeInfo ( 0 );
        final IJITypeLib typeLib = (IJITypeLib)typeInfo.getContainingTypeLib ()[0];
        Object[] result = typeLib.getDocumentation ( -1 );
        System.out.println ( ( (JIString)result[0] ).getString () );
        System.out.println ( ( (JIString)result[1] ).getString () );
        System.out.println ( ( (JIString)result[3] ).getString () );
        System.out.println ( "-------------------------------" );
        final int typeInfoCount = typeLib.getTypeInfoCount ();
        int i = 0;
        final String g_arrClassification[] = { "Enum", "Struct", "Module", "Interface", "Dispinterface", "Coclass", "Typedef", "Union" };
        for ( ; i < typeInfoCount; i++ )
        {
            result = typeLib.getDocumentation ( i );
            int j = typeLib.getTypeInfoType ( i );

            System.out.println ( ( (JIString)result[0] ).getString () );
            System.out.println ( ( (JIString)result[1] ).getString () );
            System.out.println ( ( (JIString)result[3] ).getString () );
            System.out.println ( g_arrClassification[j] );

            final IJITypeInfo typeInfo2 = typeLib.getTypeInfo ( i );
            final TypeAttr typeAttr = typeInfo2.getTypeAttr ();
            for ( j = 0; j < typeAttr.cFuncs; j++ )
            {
                final FuncDesc funcDesc = typeInfo2.getFuncDesc ( j );
                result = typeInfo2.getDocumentation ( funcDesc.memberId );
                System.out.println ( ( (JIString)result[0] ).getString () );
                System.out.println ( ( (JIString)result[1] ).getString () );
                System.out.println ( ( (JIString)result[3] ).getString () );
            }

            for ( j = 0; j < typeAttr.cVars; j++ )
            {
                if ( j == 77 )
                {
                    final int kk = 0;
                }
                final VarDesc varDesc = typeInfo2.getVarDesc ( j );
                result = typeInfo2.getDocumentation ( varDesc.memberId );
                System.out.println ( ( (JIString)result[0] ).getString () );
                System.out.println ( ( (JIString)result[1] ).getString () );
                System.out.println ( ( (JIString)result[3] ).getString () );
                //System.out.println(j);
            }

            System.out.println ( "***************************************" );
        }
        JISession.destroySession ( this.dispatch.getAssociatedSession () );
    }

    public static void main ( final String[] args )
    {
        try
        {
            if ( args.length < 4 )
            {
                System.out.println ( "Please provide address domain username password" );
                return;
            }
            final MSTypeLibraryBrowser typeLibraryBrowser = new MSTypeLibraryBrowser ( args[0], args );
            typeLibraryBrowser.start ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }

    }

}
