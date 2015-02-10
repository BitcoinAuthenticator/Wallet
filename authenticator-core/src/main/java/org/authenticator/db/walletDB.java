package org.authenticator.db;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.db.exceptions.PairingObjectWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoSettings;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alonmuroch on 2/10/15.
 */
public class WalletDB extends DBBase {

    public WalletDB() { }

    public WalletDB(String filePath) throws IOException {
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
        ProtoConfig.AuthenticatorConfiguration auth = ProtoConfig.AuthenticatorConfiguration.parseFrom(data);
        writeConfigFile(auth.toBuilder());
    }

    /**
     *
     * @return
     */
    public synchronized ProtoConfig.AuthenticatorConfiguration.Builder getConfigFileBuilder() {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = ProtoConfig.AuthenticatorConfiguration.newBuilder();
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
    public synchronized void writeConfigFile(ProtoConfig.AuthenticatorConfiguration.Builder auth) throws IOException{
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
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.getConfigAuthenticatorWalletBuilder().setPaired(false);
        auth = setDefaultSettings(auth);
        writeConfigFile(auth);
    }

    public ProtoConfig.AuthenticatorConfiguration.Builder setDefaultSettings(ProtoConfig.AuthenticatorConfiguration.Builder auth) {
        ProtoSettings.ConfigSettings.Builder b = ProtoSettings.ConfigSettings.newBuilder();
        auth.setConfigSettings(b);
        return auth;
    }

    public void setPaired(boolean paired) throws IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.getConfigAuthenticatorWalletBuilder().setPaired(paired);
        writeConfigFile(auth);
    }

    public boolean getPaired() throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigAuthenticatorWallet().getPaired();
    }

    public void markAddressAsUsed(int accountIdx, int addIndx, AuthWalletHierarchy.HierarchyAddressTypes type) throws IOException, AccountWasNotFoundException {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        ProtoConfig.ATAccount acc = getAccount(accountIdx);
        ProtoConfig.ATAccount.Builder b = ProtoConfig.ATAccount.newBuilder(acc);
        if(type == AuthWalletHierarchy.HierarchyAddressTypes.External)
            b.addUsedExternalKeys(addIndx);
        else
            ;
        updateAccount(b.build());
    }

    public boolean isUsedAddress(int accountIndex, AuthWalletHierarchy.HierarchyAddressTypes addressType, int keyIndex) throws AccountWasNotFoundException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        ProtoConfig.ATAccount acc = getAccount(accountIndex);
        if(addressType == AuthWalletHierarchy.HierarchyAddressTypes.External)
            return acc.getUsedExternalKeysList().contains(keyIndex);
        return acc.getUsedInternalKeysList().contains(keyIndex);
    }

    /**
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public List<ProtoConfig.PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigAuthenticatorWallet().getPairedWalletsList();
    }

    /**This method is used during pairing. It saves the data from the Autheticator to file
     * @throws IOException */
    @SuppressWarnings("unchecked")
    public ProtoConfig.PairedAuthenticator writePairingData(String mpubkey,
                                                            String chaincode,
                                                            byte[] key,
                                                            String GCM,
                                                            String pairingID,
                                                            int accountIndex,
                                                            boolean isEncrypted,
                                                            byte[] salt) throws IOException{
        // Create new pairing item
        ProtoConfig.PairedAuthenticator.Builder newPair = ProtoConfig.PairedAuthenticator.newBuilder();
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

        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(newPair.build());
        writeConfigFile(auth);
        return newPair.build();
    }

    public void updatePairingGCMRegistrationID(String pairingID, String newGCMRegID) throws FileNotFoundException, IOException, PairingObjectWasNotFoundException {
        List<ProtoConfig.PairedAuthenticator> all  = getAllPairingObjectArray();
        boolean found = false;
        for(ProtoConfig.PairedAuthenticator po: all) {
            if(po.getPairingID().equals(pairingID)) {
                removePairingObject(pairingID);

                ProtoConfig.PairedAuthenticator.Builder b = po.toBuilder();
                b.setGCM(newGCMRegID);
                ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
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
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        List<ProtoConfig.PairedAuthenticator> all = getAllPairingObjectArray();
        auth.clearConfigAuthenticatorWallet();
        for(ProtoConfig.PairedAuthenticator po:all)
            if(!po.getPairingID().equals(pairingID))
                auth.getConfigAuthenticatorWalletBuilder().addPairedWallets(po);
        writeConfigFile(auth);
    }

    public List<ProtoConfig.ATAccount> getAllAccounts(){
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigAccountsList();
    }

    public ProtoConfig.ATAccount getAccount(int index) throws AccountWasNotFoundException{
        List<ProtoConfig.ATAccount> all = getAllAccounts();
        // We search the account like this because its id is not necessarily its id in the array
        for(ProtoConfig.ATAccount acc:all)
            if(acc.getIndex() == index)
                return acc;
        throw new AccountWasNotFoundException("Could not find account with index " + index);
    }

    public void addAccount(ProtoConfig.ATAccount acc) throws IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.addConfigAccounts(acc);
        writeConfigFile(auth);
    }

    public void updateAccount(ProtoConfig.ATAccount account) throws IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        List<ProtoConfig.ATAccount> all = getAllAccounts();
        auth.clearConfigAccounts();
        for(ProtoConfig.ATAccount acc:all)
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
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        List<ProtoConfig.ATAccount> all = getAllAccounts();
        auth.clearConfigAccounts();
        for(ProtoConfig.ATAccount acc: all)
            if(acc.getIndex() != index)
                auth.addConfigAccounts(acc);
        writeConfigFile(auth);
    }

    public long getConfirmedBalace(int accountIdx) throws AccountWasNotFoundException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
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
        ProtoConfig.ATAccount acc = getAccount(accountIdx);
        ProtoConfig.ATAccount.Builder b = ProtoConfig.ATAccount.newBuilder(acc);
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
        ProtoConfig.ATAccount acc = getAccount(accountIdx);
        ProtoConfig.ATAccount.Builder b = ProtoConfig.ATAccount.newBuilder(acc);
        b.setUnConfirmedBalance(newBalance);
        this.updateAccount(b.build());
        return b.build().getUnConfirmedBalance();
    }

    public List<ProtoConfig.PendingRequest> getPendingRequests() throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigAuthenticatorWallet().getPendingRequestsList();
    }

    public void writeNewPendingRequest(ProtoConfig.PendingRequest req) throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.getConfigAuthenticatorWalletBuilder().addPendingRequests(req);
        writeConfigFile(auth);
    }

    public void removePendingRequest(List<ProtoConfig.PendingRequest> req) throws FileNotFoundException, IOException {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        List<ProtoConfig.PendingRequest> all = getPendingRequests();
        auth.getConfigAuthenticatorWalletBuilder().clearPendingRequests();
        for(ProtoConfig.PendingRequest pr:all) {
            boolean found = false;
            for(ProtoConfig.PendingRequest forDelete: req)
                if(pr.getRequestID().equals(forDelete.getRequestID())) {
                    found = true;
                    break;
                }

            if(!found)
                auth.getConfigAuthenticatorWalletBuilder().addPendingRequests(pr);
        }
        writeConfigFile(auth);
    }

    public ProtoConfig.AuthenticatorConfiguration.ConfigActiveAccount getActiveAccount() throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigActiveAccount();
    }

    public void writeActiveAccount(ProtoConfig.AuthenticatorConfiguration.ConfigActiveAccount acc) throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.setConfigActiveAccount(acc);
        writeConfigFile(auth);
    }

    public ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile getOnename() throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigOneNameProfile();
    }

    public void writeOnename(ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile one) throws FileNotFoundException, IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.setConfigOneNameProfile(one);
        writeConfigFile(auth);
    }

    public void deleteOneNameAvatar() throws IOException {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth.clearConfigOneNameProfile();
        writeConfigFile(auth);
    }

    public ProtoConfig.ATAccount.ATAccountAddressHierarchy geAccountHierarchy(int accountIdx, AuthWalletHierarchy.HierarchyAddressTypes type) throws AccountWasNotFoundException {
        ProtoConfig.ATAccount acc = getAccount(accountIdx);
        if(type == AuthWalletHierarchy.HierarchyAddressTypes.External)
            return acc.getAccountExternalHierarchy();
        else
            return acc.getAccountInternalHierarchy();
    }

    public void writeNextSavedTxData(String txid, String toFrom, String description) throws IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        ProtoConfig.AuthenticatorConfiguration.SavedTX.Builder saved = ProtoConfig.AuthenticatorConfiguration.SavedTX.newBuilder();
        saved.setTxid(txid);
        saved.setToFrom(toFrom);
        saved.setDescription(description);
        auth.addConfigSavedTXData(saved);
        writeConfigFile(auth);
    }

    public void writeSavedTxData(int x, String txid, String toFrom, String description) throws IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        ProtoConfig.AuthenticatorConfiguration.SavedTX.Builder saved = ProtoConfig.AuthenticatorConfiguration.SavedTX.newBuilder();
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
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        List<ProtoConfig.AuthenticatorConfiguration.SavedTX> saved = auth.getConfigSavedTXDataList();
        for (ProtoConfig.AuthenticatorConfiguration.SavedTX tx : saved){
            txid.add(tx.getTxid());
        }
        return txid;
    }

    public String getSavedDescription (int index) {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigSavedTXData(index).getDescription();
    }

    public String getSavedToFrom (int index) {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        return auth.getConfigSavedTXData(index).getToFrom();
    }

    public void resotreSettingsToDefault() throws IOException {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
        auth = this.setDefaultSettings(auth);
        writeConfigFile(auth);
    }

    public void addExtension(String id, @Nullable String description, byte[] data) throws IOException {
        Preconditions.checkState((id != null && id.length() > 0), "Extension ID must not be Null or empty");
        Preconditions.checkState((data != null && data.length > 0), "Extension data must not be Null or empty");

        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
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

        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
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

        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
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
