package authenticator.network;

public class BANeworkInfo {
	public String EXTERNAL_IP;
	public String INTERNAL_IP;
	
	BANeworkInfo(String ext, String internl){
		EXTERNAL_IP = ext;
		INTERNAL_IP = internl;
	}
}
