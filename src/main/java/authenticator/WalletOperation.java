package authenticator;

import authenticator.hierarchy.BAHierarchy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.slf4j.Logger;

import wallettemplate.Main;
import authenticator.Utils.BAUtils;
import authenticator.db.ConfigFile;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPrefixedAccountIndex;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
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
	private static Logger staticLogger;
	
	public WalletOperation(Wallet wallet, PeerGroup peerGroup) throws IOException{
		super(WalletOperation.class);
		staticLogger = this.LOG;
		if(mWalletWrapper == null){
			mWalletWrapper = new WalletWrapper(wallet,peerGroup);
			mWalletWrapper.addEventListener(new WalletListener());
		}
		
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
	
	/**
	 *
	 * 
	 * @author alon
	 *
	 */
	public class WalletListener extends AbstractWalletEventListener {
		/**
		 * just keep track we don't add the same Tx several times
		 */
        List<String> confirmedTx;
        
        public WalletListener(){
        	confirmedTx = new ArrayList<String>();
        }
		
        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        	try {
				updateBalace(tx, true);
			} catch (Exception e) { e.printStackTrace(); }
        }
        
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        	try {
				updateBalace(tx, true);
			} catch (Exception e) { e.printStackTrace(); }
        }
        
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
        	try {
				updateBalace(tx, false);
			} catch (Exception e) { e.printStackTrace(); }
        }
        
        private void updateBalace(Transaction tx, boolean isNewTx) throws Exception{
        	/**
        	 * 
        	 * Check for coins entering
        	 */
        	List<TransactionOutput> outs = tx.getOutputs();
        	boolean isChanged = false;
        	for (TransactionOutput out : outs){
    			Script scr = out.getScriptPubKey();
    			String addrStr = scr.getToAddress(Main.params).toString();
    			if(Authenticator.getWalletOperation().isWatchingAddress(addrStr)){
    				ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(addrStr);
    				TransactionConfidence conf = tx.getConfidence();
    				switch(conf.getConfidenceType()){
    				case BUILDING:
    					/**
    					 * If the transaction is new but we don't know about it, just add it to confirmed.
    					 * If the transaction is moving from pending to confirmed, make it so.
    					 */
    					if(!isNewTx && Authenticator.getWalletOperation().isPendingTx(add.getAccountIndex(), tx.getHashAsString())){ 
    						moveFundsFromUnconfirmedToConfirmed(add.getAccountIndex(), out.getValue());
    						removePendingTx(add.getAccountIndex(), tx.getHashAsString());
    						confirmedTx.add(tx.getHashAsString());
    						Authenticator.fireOnBalanceChanged(add.getAccountIndex());
    					}
    					else if(isNewTx && !confirmedTx.contains(tx.getHashAsString())){
    						addToConfirmedBalance(add.getAccountIndex(), out.getValue());
    						Authenticator.fireOnBalanceChanged(add.getAccountIndex());
    						markAddressAsUsed(add.getAccountIndex(),add.getKeyIndex(), add.getType());
    					}
    					break;
    				case PENDING:
    					if(!isNewTx)
    						; // do nothing
    					else if(!Authenticator.getWalletOperation().isPendingTx(add.getAccountIndex(), tx.getHashAsString())){
    						addToUnConfirmedBalance(add.getAccountIndex(), out.getValue());
    						addPendingTx(add.getAccountIndex(), tx.getHashAsString());
    						Authenticator.fireOnBalanceChanged(add.getAccountIndex());
    						markAddressAsUsed(add.getAccountIndex(),add.getKeyIndex(), add.getType());
    					}
    					break;
    				case DEAD:
    					// how the fuck do i know from where i should subtract ?!?!
    					break;
    				}
    			}
        	}
        	
        	/**
        	 * 
        	 * Check for coins entering
        	 */
        	if(isNewTx){
        		List<TransactionInput> ins = tx.getInputs();
            	for (TransactionInput in : ins){
            		TransactionOutput out = in.getConnectedOutput();
            		if(out != null) // could be not connected
        			{
            			Script scr = out.getScriptPubKey();
        				String addrStr = scr.getToAddress(Main.params).toString();
            			if(Authenticator.getWalletOperation().isWatchingAddress(addrStr)){
            				ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(addrStr);
            				TransactionConfidence conf = tx.getConfidence();
            				switch(conf.getConfidenceType()){
            				case BUILDING:
            					//Authenticator.getWalletOperation().subtractFromConfirmedBalance(add.getAccountIndex(), out.getValue());
            					break;
            				case PENDING:
            					subtractFromConfirmedBalance(add.getAccountIndex(), out.getValue());
            					Authenticator.fireOnBalanceChanged(add.getAccountIndex());
            					break;
            				case DEAD:
            					//Authenticator.getWalletOperation().addToConfirmedBalance(add.getAccountIndex(), out.getValue());
            					break;
            				}
            			}
        			}            			
            	}
        	}
                		
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
	private ATAddress generateNextP2SHAddress(int accountIdx, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		PairedAuthenticator po = getPairingObjectForAccountIndex(accountIdx);
		return generateNextP2SHAddress(po.getPairingID(), addressType);
	}
	@SuppressWarnings({ "static-access", "deprecation" })
	private ATAddress generateNextP2SHAddress(String pairingID, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		try {
			//Derive the child public key from the master public key.
			ArrayList<String> keyandchain = getPublicKeyAndChain(pairingID);
			byte[] key = BAUtils.hexStringToByteArray(keyandchain.get(0));
			byte[] chain = BAUtils.hexStringToByteArray(keyandchain.get(1));
			int index = (int) this.getKeyNum(pairingID);
			HDKeyDerivation HDKey = null;
	  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
	  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, index);
	  		byte[] childpublickey = childKey.getPubKey();
			ECKey authKey = new ECKey(null, childpublickey);
			
			//Create a new key pair for wallet
			DeterministicKey walletHDKey = null;
			int walletAccountIdx = getAccountIndexForPairing(pairingID);
			ATAccount walletAccount = getAccount(walletAccountIdx);
			if(addressType == HierarchyAddressTypes.External)
				walletHDKey = getNextExternalKey(walletAccountIdx, false);
			/*else
				walletHDKey = getNextSavingsKey(this.getAccountIndexForPairing(pairingID));*/
			ECKey walletKey = new ECKey(walletHDKey.getPrivKeyBytes(), walletHDKey.getPubKey()); 
			
			// generate P2SH
			ATAddress p2shAdd = getP2SHAddress(authKey, walletKey, walletAccount.getLastExternalIndex(), walletAccountIdx, addressType);
			
			addAddressToWatch(p2shAdd.getAddressStr());			
			
			return p2shAdd;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	/**
	 * 
	 * 
	 * @param k1
	 * @param k2
	 * @param indxK - <b>is the same for both keys, make sure they are both HD derived</b>
	 * @param accountIndx
	 * @param addressType
	 * @return
	 */
	private ATAddress getP2SHAddress(ECKey k1, ECKey k2, int indxK, int accountIndx, HierarchyAddressTypes addressType){
		//network params
		NetworkParameters params = getNetworkParams();
		
		//Create a 2-of-2 multisig output script.
		List<ECKey> keys = ImmutableList.of(k1,k2);//childPubKey, walletKey);
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		
		// generate object
		String ret = multisigaddr.toString();
		ATAddress.Builder b = ATAddress.newBuilder();
						  b.setAccountIndex(accountIndx);//walletAccountIdx);
						  b.setAddressStr(ret);
						  b.setIsUsed(true);
						  b.setKeyIndex(indxK);//walletAccount.getLastExternalIndex());
						  b.setType(addressType);
		return b.build();
	}
	
	
	/**
	 * Build a raw unsigned Tx
	 * 
	 * 
	 * @param candidateInputAddresses
	 * @param to
	 * @param fee
	 * @param changeAdd
	 * @return
	 * @throws AddressFormatException
	 * @throws JSONException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
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
		Address change = new Address(getNetworkParams(), changeAdd);
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
	
	
	public Transaction signStandardTxWithAddresses(Transaction tx, Map<String,ATAddress> keys){
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
	
	/**
	 * Make sure we have at least 10 keys ready in the spending (external) account
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws AddressFormatException
	 */
	public void fillExternalSpendingKeyPool() throws FileNotFoundException, IOException, AddressFormatException{
		ArrayList<ATAddress> cached = getNotUsedExternalSpendingAddressFromPool(-1);
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
	
	/**
	 * add additional 5 keys to the external spending account
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws AddressFormatException
	 */
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
		DeterministicKey newkey = getNextExternalKey(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE,true); 				
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
	 * Get filtered unused cached External addresses
	 * 
	 * @param limit - pass -1 for all
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<String> getNotUsedExternalSpendingAddressStringPool(int limit) throws FileNotFoundException, IOException{
		ArrayList<ATAddress> all = getNotUsedExternalSpendingAddressFromPool(limit);
		return configFile.getAddressString(all);
	}
	
	/**
	 * Get filtered unused cached External addresses
	 * 
	 * @param limit - pass -1 for all
	 * @return ArrayList<CachedAddrees> of addresses
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<ATAddress> getNotUsedExternalSpendingAddressFromPool(int limit) throws FileNotFoundException, IOException{
		return configFile.getNotUsedExternalSpendingAddressFromPool(limit);
	}
	
	/**
	 * get all External cached addresses, used and unused.
	 * 
	 * @return ArrayList<String> of addresses
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private ArrayList<String> getExternalSpendingAddressStringPool() throws FileNotFoundException, IOException{
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
	private List<ATAddress> getExternalSpendingAddressFromPool() throws FileNotFoundException, IOException{
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
						  b.setConfirmedBalance(0);
						  b.setUnConfirmedBalance(0);
	    configFile.writeAccount(b.build());
	    staticLogger.info("Generated new account at index, " + accoutnIdx);
		return b.build();
	}
	
	/**
	 * Get the next {@link authenticator.protobuf.ProtoConfig.ATAddress ATAddress} object.<br>
	 * If the account is a <b>standard Pay-To-PubHash</b>, a Pay-To-PubHash address will be returned (prefix 1).<br>
	 *  If the account is a <b>P2SH</b>, a P2SH address will be returned (prefix 3).<br>
	 * <b>The returned {@link authenticator.protobuf.ProtoConfig.ATAddress ATAddress} object was never used or returned from here before.</b>
	 * 
	 * @param accountI
	 * @return
	 * @throws Exception
	 */
	public ATAddress getNextExternalAddress(int accountI) throws Exception{
		if(accountI == HierarchyPrefixedAccountIndex.PrefixSpending_VALUE ||
				accountI == HierarchyPrefixedAccountIndex.PrefixSavings_VALUE)
			return getNextExternalPayToPubHashAddress(accountI,true);
		else
			return generateNextP2SHAddress(accountI, HierarchyAddressTypes.External);
	}
	
	/**
	 * 
	 * @param accountI
	 * @param shouldAddToWatchList
	 * @return
	 * @throws Exception
	 */
	private ATAddress getNextExternalPayToPubHashAddress(int accountI, boolean shouldAddToWatchList) throws Exception{
		DeterministicKey hdKey = getNextExternalKey(accountI,shouldAddToWatchList);
		return findAddressInAccounts(hdKey.toAddress(getNetworkParams()).toString()); // thats really stupid !!!
	}
	
	/**
	 * 
	 * @param accountI
	 * @param shouldAddToWatchList
	 * @return
	 * @throws AddressFormatException
	 * @throws IOException
	 */
	private DeterministicKey getNextExternalKey(int accountI, boolean shouldAddToWatchList) throws AddressFormatException, IOException{
		DeterministicKey ret = authenticatorWalletHierarchy.getNextKey(accountI, HierarchyAddressTypes.External);
		if(shouldAddToWatchList)
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
	
	public ECKey getECKeyFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		DeterministicKey hdKey = getKeyFromAccount(accountIndex, type, addressKey);
		return ECKey.fromPrivate(hdKey.getPrivKeyBytes());
	}
	
	/**
	 * Remains public if external methods need it.
	 * Asserts that the key was created before, If not will throw exception. Use <br>
	 * If the key was never created before, use {@link authenticator.WalletOperation#getNextExternalAddress getNextExternalAddress} instead.
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @return
	 */
	public DeterministicKey getKeyFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		if(type == HierarchyAddressTypes.External)
			assert(addressKey <= getAccount(accountIndex).getLastExternalIndex());
		else
			;// TODO
		return authenticatorWalletHierarchy.getKeyFromAcoount(accountIndex, type, addressKey);
	}
	
	/**
	 * Finds an address in the accounts, will throw exception if not.
	 * 
	 * @param addressStr
	 * @return {@link authenticator.protobuf.ProtoConfig.ATAddress ATAddress}
	 * @throws Exception
	 */
	public ATAddress findAddressInAccounts(String addressStr) throws Exception{
		List<ATAccount> accounts = getAllAccounts();
		for(ATAccount acc:accounts){
			// check external first
			List<ATAddress> ext = getATAddreessesFromAccount(acc.getIndex(),HierarchyAddressTypes.External, -1);
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
	
	/**
	 * get addresses from a particular account and his chain
	 * 
	 * @param accountIndex
	 * @param type
	 * @param limit - pass -1 for all
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 */
	public List<ATAddress> getATAddreessesFromAccount(int accountIndex, HierarchyAddressTypes type, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		List<ATAddress> ret = new ArrayList<ATAddress>();
		ATAccount acc = getAccount(accountIndex);
		if(type == HierarchyAddressTypes.External)
			for(int i=0;i <= Math.min(limit==-1? acc.getLastExternalIndex():limit, acc.getLastExternalIndex()) ; i++){
				ret.add(getATAddreessFromAccount(accountIndex, type, i));
			}
		
		return ret;
	}
	
	/**
	 * Gets a particular address from an account.<br>
	 * Will assert that the address was created before, if not will throw exception.
	 * 
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 */
	@SuppressWarnings("static-access")
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
								PairedAuthenticator  po = getPairingObjectForAccountIndex(accountIndex);
								//Derive the child public key of the auth.
								ArrayList<String> keyandchain = getPublicKeyAndChain(po.getPairingID());
								byte[] key = BAUtils.hexStringToByteArray(keyandchain.get(0));
								byte[] chain = BAUtils.hexStringToByteArray(keyandchain.get(1));
								int index = (int) this.getKeyNum(po.getPairingID());
								HDKeyDerivation HDKey = null;
						  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
						  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, index);
						  		byte[] childpublickey = childKey.getPubKey();
								ECKey authKey = new ECKey(null, childpublickey);
								
								// get wallet key
								ECKey walletKey = getECKeyFromAccount(accountIndex, type, addressKey);
								
								//get address
								ATAddress add = getP2SHAddress(authKey, walletKey, addressKey, accountIndex, type);
								
								atAdd.setAddressStr(add.getAddressStr());
							}
		return atAdd.build();
	}
	
	public byte[] getHierarchySeed() throws FileNotFoundException, IOException{
		return configFile.getHierarchySeed();
	}
	
	public void writeHierarchySeed(byte[] seed) throws FileNotFoundException, IOException{
		configFile.writeHierarchySeed(seed);
	}
	
	public List<ATAccount> getAllAccounts(){
		return configFile.getAllAccounts();
	}
	
	public ATAccount getAccount(int index){
		return configFile.getAccount(index);
	} 
	
	/**
	 * 
	 * @param accountIndex
	 * @param addressesType
	 * @param limit - pass -1 for all
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 */
	public ArrayList<String> getAccountNotUsedAddress(int accountIndex, HierarchyAddressTypes addressesType, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		ArrayList<String> ret = new ArrayList<String>();
		ATAccount account = getAccount(accountIndex);
		if(addressesType == HierarchyAddressTypes.External)
		for(int i=0;i < Math.min(account.getLastExternalIndex(), limit == -1? account.getLastExternalIndex():limit); i++){
			if(account.getUsedExternalKeysList().contains(i))
				continue;
			ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
			ret.add(a.getAddressStr());
		}
		
		return ret;
	}
	
	/**
	 * Returns all addresses from an account in a ArrayList of strings
	 * 
	 * @param accountIndex
	 * @param addressesType
	 * @param limit - pass -1 for all
	 * @return ArrayList of strings
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 */
	public ArrayList<String> getAccountAddresses(int accountIndex, HierarchyAddressTypes addressesType, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		ArrayList<String> ret = new ArrayList<String>();
		ATAccount account = getAccount(accountIndex);
		if(addressesType == HierarchyAddressTypes.External)
		for(int i=0;i < Math.min(account.getLastExternalIndex(), limit == -1? account.getLastExternalIndex():limit); i++){
			ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
			ret.add(a.getAddressStr());
		}
		
		return ret;
	}
	
	public void markAddressAsUsed(int accountIdx, int addIndx, HierarchyAddressTypes type) throws IOException, NoSuchAlgorithmException, JSONException, AddressFormatException{
		configFile.markAddressAsUsed(accountIdx, addIndx,type);
		ATAddress add = getATAddreessFromAccount(accountIdx, type, addIndx);
		this.LOG.info("Marked " + add.getAddressStr() + " as used.");
	}
	
	//#####################################
	//
	//		 Pairing handling
	//
	//#####################################
	
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
	
	/**
	 * Returns all addresses from a pairing in a ArrayList of strings
	 * 
	 * @param accountIndex
	 * @param addressesType
	 * @return ArrayList of strings
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 */
	public ArrayList<String> getPairingAddressesArray(String PairID, HierarchyAddressTypes addressesType, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		int accIndex = getAccountIndexForPairing(PairID);
		return getAccountAddresses(accIndex,addressesType, limit);
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
	
	public void generateNewPairing(String authMpubkey, String authhaincode, String sharedAES, String GCM, String pairingID, String pairName) throws IOException{
		int accountID = generateNewAccount().getIndex();
		writePairingData(authMpubkey,authhaincode,sharedAES,GCM,pairingID,pairName,accountID);
	}
	
	private void writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, String pairName, int accountIndex) throws IOException{
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
	
	public Coin getConfirmedBalance(int accountIdx){
		long balance = configFile.getConfirmedBalace(accountIdx);
		return Coin.valueOf(balance);
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws IOException
	 */
	public Coin addToConfirmedBalance(int accountIdx, Coin amount) throws IOException{
		Coin old = getConfirmedBalance(accountIdx);
		return setConfirmedBalance(accountIdx, old.add(amount));
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws IOException
	 */
	public Coin subtractFromConfirmedBalance(int accountIdx, Coin amount) throws IOException{
		Coin old = getConfirmedBalance(accountIdx);
		assert(old.compareTo(amount) > 0);
		return setConfirmedBalance(accountIdx, old.subtract(amount));
	}
	
	/**
	 * Will return the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 */
	public Coin setConfirmedBalance(int accountIdx, Coin newBalance) throws IOException{
		long balance = configFile.writeConfirmedBalace(accountIdx, newBalance.longValue());
		return Coin.valueOf(balance);
	}
	
	public Coin getUnConfirmedBalance(int accountIdx){
		long balance = configFile.getUnConfirmedBalace(accountIdx);
		return Coin.valueOf(balance);
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws IOException
	 */
	public Coin addToUnConfirmedBalance(int accountIdx, Coin amount) throws IOException{
		Coin old = getUnConfirmedBalance(accountIdx);
		return setUnConfirmedBalance(accountIdx, old.add(amount));
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws IOException
	 */
	public Coin subtractFromUnConfirmedBalance(int accountIdx, Coin amount) throws IOException{
		Coin old = getUnConfirmedBalance(accountIdx);
		assert(old.compareTo(amount) > 0);
		return setUnConfirmedBalance(accountIdx, old.subtract(amount));
	}
	
	/**
	 * Will return the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 */
	public Coin setUnConfirmedBalance(int accountIdx, Coin newBalance) throws IOException{
		long balance = configFile.writeUnConfirmedBalace(accountIdx, newBalance.longValue());
		return Coin.valueOf(balance);
	}
	
	/**
	 * Will return the updated confirmed balance
	 * 
	 * @param accountId
	 * @param amount
	 * @return
	 * @throws IOException 
	 */
	public Coin moveFundsFromUnconfirmedToConfirmed(int accountId,Coin amount) throws IOException{
		Coin beforeConfirmed = getConfirmedBalance(accountId);
		Coin beforeUnconf = getUnConfirmedBalance(accountId);
		assert(beforeUnconf.compareTo(amount) > 0);
		//
		Coin afterConfirmed = beforeConfirmed.add(amount);
		Coin afterUnconfirmed = beforeUnconf.subtract(amount);
		
		setConfirmedBalance(accountId,afterConfirmed);
		setUnConfirmedBalance(accountId,afterUnconfirmed);
		
		return afterConfirmed;
	}
	
	/**
	 * Will return the updated unconfirmed balance
	 * 
	 * @param accountId
	 * @param amount
	 * @return
	 * @throws IOException 
	 */
	public Coin moveFundsFromConfirmedToUnConfirmed(int accountId,Coin amount) throws IOException{
		Coin beforeConfirmed = getConfirmedBalance(accountId);
		Coin beforeUnconf = getUnConfirmedBalance(accountId);
		assert(beforeConfirmed.compareTo(amount) > 0);
		//
		Coin afterConfirmed = beforeConfirmed.subtract(amount);
		Coin afterUnconfirmed = beforeUnconf.add(amount);
		
		setConfirmedBalance(accountId,afterConfirmed);
		setUnConfirmedBalance(accountId,afterUnconfirmed);
		
		return afterUnconfirmed;
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
			staticLogger.info("Removed pending request: " + req.getRequestID());
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
		
		public String pendingRequestToString(PendingRequest op){
			String type = "";
			switch(op.getOperationType()){
				case Pairing:
						type = "Pairing";
					break;
				case Unpair:
						type = "Unpair";
					break;
				case SignAndBroadcastAuthenticatorTx:
						type = "Sign and broadcast Auth. Tx";
					break;
				case BroadcastNormalTx:
						type = "Broadcast normal Tx";	
					break;
				case updateIpAddressesForPreviousMessage:
						type = "Update Ip address from previous message";
					break;
			}
			
			PairedAuthenticator po = getPairingObject(op.getPairingID());
			String pairingName = po.getPairingName();
			
			return pairingName + ": " + type + "  ---  " + op.getRequestID();
		}
		
	//#####################################
	//
	//		Pending transactions 
	//
	//#####################################
		public List<String> getPendingTx(int accountIdx){
			return configFile.getPendingTx(accountIdx);
		}
		
		public void addPendingTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
			configFile.addPendingTx(accountIdx,txID);
		}
		
		public void removePendingTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
			configFile.removePendingTx(accountIdx,txID);
		}
		
		public boolean isPendingTx(int accountIdx, String txID) throws FileNotFoundException, IOException{
			List<String> all = getPendingTx(accountIdx);
			for(String tx:all)
				if(tx.equals(txID))
					return true;
			return false;
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
	
	public void disconnectInputs(List<TransactionInput> inputs){
		for(TransactionInput input:inputs)
			input.disconnect();
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


