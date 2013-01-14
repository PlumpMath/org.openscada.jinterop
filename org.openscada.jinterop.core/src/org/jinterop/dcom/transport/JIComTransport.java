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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import ndr.NdrBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.Endpoint;
import rpc.ProviderException;
import rpc.RpcException;
import rpc.Transport;
import rpc.core.PresentationSyntax;

/**
 * Borrowed all from ncacn_ip_tcp.RpcTransport from jarapac, modified attach api
 * to include SocketChannel.
 * 
 * @exclude
 * @since 1.0
 */
final class JIComTransport implements Transport
{

    private final static Logger logger = LoggerFactory.getLogger ( JIComTransport.class );

    public static final String PROTOCOL = "ncacn_ip_tcp";

    private static final String LOCALHOST;

    private final Properties properties;

    private String host;

    private int port;

    private Socket socket;

    private OutputStream output;

    private InputStream input;

    private boolean attached;

    private boolean timeoutModifiedfrom0 = false;

    private SocketChannel channel = null;

    static
    {
        String localhost = null;
        try
        {
            localhost = InetAddress.getLocalHost ().getHostName ();
        }
        catch ( final UnknownHostException ex )
        {
        }
        LOCALHOST = localhost;
    }

    public JIComTransport ( final String address, final Properties properties ) throws ProviderException
    {
        this.properties = properties;
        parse ( address );
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
        try
        {
            if ( logger.isDebugEnabled () )
            {
                logger.debug ( "Opening socket on " + new InetSocketAddress ( InetAddress.getByName ( this.host ), this.port ) );
            }

            this.channel = SocketChannel.open ( new InetSocketAddress ( InetAddress.getByName ( this.host ), this.port ) );
            this.socket = this.channel.socket ();//new Socket(host, port);
            this.output = null;
            this.input = null;
            this.attached = true;
            this.socket.setKeepAlive ( true );//backup for not providing a timeout.
            return new JIComEndpoint ( this, syntax );
        }
        catch ( final IOException ex )
        {
            try
            {
                close ();
            }
            catch ( final Exception ignore )
            {
            }
            throw ex;
        }
    }

    @Override
    public void close () throws IOException
    {
        try
        {
            if ( this.socket != null )
            {
                //            	input.close();
                //            	output.close();
                this.socket.shutdownInput ();
                this.socket.shutdownOutput ();
                this.socket.close ();
                this.channel.close ();

                logger.debug ( "Socket closed... {} host {}, port {}", new Object[] { this.socket, this.host, this.port } );
            }
        }
        finally
        {
            this.attached = false;
            this.socket = null;
            this.output = null;
            this.input = null;
            this.channel = null;
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
        this.channel.configureBlocking ( true );
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
        applySocketTimeout ();
        if ( this.input == null )
        {
            this.input = this.socket.getInputStream ();
        }
        buffer.length = this.input.read ( buffer.getBuffer (), 0, buffer.getCapacity () );
    }

    private void applySocketTimeout ()
    {
        int timeout = 0;
        try
        {
            timeout = Integer.parseInt ( this.properties.getProperty ( "rpc.socketTimeout", "0" ) );
            if ( timeout != 0 )
            {
                this.socket.setSoTimeout ( timeout );
                this.timeoutModifiedfrom0 = true;
            }
            else
            {
                if ( this.timeoutModifiedfrom0 )
                {
                    this.socket.setSoTimeout ( timeout );
                    this.timeoutModifiedfrom0 = false;
                }
            }
        }
        catch ( final Exception e )
        {
        }
    }

    protected void parse ( String address ) throws ProviderException
    {
        if ( address == null )
        {
            throw new ProviderException ( "Null address." );
        }
        if ( !address.startsWith ( "ncacn_ip_tcp:" ) )
        {
            throw new ProviderException ( "Not an ncacn_ip_tcp address." );
        }
        address = address.substring ( 13 );
        int index = address.indexOf ( '[' );
        if ( index == -1 )
        {
            throw new ProviderException ( "No port specifier present." );
        }
        String server = address.substring ( 0, index );
        address = address.substring ( index + 1 );
        index = address.indexOf ( ']' );
        if ( index == -1 )
        {
            throw new ProviderException ( "Port specifier not terminated." );
        }
        address = address.substring ( 0, index );
        if ( "".equals ( server ) )
        {
            server = LOCALHOST;
        }
        try
        {
            this.port = Integer.parseInt ( address );
        }
        catch ( final Exception ex )
        {
            throw new ProviderException ( "Invalid port specifier." );
        }
        this.host = server;
    }

}
