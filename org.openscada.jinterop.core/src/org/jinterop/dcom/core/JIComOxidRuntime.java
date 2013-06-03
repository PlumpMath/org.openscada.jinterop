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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.common.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.Security;

/**
 * Thread for Oxid Resolver. Creates and accepts socket
 * connections for resolving oxids. Gets started once for each instance
 * of the library.
 * Please note that the <b>"Server" <b> Service should be running on the machine
 * where the <br>
 * COM server is running.
 * 
 * @since 1.0
 */
final class JIComOxidRuntime
{
    private final static Logger logger = LoggerFactory.getLogger ( JIComOxidRuntime.class );

    private static Properties defaults = new Properties ();

    private static Properties defaults2 = new Properties ();

    private static boolean stopSystem = false;

    private static boolean resolverStarted = false;

    //	private static ArrayList listOfSockets = new ArrayList();
    private static int oxidResolverPort = -1;

    private static HashMap mapOfIPIDVsComponent = new HashMap (); //java client , com server

    private static HashMap mapOfJavaVsOxidDetails = new HashMap (); //java client , com server

    private static HashMap mapOfOxidVsOxidDetails = new HashMap ();//java client , com server

    private static HashMap mapOfOIDVsComponents = new HashMap (); //java client , com server

    //list of all exported oids per session, all these oids have to be removed.
    private static HashMap mapOfSessionIdsVsOIDs = new HashMap (); //java server , com client

    private static HashMap mapOfSetIdVsListOfOIDs = new HashMap (); //com client , java server

    private static HashMap mapOfSessionVsPingSetHolder = new HashMap (); //com client , java server

    //private static HashMap mapOfIPIDVsOID = new HashMap(); //com client , java server, //IPID vs JIObjectId, for increasing\decreasing references 
    private static HashMap mapOfAddressVsStub = new HashMap (); //java client , com server, so that we don't have to keep doing bind everytime.

    private static List listOfExportedJavaComponents = new ArrayList ();

    static final Object mutex = new Object (); //for access to the sockets

    private static final Object mutex2 = new Object ();//for access to the maps

    private static final Object mutex3 = new Object (); //for access to the AddressVsSession,Stub Map

    private static final Object mutex4 = new Object (); //for access to the mapOfAddressVsStub 

    private static ServerSocket serverSocket = null;

    private static Random randomGen = new Random ( Double.doubleToRawLongBits ( Math.random () ) );

    private static Timer pingTimer_2minutes = new Timer ( true );

    private static Timer pingTimer_8minutes = new Timer ( true );

    //one per session.
    private static class PingSetHolder
    {
        byte[] setId = null;

        String username = null;

        String password = null;

        String domain = null;

        boolean modified = false;

        boolean closed = false;

        int seqNum = 1;

        //JISession session  = null;
        Map currentSetOIDs = new HashMap ();//list of JIObjectId, this list is iterated and if the IPID ref count is 0 , 
                                            //it is added as a delete in set and a complex ping is sent.

        Map pingedOnce = new HashMap ();

        @Override
        public String toString ()
        {
            return "SetID[" + this.setId + "] , currentSetOIDs[" + this.currentSetOIDs + "]";
        }
    }

    //this task just checks for expired OIDs in the mapOfOIDVsComponents, each OID carries with itself, lastPingedTime, 
    //if that (currenttime - thattime) is < ping interval...all is okay, otherwise , all it's details are erased, thus 
    //removing any reference of the given java server from j-Interop library, after which if no one outside has references, this
    //object can be GCed.
    private static class ServerPingTimerTask extends TimerTask
    {
        @Override
        public void run ()
        {

            synchronized ( mutex2 )
            {

                logger.info ( "Running ServerPingTimerTask !" );

                final Iterator itr = mapOfOIDVsComponents.keySet ().iterator ();

                while ( itr.hasNext () )
                {
                    final JIObjectId oid = (JIObjectId)itr.next ();
                    if ( oid.hasExpired () )
                    {
                        //remove all
                        JILocalCoClass component = (JILocalCoClass)mapOfOIDVsComponents.get ( oid );
                        //this means the local system still has references and we cannot delete this object
                        //since the user may reuse it.
                        if ( component.isAssociatedReferenceAlive () )
                        {
                            continue;
                        }
                        JIComOxidDetails details = (JIComOxidDetails)mapOfJavaVsOxidDetails.get ( component );
                        mapOfOxidVsOxidDetails.remove ( details.getOxid () );
                        mapOfIPIDVsComponent.remove ( details.getIpid () );
                        mapOfJavaVsOxidDetails.remove ( component );
                        listOfExportedJavaComponents.remove ( component );
                        itr.remove ();

                        //the thread associated with this will also stop.
                        details.interruptRemUnknownThreadGroup ();

                        component = null;
                        details = null;
                    }
                }

            }

        }
    }

    static void destroySessionOIDs ( final int sessionId )
    {
        synchronized ( mutex2 )
        {
            logger.info ( "destroySessionOIDs for session: {}", sessionId );

            final List oids = (ArrayList)mapOfSessionIdsVsOIDs.remove ( new Integer ( sessionId ) );
            if ( oids == null || oids.isEmpty () )
            {
                return;
            }

            for ( int i = 0; i < oids.size (); i++ )
            {
                JIObjectId oid = (JIObjectId)oids.get ( i );
                //remove all
                JILocalCoClass component = (JILocalCoClass)mapOfOIDVsComponents.remove ( oid );
                JIComOxidDetails details = (JIComOxidDetails)mapOfJavaVsOxidDetails.get ( component );
                if ( details != null )
                {
                    mapOfOxidVsOxidDetails.remove ( details.getOxid () );
                    mapOfIPIDVsComponent.remove ( details.getIpid () );
                }
                mapOfJavaVsOxidDetails.remove ( component );
                listOfExportedJavaComponents.remove ( component );
                //the thread associated with this will also stop.
                if ( details != null )
                {
                    details.interruptRemUnknownThreadGroup ();
                }
                component = null;
                details = null;
                oid = null;
            }

            oids.clear ();
        }
    }

    private static class ClientPingTimerTask extends TimerTask
    {
        @Override
        public void run ()
        {

            Iterator itr = null;
            synchronized ( mutex3 )
            {
                itr = ( (Map)mapOfSessionVsPingSetHolder.clone () ).entrySet ().iterator ();
            }

            logger.info ( "Running ClientPingTimerTask !" );
            //iterate over the map and get the corresponding stubs and use there sessions to 
            //stub is created here and used per address

            //if set id is null send a complex ping to get back the set id for all the OIDs in the
            //PingSetHolder

            while ( itr.hasNext () )
            {
                final Map.Entry entry = (Map.Entry)itr.next ();
                final PingSetHolder holder = (PingSetHolder)entry.getValue ();
                final String address = ( (JISession)entry.getKey () ).getTargetServer ();
                //will get it from the cache, since it is getting called after every 4 minutes
                //what if this stub has timed out, I guess I will have to ask the developers to increase the timeout for now.
                JIComOxidStub stub = null;
                synchronized ( mutex4 )
                {
                    stub = (JIComOxidStub)mapOfAddressVsStub.get ( address );
                    if ( stub == null )
                    {
                        stub = new JIComOxidStub ( address, holder.domain, holder.username, holder.password );
                        mapOfAddressVsStub.put ( address, stub );
                    }
                }

                final ArrayList listOfAddedOIDs = new ArrayList ();
                final ArrayList listOfRemovedOIDs = new ArrayList ();
                //form a list if OID is 0 ref
                synchronized ( mutex3 )
                {
                    for ( final Iterator itr2 = holder.currentSetOIDs.keySet ().iterator (); itr2.hasNext (); )
                    {
                        final JIObjectId oid = (JIObjectId)itr2.next ();
                        if ( oid.getIPIDRefCount () == 0 )
                        {
                            if ( !oid.dontping )
                            {
                                listOfRemovedOIDs.add ( oid );
                                holder.pingedOnce.remove ( oid );
                                holder.modified = true;
                            }
                            itr2.remove ();
                        }
                        else
                        {
                            if ( !oid.dontping && !holder.pingedOnce.containsKey ( oid ) )
                            {
                                listOfAddedOIDs.add ( oid );
                                holder.pingedOnce.put ( oid, oid );
                                holder.modified = true;
                            }
                        }
                    }
                }

                logger.info ( "Within ClientPingTimerTask: holder.currentSetOIDs, current size of which is {}", holder.currentSetOIDs.size () );

                //this is the first time this is going and objects with no references will not be added to ping set.
                if ( holder.setId == null )
                {
                    listOfRemovedOIDs.clear ();
                }

                boolean isSimplePing = false;

                //No additions and no deletions
                if ( holder.setId != null && !holder.modified )
                {
                    //send simple set ping
                    isSimplePing = true;
                }

                //seqNum will be 0 for simple ping, but incremented for complex pings. seqNum is per setId. first one will be 0 and increments
                //there on...
                holder.setId = stub.call ( isSimplePing, holder.setId, listOfAddedOIDs, listOfRemovedOIDs, isSimplePing ? 0 : holder.seqNum++ );

                logger.info ( "Within ClientPingTimerTask: holder.seqNum {}", holder.seqNum );

                holder.modified = false;
                //stub.close(); commenting this since we are caching the stub.
                if ( holder.closed )
                {
                    //this means that this set is empty and there is no need for it. The set has emptied  itself and
                    //will get removed from COM servers side as well.
                    logger.info ( "Within ClientPingTimerTask: Holder {} is empty, will remove this from mapOfSessionVsPingSetHolder", holder );
                    itr.remove ();
                    synchronized ( mutex3 )
                    {
                        mapOfSessionVsPingSetHolder.remove ( entry.getKey () );
                    }
                }
            }
        }
    }

    static
    {
        defaults2.put ( "rpc.ntlm.lanManagerKey", "false" );
        defaults2.put ( "rpc.ntlm.sign", "false" );
        defaults2.put ( "rpc.ntlm.seal", "false" );
        defaults2.put ( "rpc.ntlm.keyExchange", "false" );
        defaults2.put ( "rpc.connectionContext", "org.jinterop.dcom.transport.JIComRuntimeNTLMConnectionContext" );
        defaults.put ( "rpc.connectionContext", "org.jinterop.dcom.transport.JIComRuntimeConnectionContext" );
    }

    //ip address
    static void addUpdateOXIDs ( final JISession session, final String IPID, JIObjectId oid )
    {
        synchronized ( mutex3 )
        {
            //make sure this is the IP address
            PingSetHolder holder = (PingSetHolder)mapOfSessionVsPingSetHolder.get ( session );
            if ( holder == null )
            {
                //new 
                holder = new PingSetHolder ();
                holder.username = session.getUserName ();
                holder.password = session.getPassword ();
                holder.domain = session.getDomain ();
                holder.currentSetOIDs.put ( oid, oid );
                holder.modified = true;
                holder.seqNum = 0;
                mapOfSessionVsPingSetHolder.put ( session, holder );
            }
            else
            //found , means it is another call for a new IPID
            {
                final JIObjectId oid2 = (JIObjectId)holder.currentSetOIDs.get ( oid );
                if ( oid2 != null )
                {
                    //have to update this oid, since the one from parameters is a "new" one.
                    oid = oid2;
                }
                else
                {
                    logger.info ( "addUpdateOXIDs: Adding OID to holder {}, current size of currentSetOIDs is {}", holder, holder.currentSetOIDs.size () );
                    holder.currentSetOIDs.put ( oid, oid );
                    holder.modified = true;
                }
            }

            oid.incrementIPIDRefCountBy1 ();
            logger.info ( "addUpdateOXIDs: finally this oid is {}", oid );
        }

    }

    static void delIPIDReference ( final String IPID, JIObjectId oid, final JISession session )
    {
        synchronized ( mutex3 )
        {
            final PingSetHolder holder = (PingSetHolder)mapOfSessionVsPingSetHolder.get ( session );
            //this will be non-null, since we are trying to remove an IPID reference so the PingSet for its OID should exist
            if ( holder != null )
            {
                final JIObjectId oid2 = (JIObjectId)holder.currentSetOIDs.get ( oid );
                if ( oid2 != null )
                {
                    //temp gets replaced by the real one.
                    oid = oid2;
                }
                else
                {
                    logger.warn ( "In delIPIDReference: Could not find Original OID for this temp OID for session: {} , temp oid is {} , and IPID is {}", new Object[] { session.getSessionIdentifier (), oid, IPID } );
                    return;
                }

                //this is the same OID as in the PingSetHolder.
                oid.decrementIPIDRefCountBy1 ();

                logger.info ( "delIPIDReference: Decrementing reference count for IPID {} on OID {}", IPID, oid );

                //should we retain this now ??? , we need not send a ping for this as well. It is being retained for the last ping only. 
                if ( oid.getIPIDRefCount () <= 0 )
                {
                    holder.currentSetOIDs.remove ( oid );
                    //everything is gone, remove the session
                    if ( holder.currentSetOIDs.size () == 0 )
                    {
                        holder.closed = true;
                        mapOfSessionVsPingSetHolder.remove ( session );
                    }
                    if ( logger.isInfoEnabled () )
                    {
                        logger.info ( "delIPIDReference: sessionid " + session.getSessionIdentifier () + "Ref count is <= 0, for OID " + oid + ", holder status: " + holder.closed );
                    }
                }
            }
            else
            {
                if ( logger.isWarnEnabled () )
                {
                    logger.warn ( "In delIPIDReference: Could not find PingSetHolder for this session: " + session.getSessionIdentifier () + " , temp oid is " + oid + " , and IPID is " + IPID );
                }
            }
        }
    }

    static void clearIPIDsforSession ( final JISession session )
    {
        synchronized ( mutex3 )
        {
            //make sure this is the IP address
            final PingSetHolder holder = (PingSetHolder)mapOfSessionVsPingSetHolder.get ( session );
            if ( holder != null )
            {
                logger.info ( "clearIPIDsforSession: holder.currentSetOIDs's size is {}", holder.currentSetOIDs.size () );

                //No need to do this we are clearing the map anyways.
                //				for (Iterator itr2 = holder.currentSetOIDs.keySet().iterator();itr2.hasNext();)
                //				{
                //					JIObjectId oid = (JIObjectId)itr2.next();
                //					oid.setIPIDRefCountTo0();
                //				}

                holder.modified = true;
                holder.currentSetOIDs.clear (); //being done since this session is being destroyed and the corresponding COM server
                                                //need not be retained by us.
                holder.closed = true;

                //Should be not remove this entry ??? I think it is being retained only for the pings ... we should let this go.
                mapOfSessionVsPingSetHolder.remove ( session );
            }
        }

        //remove the socket for this session associated with ping timer
        synchronized ( mutex4 )
        {
            final JIComOxidStub stub = (JIComOxidStub)mapOfAddressVsStub.remove ( session.getTargetServer () );
            if ( stub != null )
            {
                stub.close ();
            }
        }

    }

    static synchronized void startResolverTimer ()
    {
        //schedule only 1 timer task , the task to ping the OIDs obtained.
        pingTimer_2minutes.scheduleAtFixedRate ( new ClientPingTimerTask (), 0, 4 * 60 * 1000 );
        if ( JISystem.isJavaCoClassAutoCollectionSet () )
        {
            pingTimer_8minutes.scheduleAtFixedRate ( new ServerPingTimerTask (), 0, 8 * 60 * 1000 );
        }
    }

    //only one thread , that is the main is expected to enter this one.
    static synchronized void startResolver ()
    {
        if ( resolverStarted )
        {
            return;
        }

        final Runnable thread = new Runnable () {
            @Override
            public void run ()
            {

                try
                {
                    final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open ();
                    serverSocket = serverSocketChannel.socket ();//new ServerSocket(0); //bind on any free port
                    serverSocket.bind ( null );
                    oxidResolverPort = serverSocket.getLocalPort ();
                    //System.err.println("VIKRAM: oxidResolverPort: " + oxidResolverPort);
                    // server infinite loop
                    while ( !stopSystem )
                    {
                        final Socket socket = serverSocket.accept ();
                        //listOfSockets.add(socket);
                        //System.err.println("VIKRAM: Accepting new Call from " + socket.getPort());
                        //in a multithreaded scenario this will be serialized.
                        synchronized ( mutex )
                        {
                            JISystem.internal_setSocket ( socket );
                            //now create the JIComOxidRuntimeHelper Object and start it.
                            final Properties properties = new Properties ( defaults );
                            properties.put ( "IID", "99fcfec4-5260-101b-bbcb-00aa0021347a:0.0".toUpperCase () ); //IOxidResolver
                            final JIComOxidRuntimeHelper oxidResolver = new JIComOxidRuntimeHelper ( properties );
                            oxidResolver.startOxid ( socket.getLocalPort (), socket.getPort () );
                        }

                    }
                }
                catch ( final IOException e )
                {
                    //e.printStackTrace();
                }

                //close all sockets.
                //			    for (int i = 0; i < listOfSockets.size(); i++)
                //			    {
                //			    	Socket s = (Socket)listOfSockets.get(i);
                //			    	try {
                //						s.close();
                //					} catch (IOException e) {}
                //			    }
            }
        };

        final Thread thread2 = new Thread ( thread, "jI_OxidResolver" );
        thread2.setDaemon ( true );
        thread2.start ();
        resolverStarted = true;
    }

    static int getOxidResolverPort ()
    {
        return oxidResolverPort;
    }

    //Will be called from shutDownHook thread.
    static synchronized void stopResolver ()
    {
        stopSystem = true;
        try
        {
            serverSocket.close ();
        }
        catch ( final IOException e )
        {
        }

        pingTimer_2minutes.cancel ();
        pingTimer_8minutes.cancel ();

        final Iterator itr = mapOfAddressVsStub.values ().iterator ();
        while ( itr.hasNext () )
        {
            final JIComOxidStub s = (JIComOxidStub)itr.next ();
            s.close ();
        }
        mapOfAddressVsStub.clear (); //will clean up all the others as well
    }

    /**
     * Returns the MIP for the Java Instance, this will also have the
     * OXID,OID,IPID
     * for the same.
     * 
     * @param javaInstance
     * @return
     */
    static JIInterfacePointer getInterfacePointer ( final JISession session, final JILocalCoClass component ) throws JIException
    {
        JIInterfacePointer ptr = null;

        synchronized ( mutex2 )
        {
            if ( component.isAlreadyExported () )
            {
                throw new JIException ( JIErrorCodes.JI_JAVACOCLASS_ALREADY_EXPORTED );
            }

            component.setSession ( session );
            //
            //			JIComOxidDetails details = 	(JIComOxidDetails)mapOfJavaVsOxidDetails.get(component);
            //			
            //			if (details != null)
            //			{
            //				return details.getInterfacePtr();
            //			}

            //as the ID could be repeated, this is the ipid of the interface being requested.
            final String ipid = UUIDGenerator.generateID ();
            final String iid = component.isCoClassUnderRealIID () ? component.getCoClassIID () : IJIComObject.IID;//has to be IUnknown's IID.
            final byte[] bytes = new byte[8];
            randomGen.nextBytes ( bytes );
            final JIOxid oxid = new JIOxid ( bytes );
            final byte[] bytes2 = new byte[8];
            randomGen.nextBytes ( bytes2 );

            final JIObjectId oid = new JIObjectId ( bytes2, false );

            component.setObjectId ( oid.getOID () );

            //JIComOxidDetails details = new JIComOxidDetails();
            final JIStdObjRef objref = new JIStdObjRef ( ipid, oxid, oid );
            ptr = new JIInterfacePointer ( iid, oxidResolverPort, objref );

            final Properties properties = new Properties ( defaults2 );
            properties.put ( "IID", "00000131-0000-0000-C000-000000000046:0.0".toUpperCase () ); //IRemUnknown

            properties.put ( "rpc.ntlm.domain", session.getTargetServer () );

            int protecttionLevel = 2;

            if ( session.isSessionSecurityEnabled () )
            {
                protecttionLevel = 6;
                properties.setProperty ( "rpc.ntlm.seal", "true" );
                properties.setProperty ( "rpc.ntlm.sign", "true" );
                properties.setProperty ( "rpc.ntlm.keyExchange", "true" );
                properties.setProperty ( "rpc.ntlm.keyLength", "128" );
                properties.setProperty ( "rpc.ntlm.ntlm2", "true" );
                properties.setProperty ( Security.USERNAME, session.getUserName () );
                properties.setProperty ( Security.PASSWORD, session.getPassword () );
                properties.setProperty ( "rpc.ntlm.ntlm2", "true" );
            }

            if ( session.isNTLMv2Enabled () )
            {
                properties.setProperty ( "rpc.ntlm.ntlmv2", "true" );
            }

            final JIComOxidRuntimeHelper remUnknown = new JIComOxidRuntimeHelper ( properties );

            //now create a new JIComOxidDetails
            //this carries a reference to the javaInstance , incase we do not get pings from the client
            //at the right times, the cleaup thread will remove this entry and it's OXID as well from both the maps.
            final JIComOxidDetails details = new JIComOxidDetails ( component, oxid, oid, iid, ipid, ptr, remUnknown, protecttionLevel );

            mapOfJavaVsOxidDetails.put ( component, details );

            mapOfOxidVsOxidDetails.put ( oxid, details );

            mapOfOIDVsComponents.put ( oid, component );

            listOfExportedJavaComponents.add ( component );

            mapOfIPIDVsComponent.put ( ipid, details ); //this is the ipid of the component.

            List oids = (ArrayList)mapOfSessionIdsVsOIDs.get ( new Integer ( session.getSessionIdentifier () ) );
            if ( oids == null )
            {
                oids = new ArrayList ();
                mapOfSessionIdsVsOIDs.put ( new Integer ( session.getSessionIdentifier () ), oids );
            }
            oids.add ( oid );

            component.setAssociatedInterfacePointer ( ptr );
        }
        return ptr;
    }

    //will get called from OxidResolverImpl only
    static JIComOxidDetails getOxidDetails ( final JIOxid oxid )
    {
        synchronized ( mutex2 )
        {
            return (JIComOxidDetails)mapOfOxidVsOxidDetails.get ( oxid );
        }
    }

    //Will get called from RemQueryInterface of IRemUnknown, when it gets the IPID 
    //it will identify the correct component to act on.
    //on this component the IID (provided again by the client) will do a exportInstance, with a 
    //randomly generated IPID and this IPID will be returned to the client.
    //The oid be the one present in details object.
    //Now , when the alter context call will come with the new IID (which was just QIed), the 
    //state of RemUnknownObject will get set for the correct component using getJavaComponentForIID.
    //The next call of requestcopdu will contain the request along with the field object having the IPID of the 
    //instance to call on. Pass this to the components (identified previously) invoke API., along with the rest of params
    //How will the request get decoded with out IDL info ??? Hard code for now for toString ??
    static JIComOxidDetails getComponentFromIPID ( final String ipid )
    {
        synchronized ( mutex2 )
        {
            return (JIComOxidDetails)mapOfIPIDVsComponent.get ( ipid );
        }
    }

    static void addUpdateSets ( final JISetId setId, final ArrayList objectIdsAdded, final ArrayList objectIdsDel )
    {
        synchronized ( mutex2 )
        {

            ArrayList listOfOIDs = (ArrayList)mapOfSetIdVsListOfOIDs.get ( setId );

            if ( listOfOIDs == null )
            {
                listOfOIDs = new ArrayList ();
                //first time
                listOfOIDs.addAll ( objectIdsAdded );
                mapOfSetIdVsListOfOIDs.put ( setId, listOfOIDs );
                //del list would be empty I presume

            }
            else
            {
                for ( int i = 0; i < listOfOIDs.size (); i++ )
                {
                    final JIObjectId oid = (JIObjectId)listOfOIDs.get ( i );
                    if ( !objectIdsDel.contains ( oid ) )
                    {
                        oid.updateLastPingTime ();
                    }
                }

                listOfOIDs.addAll ( objectIdsAdded );
            }

        }
    }

    //since the IID is unique and we have to consider nested IIDs, this API will not work for component's IID
    //	static JILocalCoClass getJavaComponentForIID(String uniqueIID) 
    //	{
    //		JILocalCoClass component = null;
    //		synchronized (mutex2) {
    //			for (int i = 0; i < listOfExportedJavaComponents.size(); i++ )
    //			{
    //				component = (JILocalCoClass)listOfExportedJavaComponents.get(i);
    //				if (component.isPresent(uniqueIID))
    //				{
    //					break;
    //				}
    //				component = null;
    //			}
    //		}
    //		
    //		return component;
    //	}

    static JILocalCoClass getJavaComponentFromIPID ( final String ipid )
    {
        JILocalCoClass component = null;
        synchronized ( mutex2 )
        {
            for ( int i = 0; i < listOfExportedJavaComponents.size (); i++ )
            {
                component = (JILocalCoClass)listOfExportedJavaComponents.get ( i );
                //this will be unique, no two components will ever have same IPID for an IID.They will have different IPIDs for same IIDs.
                if ( component.getIIDFromIpid ( ipid ) != null )
                {
                    break;
                }
                component = null;
            }
        }

        return component;
    }

}
