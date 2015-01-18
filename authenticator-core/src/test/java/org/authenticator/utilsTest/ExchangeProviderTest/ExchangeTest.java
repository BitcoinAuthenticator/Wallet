package org.authenticator.utilsTest.ExchangeProviderTest;

import static org.junit.Assert.*;

import org.authenticator.Utils.ExchangeProvider.Currency;
import org.authenticator.Utils.ExchangeProvider.Exchange;
import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

/**
 * Created by alonmuroch on 1/18/15.
 */
public class ExchangeTest {
    @Test
    public void test() {
        JSONObject j = new JSONObject();
        try {
            j.put("last", 354.2);
            j.put("timestamp", "Sun, 18 Jan 2015 13:53:39 -0000");
        } catch (JSONException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        Exchange ex = new Exchange("USD", j);
        assertTrue(ex.getCurrencyCode().equals("USD"));
        assertTrue(ex.getLatestExchangeRate() == Float.valueOf("354.2"));
        assertTrue(ex.convertToCurrency(Coin.COIN).compareTo(Currency.valueOf(354, 20)) == 0);
        assertTrue(ex.convertToBitcoin(Currency.valueOf(354, 20)).compareTo(Coin.valueOf(99999690)) == 0); // because of type casting
    }
}
