package authenticator.GCM.dispacher;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.protobuf.ProtoConfig.ATGCMMessageType;


public class MessageBuilder extends JSONObject{
	public MessageBuilder(ATGCMMessageType type,String ... arg) throws JSONException
	{
		JSONObject reqPayload;
		switch (type){
			/**
			 * arg - <br>
			 * 0 - WalletID<br>
			 * 1 - ExternalIP <br>
			 * 2 - LocalIP <br>
			 * 3 - CustomMsg <br>
			 * 4 - tmp, for testing purposes <br>
			 */
			case SignTX:
				if(arg.length > 4)
					this.put("tmp", arg[4]);
				else
					this.put("tmp", new Timestamp( new java.util.Date().getTime() ));
				this.put("WalletID", arg[0]); 
				this.put("RequestType", ATGCMMessageType.SignTX_VALUE); 
				reqPayload = new JSONObject();
				reqPayload.put("ExternalIP", arg[1]);
				reqPayload.put("LocalIP", arg[2]);
				this.put("ReqPayload", reqPayload);
				this.put("CustomMsg", arg[3]); // TODO localize
				this.put("RequestID", getRequestIDDigest(this));
				break;
			/**
			 * arg - <br>
			 * 0 - WalletID<br>
			 * 1 - ExternalIP <br>
			 * 2 - LocalIP <br>
			 * 3 - CustomMsg <br>
			 */
			case UpdatePendingRequestIPs:
				this.put("tmp", new Timestamp( new java.util.Date().getTime() ));
				this.put("WalletID", arg[0]); 
				this.put("RequestType", ATGCMMessageType.UpdatePendingRequestIPs_VALUE); 
				reqPayload = new JSONObject();
				reqPayload.put("ExternalIP", arg[1]);
				reqPayload.put("LocalIP", arg[2]);
				this.put("ReqPayload", reqPayload);
				this.put("CustomMsg", arg[3]); // TODO localize
				break;
			/**
			 * arg - <br>
			 * 0 - WalletID<br>
			 * 1 - CustomMsg <br>
			 */
			case CoinsReceived:
				this.put("tmp", new Timestamp( new java.util.Date().getTime() ));
				this.put("WalletID", arg[0]); 
				this.put("RequestType", ATGCMMessageType.CoinsReceived_VALUE); 
				this.put("CustomMsg", arg[1]); 
				break;
		}
	}
	
	private String getConcatinatedPayload(MessageBuilder msg) throws JSONException{
		String ReqPayload = msg.get("ReqPayload").toString();
		String tmp = msg.get("tmp").toString();
		String RequestType = msg.get("RequestType").toString();
		String WalletID = msg.get("WalletID").toString();
		return ReqPayload +
				tmp + 
				RequestType + 
				WalletID;
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
