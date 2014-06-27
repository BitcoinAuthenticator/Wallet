package authenticator.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.parser.ParseException;

import authenticator.Authenticator;
import authenticator.Utils.KeyUtils;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPrefixedAccountIndex;
import authenticator.protobuf.ProtoConfig;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigReceiveAddresses.CachedAddrees;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.wallet.KeyChain;
import com.google.protobuf.ByteString;

import wallettemplate.Main;

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
	
	public void initConfigFile(byte[] seed) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.setHierarchySeed(ByteString.copyFrom(seed));
		auth.getConfigAuthenticatorWalletBuilder().setPaired(false);
		writeConfigFile(auth);
	}
	
	public void setPaired(boolean paired) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().setPaired(paired);
		writeConfigFile(auth);
	}
	
	public boolean getPaired() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPaired();
	}
	
	public void writeCachedSpendingAddresses(List<CachedAddrees> add) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for(CachedAddrees c: add)
			auth.getConfigReceiveAddressesBuilder().addWalletSpendingAddress(c);
		writeConfigFile(auth);
	}
	
	/*public void fillSpendingKeyPool() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		if ( auth.getConfigReceiveAddresses().getWalletSpendingAddressCount() < 10){
			int num = (10 - auth.getConfigReceiveAddresses().getWalletSpendingAddressCount());
			for (int i=0; i<num; i++){
				DeterministicKey newkey = Authenticator.getWalletOperation().getNextSpendingKey(HierarchyPrefixedAccountIndex.General_VALUE); //TODO - may be also savings 				//Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
				String pubkey =  KeyUtils.bytesToHex(newkey.getPubKey());
				//
				CachedAddrees.Builder b = CachedAddrees.newBuilder();
								      b.setAccountIndex(HierarchyPrefixedAccountIndex.General_VALUE);
									  b.setAddressStr(newkey.toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
									  b.setKeyIndex(newkey.getChildNumber().getI());
									  b.setIsUsed(false);
				auth.getConfigReceiveAddressesBuilder().addWalletSpendingAddress(b.build());
			}
			writeConfigFile(auth);
		}
	}	
	
	public void addMoreSpendingAddresses() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		Main.controller.AddressBox.getItems().remove(Main.controller.AddressBox.getItems().indexOf("                                More"));		
		for (int i=0; i<5; i++){
			DeterministicKey newkey = Authenticator.getWalletOperation().getNextSpendingKey(HierarchyPrefixedAccountIndex.General_VALUE); //TODO - may be also savings 				//Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
			String pubkey =  KeyUtils.bytesToHex(newkey.getPubKey());
			//
			CachedAddrees.Builder b = CachedAddrees.newBuilder();
							      b.setAccountIndex(HierarchyPrefixedAccountIndex.General_VALUE);
								  b.setAddressStr(newkey.toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
								  b.setKeyIndex(newkey.getChildNumber().getI());
								  b.setIsUsed(false);
			auth.getConfigReceiveAddressesBuilder().addWalletSpendingAddress(b.build());
			
			String addr = newkey.toAddress(Main.params).toString();
    		Main.controller.AddressBox.getItems().addAll(addr);
		}
		Main.controller.AddressBox.getItems().addAll("                                More");
		Main.controller.AddressBox.setValue(Main.controller.AddressBox.getItems().get(0));
		writeConfigFile(auth);  
	}*/	
	
	/*public void removeSpendingAddress(String address) throws FileNotFoundException, IOException {
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
	}*/
	
	public void markCachedSpendingAddressAsUsed(String addStr) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for (CachedAddrees c : auth.getConfigReceiveAddresses().getWalletSpendingAddressList()){
			if(c.getAddressStr().equals(addStr))
			{
				CachedAddrees.Builder b = CachedAddrees.newBuilder(c);
				b.setIsUsed(true);
				auth.getConfigReceiveAddressesBuilder().setWalletSpendingAddress(c.getDescriptor().getIndex(), b.build());
				
				break;
			}
		}
		writeConfigFile(auth);
	}
	
	public List<ProtoConfig.AuthenticatorConfiguration.ConfigReceiveAddresses.CachedAddrees> getSpendingAddressFromPool(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigReceiveAddresses().getWalletSpendingAddressList();
	}
	
	public ArrayList<CachedAddrees> getNotUsedSpendingAddressFromPool() throws FileNotFoundException, IOException{
		ArrayList<CachedAddrees> keypool = new ArrayList<CachedAddrees>();
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for (CachedAddrees add : auth.getConfigReceiveAddresses().getWalletSpendingAddressList()){
			if(add.getIsUsed() == false)
				keypool.add(add);
		}
		return keypool;
	}
	
	public ArrayList<String> getSpendingAddressStringPool(ArrayList<CachedAddrees> cache) throws FileNotFoundException, IOException{
		ArrayList<String> keypool = new ArrayList<String>();
		for (CachedAddrees add : cache){
			//ECKey key = ECKey.fromPublicOnly(KeyUtils.hexStringToByteArray(pubkey));
			//keypool.add(key);
			keypool.add(add.getAddressStr());
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
	public void addGeneratedAddressForPairing(String pairID, String addr, int indexWallet, int indexAuth) throws FileNotFoundException, IOException, ParseException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for(PairedAuthenticator o:auth.getConfigAuthenticatorWallet().getPairedWalletsList())
		{
			if(o.getPairingID().equals(pairID)){
				PairedAuthenticator.KeysObject.Builder newKeyObj = PairedAuthenticator.KeysObject.newBuilder();
				newKeyObj.setAddress(addr);
				newKeyObj.setIndexAuth(indexAuth);
				newKeyObj.setIndexWallet(indexWallet);
				
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
	
	public byte[] getHierarchySeed() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getHierarchySeed().toByteArray();
	}
	
	public void writeHierarchySeed(byte[] seed) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.setHierarchySeed(ByteString.copyFrom(seed));
		writeConfigFile(auth);
	}
}
