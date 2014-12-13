package org.authenticator.db;

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
import org.authenticator.Authenticator;
import org.authenticator.Utils.KeyUtils;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.db.exceptions.CouldNotOpenConfigFileException;
import org.authenticator.db.exceptions.PairingObjectWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import org.authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.SavedTX;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet;
import org.authenticator.protobuf.ProtoSettings.ConfigSettings;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.KeyChain;

import com.google.protobuf.ByteString;

public class walletDB extends dbBase{

	public walletDB(String filePath) throws IOException{
		super(filePath);
	}

	private synchronized AuthenticatorConfiguration.Builder getConfigFileBuilder() {
		AuthenticatorConfiguration.Builder auth = AuthenticatorConfiguration.newBuilder();
		try{ auth.mergeDelimitedFrom(new FileInputStream(filePath)); }
		catch(Exception e)
		{ 
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

	public void initConfigFile() throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.getConfigAuthenticatorWalletBuilder().setPaired(false);
		auth = setDefaultSettings(auth);
		writeConfigFile(auth);
	}
	
	public AuthenticatorConfiguration.Builder setDefaultSettings(AuthenticatorConfiguration.Builder auth) {
		ConfigSettings.Builder b = ConfigSettings.newBuilder();
		auth.setConfigSettings(b);
		return auth;
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

	public void updatePairingGCMRegistrationID(String pairingID, String newGCMRegID) throws FileNotFoundException, IOException, PairingObjectWasNotFoundException {
		List<PairedAuthenticator> all  = getAllPairingObjectArray();
		boolean found = false;
		for(PairedAuthenticator po: all) {
			if(po.getPairingID().equals(pairingID)) {
				removePairingObject(pairingID);
				
				PairedAuthenticator.Builder b = po.toBuilder();
				b.setGCM(newGCMRegID);
				AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		 		auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(b.build());
		 		writeConfigFile(auth);
		 		
		 		found = true;
		 		
				break;
			}
		}
		if(found == false)
			throw new PairingObjectWasNotFoundException("Could not find pairing id: " + pairingID);
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
	public void removePendingRequest(List<PendingRequest> req) throws FileNotFoundException, IOException {
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<PendingRequest> all = getPendingRequests();
		auth.getConfigAuthenticatorWalletBuilder().clearPendingRequests();
		for(PendingRequest pr:all) {
			boolean found = false;
			for(PendingRequest forDelete: req) 
				if(pr.getRequestID().equals(forDelete.getRequestID())) {
					found = true;
					break;
				}
			
			if(!found)
				auth.getConfigAuthenticatorWalletBuilder().addPendingRequests(pr);
		}
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
	
	public void deleteOneNameAvatar() throws IOException {
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth.clearConfigOneNameProfile();
		writeConfigFile(auth);
	}
	
//	public void writeAccountHierarchyPubKey(int accountIdx, DeterministicKey mpubkey) throws AccountWasNotFoundException, IOException{
//		ATAccount acc = getAccount(accountIdx);
//		ATAccount.Builder b = ATAccount.newBuilder(acc);
//		
//		byte[] pubkey = mpubkey.getPubKey();
//		byte[] chaincode = mpubkey.getChainCode();
//		ATAccountHierarchy.Builder hb = ATAccountHierarchy.newBuilder();
//		hb.setHierarchyMasterPublicKey(ByteString.copyFrom(pubkey));
//		hb.setHierarchyChaincode(ByteString.copyFrom(chaincode));
//		
//		b.setAccountHierarchy(hb.build());
//		updateAccount(b.build());
//	}
	
	public ATAccountAddressHierarchy geAccountHierarchy(int accountIdx, HierarchyAddressTypes type) throws AccountWasNotFoundException {
		ATAccount acc = getAccount(accountIdx);
		if(type == HierarchyAddressTypes.External)
			return acc.getAccountExternalHierarchy();
		else
			return acc.getAccountInternalHierarchy();
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
	
	public void resotreSettingsToDefault() throws IOException {
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		auth = this.setDefaultSettings(auth);
		writeConfigFile(auth);
	}
}