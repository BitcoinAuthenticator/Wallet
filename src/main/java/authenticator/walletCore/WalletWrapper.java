package authenticator.walletCore;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import authenticator.BASE;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionBroadcaster;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.wallet.CoinSelection;
import com.google.bitcoin.wallet.DefaultCoinSelector;

/**
 * <p>A wrapper class to handle all operations regarding the bitcoinj wallet.</p>
 * 
 * <p><b>A normal bitcoinj wallet</b><br>
 * This template wallet operates as a normal bitcoinj wallet, 
 * thats why we created this wrapper so we could access all info and data that a normal bitcoinj wallet has.</p>
 * 
 * <p><b>Integration with the Authenticator app</b><br>
 * Alot of out Authenticator operations depend on an underlying wallet operation, for that, we use this class
 * as an intermediary layer between the {@link authenticator.walletCore.WalletOperation} class and bitcoinj's {@link com.google.bitcoin.core.Wallet} class
 * </p>
 * 
 * <br>
 * @author alon
 *
 */
public class WalletWrapper extends BASE{

	public Wallet trackedWallet;
	private PeerGroup mPeerGroup;
	/**
	 * If PeerGroup is Null, no broadcasting function is available
	 * 
	 * @param wallet
	 * @param peerGroup
	 */
	public WalletWrapper(Wallet wallet, @Nullable PeerGroup peerGroup){
		super(WalletWrapper.class);
		this.trackedWallet = wallet;
		this.mPeerGroup = peerGroup;
	}
	public  Wallet getTrackedWallet(){ return trackedWallet; }
	public void setTrackedWallet(Wallet wallet){
		this.trackedWallet = wallet;
	}
	
	public NetworkParameters getNetworkParameters(){
		return trackedWallet.getNetworkParameters();
	}
	
	//#####################################
	//
	//		Addresses
	//
	//#####################################
	
	/**
	 * Add A new P2Sh authenticator address to watch list 
	 * @param add
	 */
	public void addAddressToWatch(String address) throws AddressFormatException
	{
		addAddressToWatch(new Address(trackedWallet.getNetworkParameters(),address));
	}
	
	public void addAddressToWatch(final Address address)
	{
		trackedWallet.addWatchedAddress(address);
	}
	
	public void addAddressesStringToWatch(List<String> addresses) throws AddressFormatException
	{
		List<Address> lst = new ArrayList<Address>();
		for(String add: addresses)
			if(!isAuthenticatorAddressWatched(add))
				lst.add(new Address(trackedWallet.getNetworkParameters(),add));
		addAddressesToWatch(lst);
	}
	
	public void addAddressesToWatch(final List<Address> addresses)
	{
		trackedWallet.addWatchedAddresses(addresses, Utils.currentTimeSeconds());
	}
	
	public boolean isAuthenticatorAddressWatched(String address) throws AddressFormatException{
		return isAuthenticatorAddressWatched(new Address(trackedWallet.getNetworkParameters(),address));
	}
	public boolean isAuthenticatorAddressWatched(Address address)
	{
		return trackedWallet.isAddressWatched(address);
	}
	
	public boolean isTransactionOutputMine(TransactionOutput out)
	{
		return out.isMine(trackedWallet);
	}
	
	
	
	//#####################################
	//
	//		Transaction outputs
	//
	//#####################################
	
	public List<TransactionOutput> getWatchedOutputs(){
		trackedWallet.allowSpendingUnconfirmedTransactions();
		return trackedWallet.getWatchedOutputs(false);
	} 
	
	public ArrayList<TransactionOutput> getUnspentOutputsForAddresses(ArrayList<String> addressArr)
	{
		List<TransactionOutput> allWatchedAddresses = getWatchedOutputs();
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>();
		for(TransactionOutput Txout: allWatchedAddresses)
			for(String lookedAddr: addressArr){
				String TxOutAddress = Txout.getScriptPubKey().getToAddress(trackedWallet.getNetworkParameters()).toString();
				if(TxOutAddress.equals(lookedAddr)){
					ret.add(Txout);
				}
			}
		return ret;
	}
		
	//#####################################
	//
	//		Broadcasting
	//
	//#####################################
	
	public Wallet.SendResult broadcastTrabsactionFromWallet(Transaction tx) throws InsufficientMoneyException
	{
		trackedWallet.commitTx(tx);
		TransactionBroadcaster tb;
		Wallet.SendResult result = new Wallet.SendResult();
        result.tx = tx;
        result.broadcastComplete =  mPeerGroup.broadcastTransaction(tx);
        return result;
	}
	
	public ECKey findKeyFromPubHash(byte[] pubkeyHash){
		return trackedWallet.findKeyFromPubHash(pubkeyHash);
	}
	
	public SendResult sendCoins(Wallet.SendRequest req) throws InsufficientMoneyException{
		return trackedWallet.sendCoins(req);
	}
	
	//#####################################
	//
	//		Listeners
	//
	//#####################################
	
	public void addEventListener(WalletEventListener listener)
	{
		trackedWallet.addEventListener(listener);
	}
	
	//#####################################
	//
	//		Transactions
	//
	//#####################################
	
	public List<Transaction> getRecentTransactions(){
		List<Transaction> ret = trackedWallet.getRecentTransactions(10, false);
		
		return ret;
	}
	
	public Coin getTxValueSentToMe(Transaction tx){
		return tx.getValueSentToMe(trackedWallet);
	}
	
	public Coin getTxValueSentFromMe(Transaction tx){
		return tx.getValueSentFromMe(trackedWallet);
	}
	
	//#####################################
	//
	//		other
	//
	//#####################################
	
	public NetworkParameters getNetworkParams()
	{
		return trackedWallet.getNetworkParameters();
	}
	
	public byte[] getWalletSeed(){
		return trackedWallet.getKeyChainSeed().getSecretBytes();
	}
	
	//#####################################
	//
	//		wallet encryption/ decryption
	//
	//#####################################
	
	public void decryptWallet(String password){
		trackedWallet.decrypt(password);
	}
	
	public void encryptWallet(String password){
		trackedWallet.encrypt(password);
	}
	
	public boolean isWalletEncrypted(){
		return trackedWallet.isEncrypted();
	}
}
