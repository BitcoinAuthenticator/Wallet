package org.authenticator.network;

import static org.junit.Assert.*;

import org.authenticator.GCM.dispacher.MessageBuilder;
import org.authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;
import org.authenticator.network.GetRequestIDPayload;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class PongPayloadTest {

	@Test
	public void validTest(){
		try {
			PongPayload payload = new PongPayload();
			byte[] b = payload.getBytes();
			String s = new String(b);
			assertTrue(s.equals("{\"WELCOME_BACK_AUTHENTICATOR\":\"\"}"));
			assertTrue(payload.getPayloadSize() == 33);
		} catch (CouldNotEncryptPayload e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
