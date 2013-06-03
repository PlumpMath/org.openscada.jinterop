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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Random;

import jcifs.Config;
import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.NtlmMessage;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.util.Encdec;
import rpc.Security;

public class NtlmAuthentication
{

    public static final int AUTHENTICATION_SERVICE_NTLM = 10;

    private static final boolean UNICODE_SUPPORTED = Config.getBoolean ( "jcifs.smb.client.useUnicode", true );

    private static final int BASIC_FLAGS = NtlmFlags.NTLMSSP_REQUEST_TARGET | NtlmFlags.NTLMSSP_NEGOTIATE_NTLM | NtlmFlags.NTLMSSP_NEGOTIATE_OEM | NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN | ( UNICODE_SUPPORTED ? NtlmFlags.NTLMSSP_NEGOTIATE_UNICODE : 0 );

    private Security security;

    protected Properties properties;

    private final NtlmPasswordAuthentication credentials;

    private AuthenticationSource authenticationSource;

    private boolean lanManagerKey;

    private boolean seal;

    private boolean sign;

    private boolean keyExchange;

    //we always go for 128
    private int keyLength = 128;

    private boolean useNtlm2sessionsecurity = false;

    private boolean useNtlmV2 = false;

    private static final Random RANDOM = new Random ();

    public NtlmAuthentication ( final Properties properties )
    {
        this.properties = properties;
        String domain = null;
        String user = null;
        String password = null;
        if ( properties != null )
        {
            this.lanManagerKey = Boolean.valueOf ( properties.getProperty ( "rpc.ntlm.lanManagerKey" ) ).booleanValue ();
            this.seal = Boolean.valueOf ( properties.getProperty ( "rpc.ntlm.seal" ) ).booleanValue ();
            this.sign = this.seal ? true : Boolean.valueOf ( properties.getProperty ( "rpc.ntlm.sign" ) ).booleanValue ();
            this.keyExchange = Boolean.valueOf ( properties.getProperty ( "rpc.ntlm.keyExchange" ) ).booleanValue ();
            final String keyLength = properties.getProperty ( "rpc.ntlm.keyLength" );
            if ( keyLength != null )
            {
                try
                {
                    this.keyLength = Integer.parseInt ( keyLength );
                }
                catch ( final NumberFormatException ex )
                {
                    throw new IllegalArgumentException ( "Invalid key length: " + keyLength );
                }
            }

            this.useNtlm2sessionsecurity = Boolean.valueOf ( properties.getProperty ( "rpc.ntlm.ntlm2" ) ).booleanValue ();
            this.useNtlmV2 = Boolean.valueOf ( properties.getProperty ( "rpc.ntlm.ntlmv2" ) ).booleanValue ();
            domain = properties.getProperty ( "rpc.ntlm.domain" );
            user = properties.getProperty ( Security.USERNAME );
            password = properties.getProperty ( Security.PASSWORD );
        }

        this.credentials = new NtlmPasswordAuthentication ( domain, user, password );

    }

    public Security getSecurity () throws IOException
    {
        return this.security;
    }

    protected AuthenticationSource getAuthenticationSource ()
    {
        if ( this.authenticationSource != null )
        {
            return this.authenticationSource;
        }
        final String sourceClass = this.properties != null ? this.properties.getProperty ( "rpc.ntlm.authenticationSource" ) : null;
        if ( sourceClass == null )
        {
            return this.authenticationSource = AuthenticationSource.getDefaultInstance ();
        }
        try
        {
            return this.authenticationSource = (AuthenticationSource)Class.forName ( sourceClass ).newInstance ();
        }
        catch ( final Exception ex )
        {
            throw new IllegalArgumentException ( "Invalid authentication source: " + ex );
        }
    }

    private int getDefaultFlags ()
    {
        int flags = BASIC_FLAGS;
        if ( this.lanManagerKey )
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_LM_KEY;
        }
        if ( this.sign )
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_SIGN;
        }
        if ( this.seal )
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_SEAL;
        }
        if ( this.keyExchange )
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_KEY_EXCH;
        }
        if ( this.keyLength >= 56 )
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_56;
        }
        if ( this.keyLength >= 128 )
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_128;
        }
        //We always negotiate for NTLM2 session security
        //        if (useNtlm2sessionsecurity)
        {
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2;
        }

        return flags;
    }

    private int adjustFlags ( int flags )
    {
        if ( UNICODE_SUPPORTED && ( flags & NtlmFlags.NTLMSSP_NEGOTIATE_UNICODE ) != 0 )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_OEM;
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_UNICODE;
        }
        else
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_UNICODE;
            flags |= NtlmFlags.NTLMSSP_NEGOTIATE_OEM;
        }
        if ( !this.lanManagerKey )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_LM_KEY;
        }
        if ( ! ( this.sign || this.seal ) )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_SIGN;
        }
        if ( !this.seal )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_SEAL;
        }
        if ( !this.keyExchange )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_KEY_EXCH;
        }
        if ( this.keyLength < 128 )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_128;
        }
        if ( this.keyLength < 56 )
        {
            flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_56;
        }
        //        if (!useNtlm2sessionsecurity)
        //        {
        //        	flags &= ~NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2;
        //        }
        return flags;
    }

    public Type1Message createType1 () throws IOException
    {
        final int flags = getDefaultFlags ();
        return new Type1Message ( flags, this.credentials.getDomain (), Type1Message.getDefaultWorkstation () );
    }

    public Type2Message createType2 ( final Type1Message type1 ) throws IOException
    {
        int flags;
        if ( type1 == null )
        {
            flags = getDefaultFlags ();
        }
        else
        {
            flags = adjustFlags ( type1.getFlags () );
        }
        flags |= 0x00020000; //challenge accept response flag

        final Type2Message type2Message = new Type2Message ( flags, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, //generate our own, since SMB will throw exception here
        this.credentials.getDomain () );

        return type2Message;
    }

    public Type3Message createType3 ( final Type2Message type2 ) throws IOException
    {
        int flags = type2.getFlags ();
        if ( ( flags & NtlmFlags.NTLMSSP_NEGOTIATE_DATAGRAM_STYLE ) != 0 )
        {
            flags = adjustFlags ( flags );
            flags &= ~0x00020000;
        }

        Type3Message type3 = null;

        final byte[] clientNonce = new byte[8];
        byte[] blob = null;

        String target = null;//getTargetFromTargetInformation(type2.getTargetInformation());

        if ( target == null )
        {
            target = this.credentials.getDomain ().toUpperCase ();
            if ( target.equals ( "" ) )
            {
                target = getTargetFromTargetInformation ( type2.getTargetInformation () );
            }
        }

        if ( this.useNtlmV2 )
        {
            RANDOM.nextBytes ( clientNonce );
            try
            {
                final byte[] lmv2Response = Responses.getLMv2Response ( target, this.credentials.getUsername (), this.credentials.getPassword (), type2.getChallenge (), clientNonce );
                final byte[][] retval = Responses.getNTLMv2Response ( target, this.credentials.getUsername (), this.credentials.getPassword (), type2.getTargetInformation (), type2.getChallenge (), clientNonce );
                final byte[] ntlmv2Response = retval[0];
                blob = retval[1];
                type3 = new Type3Message ( flags, lmv2Response, ntlmv2Response, target, this.credentials.getUsername (), Type3Message.getDefaultWorkstation () );
            }
            catch ( final Exception e )
            {
                throw new RuntimeException ( "Exception occured while forming NTLMv2 Type3Response", e );
            }

        }
        else if ( ( flags & NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2 ) != 0 ) //NTLM2 Session security response
        {
            flags = adjustFlags ( flags );
            flags &= ~0x00020000;
            //flags =  0xe2888235;
            final byte[] challenge = type2.getChallenge ();
            //LMReponse is 24 bytes. 8 byte random client nonce and the rest is null padded.
            final byte[] lmResponse = new byte[24];

            RANDOM.nextBytes ( clientNonce );
            System.arraycopy ( clientNonce, 0, lmResponse, 0, clientNonce.length );
            byte[] ntResponse;
            try
            {
                ntResponse = Responses.getNTLM2SessionResponse ( this.credentials.getPassword (), challenge, clientNonce );
            }
            catch ( final Exception e )
            {
                throw new RuntimeException ( "Exception occured while forming Session Security Type3Response", e );
            }

            type3 = new Type3Message ( flags, lmResponse, ntResponse, target, this.credentials.getUsername (), Type3Message.getDefaultWorkstation () );
        }
        else
        //Plain NTLMv1 response
        {
            final byte[] challenge = type2.getChallenge ();
            final byte[] lmResponse = NtlmPasswordAuthentication.getPreNTLMResponse ( this.credentials.getPassword (), challenge );
            final byte[] ntResponse = NtlmPasswordAuthentication.getNTLMResponse ( this.credentials.getPassword (), challenge );
            type3 = new Type3Message ( flags, lmResponse, ntResponse, target, this.credentials.getUsername (), Type3Message.getDefaultWorkstation () );
            if ( ( flags & NtlmFlags.NTLMSSP_NEGOTIATE_KEY_EXCH ) != 0 )
            {
                throw new RuntimeException ( "Key Exchange not supported by Library !" );
            }
        }
        //we have to now form lmv2 and ntlmv2 response with regards to the session security
        //the type3message also has to be altered
        if ( this.useNtlm2sessionsecurity && ( flags & NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2 ) != 0 )
        {
            final NTLMKeyFactory ntlmKeyFactory = new NTLMKeyFactory ();
            byte[] userSessionKey;
            if ( this.useNtlmV2 )
            {
                try
                {
                    userSessionKey = ntlmKeyFactory.getNTLMv2UserSessionKey ( target, this.credentials.getUsername (), this.credentials.getPassword (), type2.getChallenge (), blob );
                }
                catch ( final Exception e )
                {
                    throw new RuntimeException ( "Exception occured while forming NTLMv2 with NTLM2 Session Security for Type3Response", e );
                }
            }
            else
            {
                //now create the key for the session
                //this key will be used to RC4 a 16 byte random key and set to the type3 message
                final byte[] servernonce = new byte[16];
                System.arraycopy ( type2.getChallenge (), 0, servernonce, 0, type2.getChallenge ().length );
                System.arraycopy ( clientNonce, 0, servernonce, 8, clientNonce.length );
                try
                {
                    userSessionKey = ntlmKeyFactory.getNTLM2SessionResponseUserSessionKey ( this.credentials.getPassword (), servernonce );
                }
                catch ( final Exception e )
                {
                    throw new RuntimeException ( "Exception occured while forming Session Security for Type3Response", e );
                }

            }

            try
            {
                //now RC4 encrypt a random 16 byte key
                final byte[] secondayMasterKey = ntlmKeyFactory.getSecondarySessionKey ();
                type3.setSessionKey ( ntlmKeyFactory.encryptSecondarySessionKey ( secondayMasterKey, userSessionKey ) );
                this.security = new Ntlm1 ( flags, secondayMasterKey, false );
            }
            catch ( final Exception e )
            {
                throw new RuntimeException ( "Exception occured while forming Session Security for Type3Response", e );
            }
        }

        return type3;
    }

    private String getTargetFromTargetInformation ( final byte[] targetInformation )
    {
        String target = null;

        int i = 0;
        while ( i < targetInformation.length )
        {
            switch ( Encdec.dec_uint16le ( targetInformation, i ) )
            {
                case 1: //Server name
                    i++;
                    i++; //advance two bytes
                    int length = Encdec.dec_uint16le ( targetInformation, i );
                    i++;
                    i++;//advance two bytes
                    final byte[] domainb = new byte[length];
                    System.arraycopy ( targetInformation, i, domainb, 0, length );
                    try
                    {
                        target = new String ( domainb, "UTF-16LE" );
                    }
                    catch ( final UnsupportedEncodingException e )
                    {
                        return null;
                    }
                    i = i + length;
                    i = targetInformation.length;
                    break;
                default: //skip bytes   
                    i++;
                    i++; //advance two bytes
                    length = Encdec.dec_uint16le ( targetInformation, i );
                    i++;
                    i++;//advance two bytes
                    i = i + length;
            }
        }

        return target;
    }

    void createSecurityWhenServer ( final NtlmMessage type3 )
    {
        final Type3Message type3Message = (Type3Message)type3;
        //two things here...check for anonymous , in that case the user response key is new byte[16].
        //in case anonymous has not been sent then create the key using credentials.
        final int flags = type3Message.getFlags ();
        final NTLMKeyFactory ntlmKeyFactory = new NTLMKeyFactory ();
        byte[] secondayMasterKey;
        byte[] sessionResponseUserSessionKey = null;
        if ( type3Message.getFlag ( 0x00000800 ) )//anonymous flag
        {
            //if it is anonymous the user session key is new byte[16];
            sessionResponseUserSessionKey = new byte[16];
        }
        else if ( this.useNtlmV2 )
        {
            //TODO this needs to be checked here since the key logic will be totally different 
            //and we have to get the key out of Type3 message response (blob of the NTLMv2 response.)
            final int h = 0;
        }
        else
        {
            //now create the key for the session
            //this key will be used to RC4 a 16 byte random key and set to the type3 message
            final byte[] servernonce = new byte[16];
            final byte[] challenge = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }; //challenge is fixed
            System.arraycopy ( challenge, 0, servernonce, 0, challenge.length );
            System.arraycopy ( type3Message.getLMResponse (), 0, servernonce, 8, 8 );//first 8 bytes only , the rest are all 0x00 and not required.
            try
            {
                sessionResponseUserSessionKey = ntlmKeyFactory.getNTLM2SessionResponseUserSessionKey ( this.credentials.getPassword (), servernonce );
            }
            catch ( final Exception e )
            {
                throw new RuntimeException ( "Exception occured while forming Session Security from Type3 AUTH", e );
            }
        }

        try
        {
            //now RC4 decrypt the session key
            secondayMasterKey = ntlmKeyFactory.decryptSecondarySessionKey ( type3Message.getSessionKey (), sessionResponseUserSessionKey );
            this.security = new Ntlm1 ( flags, secondayMasterKey, true );
        }
        catch ( final Exception e )
        {
            throw new RuntimeException ( "Exception occured while forming Session Security Type3Response", e );
        }
    }

}
