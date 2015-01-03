package org.authenticator.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.util.encoders.Hex;

public class CryptoUtils {
	
	/**
	 * Encrypt payload with an AES key (with HmacSHA256)
	 * 
	 * @param rawPayload
	 * @param secretkey
	 * @return
	 * @throws CouldNotEncryptPayload
	 */
	static public byte[] encryptPayloadWithChecksum(byte[] rawPayload, SecretKey secretkey) throws CouldNotEncryptPayload {
		byte payload[] = payloadWithChecksum(rawPayload, secretkey);
		return encrypt(secretkey, payload);
	}

	static public byte[] encrypt(SecretKey secretkey, byte[] payload) throws CouldNotEncryptPayload {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretkey);
			byte[] cipherBytes = cipher.doFinal(payload);
			return cipherBytes;
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new CouldNotEncryptPayload("Couldn't encrypt payload");
		}
	}
	
	/**
	 * HmacSHA256 checksum is performed on the raw payload, return a concatenated array of both.
	 * 
	 * @param rawPayload
	 * @param secretkey
	 * @return
	 */
	static public byte[] payloadWithChecksum(byte[] rawPayload, SecretKey secretkey) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(secretkey);
			byte[] macbytes = mac.doFinal(rawPayload);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			
			outputStream.write(rawPayload);
			outputStream.write(macbytes);
			
			return outputStream.toByteArray( );
		}
		catch(Exception e) {
			return null;
		}
	}
	
	static public byte[] decryptPayloadWithChecksum(byte[] encryptedPayload, SecretKey secretkey) throws CannotDecryptMessageException {
		try {
		    String message = Hex.toHexString(decryptPayload(encryptedPayload, secretkey));
		    String sig = message.substring(0,message.length()-64);
		    String HMAC = message.substring(message.length()-64,message.length());
		    byte[] testsig = Hex.decode(sig);
		    byte[] hash = Hex.decode(HMAC);
		    //Calculate the HMAC of the message and verify it is valid
		    Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(secretkey);
			byte[] macbytes = mac.doFinal(testsig);
			if (Arrays.equals(macbytes, hash)){
				return testsig;
			}
			else {
				throw new ChecksumIncorrectException("");
			}
		}
		catch(Exception e) {
			throw new CannotDecryptMessageException(e.toString());
		}
	}

	static public byte[] decryptPayload(byte[] encryptedPayload, SecretKey secretkey) throws CannotDecryptMessageException {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretkey);
			return cipher.doFinal(encryptedPayload);
		}
		catch(Exception e) {
			throw new CannotDecryptMessageException(e.toString());
		}
	}
	
	static public SecretKey generateNewSecretAESKey() {
		try {
			//Generate 256 bit key.
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(256);
			
			// Generate the secret key specs.
			SecretKey sharedsecret = kgen.generateKey();
			return sharedsecret;
		}
		catch(NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	static public SecretKey secretKeyFromHexString(String hexKey) {
		byte[] keyBytes = Hex.decode(hexKey);
		return new SecretKeySpec(keyBytes, "AES");
	}

	static public byte[] digestSHA256(String input) {
		MessageDigest md = null;
		try {md = MessageDigest.getInstance("SHA-256");}
		catch(NoSuchAlgorithmException e) {e.printStackTrace();}
		return md.digest(input.getBytes());
	}

	/**
	 *
	 * @param AESHexKey
	 * @param salt
	 * @param accountIndex
	 * @param seed
	 * @return
	 * @throws CouldNotEncryptPayload
	 */
	static public byte[] authenticatorAESEncryption(String AESHexKey, String salt, String accountIndex, String seed) throws CouldNotEncryptPayload {
		String keyHex = null;
		{
			String concatenated = seed + salt + accountIndex;
			byte[] dd = digestSHA256(concatenated);
			keyHex = Hex.toHexString(digestSHA256(concatenated));
 		}

		SecretKey key = secretKeyFromHexString(keyHex);

		return encrypt(key, Hex.decode(AESHexKey));
	}

	/**
	 *
	 * @param encryptedAESHexKey
	 * @param salt
	 * @param accountIndex
	 * @param seed
	 * @return
	 * @throws CannotDecryptMessageException
	 */
	static public byte[] authenticatorAESDecryption(String encryptedAESHexKey, String salt, String accountIndex, String seed) throws CannotDecryptMessageException {
		String keyHex = null;
		{
			String concatenated = seed + salt + accountIndex;
			keyHex = Hex.toHexString(digestSHA256(concatenated));
		}

		SecretKey key = secretKeyFromHexString(keyHex);

		return decryptPayload(Hex.decode(encryptedAESHexKey), key);
	}
	
	static public class ChecksumIncorrectException extends Exception {
		public ChecksumIncorrectException(String str) {
			super (str);
		}
	}
	
	static public class CannotDecryptMessageException extends Exception {
		public CannotDecryptMessageException(String str) {
			super (str);
		}
	}
	
	static public class CouldNotEncryptPayload extends Exception {
		public CouldNotEncryptPayload(String str) {
			super (str);
		}
	}
}
