package org.authenticator.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.db.exceptions.PairingObjectWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import org.authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.SavedTX;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.protobuf.ProtoSettings.ConfigSettings;

import com.google.protobuf.ByteString;

import javax.annotation.Nullable;

public class WalletDB extends DBBase {

	public WalletDB() { }

	public WalletDB(String filePath) throws IOException{
		super(filePath);
	}

	@Override
	public byte[] dumpToByteArray() {
		return getConfigFileBuilder().build().toByteArray();
	}

	@Override
	public String dumpKey() { return "WalletDb.db.authenticator.org"; }

	@Override
	public void restoreFromBytes(byte[] data) throws IOException {
		AuthenticatorConfiguration auth = AuthenticatorConfiguration.parseFrom(data);
		writeConfigFile(auth.toBuilder());
	}

	/**
	 *
	 * @return
	 */
	public synchronized AuthenticatorConfiguration.Builder getConfigFileBuilder() {
		AuthenticatorConfiguration.Builder auth = AuthenticatorConfiguration.newBuilder();
		try{ auth.mergeDelimitedFrom(new FileInputStream(filePath)); }
		catch(Exception e)
		{ 
			e.printStackTrace();
		}
		
		return auth;
	}

	/**
	 *
	 * @param auth
	 * @throws IOException
	 */
	public synchronized void writeConfigFile(AuthenticatorConfiguration.Builder auth) throws IOException{
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
		updateAccount(b.build());
	}

	public boolean isUsedAddress(int accountIndex, HierarchyAddressTypes addressType, int keyIndex) throws AccountWasNotFoundException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		ATAccount acc = getAccount(accountIndex);
		if(addressType == HierarchyAddressTypes.External)
			return acc.getUsedExternalKeysList().contains(keyIndex);
		return acc.getUsedInternalKeysList().contains(keyIndex);
	}

	/**
	 *
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		return auth.getConfigAuthenticatorWallet().getPairedWalletsList();
	}

	/**This method is used during pairing. It saves the data from the Autheticator to file
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	public PairedAuthenticator writePairingData(String mpubkey,
												String chaincode,
												byte[] key,
												String GCM,
												String pairingID,
												int accountIndex,
												boolean isEncrypted,
												byte[] salt) throws IOException{
 		// Create new pairing item
 		PairedAuthenticator.Builder newPair = PairedAuthenticator.newBuilder();
 									newPair.setAesKey(ByteString.copyFrom(key));
 									newPair.setMasterPublicKey(mpubkey);
 									newPair.setChainCode(chaincode);
 									newPair.setGCM(GCM);
 									newPair.setPairingID(pairingID);
 									newPair.setTestnet(false);
 									newPair.setKeysN(0);
 									newPair.setWalletAccountIndex(accountIndex);
									newPair.setIsEncrypted(isEncrypted);
									newPair.setKeySalt(ByteString.copyFrom(salt));
 
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
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<ATAccount> all = getAllAccounts();
		auth.clearConfigAccounts();
		for(ATAccount acc: all)
			if(acc.getIndex() != index)
				auth.addConfigAccounts(acc);
		writeConfigFile(auth);
	}

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
		updateAccount(b.build());
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

	public void addExtension(String id, @Nullable String description, byte[] data) throws IOException {
		Preconditions.checkState((id != null && id.length() > 0), "Extension ID must not be Null or empty");
		Preconditions.checkState((data != null && data.length > 0), "Extension data must not be Null or empty");

		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		ProtoConfig.Extenstion.Builder ext = ProtoConfig.Extenstion.newBuilder();
		ext.setExtentionID(id);
		if(description != null)
			ext.setDescription(description);
		ext.setData(ByteString.copyFrom(data));
		auth.addConfigExtensions(ext);
		writeConfigFile(auth);
	}

	public void removeExtension(String id) throws IOException {
		Preconditions.checkState((id != null && id.length() > 0), "Extension ID must not be Null or empty");

		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<ProtoConfig.Extenstion> lst = getExtensions();
		auth.clearConfigExtensions();
		for (ProtoConfig.Extenstion ext: lst) {
			if(!ext.getExtentionID().equals(id))
				auth.addConfigExtensions(ext);
		}
		writeConfigFile(auth);
	}

	/**
	 * Will try and update an existing extension, if not found will create a new one.
	 * @param id
	 * @param description
	 * @param data
	 * @throws IOException
	 */
	public void updateExtension(String id, @Nullable String description, byte[] data) throws IOException {
		Preconditions.checkState((id != null && id.length() > 0), "Extension ID must not be Null or empty");
		Preconditions.checkState((data != null && data.length > 0), "Extension data must not be Null or empty");

		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		List<ProtoConfig.Extenstion> lst = getExtensions();
		for (Integer i=0; i< lst.size(); i++) {
			ProtoConfig.Extenstion ext = lst.get(i);
			if(ext.getExtentionID().equals(id)) {
				ProtoConfig.Extenstion.Builder b = ext.toBuilder();
				if(description != null)
					b.setDescription(description);
				else
					b.clearDescription();
				b.setData(ByteString.copyFrom(data));

				auth.setConfigExtensions(i, b);
				writeConfigFile(auth);
				return;
			}
		}

		// not found, than add a new one
		addExtension(id, description, data);
	}

	/**
	 * Will return null if extension not found
	 * @param id
	 * @return
	 */
	public ProtoConfig.Extenstion getExtension(String id) {
		Preconditions.checkState((id != null && id.length() > 0), "Extension ID must not be Null or empty");

		List<ProtoConfig.Extenstion> lst = getExtensions();
		for (ProtoConfig.Extenstion ext: lst) {
			if(ext.getExtentionID().equals(id))
				return ext;
		}
		return null;
	}

	public List<ProtoConfig.Extenstion> getExtensions() {
		return getConfigFileBuilder().getConfigExtensionsList();
	}
}