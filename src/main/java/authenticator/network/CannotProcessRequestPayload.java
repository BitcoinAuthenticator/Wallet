package authenticator.network;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.crypto.CryptoUtils;
import authenticator.crypto.CryptoUtils.CouldNotEncryptPayload;

public class CannotProcessRequestPayload extends JSONObject{
	private byte[] payload;
	private byte[] encryptedPayload;
	
	public CannotProcessRequestPayload(String desc, SecretKey secretKey) throws CouldNotEncryptPayload {
		
		try {
			put("CANNOT_PROCESS_REQUEST", "");
			put("WHY",desc);
			payload = this.toString().getBytes();
			encryptedPayload = CryptoUtils.encryptPayloadWithChecksum(payload, secretKey);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public byte[] toEncryptedBytes() {
		return encryptedPayload;
	}
	
	public int getPayloadSize() {
		return encryptedPayload.length;
	}
	
	
}
