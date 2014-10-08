package authenticator.network;

public class BANetworkInfo {
	public String EXTERNAL_IP = null;
	public String INTERNAL_IP = null;
	
	/**
	 * Network Requirements flags
	 */
	public  boolean PORT_FORWARDED = false;
	public  boolean SOCKET_OPERATIONAL = false;
	
	public BANetworkInfo() {
		
	}
	
	public BANetworkInfo(String ext, String internl){
		EXTERNAL_IP = ext;
		INTERNAL_IP = internl;
	}
}
