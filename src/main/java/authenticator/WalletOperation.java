package authenticator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import authenticator.db.KeyObject;
import authenticator.db.KeysArray;
import authenticator.db.PairingObject;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;


/**
 * This class is a collection of methods for creating and sending a transaction over to the Authenticator
 */
public class WalletOperation extends BASE{
	
	private static WalletWrapper mWalletWrapper;
	
	static String unsignedTx;
	static Transaction spendtx;
	static Map<String,Integer>  mpNumInputs;
	static Map<String,ArrayList<byte[]>> mpPublickeys;
	static Map<String,ArrayList<Integer>> mpChildkeyindex;
	static Map<String,Boolean> mpTestnet;
	
	public WalletOperation(Wallet wallet, PeerGroup peerGroup) throws IOException{
		super(WalletOperation.class);
		if(mWalletWrapper == null)
			mWalletWrapper = new WalletWrapper(wallet,peerGroup);
		if(mpNumInputs == null)
			mpNumInputs = new HashMap<String,Integer>();
		if(mpPublickeys == null)
			mpPublickeys = new HashMap<String,ArrayList<byte[]>>();
		if(mpChildkeyindex == null)
			mpChildkeyindex = new HashMap<String,ArrayList<Integer>>();
		if(mpChildkeyindex == null)
			mpTestnet = new HashMap<String,Boolean>();
		
		String filePath = BAUtils.getAbsolutePathForFile("wallet.json");//new java.io.File( "." ).getCanonicalPath() + "/wallet.json";
		File f = new File(filePath);
		if(f.exists() && !f.isDirectory()) {
			WalletFile file = new WalletFile();
			try {
				mpTestnet = file.getTestnets();
			} catch (ParseException e) {
				e.printStackTrace();
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
	 * @throws AddressFormatException 
	 */
	@SuppressWarnings("static-access")
	public String genAddress(String pairingID) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		try {
			//Derive the child public key from the master public key.
			WalletFile file = new WalletFile();
			ArrayList<String> keyandchain = file.getPubAndChain(pairingID);
			byte[] key = BAUtils.hexStringToByteArray(keyandchain.get(0));
			byte[] chain = BAUtils.hexStringToByteArray(keyandchain.get(1));
			/**
			 * Important, generatring from key number + 1
			 */
			int index = (int) file.getKeyNum(pairingID);
			HDKeyDerivation HDKey = null;
	  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
	  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, index);
	  		byte[] childpublickey = childKey.getPubKey();
	  		//Select network parameters
	  		NetworkParameters params = Authenticator.getWalletOperation().getNetworkParams();
			ECKey childPubKey = new ECKey(null, childpublickey);
			//Create a new key pair which will kept in the wallet.
			ECKey walletKey = new ECKey();
			byte[] privkey = walletKey.getPrivKeyBytes();
			List<ECKey> keys = ImmutableList.of(childPubKey, walletKey);
			//Create a 2-of-2 multisig output script.
			byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
			Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
			//Create the address
			Address multisigaddr = Address.fromP2SHScript(params, script);
			//Save keys to file
			file.writeToFile(pairingID,BAUtils.bytesToHex(privkey),multisigaddr.toString(),index);
			String ret = multisigaddr.toString();
			mWalletWrapper.addP2ShAddressToWatch(ret);
			return ret;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**Builds a raw unsigned transaction*/
	@SuppressWarnings("static-access")
	public Transaction mktx(String pairingID, ArrayList<TransactionOutput>to) throws AddressFormatException, JSONException, IOException, NoSuchAlgorithmException {
		//Get total output
		BigInteger totalOut = BigInteger.ZERO;
		for (TransactionOutput out:to){
			totalOut = totalOut.add(out.getValue());
		}
		//Get unspent watched addresses of this pairing ID
		ArrayList<TransactionOutput> candidates = this.mWalletWrapper.getUnspentOutputsForAddresses(this.getAddressesArray(pairingID));
		Transaction tx = new Transaction(this.mWalletWrapper.getNetworkParams());
		// Calculate fee
		BigInteger fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
		// Coin selection
		ArrayList<TransactionOutput> outSelected = this.mWalletWrapper.selectOutputs(totalOut.add(fee),candidates);
		// add inputs
		BigInteger inAmount = BigInteger.ZERO;
		for (TransactionOutput input : outSelected){
            tx.addInput(input);
            inAmount = inAmount.add(input.getValue());
		}
		//Add the outputs
		for (TransactionOutput output : to)
            tx.addOutput(output);
		//Add the change
		String changeaddr = genAddress(pairingID);
		Address change = new Address(this.mWalletWrapper.getNetworkParameters(), changeaddr);
		BigInteger rest = inAmount.subtract(totalOut.add(fee));
		if(rest.compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
			TransactionOutput changeOut = new TransactionOutput(this.mWalletWrapper.getNetworkParameters(), null, rest, change);
			tx.addOutput(changeOut);
			this.LOG.info("New Out Tx Sends " + Utils.bitcoinValueToFriendlyString(totalOut) + 
							", Fees " + Utils.bitcoinValueToFriendlyString(fee) + 
							", Rest " + Utils.bitcoinValueToFriendlyString(rest) + 
							". From " + Integer.toString(tx.getInputs().size()) + " Inputs" +
							", To " + Integer.toString(tx.getOutputs().size()) + " Outputs.");
		}	
		else{
			fee=fee.add(rest);
			this.LOG.info("New Out Tx Sends " + Utils.bitcoinValueToFriendlyString(totalOut) + 
					", Fees " + Utils.bitcoinValueToFriendlyString(fee) + 
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
	
	/**
	 * Get unspent balance of a single pairing between the wallet and an authenticator by pair ID.
	 * 
	 * @param pairID
	 * @return the balance as a BigInteger {@link java.math.BigInteger}
	 * @throws ScriptException
	 * @throws UnsupportedEncodingException
	 */
	public BigInteger getBalance(String pairID) throws ScriptException, UnsupportedEncodingException
	{
		return getBalance(getAddressesArray(pairID));
	}
	/**
	 * Returns the balance of the addresses using bitcoinj's wallet and its watched addresses.
	 * 
	 * @return the balance as a BigInteger {@link java.math.BigInteger}
	 * @throws UnsupportedEncodingException 
	 * @throws ScriptException */
	public BigInteger getBalance(ArrayList<String> addresses) throws ScriptException, UnsupportedEncodingException {
		return mWalletWrapper.getBalanceOfWatchedAddresses(addresses);
	}
    
	
	//#####################################
	//
	//		Simple DAL
	//
	//#####################################
	
	public ArrayList<PairingObject> getAllPairingObjectArray()
	{
		WalletFile f = new WalletFile();
		try {
			return f.getPairingObjectsArray();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public PairingObject getPairingObject(String pairID)
	{
		ArrayList<PairingObject> all = getAllPairingObjectArray();
		for(PairingObject po: all)
			if(po.pairingID.equals(pairID))
				return po;
		return null;
	}
	
	public String getAESKey(String pairID)
	{
		ArrayList<PairingObject> all = getAllPairingObjectArray();
		for(PairingObject po: all)
			if(po.pairingID.equals(pairID))
				return po.aes_key;
		return null;
	}
	
	/**
	 * Various address retrievers 
	 * @param PairID
	 * @return
	 */
	public KeysArray getKeysArray(String PairID){
		ArrayList<PairingObject> all = getAllPairingObjectArray();
		for(PairingObject po: all)
		{
			if(po.pairingID.equals(PairID))
			{
				return po.keys;
			}
		}
		return null;
	}
	public ArrayList<String> getAddressesArray(String PairID){
		KeysArray keys = getKeysArray(PairID);
		ArrayList<String> ret = new ArrayList<String>();
		for(KeyObject ko: keys.keys)
		{
			ret.add(ko.address);
		}
		return ret;
	}
	
	//#####################################
	//
	//		Helper functions
	//
	//#####################################    
	/**Defines and object of data that makes up an unspent output*/
    class UnspentOutput {
    	String txid;
    	String index;
    	long amount;
    	
    	public UnspentOutput(String id, String in, long amt){
    		this.txid = id;
    		this.index = in;
    		this.amount = amt;
    	}
    	
    	public String getTxid(){
    		return txid;
    	}
    	
    	public String getIndex(){
    		return index;
    	}
    	
    	public long getAmount(){
    		return amount;
    	}
    }
    
    //#####################################
  	//
  	//	Regular Bitocoin Wallet Operations
  	//
  	//#####################################
    /**
     * Returns the regular (Not Paired authenticator wallet) balance
     * @return
     */
    public static BigInteger getGeneralWalletEstimatedBalance()
    {
    	return mWalletWrapper.getEstimatedBalance();
    }
    
    public NetworkParameters getNetworkParams()
	{
		return mWalletWrapper.getNetworkParams();
	}
    
    public boolean isWatchingAddress(String address) throws AddressFormatException
	{
		return mWalletWrapper.isAuthenticatorAddressWatched(address);
	}
    
    public void addP2ShAddressToWatch(String address) throws AddressFormatException
	{
    	mWalletWrapper.addP2ShAddressToWatch(address);
    	this.LOG.info("Added address to watch: " + address);
	}
	public void addP2ShAddressToWatch(final Address address)
	{
		mWalletWrapper.addWatchedAddress(address);
		this.LOG.info("Added address to watch: " + address.toString());
	}
	
	public ArrayList<TransactionOutput> getUnspentOutputsForAddresses(ArrayList<String> addressArr)
	{
		return mWalletWrapper.getUnspentOutputsForAddresses(addressArr);
	}
}

