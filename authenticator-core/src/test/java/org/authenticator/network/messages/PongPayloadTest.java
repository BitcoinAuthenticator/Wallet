package org.authenticator.network.messages;

import static org.junit.Assert.*;

import org.authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;
import org.authenticator.network.messages.PongPayload;
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
