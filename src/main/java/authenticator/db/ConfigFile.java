package authenticator.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import authenticator.protobuf.ProtoConfig;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet.PendingRequest;
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
	
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException{
		ConfigAuthenticatorWallet all = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
		return all.getPairedWalletsList();
	}
	
	/**This method is used during pairing. It saves the data from the Autheticator to file
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	public void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, String pairName) throws IOException{
		// Create new pairing item
		PairedAuthenticator.Builder newPair = PairedAuthenticator.newBuilder();
									newPair.setAesKey(key);
									newPair.setMasterPublicKey(mpubkey);
									newPair.setChainCode(chaincode);
									newPair.setGCM(GCM);
									newPair.setPairingID(pairingID);
									newPair.setPairingName(pairName);
									newPair.setTestnet(false);

		ConfigAuthenticatorWallet all = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
		ConfigAuthenticatorWallet.Builder b = ConfigAuthenticatorWallet.newBuilder(all);
		b.addPairedWallets(newPair.build());
									
		FileOutputStream output = new FileOutputStream(filePath);  
		b.build().writeTo(output);          
		output.close();
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	public void addAddressAndPrivateKeyToPairing(String pairID, String privkey, String addr, int index) throws FileNotFoundException, IOException, ParseException{
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = this.getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		
		for(PairedAuthenticator o:all)
		{
			if(o.getPairingID().equals(pairID)){
				PairedAuthenticator.KeysObject.Builder newKeyObj = PairedAuthenticator.KeysObject.newBuilder();
				newKeyObj.setPrivKey(privkey);
				newKeyObj.setAddress(addr);
				newKeyObj.setIndex(index);
				
				PairedAuthenticator.Builder pairingBuilder = PairedAuthenticator.newBuilder(o);
				pairingBuilder.addGeneratedKeys(newKeyObj.build());
				PairedAuthenticator pair = pairingBuilder.build();
				
				
				ConfigAuthenticatorWallet b = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
				ConfigAuthenticatorWallet.Builder b1 = ConfigAuthenticatorWallet.newBuilder(b);
				b1.setPairedWallets(pair.getDescriptor().getIndex(), pair);
				
				FileOutputStream output = new FileOutputStream(filePath);  
				b1.build().writeTo(output);          
				output.close();
				
				break;
			}
		}
		
	}
	
	public List<PendingRequest> getPendingRequests() throws FileNotFoundException, IOException{
		ConfigAuthenticatorWallet all = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
		return all.getPendingRequestsList();
	}
	
	public void writeNewPendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
		ConfigAuthenticatorWallet b = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
		ConfigAuthenticatorWallet.Builder b1 = ConfigAuthenticatorWallet.newBuilder(b);
		b1.addPendingRequests(req);
		
		FileOutputStream output = new FileOutputStream(filePath);  
		b1.build().writeTo(output);          
		output.close();
	}
	
	public void removePendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
		ConfigAuthenticatorWallet b = ConfigAuthenticatorWallet.parseFrom(new FileInputStream(filePath));
		ConfigAuthenticatorWallet.Builder b1 = ConfigAuthenticatorWallet.newBuilder(b);
		b1.removePendingRequests(req.getDescriptor().getIndex());
		
		FileOutputStream output = new FileOutputStream(filePath);  
		b1.build().writeTo(output);          
		output.close();
	}
}
