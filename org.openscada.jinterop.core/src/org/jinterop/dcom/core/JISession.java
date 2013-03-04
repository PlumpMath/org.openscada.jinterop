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

package org.jinterop.dcom.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.IJIUnreferenced;
import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Representation of an active session with a COM server. All interface
 * references being given out by the framework for a particular COM server are
 * maintained by the session and an <code>IJIComObject</code> is associated with
 * a single session only. Sessions are also responsible for the clean up once
 * the system shuts down or <code>IJIComObject</code> go out of reference scope.
 * <p>
 * Please make sure that you call {@link #destroySession(JISession)} after you
 * are done using the session. This will ensure that any open sockets to COM
 * server are closed.
 * 
 * @since 1.0
 */
public final class JISession
{

    private final static Logger logger = LoggerFactory.getLogger ( JISession.class );

    private static Random randomGen = new Random ( Double.doubleToRawLongBits ( Math.random () ) );

    private int sessionIdentifier = -1;

    private String username = null;

    private String password = null;

    private String domain = null;

    private String targetServer = null;

    private static Map mapOfObjects = Collections.synchronizedMap ( new HashMap () );

    private static Object mutex = new Object ();

    private IJIAuthInfo authInfo = null;

    private JIComServer stub = null;

    private JIRemUnknownServer stub2 = null;

    private static int oxidResolverPort = -1;

    private static byte[] localhost = new byte[] { 127, 0, 0, 1 };

    private static String localhostStr = "127.0.0.1";

    private static String localhostStr2 = "LOCALHOST";

    private static Map mapOfSessionIdsVsSessions = new HashMap ();

    private static ArrayList listOfSessions = new ArrayList ();

    private final List listOfDeferencedIpids = new ArrayList ();

    private static Timer releaseRefsTimer = new Timer ( true );

    private final Map mapOfUnreferencedHandlers = new HashMap ();

    private int timeout = 0;

    private boolean useSessionSecurity = false;

    private boolean useNTLMv2 = false;

    private boolean isSSO = false;

    private final ArrayList links = new ArrayList ();

    private static final Map mapOfOxidsVsJISessions = new HashMap ();

    private boolean sessionInDestroy = false;

    private static class IPID_SessionID_Holder
    {
        public final String IPID;

        public final Integer sessionID;

        public final boolean isOnlySessionIDPresent;

        public final byte[] oid;

        private IPID_SessionID_Holder ( final String IPID, final int sessionID, final boolean isOnlySessionId, final byte[] oid )
        {
            this.IPID = IPID;
            this.isOnlySessionIDPresent = isOnlySessionId;
            this.sessionID = new Integer ( sessionID );
            this.oid = oid;
        }
    }

    //static List listOfSessions = new ArrayList();
    //will be read by the system thread for cleanup and then passed
    //to each session for clean up.
    static ReferenceQueue referenceQueueOfCOMObjects = new ReferenceQueue ();

    static Thread cleanUpThread = new Thread ( new Runnable () {
        @Override
        public void run ()
        {
            try
            {
                while ( true )
                {
                    final Reference r = referenceQueueOfCOMObjects.remove ();
                    if ( r != null )
                    {
                        // Object is no longer referenced.
                        //get from hash map and call release ref on that object
                        IPID_SessionID_Holder holder = null;
                        synchronized ( mapOfObjects )
                        {
                            holder = (IPID_SessionID_Holder)mapOfObjects.remove ( r );
                            if ( holder == null )
                            {
                                continue;
                            }
                        }

                        JISession session = null;
                        synchronized ( mutex )
                        {
                            session = (JISession)mapOfSessionIdsVsSessions.get ( holder.sessionID );
                        }
                        //this means that the session got lost...but this logic does not work, since
                        //session is strongly referenced from mapOfSessionIdsVsSessions and listOfSessions and even putting
                        //WeakReference for JISession when adding it to the mapOfSessionIdsVsSessions/listOfSessions does not
                        //make a difference as we always loose the session to GC before it come here.
                        if ( holder.isOnlySessionIDPresent )
                        {
                            try
                            {
                                destroySession ( session );
                            }
                            catch ( final Exception e )
                            {
                                logger.warn ( "exception from destroy session in clean up thread", e );
                            }
                        }
                        else
                        {
                            //session may have been "destroySession"...
                            if ( session == null )
                            {
                                continue;
                            }

                            try
                            {
                                final String IPID = holder.IPID;
                                //JIComOxidRuntime.delIPIDReference(IPID);
                                //session.releaseRef(IPID); Not doing release anymore, this causes a lot of calls to
                                //go across, so will save these in this list and then the cleanup thread will deal with
                                //this every 3 minutes.
                                session.addDereferencedIpids ( IPID, holder.oid );
                                holder = null;
                                final IJIUnreferenced unreferenced = session.getUnreferencedHandler ( IPID );
                                if ( unreferenced != null )
                                {
                                    unreferenced.unReferenced ();
                                }
                                session.unregisterUnreferencedHandler ( IPID );
                            }
                            catch ( final Exception e )
                            {
                                logger.warn ( "exception from removing a IPID from session in clean up thread", e );
                            }
                        }
                    }

                }
            }
            catch ( final Exception e )
            {
                logger.warn ( "CleanupThread:run()", e );
            }
        }
    }, "jI_GarbageCollector" );

    //from JDK bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037
    private static String getLocalHost ( final String destination )
    {
        DatagramSocket sock;
        InetAddress intendedDestination;
        try
        {
            sock = new DatagramSocket ();
            intendedDestination = InetAddress.getByName ( destination );
        }
        catch ( final Exception e )
        {
            return "127.0.0.1";
        }
        sock.connect ( intendedDestination, sock.getLocalPort () );
        return sock.getLocalAddress ().getHostAddress ();
    }

    static
    {
        JISystem.internal_initLogger ();
        try
        {
            final InetAddress localhostAddr = InetAddress.getLocalHost ();
            localhost = localhostAddr.getAddress ();
            localhostStr = localhostAddr.getHostAddress ();
            localhostStr2 = localhostAddr.getCanonicalHostName ();
        }
        catch ( final UnknownHostException e )
        {
        }

        System.setProperty ( "jcifs.smb.client.domain", "JIDomain" );//is being put in for completing type2 message
        //somehow windows is not taking empty domain name.

        //start the cleanup thread.
        // and create a shutdown hook also.
        cleanUpThread.setDaemon ( true );
        //cleanUpThread.setPriority(Thread.MIN_PRIORITY);
        cleanUpThread.start ();

        JIComOxidRuntime.startResolver ();
        JIComOxidRuntime.startResolverTimer ();
        oxidResolverPort = JIComOxidRuntime.getOxidResolverPort ();
        releaseRefsTimer.scheduleAtFixedRate ( new Release_References_TimerTask (), 0, 3 * 60 * 1000 );

        Runtime.getRuntime ().addShutdownHook ( new Thread ( new Runnable () {
            @Override
            public void run ()
            {
                int i = 0;
                while ( i < listOfSessions.size () )
                {
                    final JISession session = (JISession)listOfSessions.get ( i );
                    try
                    {
                        JISession.destroySession ( session );
                    }
                    catch ( final JIException e )
                    {
                        logger.warn ( "addShutDownHook Thread:run()", e );
                    }
                    i++;
                }
                JISystem.internal_writeProgIdsToFile ();
                JIComOxidRuntime.stopResolver ();
                releaseRefsTimer.cancel ();
                mapOfSessionIdsVsSessions.clear ();
                mapOfObjects.clear ();
                listOfSessions.clear ();
            }
        }, "jI_ShutdownHook" ) );

    }

    private static class Release_References_TimerTask extends TimerTask
    {
        @Override
        public void run ()
        {

            try
            {
                List listOfSessionsClone = null;
                synchronized ( mutex )
                {
                    listOfSessionsClone = (List)listOfSessions.clone ();
                }

                int i = 0;

                while ( i < listOfSessionsClone.size () )
                {
                    final JISession session = (JISession)listOfSessionsClone.get ( i );

                    //now iterate over each sessions listOfDereferencedIpids and send a call to release for the entire lot.
                    final ArrayList listToKill = new ArrayList ();
                    synchronized ( mutex )
                    {
                        logger.info ( "Release_References_TimerTask:[RUN] Session:  {} , listOfDeferencedIpids.size(): {}", session.getSessionIdentifier (), session.listOfDeferencedIpids.size () );
                        for ( int j = 0; j < session.listOfDeferencedIpids.size (); j++ )
                        {
                            try
                            {
                                listToKill.add ( session.prepareForReleaseRef ( (String)session.listOfDeferencedIpids.get ( j ) ) );
                            }
                            catch ( final JIException e )
                            {
                                //eaten, will never get thrown from the try block.
                            }
                        }
                        session.listOfDeferencedIpids.clear ();
                    }

                    if ( listToKill.size () > 0 )
                    {
                        final JIArray array = new JIArray ( listToKill.toArray ( new JIStruct[listToKill.size ()] ), true );
                        try
                        {
                            session.releaseRefs ( array, false );
                        }
                        catch ( final JIException e )
                        {
                            //This release cycle has to go on.
                            logger.warn ( "Release_References_TimerTask:run() - Exception in internal GC", e );
                        }
                    }

                    i++;
                }

            }
            catch ( final Exception e )
            {
                //This release cycle has to go on.
                logger.warn ( "Release_References_TimerTask:run() - Exception in internal GC", e );
            }
        }
    }

    void setTargetServer ( final String targetServer )
    {
        if ( targetServer.equalsIgnoreCase ( "127.0.0.1" ) )
        {
            //Replace with it's actual bindings, otherwise does not work for JCIFS authentication
            this.targetServer = getLocalhostAddressAsIPString ();
        }
        else
        {
            this.targetServer = targetServer;

            //will change the localhost to the actual address as well
            if ( localhostStr.equalsIgnoreCase ( "127.0.0.1" ) )
            { //Bug in JDK , time to find alternate logic.
                localhostStr = getLocalHost ( targetServer );
            }

        }

    }

    static byte[] getLocalhostAddressAsIPbytes ()
    {
        return localhost;
    }

    static String getLocalhostAddressAsIPString ()
    {
        return localhostStr;
    }

    static String getLocalhostCanonicalAddressAsString ()
    {
        return localhostStr2;
    }

    String getTargetServer ()
    {
        return this.targetServer;
    }

    private JISession ()
    {
    };

    static int getOxidResolverPort ()
    {
        return oxidResolverPort;
    }

    /**
     * Returns the <code>IJIAuthInfo</code> (if any) associated with this
     * session.
     * 
     * @return
     */
    public IJIAuthInfo getAuthInfo ()
    {
        return this.authInfo;
    }

    /**
     * Creates a session with the <code>authInfo</code> of the user. This
     * session is not yet attached to a
     * COM server.
     * 
     * @param authInfo
     * @return
     * @throws IllegalArgumentException
     *             if <code>authInfo</code> is <code>null</code>.
     * @see JIComServer#JIComServer(JIClsid, JISession)
     * @see JIComServer#JIComServer(JIProgId, JISession)
     */
    public static JISession createSession ( final IJIAuthInfo authInfo )
    {
        if ( authInfo == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_AUTH_NOT_SUPPLIED ) );
        }

        final JISession session = new JISession ();

        session.authInfo = authInfo;

        session.sessionIdentifier = authInfo.getUserName ().hashCode () ^ authInfo.getPassword ().hashCode () ^ authInfo.getDomain ().hashCode () ^ new Object ().hashCode () ^ (int)Runtime.getRuntime ().freeMemory () ^ randomGen.nextInt ();

        synchronized ( mutex )
        {
            mapOfSessionIdsVsSessions.put ( new Integer ( session.sessionIdentifier ), session );
            listOfSessions.add ( session );
        }

        logger.info ( "Created Session: {}", session.sessionIdentifier );
        return session;
    }

    /**
     * Creates a session. This session is not yet attached to a
     * COM server.
     * 
     * @param domain
     *            domain of the user.
     * @param username
     *            name of the user
     * @param password
     *            password of the user.
     * @return
     * @throws IllegalArgumentException
     *             if any parameter is <code>null</code>.
     * @see JIComServer#JIComServer(JIClsid, JISession)
     * @see JIComServer#JIComServer(JIProgId, JISession)
     */
    public static JISession createSession ( final String domain, final String username, final String password )
    {
        if ( username == null || password == null || domain == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_AUTH_NOT_SUPPLIED ) );
        }

        final JISession session = new JISession ();
        session.username = username;
        session.password = password;
        session.domain = domain;
        session.sessionIdentifier = username.hashCode () ^ password.hashCode () ^ domain.hashCode () ^ new Object ().hashCode () ^ (int)Runtime.getRuntime ().freeMemory () ^ randomGen.nextInt ();

        synchronized ( mutex )
        {
            mapOfSessionIdsVsSessions.put ( new Integer ( session.sessionIdentifier ), session );
            listOfSessions.add ( session );
        }

        logger.info ( "Created Session: {}", session.sessionIdentifier );
        return session;
    }

    /**
     * Creates a new session using credentials of the <code>session</code>
     * parameter. The new session is not yet attached to a
     * COM server.
     * 
     * @param session
     * @return
     * @see JIComServer#JIComServer(JIClsid, JISession)
     * @see JIComServer#JIComServer(JIProgId, JISession)
     */
    public static JISession createSession ( final JISession session )
    {
        final JISession newSession = createSession ( session.getDomain (), session.getUserName (), session.getPassword () );
        newSession.authInfo = session.authInfo;
        return newSession;

    }

    /**
     * <b>Native</b> Single Sign On capable session.
     * <b>Warning:</b>
     * <ul>
     * <li>This method works <b>only</b> on Microsoft Windows Platform.</li>
     * <li>It does <b>not</b> support NTLMv2 or NTLM1 Session Security.</li>
     * <li>It supports only NTLM1 Authentication.</li>
     * <li>This session <b>cannot</b> be used with
     * <code>JIComServer(ProgId,...)</code> ctors. JCIFS will fail to setup a
     * connection with Windows Registry if GUEST account is disabled.</li>
     * </ul>
     * 
     * @return
     * @see JIComServer#JIComServer(JIClsid, JISession)
     * @see JIComServer#JIComServer(JIProgId, JISession)
     */
    public static JISession createSession ()
    {
        if ( !System.getProperty ( "os.name" ).toLowerCase ().startsWith ( "windows" ) )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_WIN_ONLY ) );
        }

        final JISession session = new JISession ();
        session.sessionIdentifier = new Object ().hashCode () ^ (int)Runtime.getRuntime ().freeMemory () ^ randomGen.nextInt ();
        session.isSSO = true;

        synchronized ( mutex )
        {
            mapOfSessionIdsVsSessions.put ( new Integer ( session.sessionIdentifier ), session );
            listOfSessions.add ( session );
        }

        logger.info ( "Created Session for SSO: {}", session.sessionIdentifier );

        return session;
    }

    /**
     * Returns whether this session is SSO or not.
     * 
     * @return
     */
    public boolean isSSOEnabled ()
    {
        return this.isSSO;
    }

    /**
     * <p>
     * Used to destroy the <code>session</code>, this release all references of
     * the COM server and it's interfaces. It should be called in the end after
     * the developer is done with the COM server.
     * <p>
     * Note that all interface references belonging to sessions linked to this
     * session will also be destroyed.
     * 
     * @param session
     * @throws JIException
     * @see JIObjectFactory#narrowObject
     */
    public static void destroySession ( final JISession session ) throws JIException
    {
        //null session
        if ( session == null )
        {
            return;
        }

        //if stub is null then cleanup datastructures holding the session object only
        if ( session.stub == null )
        {
            synchronized ( mutex )
            {
                mapOfSessionIdsVsSessions.remove ( new Integer ( session.getSessionIdentifier () ) );
                listOfSessions.remove ( session );
            }

            //now remove the links and the OIDs
            postDestroy ( session );
            return;
        }

        try
        {
            //session may have been destroyed and this call is from finalize.
            final ArrayList list = new ArrayList ();
            final ArrayList listOfFreeIPIDs = new ArrayList ();
            synchronized ( mutex )
            {
                if ( session.sessionInDestroy )
                {
                    return;
                }
                session.sessionInDestroy = true;
                //list of dereferenced IPIDs
                for ( int j = 0; j < session.listOfDeferencedIpids.size (); j++ )
                {
                    list.add ( session.prepareForReleaseRef ( (String)session.listOfDeferencedIpids.get ( j ) ) );
                }
                listOfFreeIPIDs.addAll ( session.listOfDeferencedIpids );
                session.listOfDeferencedIpids.clear ();
            }

            synchronized ( mapOfObjects )
            {
                //now take all the objects registered with this session and call release on them.
                //				Iterator iterator = mapOfObjects.keySet().iterator();
                final Iterator iterator = mapOfObjects.entrySet ().iterator ();
                while ( iterator.hasNext () )
                {
                    //String ipid = (String)session.mapOfObjects.get(iterator.next());
                    final Entry entry = (Entry)iterator.next ();
                    //					IPID_SessionID_Holder holder = (IPID_SessionID_Holder)mapOfObjects.get(iterator.next());
                    final IPID_SessionID_Holder holder = (IPID_SessionID_Holder)entry.getValue ();
                    if ( session.getSessionIdentifier () != holder.sessionID.intValue () )
                    {
                        continue;
                    }
                    final String ipid = holder.IPID;
                    if ( ipid == null )
                    {
                        continue;
                    }

                    //Commenting the line below since there could be more than one reference of a COM object taken in by
                    //j-Interop (via the client of j-Interop) and mapOfObjects will contain two references in this case.
                    //This was identified for the issue reported by Aquafold in sql dbg.
                    //					if (!listOfFreeIPIDs.contains(ipid))
                    {
                        list.add ( session.prepareForReleaseRef ( ipid ) );
                        listOfFreeIPIDs.add ( ipid );
                    }
                    iterator.remove ();
                }
            }

            //now to kill the stub itself
            if ( session.stub.getServerInterfacePointer () != null )
            {
                if ( !listOfFreeIPIDs.contains ( session.stub.getServerInterfacePointer ().getIPID () ) )
                {
                    list.add ( session.prepareForReleaseRef ( session.stub.getServerInterfacePointer ().getIPID (), ( (JIStdObjRef)session.stub.getServerInterfacePointer ().getObjectReference ( JIInterfacePointer.OBJREF_STANDARD ) ).getPublicRefs () ) );
                    listOfFreeIPIDs.add ( session.stub.getServerInterfacePointer ().getIPID () );
                }
            }

            listOfFreeIPIDs.clear ();
            //release is performed if only something is in the session.
            if ( list.size () > 0 )
            {
                final JIArray array = new JIArray ( list.toArray ( new JIStruct[list.size ()] ), true );
                try
                {
                    session.stub.closeStub (); //close the existing connection
                    session.releaseRefs ( array, true );
                }
                catch ( final JIException e )
                {
                    //This release cycle has to go on.
                    logger.warn ( "destroySession", e );
                }
            }

            JIComOxidRuntime.clearIPIDsforSession ( session );
            logger.info ( "Destroyed Session: {}", session.sessionIdentifier );
        }
        finally
        {
            synchronized ( mutex )
            {
                mapOfSessionIdsVsSessions.remove ( new Integer ( session.getSessionIdentifier () ) );
                listOfSessions.remove ( session );
                // and remove its entry from the map
                if ( session.stub.getServerInterfacePointer () != null )
                {
                    mapOfOxidsVsJISessions.remove ( new JIOxid ( session.stub.getServerInterfacePointer ().getOXID () ) );
                }
            }
            session.stub.closeStub ();
            session.stub2.closeStub ();
        }

        postDestroy ( session );
        session.stub = null; //setting it null in the end.
        session.stub2 = null;
    }

    private static void postDestroy ( final JISession session ) throws JIException
    {
        //now destroy all linked sessions
        logger.info ( "About to destroy {} sessesion which are linked to this session: {}", session.links.size (), session.getSessionIdentifier () );

        for ( int i = 0; i < session.links.size (); i++ )
        {
            final JISession linkedSession = (JISession)session.links.get ( i );
            try
            {
                JISession.destroySession ( linkedSession );
            }
            catch ( final Exception e )
            {
                logger.warn ( "Failed to destroy linked session" + linkedSession.getSessionIdentifier () + ". Passing on to next one.", e );
            }
        }

        session.links.clear ();
        //finally any oids exported by this session.
        JIComOxidRuntime.destroySessionOIDs ( session.getSessionIdentifier () );
    }

    //each session is associated with 1 and only 1 stub.
    //adding something new now another stub for IRemUnknown operations
    void setStub ( final JIComServer stub )
    {
        this.stub = stub;
        synchronized ( mutex )
        {
            mapOfOxidsVsJISessions.put ( new JIOxid ( stub.getServerInterfacePointer ().getOXID () ), this );
        }
    }

    //IRemUnknown Stub
    void setStub2 ( final JIRemUnknownServer stub )
    {
        this.stub2 = stub;
        //no need to add this to the Oxid vs Sessions map as we would be using the same interface pointer as the other stub.
    }

    JIComServer getStub ()
    {
        return this.stub;
    }

    JIRemUnknownServer getStub2 ()
    {
        return this.stub2;
    }

    /**
     * @exclude
     * @param IPID
     */
    void addToSession ( final IJIComObject comObject, final byte[] oid )
    {

        //nothing will be done if the session is being destroyed.
        if ( this.sessionInDestroy )
        {
            return;
        }
        final IPID_SessionID_Holder holder = new IPID_SessionID_Holder ( comObject.getIpid (), getSessionIdentifier (), false, oid );
        //mapOfObjects.put(new WeakReference(comObject,referenceQueueOfCOMObjects),holder);
        synchronized ( mapOfObjects )
        {
            mapOfObjects.put ( new WeakReference ( comObject, referenceQueueOfCOMObjects ), holder );
        }
        //setting if NO PING flag has been set to true.
        addToSession ( comObject.getIpid (), oid, ( (JIStdObjRef)comObject.internal_getInterfacePointer ().getObjectReference ( JIInterfacePointer.OBJREF_STANDARD ) ).getFlags () == 0x00001000 );
        logger.info ( "for IID: {}", comObject.getInterfaceIdentifier () );

        //		Integer value = (Integer)mapOfIPIDSvsCount.get(comObject.getIpid());
        //		if (value == null)
        //		{
        //			mapOfIPIDSvsCount.put(comObject.getIpid(), new Integer(0));
        //		}

        //		debug_addIpids(comObject.getIpid(),((JIStdObjRef)comObject.internal_getInterfacePointer().getObjectReference(JIInterfacePointer.OBJREF_STANDARD)).getPublicRefs());
    }

    //just for testing
    private static Map mapOfIPIDSvsCount = Collections.synchronizedMap ( new HashMap () );

    static void debug_addIpids ( final String ipid, final int num )
    {
        //		Integer value = (Integer)mapOfIPIDSvsCount.get(ipid);
        //		if (value == null)
        //		{
        //			value = new Integer(0);
        //		}
        //		mapOfIPIDSvsCount.put(ipid, new Integer(value.intValue() + num));
    }

    static void debug_delIpids ( final String ipid, final int num )
    {
        //		Integer value = (Integer)mapOfIPIDSvsCount.get(ipid);
        //		mapOfIPIDSvsCount.put(ipid, new Integer(value.intValue() - num));
    }

    /**
     * @exclude
     * @param IPID
     */
    private void addToSession ( final String IPID, final byte[] oid, final boolean dontping )
    {
        //Weak reference of the object
        //mapOfObjects.put(new WeakReference(IPID,referenceQueueOfCOMObjects),IPID);
        //it does not matter if we create a new OID here, the OxidCOMRunttime API uses the OID in the MAP , and not this one.
        final JIObjectId joid = new JIObjectId ( oid, dontping );
        JIComOxidRuntime.addUpdateOXIDs ( this, IPID, joid );
        logger.info ( "[addToSession] Adding IPID: {} to session: {}", IPID, getSessionIdentifier () );
    }

    //	private static Map mapOfIPIDSvsCount = new HashMap();

    //	public static void debug_dumpIpidVsCountMap()
    //	{
    //		if (JISystem.getLogger().isLoggable(Level.WARNING))
    //		{
    //			JISystem.getLogger().warning("Dumping mapOfIPIDSvsCount " + mapOfIPIDSvsCount.toString());
    //		}
    //	}

    //this gets called from the cleanupthread and no place else and it calls the releaseRef of session which
    //internally calls the add_releaseRef of the JIComServer, that method is synched at the instance level.
    //I was worried about a deadlock with destroySession , since that also ultimately calls the add_releaseRef, but
    //this will not happen since under a simultaneous destroy and removefromsession call , the "mutex" object will get synch.
    //If suppose a comServer.getInterface(...) is being done (which also calls releaseRef), then that is synched at instance level
    //and so is add_releaseRef (on the same instance), so deadlock won't happen there. If a simulataneous remove and getInterface call comes
    //then getInterface(which internally calls releaseRef) will go through, since releaseRef is not synched but the api it calls i.e. add_releaseRef is synched with the same lock
    //as getInterface. The remove will have to wait till that call gets over.
    void releaseRef ( final String IPID ) throws JIException
    {
        releaseRef ( IPID, 5 );
    }

    void releaseRef ( final String IPID, final int numinstances ) throws JIException
    {
        logger.info ( "releaseRef:Reclaiming from Session: {} , the IPID: {}, numinstances is {}", numinstances, new Object[] { getSessionIdentifier (), IPID, numinstances } );

        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setParentIpid ( IPID );
        obj.setOpnum ( 2 );//release
        //length
        obj.addInParamAsShort ( (short)1, JIFlags.FLAG_NULL );
        //ipid to addfref on
        final JIArray array = new JIArray ( new rpc.core.UUID[] { new rpc.core.UUID ( IPID ) }, true );
        obj.addInParamAsArray ( array, JIFlags.FLAG_NULL );
        //TODO requesting 5 for now, will later build caching mechnaism to exhaust 5 refs first before asking for more
        // same with release.
        obj.addInParamAsInt ( numinstances, JIFlags.FLAG_NULL );
        obj.addInParamAsInt ( 0, JIFlags.FLAG_NULL );//private refs = 0
        if ( logger.isInfoEnabled () )
        {
            debug_delIpids ( IPID, numinstances );
            logger.info ( "releaseRef: Releasing numinstances {} references of IPID: {} session: {}", new Object[] { numinstances, IPID, getSessionIdentifier () } );
        }
        this.stub2.addRef_ReleaseRef ( obj );
    }

    private void addDereferencedIpids ( final String IPID, final byte[] oid )
    {
        logger.info ( "addDereferencedIpids for session : {} , IPID is: {}", getSessionIdentifier (), IPID );

        synchronized ( mutex )
        {
            if ( !this.listOfDeferencedIpids.contains ( IPID ) )
            {
                this.listOfDeferencedIpids.add ( IPID );
            }
        }

        //Will call the JIComOxidRuntime, and that is synched on mutex3, but that will not cause a deadlock, since
        //it or rather any method of JIComOxidRuntime does not call back into JISession.
        JIComOxidRuntime.delIPIDReference ( IPID, new JIObjectId ( oid, false ), this );
    }

    private void releaseRefs ( final JIArray arrayOfStructs, final boolean fromDestroy ) throws JIException
    {
        logger.info ( "In releaseRefs for session : {} , array length is: {}", getSessionIdentifier (), (short) ( (Object[])arrayOfStructs.getArrayInstance () ).length );

        final JICallBuilder obj = new JICallBuilder ( true );
        obj.setOpnum ( 2 );//release
        //length
        obj.addInParamAsShort ( (short) ( (Object[])arrayOfStructs.getArrayInstance () ).length, JIFlags.FLAG_NULL );
        obj.addInParamAsArray ( arrayOfStructs, JIFlags.FLAG_NULL );
        obj.fromDestroySession = fromDestroy;
        this.stub2.addRef_ReleaseRef ( obj );

        //ignore the results
    }

    private JIStruct prepareForReleaseRef ( final String IPID, final int numInstancesfirsttime ) throws JIException
    {
        final JIStruct remInterface = new JIStruct ();
        remInterface.addMember ( new rpc.core.UUID ( IPID ) );
        remInterface.addMember ( new Integer ( numInstancesfirsttime + 5 ) ); // numInstancesfirsttime of the original and 5 for the addRef done later on.
        remInterface.addMember ( new Integer ( 0 ) );//private refs = 0
        logger.warn ( "prepareForReleaseRef: Releasing numInstancesfirsttime + 5 references of IPID: {} session: {} , numInstancesfirsttime is {}", new Object[] { IPID, getSessionIdentifier (), numInstancesfirsttime } );
        debug_delIpids ( IPID, numInstancesfirsttime + 5 );
        return remInterface;
    }

    private JIStruct prepareForReleaseRef ( final String IPID ) throws JIException
    {
        return prepareForReleaseRef ( IPID, 5 );
    }

    /**
     * Gets the user name associated with this session.
     * 
     * @return
     */
    public String getUserName ()
    {
        return this.authInfo == null ? this.username : this.authInfo.getUserName ();
    }

    String getPassword ()
    {
        return this.authInfo == null ? this.password : this.authInfo.getPassword ();
    }

    /**
     * Gets the domain of the user associated with this session.
     * 
     * @return
     */
    public String getDomain ()
    {
        return this.authInfo == null ? this.domain : this.authInfo.getDomain ();
    }

    /**
     * Returns a unique identifier for this session.
     * 
     * @return
     */
    public int getSessionIdentifier ()
    {
        return this.sessionIdentifier;
    }

    /**
     * @exclude
     */
    @Override
    public boolean equals ( final Object obj )
    {

        if ( obj == null || ! ( obj instanceof JISession ) )
        {
            return false;
        }

        final JISession temp = (JISession)obj;
        return temp.sessionIdentifier == this.sessionIdentifier;
    }

    /**
     * @exclude
     */
    @Override
    public int hashCode ()
    {
        return this.sessionIdentifier;
    }

    @Override
    protected void finalize ()
    {
        try
        {
            destroySession ( this );
        }
        catch ( final JIException e )
        {
            logger.warn ( "Exception in finalize when destroying session", e );
        }
    }

    synchronized IJIUnreferenced getUnreferencedHandler ( final String ipid )
    {
        return (IJIUnreferenced)this.mapOfUnreferencedHandlers.get ( ipid );
    }

    synchronized void registerUnreferencedHandler ( final String ipid, final IJIUnreferenced unreferenced )
    {
        this.mapOfUnreferencedHandlers.put ( ipid, unreferenced );
    }

    synchronized void unregisterUnreferencedHandler ( final String ipid )
    {
        this.mapOfUnreferencedHandlers.remove ( ipid );
    }

    /**
     * <p>
     * Sets the timeout for all sockets opened to (not fro) the COM server for
     * this session. Default value is 0 (no timeout). The class level and the
     * method level settings in case of <code>IJIComObject</code> override this
     * timeout.
     * </p>
     * 
     * @param timeout
     *            in millisecs
     * @see IJIComObject#setInstanceLevelSocketTimeout(int)
     * @see IJIComObject#call(JICallBuilder, int)
     */
    public void setGlobalSocketTimeout ( final int timeout )
    {
        this.timeout = timeout;
    }

    /**
     * Returns the global timeout applied to all sockets opened from this
     * session to COM Server.
     * 
     * @return
     */
    public int getGlobalSocketTimeout ()
    {
        return this.timeout;
    }

    /**
     * <p>
     * Sets the use of NTLM2 Session Security. Framework will use NTLM Packet
     * Level Privacy and Sign\Seal all packets. Once the
     * <code>JIComServer</code> is bound to this session (using any of the
     * <code>JIComServer</code> constructors) the use of session security
     * <b>cannot</b> be enabled or disabled.
     * <p>
     * Please note that session security can come at any available level of
     * authentication (LM\NTLM\LMv2\NTLMv2). The framework currently only
     * supports sign and seal at NTLMv1 level.
     * <p>
     * Whether to use NTLM1 or not is dictated by this field in the Windows
     * Registry.
     * <p>
     * <code>
     * HKLM\System\CurrentControlSet\Control\Lsa\LmCompatibilityLevel <p>
     * </code> This article on MSDN talks more about it
     * http://support.microsoft.com/default.aspx?scid=KB;en-us;239869
     * 
     * @param enable
     *            <code>true</code> to enable, <code>false</code> to disable.
     */
    public void useSessionSecurity ( final boolean enable )
    {
        this.useSessionSecurity = enable;
        //		if (enable)
        //		{
        //			useNTLMv2 = enable;
        //		}
    }

    /**
     * <p>
     * Sets the use of NTLMv2 Security (default is NTLM1). This can be used in
     * combination with <code>useSessionSecurity</code> method. Once the
     * <code>JIComServer</code> is bound to this session (using any of the
     * <code>JIComServer</code> constructors) the use of NTLMv2 security
     * <b>cannot</b> be enabled or disabled.
     * <p>
     * 
     * @param enable
     *            <code>true</code> to enable.
     */
    public void useNTLMv2 ( final boolean enable )
    {
        this.useNTLMv2 = enable;
    }

    /**
     * <p>
     * Flag indicating whether session security is enabled.
     * </p>
     * 
     * @return <code>true</code> for enabled.
     */
    public boolean isSessionSecurityEnabled ()
    {
        return !this.isSSO & this.useSessionSecurity;
    }

    /**
     * <p>
     * Flag indicating whether NTLMv2 security is enabled.
     * </p>
     * 
     * @return <code>true</code> for enabled.
     */
    public boolean isNTLMv2Enabled ()
    {
        return !this.isSSO & this.useNTLMv2;
    }

    /**
     * <p>
     * Links the src with target. These two sessions can now be destroyed in a
     * cascade effect.
     * </p>
     * 
     * @param session
     */
    static void linkTwoSessions ( final JISession src, final JISession target )
    {
        if ( src.sessionInDestroy || target.sessionInDestroy )
        {
            return;
        }

        if ( src.equals ( target ) )
        {
            return;
        }

        synchronized ( mutex )
        {
            if ( !src.links.contains ( target ) )
            {
                src.links.add ( target );
            }
        }
    }

    /**
     * Removes session from src sessions list.
     */
    static void unLinkSession ( final JISession src, final JISession tobeunlinked )
    {
        if ( src.sessionInDestroy )
        {
            return;
        }

        if ( src.equals ( tobeunlinked ) )
        {
            return;
        }

        synchronized ( mutex )
        {
            src.links.remove ( tobeunlinked );
        }
    }

    /**
     * Based on the oxid returns the JISession (and thus the COM Server)
     * associated with it. This is required, since there are
     * cases where a different JISession may be passed in JIObjectFactory for an
     * JIInterfacePointer which does not belong to this JISession.
     * Under those scenarios, the COM factory will create a new instance of a
     * JISession and associate that Interface pointer with the session.
     * But that is not the right approach as a COM Server for that interface and
     * thus a session might already exist and these have to be tied together.
     * 
     * @exclude
     */
    static JISession resolveSessionForOxid ( final JIOxid oxid )
    {
        synchronized ( mutex )
        {
            return (JISession)mapOfOxidsVsJISessions.get ( oxid );
        }
    }

    boolean isSessionInDestroy ()
    {
        return this.sessionInDestroy;
    }

}
