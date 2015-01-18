package org.authenticator.utilsTest.ExchangeProviderTest;

import org.authenticator.Utils.ExchangeProvider.Currency;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by alonmuroch on 1/18/15.
 */
public class currencyTest {
    @Test
    public void toStringTest() {
        Currency c = Currency.valueOf(100);
        assertTrue(c.toString().equals("100 Cents"));
        assertTrue(c.toFriendlyString().equals("1.0"));

        c.setCurrencyCode("USD");
        assertTrue(c.toString().equals("100 USD Cents"));
        assertTrue(c.toFriendlyString().equals("1.0 USD"));
    }

}
