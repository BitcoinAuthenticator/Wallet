package authenticator.GCM.dispacher;

import javax.crypto.SecretKey;

public class Device {
	public  byte[] chaincode;
	public  byte[] mPubKey;
	public  byte[] gcmRegId;
	public  byte[] pairingID;
	public  SecretKey sharedsecret;
	
	public Device(){ }
	public Device(byte[] chain,
			byte[] pubKey, 
			byte[] gcm,
			byte[] pairID,
			SecretKey secret)
	{
		chaincode = chain;
		mPubKey = pubKey;
		gcmRegId = gcm;
		pairingID = pairID;
		sharedsecret = secret;
	}
}
