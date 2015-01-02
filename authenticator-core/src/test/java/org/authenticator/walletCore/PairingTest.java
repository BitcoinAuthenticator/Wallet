package org.authenticator.walletCore;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.interfaces.ECKey;
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
import org.authenticator.db.exceptions.PairingObjectWasNotFoundException;
import org.authenticator.db.walletDB;
import org.authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import org.authenticator.walletCore.exceptions.NoWalletPasswordException;
import org.authenticator.walletCore.utils.BAPassword;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.spongycastle.util.encoders.Hex;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*" })
@PrepareForTest({ProtoConfig.PairedAuthenticator.Builder.class,
					ProtoConfig.PairedAuthenticator.class})
public class PairingTest {

	@Test
	public void updatePairingGCMRegistrationIDTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());
		walletDB mockedWalletdb = Mockito.mock(walletDB.class);
		Mockito.doReturn(mockedWalletdb).when(woMocked).getConfigFile();

		try {
			woMocked.updatePairingGCMRegistrationID("pairing ID", "new GCM");

			Mockito.verify(mockedWalletdb, Mockito.times(1)).updatePairingGCMRegistrationID("pairing ID", "new GCM");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void getPairingObjectForAccountIndexTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());
		walletDB mockedWalletdb = Mockito.mock(walletDB.class);
		Mockito.doReturn(mockedWalletdb).when(woMocked).getConfigFile();

		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for(int i = 0; i < 100; i ++) {
				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i).when(newPair).getWalletAccountIndex();

				all.add(newPair);
			}
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// test normal
		{
			ProtoConfig.PairedAuthenticator ret = woMocked.getPairingObjectForAccountIndex(4);
			assertTrue(ret.getWalletAccountIndex() == 4);

			ret = woMocked.getPairingObjectForAccountIndex(10);
			assertTrue(ret.getWalletAccountIndex() == 10);

			ret = woMocked.getPairingObjectForAccountIndex(1);
			assertTrue(ret.getWalletAccountIndex() == 1);

			ret = woMocked.getPairingObjectForAccountIndex(87);
			assertTrue(ret.getWalletAccountIndex() == 87);

			ret = woMocked.getPairingObjectForAccountIndex(56);
			assertTrue(ret.getWalletAccountIndex() == 56);

			ret = woMocked.getPairingObjectForAccountIndex(32);
			assertTrue(ret.getWalletAccountIndex() == 32);
		}

		// test returns null for non existing account
		ProtoConfig.PairedAuthenticator ret = woMocked.getPairingObjectForAccountIndex(400);
		assertTrue(ret == null);

		ret = woMocked.getPairingObjectForAccountIndex(2300);
		assertTrue(ret == null);

		ret = woMocked.getPairingObjectForAccountIndex(13214);
		assertTrue(ret == null);

		ret = woMocked.getPairingObjectForAccountIndex(455521);
		assertTrue(ret == null);
	}

	@Test
	public void getAccountIndexForPairingTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());
		walletDB mockedWalletdb = Mockito.mock(walletDB.class);
		Mockito.doReturn(mockedWalletdb).when(woMocked).getConfigFile();

		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for (Integer i = 0; i < 100; i++) {
				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i.toString()).when(newPair).getPairingID();
				Mockito.doReturn(i + 100).when(newPair).getWalletAccountIndex();

				all.add(newPair);
			}
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// test normal
		{
			int ret = woMocked.getAccountIndexForPairing("12");
			assertTrue(ret == 12 + 100);

			ret = woMocked.getAccountIndexForPairing("2");
			assertTrue(ret == 2 + 100);

			ret = woMocked.getAccountIndexForPairing("45");
			assertTrue(ret == 45 + 100);

			ret = woMocked.getAccountIndexForPairing("87");
			assertTrue(ret == 87 + 100);

			ret = woMocked.getAccountIndexForPairing("0");
			assertTrue(ret == 0 + 100);
		}

		// test non existing accounts
		{
			int ret = woMocked.getAccountIndexForPairing("122134");
			assertTrue(ret == -1);

			ret = woMocked.getAccountIndexForPairing("1234");
			assertTrue(ret == -1);

			ret = woMocked.getAccountIndexForPairing("567846");
			assertTrue(ret == -1);

			ret = woMocked.getAccountIndexForPairing("12343");
			assertTrue(ret == -1);

			ret = woMocked.getAccountIndexForPairing("55555");
			assertTrue(ret == -1);

			ret = woMocked.getAccountIndexForPairing("9999");
			assertTrue(ret == -1);
		}
	}

	@Test
	public void getPublicKeyAndChainTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());
		walletDB mockedWalletdb = Mockito.mock(walletDB.class);
		Mockito.doReturn(mockedWalletdb).when(woMocked).getConfigFile();

		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for (Integer i = 0; i < 100; i++) {
				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i.toString()).when(newPair).getPairingID();
				Mockito.doReturn(i + 100).when(newPair).getWalletAccountIndex();
				Mockito.doReturn("master pub key " + i).when(newPair).getMasterPublicKey();
				Mockito.doReturn("chain code " + i).when(newPair).getChainCode();

				all.add(newPair);
			}
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> ret = woMocked.getPublicKeyAndChain("4");
		assertTrue(ret.get(0).equals("master pub key 4"));
		assertTrue(ret.get(1).equals("chain code 4"));

		ret = woMocked.getPublicKeyAndChain("66");
		assertTrue(ret.get(0).equals("master pub key 66"));
		assertTrue(ret.get(1).equals("chain code 66"));

		ret = woMocked.getPublicKeyAndChain("32");
		assertTrue(ret.get(0).equals("master pub key 32"));
		assertTrue(ret.get(1).equals("chain code 32"));

		ret = woMocked.getPublicKeyAndChain("99");
		assertTrue(ret.get(0).equals("master pub key 99"));
		assertTrue(ret.get(1).equals("chain code 99"));
	}

	@Test
	public void getPairedAuthenticatorKeyTest() {
		String mPubKeyHex = "03cf9ddc67974db8bfecb4a4603ce7e4771cee26fbf2726bfd464c3623c41404d9";
		String chaincodeHex = "3c54994e18a1bc13d74a81a63615f43bb098a4d46e9f4c16fed3b35275cd81a5";
		String expectedKeyIdx10 = "035660d1238380bcc6577b114793d4ec433323f0d984a153c5ffdf422241ceb7eb";
		String expectedKeyIdx20 = "03e8af7285a1ba8c5308fc7fe47bb2e0c514d8bb2cf3fd54ca669eb1ddabbde56b";

		// mocking
		WalletOperation woMocked = Mockito.spy(new WalletOperation());

		// pub and chain method
		List<String> pubKeyAndChain = new ArrayList<String>();
		pubKeyAndChain.add(mPubKeyHex);
		pubKeyAndChain.add(chaincodeHex);
		Mockito.doReturn(pubKeyAndChain).when(woMocked).getPublicKeyAndChain("1");

		// passed pairing object
		PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
		ProtoConfig.PairedAuthenticator pair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
		Mockito.doReturn("1").when(pair).getPairingID();

		// test
		org.bitcoinj.core.ECKey ret10 = woMocked.getPairedAuthenticatorKey(pair, 10);
		assertTrue(Arrays.equals(ret10.getPubKey(), Hex.decode(expectedKeyIdx10)));

		org.bitcoinj.core.ECKey ret20 = woMocked.getPairedAuthenticatorKey(pair, 20);
		assertTrue(Arrays.equals(ret20.getPubKey(), Hex.decode(expectedKeyIdx20)));

		org.bitcoinj.core.ECKey retNull = woMocked.getPairedAuthenticatorKey(pair, -1);
		assertTrue(retNull == null);
	}

	@Test
	public void getKeyNumTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());

		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for (Integer i = 0; i < 100; i++) {
				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i.toString()).when(newPair).getPairingID();
				Mockito.doReturn(i * 2).when(newPair).getKeysN();

				all.add(newPair);
			}
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		long ret = woMocked.getKeyNum("34");
		assertTrue(ret == 34 *2);

		ret = woMocked.getKeyNum("12");
		assertTrue(ret == 12 *2);

		ret = woMocked.getKeyNum("22");
		assertTrue(ret == 22 *2);

		ret = woMocked.getKeyNum("1");
		assertTrue(ret == 1 *2);

		ret = woMocked.getKeyNum("0");
		assertTrue(ret == 0 *2);

		ret = woMocked.getKeyNum("87");
		assertTrue(ret == 87 *2);
	}

	@Test
	public void getAESEncryptedKeyTest() {
		String baseDecryptedAESKey = "i am the AES key number ";
		String saltBase = "i am the salt ";
		byte[] seed = "i am the seed".getBytes();
		BAPassword password = new BAPassword("password");


		WalletOperation woMocked = Mockito.spy(new WalletOperation());
		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for (Integer i = 0; i < 100; i++) {
				// mock wallet operation method
				Integer accIdx = i * 2;
				Mockito.doReturn(accIdx).when(woMocked).getAccountIndexForPairing(i.toString());

				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i.toString()).when(newPair).getPairingID();
				Mockito.doReturn(true).when(newPair).getIsEncrypted();

				String salt = saltBase + i;
				// generate key
				byte[] encryptedAESBytes = CryptoUtils.authenticatorAESEncryption(Hex.toHexString((baseDecryptedAESKey + i).getBytes()),
																				Hex.toHexString(salt.getBytes()),
																				accIdx.toString(),
																				Hex.toHexString(seed));
				String encryptedAESHex = Hex.toHexString(encryptedAESBytes);
				Mockito.doReturn(encryptedAESHex).when(newPair).getAesKey();
				// salt
				Mockito.doReturn(ByteString.copyFrom(salt.getBytes())).when(newPair).getKeySalt();

				all.add(newPair);
			}

			// mock general wallet operation methods
			Mockito.doReturn(seed).when(woMocked).getWalletSeedBytes(password);
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//test
		try {
			String decryptedAES = woMocked.getAESKey("12", password);
			assertTrue(decryptedAES.equals(Hex.toHexString((baseDecryptedAESKey + 12).getBytes())));

			decryptedAES = woMocked.getAESKey("11", password);
			assertTrue(decryptedAES.equals(Hex.toHexString((baseDecryptedAESKey + 11).getBytes())));

			decryptedAES = woMocked.getAESKey("33", password);
			assertTrue(decryptedAES.equals(Hex.toHexString((baseDecryptedAESKey + 33).getBytes())));

			decryptedAES = woMocked.getAESKey("89", password);
			assertTrue(decryptedAES.equals(Hex.toHexString((baseDecryptedAESKey + 89).getBytes())));

			decryptedAES = woMocked.getAESKey("0", password);
			assertTrue(decryptedAES.equals(Hex.toHexString((baseDecryptedAESKey + 0).getBytes())));
		} catch (NoWalletPasswordException | CryptoUtils.CannotDecryptMessageException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		// return null of account not found
		try {
			String decryptedAES = woMocked.getAESKey("1200", password);
			assertTrue(decryptedAES == null);

			decryptedAES = woMocked.getAESKey("2345", password);
			assertTrue(decryptedAES == null);

			decryptedAES = woMocked.getAESKey("57645", password);
			assertTrue(decryptedAES == null);

			decryptedAES = woMocked.getAESKey("1233", password);
			assertTrue(decryptedAES == null);
		} catch (NoWalletPasswordException | CryptoUtils.CannotDecryptMessageException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void getPairingObjectTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());

		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for (Integer i = 0; i < 100; i++) {
				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i.toString()).when(newPair).getPairingID();

				all.add(newPair);
			}
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ProtoConfig.PairedAuthenticator res = woMocked.getPairingObject("2");
		assertTrue(res.getPairingID().equals("2"));

		res = woMocked.getPairingObject("12");
		assertTrue(res.getPairingID().equals("12"));

		res = woMocked.getPairingObject("43");
		assertTrue(res.getPairingID().equals("43"));

		res = woMocked.getPairingObject("58");
		assertTrue(res.getPairingID().equals("58"));
	}

	@Test
	public void getPairingIDsTest() {
		WalletOperation woMocked = Mockito.spy(new WalletOperation());

		// mock pairing objects
		try {
			List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
			for (Integer i = 0; i < 100; i++) {
				PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
				ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
				Mockito.doReturn(i.toString()).when(newPair).getPairingID();

				all.add(newPair);
			}
			Mockito.doReturn(all).when(woMocked).getAllPairingObjectArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayList<String> res = woMocked.getPairingIDs();
		assertTrue(res.size() == 100);
		int count = 0;
		for(String i: res) {
			assertTrue(Integer.parseInt(i) == count);
			count++;
		}
	}
}