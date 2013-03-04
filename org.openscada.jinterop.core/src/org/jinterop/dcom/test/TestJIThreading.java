package org.jinterop.dcom.test;

import java.io.IOException;

import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJIThreading
{

    private final static Logger logger = LoggerFactory.getLogger ( TestJIThreading.class );

    static final String domain = "fdgnt";

    static final String user = "roopchand";

    static final String password = "QweQwe007";

    static final String host = "estroopchandnb";

    static final String comServerName = "WbemScripting.SWbemLocator";

    static final String comObjectId = "76A6415B-CB41-11d1-8B02-00600806D9B6";

    static final int totalLoops = 500;

    static final int numThreads = 25;

    static int loopsPerThread;

    static final int waitForThreadssleepTime = 1000;

    static
    {
        loopsPerThread = totalLoops / numThreads;
    }

    public void setUp ()
    {

        try
        {
            JISystem.setInBuiltLogHandler ( false );
        }
        catch ( final SecurityException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
        catch ( final IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
        JISystem.setAutoRegisteration ( true );
        // JR: JISystem.getLogger ().setLevel ( Level.ALL );
        // JR: configure using slf4j now
    }

    public void testThreading ()
    {
        final ThreadGroup group = new ThreadGroup ( "JIThreading Group" );
        final Thread[] threads = new Thread[numThreads];
        for ( int i = 0; i < numThreads; i++ )
        {
            threads[i] = new TestThread ( group, "TestThread: " + i );
        }

        for ( int i = 0; i < numThreads; i++ )
        {
            threads[i].start ();
            //log.info( "activeCount: "+ group.activeCount() );
            //group.list();
        }

        final boolean keepSleeping = true;
        while ( keepSleeping )
        {
            try
            {
                for ( int i = 0; i < threads.length; i++ )
                {
                    final Thread thread = threads[i];
                    thread.join ();
                }
            }
            catch ( final InterruptedException e )
            {
                logger.error ( "InterruptedException caught", e );
            }

            break;
            /*
            boolean threadsRunning = false;
            int aliveCount = 0;
            for ( int i = 0; i < threads.length; i++ ) {
            	Thread thread = threads[ i ];
            	if ( thread.isAlive() ) {
            		aliveCount++;
            		threadsRunning = true;
            		//break;
            	}
            }
            log.info( "threadsRunning: "+ threadsRunning +" aliveCount: "+ aliveCount );
            if ( threadsRunning == false ) {
            	keepSleeping = false;
            	break;
            }
            */
        }
    }

    public static class TestThread extends Thread
    {
        public TestThread ( final ThreadGroup group, final String name )
        {
            super ( group, name );
        }

        @Override
        public void run ()
        {
            for ( int i = 0; i < loopsPerThread; i++ )
            {
                doStuff ();
            }
        }

        public void doStuff ()
        {

            try
            {
                final JISession session = JISession.createSession ( domain, user, password );

                //this.session.setGlobalSocketTimeout( 60000 );

                // by name, requires local access (for registry search), or a populated progIdVsClsidDB.properties
                final JIProgId progId = JIProgId.valueOf ( comServerName );

                final JIComServer baseComServer = new JIComServer ( progId, host, session );

                // Do it by clsid
                //JIClsid clsid = JIClsid.valueOf( "76A6415B-CB41-11d1-8B02-00600806D9B6" );
                //clsid.setAutoRegistration( true );
                //baseComServer = new JIComServer( clsid, host, session );

                // I'm not really sure what the deal is with this
                // Create an intermediary instance?
                final IJIComObject unknown = baseComServer.createInstance ();

                final IJIComObject baseComObject = unknown.queryInterface ( comObjectId );

                final IJIDispatch baseDispatch = (IJIDispatch)JIObjectFactory.narrowObject ( baseComObject.queryInterface ( IJIDispatch.IID ) );

                final JIVariant connectServer = baseDispatch.callMethodA ( "ConnectServer", new Object[] { new JIString ( host ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), new Integer ( 0 ), JIVariant.OPTIONAL_PARAM () } )[0];

                JISession.destroySession ( session );
                System.out.println ( "doStuff() run complete" );
            }
            catch ( final Exception e )
            {
                logger.error ( "Caught exception: ", e );
            }
        }
    }

    public static void main ( final String[] args )
    {
        final TestJIThreading testJIThreading = new TestJIThreading ();
        testJIThreading.setUp ();
        testJIThreading.testThreading ();
    }
}
