package authenticator;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import authenticator.db.KeyObject;
import authenticator.db.KeysArray;
import authenticator.db.PairingObject;

/**
 * This class manages the saving a loading of keys to and from a .json file.
 * 
 * WalletFile structure
 * 	{ // Main body
 * 
 * 		{ // Pairing Data array
 * 			
 * 			{ 
 * 				
 * 				{@link wallet.db.PairingObject}	
 * 
 * 			}
 * 
 * 		}
 * 
 * 	}
 */
public class WalletFile {
	
	String filePath = null;

	/**Contructor defines the loclation of the .json file*/
	public WalletFile(){
		try {
			filePath = BAUtils.getAbsolutePathForFile("wallet.json");//new java.io.File( "." ).getCanonicalPath() + "/wallet.json";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Saves the network paraments to the JSON file */
	void writeNetworkParams(Boolean testnet){
		//Load the existing json file
		ArrayList<PairingObject> oPairing = getPairingObjectsArray();
		for(PairingObject o:oPairing)
		{
			o.setTestNetMode(testnet);
		}
		JSONArray jsonArr = new JSONArray();
		for(PairingObject o:oPairing)
			jsonArr.add(o.getJSONObject());
		try {
			FileWriter file = new FileWriter(filePath);
			file.write(jsonArr.toString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*JSONParser parser = new JSONParser();
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
		//Save the new json file
		Map newobj=new LinkedHashMap();
		newobj.put("aes_key", aeskey);
		newobj.put("master_public_key", mpubkey);
		newobj.put("chain_code", chaincode);
		newobj.put("testnet", testnet);
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
		}*/
	}
	
	/**
	 * This method is used to save a new address and private key to file. It loads the existing .json file,
	 * adds a new wallet object to it, then saves it back to file.  
	 */
	@SuppressWarnings("unchecked")
	void writeToFile(String pairID, String privkey, String addr){
		ArrayList<PairingObject> oPairing = getPairingObjectsArray();
		PairingObject obj = null;
		for(PairingObject o:oPairing)
		{
			if(o.pairingID.equals(pairID)){
				obj = o;
				break;
			}
		}
		if(obj != null)
		{
			KeyObject newKey = new KeyObject()
								.setAddress(addr)
								.setPrivateKey(privkey)
								.setIndex(obj.keys_n); // keys_n is incremented only after adding the key
			obj.addKey(newKey);
		}
		JSONArray jsonArr = new JSONArray();
		for(PairingObject o:oPairing)
			jsonArr.add(o.getJSONObject());
		try {
			FileWriter file = new FileWriter(filePath);
			file.write(jsonArr.toString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*//Load the existing json file
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
		Boolean testnet = (Boolean) jsonObject.get("testnet");
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
		newobj.put("testnet", testnet);
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
			}*/
	}
	
	/**This method is used during pairing. It saves the data from the Autheticator to file*/
	@SuppressWarnings("unchecked")
	public void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID){
		// Create new pairing item
		PairingObject newPair = new PairingObject()
				.setAES(key)
				.setMasterPubKey(mpubkey)
				.setChainCode(chaincode)
				.setGCM(GCM)
				.setPairingID(pairingID)
				.setTestNetMode(false)
				.setKeysArray(new KeysArray());
		
		// Read data from walletfile
		JSONParser parser = new JSONParser();
		Object obj = null;
		JSONArray jsonArr = null;
		try {
			try
			{
				obj = parser.parse(new FileReader(filePath));
				jsonArr = (JSONArray)obj;
			}
			catch (Exception e) { jsonArr = new JSONArray(); }
			jsonArr.add(newPair.getJSONObject());
			FileWriter file = new FileWriter(filePath);
			file.write(jsonArr.toString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		/*JSONArray jsonlist = new JSONArray();
		Map obj=new LinkedHashMap();
		obj.put("aes_key", key);
		obj.put("master_public_key", mpubkey);
		obj.put("chain_code", chaincode);
		obj.put("GCM", GCM);
		obj.put("testnet", false);
		obj.put("keys_n", new Integer(0));
		obj.put("keys", jsonlist);
		StringWriter jsonOut = new StringWriter();
		try {
			JSONValue.writeJSONString(obj, jsonOut);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String jsonText = jsonOut.toString();
		
		//Add pairing data to file
		JSONParser parser = new JSONParser();
		Object fileObj = null;
		try {
			fileObj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		
		JSONObject jsonObject = (JSONObject) obj;
		
		try {
			FileWriter file = new FileWriter(filePath);
			file.write(jsonText);
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
	
	private ArrayList<PairingObject> getPairingObjectsArray()
	{
		ArrayList<PairingObject> ret = new ArrayList<PairingObject>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		JSONArray jsonArr = null;
		try {
			obj = parser.parse(new FileReader(filePath));
			jsonArr = (JSONArray)obj;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		if(jsonArr.size() > 0)
		{
			for(Object o:jsonArr)
			{
				PairingObject po = new PairingObject((JSONObject)o);
				ret.add(po);
			}
			
		}
		return ret;
	}
	
	public ArrayList<String> getPairingIDs()
	{
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		ArrayList<String> ret = new ArrayList<String>();
		for(PairingObject o:pObjects)
			ret.add(o.pairingID);
		return ret;
	}
	
	/**Pulls the AES key from file and returns it*/
	public String getAESKey(String pairID){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
				return o.aes_key;
		}
		return "";
		/*JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		String aeskey = (String) jsonObject.get("aes_key");
		return aeskey;*/
	}
	
	/**Returns the number of key pairs in the wallet*/
	public long getKeyNum(String pairID){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
				return o.keys_n;
		}
		return 0;
		/*JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		long numkeys = (Long) jsonObject.get("keys_n");
		return numkeys;*/
	}
	
	/**Returns the Master Public Key and Chaincode as an ArrayList object*/
	public ArrayList<String> getPubAndChain(String pairID){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		ArrayList<String> ret = new ArrayList<String>();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
			{
				ret.add(o.master_public_key);
				ret.add(o.chain_code);
			}
		}
		return ret;
		/*ArrayList<String> arr = new ArrayList<String>();
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
		return arr;*/
	}
	
	/** Returns a list of all addresses in the wallet*/
	public ArrayList<String> getAddresses(String pairID){
		ArrayList<String> arr = new ArrayList<String>();
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
				for(Object obj:o.keys.keys)
				{
					KeyObject ko = (KeyObject)obj;
					arr.add(ko.address);
				}
		}
		/*JSONParser parser = new JSONParser();
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
		}	*/	
		return arr;
	}
	
	/**Returns the Child Key Index for a given address in the wallet*/
	public long getAddrIndex(String pairID, String Address){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
			for(Object ob: o.keys)
			{
				KeyObject ko = (KeyObject)ob;
				if(ko.address == Address)
					return ko.index;
			}
		}
		return 0;
		/*ArrayList<String> arr = new ArrayList<String>();
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
		return 0;*/
	}
	
	/**Returns the private key for a given address*/
	public String getPrivKey(String pairID,String Address){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
				for(Object ob: o.keys.keys)
				{
					KeyObject ko = (KeyObject)ob;
					if(ko.address.equals(Address))
						return ko.priv_key;
				}
		}
		return null;
		/*ArrayList<String> arr = new ArrayList<String>();
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
		return null;*/
	}
	
	/**Returns the private key using an index as the input*/
	public String getPrivKeyFromIndex(String pairID, int index){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
		{
			if(o.pairingID.equals(pairID))
				for(Object ob: o.keys.keys)
				{
					KeyObject ko = (KeyObject)ob;
					if(ko.index == index)
						return ko.priv_key;
				}
		}
		return null;
		/*ArrayList<String> arr = new ArrayList<String>();
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
		return null;*/
	}
	
	public Map<String,Boolean> getTestnets(){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		Map<String,Boolean> ret = new HashMap<String,Boolean>();
		for(PairingObject o:pObjects)
			ret.put(o.pairingID, o.testnet);
		return ret;
	}
	
	public String getGCMRegID(String pairID){
		ArrayList<PairingObject> pObjects = this.getPairingObjectsArray();
		for(PairingObject o:pObjects)
			if(o.pairingID.equals(pairID))
				return o.GCM;
		return null;
		/*JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(new FileReader(filePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		String GCM = (String) jsonObject.get("GCM");
		return GCM;*/
	}
	
}
