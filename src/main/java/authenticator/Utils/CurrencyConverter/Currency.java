package authenticator.Utils.CurrencyConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

public class Currency {
	final int SATHOSIES_IN_ONE_BTC = 100000000;
	public String UPPERCASE_CURRENCY_CODE;
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
		
	public Currency(String currencyCode, JSONObject object) {
		UPPERCASE_CURRENCY_CODE = currencyCode;
		fromBitcoinaverage(object);
	}
	
	/**
	 * bitcoinaverage.com parser
	 * @throws JSONException 
	 * @throws ParseException 
	 */
	public void fromBitcoinaverage(JSONObject object) {
		try {
			ONE_BTC_TO_CURRENCY = object.getDouble("last");
			ONE_CURRENCY_TO_BTC = 1 / ONE_BTC_TO_CURRENCY;
			lastUpdated = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss -0000", Locale.ENGLISH).parse(object.getString("timestamp")); // example Sat, 26 Jul 2014 14:22:27 -0000
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public double ONE_CURRENCY_TO_BTC;
	/**
	 * A readable btc amount, e.g, 12.5 BTC
	 * @param howMuchCurrency
	 * @return
	 */
	public double convertToBTCFriendly(double howMuchCurrency){
		return howMuchCurrency * ONE_CURRENCY_TO_BTC;
	}
	/**
	 * Number of satoshies, usually will be used for Tx  creation
	 * @param howMuchCurrency
	 * @return
	 */
	public double convertToSatoshi(double howMuchCurrency){
		return howMuchCurrency * ONE_CURRENCY_TO_BTC * SATHOSIES_IN_ONE_BTC;
	}
	
	double ONE_BTC_TO_CURRENCY;
	public double convertToCurrency(double howMuchBTC){
		return howMuchBTC * ONE_BTC_TO_CURRENCY;
	}
	
	public Date lastUpdated;
}
