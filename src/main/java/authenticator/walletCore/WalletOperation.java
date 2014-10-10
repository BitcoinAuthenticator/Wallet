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
import authenticator.walletCore.exceptions.CannotGetAddressException;
import authenticator.walletCore.exceptions.CannotGetHDKeyException;
import authenticator.walletCore.exceptions.CannotGetPendingRequestsException;
import authenticator.walletCore.exceptions.CannotReadFromConfigurationFileException;
import authenticator.walletCore.exceptions.CannotRemovePendingRequestException;
import authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import authenticator.walletCore.exceptions.NoWalletPasswordException;
import authenticator.hierarchy.BAHierarchy;
import authenticator.hierarchy.HierarchyUtils;
import authenticator.hierarchy.exceptions.IncorrectPathException;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import authenticator.hierarchy.exceptions.NoUnusedKeyException;
import authenticator.listeners.BAGeneralEventsListener.AccountModificationType;
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
import org.spongycastle.crypto.InvalidCipherTextException;

import wallettemplate.Main;
import wallettemplate.ControllerHelpers.ThrottledRunnableExecutor;
import authenticator.Utils.EncodingUtils;
import authenticator.Utils.OneName.exceptions.CannotSetOneNameProfileException;
import authenticator.db.settingsDB;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.ProtoConfig.ATAccount;
import authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import authenticator.protobuf.ProtoSettings.BitcoinUnit;
import authenticator.protobuf.ProtoSettings.ConfigSettings;
import authenticator.protobuf.ProtoSettings.Languages;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.core.Wallet.ExceededMaxTransactionSize;
import org.bitcoinj.core.Wallet.SendResult;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.DefaultCoinSelector;
import org.bitcoinj.wallet.DeterministicSeed;

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
	private walletDB configFile;
	private settingsDB settingsFile;
	private static BAApplicationParameters AppParams;
	private static WalletDownloadListener blockChainDownloadListener;
	
	public WalletOperation(){ 
		super(WalletOperation.class);
	}
	
	/**
	 * Instantiate WalletOperations without bitcoinj wallet.
	 * 
	 * @param params
	 * @throws IOException
	 */
	public WalletOperation(BAApplicationParameters params){
		super(WalletOperation.class);
		init(params);
	}
	
	/**
	 * Instantiate WalletOperations with bitcoinj wallet
	 * 
	 * @param wallet
	 * @param peerGroup
	 * @throws IOException
	 */
	public WalletOperation(Wallet wallet, PeerGroup peerGroup, BAApplicationParameters params){
		super(WalletOperation.class);
		if(mWalletWrapper == null){
			mWalletWrapper = new WalletWrapper(wallet,peerGroup);
			mWalletWrapper.addEventListener(new WalletListener());
		}
			
		init(params);
	}
	
	public void dispose(){
		mWalletWrapper = null;
		authenticatorWalletHierarchy = null;
		configFile = null;
		settingsFile = null;
		AppParams = null;
		blockChainDownloadListener = null;
	}
	
	private void init(BAApplicationParameters params) {
		try {
			String configFileName = params.getApplicationDataFolderAbsolutePath() + params.getAppName() + ".config";
			AppParams = params;
			if(configFile == null){
				configFile = new walletDB(configFileName);
				/**
				 * Check to see if a config file exists, if not, initialize
				 */
				if(!configFile.checkConfigFile()){
					//byte[] seed = BAHierarchy.generateMnemonicSeed();
					configFile.initConfigFile();
				}
			}
			if(settingsFile == null) {
				settingsFile = new settingsDB(configFileName);
			}
			if(authenticatorWalletHierarchy == null)
			{
				//byte[] seed = configFile.getHierarchySeed();
				authenticatorWalletHierarchy = new BAHierarchy(HierarchyCoinTypes.CoinBitcoin);
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
			
			setOperationalState(BAOperationState.SYNCING);
		}catch(Exception e) {
			throw new RuntimeException(e.toString());
		}
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
			 * the {org.bitcoinj.wallet.DefaultCoinSelector} can only choose candidates if they
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
	
	@SuppressWarnings("unused")
	public void updateBalaceNonBlocking(Wallet wallet, Runnable completionBlock){
		int s = 2;
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
	 * @throws CannotWriteToConfigurationFileException 
	 * @throws AccountWasNotFoundException 
	 * @throws IOException 
	 * @throws Exception
	 */
	private synchronized void updateBalance(Wallet wallet) throws CannotWriteToConfigurationFileException {
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
    	    				ATAddress add = findAddressInAccounts(addrStr);
    	    				if(add == null)
    	    					continue;
    	    				
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
        	    				ATAddress add = findAddressInAccounts(addrStr);
        	    				if(add == null)
        	    					continue;
        	    				
        	    				/**
        	    				 * Subtract from internal account list
        	    				 */
        	    				for(ATAccount.Builder acc:newBalances)
        	    					if(acc.getIndex() == add.getAccountIndex())
        	    					{
        	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
        	    						acc.setConfirmedBalance(old.subtract(out.getValue()).longValue());
        	    					}
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
    	    				ATAddress add = findAddressInAccounts(addrStr);
    	    				if(add == null)
    	    					continue;
    	    				
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
        	    				ATAddress add = findAddressInAccounts(addrStr);
        	    				if(add == null)
        	    					continue;
        	    				
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
	
	public WalletDownloadListener getDownloadEvenListener() {
		if (blockChainDownloadListener == null)
			blockChainDownloadListener = new WalletDownloadListener();
		return blockChainDownloadListener;
	}
	
	public class WalletDownloadListener extends DownloadListener {
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
        	Authenticator.fireOnBlockchainDownloadChange((float)(pct / 100.0));
        	
        	if(pct < 100)
        		setOperationalState(BAOperationState.SYNCING);
        }

        @Override
        protected void doneDownload() {
        	super.doneDownload();
        	setOperationalState(BAOperationState.READY_AND_OPERATIONAL);
        	Authenticator.fireOnBlockchainDownloadChange(1.0f);        	
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
	 * @throws AddressFormatException 
	 * @throws IncorrectPathException 
	 * @throws CannotGetHDKeyException 

	 */
	private ATAddress generateNextP2SHAddress(int accountIdx, HierarchyAddressTypes addressType) throws CannotGetHDKeyException, IncorrectPathException, AddressFormatException {
		PairedAuthenticator po = getPairingObjectForAccountIndex(accountIdx);
		return generateNextP2SHAddress(po.getPairingID(), addressType);
	}
	@SuppressWarnings({ "deprecation" })
	private ATAddress generateNextP2SHAddress(String pairingID, HierarchyAddressTypes addressType) throws CannotGetHDKeyException, IncorrectPathException, AddressFormatException {
		//Create a new key pair for wallet
		DeterministicKey walletHDKey = null;
		int walletAccountIdx = getAccountIndexForPairing(pairingID);
		int keyIndex = -1;
		if(addressType == HierarchyAddressTypes.External){
			walletHDKey = getNextExternalKey(walletAccountIdx, false);
			keyIndex = HierarchyUtils.getKeyIndexFromPath(walletHDKey.getPath(), false).num();
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
				TransactionOutput out = new TransactionOutput(getNetworkParams(),
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
	 * @throws CannotGetHDKeyException 
	 * @throws AddressNotWatchedByWalletException
	 */
	public Transaction signStandardTxWithAddresses(Transaction tx, 
			Map<String,ATAddress> keys,
			@Nullable BAPassword WALLET_PW) throws AddressNotWatchedByWalletException, CannotGetHDKeyException{
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
			String addFrom = connectedOutput.getScriptPubKey().getToAddress(getNetworkParams()).toString();
			TransactionSignature sig = tx.calculateSignature(index, keys.get(addFrom), 
					connectedOutput.getScriptPubKey(), 
					Transaction.SigHash.ALL, 
					false);
			Script inputScript = ScriptBuilder.createInputScript(sig, keys.get(addFrom));
			in.setScriptSig(inputScript);
			
			try {
				in.getScriptSig().correctlySpends(tx, (long)index, connectedOutput.getScriptPubKey());
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
		LOG.info("Set hierarchy key look ahead value to {}", value);
	}
	
	//#####################################
	//
	//		 Keys handling
	//
	//#####################################
	
	public ATAccountAddressHierarchy getAccountAddressHierarchy(int accoutnIdx, HierarchyAddressTypes type, @Nullable BAPassword walletPW) throws NoWalletPasswordException {
		return authenticatorWalletHierarchy.generateAccountAddressHierarchy(
 				this.getWalletSeedBytes(walletPW), 
 				accoutnIdx, 
 				HierarchyAddressTypes.External);
	}
	
 	/**
 	 * Generate a new wallet account and writes it to the config file
 	 * @return
 	 * @throws IOException 
 	 * @throws NoWalletPasswordException 
 	 */
 	private ATAccount generateNewAccount(NetworkType nt, String accountName, WalletAccountType type, @Nullable BAPassword walletPW) throws IOException, NoWalletPasswordException{
 		int accoutnIdx = authenticatorWalletHierarchy.generateNewAccount().getAccountIndex();
 		
 		ATAccountAddressHierarchy ext = getAccountAddressHierarchy(accoutnIdx, HierarchyAddressTypes.External, walletPW);
 		ATAccountAddressHierarchy intr = getAccountAddressHierarchy(accoutnIdx, HierarchyAddressTypes.Internal, walletPW);
 		
 		ATAccount b = completeAccountObject(nt, accoutnIdx, accountName, type, ext, intr);
		//writeHierarchyNextAvailableAccountID(accoutnIdx + 1); // update 
		addNewAccountToConfigAndHierarchy(b);
 		return b;
 	}
 	
 	/**
 	 * Return the next available hierarchy account index, doesn't create or do anything besides that
 	 * 
 	 * @return
 	 */
 	public int whatIsTheNextAvailableAccountIndex() {
 		return authenticatorWalletHierarchy.whatIsTheNextAvailableAccountIndex();
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
 	public ATAccount completeAccountObject(NetworkType nt,
 				int accoutnIdx, 
 				String accountName,
 				WalletAccountType type,
 				ATAccountAddressHierarchy externalAddressHierarchy,
 				ATAccountAddressHierarchy internalAddressHierarchy){
 		ATAccount.Builder b = ATAccount.newBuilder();
						  b.setIndex(accoutnIdx);
						  b.setConfirmedBalance(0);
						  b.setUnConfirmedBalance(0);
						  b.setNetworkType(nt.getValue());
						  b.setAccountName(accountName);
						  b.setAccountType(type);
						  b.setAccountExternalHierarchy(externalAddressHierarchy);
						  b.setAccountInternalHierarchy(internalAddressHierarchy);
		return b.build();
 	}
 	
 	/**
 	 * Register an account in the config file. Should be used whenever a new account is created
 	 * @param b
 	 * @throws IOException
 	 */
 	public void addNewAccountToConfigAndHierarchy(ATAccount b) throws IOException{
 		configFile.addAccount(b);
 		LOG.info("Generated new account at index, " + b.getIndex());
 	    authenticatorWalletHierarchy.addAccountToTracker(b.getIndex(), BAHierarchy.keyLookAhead);
 	    LOG.info("Added an account at index, " + b.getIndex() + " to hierarchy");
 	}
 	
 	public ATAccount generateNewStandardAccount(NetworkType nt, String accountName, @Nullable BAPassword walletPW) throws IOException, NoWalletPasswordException{
		ATAccount ret = generateNewAccount(nt, accountName, WalletAccountType.StandardAccount, walletPW);
		Authenticator.fireOnAccountsModified(AccountModificationType.NewAccount, ret.getIndex());
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
		ATAddress ret = getATAddreessFromAccount(accountI, HierarchyAddressTypes.External, HierarchyUtils.getKeyIndexFromPath(hdKey.getPath(), false).num());
		return ret;
	}
	
	/**
	 * 
	 * @param accountI
	 * @param shouldAddToWatchList
	 * @return
	 * @throws CannotGetHDKeyException 
	 */
	private DeterministicKey getNextExternalKey(int accountI, boolean shouldAddToWatchList) throws CannotGetHDKeyException {
		try {
			ATAccount acc = this.getAccount(accountI);
			DeterministicKey ret = authenticatorWalletHierarchy.getNextPubKey(accountI, HierarchyAddressTypes.External, acc.getAccountExternalHierarchy());
			if(shouldAddToWatchList)
				addAddressToWatch( ret.toAddress(getNetworkParams()).toString() );
			return ret;
		}
		catch(Exception e) {
			throw new CannotGetHDKeyException(e.toString());
		}
	}
	
	/**
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @param WALLET_PW
	 * @param iKnowAddressFromKeyIsNotWatched
	 * @return
	 * @throws CannotGetHDKeyException 
	 * @throws AddressNotWatchedByWalletException 

	 */
	public ECKey getPrivECKeyFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey,
			@Nullable BAPassword WALLET_PW,
			boolean iKnowAddressFromKeyIsNotWatched) throws AddressNotWatchedByWalletException, CannotGetHDKeyException{
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
	 * @throws AddressNotWatchedByWalletException
	 * @throws CannotGetHDKeyException 
	 */
	public DeterministicKey getPrivKeyFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey, 
			@Nullable BAPassword WALLET_PW,
			boolean iKnowAddressFromKeyIsNotWatched) throws AddressNotWatchedByWalletException, CannotGetHDKeyException{
		try {
			byte[] seed = getWalletSeedBytes(WALLET_PW);
			DeterministicKey ret = authenticatorWalletHierarchy.getPrivKeyFromAccount(seed, accountIndex, type, addressKey);
			if(!iKnowAddressFromKeyIsNotWatched && !isWatchingAddress(ret.toAddress(getNetworkParams())))
				throw new AddressNotWatchedByWalletException("You are trying to get an unwatched address");
			return ret;
		}
		catch(NoWalletPasswordException | KeyIndexOutOfRangeException e) {
			throw new CannotGetHDKeyException(e.toString());
		}
		
	}
	
	/**
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @param iKnowAddressFromKeyIsNotWatched
	 * @return
	 * @throws AddressNotWatchedByWalletException
	 * @throws CannotGetHDKeyException
	 */
	public DeterministicKey getPubKeyFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey, 
			boolean iKnowAddressFromKeyIsNotWatched) throws AddressNotWatchedByWalletException, CannotGetHDKeyException, AddressFormatException{
		try {
			ATAccount acc = this.getAccount(accountIndex);
			ATAccountAddressHierarchy H = type == HierarchyAddressTypes.External? acc.getAccountExternalHierarchy():acc.getAccountInternalHierarchy();
			DeterministicKey ret = authenticatorWalletHierarchy.getPubKeyFromAccount(accountIndex, type, addressKey, H);
			if(!iKnowAddressFromKeyIsNotWatched && !isWatchingAddress(ret.toAddress(getNetworkParams())))
				throw new AddressNotWatchedByWalletException("You are trying to get an unwatched address");
			return ret;
		}
		catch(KeyIndexOutOfRangeException | AccountWasNotFoundException e) {
			throw new CannotGetHDKeyException(e.toString());
		}
		
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

	 */
	public ATAddress findAddressInAccounts(String addressStr) {
		if(!isWatchingAddress(addressStr))
			return null;
		List<ATAccount> accounts = getAllAccounts();
		int gapLookAhead = 30;
		while(gapLookAhead < 10000) // just arbitrary number, TODO - this is very stupid !!
		{
			for(ATAccount acc:accounts){
				for(int i = gapLookAhead - 30 ; i < gapLookAhead; i++)
				{
					ATAddress add = null;
					/*
					 * Will throw an exception if the address is not watched by the wallet, if so just break from the loop
					 */
					try { add = getATAddreessFromAccount(acc.getIndex(), HierarchyAddressTypes.External, i); } catch(CannotGetAddressException e) {}
					if (add == null)
						break; 
					
					if(add.getAddressStr().equals(addressStr))
						return add;
				}
			}
			gapLookAhead += 30;
		}
		return null;
	}
	
	/**
	 * get addresses from a particular account and his chain
	 * 
	 * @param accountIndex
	 * @param type
	 * @param limit
	 * @return
	 * @throws CannotGetAddressException 
	 */
	public List<ATAddress> getATAddreessesFromAccount(int accountIndex, HierarchyAddressTypes type,int standOff, int limit) throws CannotGetAddressException {
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
	 * Will assert that the address was created before + it is watched by the wallet, if not will throw exception.
	 * 
	 * 
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @return
	 * @throws CannotGetAddressException 
	 */
	@SuppressWarnings("static-access")
	public ATAddress getATAddreessFromAccount(int accountIndex, 
			HierarchyAddressTypes type, 
			int addressKey) throws CannotGetAddressException {		
		try {
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
								  DeterministicKey hdKey = getPubKeyFromAccount(accountIndex,type,addressKey, true);
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
		catch(Exception e) {
			throw new CannotGetAddressException(e.toString());
		}
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
		LOG.info("Removed account at index, " + index);
		Authenticator.fireOnAccountsModified(AccountModificationType.AccountDeleted, index);
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
	 * @throws CannotGetAddressException 
	 * @throws NoSuchAlgorithmException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException 
	 * @throws AddressNotWatchedByWalletException 
	 * @throws AccountWasNotFoundException 
	 */
	public ArrayList<String> getAccountNotUsedAddress(int accountIndex, HierarchyAddressTypes addressesType, int limit) throws CannotGetAddressException {
		try {
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
		catch(Exception e) {
			throw new CannotGetAddressException(e.toString());
		}
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
	 * @throws CannotGetAddressException 
	 */
	public ArrayList<String> getAccountAddresses(int accountIndex, HierarchyAddressTypes addressesType, int limit) throws CannotGetAddressException{
		ArrayList<String> ret = new ArrayList<String>();
		if(addressesType == HierarchyAddressTypes.External)
		for(int i=0;i < limit; i ++) //Math.min(account.getLastExternalIndex(), limit == -1? account.getLastExternalIndex():limit); i++){
		{
			ATAddress a = getATAddreessFromAccount(accountIndex,addressesType, i);
			ret.add(a.getAddressStr());
		}
		
		return ret;
	}

	public ATAccount setAccountName(String newName, int index) throws CannotWriteToConfigurationFileException {
		assert(newName.length() > 0);
		try {
			ATAccount.Builder b = ATAccount.newBuilder(getAccount(index));
			b.setAccountName(newName);
			updateAccount(b.build());
			
			return b.build();
		}
		catch(Exception e) {
			throw new CannotWriteToConfigurationFileException(e.toString());
		}
		
	}
	
	public void markAddressAsUsed(int accountIdx, int addIndx, HierarchyAddressTypes type) throws CannotWriteToConfigurationFileException {
		try {
			if(!isUsedAddress(accountIdx, type, addIndx)){
				configFile.markAddressAsUsed(accountIdx, addIndx,type);
				authenticatorWalletHierarchy.markAddressAsUsed(accountIdx, addIndx, type);
				ATAddress add = getATAddreessFromAccount(accountIdx, type, addIndx);
				Authenticator.fireOnAddressMarkedAsUsed(add);
				this.LOG.info("Marked " + add.getAddressStr() + " as used.");
			}
		}
		catch(Exception e) {
			throw new CannotWriteToConfigurationFileException(e.toString());
		}
		
	}
	
	public boolean isUsedAddress(int accountIndex, HierarchyAddressTypes addressType, int keyIndex) throws AccountWasNotFoundException{
		return configFile.isUsedAddress(accountIndex, addressType, keyIndex);
	}
	
	private void updateAccount(ATAccount acc) throws CannotWriteToConfigurationFileException {
		try {
			configFile.updateAccount(acc);
			LOG.info("Updated accoutn: " + acc.toString());
			Authenticator.fireOnAccountsModified(AccountModificationType.AccountBeenModified, acc.getIndex());
		} catch (IOException e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.toString());
		}
		
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
	 * @throws CannotGetAddressException 
	 */
	public ArrayList<String> getPairingAddressesArray(String PairID, HierarchyAddressTypes addressesType, int limit) throws CannotGetAddressException{
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
	 * @param walletPW
	 * @return
	 * @throws IOException
	 * @throws NoWalletPasswordException 
	 */
	public PairedAuthenticator generatePairing(String authMpubkey, 
			String authhaincode, 
			String sharedAES, 
			String GCM, 
			String pairingID, 
			String pairName,
			@Nullable Integer accID,
			NetworkType nt,
			@Nullable BAPassword walletPW) throws IOException, NoWalletPasswordException{
		int accountID ;
		if( accID == null )
			accountID = generateNewAccount(nt, pairName, WalletAccountType.AuthenticatorAccount, walletPW).getIndex();
		else{
			accountID = accID;
			
			ATAccountAddressHierarchy ext = authenticatorWalletHierarchy.generateAccountAddressHierarchy(
	 				this.getWalletSeedBytes(walletPW), 
	 				accountID, 
	 				HierarchyAddressTypes.External);
	 		ATAccountAddressHierarchy intr = authenticatorWalletHierarchy.generateAccountAddressHierarchy(
	 				this.getWalletSeedBytes(walletPW), 
	 				accountID, 
	 				HierarchyAddressTypes.Internal);
			
			ATAccount a = completeAccountObject(nt, accountID, pairName, WalletAccountType.AuthenticatorAccount, ext, intr);
			addNewAccountToConfigAndHierarchy(a);
		}
		PairedAuthenticator ret = writePairingData(authMpubkey,authhaincode,sharedAES,GCM,pairingID,accountID);
		Authenticator.fireOnAccountsModified(AccountModificationType.NewAccount, accountID);
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
	
	public Coin getConfirmedBalance(int accountIdx) throws CannotReadFromConfigurationFileException{		
		try {
			long balance = configFile.getConfirmedBalace(accountIdx);
			return Coin.valueOf(balance);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new CannotReadFromConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin addToConfirmedBalance(int accountIdx, Coin amount) throws CannotWriteToConfigurationFileException{		
		try{
			Coin old = getConfirmedBalance(accountIdx);
			Coin ret = setConfirmedBalance(accountIdx, old.add(amount));
			LOG.info("Added " + amount.toFriendlyString() + " to confirmed balance. Account: " + accountIdx );
			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin subtractFromConfirmedBalance(int accountIdx, Coin amount) throws CannotWriteToConfigurationFileException{
		try{
			Coin old = getConfirmedBalance(accountIdx);
			LOG.info("Subtracting " + amount.toFriendlyString() + " from confirmed balance(" + old.toFriendlyString() + "). Account: " + accountIdx);
			assert(old.compareTo(amount) >= 0);
			Coin ret = setConfirmedBalance(accountIdx, old.subtract(amount));
			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}		
	}
	
	/**
	 * Will return the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin setConfirmedBalance(int accountIdx, Coin newBalance) throws CannotWriteToConfigurationFileException{
		try {
			long balance = configFile.writeConfirmedBalace(accountIdx, newBalance.longValue());
			Coin ret = Coin.valueOf(balance);
			LOG.info("Set " + ret.toFriendlyString() + " in confirmed balance. Account: " + accountIdx);
			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public Coin getUnConfirmedBalance(int accountIdx) throws CannotReadFromConfigurationFileException{
		try {
			long balance = configFile.getUnConfirmedBalace(accountIdx);
			return Coin.valueOf(balance);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new CannotReadFromConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin addToUnConfirmedBalance(int accountIdx, Coin amount) throws CannotWriteToConfigurationFileException{
		try {
			Coin old = getUnConfirmedBalance(accountIdx);
			Coin ret = setUnConfirmedBalance(accountIdx, old.add(amount));
			LOG.info("Added " + amount.toFriendlyString() + " to unconfirmed balance. Account: " + accountIdx );
			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return updated balance
	 * 
	 * @param accountIdx
	 * @param amount
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin subtractFromUnConfirmedBalance(int accountIdx, Coin amount) throws CannotWriteToConfigurationFileException{		
		try {
			Coin old = getUnConfirmedBalance(accountIdx);
			LOG.info("Subtracting " + amount.toFriendlyString() + " from unconfirmed balance(" + old.toFriendlyString() + "). Account: " + accountIdx);
			assert(old.compareTo(amount) >= 0);
			Coin ret = setUnConfirmedBalance(accountIdx, old.subtract(amount));
			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return the updated balance
	 * 
	 * @param accountIdx
	 * @param newBalance
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin setUnConfirmedBalance(int accountIdx, Coin newBalance) throws CannotWriteToConfigurationFileException {
		try {
			long balance = configFile.writeUnConfirmedBalace(accountIdx, newBalance.longValue());
			Coin ret = Coin.valueOf(balance);
			LOG.info("Set " + ret.toFriendlyString() + " in unconfirmed balance. Account: " + accountIdx);
			return ret;
			
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return the updated confirmed balance
	 * 
	 * @param accountId
	 * @param amount
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 * @throws IOException 
	 */
	public Coin moveFundsFromUnconfirmedToConfirmed(int accountId,Coin amount) throws CannotWriteToConfigurationFileException{		
		try {
			Coin beforeConfirmed = getConfirmedBalance(accountId);
			Coin beforeUnconf = getUnConfirmedBalance(accountId);
			LOG.info("Moving " + amount.toFriendlyString() + 
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
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	/**
	 * Will return the updated unconfirmed balance
	 * 
	 * @param accountId
	 * @param amount
	 * @return
	 * @throws CannotWriteToConfigurationFileException 
	 */
	public Coin moveFundsFromConfirmedToUnConfirmed(int accountId,Coin amount) throws CannotWriteToConfigurationFileException {
		try{
			Coin beforeConfirmed = getConfirmedBalance(accountId);
			Coin beforeUnconf = getUnConfirmedBalance(accountId);
			LOG.info("Moving " + amount.toFriendlyString() + 
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
		catch (Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
		
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
				
				LOG.info("Removed pending requests: " + a);
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
				
		public void writeOnename(ConfigOneNameProfile one) throws FileNotFoundException, IOException{
			configFile.writeOnename(one);
			LOG.info("Set new OneName profile: " + one.toString());
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
				PairedAuthenticator p = getPairingObjectForAccountIndex(acc.getIndex());
				b1.setPairedAuthenticator(p);
			}
			try {
				writeActiveAccount(b1.build());
				Authenticator.fireOnAccountsModified(AccountModificationType.ActiveAccountChanged, accountIdx);
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
    
    public boolean isWatchingAddress(Address address){
    	return isWatchingAddress(address.toString());
    }
    
    /**
     * Will check if address is watched by the wallet
     * 
     * @param address
     * @return
     */
    public boolean isWatchingAddress(String address)
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
	
	public ArrayList<TransactionOutput> selectOutputsFromAccount(int accountIndex, Coin value) throws ScriptException, CannotGetAddressException {
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
	
	public ArrayList<TransactionOutput> getUnspentOutputsForAccount(int accountIndex) throws ScriptException, CannotGetAddressException{
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
			try {
				mWalletWrapper.decryptWallet(password.toString());
			}
			catch(KeyCrypterException returnException) { 
				throw new NoWalletPasswordException("Illegal Password");
			}
			LOG.info("Decrypted wallet with password: " + password.toString());
		}
		else
			throw new NoWalletPasswordException("Illegal Password");
	}
	
	public void encryptWallet(BAPassword password) throws NoWalletPasswordException{
		if(!isWalletEncrypted())
		if(password.hasPassword()){
			mWalletWrapper.encryptWallet(password.toString());
			LOG.info("Encrypted wallet with password: " + password.toString());
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
	    				String address = tx.getInput(a).getConnectedOutput().getScriptPubKey().getToAddress(getNetworkParams()).toString();
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
	    			String address = tx.getOutput(b).getScriptPubKey().getToAddress(getNetworkParams()).toString();
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
		return AppParams.getOperationalState();
	}
	
	public void setOperationalState(BAOperationState value){
		if(getOperationalState() == value)
			return;
		AppParams.setOperationalState(value);
		LOG.info("Changed Authenticator's operational state to " + BAOperationState.getStateString(value));
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
		} catch (Exception e) {
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
	
	public void setIsPortForwarding(boolean value) throws CannotWriteToConfigurationFileException{
		try {
			settingsFile.setIsPortForwarding(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public boolean getIsPortForwarding() {
		try {
			return settingsFile.getIsPortForwarding();
		} catch (Exception e) {
			return false;
		}
	}
}


