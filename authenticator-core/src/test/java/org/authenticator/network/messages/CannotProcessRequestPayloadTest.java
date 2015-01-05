package org.authenticator.network.messages;

import org.authenticator.Utils.CryptoUtils;
import org.authenticator.network.messages.CannotProcessRequestPayload;
import org.json.JSONObject;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import javax.crypto.SecretKey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CannotProcessRequestPayloadTest {

	@Test
	public void validTest(){
		String AES = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		SecretKey key = CryptoUtils.secretKeyFromHexString(AES);

		JSONObject jo = new JSONObject();
		try {
			jo.put("CANNOT_PROCESS_REQUEST", "");
			jo.put("WHY", "Some kind of an  error");
			byte[] payload = jo.toString().getBytes();
			CannotProcessRequestPayload requestPayload = new CannotProcessRequestPayload("Some kind of an  error", key);

			String encryptedHex = Hex.toHexString(requestPayload.toEncryptedBytes());
			assertTrue(encryptedHex.equals("6fa13abdda1c0e1b00430a161d2ab205dc305e1c5bb21e9cea499ba6132d0c0b6e373ee7f397fdb5ef67af36e38b6e915fa3dc342dfd144ca97baa33d4304cc63cb6c39a7302c53ad5bae596da61fc1f4c78d3a8cce490276a1670235a1845e0"));
			assertTrue(requestPayload.has("CANNOT_PROCESS_REQUEST"));
			assertTrue(requestPayload.getString("WHY").equals("Some kind of an  error"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
