package authenticator.GCM.dispacher;

import java.sql.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;


public class MessageBuilder extends JSONObject{
	public MessageBuilder(MessageType type,String[] ... arg) throws JSONException
	{
		switch (type){
			case test:
				this.append("data","Hello World");
				break;
			case signTx:
				this.put("tmp", new Timestamp( new java.util.Date().getTime() ));
				this.put("PairingID", arg[0][0]); 
				this.put("RequestID", 1); // TODO
				JSONObject reqPayload = new JSONObject();
				reqPayload.put("ExternalIP", arg[0][1]);
				reqPayload.put("LocalIP", arg[0][2]);
				this.put("ReqPayload", reqPayload);
				this.put("CustomMsg", "New Tx For Signing");
				break;
		}
	}
}
