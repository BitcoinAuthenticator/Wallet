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
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.bitcoin.utils.Threading;

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
public class WalletFile extends BASE{
	
	String filePath = null;
	protected final ReentrantLock lock = Threading.lock("BAwalletfile");

	/**Contructor defines the loclation of the .json file*/
	public WalletFile(){
		super(WalletFile.class);
		lock.lock();
		try {
			filePath = BAUtils.getAbsolutePathForFile("wallet.json");//new java.io.File( "." ).getCanonicalPath() + "/wallet.json";
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			lock.unlock();
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
		lock.lock();
		try {
			FileWriter file = new FileWriter(filePath);
			file.write(jsonArr.toString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{ lock.unlock(); }
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
		lock.lock();
		try {
			FileWriter file = new FileWriter(filePath);
			file.write(jsonArr.toString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally { lock.unlock(); }
	}
	
	/**This method is used during pairing. It saves the data from the Autheticator to file*/
	@SuppressWarnings("unchecked")
	public void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, String pairName){
		// Create new pairing item
		PairingObject newPair = new PairingObject()
				.setAES(key)
				.setMasterPubKey(mpubkey)
				.setChainCode(chaincode)
				.setGCM(GCM)
				.setPairingID(pairingID)
				.setPairingName(pairName)
				.setTestNetMode(false)
				.setKeysArray(new KeysArray());
		
		// Read data from walletfile
		JSONParser parser = new JSONParser();
		Object obj = null;
		JSONArray jsonArr = null;
		lock.lock();
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
		finally{ lock.unlock(); }
	}
	
	public ArrayList<PairingObject> getPairingObjectsArray()
	{
		ArrayList<PairingObject> ret = new ArrayList<PairingObject>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		JSONArray jsonArr = null;
		lock.lock();
		try {
			obj = parser.parse(new FileReader(filePath));
			jsonArr = (JSONArray)obj;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		finally{ lock.unlock(); }
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
	}
	
}
