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

import java.util.ArrayList;
import java.util.HashMap;

import ndr.NdrBuffer;
import ndr.NetworkDataRepresentation;

import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal Framework Helper class. Do not use outside of framework.
 * 
 * @exclude
 */
public final class JIFrameworkHelper
{

    private final static Logger logger = LoggerFactory.getLogger ( JIFrameworkHelper.class );

    /**
     * @exclude
     * @param src
     * @param target
     */
    static void link2Sessions ( final JISession src, final JISession target )
    {
        if ( src == null || target == null )
        {
            throw new NullPointerException ();
        }

        JISession.linkTwoSessions ( src, target );
    }

    /**
     * @exclude
     * @param src
     * @param target
     */
    static void unLinkSession ( final JISession src, final JISession unlinkedSession )
    {
        if ( src == null || unlinkedSession == null )
        {
            throw new NullPointerException ();
        }

        JISession.unLinkSession ( src, unlinkedSession );
    }

    /**
     * @exclude
     * @param src
     * @param target
     */
    static JISession resolveSessionForOXID ( final byte[] oxid )
    {
        return JISession.resolveSessionForOxid ( new JIOxid ( oxid ) );
    }

    /**
     * @exclude
     * @param src
     * @param target
     */
    static JIInterfacePointer getInterfacePointerOfStub ( final JISession session )
    {
        return session.getStub ().getServerInterfacePointer ();
    }

    /**
     * Must be called once and only once from JICallBuilder "read" to create the
     * right pointer in case of man in the middle scenario and
     * add it to the session.
     * 
     * @param session
     * @param ptr
     * @return
     * @throws JIException
     */
    static IJIComObject instantiateComObject ( final JISession session, final JIInterfacePointer ptr ) throws JIException
    {
        final IJIComObject retval = instantiateComObject2 ( session, ptr );
        addComObjectToSession ( retval.getAssociatedSession (), retval );
        return retval;
    }

    static IJIComObject instantiateComObject2 ( JISession session, final JIInterfacePointer ptr ) throws JIException
    {
        if ( ptr == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COMFACTORY_ILLEGAL_ARG ) );
        }

        IJIComObject retval = null;
        final JIInterfacePointer stubPtr = JIFrameworkHelper.getInterfacePointerOfStub ( session );
        if ( !JIInterfacePointer.isOxidEqual ( stubPtr, ptr ) )
        {
            logger.warn ( "NEW SESSION IDENTIFIED ! for ptr {}", ptr );
            //first check if a session for this OXID does not already exist and thus its stub
            JISession newsession = JIFrameworkHelper.resolveSessionForOXID ( ptr.getOXID () );
            if ( newsession == null )
            {
                //new COM server pointer
                newsession = JISession.createSession ( session );
                newsession.setGlobalSocketTimeout ( session.getGlobalSocketTimeout () );
                newsession.useSessionSecurity ( session.isSessionSecurityEnabled () );
                newsession.useNTLMv2 ( session.isNTLMv2Enabled () );
                final JIComServer comServer = new JIComServer ( newsession, ptr, null );
                retval = comServer.getInstance ();
                JIFrameworkHelper.link2Sessions ( session, newsession );
            }
            //			else
            //			{
            //				retval = new JIComObjectImpl(newsession,ptr);
            //			}

            //this is so that the reference gets added correctly.
            session = newsession;
        }

        if ( retval == null )
        {
            retval = new JIComObjectImpl ( session, ptr );
        }

        return retval;
    }

    static void addComObjectToSession ( final JISession session, final IJIComObject comObject )
    {
        session.addToSession ( comObject, comObject.internal_getInterfacePointer ().getOID () );
    }

    /**
     * Returns an Interface Pointer representation for the Java Component
     * 
     * @exclude
     * @param javaComponent
     * @return
     */
    public static IJIComObject instantiateLocalComObject ( final JISession session, final JILocalCoClass javaComponent ) throws JIException
    {
        return new JIComObjectImpl ( session, JIComOxidRuntime.getInterfacePointer ( session, javaComponent ), true );
    }

    /**
     * Returns an Interface Pointer representation from raw bytes.
     * 
     * @exclude
     * @param session
     * @param rawBytes
     * @return
     * @throws JIException
     */
    public static IJIComObject instantiateComObject ( final JISession session, final byte[] rawBytes, final String ipAddress ) throws JIException
    {
        final NetworkDataRepresentation ndr = new NetworkDataRepresentation ();
        final NdrBuffer ndrBuffer = new NdrBuffer ( rawBytes, 0 );
        ndr.setBuffer ( ndrBuffer );
        ndrBuffer.length = rawBytes.length;

        //this is a brand new session.
        if ( session.getStub () == null )
        {
            final JIComServer comServer = new JIComServer ( session, JIInterfacePointer.decode ( ndr, new ArrayList (), JIFlags.FLAG_REPRESENTATION_INTERFACEPTR_DECODE2, new HashMap () ), ipAddress );
            return comServer.getInstance ();
        }
        else
        {
            final IJIComObject retval = instantiateComObject ( session, JIInterfacePointer.decode ( ndr, new ArrayList (), JIFlags.FLAG_REPRESENTATION_INTERFACEPTR_DECODE2, new HashMap () ) );
            //increasing the reference count.
            retval.addRef ();
            return retval;
        }
    }

    /**
     * Typically used in the Man-In-The-Middle scenario, where one j-Interop
     * system interacts with another over the wire.
     * Or the IJIComObject is deserialized from a Database and is right now
     * drifting.
     * 
     * @exclude
     * @param session
     * @param comObject
     * @return
     * @throws JIException
     */
    public static IJIComObject instantiateComObject ( final JISession session, final IJIComObject comObject ) throws JIException
    {
        if ( comObject.getAssociatedSession () != null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_SESSION_ALREADY_ATTACHED ) );
        }

        if ( comObject.isLocalReference () )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_COMOBJ_LOCAL_REF ) );
        }

        return instantiateComObject ( session, comObject.internal_getInterfacePointer () );
    }

    /**
     * @exclude
     * @param comObject
     * @param identifier
     * @throws JIException
     */
    public static void detachEventHandler ( final IJIComObject comObject, final String identifier ) throws JIException
    {
        final Object[] connectionInfo = comObject.internal_getConnectionInfo ( identifier );
        if ( connectionInfo == null )
        {
            throw new JIException ( JIErrorCodes.JI_CALLBACK_INVALID_ID );
        }

        logger.info ( "Detaching event handler for  comObject: {} , identifier: {}", comObject.getInterfaceIdentifier (), identifier );

        final IJIComObject connectionPointer = (IJIComObject)connectionInfo[0];

        //first use the cookie to detach.
        final JICallBuilder object = new JICallBuilder ( true );
        object.setOpnum ( 3 );
        object.addInParamAsInt ( ( (Integer)connectionInfo[1] ).intValue (), JIFlags.FLAG_NULL );
        connectionPointer.call ( object );
        //now release the connectionPointer.
        connectionPointer.release ();
    }

    /**
     * @exclude
     * @param comObject
     * @param sourceUUID
     * @param eventListener
     * @return
     * @throws JIException
     */
    public static String attachEventHandler ( final IJIComObject comObject, final String sourceUUID, final IJIComObject eventListener ) throws JIException
    {
        if ( eventListener == null || comObject == null || sourceUUID == null || sourceUUID.equalsIgnoreCase ( "" ) )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_CALLBACK_INVALID_PARAMS ) );
        }

        if ( logger.isInfoEnabled () )
        {
            logger.info ( "Attaching event handler for  comObject: " + comObject.getInterfaceIdentifier () + " , sourceUUID: " + sourceUUID + " , eventListener: " + eventListener.getInterfaceIdentifier () + " and eventListner IPID: " + eventListener.getIpid () );
        }
        //IID of IConnectionPointContainer :- B196B284-BAB4-101A-B69C-00AA00341D07
        final IJIComObject connectionPointContainer = comObject.queryInterface ( "B196B284-BAB4-101A-B69C-00AA00341D07" );
        final JICallBuilder object = new JICallBuilder ( true );
        object.setOpnum ( 1 );
        object.addInParamAsUUID ( sourceUUID, JIFlags.FLAG_NULL );
        object.addOutParamAsObject ( IJIComObject.class, JIFlags.FLAG_NULL );
        final Object[] objects = connectionPointContainer.call ( object ); //find connection point
        final IJIComObject connectionPointer = (IJIComObject)objects[0];

        object.reInit ();
        object.setOpnum ( 2 );
        object.addInParamAsComObject ( eventListener, JIFlags.FLAG_NULL );
        object.addOutParamAsType ( Integer.class, JIFlags.FLAG_NULL );
        final Object[] obj = connectionPointer.call ( object );

        //used to unadvise from the connectionpoint
        final Integer dwcookie = (Integer)obj[0];

        logger.info ( "Event handler returned cookie {}", dwcookie );
        connectionPointContainer.release ();

        return comObject.internal_setConnectionInfo ( connectionPointer, dwcookie );

    }

    public static int reverseArrayForDispatch ( final JIArray arrayToReverse )
    {
        return arrayToReverse.reverseArrayForDispatch ();
    }
}
