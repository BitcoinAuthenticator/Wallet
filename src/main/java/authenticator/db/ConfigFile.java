package authenticator.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import authenticator.protobuf.ProtoConfig;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet;
import authenticator.protobuf.ProtoConfig.ConfigReceiveAddresses;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.wallet.KeyChain;

import wallettemplate.Main;
import wallettemplate.utils.KeyUtils;

public class ConfigFile {
	
	String filePath;
	
	public ConfigFile() throws IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + Main.APP_NAME + ".config";
	}
	
	/**
	 * Will check if the config file exists or not.<br>
	 * 
	 * @return True if exists<br>False if doesn't exists
	 * @throws IOException
	 */
	public boolean checkConfigFile() throws IOException {
		File f = new File(this.filePath);
		if(f.exists() && !f.isDirectory()) 
			return true;
        return false;
	}
	
	public void setPaired(boolean paired) throws IOException{
		ConfigAuthenticatorWallet auth = ProtoConfig.ConfigAuthenticatorWallet.newBuilder().setPaired(paired).build();
		FileOutputStream output = new FileOutputStream(filePath);  
		auth.writeTo(output);          
		output.close();
	}
	
	public boolean getPaired() throws FileNotFoundException, IOException{
		ConfigAuthenticatorWallet auth = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
		boolean paired = auth.getPaired();
		return paired;
	}
	
	public void fillKeyPool() throws FileNotFoundException, IOException{
		ConfigReceiveAddresses keys = ConfigReceiveAddresses.parseFrom(new FileInputStream(filePath));
		ConfigReceiveAddresses.Builder ra = ConfigReceiveAddresses.newBuilder();
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
	
	public void removeAddressFromPool(String address) throws FileNotFoundException, IOException {
		ConfigReceiveAddresses keys = ConfigReceiveAddresses.parseFrom(new FileInputStream(filePath));
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		for (String hexkey : keys.getWalletKeyList()){
			ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(hexkey));
			String addr = key.toAddress(Main.params).toString();
			if (!addr.equals(address)){
				keypool.add(key);
			}
		}
		ConfigReceiveAddresses.Builder ra = ConfigReceiveAddresses.newBuilder();
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
		ConfigReceiveAddresses keys = ConfigReceiveAddresses.parseFrom(new FileInputStream(filePath));
		for (String pubkey : keys.getWalletKeyList()){
			ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(pubkey));
			keypool.add(key);
		}
		return keypool;
	}

}
