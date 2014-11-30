package org.authenticator.walletCore.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.script.Script;

import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;

public class BalanceUpdater {
	static public void updateBalaceNonBlocking(WalletOperation walletOpeartion, Wallet wallet, Runnable completionBlock) {
		int s = 2;
		new Thread(){
			@Override
			public void run() {
				try {
					updateBalance(walletOpeartion, wallet);
					if(completionBlock != null)
						completionBlock.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	/**
	 * Be careful using this method directly because it can block 
	 * @param wallet
	 * @throws CannotWriteToConfigurationFileException 
	 * @throws AccountWasNotFoundException 
	 * @throws IOException 
	 * @throws Exception
	 */
	static private synchronized void updateBalance(WalletOperation walletOpeartion, Wallet wallet) throws CannotWriteToConfigurationFileException {
		List<ATAccount> accounts = walletOpeartion.getAllAccounts();
		List<ATAccount.Builder> newBalances = new ArrayList<ATAccount.Builder>();
    	for(ATAccount acc:accounts){
    		ATAccount.Builder b = ATAccount.newBuilder();
			  b.setIndex(acc.getIndex());
			  b.setConfirmedBalance(0);
			  b.setUnConfirmedBalance(0);
			  b.setNetworkType(acc.getNetworkType());
			  b.setAccountName(acc.getAccountName());
			  b.setAccountType(acc.getAccountType());
		  newBalances.add(b);
    	}
    	
    	List<Transaction> allTx = wallet.getRecentTransactions(0, false);
    	Collections.reverse(allTx);
    	for(Transaction tx: allTx){
    		/**
    		 * BUILDING
    		 */
    		if(tx.getConfidence().getConfidenceType() == ConfidenceType.BUILDING){
    			if(tx.getValueSentToMe(wallet).signum() > 0){
    				for (TransactionOutput out : tx.getOutputs()){
    					Script scr = out.getScriptPubKey();
    	    			String addrStr = scr.getToAddress(walletOpeartion.getNetworkParams()).toString();
    	    			if(walletOpeartion.isWatchingAddress(addrStr)){
    	    				ATAddress add = walletOpeartion.findAddressInAccounts(addrStr);
    	    				if(add == null)
    	    					continue;
    	    				
    	    				walletOpeartion.markAddressAsUsed(add.getAccountIndex(),add.getKeyIndex(), add.getType());
    	    				
    	    				/**
    	    				 * Add to internal account list
    	    				 */
    	    				for(ATAccount.Builder acc:newBalances)
    	    					if(acc.getIndex() == add.getAccountIndex())
    	    					{
    	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
    	    						acc.setConfirmedBalance(old.add(out.getValue()).longValue());
    	    					}
    	    			}
    				}
    			}
    			
    			if(tx.getValueSentFromMe(wallet).signum() > 0){
    				for (TransactionInput in : tx.getInputs()){
    					TransactionOutput out = in.getConnectedOutput();
    					if(out != null){
    						Script scr = out.getScriptPubKey();
        	    			String addrStr = scr.getToAddress(walletOpeartion.getNetworkParams()).toString();
        	    			if(walletOpeartion.isWatchingAddress(addrStr)){
        	    				ATAddress add = walletOpeartion.findAddressInAccounts(addrStr);
        	    				if(add == null)
        	    					continue;
        	    				
        	    				/**
        	    				 * Subtract from internal account list
        	    				 */
        	    				for(ATAccount.Builder acc:newBalances)
        	    					if(acc.getIndex() == add.getAccountIndex())
        	    					{
        	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
        	    						acc.setConfirmedBalance(old.subtract(out.getValue()).longValue());
        	    					}
        	    			}
    					}
    				}
    			}
    		}
    		
    		/**
    		 * PENDING
    		 */
    		if(tx.getConfidence().getConfidenceType() == ConfidenceType.PENDING){
    			if(tx.getValueSentToMe(wallet).signum() > 0){
    				for (TransactionOutput out : tx.getOutputs()){
    					Script scr = out.getScriptPubKey();
    	    			String addrStr = scr.getToAddress(walletOpeartion.getNetworkParams()).toString();
    	    			if(walletOpeartion.isWatchingAddress(addrStr)){
    	    				ATAddress add = walletOpeartion.findAddressInAccounts(addrStr);
    	    				if(add == null)
    	    					continue;
    	    				
    	    				walletOpeartion.markAddressAsUsed(add.getAccountIndex(),add.getKeyIndex(), add.getType());
    	    				
    	    				/**
    	    				 * Add to internal account list
    	    				 */
    	    				for(ATAccount.Builder acc:newBalances)
    	    					if(acc.getIndex() == add.getAccountIndex())
    	    					{
    	    						Coin old = Coin.valueOf(acc.getUnConfirmedBalance());
    	    						acc.setUnConfirmedBalance(old.add(out.getValue()).longValue());
    	    					}
    	    			}
    				}
    			}
    			
    			if(tx.getValueSentFromMe(wallet).signum() > 0){
    				for (TransactionInput in : tx.getInputs()){
    					TransactionOutput out = in.getConnectedOutput();
    					if(out != null){
    						Script scr = out.getScriptPubKey();
        	    			String addrStr = scr.getToAddress(walletOpeartion.getNetworkParams()).toString();
        	    			if(walletOpeartion.isWatchingAddress(addrStr)){
        	    				ATAddress add = walletOpeartion.findAddressInAccounts(addrStr);
        	    				if(add == null)
        	    					continue;
        	    				
        	    				if(out.getParentTransaction().getConfidence().getConfidenceType() == ConfidenceType.PENDING) {
        	    					/**
            	    				 * Subtract from internal account list
            	    				 */
            	    				for(ATAccount.Builder acc:newBalances)
            	    					if(acc.getIndex() == add.getAccountIndex())
            	    					{
            	    						Coin old = Coin.valueOf(acc.getUnConfirmedBalance());
            	    						acc.setUnConfirmedBalance(old.subtract(out.getValue()).longValue());
            	    					}
        	    				}
        	    				if(out.getParentTransaction().getConfidence().getConfidenceType() == ConfidenceType.BUILDING) {
        	    					/**
            	    				 * Subtract from internal account list
            	    				 */
            	    				for(ATAccount.Builder acc:newBalances)
            	    					if(acc.getIndex() == add.getAccountIndex())
            	    					{
            	    						Coin old = Coin.valueOf(acc.getConfirmedBalance());
            	    						acc.setConfirmedBalance(old.subtract(out.getValue()).longValue());
            	    					}
        	    				}
        	    			}
    					}
    				}
    			}
    		}
    	}
    	
    	for(ATAccount.Builder acc:newBalances) {
    		walletOpeartion.setConfirmedBalance(acc.getIndex(), Coin.valueOf(acc.getConfirmedBalance()));
    		walletOpeartion.setUnConfirmedBalance(acc.getIndex(), Coin.valueOf(acc.getUnConfirmedBalance()));
    	}
	}
}
