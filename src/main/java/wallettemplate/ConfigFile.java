package wallettemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.wallet.KeyChain;

import wallettemplate.utils.KeyUtils;
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
		if (keys.getWalletKeyCount()<10){
			int num = (10 - keys.getWalletKeyCount());
			for (int i=0; i<num; i++){
				DeterministicKey newkey = Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
				String pubkey =  KeyUtils.bytesToHex(newkey.getPubKey());
				ra.addWalletKey(pubkey);
			}
			FileOutputStream output = new FileOutputStream(filePath);  
			ra.build().writeTo(output);          
			output.close();
		}
	}
	
	public void removeAddress(String address) throws FileNotFoundException, IOException {
		ReceiveAddresses keys = ReceiveAddresses.parseFrom(new FileInputStream(filePath));
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		for (String hexkey : keys.getWalletKeyList()){
			ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(hexkey));
			String addr = key.toAddress(Main.params).toString();
			if (!addr.equals(address)){
				keypool.add(key);
			}
		}
		ReceiveAddresses.Builder ra = ReceiveAddresses.newBuilder();
		ra.clear().build();
		for (ECKey pkeys : keypool){
			String pubkey =  KeyUtils.bytesToHex(pkeys.getPubKey());
			ra.addWalletKey(pubkey);
		}
		FileOutputStream output = new FileOutputStream(filePath);  
		ra.build().writeTo(output);          
		output.close();
	}
	
	public ArrayList<ECKey> getKeyPool() throws FileNotFoundException, IOException{
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		ReceiveAddresses keys = ReceiveAddresses.parseFrom(new FileInputStream(filePath));
		for (String pubkey : keys.getWalletKeyList()){
			ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(pubkey));
			keypool.add(key);
		}
		return keypool;
	}

}
