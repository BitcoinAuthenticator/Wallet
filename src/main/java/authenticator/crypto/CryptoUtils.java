package authenticator.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import authenticator.Utils.EncodingUtils;

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
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(secretkey);
			byte[] macbytes = mac.doFinal(rawPayload);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			
				outputStream.write(rawPayload);
				outputStream.write(macbytes);
			
			byte payload[] = outputStream.toByteArray( );
			
			//Encrypt the payload
			Cipher cipher = null;
			try {cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");} 
			catch (NoSuchAlgorithmException e) {e.printStackTrace();} 
			catch (NoSuchPaddingException e) {e.printStackTrace();}
			try {cipher.init(Cipher.ENCRYPT_MODE, secretkey);} 
			catch (InvalidKeyException e) {e.printStackTrace();}
			byte[] cipherBytes = null;
			try {cipherBytes = cipher.doFinal(payload);} 
			catch (IllegalBlockSizeException e) {e.printStackTrace();} 
			catch (BadPaddingException e) {e.printStackTrace();}
			return cipherBytes;
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			throw new CouldNotEncryptPayload("Couldn't encrypt payload");
		}
	}
	
	
	static public class CouldNotEncryptPayload extends Exception {
		public CouldNotEncryptPayload(String str) {
			super (str);
		}
	}
}
