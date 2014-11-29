package authenticator.operations;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import authenticator.GCM.dispacher.MessageBuilder;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;

public class UpdateIPsTest {

	@Test
	public void test(){
		try{
			MessageBuilder b = new MessageBuilder(ATGCMMessageType.UpdatePendingRequestIPs,
												new String[]{"pairing id",
												"external ip",
											    "local ip", 
											    "custom msg"});
			String result = b.toString();
			JSONObject objResult = new JSONObject(result);
			assertTrue(objResult.getString("WalletID").equals("pairing id"));
			
			assertTrue(objResult.getInt("RequestType") == ATGCMMessageType.UpdatePendingRequestIPs_VALUE);
			JSONObject payload = new JSONObject(objResult.getString("ReqPayload"));
			assertTrue(payload.getString("ExternalIP").equals("external ip"));
			assertTrue(payload.getString("LocalIP").equals("local ip"));
			
			assertTrue(objResult.getString("CustomMsg").equals("custom msg"));
		}
		catch (JSONException e){
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
