package authenticator.db;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;


/**
 *  Contains all pairing data for a single device\n
 * 
 * @author alon
 * 
 */
public class PairingObject extends JSONObject{
	private static final long serialVersionUID = -8835574682842732874L;
	// Private
	public String aes_key;
	public String master_public_key;
	public String chain_code;
	public String GCM;
	public String pairingID;
	public boolean testnet; 
	public int keys_n;
	public KeysArray keys;
	
	public PairingObject() {}
	@SuppressWarnings("unchecked")
	public PairingObject(JSONObject obj)
	{
		aes_key = (String) obj.get("aes_key");
		master_public_key = (String) obj.get("master_public_key");
		chain_code = (String) obj.get("chain_code");
		GCM = (String) obj.get("GCM");
		pairingID = (String) obj.get("pairingID");
		testnet = (boolean) obj.get("testnet");
		keys_n = ((Long) obj.get("keys_n")).intValue();
		keys = new KeysArray((JSONArray) obj.get("keys")) ;
	}
	
	@SuppressWarnings("unchecked")
	public PairingObject getJSONObject()
	{
		this.put("aes_key", aes_key);
		this.put("master_public_key", master_public_key);
		this.put("chain_code", chain_code);
		this.put("GCM", GCM);
		this.put("pairingID", pairingID);
		this.put("testnet", testnet);
		this.put("keys_n", keys_n);
		this.put("keys", keys.getJSONObject());
		return this;
	}
	
	// Setting 
	public PairingObject setAES(String input){
		aes_key = input;
		return this;
	}
	public PairingObject setMasterPubKey(String input){
		master_public_key = input;
		return this;
	}
	public PairingObject setChainCode(String input){
		chain_code = input;
		return this;
	}
	public PairingObject setGCM(String input){
		GCM = input;
		return this;
	}
	public PairingObject setPairingID(String input){
		pairingID = input;
		return this;
	}
	public PairingObject setTestNetMode(boolean input){
		testnet = input;
		return this;
	}
	public PairingObject setKeysArray(KeysArray input){
		keys_n = input.size();
		keys = input;
		return this;
	}
	@SuppressWarnings("unchecked")
	public PairingObject addKey(KeyObject input){
		keys_n ++;
		keys.addKey(input);
		return this;
	}
}
