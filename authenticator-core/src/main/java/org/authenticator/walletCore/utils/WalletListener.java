package org.authenticator.walletCore.utils;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.Wallet;

import org.authenticator.Authenticator;
import org.authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.WalletOperation.BAOperationState;

/**
 *A basic listener to keep track of balances and transaction state.<br>
 *Will mark addresses as "used" when any amount of bitcoins were transfered to the address.
 * 
 * @author alon
 *
 */
public class WalletListener extends AbstractWalletEventListener {
	WalletOperation walletOp;
	public WalletListener(WalletOperation wo) {
		walletOp = wo;
	}
	
	@Override
	public void onWalletChanged(Wallet wallet) {
		/**
		 * used for confidence change UI update
		 */
		if(walletOp.getOperationalState() == BAOperationState.READY_AND_OPERATIONAL)
			BalanceUpdater.updateBalaceNonBlocking(walletOp, wallet, new Runnable(){
			@Override
			public void run() { 
				notifyBalanceUpdate(wallet,null);
			}
		});
	}
	
	@Override
    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
		/**
		 * Notify balance onCoinsSent() only if we don't receive any coins.
		 * The idea is that if we have a Tx that sends and receives coins to this wallet,
		 * notify only onCoinsReceived() so we won't send multiple update balance calls.
		 * 
		 * If the Tx only sends coins, do update the balance from here.
		 */
		if(tx.getValueSentToMe(wallet).signum() == 0)
			BalanceUpdater.updateBalaceNonBlocking(walletOp, wallet, new Runnable(){
				@Override
				public void run() { 
					notifyBalanceUpdate(wallet,tx);
				}
    		});
	}
	
	@Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
		/**
		 * the {org.bitcoinj.wallet.DefaultCoinSelector} can only choose candidates if they
		 * originated from the wallet, so this fix is so incoming tx (originated elsewhere)
		 * could be spent if not confirmed	
		 */
		tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
		
		BalanceUpdater.updateBalaceNonBlocking(walletOp, wallet, new Runnable(){
			@Override
			public void run() { 
				notifyBalanceUpdate(wallet,tx);
			}
		});
	}    
	
	private void notifyBalanceUpdate(Wallet wallet, Transaction tx){
		if(tx != null){
			if(tx.getValueSentToMe(wallet).signum() > 0){
	    		Authenticator.fireOnBalanceChanged(tx, HowBalanceChanged.ReceivedCoins, tx.getConfidence().getConfidenceType());
	    	}
			else if(tx.getValueSentFromMe(wallet).signum() > 0){
	    		Authenticator.fireOnBalanceChanged(tx, HowBalanceChanged.SentCoins, tx.getConfidence().getConfidenceType());
	    	}
		}
		else
			Authenticator.fireOnBalanceChanged(null, null, null);
    }
}
