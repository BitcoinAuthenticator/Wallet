package authenticator;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
 * as an intermediary layer between the {@link authenticator.WalletOperation} class and bitcoinj's {@link com.google.bitcoin.core.Wallet} class
 * </p>
 * 
 * <br>
 * @author alon
 *
 */
public class WalletWrapper extends BASE{

	private Wallet trackedWallet;
	private PeerGroup mPeerGroup;
	public WalletWrapper(Wallet wallet, PeerGroup peerGroup){
		super(WalletWrapper.class);
		this.trackedWallet = wallet;
		this.mPeerGroup = peerGroup;
	}
	public  Wallet getTrackedWallet(){ return trackedWallet; }
	
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
	
	public Coin getPendingWatchedTransactionsBalacnce(ArrayList<String> addressArr){
		ArrayList<TransactionOutput> watched = getUnspentOutputsForAddresses(addressArr);
		Collection<Transaction> allPending = trackedWallet.getPendingTransactions();
		Coin ret = Coin.ZERO;
		for (Iterator<Transaction> iterator = allPending.iterator(); iterator.hasNext();){
			Transaction tx  = iterator.next();
			for (TransactionOutput output : tx.getOutputs()){
				// check pendgin tx output is addressArr list
				String add = output.getScriptPubKey().getToAddress(trackedWallet.getNetworkParameters()).toString();
                if (!addressArr.contains(add)) continue;
                // add amount
                ret = ret.add(output.getValue());
			}			
		}
			
		return ret;
	}
	
	/**
	 * Will Check with wallet's watched addresses for a total balance. A use case will be to pass all the addresses of a single 
	 * authenticator pairing to get the total unspent balance.
	 * 
	 * @param addressArr
	 * @return balance of specific addresses as a BigInteger{@link java.math.BigInteger}
	 * @throws ScriptException
	 * @throws UnsupportedEncodingException
	 */
	public Coin getEstimatedBalanceOfWatchedAddresses(ArrayList<String> addressArr)
	{
		Coin retBalance = Coin.ZERO;
		LinkedList<TransactionOutput> allWatchedAddresses = trackedWallet.getWatchedOutputs(false);
		for(TransactionOutput Txout: allWatchedAddresses)
			for(String lookedAddr: addressArr){
				String TxOutAddress = Txout.getScriptPubKey().getToAddress(trackedWallet.getNetworkParameters()).toString();
				if(TxOutAddress.equals(lookedAddr)){
					retBalance = retBalance.add(Txout.getValue());
					break;
				}
			}		
		return retBalance;
	}
	
	//#####################################
	//
	//		Transaction outputs
	//
	//#####################################
	
	public LinkedList<TransactionOutput> getWatchedOutputs(){
		return trackedWallet.getWatchedOutputs(false);
	}
	
	public ArrayList<TransactionOutput> getUnspentOutputsForAddresses(ArrayList<String> addressArr)
	{
		LinkedList<TransactionOutput> allWatchedAddresses = getWatchedOutputs();
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

	public ArrayList<TransactionOutput> selectOutputs(Coin value, ArrayList<TransactionOutput> candidates)
	{
		LinkedList<TransactionOutput> outs = new LinkedList<TransactionOutput> (candidates);
		DefaultCoinSelector selector = new DefaultCoinSelector();
		CoinSelection cs = selector.select(value, outs);
		Collection<TransactionOutput> gathered = cs.gathered;
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>(gathered);
		
		
		/*//TODO some kind of coin selection
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>();
		Coin amount = Coin.ZERO;
		for(TransactionOutput out: candidates)
		{
			if(amount.compareTo(value) < 0 ){
				amount = amount.add(out.getValue());
			}
			else break;
			ret.add(out);
		}*/
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
	
	//#####################################
	//
	//		other
	//
	//#####################################
	
	public NetworkParameters getNetworkParams()
	{
		return trackedWallet.getNetworkParameters();
	}
}
