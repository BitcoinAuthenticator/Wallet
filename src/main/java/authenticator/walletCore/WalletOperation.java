package authenticator.walletCore;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BASE;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.walletCore.exceptions.AddressNotWatchedByWalletException;
import authenticator.walletCore.exceptions.AddressWasNotFoundException;
import authenticator.walletCore.exceptions.CannotBroadcastTransactionException;
import authenticator.walletCore.exceptions.CannotGetAccountFilteredTransactionsException;
import authenticator.walletCore.exceptions.CannotGetAccountUsedAddressesException;
import authenticator.walletCore.exceptions.CannotGetPendingRequestsException;
import authenticator.walletCore.exceptions.CannotRemovePendingRequestException;
import authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import authenticator.walletCore.exceptions.NoWalletPasswordException;
import authenticator.hierarchy.BAHierarchy;
import authenticator.hierarchy.HierarchyUtils;
import authenticator.hierarchy.exceptions.IncorrectPathException;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import authenticator.hierarchy.exceptions.NoUnusedKeyException;
import authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Platform;
import javafx.scene.image.Image;

import javax.annotation.Nullable;

import org.json.JSONException;
import org.slf4j.Logger;

import wallettemplate.Main;
import wallettemplate.ControllerHelpers.ThrottledRunnableExecutor;
import authenticator.Utils.EncodingUtils;
import authenticator.Utils.OneName.exceptions.CannotSetOneNameProfileException;
import authenticator.db.settingsDB;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import authenticator.protobuf.ProtoSettings.BitcoinUnit;
import authenticator.protobuf.ProtoSettings.Languages;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.core.Wallet.ExceededMaxTransactionSize;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.utils.Threading;
import com.google.bitcoin.wallet.CoinSelection;
import com.google.bitcoin.wallet.DefaultCoinSelector;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.collect.ImmutableList;


/**
 *<p>A super class for handling all wallet operations<br>
 * This class covers DB data retrieving, bitcoinj wallet operations, Authenticator wallet operations<br></p>
 * 
 * <b>Main components are:</b>
 * <ol>
 * <li>{@link authenticator.walletCore.WalletWrapper} for normal bitcoinj wallet operations</li>
 * <li>Authenticator wallet operations</li>
 * <li>Pending requests control</li>
 * <li>Active account control</li>
 * </ol>
 * @author Alon
 */
public class WalletOperation extends BASE{
	
	public  WalletWrapper mWalletWrapper;
	private BAHierarchy authenticatorWalletHierarchy;
	private  walletDB configFile;
	private  settingsDB settingsFile;
	private Logger staticLogger;
	private BAOperationState operationalState;
	private BAApplicationParameters AppParams;
	
	public WalletOperation(){ 
		super(WalletOperation.class);
	}
	
	/**
	 * Instantiate WalletOperations without bitcoinj wallet.
	 * 
	 * @param params
	 * @throws IOException
	 */
	public WalletOperation(BAApplicationParameters params, DeterministicKey mpubkey) throws IOException{
		super(WalletOperation.class);
		init(params, mpubkey);
	}
	
	/**
	 * Instantiate WalletOperations with bitcoinj wallet
	 * 
	 * @param wallet
	 * @param peerGroup
	 * @throws IOException
	 */
	public WalletOperation(Wallet wallet, PeerGroup peerGroup, BAApplicationParameters params, DeterministicKey mpubkey) throws IOException{
		super(WalletOperation.class);
		if(mWalletWrapper == null){
			mWalletWrapper = new WalletWrapper(wallet,peerGroup);
			mWalletWrapper.addEventListener(new WalletListener());
		}
		
		peerGroup.addEventListener(new WalletDownloadListener(), Threading.SAME_THREAD);
		
		init(params, mpubkey);
	}
	
	public void dispose(){
		mWalletWrapper = null;
		authenticatorWalletHierarchy = null;
		configFile = null;
		settingsFile = null;
		staticLogger = null;
	}
	
	private void init(BAApplicationParameters params, DeterministicKey mpubkey) throws IOException{
		staticLogger = this.LOG;
		AppParams = params;
		if(configFile == null){
			configFile = new walletDB(params.getAppName());
			/**
			 * Check to see if a config file exists, if not, initialize
			 */
			if(!configFile.checkConfigFile()){
				//byte[] seed = BAHierarchy.generateMnemonicSeed();
				configFile.initConfigFile(mpubkey);
			}
		}
		if(settingsFile == null) {
			settingsFile = new settingsDB(params.getAppName());
		}
		if(authenticatorWalletHierarchy == null)
		{
			//byte[] seed = configFile.getHierarchySeed();
			authenticatorWalletHierarchy = new BAHierarchy(mpubkey,HierarchyCoinTypes.CoinBitcoin);
			/**
			 * Load num of keys generated in every account to get 
			 * the next fresh key
			 */
			List<BAHierarchy.AccountTracker> accountTrackers = new ArrayList<BAHierarchy.AccountTracker>();
			List<ATAccount> allAccount = getAllAccounts();
			for(ATAccount acc:allAccount){
				BAHierarchy.AccountTracker at =   new BAHierarchy().new AccountTracker(acc.getIndex(), 
						BAHierarchy.keyLookAhead,
						acc.getUsedExternalKeysList(), 
						acc.getUsedInternalKeysList());
				
				accountTrackers.add(at);
			}
			
			authenticatorWalletHierarchy.buildWalletHierarchyForStartup(accountTrackers);
		}
		
		/*if(mWalletWrapper != null){
			updateBalaceNonBlocking(mWalletWrapper.getTrackedWallet(), null);
		}*/
		
		setOperationalState(BAOperationState.SYNCING);
	}
	
	public BAApplicationParameters getApplicationParams(){
		return AppParams;
	}
	
	/**
	 *A basic listener to keep track of balances and transaction state.<br>
	 *Will mark addresses as "used" when any amount of bitcoins were transfered to the address.
	 * 
	 * @author alon
	 *
	 */
	public class WalletListener extends AbstractWalletEventListener {
		@Override
		public void onWalletChanged(Wallet wallet) {
			/**
			 * used for confidence change UI update
			 */
			if(getOperationalState() == BAOperationState.READY_AND_OPERATIONAL)
    		updateBalaceNonBlocking(wallet, new Runnable(){
				@Override
				public void run() { 
					notifyBalanceUpdate(wallet,null);
				}
    		});
		}
		
		@Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
			/**
    		 * Notify balance onCoinsSent() only if we don't receive any coins.
    		 * The idea is that if we have a Tx that sends and receives coins to this wallet,
    		 * notify only onCoinsReceived() so we won't send multiple update balance calls.
    		 * 
    		 * If the Tx only sends coins, do update the balance from here.
    		 */
			if(tx.getValueSentToMe(wallet).signum() == 0)
				updateBalaceNonBlocking(wallet, new Runnable(){
					@Override
					public void run() { 
						notifyBalanceUpdate(wallet,tx);
					}
	    		});
		}
		
		@Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
			/**
			 * the {com.google.bitcoin.wallet.DefaultCoinSelector} can only choose candidates if they
			 * originated from the wallet, so this fix is so incoming tx (originated elsewhere)
			 * could be spent if not confirmed	
			 */
			tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
			
			updateBalaceNonBlocking(wallet, new Runnable(){
				@Override
				public void run() { 
					notifyBalanceUpdate(wallet,tx);
				}
    		});
		}        
    }
	
	private void notifyBalanceUpdate(Wallet wallet, Transaction tx){
		if(tx != null){
			if(tx.getValueSentToMe(wallet).signum() > 0){
	    		Authenticator.fireOnBalanceChanged(tx, HowBalanceChanged.ReceivedCoins, tx.getConfidence().getConfidenceType());
	    	}
	    	
	    	if(tx.getValueSentFromMe(wallet).signum() > 0){
	    		Authenticator.fireOnBalanceChanged(tx, HowBalanceChanged.SentCoins, tx.getConfidence().getConfidenceType());
	    	}
		}
		else
			Authenticator.fireOnBalanceChanged(null, null, null);
    }
	
	public void updateBalaceNonBlocking(Wallet wallet, Runnable completionBlock){
		new Thread(){
			@Override
			public void run() {
				try {
					updateBalance(wallet);
					if(completionBlock != null)
						completionBlock.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
    }
	
	/**
	 * Be careful using this method directly because it can block 
	 * @param wallet
	 * @throws Exception
	 */
	private synchronized void updateBalance(Wallet wallet) throws Exception{
		List<ATAccount> accounts = getAllAccounts();
		List<ATAccount.Builder> newBalances = new ArrayList<ATAccount.Builder>();
    	for(ATAccount acc:accounts){
    		ATAccount.Builder b = ATAccount.newBuilder();
			  b.setIndex(acc.getIndex());
			  b.setConfirmedBalance(0);
			  b.setUnConfirmedBalance(0);
			  b.setNetworkType(acc.getNetworkType());
			  b.setAccountName(acc.getAccountName());
			  b.setAccountType(acc.getAccountType());
		  newBalances.add(b);
    	}
    	
    	List<Transaction> allTx = wallet.getRecentTransactions(0, false);
    	Collections.reverse(allTx);
    	for(Transaction tx: allTx){
    		/**
    		 * BUILDING
    		 */
    		if(tx.getConfidence().getConfidenceType() == ConfidenceType.BUILDING){
    			if(tx.getValueSentToMe(wallet).signum() > 0){
    				for (TransactionOutput out : tx.getOutputs()){
    					Script scr = out.getScriptPubKey();
    	    			String addrStr = scr.getToAddress(getNetworkParams()).toString();
    	    			if(isWatchingAddress(addrStr)){
    	    				ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(addrStr);
    	    				markAddressAsUsed(add.getAccountIndex(),add.getKeyIndex(), add.getType());
    	    				
    	    				/**
    	    				 * Add to internal account list
    	    				 */
    	    				for(ATAccount.Builder acc:newBalances)
    	    					if(acc.getIndex() == add.getAccountIndex())
    	    					{
    	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
    	    						acc.setConfirmedBalance(old.add(out.getValue()).longValue());
    	    					}
    	    				//addToConfirmedBalance(add.getAccountIndex(), out.getValue());
    	    			}
    				}
    			}
    			
    			if(tx.getValueSentFromMe(wallet).signum() > 0){
    				for (TransactionInput in : tx.getInputs()){
    					TransactionOutput out = in.getConnectedOutput();
    					if(out != null){
    						Script scr = out.getScriptPubKey();
        	    			String addrStr = scr.getToAddress(getNetworkParams()).toString();
        	    			if(isWatchingAddress(addrStr)){
        	    				ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(addrStr);
        	    				
        	    				/**
        	    				 * Subtract from internal account list
        	    				 */
        	    				for(ATAccount.Builder acc:newBalances)
        	    					if(acc.getIndex() == add.getAccountIndex())
        	    					{
        	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
        	    						acc.setConfirmedBalance(old.subtract(out.getValue()).longValue());
        	    					}
        	    				//subtractFromConfirmedBalance(add.getAccountIndex(), out.getValue());
        	    			}
    					}
    				}
    			}
    		}
    		
    		/**
    		 * PENDING
    		 */
    		if(tx.getConfidence().getConfidenceType() == ConfidenceType.PENDING){
    			if(tx.getValueSentToMe(wallet).signum() > 0){
    				for (TransactionOutput out : tx.getOutputs()){
    					Script scr = out.getScriptPubKey();
    	    			String addrStr = scr.getToAddress(getNetworkParams()).toString();
    	    			if(isWatchingAddress(addrStr)){
    	    				ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(addrStr);
    	    				markAddressAsUsed(add.getAccountIndex(),add.getKeyIndex(), add.getType());
    	    				
    	    				/**
    	    				 * Add to internal account list
    	    				 */
    	    				for(ATAccount.Builder acc:newBalances)
    	    					if(acc.getIndex() == add.getAccountIndex())
    	    					{
    	    						Coin old = Coin.valueOf(acc.getUnConfirmedBalance());
    	    						acc.setUnConfirmedBalance(old.add(out.getValue()).longValue());
    	    					}
    	    				//addToUnConfirmedBalance(add.getAccountIndex(), out.getValue());
    	    			}
    				}
    			}
    			
    			if(tx.getValueSentFromMe(wallet).signum() > 0){
    				for (TransactionInput in : tx.getInputs()){
    					TransactionOutput out = in.getConnectedOutput();
    					if(out != null){
    						Script scr = out.getScriptPubKey();
        	    			String addrStr = scr.getToAddress(getNetworkParams()).toString();
        	    			if(isWatchingAddress(addrStr)){
        	    				ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(addrStr);
        	    				if(out.getParentTransaction().getConfidence().getConfidenceType() == ConfidenceType.PENDING) {
        	    					/**
            	    				 * Subtract from internal account list
            	    				 */
            	    				for(ATAccount.Builder acc:newBalances)
            	    					if(acc.getIndex() == add.getAccountIndex())
            	    					{
            	    						Coin old = Coin.valueOf(acc.getUnConfirmedBalance());
            	    						acc.setUnConfirmedBalance(old.subtract(out.getValue()).longValue());
            	    					}
        	    					
        	    					//subtractFromUnConfirmedBalance(add.getAccountIndex(), out.getValue());
        	    				}
        	    				if(out.getParentTransaction().getConfidence().getConfidenceType() == ConfidenceType.BUILDING) {
        	    					/**
            	    				 * Subtract from internal account list
            	    				 */
            	    				for(ATAccount.Builder acc:newBalances)
            	    					if(acc.getIndex() == add.getAccountIndex())
            	    					{
            	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
            	    						acc.setConfirmedBalance(old.subtract(out.getValue()).longValue());
            	    					}
        	    					//subtractFromConfirmedBalance(add.getAccountIndex(), out.getValue());
        	    				}
        	    			}
    					}
    				}
    			}
    		}
    	}
    	
    	for(ATAccount.Builder acc:newBalances) {
    		setConfirmedBalance(acc.getIndex(), Coin.valueOf(acc.getConfirmedBalance()));
    		setUnConfirmedBalance(acc.getIndex(), Coin.valueOf(acc.getUnConfirmedBalance()));
    	}
	}
	
	
	public class WalletDownloadListener extends DownloadListener {
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
        	if(pct < 1)
        		setOperationalState(BAOperationState.SYNCING);
        }

        @Override
        protected void doneDownload() {
        	setOperationalState(BAOperationState.READY_AND_OPERATIONAL);

        	/**
        	 * run an update of balances after we finished syncing
        	 */
        	updateBalaceNonBlocking(mWalletWrapper.trackedWallet, new Runnable(){
				@Override
				public void run() { 
					notifyBalanceUpdate(mWalletWrapper.trackedWallet,null);
				}
    		});
        }
    }
	
	//#####################################
	//
	//	Authenticator Wallet Operations
	//
	//#####################################
	
	/**Pushes the raw transaction 
	 * @throws CannotBroadcastTransactionException 
	 * @throws InsufficientMoneyException */
	public SendResult pushTxWithWallet(Transaction tx) throws CannotBroadcastTransactionException{
		try {
			this.LOG.info("Broadcasting to network...");
			return mWalletWrapper.broadcastTransactionFromWallet(tx);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new CannotBroadcastTransactionException(e.toString());
		}
	}
	
	/**
	 * Derives a child public key from the master public key. Generates a new local key pair.
	 * Uses the two public keys to create a 2of2 multisig address. Saves key and address to json file.
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws AddressFormatException 
	 * @throws NoAccountCouldBeFoundException 
	 * @throws NoUnusedKeyException 
	 * @throws KeyIndexOutOfRangeException 
	 * @throws IncorrectPathException 
	 */
	private ATAddress generateNextP2SHAddress(int accountIdx, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException, NoUnusedKeyException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException, IncorrectPathException{
		PairedAuthenticator po = getPairingObjectForAccountIndex(accountIdx);
		return generateNextP2SHAddress(po.getPairingID(), addressType);
	}
	@SuppressWarnings({ "deprecation" })
	private ATAddress generateNextP2SHAddress(String pairingID, HierarchyAddressTypes addressType) throws NoSuchAlgorithmException, JSONException, AddressFormatException, NoUnusedKeyException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException, IncorrectPathException{
		try {
			//Create a new key pair for wallet
			DeterministicKey walletHDKey = null;
			int walletAccountIdx = getAccountIndexForPairing(pairingID);
			int keyIndex = -1;
			if(addressType == HierarchyAddressTypes.External){
				walletHDKey = getNextExternalKey(walletAccountIdx, false);
				keyIndex = HierarchyUtils.getKeyIndexFromPath(walletHDKey.getPath()).num();//walletHDKey.getPath().get(walletHDKey.getPath().size() - 1).num();
			}
			/*else
				walletHDKey = getNextSavingsKey(this.getAccountIndexForPairing(pairingID));*/
			ECKey walletKey = null;
			if(!walletHDKey.isPubKeyOnly())
				walletKey = new ECKey(walletHDKey.getPrivKeyBytes(), walletHDKey.getPubKey()); 
			else
				walletKey = new ECKey(null, walletHDKey.getPubKey());
			
			//Derive the child public key from the master public key.
			PairedAuthenticator po = getPairingObject(pairingID);
			ECKey authKey = getPairedAuthenticatorKey(po, keyIndex);
			
			// generate P2SH
			ATAddress p2shAdd = getP2SHAddress(authKey, walletKey, keyIndex, walletAccountIdx, addressType);
			
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
	
	@SuppressWarnings("static-access")
	/**
	 * 
	 * @param tx
	 * @param outSelected
	 * @param to
	 * @param fee
	 * @param changeAdd
	 * @return
	 * @throws AddressFormatException
	 * @throws JSONException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalArgumentException
	 */
	public Transaction mkUnsignedTxWithSelectedInputs(
			ArrayList<TransactionOutput> outSelected, 
			HashMap<String, Coin> to, 
			Coin fee, 
			String changeAdd) throws AddressFormatException, JSONException, IOException, NoSuchAlgorithmException, IllegalArgumentException {
		
		Transaction tx = new Transaction(getNetworkParams());
		ArrayList<TransactionOutput> Outputs = new ArrayList<TransactionOutput>();
		for(String addStr: to.keySet()){
			Address add = new Address(getNetworkParams(), addStr);			
			long satoshis = (long) (Double.parseDouble(to.get(addStr).toPlainString()) * 100000000);
			if (Coin.valueOf(satoshis).compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
				TransactionOutput out = new TransactionOutput(Authenticator.getWalletOperation().getNetworkParams(),
															tx, 
									        				Coin.valueOf(satoshis), 
									        				add);
				Outputs.add(out);
			}
		}
		
		return this.mkUnsignedTxWithSelectedInputs(tx, outSelected, Outputs, fee, changeAdd);
	}
	private Transaction mkUnsignedTxWithSelectedInputs(Transaction tx,
			ArrayList<TransactionOutput> outSelected, 
			ArrayList<TransactionOutput>to, 
			Coin fee, 
			String changeAdd) throws AddressFormatException, JSONException, IOException, NoSuchAlgorithmException, IllegalArgumentException {
		
		tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
		
		Coin totalOut = Coin.ZERO;
		for (TransactionOutput out:to){
			totalOut = totalOut.add(out.getValue());
		}
		//Check minimum output
		if(totalOut.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0)
			throw new IllegalArgumentException("Tried to send dust with ensureMinRequiredFee set - no way to complete this");
		
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
			TransactionOutput changeOut = new TransactionOutput(this.mWalletWrapper.getNetworkParameters(), tx, rest, change);
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
        if (size > Transaction.MAX_STANDARD_TX_SIZE)
            throw new ExceededMaxTransactionSize();
		
		return tx;
	}
	
	/**
	 * If WALLET_PW is null, assumes wallet is not encrypted
	 * 
	 * @param tx
	 * @param keys
	 * @param WALLET_PW
	 * @return
	 * @throws KeyIndexOutOfRangeException
	 * @throws AddressFormatException
	 * @throws AddressNotWatchedByWalletException
	 * @throws NoWalletPasswordException 
	 */
	public Transaction signStandardTxWithAddresses(Transaction tx, 
			Map<String,ATAddress> keys,
			@Nullable BAPassword WALLET_PW) throws KeyIndexOutOfRangeException, AddressFormatException, AddressNotWatchedByWalletException, NoWalletPasswordException{
		Map<String,ECKey> keys2 = new HashMap<String,ECKey> ();
		for(String k:keys.keySet()){
			ECKey addECKey = getPrivECKeyFromAccount(keys.get(k).getAccountIndex(), 
					HierarchyAddressTypes.External, 
					keys.get(k).getKeyIndex(),
					WALLET_PW,
					true);
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
	//		 Hierarchy General
	//
	//#####################################
	
	public void setHierarchyKeyLookAhead(int value){
		authenticatorWalletHierarchy.setKeyLookAhead(value);
		staticLogger.info("Set hierarchy key look ahead value to {}", value);
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
 	private ATAccount generateNewAccount(NetworkType nt, String accountName, WalletAccountType type) throws IOException{
 		int accoutnIdx = authenticatorWalletHierarchy.generateNewAccount().getAccountIndex();
 		ATAccount b = completeAccountObject(nt, accoutnIdx, accountName, type);
		//writeHierarchyNextAvailableAccountID(accoutnIdx + 1); // update 
		addNewAccountToConfigAndHierarchy(b);
 		return b;
 	}
 	
 	/**
 	 * Giving the necessary params, will return a complete {@link authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount ATAccount} object
 	 * 
 	 * @param nt
 	 * @param accoutnIdx
 	 * @param accountName
 	 * @param type
 	 * @return
 	 */
 	public ATAccount completeAccountObject(NetworkType nt, int accoutnIdx, String accountName, WalletAccountType type){
 		ATAccount.Builder b = ATAccount.newBuilder();
						  b.setIndex(accoutnIdx);
						  b.setConfirmedBalance(0);
						  b.setUnConfirmedBalance(0);
						  b.setNetworkType(nt.getValue());
						  b.setAccountName(accountName);
						  b.setAccountType(type);
		return b.build();
 	}
 	
 	/**
 	 * Register an account in the config file. Should be used whenever a new account is created
 	 * @param b
 	 * @throws IOException
 	 */
 	public void addNewAccountToConfigAndHierarchy(ATAccount b) throws IOException{
 		configFile.addAccount(b);
 	    staticLogger.info("Generated new account at index, " + b.getIndex());
 	    authenticatorWalletHierarchy.addAccountToTracker(b.getIndex(), BAHierarchy.keyLookAhead);
		staticLogger.info("Added an account at index, " + b.getIndex() + " to hierarchy");
 	}
 	
 	public ATAccount generateNewStandardAccount(NetworkType nt, String accountName) throws IOException{
		ATAccount ret = generateNewAccount(nt, accountName, WalletAccountType.StandardAccount);
		Authenticator.fireOnNewStandardAccountAdded();
		return ret;
	}

	
	/**
	 * Get the next {@link authenticator.protobuf.ProtoConfig.ATAddress ATAddress} object that is not been used, <b>it may been seen already</b><br>
	 * If the account is a <b>standard Pay-To-PubHash</b>, a Pay-To-PubHash address will be returned (prefix 1).<br>
	 *  If the account is a <b>P2SH</b>, a P2SH address will be returned (prefix 3).<br>
	 * 
	 * @param accountI
	 * @return
	 * @throws Exception
	 */
	public ATAddress getNextExternalAddress(int accountI) throws Exception{
		ATAccount acc = getAccount(accountI);
		if(acc.getAccountType() == WalletAccountType.StandardAccount)
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
		ATAddress ret = getATAddreessFromAccount(accountI, HierarchyAddressTypes.External, HierarchyUtils.getKeyIndexFromPath(hdKey.getPath()).num());
		return ret;
	}
	
	/**
	 * 
	 * @param accountI
	 * @param shouldAddToWatchList
	 * @return
	 * @throws AddressFormatException
	 * @throws IOException
	 * @throws NoAccountCouldBeFoundException 
	 * @throws NoUnusedKeyException 
	 * @throws KeyIndexOutOfRangeException 
	 */
	private DeterministicKey getNextExternalKey(int accountI, boolean shouldAddToWatchList) throws AddressFormatException, IOException, NoUnusedKeyException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException{
		DeterministicKey ret = authenticatorWalletHierarchy.getNextPubKey(accountI, HierarchyAddressTypes.External);
		if(shouldAddToWatchList)
			addAddressToWatch( ret.toAddress(getNetworkParams()).toString() );
		return ret;
	}
	
	/**
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @param WALLET_PW
	 * @param iKnowAddressFromKeyIsNotWatched
	 * @return
	 * @throws KeyIndexOutOfRangeException
	 * @throws AddressFormatException
	 * @throws AddressNotWatchedByWalletException
	 * @throws NoWalletPasswordException
	 */
	public ECKey getPrivECKeyFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey,
			@Nullable BAPassword WALLET_PW,
			boolean iKnowAddressFromKeyIsNotWatched) throws KeyIndexOutOfRangeException, AddressFormatException, AddressNotWatchedByWalletException, NoWalletPasswordException{
		DeterministicKey ret = getPrivKeyFromAccount(accountIndex,
				type,
				addressKey,
				WALLET_PW,
				iKnowAddressFromKeyIsNotWatched);
		return new ECKey(ret.getPrivKeyBytes(), ret.getPubKey());
	}
	
	/**
	 * <b>The method remains public if any external method need it.</b><br>
	 * If u don't know if the corresponding Pay-To-PubHash address is watched, will throw exception. <br>
	 * If the key is part of a P2SH address, pass false for iKnowAddressFromKeyIsNotWatched<br>
	 * If the key was never created before, use {@link authenticator.walletCore.WalletOperation#getNextExternalAddress getNextExternalAddress} instead.
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @param WALLET_PW
	 * @param iKnowAddressFromKeyIsNotWatched
	 * @return
	 * @throws KeyIndexOutOfRangeException
	 * @throws AddressFormatException
	 * @throws AddressNotWatchedByWalletException
	 * @throws NoWalletPasswordException 
	 */
	public DeterministicKey getPrivKeyFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey, 
			@Nullable BAPassword WALLET_PW,
			boolean iKnowAddressFromKeyIsNotWatched) throws KeyIndexOutOfRangeException, AddressFormatException, AddressNotWatchedByWalletException, NoWalletPasswordException{
		byte[] seed = getWalletSeedBytes(WALLET_PW);
		DeterministicKey ret = authenticatorWalletHierarchy.getPrivKeyFromAccount(seed, accountIndex, type, addressKey);
		if(!iKnowAddressFromKeyIsNotWatched && !isWatchingAddress(ret.toAddress(getNetworkParams())))
			throw new AddressNotWatchedByWalletException("You are trying to get an unwatched address");
		return ret;
	}
	
	public DeterministicKey getPubKeyFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey, 
			boolean iKnowAddressFromKeyIsNotWatched) throws KeyIndexOutOfRangeException, AddressFormatException, AddressNotWatchedByWalletException{
		DeterministicKey ret = authenticatorWalletHierarchy.getPubKeyFromAccount(accountIndex, type, addressKey);
		if(!iKnowAddressFromKeyIsNotWatched && !isWatchingAddress(ret.toAddress(getNetworkParams())))
			throw new AddressNotWatchedByWalletException("You are trying to get an unwatched address");
		return ret;
	}
	
	/**
	 * <b>WARNING</b> - This is a very costly operation !!<br> 
	 * Finds an address in the accounts, will throw exception if not.<br>
	 * Will only search for external address cause its reasonable that only they will be needing search with only the address string.<br>
	 * <br>
	 * <b>Assumes the address is already watched by the wallet</b>
	 * 
	 * @param addressStr
	 * @return {@link authenticator.protobuf.ProtoConfig.ATAddress ATAddress}
	 * @throws AddressWasNotFoundException
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AddressFormatException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws AccountWasNotFoundException 
	 */
	public ATAddress findAddressInAccounts(String addressStr) throws AddressWasNotFoundException, NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AccountWasNotFoundException{
		if(!isWatchingAddress(addressStr))
			throw new AddressWasNotFoundException("Cannot find address in accounts");
		List<ATAccount> accounts = getAllAccounts();
		int gapLookAhead = 30;
		while(gapLookAhead < 10000) // just arbitrary number, TODO - this is very stupid !!
		{
			for(ATAccount acc:accounts){
				for(int i = gapLookAhead - 30 ; i < gapLookAhead; i++)
				{
					try{
						ATAddress add = getATAddreessFromAccount(acc.getIndex(), HierarchyAddressTypes.External, i);
						if(add.getAddressStr().equals(addressStr))
							return add;
					}
					catch (AddressNotWatchedByWalletException e) {
						break; // address is not watched which means we reached the end on the generated addresses
					}
				}
			}
			gapLookAhead += 30;
		}
		throw new AddressWasNotFoundException("Cannot find address in accounts");
	}
	
	/**
	 * get addresses from a particular account and his chain
	 * 
	 * @param accountIndex
	 * @param type
	 * @param limit
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AddressNotWatchedByWalletException 
	 * @throws AccountWasNotFoundException 
	 */
	public List<ATAddress> getATAddreessesFromAccount(int accountIndex, HierarchyAddressTypes type,int standOff, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException, AccountWasNotFoundException{
		List<ATAddress> ret = new ArrayList<ATAddress>();
		if(type == HierarchyAddressTypes.External)
			for(int i = standOff;i <= limit; i++)//Math.min(limit==-1? acc.getLastExternalIndex():limit, acc.getLastExternalIndex()) ; i++){
			{
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
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AddressNotWatchedByWalletException 
	 * @throws AccountWasNotFoundException 
	 */
	@SuppressWarnings("static-access")
	public ATAddress getATAddreessFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey) throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException, AccountWasNotFoundException{		
		ATAccount acc = getAccount(accountIndex);
		ATAddress.Builder atAdd = ATAddress.newBuilder();
						  atAdd.setAccountIndex(accountIndex);
						  atAdd.setKeyIndex(addressKey);
						  atAdd.setType(type);
						  /**
						   * Standard Pay-To-PubHash
						   */
						  if(acc.getAccountType() == WalletAccountType.StandardAccount){
							  //TODO - THIS LINE THROWS A NULLPOINTER EXCEPTION DUE TO CHANGE IN HIERARCHY
							  DeterministicKey hdKey = getPubKeyFromAccount(accountIndex,type,addressKey, false);
							  atAdd.setAddressStr(hdKey.toAddress(getNetworkParams()).toString());
						  }
						  else{
							  /**
							   * P2SH
							   */
							PairedAuthenticator  po = getPairingObjectForAccountIndex(accountIndex);
							
							// Auth key
							ECKey authKey = getPairedAuthenticatorKey(po, addressKey);
							
							// wallet key
							ECKey walletKey = getPubKeyFromAccount(accountIndex, type, addressKey, true);
							
							//get address
							ATAddress add = getP2SHAddress(authKey, walletKey, addressKey, accountIndex, type);
							
							atAdd.setAddressStr(add.getAddressStr());
						  }
						  
						  
		return atAdd.build();
	}
	
	public List<ATAccount> getAllAccounts(){
		return configFile.getAllAccounts();
	}
	
	public ATAccount getAccount(int index) throws AccountWasNotFoundException{
		return configFile.getAccount(index);
	} 

	/**
	 * Remove account from config file.<br>
	 * <b>Will assert at least one account remains after the removal</b>
	 * 
	 * @param index
	 * @throws IOException
	 */
	public void removeAccount(int index) throws IOException{
		PairedAuthenticator po =  getPairingObjectForAccountIndex(index);
		if(po != null)
			removePairingObject(po.getPairingID());
		configFile.removeAccount(index);
		staticLogger.info("Removed account at index, " + index);
		Authenticator.fireOnAccountDeleted(index);
	}
	
	public ATAccount getAccountByName(String name){
		List<ATAccount> all = getAllAccounts();
		for(ATAccount acc: all)
			if(acc.getAccountName().equals(name))
				return acc;
		
		return null;
	}
	
	/**
	 * 
	 * @param accountIndex
	 * @param addressesType
	 * @param limit
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AddressNotWatchedByWalletException 
	 * @throws AccountWasNotFoundException 
	 */
	public ArrayList<String> getAccountNotUsedAddress(int accountIndex, HierarchyAddressTypes addressesType, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException, AccountWasNotFoundException{
		ArrayList<String> ret = new ArrayList<String>();
		ATAccount account = getAccount(accountIndex);
		if(addressesType == HierarchyAddressTypes.External)
		for(int i=0;i < limit; i++)//Math.min(account.getLastExternalIndex(), limit == -1? account.getLastExternalIndex():limit); i++){
		{
			if(account.getUsedExternalKeysList().contains(i))
				continue;
			ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
			ret.add(a.getAddressStr());
		}
		
		return ret;
	}
	
	/**
	 * Will return all used address of the account
	 * 
	 * @param accountIndex
	 * @param addressesType
	 * @return
	 * @throws CannotGetAccountUsedAddressesException 
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException
	 * @throws AddressNotWatchedByWalletException
	 * @throws AccountWasNotFoundException 
	 */
	public ArrayList<ATAddress> getAccountUsedAddresses(int accountIndex, HierarchyAddressTypes addressesType) throws CannotGetAccountUsedAddressesException{
		ArrayList<ATAddress> ret = new ArrayList<ATAddress>();
		try {
			ATAccount acc = getAccount(accountIndex);
			if(addressesType == HierarchyAddressTypes.External){
				List<Integer> used = acc.getUsedExternalKeysList();
				for(Integer i:used){
					ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
					ret.add(a);
				}
			}
		}
		catch(Exception e) {
			throw new CannotGetAccountUsedAddressesException("Canont get used addresses");
		}
		
		return ret;
	}
	
	public ArrayList<String> getAccountUsedAddressesString(int accountIndex, HierarchyAddressTypes addressesType) throws CannotGetAccountUsedAddressesException {
		ArrayList<ATAddress> addresses = getAccountUsedAddresses(accountIndex, addressesType);
		ArrayList<String> ret = new ArrayList<String>();
		for(ATAddress add: addresses)
			ret.add(add.getAddressStr());
		return ret;
	}
	
	/**
	 * Returns all addresses from an account in a ArrayList of strings
	 * 
	 * @param accountIndex
	 * @param addressesType
	 * @param limit
	 * @return ArrayList of strings
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AddressNotWatchedByWalletException 
	 * @throws AccountWasNotFoundException 
	 */
	public ArrayList<String> getAccountAddresses(int accountIndex, HierarchyAddressTypes addressesType, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException, AccountWasNotFoundException{
		ArrayList<String> ret = new ArrayList<String>();
		if(addressesType == HierarchyAddressTypes.External)
		for(int i=0;i < limit; i ++) //Math.min(account.getLastExternalIndex(), limit == -1? account.getLastExternalIndex():limit); i++){
		{
			ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
			ret.add(a.getAddressStr());
		}
		
		return ret;
	}

	public ATAccount setAccountName(String newName, int index) throws IOException, AccountWasNotFoundException{
		assert(newName.length() > 0);
		ATAccount.Builder b = ATAccount.newBuilder(getAccount(index));
		b.setAccountName(newName);
		configFile.updateAccount(b.build());
		Authenticator.fireOnAccountBeenModified(index);
		return b.build();
	}
	
	public void markAddressAsUsed(int accountIdx, int addIndx, HierarchyAddressTypes type) throws IOException, NoSuchAlgorithmException, JSONException, AddressFormatException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException, AccountWasNotFoundException{
		if(!isUsedAddress(accountIdx, type, addIndx)){
			configFile.markAddressAsUsed(accountIdx, addIndx,type);
			authenticatorWalletHierarchy.markAddressAsUsed(accountIdx, addIndx, type);
			ATAddress add = getATAddreessFromAccount(accountIdx, type, addIndx);
			Authenticator.fireOnAddressMarkedAsUsed(add);
			this.LOG.info("Marked " + add.getAddressStr() + " as used.");
		}
	}
	
	public boolean isUsedAddress(int accountIndex, HierarchyAddressTypes addressType, int keyIndex) throws AccountWasNotFoundException{
		return configFile.isUsedAddress(accountIndex, addressType, keyIndex);
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
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AccountWasNotFoundException 
	 * @throws AddressNowWatchedByWalletException 
	 */
	public ArrayList<String> getPairingAddressesArray(String PairID, HierarchyAddressTypes addressesType, int limit) throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException, AccountWasNotFoundException{
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
	
	public ECKey getPairedAuthenticatorKey(PairedAuthenticator po, int keyIndex){
		ArrayList<String> keyandchain = getPublicKeyAndChain(po.getPairingID());
		byte[] key = EncodingUtils.hexStringToByteArray(keyandchain.get(0));
		byte[] chain = EncodingUtils.hexStringToByteArray(keyandchain.get(1));
		HDKeyDerivation HDKey = null;
  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, keyIndex);
  		byte[] childpublickey = childKey.getPubKey();
		ECKey authKey = new ECKey(null, childpublickey);
		
		return authKey;
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
	
	/**
	 * If accID is provided, will not create a new account but will use the account ID
	 * 
	 * @param authMpubkey
	 * @param authhaincode
	 * @param sharedAES
	 * @param GCM
	 * @param pairingID
	 * @param pairName
	 * @param accID
	 * @param nt
	 * @throws IOException
	 */
	public PairedAuthenticator generatePairing(String authMpubkey, 
			String authhaincode, 
			String sharedAES, 
			String GCM, 
			String pairingID, 
			String pairName,
			@Nullable Integer accID,
			NetworkType nt) throws IOException{
		int accountID ;
		if( accID == null )
			accountID = generateNewAccount(nt, pairName, WalletAccountType.AuthenticatorAccount).getIndex();
		else{
			accountID = accID;
			ATAccount a = completeAccountObject(nt, accountID, pairName, WalletAccountType.AuthenticatorAccount);
			addNewAccountToConfigAndHierarchy(a);
		}
		PairedAuthenticator ret = writePairingData(authMpubkey,authhaincode,sharedAES,GCM,pairingID,accountID);
		Authenticator.fireOnNewPairedAuthenticator();
		return ret;
	}
	
	private PairedAuthenticator writePairingData(String mpubkey, String chaincode, String key, String GCM, String pairingID, int accountIndex) throws IOException{
		return configFile.writePairingData(mpubkey, chaincode, key, GCM, pairingID, accountIndex);
	}

	/**
	 * 
	 * @param pairingID
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void removePairingObject(String pairingID) throws FileNotFoundException, IOException{
		configFile.removePairingObject(pairingID);
	}
	
	/*public void addGeneratedAddressForPairing(String pairID, String addr, int indexWallet, int indexAuth) throws FileNotFoundException, IOException, ParseException{
		configFile.addGeneratedAddressForPairing(pairID,  addr, indexWallet, indexAuth);
	}*/
	
	//#####################################
	//
	//		 Balances handling
	//
	//#####################################
	
	public Coin getConfirmedBalance(int accountIdx) throws AccountWasNotFoundException{
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
	 * @throws AccountWasNotFoundException 
	 */
	public Coin addToConfirmedBalance(int accountIdx, Coin amount) throws IOException, AccountWasNotFoundException{
		Coin old = getConfirmedBalance(accountIdx);
		Coin ret = setConfirmedBalance(accountIdx, old.add(amount));
		staticLogger.info("Added " + amount.toFriendlyString() + " to confirmed balance. Account: " + accountIdx );
		return ret;
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws IOException
	 * @throws AccountWasNotFoundException 
	 */
	public Coin subtractFromConfirmedBalance(int accountIdx, Coin amount) throws IOException, AccountWasNotFoundException{
		Coin old = getConfirmedBalance(accountIdx);
		staticLogger.info("Subtracting " + amount.toFriendlyString() + " from confirmed balance(" + old.toFriendlyString() + "). Account: " + accountIdx);
		assert(old.compareTo(amount) >= 0);
		Coin ret = setConfirmedBalance(accountIdx, old.subtract(amount));
		return ret;
	}
	
	/**
	 * Will return the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 * @throws AccountWasNotFoundException 
	 */
	public Coin setConfirmedBalance(int accountIdx, Coin newBalance) throws IOException, AccountWasNotFoundException{
		long balance = configFile.writeConfirmedBalace(accountIdx, newBalance.longValue());
		Coin ret = Coin.valueOf(balance);
		staticLogger.info("Set " + ret.toFriendlyString() + " in confirmed balance. Account: " + accountIdx);
		return ret;
	}
	
	public Coin getUnConfirmedBalance(int accountIdx) throws AccountWasNotFoundException{
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
	 * @throws AccountWasNotFoundException 
	 */
	public Coin addToUnConfirmedBalance(int accountIdx, Coin amount) throws IOException, AccountWasNotFoundException{
		Coin old = getUnConfirmedBalance(accountIdx);
		Coin ret = setUnConfirmedBalance(accountIdx, old.add(amount));
		staticLogger.info("Added " + amount.toFriendlyString() + " to unconfirmed balance. Account: " + accountIdx );
		return ret;
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws IOException
	 * @throws AccountWasNotFoundException 
	 */
	public Coin subtractFromUnConfirmedBalance(int accountIdx, Coin amount) throws IOException, AccountWasNotFoundException{
		Coin old = getUnConfirmedBalance(accountIdx);
		staticLogger.info("Subtracting " + amount.toFriendlyString() + " from unconfirmed balance(" + old.toFriendlyString() + "). Account: " + accountIdx);
		assert(old.compareTo(amount) >= 0);
		Coin ret = setUnConfirmedBalance(accountIdx, old.subtract(amount));
		return ret;
	}
	
	/**
	 * Will return the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws IOException
	 * @throws AccountWasNotFoundException 
	 */
	public Coin setUnConfirmedBalance(int accountIdx, Coin newBalance) throws IOException, AccountWasNotFoundException{
		long balance = configFile.writeUnConfirmedBalace(accountIdx, newBalance.longValue());
		Coin ret = Coin.valueOf(balance);
		staticLogger.info("Set " + ret.toFriendlyString() + " in unconfirmed balance. Account: " + accountIdx);
		return ret;
	}
	
	/**
	 * Will return the updated confirmed balance
	 * 
	 * @param accountId
	 * @param amount
	 * @return
	 * @throws IOException 
	 * @throws AccountWasNotFoundException 
	 */
	public Coin moveFundsFromUnconfirmedToConfirmed(int accountId,Coin amount) throws IOException, AccountWasNotFoundException{
		Coin beforeConfirmed = getConfirmedBalance(accountId);
		Coin beforeUnconf = getUnConfirmedBalance(accountId);
		staticLogger.info("Moving " + amount.toFriendlyString() + 
				" from unconfirmed(" + beforeUnconf.toFriendlyString() 
				+") to confirmed(" + beforeConfirmed.toFriendlyString() + ") balance. Account: " + accountId );
		assert(beforeUnconf.compareTo(amount) >= 0);
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
	 * @throws AccountWasNotFoundException 
	 */
	public Coin moveFundsFromConfirmedToUnConfirmed(int accountId,Coin amount) throws IOException, AccountWasNotFoundException{
		Coin beforeConfirmed = getConfirmedBalance(accountId);
		Coin beforeUnconf = getUnConfirmedBalance(accountId);
		staticLogger.info("Moving " + amount.toFriendlyString() + 
				" from confirmed(" + beforeConfirmed.toFriendlyString() 
				+") to unconfirmed(" + beforeUnconf.toFriendlyString() + ") balance. Account: " + accountId );
		assert(beforeConfirmed.compareTo(amount) >= 0);
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
		
		public void addPendingRequest(PendingRequest req) throws FileNotFoundException, IOException{
			configFile.writeNewPendingRequest(req);
		}
		
		public void removePendingRequest(PendingRequest req) throws CannotRemovePendingRequestException{
			List<PendingRequest> l = new ArrayList<PendingRequest>();
			l.add(req);
			removePendingRequest(l);
		}
		
		public void removePendingRequest(List<PendingRequest> req) throws CannotRemovePendingRequestException {
			try {
				String a = "";
				for(PendingRequest pr:req)
					a = a + pr.getRequestID() + "\n					";
				
				staticLogger.info("Removed pending requests: " + a);
				configFile.removePendingRequest(req);
			}
			catch(Exception e) {
				throw new CannotRemovePendingRequestException(e.getMessage());
			}
		}
		
		public int getPendingRequestSize(){
			try {
				return getPendingRequests().size();
			} catch (CannotGetPendingRequestsException e) { }
			return 0;
		}
		
		public List<PendingRequest> getPendingRequests() throws CannotGetPendingRequestsException {
			try {
				return configFile.getPendingRequests();
			}
			catch(Exception e) {
				throw new CannotGetPendingRequestsException(e.getMessage());
			}
		}
		
		public String pendingRequestToString(PendingRequest op) throws AccountWasNotFoundException{
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
			ATAccount acc = getAccount(po.getWalletAccountIndex());
			String pairingName = acc.getAccountName();			
			return pairingName + ": " + type + "  ---  " + op.getRequestID();
		}
		
	
	//#####################################
	//
	//		One name
	//
	//#####################################
		
		public ConfigOneNameProfile getOnename(){
			try {
				AuthenticatorConfiguration.ConfigOneNameProfile on = configFile.getOnename();
				if(on.getOnename().length() == 0)
					return null;
				return on;
			} 
			catch (IOException e) {  return null; }
		}
		
		public ConfigOneNameProfile setOneName(String onename, String formattedname, @Nullable String imgURL, @Nullable String imgPath) throws CannotSetOneNameProfileException  {
			try {
				ConfigOneNameProfile.Builder onb = ConfigOneNameProfile.newBuilder();
			    onb.setOnename(onename);
			    onb.setOnenameFormatted(formattedname);
			    if(imgURL != null)
			    	onb.setOnenameAvatarURL(imgURL);
			    if(imgPath != null)
			    	onb.setOnenameAvatarFilePath(imgPath);
			    writeOnename(onb.build());
			    return onb.build();
			}
			catch(IOException e) {
				e.printStackTrace();
				throw new CannotSetOneNameProfileException("Cannot se one name profile");
			}
		}
				
		private void writeOnename(ConfigOneNameProfile one) throws FileNotFoundException, IOException{
			configFile.writeOnename(one);
		}
		
	//#####################################
	//
	//		Active account Control
	//
	//#####################################
		
		/**
		 * Surrounded with try & catch because we access it a lot.
		 * 
		 * @return
		 */
		public AuthenticatorConfiguration.ConfigActiveAccount getActiveAccount() {
			try {
				return configFile.getActiveAccount();
			} catch (IOException e) { e.printStackTrace(); }
			return null;
		}

		/**
		 * Sets the active account according to account index, returns the active account.<br>
		 * Will return null in case its not successful
		 * @param accountIdx
		 * @return
		 * @throws AccountWasNotFoundException 
		 * @throws IOException 
		 * @throws FileNotFoundException 
		 */
		public ATAccount setActiveAccount(int accountIdx) throws AccountWasNotFoundException{
			ATAccount acc = getAccount(accountIdx);
			AuthenticatorConfiguration.ConfigActiveAccount.Builder b1 = AuthenticatorConfiguration.ConfigActiveAccount.newBuilder();
			b1.setActiveAccount(acc);
			if(acc.getAccountType() == WalletAccountType.AuthenticatorAccount){
				PairedAuthenticator p = Authenticator.getWalletOperation().getPairingObjectForAccountIndex(acc.getIndex());
				b1.setPairedAuthenticator(p);
			}
			try {
				writeActiveAccount(b1.build());
				return acc;
			} catch (IOException e) { e.printStackTrace(); }
			return null;
		}

		private void writeActiveAccount(AuthenticatorConfiguration.ConfigActiveAccount acc) throws FileNotFoundException, IOException{

			configFile.writeActiveAccount(acc);
		}
		
	//#####################################
  	//
  	//	Regular Bitocoin Wallet Operations
  	//
  	//#####################################
		
	public Wallet getTrackedWallet(){
		return mWalletWrapper.getTrackedWallet();
	}
	
	public void setTrackedWallet(Wallet wallet){
		if(mWalletWrapper == null)
			mWalletWrapper = new WalletWrapper(wallet,null);
		else
			mWalletWrapper.setTrackedWallet(wallet);
		mWalletWrapper.addEventListener(new WalletListener());
	}
    
    public NetworkParameters getNetworkParams()
	{
    	assert(mWalletWrapper != null);
		return mWalletWrapper.getNetworkParams();
	}
    
    public boolean isWatchingAddress(Address address) throws AddressFormatException{
    	return isWatchingAddress(address.toString());
    }
    public boolean isWatchingAddress(String address) throws AddressFormatException
	{
    	assert(mWalletWrapper != null);
		return mWalletWrapper.isAuthenticatorAddressWatched(address);
	}
    
    public boolean isTransactionOutputMine(TransactionOutput out)
	{
    	assert(mWalletWrapper != null);
		return mWalletWrapper.isTransactionOutputMine(out);
	}
    
    public void addAddressToWatch(String address) throws AddressFormatException
	{
    	assert(mWalletWrapper != null);
    	if(!mWalletWrapper.isAuthenticatorAddressWatched(address)){
    		mWalletWrapper.addAddressToWatch(address);
        	this.LOG.info("Added address to watch: " + address);
    	}
	}
    
    public void addAddressesToWatch(final List<String> addresses) throws AddressFormatException
	{
    	assert(mWalletWrapper != null);
    	mWalletWrapper.addAddressesStringToWatch(addresses);
    	this.LOG.info("Added {} addresses to watch", addresses.size());
	}
    
    /**
     * Searches for a TransactionOutput hash and returns that TransactionOutput.<br>
     * If not found will return null
     * 
     * @param parentTransactionOutpointHash
     * @return
     */
    public TransactionOutput findTransactionOutpointByHash(String parentTransactionOutpointHash, long index)
	{
		assert(mWalletWrapper != null);
		List<TransactionOutput> unspentOutputs = mWalletWrapper.getWatchedOutputs();
		for(TransactionOutput out:unspentOutputs) {
			String hashOut = out.getParentTransaction().getHashAsString();
			if(hashOut.equals(parentTransactionOutpointHash) && out.getIndex() == index){
				return out;
			}
		}
		
		return null;
	}
    
//	public void connectInputs(List<TransactionInput> inputs)
//	{
//		assert(mWalletWrapper != null);
//		List<TransactionOutput> unspentOutputs = mWalletWrapper.getWatchedOutputs();
//		for(TransactionOutput out:unspentOutputs)
//			for(TransactionInput in:inputs){
//				String hashIn = in.getOutpoint().getHash().toString();
//				String hashOut = out.getParentTransaction().getHash().toString();
//				if(hashIn.equals(hashOut)){
//					in.connect(out);
//					break;
//				}
//			}
//	}
//	
//	public void disconnectInputs(List<TransactionInput> inputs){
//		for(TransactionInput input:inputs)
//			input.disconnect();
//	}
	
//	public SendResult sendCoins(Wallet.SendRequest req) throws InsufficientMoneyException
//	{
//		assert(mWalletWrapper != null);
//		this.LOG.info("Sent Tx: " + req.tx.getHashAsString());
//		return mWalletWrapper.sendCoins(req);
//	}
	
	public void addEventListener(WalletEventListener listener)
	{
		assert(mWalletWrapper != null);
		mWalletWrapper.addEventListener(listener);
	}
	
	public ECKey findKeyFromPubHash(byte[] pubkeyHash){
		assert(mWalletWrapper != null);
		return mWalletWrapper.findKeyFromPubHash(pubkeyHash);
	}
	
	public List<Transaction> getRecentTransactions(){
		assert(mWalletWrapper != null);
		return mWalletWrapper.getRecentTransactions();
	}
	
	public ArrayList<TransactionOutput> selectOutputsFromAccount(int accountIndex, Coin value) throws ScriptException, NoSuchAlgorithmException, AddressWasNotFoundException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AccountWasNotFoundException{
		ArrayList<TransactionOutput> all = getUnspentOutputsForAccount(accountIndex);
		ArrayList<TransactionOutput> ret = selectOutputs(value, all);
		return ret;
	}
	
	public ArrayList<TransactionOutput> selectOutputs(Coin value, ArrayList<TransactionOutput> candidates)
	{
		LinkedList<TransactionOutput> outs = new LinkedList<TransactionOutput> (candidates);
		DefaultCoinSelector selector = new DefaultCoinSelector();
		CoinSelection cs = selector.select(value, outs);
		Collection<TransactionOutput> gathered = cs.gathered;
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>(gathered);
	
		return ret;
	}
	
	public ArrayList<TransactionOutput> getUnspentOutputsForAccount(int accountIndex) throws ScriptException, NoSuchAlgorithmException, AddressWasNotFoundException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AccountWasNotFoundException{
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>();
		List<TransactionOutput> all = mWalletWrapper.getWatchedOutputs();
		for(TransactionOutput unspentOut:all){
			ATAddress add = findAddressInAccounts(unspentOut.getScriptPubKey().getToAddress(getNetworkParams()).toString());
			if(add.getAccountIndex() == accountIndex)
				ret.add(unspentOut);
		}
		return ret;
	}
 
	public ArrayList<TransactionOutput> getUnspentOutputsForAddresses(ArrayList<String> addressArr)
	{
		return mWalletWrapper.getUnspentOutputsForAddresses(addressArr);
	}
	
	public Coin getTxValueSentToMe(Transaction tx){
		return mWalletWrapper.getTxValueSentToMe(tx);
	}
	
	public Coin getTxValueSentFromMe(Transaction tx){
		return mWalletWrapper.getTxValueSentFromMe(tx);
	}
	
	public void decryptWallet(BAPassword password) throws NoWalletPasswordException{
		if(isWalletEncrypted())
		if(password.hasPassword()){
			mWalletWrapper.decryptWallet(password.toString());
			staticLogger.info("Decrypted wallet with password: " + password.toString());
		}
		else
			throw new NoWalletPasswordException("Illegal Password");
	}
	
	public void encryptWallet(BAPassword password) throws NoWalletPasswordException{
		if(!isWalletEncrypted())
		if(password.hasPassword()){
			mWalletWrapper.encryptWallet(password.toString());
			staticLogger.info("Encrypted wallet with password: " + password.toString());
		}
		else
			throw new NoWalletPasswordException("Illegal Password");
	}
	
	/**
	 * If pw is not null, will decrypt the wallet, get the seed and encrypt the wallet.
	 * 
	 * @param pw
	 * @return
	 * @throws NoWalletPasswordException 
	 */
	public DeterministicSeed getWalletSeed(@Nullable BAPassword pw) throws NoWalletPasswordException{
		if(isWalletEncrypted() && !pw.hasPassword())
				throw new NoWalletPasswordException("Wallet is encrypted, cannot get seed");
		DeterministicSeed ret;
		if(isWalletEncrypted()){
			decryptWallet(pw);
			ret = mWalletWrapper.getWalletSeed();
			encryptWallet(pw);
		}
		else
			ret = mWalletWrapper.getWalletSeed();
		return ret;
	}
		
	/**
	 * If pw is not null, will decrypt the wallet, get the seed and encrypt the wallet.
	 * 
	 * @param pw
	 * @return
	 * @throws NoWalletPasswordException 
	 */
	 public byte[] getWalletSeedBytes(@Nullable BAPassword pw) throws NoWalletPasswordException{
	 		return getWalletSeed(pw).getSecretBytes();
	 }
	
	public boolean isWalletEncrypted(){
		return mWalletWrapper.isWalletEncrypted();
	}
	
	//#####################################
	//
	//		Tx history
	//
	//#####################################
	
	public void writeNextSavedTxData(String txid, String toFrom, String description) throws CannotWriteToConfigurationFileException {
		try {
			configFile.writeNextSavedTxData(txid, toFrom, description);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public void writeSavedTxData(int x, String txid, String toFrom, String description) throws CannotWriteToConfigurationFileException {
		try {
			configFile.writeSavedTxData(x, txid, toFrom, description);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public ArrayList<String> getSavedTxidList(){
		return configFile.getSavedTxidList();
	}
	
	public String getSavedDescription (int index) {
		return configFile.getSavedDescription(index);
	}
	
	public String getSavedToFrom (int index) {
		return configFile.getSavedToFrom(index);
	}
	
	public ArrayList<Transaction> filterTransactionsByAccount (int accountIndex) throws CannotGetAccountFilteredTransactionsException {
		ArrayList<Transaction> filteredHistory = new ArrayList<Transaction>();
		try {
			ArrayList<String> usedExternalAddressList = getAccountUsedAddressesString(accountIndex, HierarchyAddressTypes.External);
			//ArrayList<String> usedInternalAddressList = getAccountUsedAddressesString(accountIndex, HierarchyAddressTypes.Internal);
			Set<Transaction> fullTxSet = mWalletWrapper.trackedWallet.getTransactions(false);
	    	for (Transaction tx : fullTxSet){
	    		for (int a=0; a<tx.getInputs().size(); a++){
	    			if (tx.getInput(a).getConnectedOutput()!=null){
	    				String address = tx.getInput(a).getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
	    				for (String addr : usedExternalAddressList){
	    					if (addr.equals(address)){
	    						if (!filteredHistory.contains(tx)){filteredHistory.add(tx);}
	    					}
	    				}
	    				/*for (String addr : usedInternalAddressList){
	    					if (addr.equals(address)){
	    						if (!filteredHistory.contains(tx)){filteredHistory.add(tx);}
	    					}
	    				}*/
	    			}
	    			//We need to do the same thing here for internal addresses
	    			
	    		}
	    		for (int b=0; b<tx.getOutputs().size(); b++){
	    			String address = tx.getOutput(b).getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
	    			for (String addr : usedExternalAddressList){
	    				if (addr.equals(address)){
	    					if (!filteredHistory.contains(tx)){filteredHistory.add(tx);}
	    				}
	    			}
	    			//Same thing here, we need to check internal addresses as well.
	    		}
	    	}	
		}
		catch (Exception e) {
			throw new CannotGetAccountFilteredTransactionsException("Cannot filter account's transactions");
		}
		
		return filteredHistory;
	}
	
	//#####################################
	//
	//		Operational State
	//
	//#####################################
	
	public BAOperationState getOperationalState(){
		return operationalState;
	}
	
	public void setOperationalState(BAOperationState value){
		operationalState = value;
		staticLogger.info("Changed Authenticator's operational state to " + BAOperationState.getStateString(value));
	}
	
	public enum BAOperationState{
		/**
		 * Indicates the wallet cannot sync
		 */
		NOT_SYNCED,
		/**
		 * When wallet is booting and downloading the blockchain, this state is on
		 */
		SYNCING,
		/**
		 * Indicates the wallet is ready and operational
		 */
		READY_AND_OPERATIONAL;
		
		public static String getStateString(BAOperationState state){
			switch(state){
			case NOT_SYNCED:
				return "Not Synced";
			case SYNCING:
				return "Syncing";
			case READY_AND_OPERATIONAL:
				return "Ready and Operational";
			}
			return null;
		}
	}
	
	//#####################################
	//
	//		Settings config
	//
	//#####################################
	
	public BitcoinUnit getAccountUnitFromSettings() {
		try {
			return settingsFile.getAccountUnit();
		} catch (IOException e) {
			return null;
		}
	}
	
	public void setAccountUnitInSettings(BitcoinUnit value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setAccountUnit(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public int getDecimalPointFromSettings() {
		try {
			return settingsFile.getDecimalPoint();
		} catch (IOException e) {
			return 0;
		}
	}
	
	public void setDecimalPointInSettings(int value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setDecimalPoint(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public String getLocalCurrencySymbolFromSettings() {
		try {
			return settingsFile.getLocalCurrencySymbol();
		} catch (IOException e) {
			return "";
		}
	}
	
	public void setLocalCurrencySymbolInSettings(String value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setLocalCurrencySymbol(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public Languages getLanguageFromSettings() {
		try {
			return settingsFile.getLanguage();
		} catch (IOException e) {
			return null;
		}
	}
	
	public void setLanguageInSettings(Languages value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setLanguage(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public boolean getIsUsingTORFromSettings() {
		try {
			return settingsFile.getIsUsingTOR();
		} catch (IOException e) {
			return false;
		}
	}
	
	public void setIsUsingTORInSettings(boolean value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setIsUsingTOR(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public boolean getIsConnectingToLocalHostFromSettings() {
		try {
			return settingsFile.getIsConnectingToLocalHost();
		} catch (IOException e) {
			return false;
		}
	}
	
	public void setIsConnectingToLocalHostInSettings(boolean value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setIsConnectingToLocalHost(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public boolean getIsConnectingToTrustedPeerFromSettings() {
		try {
			return settingsFile.getIsConnectingToTrustedPeer();
		} catch (IOException e) {
			return false;
		}
	}
	
	public void setIsConnectingToTrustedPeerInSettings(boolean value, @Nullable String peerIP) throws CannotWriteToConfigurationFileException {
		try {
			if(value)
				settingsFile.setIsConnectingToTrustedPeer(value, peerIP);
			else
				settingsFile.setNotConnectingToTrustedPeer();
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public String getTrustedPeerIPFromSettings() {
		try {
			return settingsFile.getTrustedPeerIP();
		} catch (IOException e) {
			return "";
		}
	}
	
	public double getBloomFilterFalsePositiveRateFromSettings() {
		try {
			return settingsFile.getBloomFilterFalsePositiveRate();
		} catch (IOException e) {
			return 0.0;
		}
	}
	
	public void setBloomFilterFalsePositiveRateInSettings(double value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setBloomFilterFalsePositiveRate((float)value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public double getDefaultFeeFromSettings() {
		try {
			return settingsFile.getDefaultFee();
		} catch (IOException e) {
			return 0.0;
		}
	}
	
	public void setDefaultFeeInSettings(int value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setDefaultFee(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
}


