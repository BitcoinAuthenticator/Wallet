package authenticator;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.utils.Threading;

/**
 * A wrapper class to handle all operations regarding the bitcoinj wallet. All operations requiring wallet functions done by the authenticator 
 * Should pass here in order to integrate the regular wallet operations with the authenticator added functionality.
 * @author alon
 *
 */
public class WalletWrapper extends BASE{

	private static Logger staticLogger;
	private static Wallet trackedWallet;
	public WalletWrapper(Wallet wallet){
		super(WalletWrapper.class);
		staticLogger = this.LOG;
		this.trackedWallet = wallet;
	}
	
	/**
	 * Add A new P2Sh authenticator address to watch list 
	 * @param add
	 */
	public static void addP2ShAddressToWatch(String address) throws AddressFormatException
	{
		addP2ShAddressToWatch(new Address(trackedWallet.getNetworkParameters(),address));
	}
	public static void addP2ShAddressToWatch(final Address address)
	{
		if(trackedWallet != null)
		{
			trackedWallet.addWatchedAddress(address);
			staticLogger.info("Added New Address To Wallet Watch: " + address.toString());
		}
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

		BigInteger retBalance = null;
		LinkedList<TransactionOutput> allWatchedAddresses = trackedWallet.getWatchedOutputs(false);
		for(TransactionOutput Txout: allWatchedAddresses)
			for(String lookedAddr: addressArr){
				String TxOutAddress = Txout.getScriptPubKey().getToAddress(trackedWallet.getNetworkParameters()).toString();
				if(TxOutAddress.equals(lookedAddr)){
					if(retBalance == null)
						retBalance = Txout.getValue();
					else
						retBalance.add(Txout.getValue());
					break;
				}
			}		
		return retBalance;
	}
	
	public static BigInteger getEstimatedBalance()
	{
		BigInteger walletBalance = trackedWallet.getBalance(Wallet.BalanceType.ESTIMATED);
		return walletBalance;
	}
	
	/**
	 * 
	 * @param addresses
	 * @return
	 */
	public static Wallet.SendRequest createSendRequest(Map<Address,BigInteger> addresses){
		Wallet.SendRequest ret = null;
		// Construct partial send request
		for(Address address : addresses.keySet())
		{
			BigInteger outAmount = addresses.get(address);
			if(ret == null)
				ret = Wallet.SendRequest.to(address, outAmount);
			else
				ret.tx.addOutput(outAmount, address);
		}
				
		return ret;
	}

}
