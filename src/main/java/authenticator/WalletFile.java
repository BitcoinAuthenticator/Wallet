package authenticator;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This class manages the saving a loading of keys to and from a .json file.
 */
public class WalletFile {
	
	String filePath = null;

	/**Contructor defines the loclation of the .json file*/
	public WalletFile(){
		try {
			filePath = new java.io.File( "." ).getCanonicalPath() + "/wallet.json";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is used to save a new address and private key to file. It loads the existing .json file,
	 * adds a new wallet object to it, then saves it back to file.  
	 */
	void writeToFile(String privkey, String addr){
		//Load the existing json file
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		String aeskey = (String) jsonObject.get("aes_key");
		String mpubkey = (String) jsonObject.get("master_public_key");
		String chaincode = (String) jsonObject.get("chain_code");
		long numkeys = (Long) jsonObject.get("keys_n");
		JSONArray msg = (JSONArray) jsonObject.get("keys");
		Iterator<JSONObject> iterator = msg.iterator();
		JSONArray jsonlist = new JSONArray();
		while (iterator.hasNext()) {
			jsonlist.add(iterator.next());
		}
		//Create the key json object
		numkeys ++;
		JSONObject keyobj = new JSONObject();
		keyobj.put("index",new Integer((int) numkeys));
		keyobj.put("priv_key", privkey);
		keyobj.put("address", addr);
		//Add key object to array
		jsonlist.add(keyobj);
		//Save the new json file
		Map newobj=new LinkedHashMap();
		newobj.put("aes_key", aeskey);
		newobj.put("master_public_key", mpubkey);
		newobj.put("chain_code", chaincode);
		newobj.put("keys_n", numkeys);
		newobj.put("keys", jsonlist);
		StringWriter jsonOut = new StringWriter();
		try {
			JSONValue.writeJSONString(newobj, jsonOut);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String jsonText = jsonOut.toString();
			try {
				FileWriter file = new FileWriter(filePath);
				file.write(jsonText);
				file.flush();
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	/**This method is used during pairing. It saves the data from the Autheticator to file*/
	public void writePairingData(String mpubkey, String chaincode, String key){
		JSONArray jsonlist = new JSONArray();
		Map obj=new LinkedHashMap();
		obj.put("aes_key", key);
		obj.put("master_public_key", mpubkey);
		obj.put("chain_code", chaincode);
		obj.put("keys_n", new Integer(0));
		obj.put("keys", jsonlist);
		StringWriter jsonOut = new StringWriter();
		try {
			JSONValue.writeJSONString(obj, jsonOut);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String jsonText = jsonOut.toString();
		try {
			FileWriter file = new FileWriter(filePath);
			file.write(jsonText);
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Pulls the AES key from file and returns it*/
	public String getAESKey(){
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		String aeskey = (String) jsonObject.get("aes_key");
		return aeskey;
	}
	
	/**Returns the number of key pairs in the wallet*/
	public long getKeyNum(){
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		long numkeys = (Long) jsonObject.get("keys_n");
		return numkeys;
	}
	
	/**Returns the Master Public Key and Chaincode as an ArrayList object*/
	public ArrayList<String> getPubAndChain(){
		ArrayList<String> arr = new ArrayList<String>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		arr.add((String) jsonObject.get("master_public_key"));
		arr.add((String) jsonObject.get("chain_code"));
		return arr;
	}
	
	/** Returns a list of all addresses in the wallet*/
	public ArrayList<String> getAddresses(){
		ArrayList<String> arr = new ArrayList<String>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		JSONArray msg = (JSONArray) jsonObject.get("keys");
		Iterator<JSONObject> iterator = msg.iterator();
		JSONArray jsonlist = new JSONArray();
		while (iterator.hasNext()) {
			jsonlist.add(iterator.next());
		}
		JSONObject jsonAddr = (JSONObject) obj;
		for(int i=0; i<jsonlist.size(); i++){
			jsonAddr = (JSONObject) jsonlist.get(i);
			arr.add((String) jsonAddr.get("address"));
		}		
		return arr;
	}
	
	/**Returns the Child Key Index for a given address in the wallet*/
	public long getAddrIndex(String Address){
		ArrayList<String> arr = new ArrayList<String>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		JSONArray msg = (JSONArray) jsonObject.get("keys");
		Iterator<JSONObject> iterator = msg.iterator();
		JSONArray jsonlist = new JSONArray();
		while (iterator.hasNext()) {
			jsonlist.add(iterator.next());
		}
		JSONObject jsonAddr = (JSONObject) obj;
		for(int i=0; i<jsonlist.size(); i++){
			jsonAddr = (JSONObject) jsonlist.get(i);
			String jaddr = (String) jsonAddr.get("address");
			long index = (Long) jsonAddr.get("index");
			if (jaddr.equals(Address)){return index;}	
		}
		return 0;
	}
	
	/**Returns the private key for a given address*/
	public String getPrivKey(String Address){
		ArrayList<String> arr = new ArrayList<String>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		JSONArray msg = (JSONArray) jsonObject.get("keys");
		Iterator<JSONObject> iterator = msg.iterator();
		JSONArray jsonlist = new JSONArray();
		while (iterator.hasNext()) {
			jsonlist.add(iterator.next());
		}
		JSONObject jsonAddr = (JSONObject) obj;
		for(int i=0; i<jsonlist.size(); i++){
			jsonAddr = (JSONObject) jsonlist.get(i);
			String jaddr = (String) jsonAddr.get("address");
			String pkey = (String) jsonAddr.get("priv_key");
			if (jaddr.equals(Address)){return pkey;}	
		}
		return null;
	}
	
	/**Returns the private key using an index as the input*/
	public String getPrivKeyFromIndex(long index){
		ArrayList<String> arr = new ArrayList<String>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		JSONArray msg = (JSONArray) jsonObject.get("keys");
		Iterator<JSONObject> iterator = msg.iterator();
		JSONArray jsonlist = new JSONArray();
		while (iterator.hasNext()) {
			jsonlist.add(iterator.next());
		}
		JSONObject jsonAddr = (JSONObject) obj;
		for(int i=0; i<jsonlist.size(); i++){
			jsonAddr = (JSONObject) jsonlist.get(i);
			long jIndex = (Long) jsonAddr.get("index");
			String pkey = (String) jsonAddr.get("priv_key");
			if (jIndex==index){return pkey;}	
		}
		return null;
	}
	
}
