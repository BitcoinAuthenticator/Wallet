package org.authenticator.Utils.ExchangeProvider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.json.JSONException;
import org.json.JSONObject;
import sun.util.resources.cldr.ebu.CurrencyNames_ebu;

public class Exchange implements ExchangeProvider {
	private String uppercaseCurrencyCode;
	private long oneCurrencyToSatoshies; // in satoshies
	private long oneBTCToCurrency; // in cents
	private Date lastUpdated;

	public Exchange(String currencyCode, JSONObject object) {
		uppercaseCurrencyCode = currencyCode;
		fromBitcoinaverage(object);
	}

	/**
	 * bitcoinaverage.com parser
	 * @throws JSONException 
	 * @throws ParseException 
	 */
	private void fromBitcoinaverage(JSONObject object) {
		try {
			oneBTCToCurrency = (long)((float)object.getDouble("last") * (float)Currency.ONE.getValue());
			oneCurrencyToSatoshies = Coin.COIN.divide(Currency.valueOf(oneBTCToCurrency).divide(Currency.ONE)).longValue();
			lastUpdated = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss -0000", Locale.ENGLISH).parse(object.getString("timestamp")); // example Sat, 26 Jul 2014 14:22:27 -0000
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private Coin toBitcoin(Currency currency){
		return Coin.valueOf(currency.divide(Currency.ONE) * oneCurrencyToSatoshies);
	}

	private Currency toCurrency(Coin bitcoins){
		return currencyValueOf(bitcoins.divide(Coin.COIN) * oneBTCToCurrency);
	}

	private Currency currencyValueOf(long value) {
		Currency c = Currency.valueOf(value);
		c.setCurrencyCode(uppercaseCurrencyCode);
		return c;
	}

	//#######################
	//
	//		API
	//
	//#######################

	@Override
	public String getCurrencyCode() {
		return uppercaseCurrencyCode;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	@Override
	public float getLatestExchangeRate() {
		return (float)oneBTCToCurrency / (float)Currency.ONE.getValue();
	}

	@Override
	public float getExchangeRate(long unixTime) throws ExchangeProviderNoDataException {
		throw new ExchangeProviderNoDataException("No history exchange data");
	}

	@Override
	public Currency convertToCurrency(Coin sathosies) {
		return toCurrency(sathosies);
	}

	@Override
	public Coin convertToBitcoin(Currency currency) {
		return toBitcoin(currency);
	}
}
