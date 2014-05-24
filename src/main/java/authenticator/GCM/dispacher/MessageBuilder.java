package authenticator.GCM.dispacher;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
				this.put("RequestType", type.getValue()); 
				JSONObject reqPayload = new JSONObject();
				reqPayload.put("ExternalIP", arg[0][1]);
				reqPayload.put("LocalIP", arg[0][2]);
				this.put("ReqPayload", reqPayload);
				this.put("CustomMsg", "New Tx For Signing"); // TODO localize
				this.put("RequestID", getRequestIDDigest(this));
				break;
		}
	}
	
	private String getConcatinatedPayload(MessageBuilder msg) throws JSONException{
		String ReqPayload = msg.get("ReqPayload").toString();
		String tmp = msg.get("tmp").toString();
		String RequestType = msg.get("RequestType").toString();
		String PairingID = msg.get("PairingID").toString();
		return ReqPayload +
				tmp + 
				RequestType + 
				PairingID;
	}
	
	private String getRequestIDDigest(MessageBuilder msg) throws JSONException
	 {
		MessageDigest md = null;
		try {md = MessageDigest.getInstance("SHA-1");}
		catch(NoSuchAlgorithmException e) {e.printStackTrace();} 
	    byte[] digest = md.digest(getConcatinatedPayload(msg).getBytes());
	    String ret = new BigInteger(1, digest).toString(16);
	    //Make sure it is 40 chars, if less pad with 0, if more substringit
	    if(ret.length() > 40)
	    {
	    	ret = ret.substring(0, 39);
	    }
	    else if(ret.length() < 40)
	    {
	    	int paddingNeeded = 40 - ret.length();
	    	String padding = "";
	    	for(int i=0;i<paddingNeeded;i++)
	    		padding = padding + "0";
	    	ret = padding + ret;
	    }
	    return ret;
	}
}
