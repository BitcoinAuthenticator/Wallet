package org.authenticator.walletCore;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.authenticator.Utils.ExchangeProvider.Currency;
import org.authenticator.Utils.ExchangeProvider.Exchange;
import org.bitcoinj.core.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by alonmuroch on 1/19/15.
 */
public class ExtendedTransactionOutputTest {
    @Test
    public void CalculateEndPointPrices1Test() throws IOException {
        Wallet wallet = Mockito.mock(Wallet.class);
        List<TransactionOutput> outputs = outputData1(wallet);

        ExtendedTransactionOutput.ValuesExtension valuesExtension = getValueExtension(wallet, outputs.get(0));
        List<List<Number>> prices = valuesExtension.getOriginPricePoints();
        // first end point
        assertTrue(prices.size() == 1);
        assertTrue(prices.get(0).get(0).longValue() == 5 * Coin.COIN.longValue());
        assertTrue(prices.get(0).get(1).floatValue() == (float)331.62714);
        assertTrue(prices.get(0).get(2).longValue() == 1419077532);
        assertEquals(valuesExtension.getCurrentTotalValue().getValue(), 129276);
        assertTrue(valuesExtension.getGainOrLoss() == (float) -365.37);


        valuesExtension = getValueExtension(wallet, outputs.get(1));
        prices = valuesExtension.getOriginPricePoints();
        // second end point
        assertTrue(prices.size() == 3);
        assertTrue(prices.get(0).get(0).longValue() == 44444440);
        assertTrue(prices.get(0).get(1).floatValue() == (float)331.62714);
        assertTrue(prices.get(0).get(2).longValue() == 1419077532);
        assertTrue(prices.get(1).get(0).longValue() == 88888880);
        assertTrue(prices.get(1).get(1).floatValue() == (float)582.0571);
        assertTrue(prices.get(1).get(2).longValue() == 1393848732);
        assertTrue(prices.get(2).get(0).longValue() == 66666660);
        assertTrue(prices.get(2).get(1).floatValue() == (float)446.54572);
        assertTrue(prices.get(2).get(2).longValue() == 1399119132);
        assertEquals(valuesExtension.getCurrentTotalValue().getValue(), 51710);
        assertTrue(valuesExtension.getGainOrLoss() == (float)-445.35);
    }

    @Test
    public void CalculateEndPointPrices2Test() throws IOException {
        Wallet wallet = Mockito.mock(Wallet.class);
        List<TransactionOutput> outputs = outputData2(wallet);

        ExtendedTransactionOutput.ValuesExtension valuesExtension = getValueExtension(wallet, outputs.get(0));
        List<List<Number>> prices = valuesExtension.getOriginPricePoints();
        /*
         first end point
         */
        assertTrue(prices.size() == 6);
        assertTrue(prices.get(0).get(0).longValue()     == 64285710);
        assertTrue(prices.get(0).get(1).floatValue()    == (float)206.63286);
        assertTrue(prices.get(0).get(2).longValue()     == 1383567132);
        assertTrue(prices.get(1).get(0).longValue()     == 321428600);
        assertTrue(prices.get(1).get(1).floatValue()    == (float)170.49715);
        assertTrue(prices.get(1).get(2).longValue()     == 1382530332);
        assertTrue(prices.get(2).get(0).longValue()     == 32142860);
        assertTrue(prices.get(2).get(1).floatValue()    == (float)126.76143);
        assertTrue(prices.get(2).get(2).longValue()     == 1379506332);
        assertTrue(prices.get(3).get(0).longValue()     == 321428600);
        assertTrue(prices.get(3).get(1).floatValue()    == (float)104.02097);
        assertTrue(prices.get(3).get(2).longValue()     == 1376309532);
        assertTrue(prices.get(4).get(0).longValue()     == 128571400);
        assertTrue(prices.get(4).get(1).floatValue()    == (float)74.9592);
        assertTrue(prices.get(4).get(2).longValue()     == 1373371932);
        assertTrue(prices.get(5).get(0).longValue()     == 32142860);
        assertTrue(prices.get(5).get(1).floatValue()    == (float)114.31817);
        assertTrue(prices.get(5).get(2).longValue() == 1370779932);
        assertEquals(valuesExtension.getCurrentTotalValue().getValue(), 232697);
        assertTrue(valuesExtension.getGainOrLoss() == (float) 1137.92);

        valuesExtension = getValueExtension(wallet, outputs.get(1));
        prices = valuesExtension.getOriginPricePoints();
        /*
         second end point
         */
        assertTrue(prices.size() == 6);
        assertTrue(prices.get(0).get(0).longValue()     == 7142857);
        assertTrue(prices.get(0).get(1).floatValue()    == (float)206.63286);
        assertTrue(prices.get(0).get(2).longValue()     == 1383567132);
        assertTrue(prices.get(1).get(0).longValue()     == 35714290);
        assertTrue(prices.get(1).get(1).floatValue()    == (float)170.49715);
        assertTrue(prices.get(1).get(2).longValue()     == 1382530332);
        assertTrue(prices.get(2).get(0).longValue()     == 3571429);
        assertTrue(prices.get(2).get(1).floatValue()    == (float)126.76143);
        assertTrue(prices.get(2).get(2).longValue()     == 1379506332);
        assertTrue(prices.get(3).get(0).longValue()     == 35714290);
        assertTrue(prices.get(3).get(1).floatValue()    == (float)104.02097);
        assertTrue(prices.get(3).get(2).longValue()     == 1376309532);
        assertTrue(prices.get(4).get(0).longValue()     == 14285710);
        assertTrue(prices.get(4).get(1).floatValue()    == (float)74.9592);
        assertTrue(prices.get(4).get(2).longValue()     == 1373371932);
        assertTrue(prices.get(5).get(0).longValue()     == 3571429);
        assertTrue(prices.get(5).get(1).floatValue()    == (float)114.31817);
        assertTrue(prices.get(5).get(2).longValue()     == 1370779932);
        assertEquals(valuesExtension.getCurrentTotalValue().getValue(), 25855);
        assertTrue(valuesExtension.getGainOrLoss() == (float) 126.46);
    }

    /**
     *  ext - external input or address
     *  *   - a transaction
     *  0   - wallet address
     *
     *             A                     B         C         D     E
     *
     *           2 ext                                          16 ext
     * 1)          *                                               *---- ext 1btc
     *             |                                               |
     * 2)     4btc 0                                          3btc 0
     *             |                                               |
     * 3)          *-- ext 1btc                    ext 1btc \      *-- ext 1btc
     *             |                                         \     |
     * 4)     3btc 0                 ext 2            1btc 0--*----0 2btc
     *             |                     |                 |
     * 5)          ----------------------*------------------
     *                                   |
     * 6)                           2btc 0--------*--- ext 5
     *                                   | 2btc   |
     *                                   |        |
     * 7)                    ext 2btc -- *        0 5btc
     *                                   |
     * 8)                           2btc 0
     *
     *
     *
     *
     * @return
     */
    private List<TransactionOutput> outputData1(Wallet wallet) {
        TransactionInput in1 = null;
        TransactionInput in2 = null;

        // transaction 1A - update time 3/3/14:12:12:12, unix 1393848732
        Transaction tx1A = generateMokcedTx(wallet, 2, Coin.ZERO, new Date(1393848732 * 1000L));
        // transaction 3A - update time 24/3/14:12:12:12, unix 1395663132
        List<TransactionInput> ins = new ArrayList<TransactionInput>();
        Transaction tx3A = generateMokcedTx(wallet, 1, Coin.COIN.multiply(4), new Date(1395663132 * 1000L));
        in1 = generateMockedTransactionInput(tx3A, tx1A, Coin.COIN.multiply(4));
        ins.add(in1); Mockito.when(tx3A.getInputs()).thenReturn(ins);

        // transaction 1E, update time 3/5/2014:12:12:12
        Transaction tx1E = generateMokcedTx(wallet, 16, Coin.ZERO, new Date(1399119132 * 1000L));

        // transaction 3E, update time 4/6/2014:12:12:12
        Transaction tx3E = generateMokcedTx(wallet, 1, Coin.COIN.multiply(3), new Date(1401883932 * 1000L));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx3E, tx1E, Coin.COIN.multiply(3));
        ins.add(in1); Mockito.when(tx3E.getInputs()).thenReturn(ins);
        // transaction 4D, update time 1/10/2014:12:12:12
        Transaction tx4D = generateMokcedTx(wallet, 1, Coin.COIN.multiply(2), new Date(1412165532 * 1000L));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx4D, tx3E, Coin.COIN.multiply(2));
        ins.add(in1); Mockito.when(tx4D.getInputs()).thenReturn(ins);

        // transaction 5B, update time 18/10/2014:12:12:12
        Transaction tx5B = generateMokcedTx(wallet, 2, Coin.COIN.multiply(4), new Date(1413634332 * 1000L));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx5B, tx3A, Coin.COIN.multiply(3));
        in2 = generateMockedTransactionInput(tx5B, tx4D, Coin.COIN.multiply(1));
        ins.add(in1); ins.add(in2);
        Mockito.when(tx5B.getInputs()).thenReturn(ins);

        // transaction 6C, update time 20/12/2014:12:12:12
        Transaction tx6C = generateMokcedTx(wallet, 5, Coin.ZERO, new Date(1419077532 * 1000L));

        // transaction 7B, update time 24/12/2014:12:12:12
        Transaction tx7B = generateMokcedTx(wallet, 2, Coin.COIN.multiply(4), new Date(1419423132 * 1000L));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx7B, tx6C, Coin.COIN.multiply(2));
        in2 = generateMockedTransactionInput(tx7B, tx5B, Coin.COIN.multiply(2));
        ins.add(in1); ins.add(in2);
        Mockito.when(tx7B.getInputs()).thenReturn(ins);


        List<TransactionOutput> ret = new ArrayList<TransactionOutput>();
        TransactionOutput output8B = Mockito.mock(TransactionOutput.class);
        Mockito.when(output8B.getValue()).thenReturn(Coin.COIN.multiply(2));
        Mockito.when(output8B.getParentTransaction()).thenReturn(tx7B);

        TransactionOutput output7C = Mockito.mock(TransactionOutput.class);
        Mockito.when(output7C.getValue()).thenReturn(Coin.COIN.multiply(5));
        Mockito.when(output7C.getParentTransaction()).thenReturn(tx6C);
        ret.add(output7C);
        ret.add(output8B);

        return ret;
    }

    /**
     *  ext - external input or address
     *  *   - a transaction
     *  0   - wallet address
     *
     *
     *            A       B       C    D  E       F       G
     *
     *          3 ext   4 ext   5 ext   6 ext   7 ext   8 ext
     * 1)         *       *       *       *       *       *
     *       1btc |  5btc | .5btc |  5btc |  2btc | .5btc |
     *            |       |       |       |       |       |
     *            |       |       |       |       |       |
     *            |       |       |       |       |       |
     *            --------------------0--------------------
     *                                |
     * 2)                    1btc 0---*-- ext 4 btc
     *                                |
     *                                0 9btc
     *
     *
     *
     *
     */
    private List<TransactionOutput> outputData2(Wallet wallet) {
        List<TransactionInput> ins;
        TransactionInput in1 = null;
        TransactionInput in2 = null;
        TransactionInput in3 = null;
        TransactionInput in4 = null;
        TransactionInput in5 = null;
        TransactionInput in6 = null;

        // transaction 1A - update time 4/11/13:12:12:12, unix 1393848732
        Transaction tx1A = generateMokcedTx(wallet, 3, Coin.ZERO, new Date(1383567132 * 1000L));

        // transaction 1AB - update time 10/24/13:12:12:12, unix 1393848732
        Transaction tx1B = generateMokcedTx(wallet, 4, Coin.ZERO, new Date(1382530332 * 1000L));

        // transaction 1C - update time 9/18/13:12:12:12, unix 1393848732
        Transaction tx1C = generateMokcedTx(wallet, 5, Coin.ZERO, new Date(1379506332 * 1000L));

        // transaction 1E - update time 8/12/13:12:12:12, unix 1393848732
        Transaction tx1E = generateMokcedTx(wallet, 6, Coin.ZERO, new Date(1376309532 * 1000L));

        // transaction 1F - update time 7/9/13:12:12:12, unix 1393848732
        Transaction tx1F = generateMokcedTx(wallet, 7, Coin.ZERO, new Date(1373371932 * 1000L));

        // transaction 1G - update time 6/9/13:12:12:12, unix 1393848732
        Transaction tx1G = generateMokcedTx(wallet, 8, Coin.ZERO, new Date(1370779932 * 1000L));

        // transaction 2D - update time 12/25/13:12:12:12, unix 1393848732
        Transaction tx2D = generateMokcedTx(wallet, 5, Coin.COIN.multiply(14), new Date(1387973532 * 1000L));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx2D, tx1A, Coin.COIN.multiply(1));
        in2 = generateMockedTransactionInput(tx2D, tx1B, Coin.COIN.multiply(5));
        in3 = generateMockedTransactionInput(tx2D, tx1C, Coin.COIN.divide(2));
        in4 = generateMockedTransactionInput(tx2D, tx1E, Coin.COIN.multiply(5));
        in5 = generateMockedTransactionInput(tx2D, tx1F, Coin.COIN.multiply(2));
        in6 = generateMockedTransactionInput(tx2D, tx1G, Coin.COIN.divide(2));
        ins.add(in1); ins.add(in2);ins.add(in3); ins.add(in4);ins.add(in5);ins.add(in6);
        Mockito.when(tx2D.getInputs()).thenReturn(ins);


        List<TransactionOutput> ret = new ArrayList<TransactionOutput>();
        TransactionOutput output8B = Mockito.mock(TransactionOutput.class);
        Mockito.when(output8B.getValue()).thenReturn(Coin.COIN.multiply(1));
        Mockito.when(output8B.getParentTransaction()).thenReturn(tx2D);

        TransactionOutput output7C = Mockito.mock(TransactionOutput.class);
        Mockito.when(output7C.getValue()).thenReturn(Coin.COIN.multiply(9));
        Mockito.when(output7C.getParentTransaction()).thenReturn(tx2D);
        ret.add(output7C);
        ret.add(output8B);

        return ret;
    }

    private Transaction generateMokcedTx(Wallet wallet, int numOfInputs, Coin valueSentFromMe, Date updateTime) {
        Transaction tx = Mockito.mock(Transaction.class);
        Mockito.when(tx.getValueSentFromMe(wallet)).thenReturn(valueSentFromMe);
        Mockito.when(tx.getUpdateTime()).thenReturn(updateTime);

        return tx;
    }

    private TransactionInput generateMockedTransactionInput(Transaction parentTx, Transaction connectedTx, Coin value) {
        TransactionInput in = Mockito.mock(TransactionInput.class);
        Mockito.when(in.getParentTransaction()).thenReturn(parentTx);

        TransactionOutPoint op = Mockito.mock(TransactionOutPoint.class);
        Mockito.when(in.getOutpoint()).thenReturn(op);

        TransactionOutput connectedOp = Mockito.mock(TransactionOutput.class);
        Mockito.when(connectedOp.getParentTransaction()).thenReturn(connectedTx);
        Mockito.when(connectedOp.getValue()).thenReturn(value);
        Mockito.when(op.getConnectedOutput()).thenReturn(connectedOp);

        return in;
    }

    private ExtendedTransactionOutput.ValuesExtension getValueExtension(Wallet wallet, TransactionOutput output) throws IOException {
        JSONObject j = new JSONObject();
        try {
            j.put("last", 354.2);
            j.put("timestamp", "Sun, 18 Jan 2015 13:53:39 -0000");
        } catch (JSONException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        URL url = Resources.getResource("org/authenticator/walletCore/blockchain-info-usd-price-chart-data.json");
        String rawData = Resources.toString(url, Charsets.UTF_8);
        Exchange ex = new Exchange("USD", j, rawData);

        ExtendedTransactionOutput.ValuesExtension ve = new ExtendedTransactionOutput.ValuesExtension();
        ve.activate(output, ex, wallet);
        return ve;
    }
}
