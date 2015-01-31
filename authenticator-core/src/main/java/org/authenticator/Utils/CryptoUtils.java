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
	 * Will generate a AES key derived from the seed, salt and additional argumets.
	 * @param salt
	 * @param seed
	 * @param additionalArgs
	 * @return
	 */
	static public SecretKey getBaAESKey(byte[] salt, byte[] seed, String ... additionalArgs) {
		String keyHex = null;
		{
			String concatenated = Hex.toHexString(seed) +
					Hex.toHexString(salt);
			for(String arg: additionalArgs)
				concatenated += arg;
			keyHex = Hex.toHexString(digestSHA256(concatenated));
		}

		SecretKey key = secretKeyFromHexString(keyHex);
		return key;
	}

	/**
	 * This method encrypts a payload with an AES key derived from the seed, salt and additional argumets.<br>
	 * It is a general purpose encryption util if you want to tie the key with this specific wallet.
	 *
	 * @param payload - in hex
	 * @param salt
	 * @param additionalArgs - additional String arguments to add to the digest
	 * @param seed
	 * @return
	 * @throws CouldNotEncryptPayload
	 */
	static public byte[] encryptHexPayloadWithBaAESKey(byte[] payload, byte[] salt, byte[] seed, String ... additionalArgs) throws CouldNotEncryptPayload {
		SecretKey key = getBaAESKey(salt, seed, additionalArgs);
		return encrypt(key, Hex.decode(payload));
	}

	/**
	 *This method decrypts a payload with an AES key derived from the seed, salt and additional argumets.<br>
	 *
	 * @param payload - in hex
	 * @param salt
	 * @param additionalArgs - additional String arguments to add to the digest
	 * @param seed
	 * @return
	 * @throws CannotDecryptMessageException
	 */
	static public byte[] decryptHexPayloadWithBaAESKey(byte[] payload, byte[] salt, byte[] seed, String ... additionalArgs) throws CannotDecryptMessageException {
		SecretKey key = getBaAESKey(salt, seed, additionalArgs);
		return decryptPayload(Hex.decode(payload), key);
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
