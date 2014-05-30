package authenticator.network;

import java.math.BigInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.bitcoin.core.Transaction;

import authenticator.Authenticator;
import authenticator.Utils.BAUtils;
import authenticator.operations.ATOperationType;

public class PendingRequest {
	public String pairingID;
	public String requestID;
	public ATOperationType operationType;
	// It seems too specific to put a Transaction variable here, but
	// most of the communication will involve a Tx
	public Transaction rawTx;
	public byte[] payloadIncoming;
	public byte[] payloadToSendInCaseOfConnection;
	public Contract contract;
	
	public PendingRequest(){
		contract = new Contract();
		contract.SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION = false;
		contract.SHOULD_SEND_PAYLOAD_ON_CONNECTION = false;
	}
	
	public PendingRequest(JSONObject json){
		this.pairingID = json.get("pairingID").toString();
		this.requestID = json.get("requestID").toString();
		switch(Integer.parseInt(json.get("operationType").toString())){
		case 1:
			this.operationType = ATOperationType.Pairing;
			break;
		case 2:
			this.operationType = ATOperationType.Unpair;
			break;
		case 4:
			this.operationType = ATOperationType.SignTx;
			break;
		}
		//
		if(json.containsKey("rawTx")){
			byte[] tx = BAUtils.hexStringToByteArray(json.get("rawTx").toString());
			this.rawTx = new Transaction(Authenticator.getWalletOperation().getNetworkParams(),tx);
		}
		if(json.containsKey("payloadIncoming"))
			this.payloadIncoming = BAUtils.hexStringToByteArray(json.get("payloadIncoming").toString());
		if(json.containsKey("payloadToSendInCaseOfConnection")){
			this.payloadToSendInCaseOfConnection = BAUtils.hexStringToByteArray(json.get("payloadToSendInCaseOfConnection").toString());
		}
		//
		this.contract = new Contract((JSONObject)json.get("contract"));
	}
	
	public PendingRequest(Object str){
		this((JSONObject)str);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getJsonObject(){
		JSONObject ret = new JSONObject();
		ret.put("pairingID", pairingID);
		ret.put("requestID", requestID);
		ret.put("operationType", operationType.getValue());
		if(rawTx != null)
			ret.put("rawTx",BAUtils.getStringTransaction(rawTx));
		if(payloadIncoming != null){
			ret.put("payloadIncoming",BAUtils.bytesToHex(payloadIncoming));
		}
		if(payloadToSendInCaseOfConnection != null){
			ret.put("payloadToSendInCaseOfConnection",BAUtils.bytesToHex(payloadToSendInCaseOfConnection));
		}
		//contract
		ret.put("contract", contract.getJsonObject());
		return ret;
	}
	
	/**
	 * Essentially a bunch of flags to indicate what should we do in case of a connection
	 * 
	 * @author alon
	 *
	 */
	public class Contract{
		public boolean SHOULD_SEND_PAYLOAD_ON_CONNECTION;
		public boolean SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION;
		
		@SuppressWarnings("unchecked")
		public JSONObject getJsonObject(){
			JSONObject ret = new JSONObject();
			ret.put("SHOULD_SEND_PAYLOAD_ON_CONNECTION", contract.SHOULD_SEND_PAYLOAD_ON_CONNECTION);
			ret.put("SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION", contract.SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION);
			
			return ret;
		}
		public Contract(){
			
		}
		public Contract(JSONObject obj){
			this.SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION = (boolean)obj.get("SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION");
			this.SHOULD_SEND_PAYLOAD_ON_CONNECTION = (boolean)obj.get("SHOULD_SEND_PAYLOAD_ON_CONNECTION");
		}
	}
}
