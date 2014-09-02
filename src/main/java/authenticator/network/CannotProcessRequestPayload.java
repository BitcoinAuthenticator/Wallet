package authenticator.network;

import org.json.JSONException;
import org.json.JSONObject;

public class CannotProcessRequestPayload extends JSONObject{
	private byte[] payload;
	
	public CannotProcessRequestPayload() {
		
		try {
			put("CANNOT_PROCESS_REQUEST", "");
			payload = this.toString().getBytes();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public byte[] toBytes() {
		return payload;
	}
	
	public int getPayloadSize() {
		return payload.length;
	}
	
}
