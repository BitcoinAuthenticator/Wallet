package org.authenticator.walletCore;

import org.authenticator.Authenticator;
import org.authenticator.BAApplicationParameters;
import org.authenticator.BASE;
import org.authenticator.BAApplicationParameters.NetworkType;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.hierarchy.SingleAccountManagerImpl;
import org.authenticator.protobuf.ConfigExtension;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.walletCore.exceptions.*;
import org.authenticator.walletCore.utils.BAPassword;
import org.authenticator.walletCore.utils.CoinsReceivedNotificationSender;
import org.authenticator.walletCore.utils.WalletListener;
import org.authenticator.hierarchy.BAHierarchy;
import org.authenticator.hierarchy.HierarchyUtils;
import org.authenticator.hierarchy.exceptions.IncorrectPathException;
import org.authenticator.listeners.BAGeneralEventsAdapter;
import org.authenticator.listeners.BAGeneralEventsListener.AccountModificationType;
import org.authenticator.listeners.BAGeneralEventsListener.PendingRequestUpdateType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

import javax.annotation.Nullable;

import org.spongycastle.util.encoders.Hex;
import org.authenticator.db.SettingsDb;
import org.authenticator.db.WalletDb;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import org.authenticator.protobuf.ProtoSettings.BitcoinUnit;
import org.authenticator.protobuf.ProtoSettings.Languages;
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
 * <li>{@link org.authenticator.walletCore.WalletWrapper} for normal bitcoinj wallet operations</li>
 * <li>Authenticator wallet operations</li>
 * <li>Pending requests control</li>
 * <li>Active account control</li>
 * </ol>
 * @author Alon
 */
public class WalletOperation extends BASE{

	private WalletWrapper mWalletWrapper;
	private BAHierarchy authenticatorWalletHierarchy;
	private WalletDb configFile;
	private SettingsDb settingsFile;
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
			mWalletWrapper.addEventListener(new WalletListener(WalletOperation.this));
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
			String getConfigFileName = params.getApplicationDataFolderAbsolutePath() + params.getAppName() + ".config";
			AppParams = params;
			if(configFile == null){
				configFile = new WalletDb(getConfigFileName);
				/**
				 * Check to see if a config file exists, if not, initialize
				 */
				if(!configFile.checkConfigFile()){
					//byte[] seed = BAHierarchy.generateMnemonicSeed();
					configFile.initConfigFile();
				}
			}
			if(settingsFile == null) {
				settingsFile = new SettingsDb(getConfigFileName);
			}
			if(getWalletHierarchy() == null)
			{
				//byte[] seed = getConfigFile.getHierarchySeed();
				authenticatorWalletHierarchy = new BAHierarchy();
				/**
				 * Load num of keys generated in every account to get 
				 * the next fresh key
				 */
				List<SingleAccountManagerImpl> accountTrackers = new ArrayList<SingleAccountManagerImpl>();
				List<ATAccount> allAccount = getAllAccounts();
				for(ATAccount acc:allAccount){
					SingleAccountManagerImpl at =   new SingleAccountManagerImpl(acc.getIndex(),
							BAHierarchy.keyLookAhead,
							acc.getUsedExternalKeysList(), 
							acc.getUsedInternalKeysList());
					
					accountTrackers.add(at);
				}

				getWalletHierarchy().buildWalletHierarchyForStartup(accountTrackers);
			}
			
			setOperationalState(BAOperationState.SYNCING);
		}catch(Exception e) {
			throw new RuntimeException(e.toString());
		}
	}

	//#######################################
	//
	//		Getters and setters
	//
	//#######################################
	public WalletDb getConfigFile() {
		return configFile;
	}

	public BAHierarchy getWalletHierarchy() { return authenticatorWalletHierarchy; }

	public Wallet getTrackedWallet(){
		return getWalletWrapper().getTrackedWallet();
	}

	public WalletWrapper getWalletWrapper() { return  mWalletWrapper; }

	public void setTrackedWallet(Wallet wallet){
		if(mWalletWrapper == null)
			mWalletWrapper = new WalletWrapper(wallet,null);
		else
			mWalletWrapper.setTrackedWallet(wallet);
		mWalletWrapper.addEventListener(new WalletListener(WalletOperation.this));
	}
	
	public BAApplicationParameters getApplicationParams(){
		return AppParams;
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
			return getWalletWrapper().broadcastTransactionFromWallet(tx);
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

	/**
	 *	Will generate a transaction with selected inputs, outputs, fee and change.
	 *
	 * @param outSelected
	 * @param to
	 * @param fee
	 * @param changeAdd
	 * @return
	 * @throws UnableToCompleteTransactionException
	 */
	public Transaction mkUnsignedTxWithSelectedInputs(
			ArrayList<TransactionOutput> outSelected, 
			HashMap<String, Coin> to, 
			Coin fee, 
			String changeAdd) throws UnableToCompleteTransactionException {

		try {
			Transaction tx = new Transaction(getNetworkParams());
			ArrayList<TransactionOutput> Outputs = new ArrayList<TransactionOutput>();
			for(String addStr: to.keySet()){
				Address add = new Address(getNetworkParams(), addStr);
				Coin value = to.get(addStr);
				if (value.compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
					TransactionOutput out = new TransactionOutput(getNetworkParams(),
							tx,
							value,
							add);
					Outputs.add(out);
				}
			}

			return this.mkUnsignedTxWithSelectedInputs(tx, outSelected, Outputs, fee, changeAdd);
		}
		catch (Exception e) {
			throw new UnableToCompleteTransactionException(e.toString());
		}
	}

	/**
	 *	Will complete the passed transaction with selected inputs, outputs, fee and change.
	 *
	 * @param tx
	 * @param outSelected
	 * @param to
	 * @param fee
	 * @param changeAdd
	 * @return
	 * @throws UnableToCompleteTransactionException
	 */
	private Transaction mkUnsignedTxWithSelectedInputs(Transaction tx,
			ArrayList<TransactionOutput> outSelected, 
			ArrayList<TransactionOutput>to, 
			Coin fee, 
			String changeAdd) throws UnableToCompleteTransactionException {
		try {
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

			ArrayList<TransactionOutput> outputs = new ArrayList<>();

			//Add the outputs to the output list
			for (TransactionOutput output : to){
				outputs.add(output);
			}

			//Add the change
			Address change = new Address(getNetworkParams(), changeAdd);
			Coin rest = inAmount.subtract(totalOut.add(fee));
			if(rest.compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
				TransactionOutput changeOut = new TransactionOutput(getNetworkParams(), tx, rest, change);
				outputs.add(changeOut);
			}
			else
				fee = fee.add(rest);

			//Shuffle the outputs to improve privacy then add to the tx.
			Collections.shuffle(outputs);
			for (TransactionOutput out: outputs){
				tx.addOutput(out);
			}

			// Check size.
			int size = tx.bitcoinSerialize().length;
			if (size > Transaction.MAX_STANDARD_TX_SIZE)
				throw new ExceededMaxTransactionSize();

			LOG.info("New Transaction: " + tx.toString());

			return tx;
		}
		catch (Exception e) {
			throw new UnableToCompleteTransactionException(e.toString());
		}
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
		getWalletHierarchy().setKeyLookAhead(value);
		LOG.info("Set hierarchy key look ahead value to {}", value);
	}
	
	//#####################################
	//
	//		 Keys handling
	//
	//#####################################
	
	public ATAccountAddressHierarchy getAccountAddressHierarchy(int accoutnIdx, HierarchyAddressTypes type, @Nullable BAPassword walletPW) throws WrongWalletPasswordException {
		return HierarchyUtils.generateAccountAddressHierarchy(
 				getWalletSeedBytes(walletPW),
				accoutnIdx,
				HierarchyAddressTypes.External,
				HierarchyCoinTypes.CoinBitcoin);
	}

	/**
 	 * Generate a new wallet account and writes it to the config file
 	 * @return
 	 * @throws IOException 
 	 * @throws org.authenticator.walletCore.exceptions.WrongWalletPasswordException
 	 */
 	private ATAccount generateNewAccount(NetworkType nt, String accountName, WalletAccountType type, @Nullable BAPassword walletPW) throws IOException, WrongWalletPasswordException {
 		int accoutnIdx = getWalletHierarchy().generateNewAccount().getAccountIndex();
 		
 		ATAccountAddressHierarchy ext = getAccountAddressHierarchy(accoutnIdx,
																	HierarchyAddressTypes.External,
																	walletPW);
 		ATAccountAddressHierarchy intr = getAccountAddressHierarchy(accoutnIdx,
																	HierarchyAddressTypes.Internal,
																	walletPW);
 		
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
 		return getWalletHierarchy().whatIsTheNextAvailableAccountIndex();
 	}
 	
 	/**
 	 * Giving the necessary params, will return a complete {@link org.authenticator.protobuf.ProtoConfig.ATAccount ATAccount} object
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
 		getConfigFile().addAccount(b);
 		LOG.info("Generated new account at index, " + b.getIndex());
		getWalletHierarchy().addAccountToTracker(b.getIndex(), BAHierarchy.keyLookAhead);
 	    LOG.info("Added an account at index, " + b.getIndex() + " to hierarchy");
 	}
 	
 	public ATAccount generateNewStandardAccount(NetworkType nt, String accountName, @Nullable BAPassword walletPW) throws IOException, WrongWalletPasswordException {
		ATAccount ret = generateNewAccount(nt, accountName, WalletAccountType.StandardAccount, walletPW);
		Authenticator.fireOnAccountsModified(AccountModificationType.NewAccount, ret.getIndex());
		return ret;
	}

	
	/**
	 * Get the next {@link org.authenticator.protobuf.ProtoConfig.ATAddress ATAddress} object that is not been used, <b>it may been seen already</b><br>
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
			DeterministicKey ret = getWalletHierarchy().getNextPubKey(accountI, HierarchyAddressTypes.External, acc.getAccountExternalHierarchy());
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
	 * If the key was never created before, use {@link org.authenticator.walletCore.WalletOperation#getNextExternalAddress getNextExternalAddress} instead.
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
			boolean iKnowAddressFromKeyIsNotWatched) throws AddressNotWatchedByWalletException, CannotGetHDKeyException {
		try {
			byte[] seed = getWalletSeedBytes(WALLET_PW);
			DeterministicKey ret = HierarchyUtils.getPrivKeyFromAccount(seed,
					accountIndex,
					type,
					addressKey,
					HierarchyCoinTypes.CoinBitcoin);
			if(!iKnowAddressFromKeyIsNotWatched && !isWatchingAddress(ret.toAddress(getNetworkParams())))
				throw new AddressNotWatchedByWalletException("You are trying to get an unwatched address");
			return ret;
		}
		catch(WrongWalletPasswordException e) {
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
			DeterministicKey ret = HierarchyUtils.getPubKeyFromAccount(accountIndex, type, addressKey, H);
			if(!iKnowAddressFromKeyIsNotWatched && !isWatchingAddress(ret.toAddress(getNetworkParams())))
				throw new AddressNotWatchedByWalletException("You are trying to get an unwatched address");
			return ret;
		}
		catch(AccountWasNotFoundException e) {
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
	 * @return {@link org.authenticator.protobuf.ProtoConfig.ATAddress ATAddress}

	 */
	public ATAddress findAddressInAccounts(String addressStr) {
		if(!isWatchingAddress(addressStr))
			return null;
		List<ATAccount> accounts = getAllAccounts();
		for(ATAccount acc:accounts){
			for(int i = 0 ; i <= acc.getUsedExternalKeysCount() + BAHierarchy.keyLookAhead; i++)
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
		return getConfigFile().getAllAccounts();
	}
	
	public ATAccount getAccount(int index) throws AccountWasNotFoundException{
		return getConfigFile().getAccount(index);
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
		getConfigFile().removeAccount(index);
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
				getConfigFile().markAddressAsUsed(accountIdx, addIndx, type);
				getWalletHierarchy().markAddressAsUsed(accountIdx, addIndx, type);
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
		return getConfigFile().isUsedAddress(accountIndex, addressType, keyIndex);
	}
	
	private void updateAccount(ATAccount acc) throws CannotWriteToConfigurationFileException {
		try {
			getConfigFile().updateAccount(acc);
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
	public void updatePairingGCMRegistrationID(String pairingID, String newGCMRegID) throws CannotWriteToConfigurationFileException {
		try {
			getConfigFile().updatePairingGCMRegistrationID(pairingID, newGCMRegID);
			LOG.info("Updated GCM for pairing: " + pairingID);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}

	/**
	 * Will return null for a non existing account
	 *
	 * @param accIdx
	 * @return
	 */
	public PairedAuthenticator getPairingObjectForAccountIndex(int accIdx) {
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator po: all)
			{
				if(po.getWalletAccountIndex() == accIdx)
				{
					return po;
				}
			}
		} catch (IOException e) { e.printStackTrace(); }

		return null;
	}

	/**
	 * will return -1 for a non existing account
	 *
	 * @param PairID
	 * @return
	 */
	public int getAccountIndexForPairing(String PairID){
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator po: all)
			{
				if(po.getPairingID().equals(PairID))
				{
					return po.getWalletAccountIndex();
				}
			}
		} catch (IOException e) { e.printStackTrace(); }

		return -1;
	}

	/**
	 * Returns all addresses from a pairing in a ArrayList of strings
	 *
	 * @param PairID
	 * @param addressesType
	 * @param limit
	 * @return
	 * @throws CannotGetAddressException
	 */
	public ArrayList<String> getPairingAddressesArray(String PairID, HierarchyAddressTypes addressesType, int limit) throws CannotGetAddressException{
		int accIndex = getAccountIndexForPairing(PairID);
		return getAccountAddresses(accIndex,addressesType, limit);
	}
	
	/**
	 * Returns the Master Public [index 0] Key and Chaincode [index 1] as an ArrayList object
	 *
	 */
	public ArrayList<String> getPublicKeyAndChain(String pairingID) {
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
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
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}
	
	public ECKey getPairedAuthenticatorKey(PairedAuthenticator po, int keyIndex){
		if(keyIndex < 0)
			return null;
		ArrayList<String> keyandchain = getPublicKeyAndChain(po.getPairingID());
		byte[] key = Hex.decode(keyandchain.get(0));
		byte[] chain = Hex.decode(keyandchain.get(1));
		HDKeyDerivation HDKey = null;
  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, keyIndex);
  		byte[] childpublickey = childKey.getPubKey();
		ECKey authKey = new ECKey(null, childpublickey);
		
		return authKey;
	}
	
	/**Returns the number of key pairs in the wallet */
	public long getKeyNum(String pairID){
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator o:all)
			{
				if(o.getPairingID().equals(pairID))
					return o.getKeysN();
			}
		} catch (IOException e) { e.printStackTrace(); }

		return 0;
	}

	/**
	 * Returns the decrypted (in case encrypted) AES key bytes (in plain, not hex), null if no key found.
	 *
	 * @param pairID
	 * @param walletPW
	 * @return
	 */
	public byte[] getAESKey(String pairID, @Nullable BAPassword walletPW) throws WrongWalletPasswordException, CryptoUtils.CannotDecryptMessageException {
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator o:all)
			{
				if(o.getPairingID().equals(pairID)) {
					if(!o.getIsEncrypted()) {
						return o.getAesKey().toByteArray();
					}
					else {
						LOG.info("Pairing " + pairID + " AES key is encrypted. Returning decrypted key");
						int accountIdx = getAccountIndexForPairing(pairID);
						byte[] seed = getWalletSeedBytes(walletPW);
						byte[] key = Hex.toHexString(o.getAesKey().toByteArray()).getBytes();

						// return in plain byte array, not hex
						String[] additionalArgs = new String[]{ new Integer(accountIdx).toString() };
						return CryptoUtils.decryptHexPayloadWithBaAESKey(key,
								o.getKeySalt().toByteArray(),
								seed,
								additionalArgs);
					}
				}
			}
		} catch (IOException e) { e.printStackTrace(); }

		return null;
	}
		
	public List<PairedAuthenticator> getAllPairingObjectArray() throws FileNotFoundException, IOException
	{
		return getConfigFile().getAllPairingObjectArray();
	}
	
	public PairedAuthenticator getPairingObject(String pairID)
	{
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator po: all)
				if(po.getPairingID().equals(pairID))
					return po;
		} catch (IOException e) { e.printStackTrace(); }

		return null;
	}
	
	public ArrayList<String> getPairingIDs()
	{
		ArrayList<String> ret = new ArrayList<String>();
		try {
			List<PairedAuthenticator> all = getAllPairingObjectArray();
			for(PairedAuthenticator o:all)
				ret.add(o.getPairingID());
		} catch (IOException e) { e.printStackTrace(); }

		return ret;
	}

	/**
	 *
	 * @param authMpubkey
	 * @param authhaincode
	 * @param key - <b>must be passed not encrypted !</b>
	 * @param GCM
	 * @param pairingID
	 * @param pairName
	 * @param passedAccountID - <b>if null will generate a new account</b>
	 * @param nt
	 * @param shouldEncrypt - <b>if true, will encrypt the shared AES key with</b>
	 * @param walletPW
	 * @return
	 * @throws IOException
	 * @throws org.authenticator.walletCore.exceptions.WrongWalletPasswordException
	 * @throws CryptoUtils.CouldNotEncryptPayload
	 */
	public PairedAuthenticator generatePairing(String authMpubkey, 
			String authhaincode, 
			byte[] key,
			String GCM, 
			String pairingID, 
			String pairName,
			@Nullable Integer passedAccountID,
			NetworkType nt,
			boolean shouldEncrypt,
			@Nullable BAPassword walletPW) throws IOException, WrongWalletPasswordException, CryptoUtils.CouldNotEncryptPayload {
		int finalAccountID ;
		if( passedAccountID == null )
			finalAccountID = generateNewAccount(nt, pairName, WalletAccountType.AuthenticatorAccount, walletPW).getIndex();
		else{
			finalAccountID = passedAccountID;
			
			ATAccountAddressHierarchy ext = getAccountAddressHierarchy(finalAccountID,
					HierarchyAddressTypes.External,
					walletPW);
	 		ATAccountAddressHierarchy intr = getAccountAddressHierarchy(finalAccountID,
					HierarchyAddressTypes.Internal,
					walletPW);
			
			ATAccount a = completeAccountObject(nt,
					finalAccountID,
					pairName,
					WalletAccountType.AuthenticatorAccount,
					ext,
					intr);
			addNewAccountToConfigAndHierarchy(a);
		}

		SecureRandom random = new SecureRandom();
		byte[] saltBytes = new byte[20];
		random.nextBytes(saltBytes);
		byte[] salt = saltBytes;
		byte[] sharedEncryptedAES = key;
		if(shouldEncrypt) {
			byte[] seed = getWalletSeedBytes(walletPW);
			String[] additionalArgs = new String[]{ new Integer(finalAccountID).toString() };
			sharedEncryptedAES = CryptoUtils.encryptHexPayloadWithBaAESKey(Hex.toHexString(key).getBytes(),
					salt,
					seed,
					additionalArgs);
		}

		PairedAuthenticator ret = writePairingData(authMpubkey,
				authhaincode,
				sharedEncryptedAES, // plain byte array, not hex
				GCM,
				pairingID,
				finalAccountID,
				shouldEncrypt,
				salt);
		Authenticator.fireOnAccountsModified(AccountModificationType.NewAccount, finalAccountID);
		LOG.info("Created new pairing authenticator object: " + ret.toString());
		return ret;
	}
	
	private PairedAuthenticator writePairingData(String mpubkey,
												 String chaincode,
												 byte[] key,
												 String GCM,
												 String pairingID,
												 int accountIndex,
												 boolean isEncrypted,
												 byte[] salt) throws IOException{
		return getConfigFile().writePairingData(mpubkey, chaincode, key, GCM, pairingID, accountIndex, isEncrypted, salt);
	}

	/**
	 * 
	 * @param pairingID
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void removePairingObject(String pairingID) throws FileNotFoundException, IOException{
		getConfigFile().removePairingObject(pairingID);
	}

	//#####################################
	//
	//		 Balances handling
	//
	//#####################################
	
	public Coin getConfirmedBalance(int accountIdx) throws CannotReadFromConfigurationFileException{		
		try {
			long balance = getConfigFile().getConfirmedBalace(accountIdx);
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
			long balance = getConfigFile().writeConfirmedBalace(accountIdx, newBalance.longValue());
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
			long balance = getConfigFile().getUnConfirmedBalace(accountIdx);
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
			long balance = getConfigFile().writeUnConfirmedBalace(accountIdx, newBalance.longValue());
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
			getConfigFile().writeNewPendingRequest(req);
			LOG.info("Added pending request: " + req.getRequestID());
			Authenticator.fireOnPendingRequestUpdate(Arrays.asList(req), PendingRequestUpdateType.RequestAdded);
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

				getConfigFile().removePendingRequest(req);
				LOG.info("Removed pending requests: " + a);
				Authenticator.fireOnPendingRequestUpdate(req, PendingRequestUpdateType.RequestDeleted);
			}
			catch(Exception e) {
				e.printStackTrace();
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
				return getConfigFile().getPendingRequests();
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
	//	 On Coins received Notifications
	//
	//#####################################
	/**
	 * If called, will send a notification to the appropriate paired device when coins are received
	 */
	BAGeneralEventsAdapter eventsAdapterForCoinsReceivedNotificaiton = null;
	public void sendNotificationToAuthenticatorWhenCoinsReceived() {
		if(eventsAdapterForCoinsReceivedNotificaiton == null) {
			eventsAdapterForCoinsReceivedNotificaiton = new BAGeneralEventsAdapter() {
				@Override
				public void onBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged) {
					if(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(WalletOperation.this, tx))
					{ // send
						try {
							CoinsReceivedNotificationSender.send(WalletOperation.this,
									tx,
									howBalanceChanged, Authenticator.getLongLivingDataBinder().getWalletPassword());
						} catch (Exception e) {
							e.printStackTrace();
							if(e instanceof WrongWalletPasswordException)
								Authenticator.getLongLivingOperationsListener().onError(null, new Exception("We tried notifying your Authenticator app that you received some coins but we failed because your wallet is locked"), null);
							else
								Authenticator.getLongLivingOperationsListener().onError(null, e, null);
						}
					}
				}
			};
			Authenticator.addGeneralEventsListener(eventsAdapterForCoinsReceivedNotificaiton);
		}
	}
		
	//#####################################
	//
	//		One name
	//
	//#####################################
		
		public ConfigOneNameProfile getOnename(){
			try {
				AuthenticatorConfiguration.ConfigOneNameProfile on = getConfigFile().getOnename();
				if(on.getOnename().length() == 0)
					return null;
				return on;
			} 
			catch (IOException e) {  return null; }
		}
				
		public void writeOnename(ConfigOneNameProfile one) throws FileNotFoundException, IOException{
			getConfigFile().writeOnename(one);
			LOG.info("Set new OneName profile: " + one.toString());
		}
		
		public boolean isOnenameAvatarSet() {
			try {
				AuthenticatorConfiguration.ConfigOneNameProfile on = getConfigFile().getOnename();
				if(on.getOnename().length() == 0)
					return false;
				return true;
			} 
			catch (IOException e) {  return false; }
		}
		
		public boolean deleteOneNameAvatar() throws IOException {
			if(isOnenameAvatarSet()) {
				getConfigFile().deleteOneNameAvatar();
				LOG.info("Deleted OneName profile");
				return true;
			}
			return false;
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
				return getConfigFile().getActiveAccount();
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
			getConfigFile().writeActiveAccount(acc);
		}
		
	//#####################################
  	//
  	//	Regular Bitocoin Wallet Operations
  	//
  	//#####################################

    public NetworkParameters getNetworkParams()
	{
		return getWalletWrapper().getNetworkParams();
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
    	return getWalletWrapper().isAuthenticatorAddressWatched(address);
	}
    
    public boolean isTransactionOutputMine(TransactionOutput out)
	{
		return getWalletWrapper().isTransactionOutputMine(out);
	}
    
    public void addAddressToWatch(String address) throws AddressFormatException
	{
    	if(!getWalletWrapper().isAuthenticatorAddressWatched(address)){
			getWalletWrapper().addAddressToWatch(address);
        	this.LOG.info("Added address to watch: " + address);
    	}
	}
    
    public void addAddressesToWatch(final List<String> addresses) throws AddressFormatException
	{
		getWalletWrapper().addAddressesStringToWatch(addresses);
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
		List<TransactionOutput> unspentOutputs = getWalletWrapper().getWatchedOutputs();
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
		getWalletWrapper().addEventListener(listener);
	}
	
	public ECKey findKeyFromPubHash(byte[] pubkeyHash){
		return getWalletWrapper().findKeyFromPubHash(pubkeyHash);
	}
	
	public List<Transaction> getRecentTransactions(){
		return getWalletWrapper().getRecentTransactions();
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
		List<TransactionOutput> all = getWalletWrapper().getWatchedOutputs();
		for(TransactionOutput unspentOut:all){
			ATAddress add = findAddressInAccounts(unspentOut.getScriptPubKey().getToAddress(getNetworkParams()).toString());
			/*
			 * could happen if an unspent address belongs to a deleted account, it will not find it
			 */
			if(add == null)
				continue;
			
			if(add.getAccountIndex() == accountIndex)
				ret.add(unspentOut);
		}
		return ret;
	}
 
	public ArrayList<TransactionOutput> getUnspentOutputsForAddresses(ArrayList<String> addressArr)
	{
		return getWalletWrapper().getUnspentOutputsForAddresses(addressArr);
	}
	
	public Coin getTxValueSentToMe(Transaction tx){
		return getWalletWrapper().getTxValueSentToMe(tx);
	}
	
	public Coin getTxValueSentFromMe(Transaction tx){
		return getWalletWrapper().getTxValueSentFromMe(tx);
	}
	
	public void decryptWallet(BAPassword password) throws WrongWalletPasswordException {
		if(isWalletEncrypted())
		if(password.hasPassword()){
			try {
				getWalletWrapper().decryptWallet(password.toString());
			}
			catch(KeyCrypterException returnException) { 
				throw new WrongWalletPasswordException("Illegal Password");
			}
			LOG.info("Decrypted wallet");
		}
		else
			throw new WrongWalletPasswordException("Illegal Password");
	}
	
	public void encryptWallet(BAPassword password) throws WrongWalletPasswordException {
		if(!isWalletEncrypted())
		if(password.hasPassword()){
			getWalletWrapper().encryptWallet(password.toString());
			LOG.info("Encrypted wallet");
		}
		else
			throw new WrongWalletPasswordException("Illegal Password");
	}
	
	/**
	 * If pw is not null, will decrypt the wallet, get the seed and encrypt the wallet.
	 * 
	 * @param pw
	 * @return
	 * @throws org.authenticator.walletCore.exceptions.WrongWalletPasswordException
	 */
	public DeterministicSeed getWalletSeed(@Nullable BAPassword pw) throws WrongWalletPasswordException {
		if(isWalletEncrypted() && !pw.hasPassword())
				throw new WrongWalletPasswordException("Wallet is encrypted, cannot get seed");
		DeterministicSeed ret;
		if(isWalletEncrypted()){
			decryptWallet(pw);
			ret = getWalletWrapper().getWalletSeed();
			encryptWallet(pw);
		}
		else
			ret = getWalletWrapper().getWalletSeed();
		return ret;
	}
		
	/**
	 * If pw is not null, will decrypt the wallet, get the seed and encrypt the wallet.
	 * 
	 * @param pw
	 * @return
	 * @throws org.authenticator.walletCore.exceptions.WrongWalletPasswordException
	 */
	 public byte[] getWalletSeedBytes(@Nullable BAPassword pw) throws WrongWalletPasswordException {
	 		return getWalletSeed(pw).getSecretBytes();
	 }
	
	public boolean isWalletEncrypted(){
		return getWalletWrapper().isWalletEncrypted();
	}

	//#####################################
	//
	//		Tx history
	//
	//#####################################
	
	public void writeNextSavedTxData(String txid, String toFrom, String description) throws CannotWriteToConfigurationFileException {
		try {
			getConfigFile().writeNextSavedTxData(txid, toFrom, description);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public void writeSavedTxData(int x, String txid, String toFrom, String description) throws CannotWriteToConfigurationFileException {
		try {
			getConfigFile().writeSavedTxData(x, txid, toFrom, description);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
	
	public ArrayList<String> getSavedTxidList(){
		return getConfigFile().getSavedTxidList();
	}
	
	public String getSavedDescription (int index) {
		return getConfigFile().getSavedDescription(index);
	}
	
	public String getSavedToFrom (int index) {
		return getConfigFile().getSavedToFrom(index);
	}
	
	public ArrayList<Transaction> filterTransactionsByAccount (int accountIndex) throws CannotGetAccountFilteredTransactionsException {
		ArrayList<Transaction> filteredHistory = new ArrayList<Transaction>();
		try {
			ArrayList<String> usedExternalAddressList = getAccountUsedAddressesString(accountIndex, HierarchyAddressTypes.External);
			//ArrayList<String> usedInternalAddressList = getAccountUsedAddressesString(accountIndex, HierarchyAddressTypes.Internal);
			Set<Transaction> fullTxSet = getTrackedWallet().getTransactions(false);
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
	//		Config extensions
	//
	//#####################################

	public void addExtension(ConfigExtension extension) throws IOException {
		configFile.addExtension(extension.getID(), extension.getDescription(), extension.serialize());
	}

	/**
	 * Will return null if extension not found
	 * @param id
	 * @return
	 */
	public ProtoConfig.Extenstion getExtesntion(String id) {
		return configFile.getExtesntion(id);
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

	/**
	 * Will return {@link org.bitcoinj.core.Coin Coin} of the default Tx fee for this wallet.<br>
	 * If extracting the default fee is except, will return {@link org.bitcoinj.core.Transaction#REFERENCE_DEFAULT_MIN_TX_FEE REFERENCE_DEFAULT_MIN_TX_FEE}
	 * @return
	 */
	public Coin getDefaultFeeFromSettings() {
		try {
			return Coin.valueOf(settingsFile.getDefaultFee());
		} catch (Exception e) {
			e.printStackTrace();
			return Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
		}
	}

	/**
	 * pass the default fee in {@link org.bitcoinj.core.Coin#SATOSHI satoshies} satoshies
	 * @param value
	 * @throws CannotWriteToConfigurationFileException
	 */
	public void setDefaultFeeInSettings(long value) throws CannotWriteToConfigurationFileException {
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

	public boolean getShouldBackupToCloud() {
		return settingsFile.getShouldBackupToCloud();
	}

	public void setShouldBackupToCloud(boolean value) throws CannotWriteToConfigurationFileException {
		try {
			settingsFile.setShouldBackupToCloud(value);
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}

	public void resotreSettingsToDefault() throws CannotWriteToConfigurationFileException {
		try {
			getConfigFile().resotreSettingsToDefault();
		} catch (IOException e) {
			throw new CannotWriteToConfigurationFileException(e.getMessage());
		}
	}
}


