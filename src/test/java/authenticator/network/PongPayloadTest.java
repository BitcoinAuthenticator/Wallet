package authenticator.network;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import authenticator.GCM.dispacher.MessageBuilder;
import authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;
import authenticator.network.GetRequestIDPayload;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;

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
