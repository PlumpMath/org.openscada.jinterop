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

import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

class NTLMKeyFactory
{

    Random random = new Random ();

    private static final byte[] clientSigningMagicConstant = new byte[] { 0x73, 0x65, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x74, 0x6f, 0x20, 0x63, 0x6c, 0x69, 0x65, 0x6e, 0x74, 0x2d, 0x74, 0x6f, 0x2d, 0x73, 0x65, 0x72, 0x76, 0x65, 0x72, 0x20, 0x73, 0x69, 0x67, 0x6e, 0x69, 0x6e, 0x67, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x6d, 0x61, 0x67, 0x69, 0x63, 0x20, 0x63, 0x6f, 0x6e, 0x73, 0x74, 0x61, 0x6e, 0x74, 0x00 };//"session key to client-to-server signing key magic constant";

    private static final byte[] serverSigningMagicConstant = new byte[] { 0x73, 0x65, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x74, 0x6f, 0x20, 0x73, 0x65, 0x72, 0x76, 0x65, 0x72, 0x2d, 0x74, 0x6f, 0x2d, 0x63, 0x6c, 0x69, 0x65, 0x6e, 0x74, 0x20, 0x73, 0x69, 0x67, 0x6e, 0x69, 0x6e, 0x67, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x6d, 0x61, 0x67, 0x69, 0x63, 0x20, 0x63, 0x6f, 0x6e, 0x73, 0x74, 0x61, 0x6e, 0x74, 0x00 };//"session key to server-to-client signing key magic constant";

    private static final byte[] clientSealingMagicConstant = new byte[] { 0x73, 0x65, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x74, 0x6f, 0x20, 0x63, 0x6c, 0x69, 0x65, 0x6e, 0x74, 0x2d, 0x74, 0x6f, 0x2d, 0x73, 0x65, 0x72, 0x76, 0x65, 0x72, 0x20, 0x73, 0x65, 0x61, 0x6c, 0x69, 0x6e, 0x67, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x6d, 0x61, 0x67, 0x69, 0x63, 0x20, 0x63, 0x6f, 0x6e, 0x73, 0x74, 0x61, 0x6e, 0x74, 0x00 };//"session key to client-to-server sealing key magic constant";

    private static final byte[] serverSealingMagicConstant = new byte[] { 0x73, 0x65, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x74, 0x6f, 0x20, 0x73, 0x65, 0x72, 0x76, 0x65, 0x72, 0x2d, 0x74, 0x6f, 0x2d, 0x63, 0x6c, 0x69, 0x65, 0x6e, 0x74, 0x20, 0x73, 0x65, 0x61, 0x6c, 0x69, 0x6e, 0x67, 0x20, 0x6b, 0x65, 0x79, 0x20, 0x6d, 0x61, 0x67, 0x69, 0x63, 0x20, 0x63, 0x6f, 0x6e, 0x73, 0x74, 0x61, 0x6e, 0x74, 0x00 };//"session key to server-to-client sealing key magic constant";

    NTLMKeyFactory ()
    {

    }

    /**
     * NTLMv1 User Session Key. Cases where LMcompatibilitylevel is 0,1,2. For
     * 3,4,5 the logic is different
     * and based upon the reponses being sent back (either LMv2 or NTLMv2)
     * 
     * @param password
     * @return
     * @throws UnsupportedEncodingException
     * @throws DigestException
     */
    byte[] getNTLMUserSessionKey ( final String password ) throws UnsupportedEncodingException, DigestException
    {
        //look at NTLMPasswordAuthentication in jcifs. It supports only the NTLMUserSessionKey and the LMv2UserSessionKey...we need more :(
        final byte[] ntlmHash = Responses.ntlmHash ( password );
        return digestMD4 ( ntlmHash );
    }

    byte[] getNTLMv2UserSessionKey ( final String target, final String user, final String password, final byte[] challenge, final byte[] blob ) throws Exception
    {
        byte key[] = new byte[16];
        final byte[] ntlm2Hash = Responses.ntlmv2Hash ( target, user, password );
        final byte[] data = new byte[challenge.length + blob.length];
        System.arraycopy ( challenge, 0, data, 0, challenge.length );
        System.arraycopy ( blob, 0, data, challenge.length, blob.length );
        final byte[] mac = Responses.hmacMD5 ( data, ntlm2Hash );
        key = Responses.hmacMD5 ( mac, ntlm2Hash );
        return key;
    }

    /**
     * Password of the user
     * 
     * @param password
     * @param servernonce
     *            challenge + nonce from NTLM2 Session Response
     * @return
     * @throws DigestException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    byte[] getNTLM2SessionResponseUserSessionKey ( final String password, final byte[] servernonce ) throws NoSuchAlgorithmException, UnsupportedEncodingException, DigestException
    {
        return Responses.hmacMD5 ( servernonce, getNTLMUserSessionKey ( password ) );
    }

    /**
     * Randomly generated 16 bytes
     * 
     * @return
     */
    byte[] getSecondarySessionKey ()
    {
        final byte[] key = new byte[16];
        this.random.nextBytes ( key );
        return key;
    }

    StreamCipher getRC4 ( final byte[] key )
    {
        final RC4Engine rc4 = new RC4Engine ();
        rc4.init ( true, new KeyParameter ( key ) );
        return rc4;
    }

    /*
    IRandom getARCFOUR ( final byte[] key )
    {
        final HashMap attrib = new HashMap ();
        final IRandom keystream = new ARCFour ();
        attrib.put ( ARCFour.ARCFOUR_KEY_MATERIAL, key );
        keystream.init ( attrib );
        return keystream;
    }
    */

    byte[] applyRC4 ( final StreamCipher streamCipher, final byte[] data )
    {
        final byte[] retData = new byte[data.length];

        streamCipher.processBytes ( data, 0, data.length, retData, 0 );

        return retData;
    }

    /*
    byte[] applyARCFOUR ( final IRandom keystream, final byte[] data ) throws IllegalStateException, LimitReachedException
    {
        final byte[] retData = new byte[data.length];

        for ( int i = 0; i < data.length; i++ )
        {
            retData[i] = (byte) ( data[i] ^ keystream.nextByte () );
        }

        return retData;
    }*/

    /*
    byte[] decryptSecondarySessionKey ( final byte[] encryptedData, final byte[] key ) throws IllegalStateException, LimitReachedException
    {
        return applyARCFOUR ( getARCFOUR ( key ), encryptedData );
    }

    byte[] encryptSecondarySessionKey ( final byte[] plainData, final byte[] key ) throws IllegalStateException, LimitReachedException
    {
        return applyARCFOUR ( getARCFOUR ( key ), plainData );
    }
    */

    byte[] decryptSecondarySessionKey ( final byte[] encryptedData, final byte[] key ) throws IllegalStateException
    {
        return applyRC4 ( getRC4 ( key ), encryptedData );
    }

    byte[] encryptSecondarySessionKey ( final byte[] plainData, final byte[] key ) throws IllegalStateException
    {
        return applyRC4 ( getRC4 ( key ), plainData );
    }

    byte[] generateClientSigningKeyUsingNegotiatedSecondarySessionKey ( final byte[] secondarySessionKey )
    {
        //TODO this can be moved out of here...
        final byte[] dataforhash = new byte[secondarySessionKey.length + clientSigningMagicConstant.length];
        System.arraycopy ( secondarySessionKey, 0, dataforhash, 0, secondarySessionKey.length );
        System.arraycopy ( clientSigningMagicConstant, 0, dataforhash, secondarySessionKey.length, clientSigningMagicConstant.length );
        return digestMD5 ( dataforhash );
    }

    byte[] generateClientSealingKeyUsingNegotiatedSecondarySessionKey ( final byte[] secondarySessionKey )
    {
        //TODO this can be moved out of here...
        final byte[] dataforhash = new byte[secondarySessionKey.length + clientSealingMagicConstant.length];
        System.arraycopy ( secondarySessionKey, 0, dataforhash, 0, secondarySessionKey.length );
        System.arraycopy ( clientSealingMagicConstant, 0, dataforhash, secondarySessionKey.length, clientSealingMagicConstant.length );
        return digestMD5 ( dataforhash );
    }

    byte[] generateServerSigningKeyUsingNegotiatedSecondarySessionKey ( final byte[] secondarySessionKey )
    {
        //TODO this can be moved out of here...
        final byte[] dataforhash = new byte[secondarySessionKey.length + serverSigningMagicConstant.length];
        System.arraycopy ( secondarySessionKey, 0, dataforhash, 0, secondarySessionKey.length );
        System.arraycopy ( serverSigningMagicConstant, 0, dataforhash, secondarySessionKey.length, serverSigningMagicConstant.length );
        final byte[] digest = digestMD5 ( dataforhash );
        return digest;
    }

    public static byte[] digestMD5 ( final byte[] dataforhash )
    {
        final MD5Digest md5 = new MD5Digest ();
        md5.update ( dataforhash, 0, dataforhash.length );
        final byte[] digest = new byte[md5.getDigestSize ()];
        md5.doFinal ( digest, 0 );
        return digest;
    }

    public static byte[] digestMD4 ( final byte[] dataforhash )
    {
        final MD4Digest md4 = new MD4Digest ();
        md4.update ( dataforhash, 0, dataforhash.length );
        final byte[] digest = new byte[md4.getDigestSize ()];
        md4.doFinal ( digest, 0 );
        return digest;
    }

    byte[] generateServerSealingKeyUsingNegotiatedSecondarySessionKey ( final byte[] secondarySessionKey )
    {
        //TODO this can be moved out of here...
        final byte[] dataforhash = new byte[secondarySessionKey.length + serverSealingMagicConstant.length];
        System.arraycopy ( secondarySessionKey, 0, dataforhash, 0, secondarySessionKey.length );
        System.arraycopy ( serverSealingMagicConstant, 0, dataforhash, secondarySessionKey.length, serverSealingMagicConstant.length );
        return digestMD5 ( dataforhash );
    }

    //TODO merge the signing routine for both client and server all that they differ by are keys...as expected
    byte[] signingPt1 ( final int sequenceNumber, final byte[] signingKey, final byte[] data, final int lengthOfBuffer ) throws NoSuchAlgorithmException, IllegalStateException
    {
        final byte[] seqNumPlusData = new byte[4 + lengthOfBuffer];

        seqNumPlusData[0] = (byte) ( sequenceNumber & 0xFF );
        seqNumPlusData[1] = (byte) ( sequenceNumber >> 8 & 0xFF );
        seqNumPlusData[2] = (byte) ( sequenceNumber >> 16 & 0xFF );
        seqNumPlusData[3] = (byte) ( sequenceNumber >> 24 & 0xFF );

        System.arraycopy ( data, 0, seqNumPlusData, 4, lengthOfBuffer );

        final byte[] retval = new byte[16];
        retval[0] = 0x01; //Version number LE 1.

        final byte[] sign = Responses.hmacMD5 ( seqNumPlusData, signingKey );

        for ( int i = 0; i < 8; i++ )
        {
            retval[i + 4] = sign[i];
        }

        retval[12] = (byte) ( sequenceNumber & 0xFF );
        retval[13] = (byte) ( sequenceNumber >> 8 & 0xFF );
        retval[14] = (byte) ( sequenceNumber >> 16 & 0xFF );
        retval[15] = (byte) ( sequenceNumber >> 24 & 0xFF );

        return retval;
    }

    /*
    void signingPt2 ( final byte[] verifier, final IRandom rc4 ) throws IllegalStateException, LimitReachedException
    {
        for ( int i = 0; i < 8; i++ )
        {
            verifier[i + 4] = (byte) ( verifier[i + 4] ^ rc4.nextByte () );
        }
    }
    */

    void signingPt2 ( final byte[] verifier, final StreamCipher rc4 ) throws IllegalStateException
    {
        for ( int i = 0; i < 8; i++ )
        {
            verifier[i + 4] = rc4.returnByte ( verifier[i + 4] );
        }
    }

    boolean compareSignature ( final byte[] src, final byte[] target )
    {
        return Arrays.equals ( src, target );
    }

    //TODO merge the signing routine for both client and server all that they differ by are keys...as expected
    //	byte[] serverSigning(int sequenceNumber, byte[] serverSigningKey, byte[] data, IRandom rc4) throws NoSuchAlgorithmException, IllegalStateException, LimitReachedException
    //	{
    //		byte[] seqNumPlusData = new byte[4 + data.length];
    //
    //		seqNumPlusData[0] = (byte)(sequenceNumber & 0xFF);
    //		seqNumPlusData[1] = (byte)((sequenceNumber >> 8) & 0xFF);
    //		seqNumPlusData[2] = (byte)((sequenceNumber >> 16) & 0xFF);
    //		seqNumPlusData[3] = (byte)((sequenceNumber >> 24) & 0xFF);
    //
    //		System.arraycopy(data, 0, seqNumPlusData, 4, data.length);
    //
    //		byte[] retval = new byte[16];
    //		retval[0] = 0x01; //Version number LE 1.
    //
    //		byte[] sign = Responses.hmacMD5(seqNumPlusData, serverSigningKey);
    //
    //		for (int i = 0; i < 8; i++) {
    //			retval[i+4] = (byte) (sign[i] ^ rc4.nextByte());
    //		}
    //
    //		retval[12] = (byte)(sequenceNumber & 0xFF);
    //		retval[13] = (byte)((sequenceNumber >> 8) & 0xFF);
    //		retval[14] = (byte)((sequenceNumber >> 16) & 0xFF);
    //		retval[15] = (byte)((sequenceNumber >> 24) & 0xFF);
    //
    //		return retval;
    //	}

    //	byte[] clientSealing(int sequenceNumber, byte[] clientSealingKey, byte[] clientSigningKey, byte[] data,IRandom rc4) throws IllegalStateException, LimitReachedException, NoSuchAlgorithmException
    //	{
    //		//TODO..Imp... this implementation is not correct and should work for sequence 0, for the rest of the
    //		// sequences the arcfour state has to be maintained and not a new one used everytime...
    //		byte[] cipheredData = applyARCFOUR(rc4, data);
    //		byte[] signature = clientSigning(sequenceNumber, clientSigningKey, data, rc4);
    //		byte[] retval = new byte[cipheredData.length + signature.length];
    //		System.arraycopy(cipheredData, 0, retval, 0, cipheredData.length);
    //		System.arraycopy(signature, 0, retval, cipheredData.length,signature.length);
    //		return retval;
    //	}
    //
    //	byte[] serverSealing(int sequenceNumber, byte[] serverSealingKey, byte[] serverSigningKey, byte[] data, IRandom rc4) throws IllegalStateException, LimitReachedException, NoSuchAlgorithmException
    //	{
    //		//TODO..Imp... this implementation is not correct and should work for sequence 0, for the rest of the
    //		// sequences the arcfour state has to be maintained and not a new one used everytime...
    //		byte[] cipheredData = applyARCFOUR(rc4, data);
    //		byte[] signature = clientSigning(sequenceNumber, serverSigningKey, data, rc4);
    //		byte[] retval = new byte[cipheredData.length + signature.length];
    //		System.arraycopy(cipheredData, 0, retval, 0, cipheredData.length);
    //		System.arraycopy(signature, 0, retval, cipheredData.length,signature.length);
    //		return retval;
    //	}

    //	static void testFromDavenportPaper()
    //	{
    //		try
    //		{
    //
    //			NTLMKeyFactory keyFactory = new NTLMKeyFactory();
    //			byte[] challengePlusclientNonce = Util.toBytesFromString("677f1c557a5ee96c404d1b6f69152580");
    //			byte [] ntlm2UserSessionReponseKey = keyFactory.getNTLM2SessionResponseUserSessionKey("test1234", challengePlusclientNonce);
    //
    //			System.out.println(Util.dumpString(ntlm2UserSessionReponseKey));
    //
    //			byte[] secondaryEncryptedKey = Util.toBytesFromString("727a5240822ec7af4e9100c43e6fee7f");
    //
    //			byte[] decryptedSecondaryKey = keyFactory.decryptSecondarySessionKey(secondaryEncryptedKey, ntlm2UserSessionReponseKey);
    //			System.out.println(Util.dumpString(decryptedSecondaryKey));
    //
    //			//now lets try signature from server
    //			byte[] data = new byte[]{0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
    //			byte[] serverSigningKey = keyFactory.generateServerSigningKeyUsingNegotiatedSecondarySessionKey(decryptedSecondaryKey);
    //			System.out.println(Util.dumpString(serverSigningKey));
    //			byte[] serverSealingKey = keyFactory.generateServerSealingKeyUsingNegotiatedSecondarySessionKey(decryptedSecondaryKey);
    //			System.out.println(Util.dumpString(serverSealingKey));
    //			IRandom rc4 = keyFactory.getARCFOUR(serverSealingKey);
    //			System.out.println(Util.dumpString(keyFactory.serverSigning(0, serverSigningKey, data, rc4)));
    //			byte[] cipheredPack = keyFactory.serverSealing(1, serverSealingKey, serverSigningKey,data, rc4);
    //			System.out.println(Util.dumpString(cipheredPack));
    //
    //			IRandom rc4fordecipher = keyFactory.getARCFOUR(serverSealingKey);
    //			keyFactory.serverSigning(0, serverSigningKey, data, rc4fordecipher);//just like that for increasing rc4fordecipher state...will not be like this
    //			//in the actual implementation...
    //			byte[] cipheredData = new byte[8];
    //			System.arraycopy(cipheredPack, 0, cipheredData, 0, 8);
    //
    //			System.out.println(Util.dumpString(keyFactory.applyARCFOUR(rc4fordecipher, cipheredData)));
    //			int i = 0;
    //		}catch(Exception e)
    //		{
    //			e.printStackTrace();
    //		}
    //
    //	}
    //
    //	/**
    //	 * @param args
    //	 */
    //	static void main(String[] args) {
    //
    //		try
    //		{
    //
    //			NTLMKeyFactory keyFactory = new NTLMKeyFactory();
    //			byte[] challengePlusclientNonce = Util.toBytesFromString("38c2c82866a284b6a2d45d0f58feb085");
    //			byte [] ntlm2UserSessionReponseKey = keyFactory.getNTLM2SessionResponseUserSessionKey("enterprise", challengePlusclientNonce);
    //
    //			System.out.println(Util.dumpString(ntlm2UserSessionReponseKey));
    //
    //			byte[] secondaryEncryptedKey = Util.toBytesFromString("fa650f59feb62161fc08defeb9e5f5d2");
    //
    //			byte[] decryptedSecondaryKey = keyFactory.decryptSecondarySessionKey(secondaryEncryptedKey, ntlm2UserSessionReponseKey);
    //			System.out.println(Util.dumpString(decryptedSecondaryKey));
    //
    //			//now lets try signature from server
    //			byte[] data = new byte[]{0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
    //			byte[] clientSigningKey = keyFactory.generateClientSigningKeyUsingNegotiatedSecondarySessionKey(decryptedSecondaryKey);
    //			System.out.println(Util.dumpString(clientSigningKey));
    //			byte[] clientSealingKey = keyFactory.generateClientSealingKeyUsingNegotiatedSecondarySessionKey(decryptedSecondaryKey);
    //			System.out.println(Util.dumpString(clientSealingKey));
    ////			IRandom rc4 = keyFactory.getARCFOUR(serverSealingKey);
    //
    ////			byte[] cipheredPack = keyFactory.serverSealing(0, serverSealingKey, serverSigningKey,data, rc4);
    ////			System.out.println(Util.dumpString(cipheredPack));
    //
    //			IRandom rc4fordecipher = keyFactory.getARCFOUR(clientSealingKey);
    ////			keyFactory.serverSigning(0, serverSigningKey, data, rc4fordecipher);//just like that for increasing rc4fordecipher state...will not be like this
    //			//in the actual implementation...
    //			byte[] cipheredData = new byte[496];
    //			FileInputStream stream = new FileInputStream("c:/temp/encrypted");
    //			stream.read(cipheredData, 0, 496);
    ////			System.arraycopy(cipheredPack, 0, cipheredData, 0, 8);
    //			cipheredData = keyFactory.applyARCFOUR(rc4fordecipher, cipheredData);
    //        	Hexdump.hexdump(System.out, cipheredData, 0, cipheredData.length);
    //			int i = 0;
    //		}catch(Exception e)
    //		{
    //			e.printStackTrace();
    //		}
    //
    //	}

}
