package authenticator.network;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.Utils.CryptoUtils;
import authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;

/**
 * When receiving a request ID payload form the authenticator, this payload will be sent to confirm connection
 * 
 * @author Alon Muroch
 *
 */
public class GetRequestIDPayload extends JSONObject{
	private byte[] payload;
	private String requestID; 
	private String pairingID; 
	
	public GetRequestIDPayload(byte[] payloadBytes) throws JSONException{
		payload = payloadBytes;
		
		JSONObject jo = new JSONObject(new String(payload));
		requestID = jo.getString("requestID");
		pairingID = jo.getString("pairingID");	
	}
	
	public String getRequestID() {
		return requestID;
	}
	
	public String getPairingID() {
		return pairingID;
	}
}
