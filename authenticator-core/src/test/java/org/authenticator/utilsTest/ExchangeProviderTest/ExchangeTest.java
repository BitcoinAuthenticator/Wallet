package org.authenticator.utilsTest.ExchangeProviderTest;

import static org.junit.Assert.*;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.authenticator.Utils.ExchangeProvider.Currency;
import org.authenticator.Utils.ExchangeProvider.Exchange;
import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;
import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * Created by alonmuroch on 1/18/15.
 */
public class ExchangeTest {
    @Test
    public void currentPriceTest() {
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

    @Test
    public void getPriceByUnixTimeTest() throws IOException, ExchangeProviderNoDataException {
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


        // check existing values
        assertTrue(ex.getExchangeRate((long) 1303409705)    == (float)1.1508129);
        assertTrue(ex.getExchangeRate((long) 1314555305)    == (float)10.606648);
        assertTrue(ex.getExchangeRate((long) 1318356905)    == (float)4.598197);
        assertTrue(ex.getExchangeRate((long) 1342721705)    == (float)8.527354);
        assertTrue(ex.getExchangeRate((long) 1384020905)    == (float)273.49286);
        assertTrue(ex.getExchangeRate((long) 1394388905)    == (float)647.75287);
        assertTrue(ex.getExchangeRate((long) 1415988905)    == (float)382.3257);

        // check too early data
        assertTrue(ex.getExchangeRate((long) 1231006504)    == (float)0);
        assertTrue(ex.getExchangeRate((long) 1230006505)    == (float)0);
        assertTrue(ex.getExchangeRate((long) 1131006505)    == (float)0);

        // check too late data
        assertTrue(ex.getExchangeRate((long) 1421237622)    == (float)258.55286);
        assertTrue(ex.getExchangeRate((long) 1421337621)    == (float)258.55286);
        assertTrue(ex.getExchangeRate((long) 1421237721)    == (float)258.55286);
    }
}
