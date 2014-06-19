package wallettemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.wallet.KeyChain;

import wallettemplate.utils.ProtoConfig;
import wallettemplate.utils.ProtoConfig.Authenticator;
import wallettemplate.utils.ProtoConfig.ReceiveAddresses;
import wallettemplate.utils.ProtoConfig.ReceiveAddresses.Builder;

public class ConfigFile {
	
	String filePath;
	
	public ConfigFile() throws IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + Main.APP_NAME + ".config";
	}
	
	public void setPaired(boolean paired) throws IOException{
		Authenticator auth = ProtoConfig.Authenticator.newBuilder().setPaired(paired).build();
		FileOutputStream output = new FileOutputStream(filePath);  
		auth.writeTo(output);          
		output.close();
	}
	
	public boolean getPaired() throws FileNotFoundException, IOException{
		Authenticator auth = Authenticator.parseFrom(new FileInputStream(filePath));
		boolean paired = auth.getPaired();
		return paired;
	}
	
	public void fillKeyPool() throws FileNotFoundException, IOException{
		ReceiveAddresses keys = ReceiveAddresses.parseFrom(new FileInputStream(filePath));
		ReceiveAddresses.Builder ra = ReceiveAddresses.newBuilder();
		if (keys.getPubkeyCount()<9){
			int num = (9 - keys.getPubkeyCount());
			for (int i=0; i<num; i++){
				DeterministicKey newkey = Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
				String pubkey =  bytesToHex(newkey.getPubKey());
				ra.addPubkey(pubkey);
			}
			FileOutputStream output = new FileOutputStream(filePath);  
			ra.build().writeTo(output);          
			output.close();
		}
	}
	
	public void removeAddress(String address) throws FileNotFoundException, IOException {
		ReceiveAddresses keys = ReceiveAddresses.parseFrom(new FileInputStream(filePath));
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		for (String hexkey : keys.getPubkeyList()){
			ECKey key = ECKey.fromPublicOnly(hexStringToByteArray(hexkey));
			String addr = key.toAddress(Main.params).toString();
			if (!addr.equals(address)){
				keypool.add(key);
			}
		}
		keys.newBuilder().clear();
		ReceiveAddresses.Builder ra = ReceiveAddresses.newBuilder();
		for (ECKey pkeys : keypool){
			String pubkey =  bytesToHex(pkeys.getPubKey());
			ra.addPubkey(pubkey);
		}
		FileOutputStream output = new FileOutputStream(filePath);  
		ra.build().writeTo(output);          
		output.close();
	}
	
	public ArrayList<ECKey> getKeyPool() throws FileNotFoundException, IOException{
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		ReceiveAddresses keys = ReceiveAddresses.parseFrom(new FileInputStream(filePath));
		for (String pubkey : keys.getPubkeyList()){
			ECKey key = ECKey.fromPublicOnly(hexStringToByteArray(pubkey));
			keypool.add(key);
		}
		return keypool;
	}

	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
