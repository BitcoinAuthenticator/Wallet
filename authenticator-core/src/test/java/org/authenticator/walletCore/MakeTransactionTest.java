package org.authenticator.walletCore;

import static org.junit.Assert.*;
import org.authenticator.walletCore.exceptions.UnableToCompleteTransactionException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by alonmuroch on 1/18/15.
 */
public class MakeTransactionTest {
    @Test
    public void makeSimpleTx() {
        WalletOperation wo = Mockito.spy(new WalletOperation());
        Mockito.doReturn(MainNetParams.get()).when(wo).getNetworkParams();

        ArrayList<TransactionOutput> selectedOutputs = new ArrayList<TransactionOutput>();
        TransactionOutput o1 = mockTransactionOutput(Coin.COIN.multiply(2)); selectedOutputs.add(o1);
        TransactionOutput o2 = mockTransactionOutput(Coin.COIN.multiply(4)); selectedOutputs.add(o2);
        TransactionOutput o3 = mockTransactionOutput(Coin.COIN.multiply(6)); selectedOutputs.add(o3);
        TransactionOutput o4 = mockTransactionOutput(Coin.COIN.multiply(3)); selectedOutputs.add(o4);

        HashMap<String, Coin> to = new HashMap<String, Coin>();
        to.put("1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp", Coin.COIN.multiply(12));

        Coin fee = Coin.MICROCOIN;
        String changeAddress = "1LBNuR7g2b3w6xTLWzcrSpLtRiUnHtpzbF";


        try {
            Transaction tx = wo.mkUnsignedTxWithSelectedInputs(selectedOutputs, to, fee, changeAddress);
            assertTrue(tx.getInputs().size() == 4);
            assertTrue(tx.getInput(0).getValue().compareTo(Coin.COIN.multiply(2)) == 0);
            assertTrue(tx.getInput(1).getValue().compareTo(Coin.COIN.multiply(4)) == 0);
            assertTrue(tx.getInput(2).getValue().compareTo(Coin.COIN.multiply(6)) == 0);
            assertTrue(tx.getInput(3).getValue().compareTo(Coin.COIN.multiply(3)) == 0);

            assertTrue(tx.getOutputs().size() == 2); // including change
            boolean found1 = false;
            boolean found2 = false;
            for(int i=0; i< 2; i++) {
                TransactionOutput op = tx.getOutput(i);
                if (op.getValue().compareTo(Coin.COIN.multiply(12)) == 0) {
                    found1 = true;
                    assertTrue(op.getAddressFromP2PKHScript(MainNetParams.get()).toString().equals("1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp"));
                }
                if (op.getValue().compareTo(Coin.COIN.multiply(3).subtract(Coin.MICROCOIN)) == 0) {
                    found2 = true;
                    assertTrue(op.getAddressFromP2PKHScript(MainNetParams.get()).toString().equals("1LBNuR7g2b3w6xTLWzcrSpLtRiUnHtpzbF"));
                }
            }
            assertTrue(found1 && found2);

        } catch (UnableToCompleteTransactionException e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }

    @Test
    public void makeTxTotalOutTooSmall() {
        WalletOperation wo = Mockito.spy(new WalletOperation());
        Mockito.doReturn(MainNetParams.get()).when(wo).getNetworkParams();

        ArrayList<TransactionOutput> selectedOutputs = new ArrayList<TransactionOutput>();
        TransactionOutput o1 = mockTransactionOutput(Coin.COIN.multiply(2)); selectedOutputs.add(o1);

        HashMap<String, Coin> to = new HashMap<String, Coin>();
        to.put("1VayNert3x1KzbpzMGt2qdqrAThiRovi8", Transaction.MIN_NONDUST_OUTPUT.subtract(Coin.MILLICOIN));

        Coin fee = Coin.MICROCOIN;
        String changeAddress = "1FFirnLctcZxVx5otnLNZ4dDGUkMBM4vNr";


        try {
            Transaction tx = wo.mkUnsignedTxWithSelectedInputs(selectedOutputs, to, fee, changeAddress);
            assertTrue(false);
        } catch (UnableToCompleteTransactionException e) {
            assertTrue(e.getMessage().equals("org.authenticator.walletCore.exceptions.UnableToCompleteTransactionException: java.lang.IllegalArgumentException: Tried to send dust with ensureMinRequiredFee set - no way to complete this"));
        }

    }

    @Test
    public void makeTxWithInsufficientInputs() {
        WalletOperation wo = Mockito.spy(new WalletOperation());
        Mockito.doReturn(MainNetParams.get()).when(wo).getNetworkParams();

        ArrayList<TransactionOutput> selectedOutputs = new ArrayList<TransactionOutput>();
        TransactionOutput o1 = mockTransactionOutput(Coin.COIN.multiply(2)); selectedOutputs.add(o1);

        HashMap<String, Coin> to = new HashMap<String, Coin>();
        to.put("1VayNert3x1KzbpzMGt2qdqrAThiRovi8", Coin.COIN.multiply(3));

        Coin fee = Coin.MICROCOIN;
        String changeAddress = "1FFirnLctcZxVx5otnLNZ4dDGUkMBM4vNr";


        try {
            Transaction tx = wo.mkUnsignedTxWithSelectedInputs(selectedOutputs, to, fee, changeAddress);
            assertTrue(false);
        } catch (UnableToCompleteTransactionException e) {
            assertTrue(e.getMessage().equals("org.authenticator.walletCore.exceptions.UnableToCompleteTransactionException: java.lang.IllegalArgumentException: Insufficient funds! You cheap bastard !"));
        }

    }

    @Test
    public void makeTxTooBig() {
        WalletOperation wo = Mockito.spy(new WalletOperation());
        Mockito.doReturn(MainNetParams.get()).when(wo).getNetworkParams();

        ArrayList<TransactionOutput> selectedOutputs = new ArrayList<TransactionOutput>();
        for(int i=0 ; i<3000; i++) {
            TransactionOutput o = mockTransactionOutput(Coin.COIN.multiply(13).divide(3000)); selectedOutputs.add(o);
        }

        HashMap<String, Coin> to = new HashMap<String, Coin>();
        to.put("1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp", Coin.COIN.multiply(12));

        Coin fee = Coin.MICROCOIN;
        String changeAddress = "1LBNuR7g2b3w6xTLWzcrSpLtRiUnHtpzbF";


        try {
            Transaction tx = wo.mkUnsignedTxWithSelectedInputs(selectedOutputs, to, fee, changeAddress);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("org.authenticator.walletCore.exceptions.UnableToCompleteTransactionException: org.bitcoinj.core.Wallet$ExceededMaxTransactionSize"));
        }

    }

    private TransactionOutput mockTransactionOutput(Coin value) {
        TransactionOutput o = Mockito.mock(TransactionOutput.class);
        Mockito.when(o.getValue()).thenReturn(value);

        return o;
    }
}
