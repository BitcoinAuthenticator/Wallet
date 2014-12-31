package org.authenticator.db;

import static org.junit.Assert.*;

import org.authenticator.GCM.dispacher.MessageBuilder;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProtoConfig.AuthenticatorConfiguration.Builder.class,
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class,
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.class,
		ProtoConfig.ATAccount.class,
		ProtoConfig.ATAccount.Builder.class,
		ProtoConfig.ATAddress.class,
		ProtoConfig.ATAddress.Builder.class})
public class WalletDBTest {

	@Test
	public void setPairedTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);

		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		try {
			mockedWalletdb.setPaired(true);

			Mockito.verify(mockedAuthWalletBuilder, Mockito.times(1)).setPaired(true);
			Mockito.verify(mockedAuthWalletBuilder, Mockito.times(0)).setPaired(false);
		} catch (IOException e) { e.printStackTrace(); }
	}

	@Test
	public void getPairedTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet mockedAuthWallet =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.class);
		Mockito.doReturn(true).when(mockedAuthWallet).getPaired();

		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWallet()).thenReturn(mockedAuthWallet);

		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		try {
			boolean result = mockedWalletdb.getPaired();
			Mockito.verify(mockedAuthWallet, Mockito.times(1)).getPaired();
			assertTrue(result);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void markAddressAsUsedTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		// getConfigFileBuilder method stub
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}


		// ATAccount stub
		PowerMockito.mockStatic(ProtoConfig.ATAccount.class);
		ProtoConfig.ATAccount mockedAccount = PowerMockito.mock(ProtoConfig.ATAccount.class);
		try { Mockito.doReturn(mockedAccount).when(mockedWalletdb).getAccount(1); } catch (AccountWasNotFoundException e) { e.printStackTrace(); }
		try { Mockito.doThrow(AccountWasNotFoundException.class).when(mockedWalletdb).getAccount(2); } catch (AccountWasNotFoundException e) { e.printStackTrace(); }
		// ATAccount.Builder stub
		PowerMockito.mockStatic(ProtoConfig.ATAccount.Builder.class);
		ProtoConfig.ATAccount.Builder mockedAccountBuilder = PowerMockito.mock(ProtoConfig.ATAccount.Builder.class);
//		Mockito.doNothing().when(mockedAccountBuilder).addUsedExternalKeys(1);
		// return the stubbed builder
		Mockito.when(ProtoConfig.ATAccount.newBuilder(mockedAccount)).thenReturn(mockedAccountBuilder);
		// make sure updateAccount is stubbed
		try { Mockito.doNothing().when(mockedWalletdb).updateAccount(mockedAccount); } catch (IOException e) { e.printStackTrace(); }


		// test external account
		try {
			mockedWalletdb.markAddressAsUsed(1, 1, AuthWalletHierarchy.HierarchyAddressTypes.External);
			Mockito.verify(mockedAccountBuilder, Mockito.times(1)).addUsedExternalKeys(1);
		} catch (IOException | AccountWasNotFoundException e) {
			e.printStackTrace();
		}

		// test account not found
		boolean didFallOnException = false;
		try {
			mockedWalletdb.markAddressAsUsed(2, 1, AuthWalletHierarchy.HierarchyAddressTypes.External);
		} catch (Exception  e) {
			didFallOnException = true;
			if(e instanceof AccountWasNotFoundException)
				assertTrue(true);
			else
				assertTrue(false);
		}
		if(!didFallOnException) assertTrue(false);
	}

}
