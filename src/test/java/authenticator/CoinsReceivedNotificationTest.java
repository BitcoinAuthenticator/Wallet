package authenticator;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import authenticator.GCM.dispacher.MessageBuilder;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;

public class CoinsReceivedNotificationTest {

	@Test
	public void test(){
		try{
			MessageBuilder b = new MessageBuilder(ATGCMMessageType.CoinsReceived,
					new String[]{"pairing id", "custom msg"});
			String result = b.toString();
			JSONObject objResult = new JSONObject(result);
			assertTrue(objResult.getString("WalletID").equals("pairing id"));
			assertTrue(objResult.getInt("RequestType") == ATGCMMessageType.CoinsReceived_VALUE);			
			assertTrue(objResult.getString("CustomMsg").equals("custom msg"));
		}
		catch (JSONException e){
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
