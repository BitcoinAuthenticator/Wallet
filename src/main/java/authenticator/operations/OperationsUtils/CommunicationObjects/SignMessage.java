package authenticator.operations.OperationsUtils.CommunicationObjects;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import authenticator.Utils.BAUtils;

public class SignMessage  extends JSONObject{
	private static final long serialVersionUID = 1L;
	//Vars
	public int version;
	public int numInputs;
	public boolean testnet;
	public String transaction;
	public ArrayList<KeyIndex> keyIndexArr;
	
	public SignMessage(){
		
	}
	//Serialization
	@SuppressWarnings("unchecked")
	public byte[] serializeToBytes(){
		this.put("version", version);
		this.put("testnet", testnet);
		this.put("ins_n", numInputs);
		this.put("tx", transaction);
		JSONArray keylist = new JSONArray();
		for (int a=0; a<numInputs; a++)
			keylist.add(keyIndexArr.get(a).getJsonObject());
		this.put("keylist", keylist);
		//Serialize and convert to byte[]
		StringWriter jsonOut = new StringWriter();
		try {JSONValue.writeJSONString(this, jsonOut);} 
		catch (IOException e1) {e1.printStackTrace();}
		String jsonText = jsonOut.toString();
		System.out.println(jsonText);
		byte[] jsonBytes = jsonText.getBytes();
		return jsonBytes;
	}
	
	public static ArrayList<byte[]> deserializeToBytes(byte[] payload) throws ParseException{
			 ArrayList<byte[]> sigs = new ArrayList<byte[]>();
			 String strJson = new String(payload);
			 JSONParser parser=new JSONParser();	  
			 Object obj = parser.parse(strJson);
			 JSONObject jsonObject = (JSONObject) obj;
			 int version = ((Long) jsonObject.get("version")).intValue();
			 int numSigs = ((Long) jsonObject.get("sigs_n")).intValue();
			 JSONArray msg = (JSONArray) jsonObject.get("siglist");
			 Iterator<JSONObject> iterator = msg.iterator();
			 JSONArray jsonlist = new JSONArray();
			 while (iterator.hasNext()) {
				 jsonlist.add(iterator.next());
			 }
			 JSONObject jsonObj = (JSONObject) obj;
			 for(int i=0; i<jsonlist.size(); i++){
				 jsonObj = (JSONObject) jsonlist.get(i);
				 String sig = (String) jsonObj.get("signature");
				 sigs.add(BAUtils.hexStringToByteArray(sig));
			 }
			 return sigs;
		 }
	
	public static JSONObject deserializeRefuseMessageToBoolean(byte[] payload) throws ParseException{
		 String strJson = new String(payload);
		 JSONParser parser=new JSONParser();	  
		 Object obj = parser.parse(strJson);
		 JSONObject jsonObject = (JSONObject) obj;
		 return jsonObject;
	 }
	
	//Setters
	public SignMessage setVersion(int value){
		this.version = value;
		return this;
	}
	public SignMessage setTestnet(boolean value){
		this.testnet = value;
		return this;
	}
	public SignMessage setInputNumber(int value){
		this.numInputs = value;
		return this;
	}
	public SignMessage setTxString(String value){
		this.transaction = value;
		return this;
	}
	public SignMessage setKeyIndexArray(ArrayList<byte[]> publickeys, ArrayList<Integer> childkeyindex){
		assert(publickeys.size() == childkeyindex.size());
		this.keyIndexArr = new ArrayList<KeyIndex>();
		for(int i=0;i<publickeys.size();i++)
		{
			KeyIndex ki = new KeyIndex(BAUtils.bytesToHex(publickeys.get(i)),childkeyindex.get(i));
			this.keyIndexArr.add(ki);
		}
		return this;
	}
	
	private class KeyIndex extends JSONObject{
		public String pubkey;
		public int index;
		public KeyIndex(String pubkey,int index){
			this.pubkey = pubkey;
			this.index = index;
		}
		public JSONObject getJsonObject(){
			this.put("pubkey", pubkey);
			this.put("index", index);
			return this;
		}
	}
}
