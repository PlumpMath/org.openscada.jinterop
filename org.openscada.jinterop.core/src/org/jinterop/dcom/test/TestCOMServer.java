package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class TestCOMServer
{

    private JIComServer comStub = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public TestCOMServer ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );

        //instead of this the ProgID "TestCOMServer.ITestCOMServer"	can be used as well.
        //comStub = new JIComServer(JIProgId.valueOf(session,"TestCOMServer.ITestCOMServer"),address,session);
        //CLSID of ITestCOMServer
        this.comStub = new JIComServer ( JIClsid.valueOf ( "44A9CD09-0D9B-4FD2-9B8A-0151F2E0CAD1" ), address, session );
    }

    public void execute () throws JIException
    {
        this.unknown = this.comStub.createInstance ();
        //CLSID of IITestCOMServer
        final IJIComObject comObject = this.unknown.queryInterface ( "4AE62432-FD04-4BF9-B8AC-56AA12A47FF9" );
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( comObject.queryInterface ( IJIDispatch.IID ) );

        //Now call via automation
        Object results[] = this.dispatch.callMethodA ( "Add", new Object[] { new Integer ( 1 ), new Integer ( 2 ), new JIVariant ( 0, true ) } );
        System.out.println ( results[1] );

        //now without automation
        final JICallBuilder callObject = new JICallBuilder ();
        callObject.setOpnum ( 1 );//obtained from the IDL or TypeLib.
        callObject.addInParamAsInt ( 1, JIFlags.FLAG_NULL );
        callObject.addInParamAsInt ( 2, JIFlags.FLAG_NULL );
        callObject.addInParamAsPointer ( new JIPointer ( new Integer ( 0 ) ), JIFlags.FLAG_NULL );
        //Since the retval is a top level pointer , it will get replaced with it's base type.
        callObject.addOutParamAsObject ( Integer.class, JIFlags.FLAG_NULL );
        results = comObject.call ( callObject );
        System.out.println ( results[0] );
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
            final TestCOMServer test = new TestCOMServer ( args[0], args );
            test.execute ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
