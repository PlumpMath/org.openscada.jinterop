package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class WshShell
{

    private final int xlWorksheet = -4167;

    private final int xlXYScatterLinesNoMarkers = 75;

    private final int xlColumns = 2;

    private JIComServer comServer = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    private final IJIDispatch dispatchOfWorkSheet = null;

    private final IJIDispatch dispatchOfWorkBook = null;

    private JISession session = null;

    public WshShell ( final String address, final String domain, final String username, final String password ) throws JIException, UnknownHostException
    {

        // JR: JISystem.getLogger ().setLevel ( Level.SEVERE );
        // JR: configure using slf4j now

        this.session = JISession.createSession ( domain, username, password );

        this.comServer = new JIComServer ( JIProgId.valueOf ( "WScript.Shell" ), address, this.session );

    }

    public void startWScript () throws JIException

    {

        System.out.println ( this.comServer.getProperties () );

        this.unknown = this.comServer.createInstance ();

        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );

        JIVariant jv = this.dispatch.get ( "CurrentDirectory" );

        System.out.println ( jv.getObjectAsString ().getString () );

        final int dispId = this.dispatch.getIDsOfNames ( "CurrentDirectory" );

        System.out.println ( dispId );

        final JIVariant variant = new JIVariant ( "C://WINDOWS" );

        this.dispatch.put ( dispId, variant );

        jv = this.dispatch.get ( "CurrentDirectory" );

        System.out.println ( jv.getObjectAsString ().getString () );

        try
        {
            Thread.sleep ( 60 * 1000 * 3 );
        }
        catch ( final InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }

        //WshShell.Exec

        System.out.println ( this.dispatch.callMethodA ( "Exec", new Object[] { new JIString ( "calc" ) } )[0] );

        try
        {
            Thread.sleep ( 60 * 1000 * 3 );
        }
        catch ( final InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
        //WshShell.Run

        System.out.println ( this.dispatch.callMethodA ( "Run", new Object[] { new JIString ( "notepad" ), new JIVariant ( 10 ), JIVariant.OPTIONAL_PARAM () } )[0] );

        //JISession.destroySession(session);

    }

    public static void main ( final String[] args )
    {

        try
        {

            JISystem.setAutoRegisteration ( true );

            final WshShell wScript = new WshShell ( "localhost", "domain", "username", "password" );

            wScript.startWScript ();

        }
        catch ( final Exception e )
        {

            // TODO Auto-generated catch block

            e.printStackTrace ();

        }

    }

}