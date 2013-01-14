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
package org.jinterop.dcom.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

import ndr.NdrBuffer;

import org.jinterop.dcom.common.JISystem;

import rpc.Endpoint;
import rpc.ProviderException;
import rpc.RpcException;
import rpc.Transport;
import rpc.core.PresentationSyntax;

/**
 * @exclude
 * @since 1.0
 */
final class JIComRuntimeTransport implements Transport
{

    public static final String PROTOCOL = "ncacn_ip_tcp";

    private final Properties properties;

    private Socket socket;

    private OutputStream output;

    private InputStream input;

    private boolean attached;

    public JIComRuntimeTransport ( final String address, final Properties properties ) throws ProviderException
    {
        this.properties = properties;
        //address is ignored
    }

    @Override
    public String getProtocol ()
    {
        return PROTOCOL;
    }

    @Override
    public Properties getProperties ()
    {
        return this.properties;
    }

    @Override
    public Endpoint attach ( final PresentationSyntax syntax ) throws IOException
    {
        if ( this.attached )
        {
            throw new RpcException ( "Transport already attached." );
        }

        Endpoint endPoint = null;
        try
        {
            this.socket = (Socket)JISystem.internal_getSocket ();
            this.output = null;
            this.input = null;
            this.attached = true;
            endPoint = new JIComRuntimeEndpoint ( this, syntax );
        }
        catch ( final Exception ex )
        {
            try
            {
                close ();
            }
            catch ( final Exception ignore )
            {
            }
        }
        return endPoint;
    }

    @Override
    public void close () throws IOException
    {
        try
        {
            if ( this.socket != null )
            {
                this.socket.close ();
            }
        }
        finally
        {
            this.attached = false;
            this.socket = null;
            this.output = null;
            this.input = null;
        }
    }

    @Override
    public void send ( final NdrBuffer buffer ) throws IOException
    {
        if ( !this.attached )
        {
            throw new RpcException ( "Transport not attached." );
        }
        if ( this.output == null )
        {
            this.output = this.socket.getOutputStream ();
        }
        this.output.write ( buffer.getBuffer (), 0, buffer.getLength () );
        this.output.flush ();
    }

    @Override
    public void receive ( final NdrBuffer buffer ) throws IOException
    {
        if ( !this.attached )
        {
            throw new RpcException ( "Transport not attached." );
        }
        if ( this.input == null )
        {
            this.input = this.socket.getInputStream ();
        }
        buffer.length = this.input.read ( buffer.getBuffer (), 0, buffer.getCapacity () );
    }

}
