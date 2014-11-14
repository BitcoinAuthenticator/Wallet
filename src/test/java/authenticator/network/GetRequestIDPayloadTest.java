package authenticator.network;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import authenticator.GCM.dispacher.MessageBuilder;
import authenticator.network.GetRequestIDPayload;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;

public class GetRequestIDPayloadTest {

	@Test
	public void validTest(){
		JSONObject jo = new JSONObject();
		try {
			jo.put("requestID", "the req id");
			jo.put("pairingID", "the pairing id");
			
			byte[] payload = jo.toString().getBytes();
			GetRequestIDPayload reqPayload = new GetRequestIDPayload(payload);
			assertTrue("the req id".equals(reqPayload.getRequestID()));
			assertTrue("the pairing id".equals(reqPayload.getPairingID()));
			
		} catch (JSONException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void notValidTest(){
		JSONObject jo = new JSONObject();
		try {
			jo.put("requestID", "the req id");
			jo.put("false", "the pairing id");
			
			byte[] payload = jo.toString().getBytes();
			GetRequestIDPayload reqPayload = new GetRequestIDPayload(payload);
			assertTrue("the req id".equals(reqPayload.getRequestID()));
			assertFalse("the pairing id".equals(reqPayload.getPairingID())); // doesnt get here because of parsing exception in GetRequestIDPayload
			
		} catch (JSONException e) {
			assertTrue(true); // should get here
		}
	}

}
