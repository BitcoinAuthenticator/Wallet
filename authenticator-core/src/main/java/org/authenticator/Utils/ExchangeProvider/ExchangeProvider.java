package org.authenticator.Utils.ExchangeProvider;

import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;
import org.bitcoinj.core.Coin;

import java.util.Date;

/**
 * Interface for providing exchange data
 *
 * Created by alonmuroch on 1/18/15.
 */
public interface ExchangeProvider {
    public String getCurrencyCode();
    public Date getLastUpdated();
    public float getLatestExchangeRate();
    /**
     *
     * @param unixTime
     * @return
     * @throws ExchangeProviderNoDataException - the provider doesn't have to contain history exchange data
     */
    public float getExchangeRate(long unixTime) throws ExchangeProviderNoDataException;
    public boolean hasHistoricData();

    /**
     * Will convert {@link org.bitcoinj.core.Coin Coin} satoshi value into {@link org.authenticator.Utils.ExchangeProvider.Currency Currency}
     * @param sathosies
     * @return
     */
    public Currency convertToCurrency(Coin sathosies);
    /**
     * Will convert {@link org.authenticator.Utils.ExchangeProvider.Currency Currency} cents value into {@link org.bitcoinj.core.Coin Coin}
     * @param currency
     * @return
     */
    public Coin convertToBitcoin(Currency currency);

    public static String[] AVAILBLE_CURRENCY_CODES = new String[]{
            "USD",
            // TODO
//		"AED",
//        "AFN",
//        "ALL",
//        "AMD",
//        "ANG",
//        "AOA",
//        "ARS",
//        "AUD",
//        "AWG",
//        "AZN",
//        "BAM",
//        "BBD",
//        "BDT",
//        "BGN",
//        "BHD",
//        "BIF",
//        "BMD",
//        "BND",
//        "BOB",
//        "BRL",
//        "BSD",
//        "BTN",
//        "BWP",
//        "BYR",
//        "BZD",
//        "CAD",
//        "CDF",
//        "CHF",
//        "CLF",
//        "CLP",
//        "CLY",
//        "COP",
//        "CRC",
//        "CUP",
//        "CVE",
//        "CZK",
//        "DJF",
//        "DKK",
//        "DOP",
//        "DZD",
//        "EEK",
//        "EGP",
//        "ERN",
//        "ETB",
//        "EUR",
//        "FJD",
//        "FKP",
//        "GBP",
//        "GEL",
//        "GGP",
//        "GHS",
//        "GIP",
//        "GMD",
//        "GNF",
//        "GTQ",
//        "GYD",
//        "HKD",
//        "HNL",
//        "HRK",
//        "HTG",
//        "HUF",
//        "IDR",
//        "ILS",
//        "IMP",
//        "INR",
//        "IQD",
//        "IRR",
//        "ISK",
//        "JEP",
//        "JMD",
//        "JPY",
//        "KES",
//        "KGS",
//        "KHR",
//        "KMF",
//        "KPW",
//        "KRW",
//        "KWD",
//        "KYD",
//        "KZT",
//        "LAK",
//        "LBP",
//        "LKR",
//        "LRD",
//        "LSL",
//        "LTL",
//        "LVL",
//        "LYD",
//        "MAD",
//        "MDL",
//        "MGA",
//        "MKD",
//        "MMK",
//        "MNT",
//        "MOP",
//        "MRO",
//        "MTL",
//        "MUR",
//        "MVR",
//        "MWK",
//        "MXN",
//        "MYR",
//        "MZN",
//        "NAD",
//        "NGN",
//        "NIO",
//        "NOK",
//        "NPR",
//        "NZD",
//        "OMR",
//        "PAB",
//        "PEN",
//        "PGK",
//        "PHP",
//        "PKR",
//        "PLN",
//        "PYG",
//        "QAR",
//        "RON",
//        "RSD",
//        "RUB",
//        "RWF",
//        "SAR",
//        "SBD",
//        "SCR",
//        "SDG",
//        "SEK",
//        "SGD",
//        "SHP",
//        "SLL",
//        "SOS",
//        "SRD",
//        "STD",
//        "SVC",
//        "SYP",
//        "SZL",
//        "THB",
//        "TJS",
//        "TMT",
//        "TND",
//        "TOP",
//        "TRY",
//        "TTD",
//        "TWD",
//        "TZS",
//        "UAH",
//        "UGX",
//        "UYU",
//        "UZS",
//        "VEF",
//        "VND",
//        "VUV",
//        "WST",
//        "XAF",
//        "XAG",
//        "XAU",
//        "XCD",
//        "XDR",
//        "XOF",
//        "XPF",
//        "YER",
//        "ZAR",
//        "ZMK",
//        "ZMW",
//        "ZWL"
    };
}
