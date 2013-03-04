/*
 * Main.java
 *
 * Created on 20 ������ 2007 �., 14:47
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jinterop.dcom.test;

import java.io.IOException;
import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class Main
{

    public void Execute ( final JIString str )
    {
        System.out.println ( str.getString () );
    }

    /**
     * @param args
     */
    public static void main ( final String[] args )
    {

        if ( args.length < 4 )
        {
            System.out.println ( "Please provide address domain username password" );
            return;
        }

        try
        {

            final String domain = args[1];
            final String username = args[2];
            final String password = args[3];

            // JR: JISystem.getLogger ().setLevel ( Level.FINEST );
            // JR: configure using slf4j now
            JISystem.setInBuiltLogHandler ( false );
            JISystem.setAutoRegisteration ( true );
            final JISession session3 = JISession.createSession ( domain, username, password );
            session3.useSessionSecurity ( true );
            final JIComServer virtualServer = new JIComServer ( JIProgId.valueOf ( "VirtualServer.Application" ), args[0], session3 );
            final IJIComObject unkVirtualServer = virtualServer.createInstance ();
            final IJIDispatch dispatchVirtualServer = (IJIDispatch)JIObjectFactory.narrowObject ( unkVirtualServer.queryInterface ( IJIDispatch.IID ) );

        }
        catch ( final UnknownHostException e )
        {
            e.printStackTrace ();
        }
        catch ( final JIException e )
        {
            e.printStackTrace ();
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

    }

}