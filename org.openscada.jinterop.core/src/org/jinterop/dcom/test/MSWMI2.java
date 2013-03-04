package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJIEnumVariant;

/**
 * WMI example showing how to use a new logger implementation.
 * 
 * @since 1.23
 */
public class MSWMI2
{

    private JIComServer comStub = null;

    private IJIComObject comObject = null;

    private IJIDispatch dispatch = null;

    private String address = null;

    private JISession session = null;

    public MSWMI2 ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        this.address = address;
        this.session = JISession.createSession ( args[1], args[2], args[3] );
        //		session.useSessionSecurity(true);
        //		session.setGlobalSocketTimeout(5000);
        this.comStub = new JIComServer ( JIClsid.valueOf ( "76a64158-cb41-11d1-8b02-00600806d9b6" ), address, this.session );
        final IJIComObject unknown = this.comStub.createInstance ();
        this.comObject = unknown.queryInterface ( "76A6415B-CB41-11d1-8B02-00600806D9B6" );//ISWbemLocator
        //This will obtain the dispatch interface
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.comObject.queryInterface ( IJIDispatch.IID ) );
    }

    public void performOp () throws JIException, InterruptedException
    {
        //		IJIDispatch securityDisp = (IJIDispatch)JIObjectFactory.narrowObject(dispatch.get("Security_").getObjectAsComObject());
        //		securityDisp.put("ImpersonationLevel", new JIVariant(3));
        JIVariant results[] = this.dispatch.callMethodA ( "ConnectServer", new Object[] { JIVariant.OPTIONAL_PARAM (), new JIString ( "ROOT\\CIMV2" ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), new Integer ( 0 ), JIVariant.OPTIONAL_PARAM () } );

        final IJIDispatch wbemServices_dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( results[0].getObjectAsComObject () );
        results = wbemServices_dispatch.callMethodA ( "ExecQuery", new Object[] { new JIString ( "select * from Win32_OperatingSystem where Primary=True" ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        final IJIDispatch wbemObjectSet_dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( results[0].getObjectAsComObject () );
        final JIVariant variant = wbemObjectSet_dispatch.get ( "_NewEnum" );
        final IJIComObject object2 = variant.getObjectAsComObject ();

        final IJIEnumVariant enumVARIANT = (IJIEnumVariant)JIObjectFactory.narrowObject ( object2.queryInterface ( IJIEnumVariant.IID ) );

        final JIVariant Count = wbemObjectSet_dispatch.get ( "Count" );
        final int count = Count.getObjectAsInt ();
        for ( int i = 0; i < count; i++ )
        {
            final Object[] values = enumVARIANT.next ( 1 );
            final JIArray array = (JIArray)values[0];
            final Object[] arrayObj = (Object[])array.getArrayInstance ();
            for ( int j = 0; j < arrayObj.length; j++ )
            {
                final IJIDispatch wbemObject_dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( ( (JIVariant)arrayObj[j] ).getObjectAsComObject () );
                final JIVariant variant2 = wbemObject_dispatch.callMethodA ( "GetObjectText_", new Object[] { new Integer ( 1 ) } )[0];
                System.out.println ( variant2.getObjectAsString ().getString () );
                System.out.println ( "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
            }
        }

    }

    private void killme () throws JIException
    {
        JISession.destroySession ( this.session );
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

            JISystem.setInBuiltLogHandler ( false );
            // JR: JISystem.getLogger ().setLevel ( Level.FINEST );
            // JR: configure using slf4j now
            JISystem.setAutoRegisteration ( true );
            final MSWMI2 test = new MSWMI2 ( args[0], args );
            for ( int i = 0; i < 2; i++ )
            {
                System.out.println ( "Index i: " + i );
                test.performOp ();
            }
            test.killme ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
