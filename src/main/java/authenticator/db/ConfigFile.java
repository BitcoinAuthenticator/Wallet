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
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPrefixedAccountIndex;
import authenticator.protobuf.ProtoConfig;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
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
	
	public void writeCachedExternalSpendingAddresses(List<ATAddress> add) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for(ATAddress c: add)
			auth.getCachedExternalSpendingBuilder().addWalletCachedExternalSpendingAddress(c);
		writeConfigFile(auth);
	}
	
	public void markAddressAsUsed(int accountIdx, int addIndx, HierarchyAddressTypes type) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		if(type == HierarchyAddressTypes.External)
			auth.getConfigAccountsBuilder(accountIdx).addUsedExternalKeys(addIndx);
		else
			;
		writeConfigFile(auth);
	}
	
	public List<ATAddress> getExternalSpendingAddressFromPool(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getCachedExternalSpending().getWalletCachedExternalSpendingAddressList();
	}
	
	public ArrayList<ATAddress> getNotUsedExternalSpendingAddressFromPool(int limit) throws FileNotFoundException, IOException{
		ArrayList<ATAddress> keypool = new ArrayList<ATAddress>();
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		for (ATAddress add : auth.getCachedExternalSpending().getWalletCachedExternalSpendingAddressList()){
			if(!isUsedAddress(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE,HierarchyAddressTypes.External,add.getKeyIndex()))
				keypool.add(add);
			if(keypool.size() == limit && limit != -1)
				break;
		}
		return keypool;
	}	
	
	public ArrayList<String> getAddressString(List<ATAddress> cache) throws FileNotFoundException, IOException{
		ArrayList<String> keypool = new ArrayList<String>();
		for (ATAddress add : cache){
			keypool.add(add.getAddressStr());
		}
		return keypool;
	}
	
	public boolean isUsedAddress(int accountIndex, HierarchyAddressTypes addressType, int keyIndex){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		if(addressType == HierarchyAddressTypes.External)
			return auth.getConfigAccounts(accountIndex).getUsedExternalKeysList().contains(keyIndex);
		return auth.getConfigAccounts(accountIndex).getUsedInternalKeysList().contains(keyIndex);
	}
	
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPairedWalletsList();
	}
	
	/**This method is used during pairing. It saves the data from the Autheticator to file
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	public void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, String pairName, int accountIndex) throws IOException{
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
									newPair.setWalletAccountIndex(accountIndex);

		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(newPair.build());
		writeConfigFile(auth);
	}
	
	/*@SuppressWarnings({ "unchecked", "static-access" })
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
		
	}*/
	
	public List<ATAccount> getAllAccounts(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAccountsList();
	}
	
	public ATAccount getAccount(int index){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAccounts(index);
	}
	
	public void writeAccount(ATAccount acc) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.addConfigAccounts(acc);
		writeConfigFile(auth);
	}
	
	public void bumpByOneAccountsLastIndex(int accountIndex, HierarchyAddressTypes addressType) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		if(addressType == HierarchyAddressTypes.External){
			int last = auth.getConfigAccounts(accountIndex).getLastExternalIndex();
			auth.getConfigAccountsBuilder(accountIndex).setLastExternalIndex(last+1);
		}
		else{
			int last = auth.getConfigAccounts(accountIndex).getLastInternalIndex();
			auth.getConfigAccountsBuilder(accountIndex).setLastInternalIndex(last+1);
		}		
		writeConfigFile(auth);
	}
	
	public long getConfirmedBalace(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAccounts(accountIdx).getConfirmedBalance();
	}
	
	/**
	 * updates confirmed balance and returns the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 */
	public long writeConfirmedBalace(int accountIdx, long newBalance) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAccountsBuilder(accountIdx).setConfirmedBalance(newBalance);
		writeConfigFile(auth);
		return auth.getConfigAccounts(accountIdx).getConfirmedBalance();
	}
	
	public long getUnConfirmedBalace(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAccounts(accountIdx).getUnConfirmedBalance();
	}
	
	/**
	 * updates unconfirmed balance and returns the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 */
	public long writeUnConfirmedBalace(int accountIdx, long newBalance) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAccountsBuilder(accountIdx).setUnConfirmedBalance(newBalance);
		writeConfigFile(auth);
		return auth.getConfigAccounts(accountIdx).getUnConfirmedBalance();
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
		List<PendingRequest> all = getPendingRequests();
		auth.getConfigAuthenticatorWalletBuilder().clearPendingRequests();
		for(PendingRequest pr:all)
			if(!pr.getRequestID().equals(req.getRequestID()))
				auth.getConfigAuthenticatorWalletBuilder().addPendingRequests(pr);
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
	
	public List<String> getPendingTx(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAccounts(accountIdx).getPendingTxList();
	}
	
	public void addPendingTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAccountsBuilder(accountIdx).addPendingTx(txID);
		writeConfigFile(auth);
	}
	
	public void removePendingTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<String> all = getPendingTx(accountIdx);
		auth.getConfigAccountsBuilder(accountIdx).clearPendingTx();
		for(int i=0;i < all.size(); i++)
			if(!all.get(i).equals(txID))
				auth.getConfigAccountsBuilder(accountIdx).addPendingTx(all.get(i));
		writeConfigFile(auth);
	}
}
