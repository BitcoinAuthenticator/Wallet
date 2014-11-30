package org.authenticator.walletCore.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.authenticator.Authenticator;
import org.authenticator.BAApplicationParameters;
import org.authenticator.GCM.dispacher.Device;
import org.authenticator.GCM.dispacher.Dispacher;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import org.authenticator.walletCore.WalletOperation;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.spongycastle.util.encoders.Hex;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ATAddress.class, ATAccount.class, PairedAuthenticator.class})
public class CoinsReceivedNotificationSenderTest {

	@Test
	public void checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoinsTest() {
		// received zero --- sent zero
		WalletOperation mocked = Mockito.mock(WalletOperation.class);
		Transaction tx = null;
		Mockito.when(mocked.getTxValueSentToMe(tx)).thenReturn(Coin.ZERO);
		Mockito.when(mocked.getTxValueSentFromMe(tx)).thenReturn(Coin.ZERO);
		assertFalse(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(mocked, tx));
		
		// received some --- sent zero
		Mockito.when(mocked.getTxValueSentToMe(tx)).thenReturn(Coin.valueOf(10000));
		Mockito.when(mocked.getTxValueSentFromMe(tx)).thenReturn(Coin.ZERO);
		assertTrue(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(mocked, tx));
		
		// received some --- sent some
		Mockito.when(mocked.getTxValueSentToMe(tx)).thenReturn(Coin.valueOf(10000));
		Mockito.when(mocked.getTxValueSentFromMe(tx)).thenReturn(Coin.valueOf(10000));
		assertFalse(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(mocked, tx));
		
		// received zero --- sent some
		Mockito.when(mocked.getTxValueSentToMe(tx)).thenReturn(Coin.ZERO);
		Mockito.when(mocked.getTxValueSentFromMe(tx)).thenReturn(Coin.valueOf(10000));
		assertFalse(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(mocked, tx));
	}

	@Test
	@Ignore
	public void sendTest() {
		WalletOperation mocked = Mockito.mock(WalletOperation.class);
		{
			Mockito.when(mocked.getNetworkParams()).thenReturn(NetworkParameters.testNet());
		
			Mockito.when(mocked.isWatchingAddress("address0")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address1")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address2")).thenReturn(true);
			Mockito.when(mocked.isWatchingAddress("address3")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address4")).thenReturn(false);
			
			Mockito.when(mocked.findAddressInAccounts("address2")).thenReturn(mockedATAddress());
			
			try {
				Mockito.when(mocked.getAccount(1)).thenReturn(getMockedAccount());
			} catch (AccountWasNotFoundException e) {
				e.printStackTrace();
				assertTrue(false);
			}
			
			Mockito.when(mocked.getPairingObjectForAccountIndex(1)).thenReturn(getMockedPairedAuthenticator());
		}
		
		Dispacher ds = Mockito.mock(Dispacher.class);
		try {
			Mockito.when(
					ds.dispachMessage(ATGCMMessageType.CoinsReceived, 
							deviceSentTo(getMockedPairedAuthenticator()), 
							new String[]{ "Coins Received: " + Coin.valueOf(10000).toFriendlyString() })).thenReturn("");
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
		/**
		 * test send 
		 */
		{
			CoinsReceivedNotificationSender.send(mocked, ds, mockedTx(mocked), HowBalanceChanged.ReceivedCoins);
		}
		
//		try {
//			Mockito.verify(ds).dispachMessage(ATGCMMessageType.CoinsReceived, 
//								deviceSentTo(getMockedPairedAuthenticator()), 
//								new String[]{ "Coins Received: " + Coin.valueOf(10000).toFriendlyString() });
//		} catch (JSONException | IOException e) {
//			e.printStackTrace();
//			assertTrue(false);
//		}
	}
	
	private Transaction mockedTx(WalletOperation wo) {
		Transaction tx = Mockito.mock(Transaction.class);	
		List<TransactionOutput> lTxOutput = new ArrayList<TransactionOutput>();
		for(int i=0; i<5; i++) {
			TransactionOutput out = Mockito.mock(TransactionOutput.class);	
			Mockito.when(out.getValue()).thenReturn(Coin.valueOf(10000));
			
			Script scr = Mockito.mock(Script.class);	
			Mockito.when(out.getScriptPubKey()).thenReturn(scr);
			
			Address add = Mockito.mock(Address.class);
			Mockito.when(add.toString()).thenReturn("address" + i);
			
			Mockito.when(scr.getToAddress(wo.getNetworkParams())).thenReturn(add);
			
			tx.addOutput(out);
		}
		
		return tx;
	}
	
	private ATAddress mockedATAddress() {
		ATAddress add = PowerMockito.mock(ATAddress.class);	
		Mockito.when(add.getAccountIndex()).thenReturn(1);
		return add;
	}
	
	private ATAccount getMockedAccount() {
		ATAccount acc = PowerMockito.mock(ATAccount.class);	
		Mockito.when(acc.getAccountType()).thenReturn(WalletAccountType.AuthenticatorAccount);
		Mockito.when(acc.getIndex()).thenReturn(1);
		return acc;
	}
	
	private PairedAuthenticator getMockedPairedAuthenticator() {
		PairedAuthenticator po = PowerMockito.mock(PairedAuthenticator.class);	
		
		String hexSk = "A2F72940109899C1708511B4867727E507299E276B2E44B5D48BBFE8689C17F0";
		Mockito.when(po.getAesKey()).thenReturn(hexSk);
		
		String gcm = "i am the gcm registration id hex";
		Mockito.when(po.getGCM()).thenReturn(gcm);
		
		String chainCode = "i am the the chain code";
		Mockito.when(po.getChainCode()).thenReturn(chainCode);
		
		String masterPubKey = "i am the the master public key";
		Mockito.when(po.getMasterPublicKey()).thenReturn(masterPubKey);
		
		String pairingId = "i am the the pairing id";
		Mockito.when(po.getPairingID()).thenReturn(pairingId);
		
		return po;
	}
	
	private Device deviceSentTo(PairedAuthenticator pairing) {
		SecretKey secretkey = new SecretKeySpec(Hex.decode(pairing.getAesKey()), "AES");						
		byte[] gcmID = pairing.getGCM().getBytes();
		Device d = new Device(pairing.getChainCode().getBytes(),
				pairing.getMasterPublicKey().getBytes(),
				gcmID,
				pairing.getPairingID().getBytes(),
				secretkey);
		
		return d;
	}
}
