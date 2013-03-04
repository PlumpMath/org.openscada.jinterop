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

package org.jinterop.winreg.smb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Properties;

import jcifs.smb.SmbException;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JIRuntimeException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.winreg.IJIWinReg;
import org.jinterop.winreg.JIPolicyHandle;

import rpc.Endpoint;
import rpc.Stub;

/**
 * @exclude
 * @since 1.0
 */
public class JIWinRegStub extends Stub implements IJIWinReg
{

    //"ncacn_np:" + servername + "[\\PIPE\\winreg]"
    public JIWinRegStub ( final IJIAuthInfo authInfo, String serverName ) throws UnknownHostException
    {
        super ();
        if ( authInfo == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_AUTH_NOT_SUPPLIED ) );
        }

        super.setTransportFactory ( new rpc.ncacn_np.TransportFactory () );
        super.setProperties ( new Properties () );
        super.getProperties ().setProperty ( "rpc.ncacn_np.username", authInfo.getUserName () );
        String password = null;
        try
        {
            password = URLEncoder.encode ( authInfo.getPassword (), "utf-8" );
        }
        catch ( final UnsupportedEncodingException e )
        {
            try
            {
                password = URLEncoder.encode ( authInfo.getPassword (), System.getProperty ( "file.encoding" ) );
            }
            catch ( final UnsupportedEncodingException e1 )
            {
                throw new JIRuntimeException ( JIErrorCodes.JI_WINREG_EXCEPTION2 );
            }
        }
        //some strange issue with the space character, it gets encoded to '+' (which is right) , but Windows refuses it.
        //Manually changing + to %20
        final StringBuffer password_ = new StringBuffer ();
        for ( int i = 0; i < password.length (); i++ )
        {
            final char ch = password.charAt ( i );
            if ( ch == '+' )
            {
                password_.append ( "%20" );
                continue;
            }

            password_.append ( ch );
        }

        super.getProperties ().setProperty ( "rpc.ncacn_np.password", password_.toString () );
        super.getProperties ().setProperty ( "rpc.ncacn_np.domain", authInfo.getDomain () );
        serverName = serverName.trim ();
        serverName = InetAddress.getByName ( serverName ).getHostAddress ();
        super.setAddress ( "ncacn_np:" + serverName + "[\\PIPE\\winreg]" );

    }

    public JIWinRegStub ( String serverName ) throws UnknownHostException
    {
        super ();
        super.setTransportFactory ( new rpc.ncacn_np.TransportFactory () );
        super.setProperties ( new Properties () );
        super.getProperties ().setProperty ( "rpc.ntlm.sso", "true" );
        serverName = serverName.trim ();
        serverName = InetAddress.getByName ( serverName ).getHostAddress ();
        super.setAddress ( "ncacn_np:" + serverName + "[\\PIPE\\winreg]" );

    }

    @Override
    public JIPolicyHandle winreg_OpenHKLM () throws JIException
    {
        final openHKLM openhklm = new openHKLM ();
        final JIPolicyHandle handle = new JIPolicyHandle ( false );
        try
        {
            call ( Endpoint.IDEMPOTENT, openhklm );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.JI_WINREG_EXCEPTION, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        System.arraycopy ( openhklm.policyhandle, 0, handle.handle, 0, 20 );

        return handle;
    }

    @Override
    public JIPolicyHandle winreg_OpenHKCR () throws JIException
    {
        final openHKCR openhkcr = new openHKCR ();
        final JIPolicyHandle handle = new JIPolicyHandle ( false );
        try
        {
            call ( Endpoint.IDEMPOTENT, openhkcr );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.JI_WINREG_EXCEPTION, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        System.arraycopy ( openhkcr.policyhandle, 0, handle.handle, 0, 20 );

        return handle;
    }

    @Override
    public JIPolicyHandle winreg_OpenHKCU () throws JIException
    {
        final openHKCU openhkcu = new openHKCU ();
        final JIPolicyHandle handle = new JIPolicyHandle ( false );
        try
        {
            call ( Endpoint.IDEMPOTENT, openhkcu );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.JI_WINREG_EXCEPTION, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        System.arraycopy ( openhkcu.policyhandle, 0, handle.handle, 0, 20 );

        return handle;
    }

    @Override
    public JIPolicyHandle winreg_OpenHKU () throws JIException
    {
        final openHKU openhku = new openHKU ();
        final JIPolicyHandle handle = new JIPolicyHandle ( false );
        try
        {
            call ( Endpoint.IDEMPOTENT, openhku );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.JI_WINREG_EXCEPTION, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        System.arraycopy ( openhku.policyhandle, 0, handle.handle, 0, 20 );

        return handle;
    }

    @Override
    public JIPolicyHandle winreg_OpenKey ( final JIPolicyHandle handle, final String key, final int accessMask ) throws JIException
    {
        final openKey openkey = new openKey ();
        openkey.accessMask = accessMask;
        openkey.key = key;
        openkey.parentKey = handle;
        final JIPolicyHandle newHandle = new JIPolicyHandle ( false );
        try
        {
            call ( Endpoint.IDEMPOTENT, openkey );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        System.arraycopy ( openkey.policyhandle, 0, newHandle.handle, 0, 20 );

        return newHandle;
    }

    @Override
    public void winreg_CloseKey ( final JIPolicyHandle handle ) throws JIException
    {
        final closeKey closekey = new closeKey ();
        closekey.key = handle;
        try
        {
            call ( Endpoint.IDEMPOTENT, closekey );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }
    }

    @Override
    public void winreg_DeleteKeyOrValue ( final JIPolicyHandle handle, final String valueName, final boolean isKey ) throws JIException
    {
        final deleteValueOrKey delete = new deleteValueOrKey ();
        delete.parentKey = handle;
        delete.valueName = valueName;
        delete.isKey = isKey;
        try
        {
            call ( Endpoint.IDEMPOTENT, delete );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }
    }

    @Override
    public byte[] winreg_QueryValue ( final JIPolicyHandle handle, final int bufferSize ) throws JIException
    {
        final queryValue queryvalue = new queryValue ();
        queryvalue.parentKey = handle;
        queryvalue.bufferLength = bufferSize;
        try
        {
            call ( Endpoint.IDEMPOTENT, queryvalue );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        //return queryvalue.key;
        return queryvalue.buffer;
    }

    @Override
    public Object[] winreg_QueryValue ( final JIPolicyHandle handle, final String valueName, final int bufferSize ) throws JIException
    {
        final queryValue queryvalue = new queryValue ();
        queryvalue.parentKey = handle;
        queryvalue.bufferLength = bufferSize;
        queryvalue.key = valueName;

        try
        {
            call ( Endpoint.IDEMPOTENT, queryvalue );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        return new Object[] { new Integer ( queryvalue.type ), queryvalue.buffer != null ? (Object)queryvalue.buffer : (Object)queryvalue.buffer2 };
    }

    @Override
    public void winreg_SaveFile ( final JIPolicyHandle handle, final String fileName ) throws JIException
    {
        final saveFile savefile = new saveFile ();
        savefile.parentKey = handle;
        savefile.fileName = fileName;

        try
        {
            call ( Endpoint.IDEMPOTENT, savefile );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

    }

    @Override
    public JIPolicyHandle winreg_CreateKey ( final JIPolicyHandle handle, final String subKey, final int options, final int accessMask ) throws JIException
    {
        final createKey createkey = new createKey ();
        createkey.accessMask = accessMask;
        createkey.key = subKey;
        createkey.parentKey = handle;
        createkey.options = options;

        try
        {
            call ( Endpoint.IDEMPOTENT, createkey );
        }
        catch ( final SmbException e )
        {
            throw new JIException ( e.getNtStatus (), e );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        final JIPolicyHandle newHandle = new JIPolicyHandle ( createkey.actiontaken == 1 ? true : false );
        System.arraycopy ( createkey.policyhandle, 0, newHandle.handle, 0, 20 );

        return newHandle;
    }

    @Override
    public void winreg_SetValue ( final JIPolicyHandle handle, final String valueName, final byte[][] data ) throws JIException
    {
        if ( data == null )
        {
            throw new IllegalArgumentException ( JISystem.getLocalizedMessage ( JIErrorCodes.JI_WINREG_EXCEPTION5 ) );
        }

        //calculate length of all strings + extra null in the end
        final int totalStrings = data.length;
        int length = 0;
        for ( int i = 0; i < totalStrings; i++ )
        {
            final int j = data[i].length;
            length = length + ( j + 1 ) * 2; //including null termination
        }

        length = length + 2; //final termination

        final setValue setvalue = new setValue ();
        setvalue.clazzType = REG_MULTI_SZ;
        setvalue.data2 = data;
        setvalue.lengthInBytes = length;
        setvalue.parentKey = handle;
        setvalue.valueName = valueName;
        setValue ( setvalue );
    }

    @Override
    public void winreg_SetValue ( final JIPolicyHandle handle, final String valueName ) throws JIException
    {
        final setValue setvalue = new setValue ();
        setvalue.clazzType = REG_NONE;
        setvalue.parentKey = handle;
        setvalue.valueName = valueName;
        setValue ( setvalue );
    }

    @Override
    public void winreg_SetValue ( final JIPolicyHandle handle, final String valueName, final byte[] data, final boolean isBinary, final boolean expand_sz ) throws JIException
    {
        final setValue setvalue = new setValue ();
        if ( isBinary )
        {
            setvalue.clazzType = REG_BINARY;
        }
        else
        {
            if ( expand_sz )
            {
                setvalue.clazzType = REG_EXPAND_SZ;
            }
            else
            {
                setvalue.clazzType = REG_SZ;
            }
        }

        setvalue.data = data;
        setvalue.lengthInBytes = data.length;
        setvalue.parentKey = handle;
        setvalue.valueName = valueName;
        setValue ( setvalue );
    }

    @Override
    public void winreg_SetValue ( final JIPolicyHandle handle, final String valueName, final int data ) throws JIException
    {
        final setValue setvalue = new setValue ();
        setvalue.clazzType = REG_DWORD;
        setvalue.lengthInBytes = 4;
        setvalue.dword = data;
        setvalue.parentKey = handle;
        setvalue.valueName = valueName;
        setValue ( setvalue );
    }

    @Override
    public String[] winreg_EnumKey ( final JIPolicyHandle handle, final int index ) throws JIException
    {
        final enumKey enumkey = new enumKey ();
        enumkey.parentKey = handle;
        enumkey.index = index;

        try
        {
            call ( Endpoint.IDEMPOTENT, enumkey );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        return enumkey.retval;
    }

    @Override
    public Object[] winreg_EnumValue ( final JIPolicyHandle handle, final int index ) throws JIException
    {
        final enumValue enumvalue = new enumValue ();
        enumvalue.parentKey = handle;
        enumvalue.index = index;

        try
        {
            call ( Endpoint.IDEMPOTENT, enumvalue );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }

        return enumvalue.retval;
    }

    private void setValue ( final setValue setvalue ) throws JIException
    {
        try
        {
            call ( Endpoint.IDEMPOTENT, setvalue );
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
        catch ( final JIRuntimeException e )
        {
            throw new JIException ( e );
        }
    }

    @Override
    protected String getSyntax ()
    {
        // WinReg Service
        return "338cd001-2244-31f1-aaaa-900038001003:1.0";
    }

    @Override
    public void closeConnection () throws JIException
    {
        try
        {
            super.detach ();
        }
        catch ( final IOException e )
        {
            throw new JIException ( JIErrorCodes.RPC_E_UNEXPECTED, e );
        }
    }

}
