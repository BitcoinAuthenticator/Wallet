package authenticator.db;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.bitcoin.utils.Threading;

import authenticator.BASE;
import authenticator.Utils.BAUtils;
import authenticator.network.PendingRequest;

/**
 * <p>This class is the authenticators DB.</p>
 * 
 * <br>
 * @author Alon
 */
public class PendingRequestsFile extends BASE{
	
	String filePath = null;
	protected final ReentrantLock lock = Threading.lock("BApendingrequestfile");

	/**Contructor defines the loclation of the .json file*/
	public PendingRequestsFile(){
		super(PendingRequestsFile.class);
		lock.lock();
		try {
			filePath = BAUtils.getAbsolutePathForFile("pendingrequest.json");//new java.io.File( "." ).getCanonicalPath() + "/wallet.json";
		} catch (IOException e) {
			// it just means we don't have the file !
		}
		finally{
			lock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void writeNewPendingRequest(PendingRequest req){
		//Load the existing json file
		ArrayList<PendingRequest> arr = getPendingRequests();
		
		JSONArray jsonArr = new JSONArray();
		for(PendingRequest o:arr)
			jsonArr.add(o.getJsonObject());
		jsonArr.add(req.getJsonObject());
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
	
	@SuppressWarnings("unchecked")
	public void removePendingRequest(PendingRequest req){
		//Load the existing json file
		ArrayList<PendingRequest> arr = getPendingRequests();
		
		JSONArray jsonArr = new JSONArray();
		for(PendingRequest o:arr){
			if(!o.pairingID.equals(req.pairingID))
				jsonArr.add(o.getJsonObject());
		}
		
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
	
	public ArrayList<PendingRequest> getPendingRequests(){
		ArrayList<PendingRequest> ret = new ArrayList<PendingRequest>();
		JSONParser parser = new JSONParser();
		Object obj = null;
		JSONArray jsonArr = null;
		lock.lock();
		try {
			obj = parser.parse(new FileReader(filePath));
			jsonArr = (JSONArray)obj;
			if(jsonArr.size() > 0)
			{
				for(Object o:jsonArr)
				{
					PendingRequest po = new PendingRequest((JSONObject)o);
					ret.add(po);
				}
				
			}
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			lock.unlock();
		}
		
		return ret;
	}
	
}
