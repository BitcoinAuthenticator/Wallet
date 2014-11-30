package org.authenticator.GCM;

import static org.junit.Assert.*;

import org.authenticator.GCM.dispacher.MessageBuilder;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class MessageBuilderTest {

	@Test
	public void signTXTest() {
		try {
			String[] args = new String[] { "wallet ID", 
					"ext IP",
					"local IP",
					"my msg",
					"my time test"};
			MessageBuilder mb = new MessageBuilder(ATGCMMessageType.SignTX, args);
			
			mb.getString("tmp"); // just make sure it exists
			assertTrue("wallet ID".equals(mb.getString("WalletID")));
			assertTrue(ATGCMMessageType.SignTX_VALUE == mb.getInt("RequestType"));
			JSONObject reqPayload = new JSONObject(mb.getString("ReqPayload"));
				assertTrue("ext IP".equals(reqPayload.getString("ExternalIP")));
				assertTrue("local IP".equals(reqPayload.getString("LocalIP")));			
			assertTrue("my msg".equals(mb.getString("CustomMsg")));
			assertTrue("86b185e84f6e79e92d8b66b1384f08d056bd5e4f".equals(mb.getString("RequestID")));
			
		} catch (JSONException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void updatePendingReqIPsTest() {
		try {
			String[] args = new String[] { "wallet ID", 
					"ext IP",
					"local IP",
					"my msg"};
			MessageBuilder mb = new MessageBuilder(ATGCMMessageType.UpdatePendingRequestIPs, args);
			
			mb.getString("tmp"); // just make sure it exists
			assertTrue("wallet ID".equals(mb.getString("WalletID")));
			assertTrue(ATGCMMessageType.UpdatePendingRequestIPs_VALUE == mb.getInt("RequestType"));
			JSONObject reqPayload = new JSONObject(mb.getString("ReqPayload"));
				assertTrue("ext IP".equals(reqPayload.getString("ExternalIP")));
				assertTrue("local IP".equals(reqPayload.getString("LocalIP")));			
			assertTrue("my msg".equals(mb.getString("CustomMsg")));
			
		} catch (JSONException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void coinsReceivedTest() {
		try {
			String[] args = new String[] { "wallet ID", 
					"my msg"};
			MessageBuilder mb = new MessageBuilder(ATGCMMessageType.CoinsReceived, args);
			
			mb.getString("tmp"); // just make sure it exists
			assertTrue("wallet ID".equals(mb.getString("WalletID")));
			assertTrue(ATGCMMessageType.CoinsReceived_VALUE == mb.getInt("RequestType"));	
			assertTrue("my msg".equals(mb.getString("CustomMsg")));
			
		} catch (JSONException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
