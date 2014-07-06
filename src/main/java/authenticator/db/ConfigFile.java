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
	
	/**
	 * 
	 * 
	 * @param networkType - 1 for main net, 0 for testnet
	 * @throws IOException
	 */
	public ConfigFile(String appName) throws IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + appName + ".config";
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
		auth.getConfigHierarchyBuilder().setHierarchySeed(ByteString.copyFrom(seed));
		auth.getConfigAuthenticatorWalletBuilder().setPaired(false);
		auth.getConfigHierarchyBuilder().setHierarchyNextAvailableAccountID(0);
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
		
	public void markAddressAsUsed(int accountIdx, int addIndx, HierarchyAddressTypes type) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		if(type == HierarchyAddressTypes.External)
			b.addUsedExternalKeys(addIndx);
		else
			;
		//writeConfigFile(auth);
		this.updateAccount(b.build());
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
		ATAccount acc = getAccount(accountIndex);
		if(addressType == HierarchyAddressTypes.External)
			return acc.getUsedExternalKeysList().contains(keyIndex);
		return acc.getUsedInternalKeysList().contains(keyIndex);
	}
	
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPairedWalletsList();
	}
	
	/**This method is used during pairing. It saves the data from the Autheticator to file
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	public void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, int accountIndex) throws IOException{
		// Create new pairing item
		PairedAuthenticator.Builder newPair = PairedAuthenticator.newBuilder();
									newPair.setAesKey(key);
									newPair.setMasterPublicKey(mpubkey);
									newPair.setChainCode(chaincode);
									newPair.setGCM(GCM);
									newPair.setPairingID(pairingID);
									//newPair.setPairingName(pairName);
									newPair.setTestnet(false);
									newPair.setKeysN(0);
									newPair.setWalletAccountIndex(accountIndex);

		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(newPair.build());
		writeConfigFile(auth);
	}
	
	public void removePairingObject(String pairingID) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<PairedAuthenticator> all = getAllPairingObjectArray();
		auth.clearConfigAuthenticatorWallet();
		for(PairedAuthenticator po:all)
			if(!po.getPairingID().equals(pairingID))
				auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(po);
		writeConfigFile(auth);
	}
	
	public List<ATAccount> getAllAccounts(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAccountsList();
	}
	
	public ATAccount getAccount(int index){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<ATAccount> all = getAllAccounts();
		// We search the account like this because its id is not necessarly its id in the array
		for(ATAccount acc:all)
			if(acc.getIndex() == index)
				return acc;
		return null;
	}
	
	public void addAccount(ATAccount acc) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.addConfigAccounts(acc);
		writeConfigFile(auth);
	}
	
	public void updateAccount(ATAccount account) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<ATAccount> all = getAllAccounts();
		auth.clearConfigAccounts();
		for(ATAccount acc:all)
			if(acc.getIndex() == account.getIndex())
				auth.addConfigAccounts(account);
			else
				auth.addConfigAccounts(acc);
		
		writeConfigFile(auth);
	}
	
	/**
	 * Remove account from config file, make sure at least one remains
	 * 
	 * @param index
	 * @throws IOException
	 */
	public void removeAccount(int index) throws IOException{
		assert (getAllAccounts().size() > 1);
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<ATAccount> all = getAllAccounts();
		auth.clearConfigAccounts();
		for(ATAccount acc: all)
			if(acc.getIndex() != index)
				auth.addConfigAccounts(acc);
		writeConfigFile(auth);
	}
	
	public void bumpByOneAccountsLastIndex(int accountIndex, HierarchyAddressTypes addressType) throws IOException{
		//AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		ATAccount acc = getAccount(accountIndex);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		if(addressType == HierarchyAddressTypes.External){
			int last = acc.getLastExternalIndex();
			//auth.getConfigAccountsBuilder(accountIndex).setLastExternalIndex(last+1);
			b.setLastExternalIndex(last+1);
		}
		else{
			int last = acc.getLastInternalIndex();
			//auth.getConfigAccountsBuilder(accountIndex).setLastInternalIndex(last+1);
			b.setLastInternalIndex(last+1);
		}		
		//writeConfigFile(auth);
		this.updateAccount(b.build());
	}
	
	public long getConfirmedBalace(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return getAccount(accountIdx).getConfirmedBalance();
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
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.setConfirmedBalance(newBalance);
		updateAccount(b.build());
		return b.build().getConfirmedBalance();
	}
	
	public long getUnConfirmedBalace(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return getAccount(accountIdx).getUnConfirmedBalance();
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
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.setUnConfirmedBalance(newBalance);
		this.updateAccount(b.build());
		return b.build().getUnConfirmedBalance();
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
		return auth.getConfigHierarchy().getHierarchySeed().toByteArray();
	}
	
	public void writeHierarchySeed(byte[] seed) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigHierarchyBuilder().setHierarchySeed(ByteString.copyFrom(seed));
		writeConfigFile(auth);
	}
	
	public List<String> getPendingOutTx(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return getAccount(accountIdx).getPendingOutTxList();
	}
	
	public void addPendingOutTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.addPendingOutTx(txID);
		updateAccount(b.build());
	}
	
	public void removePendingOutTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
		List<String> all = getPendingOutTx(accountIdx);
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.clearPendingOutTx();
		for(int i=0;i < all.size(); i++)
			if(!all.get(i).equals(txID))
				b.addPendingOutTx(all.get(i));
		updateAccount(b.build());
	}
	
	public List<String> getPendingInTx(int accountIdx){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return getAccount(accountIdx).getPendingInTxList();
	}
	
	public void addPendingInTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.addPendingInTx(txID);
		updateAccount(b.build());
	}
	
	public void removePendingInTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
		List<String> all = getPendingInTx(accountIdx);
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.clearPendingInTx();
		for(int i=0;i < all.size(); i++)
			if(!all.get(i).equals(txID))
				b.addPendingInTx(all.get(i));
		updateAccount(b.build());
	}
	
	public int getHierarchyNextAvailableAccountID(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigHierarchy().getHierarchyNextAvailableAccountID();
	}

	public void writeHierarchyNextAvailableAccountID(int i) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigHierarchyBuilder().setHierarchyNextAvailableAccountID(i);
		writeConfigFile(auth);
	}
}
