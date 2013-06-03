/* Donated by Jarapac (http://jarapac.sourceforge.net/)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110, USA
 */

package rpc.security.ntlm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.ntlmssp.NtlmFlags;
import ndr.NdrBuffer;
import ndr.NetworkDataRepresentation;

import org.bouncycastle.crypto.StreamCipher;

import rpc.IntegrityException;
import rpc.Security;

public class Ntlm1 implements NtlmFlags, Security
{

    private static final int NTLM1_VERIFIER_LENGTH = 16;

    private final StreamCipher clientCipher;

    private final StreamCipher serverCipher;

    private final byte[] clientSigningKey;

    private final byte[] serverSigningKey;

    private final NTLMKeyFactory keyFactory;

    private boolean isServer = false;

    private final int protectionLevel;

    private int requestCounter = 0;

    private int responseCounter = 0;

    private static final Logger logger = Logger.getLogger ( "org.jinterop" );

    public Ntlm1 ( final int flags, final byte[] sessionKey, final boolean isServer )
    {

        this.protectionLevel = ( flags & NTLMSSP_NEGOTIATE_SEAL ) != 0 ? PROTECTION_LEVEL_PRIVACY : PROTECTION_LEVEL_INTEGRITY;

        this.isServer = isServer;
        this.keyFactory = new NTLMKeyFactory ();
        this.clientSigningKey = this.keyFactory.generateClientSigningKeyUsingNegotiatedSecondarySessionKey ( sessionKey );
        final byte[] clientSealingKey = this.keyFactory.generateClientSealingKeyUsingNegotiatedSecondarySessionKey ( sessionKey );

        this.serverSigningKey = this.keyFactory.generateServerSigningKeyUsingNegotiatedSecondarySessionKey ( sessionKey );
        final byte[] serverSealingKey = this.keyFactory.generateServerSealingKeyUsingNegotiatedSecondarySessionKey ( sessionKey );

        //Used by the server to decrypt client messages
        this.clientCipher = this.keyFactory.getRC4 ( clientSealingKey );

        //Used by the client to decrypt server messages
        this.serverCipher = this.keyFactory.getRC4 ( serverSealingKey );

        if ( logger.isLoggable ( Level.FINEST ) )
        {
            logger.finest ( "Client Signing Key derieved from the session key: [" + dumpString ( this.clientSigningKey ) + "]" );
            logger.finest ( "Client Sealing Key derieved from the session key: [" + dumpString ( clientSealingKey ) + "]" );
            logger.finest ( "Server Signing Key derieved from the session key: [" + dumpString ( this.serverSigningKey ) + "]" );
            logger.finest ( "Server Sealing Key derieved from the session key: [" + dumpString ( serverSealingKey ) + "]" );
        }
    }

    private static String dumpString ( final byte[] data )
    {
        final StringBuilder sb = new StringBuilder ( data.length * 3 );

        for ( int i = 0; i < data.length; i++ )
        {
            if ( i % 20 == 0 && i != 0 )
            {
                sb.append ( "\n" );
            }
            sb.append ( String.format ( "%02x ", data[i] ) );
        }

        return sb.toString ();
    }

    @Override
    public int getVerifierLength ()
    {
        return NTLM1_VERIFIER_LENGTH;
    }

    @Override
    public int getAuthenticationService ()
    {
        return NtlmAuthentication.AUTHENTICATION_SERVICE_NTLM;
    }

    @Override
    public int getProtectionLevel ()
    {
        return this.protectionLevel;
    }

    @Override
    public void processIncoming ( final NetworkDataRepresentation ndr, final int index, final int length, final int verifierIndex, final boolean isFragmented ) throws IOException
    {
        try
        {
            final NdrBuffer buffer = ndr.getBuffer ();

            final byte[] signingKey;
            final StreamCipher cipher;

            //reverse of what it is
            if ( !this.isServer )
            {
                signingKey = this.serverSigningKey;
                cipher = this.serverCipher;
            }
            else
            {
                signingKey = this.clientSigningKey;
                cipher = this.clientCipher;
            }

            byte[] data = new byte[length];
            System.arraycopy ( ndr.getBuffer ().getBuffer (), index, data, 0, data.length );

            if ( getProtectionLevel () == PROTECTION_LEVEL_PRIVACY )
            {
                data = this.keyFactory.applyRC4 ( cipher, data );
                System.arraycopy ( data, 0, ndr.getBuffer ().buf, index, data.length );
            }

            if ( logger.isLoggable ( Level.FINEST ) )
            {
                logger.finest ( "\n AFTER Decryption" );
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
                jcifs.util.Hexdump.hexdump ( new PrintStream ( byteArrayOutputStream ), data, 0, data.length );
                logger.finest ( "\n" + byteArrayOutputStream.toString () );
                logger.finest ( "\nLength is: " + data.length );
            }

            final byte[] verifier = this.keyFactory.signingPt1 ( this.responseCounter, signingKey, buffer.getBuffer (), verifierIndex );
            this.keyFactory.signingPt2 ( verifier, cipher );

            buffer.setIndex ( verifierIndex );
            //now read the next 16 bytes and pass compare them
            final byte[] signing = new byte[16];
            ndr.readOctetArray ( signing, 0, signing.length );

            //this should result in an access denied fault
            if ( !this.keyFactory.compareSignature ( verifier, signing ) )
            {
                throw new IntegrityException ( "Message out of sequence. Perhaps the user being used to run this application is different from the one under which the COM server is running !." );
            }

            //only clients increment, servers just respond to the clients seq id.
            //            if (!isServer || isFragmented)
            //            {
            //            	responseCounter++;
            //            }

            this.responseCounter++;

        }
        catch ( final IOException ex )
        {
            logger.log ( Level.SEVERE, "", ex );
            throw ex;
        }
        catch ( final Exception ex )
        {
            logger.log ( Level.SEVERE, "", ex );
            throw new IntegrityException ( "General error: " + ex.getMessage () );
        }
    }

    @Override
    public void processOutgoing ( final NetworkDataRepresentation ndr, final int index, final int length, final int verifierIndex, final boolean isFragmented ) throws IOException
    {
        try
        {
            final NdrBuffer buffer = ndr.getBuffer ();

            final byte[] signingKey;
            final StreamCipher cipher;
            if ( this.isServer )
            {
                signingKey = this.serverSigningKey;
                cipher = this.serverCipher;
            }
            else
            {
                signingKey = this.clientSigningKey;
                cipher = this.clientCipher;
            }

            final byte[] verifier = this.keyFactory.signingPt1 ( this.requestCounter, signingKey, buffer.getBuffer (), verifierIndex );
            final byte[] data = new byte[length];
            System.arraycopy ( ndr.getBuffer ().getBuffer (), index, data, 0, data.length );
            if ( logger.isLoggable ( Level.FINEST ) )
            {
                logger.finest ( "\n BEFORE Encryption" );
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
                jcifs.util.Hexdump.hexdump ( new PrintStream ( byteArrayOutputStream ), data, 0, data.length );
                logger.finest ( "\n" + byteArrayOutputStream.toString () );
                logger.finest ( "\n Length is: " + data.length );
            }

            if ( getProtectionLevel () == PROTECTION_LEVEL_PRIVACY )
            {
                final byte[] data2 = this.keyFactory.applyRC4 ( cipher, data );
                System.arraycopy ( data2, 0, ndr.getBuffer ().buf, index, data2.length );
            }
            this.keyFactory.signingPt2 ( verifier, cipher );
            buffer.setIndex ( verifierIndex );
            buffer.writeOctetArray ( verifier, 0, verifier.length );

            //            if (isServer && !isFragmented)
            //            {
            //            	responseCounter++;
            //            }

            this.requestCounter++;

        }
        catch ( final Exception ex )
        {
            throw new IntegrityException ( "General error: " + ex.getMessage () );
        }
    }

}
