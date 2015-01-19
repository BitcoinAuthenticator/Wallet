package org.authenticator.Utils.ExchangeProvider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sun.util.resources.cldr.ebu.CurrencyNames_ebu;

import javax.annotation.Nullable;

public class Exchange implements ExchangeProvider {
	private List<PricePoint> pricePoints;
	private String uppercaseCurrencyCode;
	private long oneCurrencyToSatoshies; // in satoshies
	private long oneBTCToCurrency; // in cents
	private Date lastUpdated;

	public Exchange(String currencyCode, JSONObject exchangeJson) {
		this(currencyCode, exchangeJson, null);
	}

	public Exchange(String currencyCode, JSONObject exchangeJson, @Nullable String rawHistoryData) {
		uppercaseCurrencyCode = currencyCode;
		fromBitcoinaverage(exchangeJson);
		if(rawHistoryData != null)
			parseAndSetPriceDataFromBlockchainInfo(rawHistoryData);
	}

	public void parseAndSetPriceDataFromBlockchainInfo(String s) {
		try {
			pricePoints = new ArrayList<PricePoint>();
			JSONObject ob = new JSONObject(s);

			JSONArray arr = ob.getJSONArray("values");
			for (int i = 0; i < arr.length(); i++) {
				JSONObject row = arr.getJSONObject(i);
				pricePoints.add(new PricePoint(row.getLong("x"), row.getDouble("y")));
			}

			Collections.sort(pricePoints);
		} catch (JSONException e) {
			e.printStackTrace();
		}
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

	private PricePoint getClosestPriceToUnixTime(Long unix) {
		int dataGap = 60 * 60 * 24; // 86400
		// 1)
		PricePoint firstPoint = pricePoints.get(0);
		Long diff = unix - firstPoint.getUnixTime() + dataGap;
		if (diff <= 0 || diff < dataGap)
			return firstPoint;

		//2)
		diff /= dataGap;
		if (diff >= pricePoints.size() - 1)
			return pricePoints.get(pricePoints.size() - 1);
		int startIdx = Math.min(diff.intValue(), pricePoints.size());
		PricePoint closest = pricePoints.get(startIdx);
		for (int i = startIdx - 1; i > 0; i--) {
			PricePoint p = pricePoints.get(i);
			Long closestDiff = closest.getUnixTime() - unix;
			Long currentDiff = p.getUnixTime() - unix;

			if (closestDiff == 0)
				return closest;
			if (currentDiff == 0)
				return p;

			if (currentDiff < 0 && closestDiff > 0) {
				currentDiff = Math.abs(currentDiff);
				if (currentDiff > closestDiff)
					return closest;
				else
					return p;
			}
			closest = p;
		}

		return null;
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
		if(pricePoints == null)
			throw new ExchangeProviderNoDataException("No history price data");
		PricePoint p = getClosestPriceToUnixTime(unixTime);
		return (float)p.getPrice();
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
