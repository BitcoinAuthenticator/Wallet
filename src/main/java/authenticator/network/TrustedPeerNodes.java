package authenticator.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.bitcoin.core.PeerAddress;

public class TrustedPeerNodes {
	private static String[] MAIN_NET = new String[]{
		"riker.plan99.net",
		// IPV6
		"InductiveSoul.US",
		"caffeinator.net",
		"messier.bzfx.net",
		// IPV4
		"bitcoin.coinprism.com",
		"btcnode1.evolyn.net",
		"InductiveSoul.US",
	};
	
	public static PeerAddress[] MAIN_NET() {
		PeerAddress[] ret = new PeerAddress[MAIN_NET.length];
		
		for(int i=0; i< MAIN_NET.length; i++)
			try {
				ret[i] = new PeerAddress(InetAddress.getByName(MAIN_NET[i]));
			} catch (UnknownHostException e) { e.printStackTrace(); }
		
		return ret;
	}
}
