package authenticator;

import authenticator.hierarchy.BAHierarchy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import wallettemplate.Main;
import authenticator.Utils.BAUtils;
import authenticator.Utils.KeyUtils;
import authenticator.db.ConfigFile;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPrefixedAccountIndex;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;


/**
 *<p>A super class for handling all wallet operations<br>
 * This class covers DB data retrieving, bitcoinj wallet operations, Authenticator wallet operations<br></p>
 * 
 * <b>Main components are:</b>
 * <ol>
 * <li>{@link authenticator.WalletWrapper} for normal bitcoinj wallet operations</li>
 * <li>Authenticator wallet operations</li>
 * <li>Pending requests control</li>
 * <li>Active account control</li>
 * </ol>
 * @author Alon
 */
public class WalletOperation extends BASE{
	
	private static WalletWrapper mWalletWrapper;
	private static BAHierarchy authenticatorWalletHierarchy;
	public static ConfigFile configFile;
	
	public WalletOperation(Wallet wallet, PeerGroup peerGroup) throws IOException{
		super(WalletOperation.class);
		if(mWalletWrapper == null)
			mWalletWrapper = new WalletWrapper(wallet,peerGroup);
		
		if(configFile == null){
			configFile = new ConfigFile();
			/**
			 * Check to see if a config file exists, if not, initialize
			 */
			if(!configFile.checkConfigFile()){
				byte[] seed = BAHierarchy.generateMnemonicSeed();
				configFile.initConfigFile(seed);
		        //configFile.setPaired(false);
				//configFile.setOneName("NULL");
			}
		}
		if(authenticatorWalletHierarchy == null)
		{
			byte[] seed = configFile.getHierarchySeed();
			authenticatorWalletHierarchy = new BAHierarchy(seed,HierarchyCoinTypes.CoinBitcoin);
			/**
			 * Load num of keys generated in every account to get 
			 * the next fresh key
			 */
			List<Integer[]> accountByNumberOfKeys = new ArrayList<Integer[]>();
			List<ATAccount> allAccount = getAllAccounts();
			for(ATAccount acc:allAccount){
				Integer[] a = new Integer[]{
					acc.getLastExternalIndex(),	
					acc.getLastInternalIndex()
				};
				
				accountByNumberOfKeys.add(a);
			}
			
			authenticatorWalletHierarchy.buildWalletHierarchyForStartup(accountByNumberOfKeys);
		}
	}
	
	//#####################################
	//
	//	Authenticator Wallet Operations
	//
	//#####################################
	
	/**Pushes the raw transaction the the Eligius mining pool
	 * @throws InsufficientMoneyException */
	public SendResult pushTxWithWallet(Transaction tx) throws IOException, InsufficientMoneyException{
		this.LOG.info("Broadcasting to network...");
		return this.mWalletWrapper.broadcastTrabsactionFromWallet(tx);
	}
	
	/**
	 * Derives a child public key from the master public key. Generates a new local key pair.
	 * Uses the two public keys to create a 2of2 multisig address. Saves key and address to json file.
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws AddressFormatException 
	 */
	
	private ATAddress genP2SHAddress(int accountIdx, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		PairedAuthenticator po = getPairingObjectForAccountIndex(accountIdx);
		return genP2SHAddress(po.getPairingID(), addressType);
	}
	@SuppressWarnings("static-access")
	private ATAddress genP2SHAddress(String pairingID, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		try {
			//Derive the child public key from the master public key.
			ArrayList<String> keyandchain = this.getPublicKeyAndChain(pairingID);
			byte[] key = BAUtils.hexStringToByteArray(keyandchain.get(0));
			byte[] chain = BAUtils.hexStringToByteArray(keyandchain.get(1));
			int index = (int) this.getKeyNum(pairingID);
			HDKeyDerivation HDKey = null;
	  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
	  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, index);
	  		byte[] childpublickey = childKey.getPubKey();
	  		
	  		//Select network parameters
	  		NetworkParameters params = Authenticator.getWalletOperation().getNetworkParams();
			ECKey childPubKey = new ECKey(null, childpublickey);
			
			//Create a new key pair for wallet
			DeterministicKey walletHDKey = null;
			int walletAccountIdx = this.getAccountIndexForPairing(pairingID);
			ATAccount walletAccount = getAccount(walletAccountIdx);
			if(addressType == HierarchyAddressTypes.External)
				walletHDKey = getNextExternalKey(walletAccountIdx);
			/*else
				walletHDKey = getNextSavingsKey(this.getAccountIndexForPairing(pairingID));*/
			
			ECKey walletKey = new ECKey(null, walletHDKey.getPubKey()); 
			//Create a 2-of-2 multisig output script.
			List<ECKey> keys = ImmutableList.of(childPubKey, walletKey);
			byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
			Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
			
			//Create the address
			Address multisigaddr = Address.fromP2SHScript(params, script);
			//
			String ret = multisigaddr.toString();
			mWalletWrapper.addAddressToWatch(ret);
			this.LOG.info("Generated a new address: " + ret);
			ATAddress.Builder b = ATAddress.newBuilder();
							  b.setAccountIndex(walletAccountIdx);
							  b.setAddressStr(ret);
							  b.setIsUsed(true);
							  b.setKeyIndex(walletAccount.getLastExternalIndex());
							  b.setType(addressType);
			return b.build();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**Builds a raw unsigned transaction*/
	@SuppressWarnings("static-access")
	public Transaction mkUnsignedTx(ArrayList<String> candidateInputAddresses, ArrayList<TransactionOutput>to, Coin fee, String changeAdd) throws AddressFormatException, JSONException, IOException, NoSuchAlgorithmException {
		//Get total output
		Coin totalOut = Coin.ZERO;
		for (TransactionOutput out:to){
			totalOut = totalOut.add(out.getValue());
		}
		//Check minimum output
		if(totalOut.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0)
			throw new IllegalArgumentException("Tried to send dust with ensureMinRequiredFee set - no way to complete this");
		
		//Get unspent watched addresses of this pairing ID
		ArrayList<TransactionOutput> candidates = this.mWalletWrapper.getUnspentOutputsForAddresses(candidateInputAddresses);
		Transaction tx = new Transaction(this.mWalletWrapper.getNetworkParams());
		// Coin selection
		ArrayList<TransactionOutput> outSelected = this.mWalletWrapper.selectOutputs(totalOut.add(fee), candidates);
		// add inputs
		Coin inAmount = Coin.ZERO;
		for (TransactionOutput input : outSelected){
            tx.addInput(input);
            inAmount = inAmount.add(input.getValue());
		}
		
		//Check in covers the out
		if(inAmount.compareTo(totalOut.add(fee)) < 0)
			throw new IllegalArgumentException("Insufficient funds! You cheap bastard !");
		
		//Add the outputs
		for (TransactionOutput output : to)
            tx.addOutput(output);
		//Add the change
		Address change = new Address(this.mWalletWrapper.getNetworkParameters(), changeAdd);
		Coin rest = inAmount.subtract(totalOut.add(fee));
		if(rest.compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
			TransactionOutput changeOut = new TransactionOutput(this.mWalletWrapper.getNetworkParameters(), null, rest, change);
			tx.addOutput(changeOut);
			this.LOG.info("New Out Tx Sends " + totalOut.toFriendlyString() + 
							", Fees " + fee.toFriendlyString() + 
							", Rest " + rest.toFriendlyString() + 
							". From " + Integer.toString(tx.getInputs().size()) + " Inputs" +
							", To " + Integer.toString(tx.getOutputs().size()) + " Outputs.");
		}	
		else{
			fee = fee.add(rest);
			this.LOG.info("New Out Tx Sends " + totalOut.toFriendlyString() + 
					", Fees " + fee.toFriendlyString() + 
					". From " + Integer.toString(tx.getInputs().size()) + " Inputs" +
					", To " + Integer.toString(tx.getOutputs().size()) + " Outputs.");
		}
		
		
		
		// Check size.
        int size = tx.bitcoinSerialize().length;
        if (size > Transaction.MAX_STANDARD_TX_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Transaction could not be created without exceeding max size: %d vs %d", size,
                        Transaction.MAX_STANDARD_TX_SIZE));
        }
		
		return tx;
	}
	
	
	public Transaction signStandardTxWithKeys(Transaction tx, Map<String,ATAddress> keys){
		Map<String,ECKey> keys2 = new HashMap<String,ECKey> ();
		for(String k:keys.keySet()){
			ECKey addECKey = Authenticator.getWalletOperation().getECKeyFromAccount(keys.get(k).getAccountIndex(), 
					HierarchyAddressTypes.External, 
					keys.get(k).getKeyIndex());
			keys2.put(k, addECKey);
		}
		
		return signStandardTx(tx, keys2);
	}
	private Transaction signStandardTx(Transaction tx, Map<String,ECKey> keys){
		// sign
		for(int index=0;index < tx.getInputs().size(); index++){
			TransactionInput in = tx.getInput(index);
			TransactionOutput connectedOutput = in.getConnectedOutput();
			String addFrom = connectedOutput.getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
			TransactionSignature sig = tx.calculateSignature(index, keys.get(addFrom), 
					connectedOutput.getScriptPubKey(), 
					Transaction.SigHash.ALL, 
					false);
			Script inputScript = ScriptBuilder.createInputScript(sig, keys.get(addFrom));
			in.setScriptSig(inputScript);
			
			try {
				in.getScriptSig().correctlySpends(tx, index, connectedOutput.getScriptPubKey(), false);
			} catch (ScriptException e) {
	            return null;
	        }
		}
		
		return tx;
	}
	
	//#####################################
	//
	// Default spending account pool handling
	//
	// WHY ?
	// All accounts get their keys from the wallet hierarchy.
	// We cache at least 10 external keys from the spending account for performance.
	// Access to those keys is not from the wallet hierarchy but from the cache
	//
	//#####################################
	
	public void fillExternalSpendingKeyPool() throws FileNotFoundException, IOException, AddressFormatException{
		ArrayList<ATAddress> cached = getNotUsedExternalSpendingAddressFromPool();
		ArrayList<ATAddress> forWriting = new ArrayList<ATAddress>();
		if ( cached.size() < 10){
			int num = (10 - cached.size());
			for (int i=0; i<num; i++){
				ATAddress add = getNewSpendingExternalAddress(false);
			    forWriting.add(add);
			}
		}
		configFile.writeCachedExternalSpendingAddresses(forWriting);
	}
	
	public ArrayList<String> addMoreExternalSpendingAddresses() throws FileNotFoundException, IOException, AddressFormatException{
		ArrayList<ATAddress> forWriting = new ArrayList<ATAddress>();
		ArrayList<String> ret = new ArrayList<String>();
		for (int i=0; i<5; i++){
			
			ATAddress add = getNewSpendingExternalAddress(false);
		    forWriting.add(add);
		    ret.add(add.getAddressStr());
		}
		configFile.writeCachedExternalSpendingAddresses(forWriting);
		return ret;
	}
	
	/**
	 * 
	 * 
	 * @param shouldWrite - True to cache the newly created address
	 * @return
	 * @throws AddressFormatException
	 * @throws IOException
	 */
	public ATAddress getNewSpendingExternalAddress(boolean shouldWrite) throws AddressFormatException, IOException{
		DeterministicKey newkey = getNextExternalKey(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE); 				
		//
		ATAddress.Builder b = ATAddress.newBuilder();
						      //b.setAccountIndex(HierarchyPrefixedAccountIndex.General_VALUE);
							  b.setAddressStr(newkey.toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
							  b.setKeyIndex(newkey.getChildNumber().getI());
							 // b.setIsUsed(false);
		if(shouldWrite){
			List<ATAddress> arr = new ArrayList<ATAddress>();
			arr.add(b.build());
			configFile.writeCachedExternalSpendingAddresses(arr);
		}
		return b.build();
	}
	
	/**
	 * 
	 * 
	 * @param shouldWrite - True to cache the newly created address
	 * @return
	 * @throws AddressFormatException
	 * @throws IOException
	 */
	private DeterministicKey getNewSpendingExternalKey(boolean shouldWrite) throws AddressFormatException, IOException{
		DeterministicKey newkey = getNextExternalKey(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE); 				
		if(shouldWrite){
			ATAddress.Builder b = ATAddress.newBuilder();
						      //b.setAccountIndex(HierarchyPrefixedAccountIndex.General_VALUE);
							  b.setAddressStr(newkey.toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
							  b.setKeyIndex(newkey.getChildNumber().getI());
							 // b.setIsUsed(false);
		
			List<ATAddress> arr = new ArrayList<ATAddress>();
			arr.add(b.build());
			configFile.writeCachedExternalSpendingAddresses(arr);
		}
		return newkey;
	}
		
	/**
	 * Get filtered unused cached External addresses
	 * 
	 * @return ArrayList<String> of addresses
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<String> getNotUsedExternalSpendingAddressStringPool() throws FileNotFoundException, IOException{
		ArrayList<ATAddress> all = getNotUsedExternalSpendingAddressFromPool();
		return configFile.getAddressString(all);
	}
	
	/**
	 * Get filtered unused cached External addresses
	 * 
	 * @return ArrayList<CachedAddrees> of addresses
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<ATAddress> getNotUsedExternalSpendingAddressFromPool() throws FileNotFoundException, IOException{
		return configFile.getNotUsedExternalSpendingAddressFromPool();
	}
	
	/**
	 * get all External cached addresses, used and unused.
	 * 
	 * @return ArrayList<String> of addresses
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<String> getExternalSpendingAddressStringPool() throws FileNotFoundException, IOException{
		List<ATAddress> all = getExternalSpendingAddressFromPool();
		return configFile.getAddressString(all);
	}
	
	/**
	 * Get all cached External addresses
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<ATAddress> getExternalSpendingAddressFromPool() throws FileNotFoundException, IOException{
		return configFile.getExternalSpendingAddressFromPool();
	}
	
	/**
	 * Find the corresponding {@link authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigReceiveAddresses.CachedAddrees CachedAddrees}  
	 * cached object from an address string
	 * 
	 * @param addressStr
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ATAddress getCachedAddressFromString(String addressStr) throws FileNotFoundException, IOException{
		List<ATAddress> all = null;
		all = getExternalSpendingAddressFromPool();
		for(ATAddress ca:all){
			if(ca.getAddressStr().equals(addressStr))
				return ca;
		}
		return null;
	}
	
	public void markCachedSpendingAddressAsUsed(String addStr) throws IOException{
		configFile.markCachedExternalSpendingAddressAsUsed(addStr);
	}
    
	//#####################################
	//
	//		 Keys handling
	//
	//#####################################
	
	/**
	 * Generate a new wallet account and writes it to the config file
	 * @return
	 * @throws IOException 
	 */
	public ATAccount generateNewAccount() throws IOException{
		int accoutnIdx = authenticatorWalletHierarchy.generateNewAccount();
		ATAccount.Builder b = ATAccount.newBuilder();
						  b.setIndex(accoutnIdx);
						  b.setLastExternalIndex(0);
						  b.setLastInternalIndex(0);
	    configFile.writeAccount(b.build());
		return b.build();
	}
	
	public ATAddress getNextExternalAddress(int accountI) throws Exception{
		if(accountI == HierarchyPrefixedAccountIndex.PrefixSpending_VALUE ||
				accountI == HierarchyPrefixedAccountIndex.PrefixSavings_VALUE)
			return getNextExternalPayToPubHashAddress(accountI);
		else
			return genP2SHAddress(accountI, HierarchyAddressTypes.External);
	}
	
	private ATAddress getNextExternalPayToPubHashAddress(int accountI) throws Exception{
		DeterministicKey hdKey = getNextExternalKey(accountI);
		return findAddressInAccounts(hdKey.toAddress(getNetworkParams()).toString()); // thats really stupid !!!
	}
	
	private DeterministicKey getNextExternalKey(int accountI) throws AddressFormatException, IOException{
		DeterministicKey ret = authenticatorWalletHierarchy.getNextKey(accountI, HierarchyAddressTypes.External);
		addAddressToWatch( ret.toAddress(getNetworkParams()).toString() );
		// update account
		configFile.bumpByOneAccountsLastIndex(accountI, HierarchyAddressTypes.External);
		return ret;
	}
	
	/*public DeterministicKey getNextInternalKey(int accountI) throws AddressFormatException{
		DeterministicKey ret = authenticatorWalletHierarchy.getNextKey(accountI, HierarchyAddressTypes);
		addAddressToWatch( ret.toAddress(getNetworkParams()).toString() );
		return ret;
	}*/
	
	private ECKey getECKeyFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		return authenticatorWalletHierarchy.getECKeyFromAcoount(accountIndex, type, addressKey);
	}
	
	public DeterministicKey getKeyFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		return authenticatorWalletHierarchy.getKeyFromAcoount(accountIndex, type, addressKey);
	}
	
	public ATAddress findAddressInAccounts(String addressStr) throws Exception{
		List<ATAccount> accounts = getAllAccounts();
		for(ATAccount acc:accounts){
			// check external first
			List<ATAddress> ext = getATAddreessesFromAccount(acc.getIndex(),HierarchyAddressTypes.External);
			for(ATAddress add:ext)
				if(add.getAddressStr().equals(addressStr))
					return add;
			
			// check internal
			/*List<ATAddress> int = getATAddreessesFromAccount(acc.getIndex(),HierarchyAddressTypes);
			for(ATAddress add:int)
				if(add.getAddressStr().equals(addressStr))
					return add;*/
		}
		throw new Exception("Cannot find address in accounts");
	}
	
	public List<ATAddress> getATAddreessesFromAccount(int accountIndex, HierarchyAddressTypes type) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		List<ATAddress> ret = new ArrayList<ATAddress>();
		ATAccount acc = getAccount(accountIndex);
		if(type == HierarchyAddressTypes.External)
			for(int i=0;i <= acc.getLastExternalIndex(); i++){
				ret.add(getATAddreessFromAccount(accountIndex, type, i));
			}
		
		return ret;
	}
	
	public ATAddress getATAddreessFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		DeterministicKey hdKey = getKeyFromAccount(accountIndex,type,addressKey);
		ATAddress.Builder atAdd = ATAddress.newBuilder();
						  atAdd.setAccountIndex(accountIndex);
						  atAdd.setKeyIndex(addressKey);
						  atAdd.setType(type);
						  if(accountIndex == HierarchyPrefixedAccountIndex.PrefixSpending_VALUE || 
								  accountIndex == HierarchyPrefixedAccountIndex.PrefixSavings_VALUE){
								atAdd.setAddressStr(hdKey.toAddress(getNetworkParams()).toString());
							}
							else{
								atAdd.setAddressStr(genP2SHAddress(accountIndex, HierarchyAddressTypes.External).getAddressStr());
							}
		return atAdd.build();
	}
	
	public byte[] getHierarchySeed() throws FileNotFoundException, IOException{
		return configFile.getHierarchySeed();
	}
	
	public void writeHierarchySeed(byte[] seed) throws FileNotFoundException, IOException{
		configFile.writeHierarchySeed(seed);
	}
	
	public PairedAuthenticator getPairingObjectForAccountIndex(int accIdx){
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		for(PairedAuthenticator po: all)
		{
			if(po.getWalletAccountIndex() == accIdx)
			{
				return po;
			}
		}
		return null;
	}
	
	public int getAccountIndexForPairing(String PairID){
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		for(PairedAuthenticator po: all)
		{
			if(po.getPairingID().equals(PairID))
			{
				return po.getWalletAccountIndex();
			}
		}
		return -1;
	}
	
	public List<ATAccount> getAllAccounts(){
		return configFile.getAllAccounts();
	}
	
	public ATAccount getAccount(int index){
		return configFile.getAccount(index);
	} 
	
	public ArrayList<String> getAccountAddressesArray(int accountIndex, HierarchyAddressTypes addressesType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		ArrayList<String> ret = new ArrayList<String>();
		ATAccount account = getAccount(accountIndex);
		if(addressesType == HierarchyAddressTypes.External)
		for(int i=0;i < account.getLastExternalIndex(); i++){
			if(account.getUsedExternalKeysList().contains(i))
				continue;
			
			ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
			ret.add(a.getAddressStr());
		}
		
		return ret;
	}
	
	//#####################################
	//
	//		 Pairing handling
	//
	//#####################################
	
	public ArrayList<String> getPairingAddressesArray(String PairID, HierarchyAddressTypes addressesType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		int accIndex = getAccountIndexForPairing(PairID);
		return getAccountAddressesArray(accIndex,addressesType);
	}
	
	/**Returns the Master Public Key and Chaincode as an ArrayList object */
	public ArrayList<String> getPublicKeyAndChain(String pairingID){
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		
		ArrayList<String> ret = new ArrayList<String>();
		for(PairedAuthenticator o:all)
		{
			if(o.getPairingID().equals(pairingID))
			{
				ret.add(o.getMasterPublicKey());
				ret.add(o.getChainCode());
			}
		}
		return ret;
	}
	
	/**Returns the number of key pairs in the wallet */
	public long getKeyNum(String pairID){
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		for(PairedAuthenticator o:all)
		{
			if(o.getPairingID().equals(pairID))
				return o.getKeysN();
		}
		return 0;
	}
	
	/**Pulls the AES key from file and returns it  */
	public String getAESKey(String pairID) {
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		for(PairedAuthenticator o:all)
		{
			if(o.getPairingID().equals(pairID))
				return o.getAesKey();
		}
		return "";
	}
	
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException
	{
		return configFile.getAllPairingObjectArray();
	}
	
	public PairedAuthenticator getPairingObject(String pairID)
	{
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		for(PairedAuthenticator po: all)
			if(po.getPairingID().equals(pairID))
				return po;
		return null;
	}
	
	public ArrayList<String> getPairingIDs()
	{
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		ArrayList<String> ret = new ArrayList<String>();
		for(PairedAuthenticator o:all)
			ret.add(o.getPairingID());
		return ret;
	}
	
	public void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, String pairName, int accountIndex) throws IOException{
		configFile.writePairingData(mpubkey, chaincode, key, GCM, pairingID, pairName, accountIndex);
	}
	
	/*public void addGeneratedAddressForPairing(String pairID, String addr, int indexWallet, int indexAuth) throws FileNotFoundException, IOException, ParseException{
		configFile.addGeneratedAddressForPairing(pairID,  addr, indexWallet, indexAuth);
	}*/
	
	//#####################################
	//
	//		 Balances handling
	//
	//#####################################
	
	/**
	 * Returns a pending balance for a particular account.<br>
	 * Default general wallet account is 0, any other account is considered an Authenticator account.
	 * 
	 * @param pairingID
	 * @return
	 * @throws AddressFormatException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 */
	public Coin getUnconfirmedBalance(int accountID, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		if(accountID == HierarchyPrefixedAccountIndex.PrefixSpending_VALUE){
			try {
				return getPendingBalanceForAddresses(getExternalSpendingAddressStringPool());
			} catch (IOException e) { e.printStackTrace();  }
			return Coin.ZERO;
		}
		else{
			return getPendingBalanceForAddresses(getAccountAddressesArray(accountID, addressType));
		}
	}
	
	/**
	 * Returns the confirmed balance for a particular account.<br>
	 * Default general wallet account is 0, any other account is considered an Authenticator account.
	 * 
	 * @param pairingID
	 * @return
	 * @throws AddressFormatException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 */
	public Coin getConfirmedBalance(int accountID, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException 
	{
		if(accountID == HierarchyPrefixedAccountIndex.PrefixSpending_VALUE){
			try {
				return getConfirmedBalance(getExternalSpendingAddressStringPool());
			} catch (IOException e) { e.printStackTrace(); }
			return Coin.ZERO;
		}
		else{
			return getConfirmedBalance(getAccountAddressesArray(accountID, addressType));
		}
	}
	
	private Coin getPendingBalanceForAddresses(ArrayList<String> addressArr)
	{
		return mWalletWrapper.getPendingWatchedTransactionsBalacnce(addressArr);
	}
	
	private Coin getConfirmedBalance(ArrayList<String> addressArr)
	{
		Coin estimated = mWalletWrapper.getEstimatedBalanceOfWatchedAddresses(addressArr);
		Coin unconfirmed = getPendingBalanceForAddresses(addressArr);
		return estimated.subtract(unconfirmed);
	}
	
	
	//#####################################
	//
	//		Pending Requests Control
	//
	//#####################################
		
		public static void addPendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
			configFile.writeNewPendingRequest(req);
		}
		
		public static void removePendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
			configFile.removePendingRequest(req);
		}
		
		public static int getPendingRequestSize(){
			try {
				return getPendingRequests().size();
			} catch (FileNotFoundException e) { } catch (IOException e) { }
			return 0;
		}
		
		public static List<PendingRequest> getPendingRequests() throws FileNotFoundException, IOException{
			return configFile.getPendingRequests();
		}
		
	//#####################################
	//
	//		One name
	//
	//#####################################
		
		public AuthenticatorConfiguration.ConfigOneNameProfile getOnename(){
			try {
				AuthenticatorConfiguration.ConfigOneNameProfile on = configFile.getOnename();
				if(on.getOnename().length() == 0)
					return null;
				return on;
			} catch (IOException e) { e.printStackTrace(); }
			return null;
		}
		
		public void writeOnename(AuthenticatorConfiguration.ConfigOneNameProfile one) throws FileNotFoundException, IOException{
			configFile.writeOnename(one);
		}
		
	//#####################################
	//
	//		Active account Control
	//
	//#####################################
		
	public AuthenticatorConfiguration.ConfigActiveAccount getActiveAccount() throws FileNotFoundException, IOException{
		return configFile.getActiveAccount();
	}

	public void writeActiveAccount(AuthenticatorConfiguration.ConfigActiveAccount acc) throws FileNotFoundException, IOException{
		
		configFile.writeActiveAccount(acc);
	}
		
	//#####################################
  	//
  	//	Regular Bitocoin Wallet Operations
  	//
  	//#####################################
    
    public NetworkParameters getNetworkParams()
	{
		return mWalletWrapper.getNetworkParams();
	}
    
    public boolean isWatchingAddress(String address) throws AddressFormatException
	{
		return mWalletWrapper.isAuthenticatorAddressWatched(address);
	}
    
    public boolean isTransactionOutputMine(TransactionOutput out)
	{
		return mWalletWrapper.isTransactionOutputMine(out);
	}
    
    public void addAddressToWatch(String address) throws AddressFormatException
	{
    	mWalletWrapper.addAddressToWatch(address);
    	this.LOG.info("Added address to watch: " + address);
	}
    
	public void connectInputs(List<TransactionInput> inputs)
	{
		LinkedList<TransactionOutput> unspentOutputs = mWalletWrapper.getWatchedOutputs();
		for(TransactionOutput out:unspentOutputs)
			for(TransactionInput in:inputs){
				String hashIn = in.getOutpoint().getHash().toString();
				String hashOut = out.getParentTransaction().getHash().toString();
				if(hashIn.equals(hashOut)){
					in.connect(out);
					break;
				}
			}
	}
	
	public SendResult sendCoins(Wallet.SendRequest req) throws InsufficientMoneyException
	{
		this.LOG.info("Sent Tx: " + req.tx.getHashAsString());
		return mWalletWrapper.sendCoins(req);
	}
	
	public void addEventListener(WalletEventListener listener)
	{
		mWalletWrapper.addEventListener(listener);
	}
	
	public ECKey findKeyFromPubHash(byte[] pubkeyHash){
		return mWalletWrapper.findKeyFromPubHash(pubkeyHash);
	}
	
	public List<Transaction> getRecentTransactions(){
		return mWalletWrapper.getRecentTransactions();
	}
 
}


