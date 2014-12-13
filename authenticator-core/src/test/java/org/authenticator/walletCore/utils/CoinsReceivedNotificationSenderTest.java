package org.authenticator.walletCore.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.protobuf.ByteString;
import org.authenticator.Authenticator;
import org.authenticator.BAApplicationParameters;
import org.authenticator.GCM.dispacher.Device;
import org.authenticator.GCM.dispacher.Dispacher;
import org.authenticator.GCM.exceptions.GCMSendFailedException;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import org.authenticator.protobuf.AuthWalletHierarchy;
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
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
		Transaction tx = Mockito.mock(Transaction.class);
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

		// transaction null
		assertFalse(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(mocked, null));
		// wallet operation null
		assertFalse(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(null, tx));
		// both null
		assertFalse(CoinsReceivedNotificationSender.checkIfNotificationShouldBeSentToPairedDeviceOnReceivedCoins(null, null));
	}

	@Test
	public void sendTest() throws IOException {
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
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
		/**
		 * test send 
		 */
		{
			CoinsReceivedNotificationSender.send(mocked, ds, mockedTx(mocked), HowBalanceChanged.ReceivedCoins);
		}
		
		try {
			ArgumentCaptor<ATGCMMessageType> argMsgType = ArgumentCaptor.forClass(ATGCMMessageType.class);
			ArgumentCaptor<Device> argDevice = ArgumentCaptor.forClass(Device.class);
			ArgumentCaptor<String[]> argStr = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(ds).dispachMessage(argMsgType.capture(),
					argDevice.capture(),
					argStr.capture());

			assertTrue(argMsgType.getValue() == ATGCMMessageType.CoinsReceived);

			assertTrue(Arrays.equals(argDevice.getValue().chaincode, deviceSentTo(getMockedPairedAuthenticator()).chaincode));
			assertTrue(Arrays.equals(argDevice.getValue().mPubKey, deviceSentTo(getMockedPairedAuthenticator()).mPubKey));
			assertTrue(Arrays.equals(argDevice.getValue().gcmRegId, deviceSentTo(getMockedPairedAuthenticator()).gcmRegId));
			assertTrue(Arrays.equals(argDevice.getValue().pairingID, deviceSentTo(getMockedPairedAuthenticator()).pairingID));
			assertTrue(Arrays.equals(argDevice.getValue().sharedsecret.getEncoded(),
					deviceSentTo(getMockedPairedAuthenticator()).sharedsecret.getEncoded()));

//			String[] e = argStr.getValue();
//			assertTrue(e.equals("Coins Received: " + Coin.valueOf(10000).toFriendlyString()));
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}


	}

	@Test
	public void noSendBecauseOfWrongAccountTypeTest() throws IOException {
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
				Mockito.when(mocked.getAccount(1)).thenReturn(getMockedStandardAccount());
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
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		{
			CoinsReceivedNotificationSender.send(mocked, ds, mockedTx(mocked), HowBalanceChanged.ReceivedCoins);
		}

		try {
			ArgumentCaptor<ATGCMMessageType> argMsgType = ArgumentCaptor.forClass(ATGCMMessageType.class);
			ArgumentCaptor<Device> argDevice = ArgumentCaptor.forClass(Device.class);
			ArgumentCaptor<String[]> argStr = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(ds, Mockito.never()).dispachMessage(argMsgType.capture(),
					argDevice.capture(),
					argStr.capture());
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void noSendBecauseAddressNotFoundTest() throws IOException {
		WalletOperation mocked = Mockito.mock(WalletOperation.class);
		{
			Mockito.when(mocked.getNetworkParams()).thenReturn(NetworkParameters.testNet());

			Mockito.when(mocked.isWatchingAddress("address0")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address1")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address2")).thenReturn(true);
			Mockito.when(mocked.isWatchingAddress("address3")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address4")).thenReturn(false);

			Mockito.when(mocked.findAddressInAccounts("address2")).thenReturn(null);

			try {
				Mockito.when(mocked.getAccount(1)).thenReturn(getMockedStandardAccount());
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
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		{
			CoinsReceivedNotificationSender.send(mocked, ds, mockedTx(mocked), HowBalanceChanged.ReceivedCoins);
		}

		try {
			ArgumentCaptor<ATGCMMessageType> argMsgType = ArgumentCaptor.forClass(ATGCMMessageType.class);
			ArgumentCaptor<Device> argDevice = ArgumentCaptor.forClass(Device.class);
			ArgumentCaptor<String[]> argStr = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(ds, Mockito.never()).dispachMessage(argMsgType.capture(),
					argDevice.capture(),
					argStr.capture());
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void noSendBecauseAddressNotWatchedTest() throws IOException {
		WalletOperation mocked = Mockito.mock(WalletOperation.class);
		{
			Mockito.when(mocked.getNetworkParams()).thenReturn(NetworkParameters.testNet());

			Mockito.when(mocked.isWatchingAddress("address0")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address1")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address2")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address3")).thenReturn(false);
			Mockito.when(mocked.isWatchingAddress("address4")).thenReturn(false);

			Mockito.when(mocked.findAddressInAccounts("address2")).thenReturn(null);

			try {
				Mockito.when(mocked.getAccount(1)).thenReturn(getMockedStandardAccount());
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
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		{
			CoinsReceivedNotificationSender.send(mocked, ds, mockedTx(mocked), HowBalanceChanged.ReceivedCoins);
		}

		try {
			ArgumentCaptor<ATGCMMessageType> argMsgType = ArgumentCaptor.forClass(ATGCMMessageType.class);
			ArgumentCaptor<Device> argDevice = ArgumentCaptor.forClass(Device.class);
			ArgumentCaptor<String[]> argStr = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(ds, Mockito.never()).dispachMessage(argMsgType.capture(),
					argDevice.capture(),
					argStr.capture());
		} catch (GCMSendFailedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
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

			lTxOutput.add(out);
		}

		Mockito.when(tx.getOutputs()).thenReturn(lTxOutput);
		
		return tx;
	}
	
	private ATAddress mockedATAddress() {
		ATAddress.Builder atAdd = ATAddress.newBuilder();
		atAdd.setAccountIndex(1);
		atAdd.setKeyIndex(0);
		atAdd.setType(AuthWalletHierarchy.HierarchyAddressTypes.External);
		atAdd.setAddressStr("address");
		return atAdd.build();
	}
	
	private ATAccount getMockedAccount() {
		ATAccount.Builder b = ATAccount.newBuilder();
			b.setIndex(1);
			b.setConfirmedBalance(0);
			b.setUnConfirmedBalance(0);
			b.setNetworkType(1);
			b.setAccountName("name");
			b.setAccountType(WalletAccountType.AuthenticatorAccount);

			ATAccount.ATAccountAddressHierarchy.Builder b2 = ATAccount.ATAccountAddressHierarchy.newBuilder();
			b2.setHierarchyChaincode(ByteString.copyFrom("".getBytes()));
			b2.setHierarchyKey(ByteString.copyFrom("".getBytes()));
			b.setAccountExternalHierarchy(b2.build());
			b.setAccountInternalHierarchy(b2.build());
		return b.build();
	}

	private ATAccount getMockedStandardAccount() {
		ATAccount.Builder b = ATAccount.newBuilder();
		b.setIndex(1);
		b.setConfirmedBalance(0);
		b.setUnConfirmedBalance(0);
		b.setNetworkType(1);
		b.setAccountName("name");
		b.setAccountType(WalletAccountType.StandardAccount);

		ATAccount.ATAccountAddressHierarchy.Builder b2 = ATAccount.ATAccountAddressHierarchy.newBuilder();
		b2.setHierarchyChaincode(ByteString.copyFrom("".getBytes()));
		b2.setHierarchyKey(ByteString.copyFrom("".getBytes()));
		b.setAccountExternalHierarchy(b2.build());
		b.setAccountInternalHierarchy(b2.build());
		return b.build();
	}
	
	private PairedAuthenticator getMockedPairedAuthenticator() {
		PairedAuthenticator.Builder newPair = PairedAuthenticator.newBuilder();
			String hexSk = "A2F72940109899C1708511B4867727E507299E276B2E44B5D48BBFE8689C17F0";
			newPair.setAesKey(hexSk);
			String masterPubKey = "i am the the master public key";
			newPair.setMasterPublicKey(masterPubKey);
			String chainCode = "i am the the chain code";
			newPair.setChainCode(chainCode);
			String gcm = "i am the gcm registration id hex";
			newPair.setGCM(gcm);
			String pairingId = "i am the the pairing id";
			newPair.setPairingID(pairingId);
			newPair.setTestnet(false);
			newPair.setKeysN(0);
			newPair.setWalletAccountIndex(1);
		return newPair.build();
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
