package org.authenticator.walletCore.utils;

import java.io.IOException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.json.JSONException;
import org.spongycastle.util.encoders.Hex;
import org.authenticator.GCM.dispacher.Device;
import org.authenticator.GCM.dispacher.Dispacher;
import org.authenticator.GCM.exceptions.GCMSendFailedException;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import org.authenticator.walletCore.WalletOperation;

public class CoinsReceivedNotificationSender {
	/**
	 * check if a transaction received coins that are not change, if so, return true<br>
	 */
	static public boolean checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(WalletOperation wo, Transaction tx) {
		if(tx == null)
			return false;
		if(wo == null)
			return false;
		
		Coin enter = wo.getTxValueSentToMe(tx);
		Coin exit = wo.getTxValueSentFromMe(tx);
		// return true only if we receive coins but dont send any. The logic is, if the Tx sends coins from the wallet,
		// most probably and received coins will be change.
		if (exit.compareTo(Coin.ZERO) == 0 && enter.compareTo(Coin.ZERO) > 0) {
			return true;
		} 
		return false;
	}
	
	static public void send(WalletOperation wo, Transaction tx, HowBalanceChanged howBalanceChanged) {
		send(wo, new Dispacher(null,null), tx, howBalanceChanged);
	}
	
	static public void send(WalletOperation wo, Dispacher disp, Transaction tx, HowBalanceChanged howBalanceChanged) {
		if(howBalanceChanged == HowBalanceChanged.ReceivedCoins)
			for (TransactionOutput out : tx.getOutputs()){
				Script scr = out.getScriptPubKey();
				String addrStr = scr.getToAddress(wo.getNetworkParams()).toString();
				if(wo.isWatchingAddress(addrStr)){
					ATAddress add = wo.findAddressInAccounts(addrStr);
					if(add == null)
						continue;
			
					// incoming sum
					Coin receivedSum = out.getValue();
			
					ATAccount account = null;
					try {
						account = wo.getAccount(add.getAccountIndex());
						if(account.getAccountType() != WalletAccountType.AuthenticatorAccount)
							continue;
						PairedAuthenticator pairing = wo.getPairingObjectForAccountIndex(account.getIndex());
						SecretKey secretkey = new SecretKeySpec(Hex.decode(pairing.getAesKey()), "AES");						
						byte[] gcmID = pairing.getGCM().getBytes();
						assert(gcmID != null);
						Device d = new Device(pairing.getChainCode().getBytes(),
								pairing.getMasterPublicKey().getBytes(),
								gcmID,
								pairing.getPairingID().getBytes(),
								secretkey);
				
						System.out.println("Sending a Coins Received Notification");
						disp.dispachMessage(ATGCMMessageType.CoinsReceived, d, new String[]{ "Coins Received: " + receivedSum.toFriendlyString() });	
					} 
					catch (AccountWasNotFoundException | GCMSendFailedException e1) {
						e1.printStackTrace();
					}
				}
			}
	}
}
