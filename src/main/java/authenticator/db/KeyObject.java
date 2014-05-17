package authenticator.db;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public class KeyObject  extends JSONObject{
	private static final long serialVersionUID = -8131380065862785147L;
	// Private
	public String priv_key;
	public String address;
	public int index;
	
	public KeyObject() {}
	@SuppressWarnings("unchecked")
	public KeyObject(JSONObject obj)
	{
		priv_key = (String) obj.get("priv_key");
		address = (String) obj.get("address");
		index = ((Long) obj.get("index")).intValue();

	}
	
	@SuppressWarnings("unchecked")
	public KeyObject getJSONObject()
	{
		this.put("priv_key", priv_key);
		this.put("address", address);
		this.put("index", index);
		return this;
	}
	
	// Setting 
	public KeyObject setPrivateKey(String input){
		priv_key = input;
		return this;
	}
	public KeyObject setAddress(String input){
		address = input;
		return this;
	}
	public KeyObject setIndex(int input){
		index = input;
		return this;
	}
}
