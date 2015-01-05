package org.authenticator.network;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import org.authenticator.Utils.CryptoUtils;
import org.authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;

public class CannotProcessRequestPayload extends JSONObject{
	private byte[] payload;
	private byte[] encryptedPayload;
	
	public CannotProcessRequestPayload(String desc, SecretKey secretKey) throws CouldNotEncryptPayload, JSONException {
		put("CANNOT_PROCESS_REQUEST", "");
		put("WHY",desc);
		payload = this.toString().getBytes();
		encryptedPayload = CryptoUtils.encryptPayloadWithChecksum(payload, secretKey);
	}
	
	public byte[] toEncryptedBytes() {
		return encryptedPayload;
	}
	
	public int getPayloadSize() {
		return encryptedPayload.length;
	}
	
	
}
