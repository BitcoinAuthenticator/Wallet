package authenticator;

import java.math.BigInteger;

import org.slf4j.Logger;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Wallet;

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
	
	
	public static void addP2ShAddressToWathc(String address) throws AddressFormatException
	{
		addP2ShAddressToWathc(new Address(trackedWallet.getNetworkParameters(),address));
	}
	
	/**
	 * Add A new P2Sh authenticator address to watch list 
	 * @param add
	 */
	public static void addP2ShAddressToWathc(final Address address)
	{
		if(trackedWallet != null)
		{
			trackedWallet.addWatchedAddress(address);
			staticLogger.info("Added New Address To Wallet Watch: " + address.toString());
		}
	}
}
