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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the oxid details in memory.
 * 
 * @since 1.0
 */
final class JIComOxidDetails
{

    private final static Logger logger = LoggerFactory.getLogger ( JIComOxidDetails.class );

    private JILocalCoClass referent = null;

    private String ipid = null;

    private String remUnknownIpid = null;

    private JIOxid oxid = null;

    private JIObjectId oid = null;

    private String iid = null;

    private JIComOxidRuntimeHelper comRuntimeHelper = null;

    private int portForRemUnknown = -1;

    private int protectionLevel = 2;

    private ThreadGroup remUnknownThread = null;

    JIComOxidDetails ( final JILocalCoClass javaInstance, final JIOxid oxid, final JIObjectId oid, final String iid, final String ipid, final JIInterfacePointer ptr, final JIComOxidRuntimeHelper helper, final int protectionLevel )
    {
        this.referent = javaInstance;
        this.ipid = ipid;
        this.oxid = oxid;
        this.oid = oid;
        this.iid = iid;
        this.protectionLevel = protectionLevel;
        this.comRuntimeHelper = helper;
    }

    void setPortForRemUnknown ( final int port )
    {
        this.portForRemUnknown = port;
    }

    int getPortForRemUnknown ()
    {
        return this.portForRemUnknown;
    }

    String getIID ()
    {
        return this.iid;
    }

    String getIpid ()
    {
        return this.ipid;
    }

    String getRemUnknownIpid ()
    {
        return this.remUnknownIpid;
    }

    void setRemUnknownIpid ( final String ipid )
    {
        this.remUnknownIpid = ipid;
    }

    JIObjectId getOid ()
    {
        return this.oid;
    }

    JIOxid getOxid ()
    {
        return this.oxid;
    }

    JILocalCoClass getReferent ()
    {
        return this.referent;
    }

    JIComOxidRuntimeHelper getCOMRuntimeHelper ()
    {
        return this.comRuntimeHelper;
    }

    int getProtectionLevel ()
    {
        return this.protectionLevel;
    }

    void setRemUnknownThreadGroup ( final ThreadGroup remUnknown )
    {
        this.remUnknownThread = remUnknown;
    }

    void interruptRemUnknownThreadGroup ()
    {
        if ( this.remUnknownThread != null )
        {
            try
            {
                this.remUnknownThread.interrupt ();
                //				remUnknownThread.destroy();
            }
            catch ( final Exception e )
            {
                logger.info ( "JIComOxidDetails interruptRemUnknownThreadGroup", e );
            }
        }
    }
}
