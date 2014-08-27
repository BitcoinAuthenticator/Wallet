package authenticator.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.json.simple.parser.ParseException;

import authenticator.Authenticator;
import authenticator.Utils.KeyUtils;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.db.exceptions.CouldNotOpenConfigFileException;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.SavedTX;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet;
import authenticator.protobuf.ProtoSettings.ConfigSettings;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.wallet.KeyChain;
import com.google.protobuf.ByteString;

import wallettemplate.Main;

public class walletDB extends dbBase{

	public walletDB(String appName) throws IOException{
		super(appName);
	}

	private synchronized AuthenticatorConfiguration.Builder getConfigFileBuilder() {
		AuthenticatorConfiguration.Builder auth = AuthenticatorConfiguration.newBuilder();
		try{ auth.mergeDelimitedFrom(new FileInputStream(filePath)); }
		catch(Exception e)
		{ 
			//throw new CouldNotOpenConfigFileException("Could not open config file");
			e.printStackTrace();
		}
		
		return auth;
	}

	private synchronized void writeConfigFile(AuthenticatorConfiguration.Builder auth) throws IOException{
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

	public void initConfigFile(DeterministicKey mpubkey) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		byte[] pubkey = mpubkey.getPubKey();
		byte[] chaincode = mpubkey.getChainCode();
		auth.getConfigHierarchyBuilder().setHierarchyMasterPublicKey(ByteString.copyFrom(pubkey));
		auth.getConfigHierarchyBuilder().setHierarchyChaincode(ByteString.copyFrom(chaincode));
		auth.getConfigAuthenticatorWalletBuilder().setPaired(false);
		ConfigSettings.Builder b = ConfigSettings.newBuilder();
		auth.setConfigSettings(b);
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

	public void markAddressAsUsed(int accountIdx, int addIndx, HierarchyAddressTypes type) throws IOException, AccountWasNotFoundException{
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

	public boolean isUsedAddress(int accountIndex, HierarchyAddressTypes addressType, int keyIndex) throws AccountWasNotFoundException{
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
	public PairedAuthenticator writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, int accountIndex) throws IOException{
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
 		return newPair.build();
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

	public ATAccount getAccount(int index) throws AccountWasNotFoundException{
		List<ATAccount> all = getAllAccounts();
		// We search the account like this because its id is not necessarily its id in the array
		for(ATAccount acc:all)
			if(acc.getIndex() == index)
				return acc;
		throw new AccountWasNotFoundException("Could not find account with index " + index);
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

	/*public void bumpByOneAccountsLastIndex(int accountIndex, HierarchyAddressTypes addressType) throws IOException{
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
	}*/

	public long getConfirmedBalace(int accountIdx) throws AccountWasNotFoundException{
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
	 * @throws AccountWasNotFoundException 
	 */
	public long writeConfirmedBalace(int accountIdx, long newBalance) throws IOException, AccountWasNotFoundException{
		ATAccount acc = getAccount(accountIdx);
		ATAccount.Builder b = ATAccount.newBuilder(acc);
		b.setConfirmedBalance(newBalance);
		this.updateAccount(b.build());
		return b.build().getConfirmedBalance();
	}

	public long getUnConfirmedBalace(int accountIdx) throws AccountWasNotFoundException{
		return getAccount(accountIdx).getUnConfirmedBalance();
	}

	/**
	 * updates unconfirmed balance and returns the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 * @throws AccountWasNotFoundException 
	 */
	public long writeUnConfirmedBalace(int accountIdx, long newBalance) throws IOException, AccountWasNotFoundException{
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
	public void removePendingRequest(List<PendingRequest> req) throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<PendingRequest> all = getPendingRequests();
		auth.getConfigAuthenticatorWalletBuilder().clearPendingRequests();
		for(PendingRequest pr:all)
			if(!req.contains(pr))//!pr.getRequestID().equals(req.getRequestID()))
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
	
	public void writeHierarchyPubKey(DeterministicKey mpubkey) throws IOException{
		byte[] pubkey = mpubkey.getPubKey();
		byte[] chaincode = mpubkey.getChainCode();
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigHierarchyBuilder().setHierarchyMasterPublicKey(ByteString.copyFrom(pubkey));
		auth.getConfigHierarchyBuilder().setHierarchyChaincode(ByteString.copyFrom(chaincode));
		writeConfigFile(auth);
	}
	
	public DeterministicKey getHierarchyPubKey(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		byte[] mpubkey = auth.getConfigHierarchy().getHierarchyMasterPublicKey().toByteArray();
		byte[] chaincode = auth.getConfigHierarchy().getHierarchyChaincode().toByteArray();
		DeterministicKey key = HDKeyDerivation.createMasterPubKeyFromBytes(mpubkey, chaincode);
		return key;
	}
	
	public void writeNextSavedTxData(String txid, String toFrom, String description) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		SavedTX.Builder saved = SavedTX.newBuilder();
		saved.setTxid(txid);
		saved.setToFrom(toFrom);
		saved.setDescription(description);
		auth.addConfigSavedTXData(saved);
		writeConfigFile(auth);
	}
	
	public void writeSavedTxData(int x, String txid, String toFrom, String description) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		SavedTX.Builder saved = SavedTX.newBuilder();
		saved.setTxid(txid);
		saved.setToFrom(toFrom);
		saved.setDescription(description);
		auth.getConfigSavedTXDataBuilder(x).setTxid(txid);
		auth.getConfigSavedTXDataBuilder(x).setToFrom(toFrom);
		auth.getConfigSavedTXDataBuilder(x).setDescription(description);
		writeConfigFile(auth);
	}
	
	public ArrayList<String> getSavedTxidList(){
		ArrayList<String> txid = new ArrayList<String>();
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<SavedTX> saved = auth.getConfigSavedTXDataList();
		for (SavedTX tx : saved){
			txid.add(tx.getTxid());
		}
		return txid;
	}
	
	public String getSavedDescription (int index) {
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigSavedTXData(index).getDescription();
	}
	
	public String getSavedToFrom (int index) {
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigSavedTXData(index).getToFrom();
	}

 	/*public List<String> getPendingOutTx(int accountIdx){
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
 	}*/

	/*public int getHierarchyNextAvailableAccountID(){
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigHierarchy().getHierarchyNextAvailableAccountID();
	}

	public void writeHierarchyNextAvailableAccountID(int i) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigHierarchyBuilder().setHierarchyNextAvailableAccountID(i);
		writeConfigFile(auth);
	}*/
}