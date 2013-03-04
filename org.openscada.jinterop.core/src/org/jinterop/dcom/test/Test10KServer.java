package org.jinterop.dcom.test;

import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class Test10KServer
{

    private final JIComServer comStub = null;

    private final IJIDispatch dispatch = null;

    private final IJIComObject unknown = null;

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
            JISystem.setAutoRegisteration ( true );
            for ( int i = 0; i < 10000; ++i )
            {

                final JISession session = JISession.createSession ( args[1], args[2], args[3] );
                final JIComServer comServer = new JIComServer ( JIProgId.valueOf ( "MSMQ.MSMQQueueInfo" ), args[0], session );
                final IJIComObject unknown = comServer.createInstance ();
                final IJIDispatch dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( unknown.queryInterface ( IJIDispatch.IID ) );
                //JISession.destroySession(session);
                Thread.sleep ( 150 );
                if ( i % 100 == 0 )
                {
                    new String ();
                    System.out.println ( String.valueOf ( i ) );
                }
                System.gc ();
            }

        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
