package authenticator;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionBroadcaster;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;

/**
 * A wrapper class to handle all operations regarding the bitcoinj wallet. All operations requiring wallet functions done by the authenticator 
 * Should pass here in order to integrate the regular wallet operations with the authenticator added functionality.
 * @author alon
 *
 */
public class WalletWrapper extends Wallet{

	private Wallet trackedWallet;
	private PeerGroup mPeerGroup;
	public WalletWrapper(Wallet wallet, PeerGroup peerGroup){
		super(wallet.getNetworkParameters());
		this.trackedWallet = wallet;
		this.mPeerGroup = peerGroup;
	}
	public  Wallet getTrackedWallet(){ return trackedWallet; }
	
	/**
	 * Add A new P2Sh authenticator address to watch list 
	 * @param add
	 */
	public void addP2ShAddressToWatch(String address) throws AddressFormatException
	{
		addP2ShAddressToWatch(new Address(trackedWallet.getNetworkParameters(),address));
	}
	public void addP2ShAddressToWatch(final Address address)
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
	
	/**
	 * Will Check with wallet's watched addresses for a total balance. A use case will be to pass all the addresses of a single 
	 * authenticator pairing to get the total unspent balance.
	 * 
	 * @param addressArr
	 * @return balance of specific addresses as a BigInteger{@link java.math.BigInteger}
	 * @throws ScriptException
	 * @throws UnsupportedEncodingException
	 */
	public BigInteger getBalanceOfWatchedAddresses(ArrayList<String> addressArr) throws ScriptException, UnsupportedEncodingException
	{
		BigInteger retBalance = BigInteger.ZERO;
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
	
	public BigInteger getEstimatedBalance()
	{
		BigInteger walletBalance = trackedWallet.getBalance(Wallet.BalanceType.ESTIMATED);
		return walletBalance;
	}
	
	public ArrayList<TransactionOutput> getUnspentOutputsForAddresses(ArrayList<String> addressArr)
	{
		LinkedList<TransactionOutput> allWatchedAddresses = trackedWallet.getWatchedOutputs(false);
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

	public ArrayList<TransactionOutput> selectOutputs(BigInteger value, ArrayList<TransactionOutput> candidates)
	{
		//TODO some kind of coin selection
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>();
		BigInteger amount = BigInteger.ZERO;
		for(TransactionOutput out: candidates)
		{
			if(amount.compareTo(value) < 0){
				amount = amount.add(out.getValue());
			}
			else break;
			ret.add(out);
		}
		return ret;
	}
	
	public NetworkParameters getNetworkParams()
	{
		return trackedWallet.getNetworkParameters();
	}
	
	public SendResult broadcastTrabsactionFromWallet(Transaction tx) throws InsufficientMoneyException
	{
		trackedWallet.commitTx(tx);
		TransactionBroadcaster tb;
		SendResult result = new SendResult();
        result.tx = tx;
        result.broadcastComplete =  mPeerGroup.broadcastTransaction(tx);
        return result;
	}
}