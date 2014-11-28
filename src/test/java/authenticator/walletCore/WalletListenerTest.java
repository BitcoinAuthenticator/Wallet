package authenticator.walletCore;

import static org.junit.Assert.*;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.junit.Test;
import org.mockito.Mockito;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.listeners.BAGeneralEventsAdapter;
import authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import authenticator.walletCore.WalletOperation.BAOperationState;

public class WalletListenerTest {

	boolean wasAuthenticatorOnBalanceChangedCalledAndOK = false;
	@Test
	public void onWalletChangeTest() {		
		// ## mock wallet operation 
		WalletOperation mockWalletOperation = Mockito.mock(WalletOperation.class);
		Mockito.when(mockWalletOperation.getOperationalState()).thenReturn(BAOperationState.NOT_SYNCED);
		
		// ## mock wallet
		Wallet mockWallet = Mockito.mock(Wallet.class);
		
		//
		new Authenticator(new BAApplicationParameters());
		Authenticator.addGeneralEventsListener(new BAGeneralEventsAdapter() {
			@Override
			public void onBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence) {
				if(tx == null && howBalanceChanged == null && confidence == null)
					wasAuthenticatorOnBalanceChangedCalledAndOK = true;
			}
		});
		
		WalletListener wl = new WalletListener(mockWalletOperation);
		wl.onWalletChanged(mockWallet);
		
		/**
		 * check when operational state is NOT_SYNCED
		 */
		
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					if(wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
		
		/**
		 * check when operational state is READY_AND_OPERATIONAL
		 */
		Mockito.when(mockWalletOperation.getOperationalState()).thenReturn(BAOperationState.READY_AND_OPERATIONAL);
		wl.onWalletChanged(mockWallet);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					Authenticator.disposeOfAuthenticator();
					if(!wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
	}
	
	@Test
	public void onCoinsSentTest() {
		// ## mock wallet operation 
		WalletOperation mockWalletOperation = Mockito.mock(WalletOperation.class);
		
		// ## mock wallet
		Wallet mockWallet = Mockito.mock(Wallet.class);
		
		// ## mock tx
		Transaction tx = Mockito.mock(Transaction.class);
		
		//
		new Authenticator(new BAApplicationParameters());
		Authenticator.addGeneralEventsListener(new BAGeneralEventsAdapter() {
			@Override
			public void onBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence) {
				if(wasAuthenticatorOnBalanceChangedCalledAndOK == false) // check called once
					wasAuthenticatorOnBalanceChangedCalledAndOK = true;
				else {
					System.out.println("onBalanceChanged called twice");
					wasAuthenticatorOnBalanceChangedCalledAndOK = false;
				}
			}
		});
		
		//
		WalletListener wl = new WalletListener(mockWalletOperation);
		
		/**
		 * check some sent from me and zero received, should call onBalanceChanged once
		 */
		
		Mockito.when(tx.getValueSentToMe(mockWallet)).thenReturn(Coin.ZERO);
		Mockito.when(tx.getValueSentFromMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		TransactionConfidence txc = Mockito.mock(TransactionConfidence.class);
		Mockito.when(txc.getConfidenceType()).thenReturn(ConfidenceType.BUILDING);
		Mockito.when(tx.getConfidence()).thenReturn(txc);
		wl.onCoinsSent(mockWallet, tx, null, null);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					if(!wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
		
		/**
		 * check some sent from me and some received, should not call onBalanceChanged
		 */
		wasAuthenticatorOnBalanceChangedCalledAndOK = false;
		Mockito.when(tx.getValueSentToMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		Mockito.when(tx.getValueSentFromMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		Mockito.when(txc.getConfidenceType()).thenReturn(ConfidenceType.BUILDING);
		Mockito.when(tx.getConfidence()).thenReturn(txc);
		wl.onCoinsSent(mockWallet, tx, null, null);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					if(wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
		
		/**
		 * check nothing sent from me, should not call onBalanceChanged
		 */
		wasAuthenticatorOnBalanceChangedCalledAndOK = false;
		Mockito.when(tx.getValueSentToMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		Mockito.when(tx.getValueSentFromMe(mockWallet)).thenReturn(Coin.ZERO);
		wl.onCoinsSent(mockWallet, tx, null, null);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					Authenticator.disposeOfAuthenticator();
					if(wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
	}
	
	@Test
	public void onCoinsReceivedTest() {
		// ## mock wallet operation 
		WalletOperation mockWalletOperation = Mockito.mock(WalletOperation.class);
		
		// ## mock wallet
		Wallet mockWallet = Mockito.mock(Wallet.class);
		
		// ## mock tx
		Transaction tx = Mockito.mock(Transaction.class);
		
		//
		new Authenticator(new BAApplicationParameters());
		Authenticator.addGeneralEventsListener(new BAGeneralEventsAdapter() {
			@Override
			public void onBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence) {
				if(wasAuthenticatorOnBalanceChangedCalledAndOK == false) // check called once
					wasAuthenticatorOnBalanceChangedCalledAndOK = true;
				else {
					System.out.println("onBalanceChanged called twice");
					wasAuthenticatorOnBalanceChangedCalledAndOK = false;
				}
			}
		});
		
		/**
		 * check some sent from me and some received, should call onBalanceChanged once
		 */
		
		Mockito.when(tx.getValueSentToMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		Mockito.when(tx.getValueSentFromMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		TransactionConfidence txc = Mockito.mock(TransactionConfidence.class);
		Mockito.when(txc.getConfidenceType()).thenReturn(ConfidenceType.BUILDING);
		Mockito.when(tx.getConfidence()).thenReturn(txc);
		
		WalletListener wl = new WalletListener(mockWalletOperation);
		wl.onCoinsReceived(mockWallet, tx, null, null);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					if(!wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
		
		/**
		 * check some sent from me and non received, should call onBalanceChanged once
		 */
		wasAuthenticatorOnBalanceChangedCalledAndOK = false;
		Mockito.when(tx.getValueSentToMe(mockWallet)).thenReturn(Coin.ZERO);
		Mockito.when(tx.getValueSentFromMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		wl.onCoinsReceived(mockWallet, tx, null, null);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					if(!wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
		
		/**
		 * check non sent from me and some received, should call onBalanceChanged once
		 */
		wasAuthenticatorOnBalanceChangedCalledAndOK = false;
		Mockito.when(tx.getValueSentToMe(mockWallet)).thenReturn(Coin.valueOf(10000));
		Mockito.when(tx.getValueSentFromMe(mockWallet)).thenReturn(Coin.ZERO);
		wl.onCoinsReceived(mockWallet, tx, null, null);
		// checks performed in listener
		new Thread() {
			@Override
			public void run() {
				try {
					// wait for a sec, if Authenticator general listener was not called fail the test
					Thread.sleep(1000);
					if(!wasAuthenticatorOnBalanceChangedCalledAndOK)
						assertTrue(false);
				} catch (InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				
			}
		}.run();
	}
}
