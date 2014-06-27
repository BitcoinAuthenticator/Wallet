package authenticator.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.parser.ParseException;

import authenticator.protobuf.ProtoConfig;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.wallet.KeyChain;

import wallettemplate.Main;
import wallettemplate.utils.KeyUtils;

public class ConfigFile {
	
	static public String filePath;
	
	public ConfigFile() throws IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + Main.APP_NAME + ".config";
	}
	
	private AuthenticatorConfiguration.Builder getConfigFileBuilder(){
		AuthenticatorConfiguration.Builder auth = AuthenticatorConfiguration.newBuilder();
		try{ auth.mergeDelimitedFrom(new FileInputStream(filePath)); }
		catch(Exception e)
		{ 
			e.printStackTrace(); 
			/* file may not exsist because setPaired is the first thing to be called on a new wallet */ 
		}
		return auth;
	}
	
	private void writeConfigFile(AuthenticatorConfiguration.Builder auth) throws IOException{
		FileOutputStream output = new FileOutputStream(filePath);  
		auth.build().writeDelimitedTo(output);          
		output.close();
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
	
	/*public void setOneName(String onename) throws IOException{
		ConfigSettings settings = ProtoConfig.ConfigSettings.newBuilder().setOnename(onename).build();
		FileOutputStream output = new FileOutputStream(filePath);  
		settings.writeTo(output);          
		output.close();
	}
	
	public String getOneName() throws FileNotFoundException, IOException{
		ConfigSettings settings = ConfigSettings.parseFrom(new FileInputStream(filePath));
		String onename = settings.getOnename();
		return onename;
	}*/
	
	public void setPaired(boolean paired) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().setPaired(paired);
		writeConfigFile(auth);
	}
	
	public boolean getPaired() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPaired();
	}
	
	public void fillKeyPool() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		if ( auth.getConfigReceiveAddresses().getWalletKeyCount()<10){
			int num = (10 - auth.getConfigReceiveAddresses().getWalletKeyCount());
			for (int i=0; i<num; i++){
				DeterministicKey newkey = Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
				String pubkey =  KeyUtils.bytesToHex(newkey.getPubKey());
				auth.getConfigReceiveAddressesBuilder().addWalletKey(pubkey);
			}
			writeConfigFile(auth);
		}
	}	
	
	public void addMoreAddresses() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		Main.controller.AddressBox.getItems().remove(Main.controller.AddressBox.getItems().indexOf("                                More"));		
		for (int i=0; i<5; i++){
			DeterministicKey newkey = Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
			String pubkey =  KeyUtils.bytesToHex(newkey.getPubKey());
			auth.getConfigReceiveAddressesBuilder().addWalletKey(pubkey);
			String addr = newkey.toAddress(Main.params).toString();
    		Main.controller.AddressBox.getItems().addAll(addr);
		}
		Main.controller.AddressBox.getItems().addAll("                                More");
		Main.controller.AddressBox.setValue(Main.controller.AddressBox.getItems().get(0));
		writeConfigFile(auth);  
	}	
	
	public void removeAddress(String address) throws FileNotFoundException, IOException {
		System.out.println("rec addr: " + address);
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		for (String hexkey : auth.getConfigReceiveAddresses().getWalletKeyList()){
			ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(hexkey));
			String addr = key.toAddress(Main.params).toString();
			if (!addr.equals(address)){
				keypool.add(key);
			}
		}
		//ConfigReceiveAddresses.Builder ra = ConfigReceiveAddresses.newBuilder();
		auth.getConfigReceiveAddressesBuilder().getWalletKeyList().clear();
		for (ECKey pkeys : keypool){
			String pubkey =  KeyUtils.bytesToHex(pkeys.getPubKey());
			auth.getConfigReceiveAddressesBuilder().addWalletKey(pubkey);
		}
		writeConfigFile(auth);
	}
	
	public ArrayList<ECKey> getKeyPool() throws FileNotFoundException, IOException{
		ArrayList<ECKey> keypool = new ArrayList<ECKey>();
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for (String pubkey : auth.getConfigReceiveAddresses().getWalletKeyList()){
			ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(pubkey));
			keypool.add(key);
		}
		return keypool;
	}
	
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPairedWalletsList();
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
									newPair.setKeysN(0);

		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(newPair.build());
		writeConfigFile(auth);
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	public void addAddressAndPrivateKeyToPairing(String pairID, String privkey, String addr, int index) throws FileNotFoundException, IOException, ParseException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for(PairedAuthenticator o:auth.getConfigAuthenticatorWallet().getPairedWalletsList())
		{
			if(o.getPairingID().equals(pairID)){
				PairedAuthenticator.KeysObject.Builder newKeyObj = PairedAuthenticator.KeysObject.newBuilder();
				newKeyObj.setPrivKey(privkey);
				newKeyObj.setAddress(addr);
				newKeyObj.setIndex(index);
				
				int i = o.getDescriptor().getIndex();
				auth.getConfigAuthenticatorWalletBuilder().getPairedWalletsBuilder(i).addGeneratedKeys(newKeyObj.build());
				writeConfigFile(auth);
				
				break;
			}
		}
		
	}
	
	public List<PendingRequest> getPendingRequests() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPendingRequestsList();
	}
	
	public void writeNewPendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().addPendingRequests(req);
		writeConfigFile(auth);
	}
	
	@SuppressWarnings("static-access")
	public void removePendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().removePendingRequests(req.getDescriptor().getIndex());
		writeConfigFile(auth);
	}
	
	public AuthenticatorConfiguration.ConfigActiveAccount getActiveAccount() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigActiveAccount();
	}
	
	public void writeActiveAccount(AuthenticatorConfiguration.ConfigActiveAccount acc) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.setConfigActiveAccount(acc);
		writeConfigFile(auth);
	}

	public AuthenticatorConfiguration.ConfigOneNameProfile getOnename() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigOneNameProfile();
	}
	
	public void writeOnename(AuthenticatorConfiguration.ConfigOneNameProfile one) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.setConfigOneNameProfile(one);
		writeConfigFile(auth);
	}
}
