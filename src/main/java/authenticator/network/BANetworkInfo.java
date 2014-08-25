package authenticator.network;

public class BANetworkInfo {
	public String EXTERNAL_IP = null;
	public String INTERNAL_IP = null;
	
	BANetworkInfo(String ext, String internl){
		EXTERNAL_IP = ext;
		INTERNAL_IP = internl;
	}
}
