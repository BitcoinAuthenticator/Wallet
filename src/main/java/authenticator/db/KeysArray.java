package authenticator.db;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class KeysArray extends JSONArray{
	private static final long serialVersionUID = -3061086209028050795L;

	public ArrayList<KeyObject> keys;
	
	public KeysArray() { keys = new ArrayList<KeyObject>(); }
	public KeysArray(JSONArray arr)
	{
		keys = new ArrayList<KeyObject>();
		for(Object o:arr)
		{
			KeyObject ko = new KeyObject((JSONObject)o);
			keys.add(ko);
		}
	}
	
	@SuppressWarnings("unchecked")
	public KeysArray getJSONObject()
	{
		for(KeyObject o:keys)
			this.add(o.getJSONObject());
		return this;
	}
	
	public KeysArray addKey(KeyObject key)
	{
		keys.add(key);
		return this;
	}
}
