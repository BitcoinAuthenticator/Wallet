package authenticator;

import hierarchy.BAHierarchy;

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
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigReceiveAddresses.CachedAddrees;

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
			List<Integer> accountByNumberOfKeys = new ArrayList<Integer>();
			List<CachedAddrees> s = getSpendingAddressFromPool();		
			accountByNumberOfKeys.add(s.size());
			
			// 2)	Authenticator paired accounts
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator po:all){
				accountByNumberOfKeys.add(po.getGeneratedKeysCount());
			}
			
			authenticatorWalletHierarchy.buildWalletHierarchyForStartup(accountByNumberOfKeys);
		}
	}
	
	//#####################################
	//
	//	Authenticator Wallet Hierarchy
	//
	//#####################################
	
	/*public void loadHierarchy() throws FileNotFoundException, IOException{
		List<Integer> accountByNumberOfKeys = new ArrayList<Integer>();
		// 1) get Pay-To-Pub-Hash account
		fillKeyPool();
		ArrayList<ECKey> s = getKeyPool();		
		accountByNumberOfKeys.add(s.size());
		
		// 2)	Authenticator paired accounts
		List<PairedAuthenticator> all = getAllPairingObjectArray();
		for(PairedAuthenticator po:all){
			accountByNumberOfKeys.add(po.getGeneratedKeysCount());
		}
		
		authenticatorWalletHierarchy.buildWalletHierarchyForStartup(accountByNumberOfKeys);
	}*/
	
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
	 * @throws AddressFormatException 
	 */
	@SuppressWarnings("static-access")
	// TODO - use addressType
	public String genP2SHAddress(String pairingID, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		try {
			//Derive the child public key from the master public key.
			ArrayList<String> keyandchain = this.getPublicKeyAndChain(pairingID);
			byte[] key = BAUtils.hexStringToByteArray(keyandchain.get(0));
			byte[] chain = BAUtils.hexStringToByteArray(keyandchain.get(1));
			/**
			 * Important, generatring from key number + 1
			 */
			int index = (int) this.getKeyNum(pairingID);
			HDKeyDerivation HDKey = null;
	  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
	  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, index);
	  		byte[] childpublickey = childKey.getPubKey();
	  		//Select network parameters
	  		NetworkParameters params = Authenticator.getWalletOperation().getNetworkParams();
			ECKey childPubKey = new ECKey(null, childpublickey);
			//Create a new key pair for wallet
			//ECKey walletKey = new ECKey();
			//byte[] privkey = walletKey.getPrivKeyBytes();
			DeterministicKey walletHDKey = getNextSpendingKey(this.getAccountIndexForPairing(pairingID), HierarchyAddressTypes.Spending);
			ECKey walletKey = new ECKey(null, walletHDKey.getPubKey()); 
			//Create a 2-of-2 multisig output script.
			List<ECKey> keys = ImmutableList.of(childPubKey, walletKey);
			byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
			Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
			//Create the address
			Address multisigaddr = Address.fromP2SHScript(params, script);
			//Save keys to file
			this.addGeneratedAddressForPairing(pairingID,
					multisigaddr.toString(), 
					this.getAccountIndexForPairing(pairingID),
					index);
			String ret = multisigaddr.toString();
			mWalletWrapper.addAddressToWatch(ret);
			this.LOG.info("Generated a new address: " + ret);
			return ret;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**Builds a raw unsigned transaction*/
	@SuppressWarnings("static-access")
	public Transaction mkUnsignedTx(ArrayList<String> candidateInputAddresses, ArrayList<TransactionOutput>to, Coin fee, DeterministicKey changeAdd) throws AddressFormatException, JSONException, IOException, NoSuchAlgorithmException {
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
		String changeaddr = changeAdd.toAddress(getNetworkParams()).toString(); 
		Address change = new Address(this.mWalletWrapper.getNetworkParameters(), changeaddr);
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
	
	public Transaction signStandardTx(Transaction tx, Map<String,ECKey> keys){
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
	//		 Keys handling
	//
	//#####################################
	
	public int generateNewAccount(){
		return authenticatorWalletHierarchy.generateNewAccount();
	}
	
	public DeterministicKey getNextSpendingKey(int accountI, HierarchyAddressTypes type) throws AddressFormatException{
		DeterministicKey ret = authenticatorWalletHierarchy.getNextSpendingKey(accountI, HierarchyAddressTypes.Spending);
		addAddressToWatch( ret.toAddress(getNetworkParams()).toString() );
		return ret;
	}
	
	public ECKey getECKeyFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		return authenticatorWalletHierarchy.getECKeyFromAcoount(accountIndex, type, addressKey);
	}
	
	public DeterministicKey getKeyFromAcoount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		return authenticatorWalletHierarchy.getKeyFromAcoount(accountIndex, type, addressKey);
	}
	
	public byte[] getHierarchySeed() throws FileNotFoundException, IOException{
		return configFile.getHierarchySeed();
	}
	
	public void writeHierarchySeed(byte[] seed) throws FileNotFoundException, IOException{
		configFile.writeHierarchySeed(seed);
	}
	
	/*public void removeAddressFromPool(String address) throws FileNotFoundException, IOException
	{
		configFile.removeAddress(address);
	}*/
	
	public void markCachedSpendingAddressAsUsed(String addStr) throws IOException{
		configFile.markCachedSpendingAddressAsUsed(addStr);
	}
	
	public void fillSpendingKeyPool() throws FileNotFoundException, IOException, AddressFormatException{
		ArrayList<CachedAddrees> cached = getNotUsedSpendingAddressFromPool();
		ArrayList<CachedAddrees> forWriting = new ArrayList<CachedAddrees>();
		if ( cached.size() < 10){
			int num = (10 - cached.size());
			for (int i=0; i<num; i++){
				DeterministicKey newkey = getNextSpendingKey(HierarchyPrefixedAccountIndex.General_VALUE, HierarchyAddressTypes.Spending); //TODO - may be also savings 				
				//
				CachedAddrees.Builder b = CachedAddrees.newBuilder();
								      b.setAccountIndex(HierarchyPrefixedAccountIndex.General_VALUE);
									  b.setAddressStr(newkey.toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
									  b.setKeyIndex(newkey.getChildNumber().getI());
									  b.setIsUsed(false);
			    forWriting.add(b.build());
			}
		}
		configFile.writeCachedSpendingAddresses(forWriting);
	}
	
	public ArrayList<String> addMoreSpendingAddresses() throws FileNotFoundException, IOException, AddressFormatException{
		ArrayList<CachedAddrees> forWriting = new ArrayList<CachedAddrees>();
		ArrayList<String> ret = new ArrayList<String>();
		for (int i=0; i<5; i++){
			DeterministicKey newkey = getNextSpendingKey(HierarchyPrefixedAccountIndex.General_VALUE, HierarchyAddressTypes.Spending); //TODO - may be also savings 				
			//
			CachedAddrees.Builder b = CachedAddrees.newBuilder();
							      b.setAccountIndex(HierarchyPrefixedAccountIndex.General_VALUE);
								  b.setAddressStr(newkey.toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
								  b.setKeyIndex(newkey.getChildNumber().getI());
								  b.setIsUsed(false);
		    forWriting.add(b.build());
		    ret.add(newkey.toAddress(getNetworkParams()).toString());
		}
		configFile.writeCachedSpendingAddresses(forWriting);
		return ret;
	}
	
	public CachedAddrees getCachedSpendingAddressFromString(String addressStr) throws FileNotFoundException, IOException{
		List<CachedAddrees> all = getSpendingAddressFromPool();
		for(CachedAddrees ca:all){
			if(ca.getAddressStr().equals(addressStr))
				return ca;
		}
		return null;
	}
	
	public ArrayList<String> getNotUsedSpendingAddressStringPool() throws FileNotFoundException, IOException{
		ArrayList<CachedAddrees> all = getNotUsedSpendingAddressFromPool();
		return configFile.getSpendingAddressStringPool(all);
	}
	
	public ArrayList<String> getSpendingAddressStringPool() throws FileNotFoundException, IOException{
		List<CachedAddrees> all = getSpendingAddressFromPool();
		return configFile.getSpendingAddressStringPool(all);
	}
	
	public List<CachedAddrees> getSpendingAddressFromPool() throws FileNotFoundException, IOException{
		return configFile.getSpendingAddressFromPool();
	}
	
	public ArrayList<CachedAddrees> getNotUsedSpendingAddressFromPool() throws FileNotFoundException, IOException{
		return configFile.getNotUsedSpendingAddressFromPool();
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
	
	/**
	 * Various address retrievers 
	 * @param PairID
	 * @return
	 */
	public List<PairedAuthenticator.KeysObject> getKeysArray(String PairID){
		List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
		try {
			all = getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
		for(PairedAuthenticator po: all)
		{
			if(po.getPairingID().equals(PairID))
			{
				return po.getGeneratedKeysList();
			}
		}
		return null;
	}
	
	public ArrayList<String> getAddressesArray(String PairID){
		List<PairedAuthenticator.KeysObject> keys = getKeysArray(PairID);
		ArrayList<String> ret = new ArrayList<String>();
		for(PairedAuthenticator.KeysObject ko: keys)
		{
			ret.add(ko.getAddress());
		}
		return ret;
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
	
	
	//#####################################
	//
	//		 Pairing handling
	//
	//#####################################
	
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
	
	public void addGeneratedAddressForPairing(String pairID, String addr, int indexWallet, int indexAuth) throws FileNotFoundException, IOException, ParseException{
		configFile.addGeneratedAddressForPairing(pairID,  addr, indexWallet, indexAuth);
	}
	
	//#####################################
	//
	//		 Balances handling
	//
	//#####################################
	
	/**
	 * Returns a pending balance for a particular paired authenticator
	 * 
	 * @param pairingID
	 * @return
	 */
	public Coin getUnconfirmedBalance(String pairingID){
		return getPendingBalanceForAddresses(this.getAddressesArray(pairingID));
	}
	
	public Coin getConfirmedBalance(String pairingID) 
	{
		return getConfirmedBalance(this.getAddressesArray(pairingID));
	}
	
	public Coin getUnconfirmedSpendingBalance(){
		try {
			return getPendingBalanceForAddresses(getSpendingAddressStringPool());
		} catch (IOException e) { e.printStackTrace();  }
		return Coin.ZERO;
	}
	
	public Coin getConfirmedSpendingBalance(){
		try {
			return getConfirmedBalance(getSpendingAddressStringPool());
		} catch (IOException e) { e.printStackTrace(); }
		return Coin.ZERO;
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


