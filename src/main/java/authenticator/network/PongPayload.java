package authenticator.network;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.crypto.CryptoUtils;
import authenticator.crypto.CryptoUtils.CouldNotEncryptPayload;

/**
 * When receiving a request ID payload form the authenticator, this payload will be sent to confirm connection
 * 
 * @author Alon Muroch
 *
 */
public class PongPayload extends JSONObject{
	private byte[] payload;
	
	public PongPayload() throws CouldNotEncryptPayload{
		
		try {
			put("WELCOME_BACK_AUTHENTICATOR", "");
			payload = this.toString().getBytes();
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public byte[] getBytes()  {
		return payload;
	}
	
	public int getPayloadSize() {			
		return payload.length;
	}
	
}
