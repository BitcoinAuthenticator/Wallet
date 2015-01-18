package org.authenticator.Utils.ExchangeProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import org.authenticator.Utils.EncodingUtils;
import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class Exchanges {
	public CurrencyList currencies;

	public boolean isReady = false;

	public static void init(String[] lstCurrencies, ExchangeProviderImplListener listener) {
		instance = new Exchanges(lstCurrencies, listener);
	}

	private static Exchanges instance;
	public static Exchanges getInstance() {
		if(instance == null)
			instance = new Exchanges();
		return instance;
	}

	Exchanges() { }

	/**
	 * Requires downloading the currency data in an async process.<br>
	 * Use this method as follows:<br>
	 * <b>new CurrencyConverterSingelton(new CurrencyConverterListener(){ });</b>
	 * 
	 * @param listener
	 */
	Exchanges(String[] lstCurrencies, ExchangeProviderImplListener listener){
		List<String> currenciesList = Arrays.asList(lstCurrencies);
		if(isReady == false){
			currencies = new CurrencyList();
			try {
				getCurrencies(currenciesList, listener, this);
			} catch (IOException | JSONException e) {
				e.printStackTrace();
				if(listener != null)
					listener.onErrorGettingCurrencyData(e);
			}
		}
		else if(listener != null){
			listener.onFinishedGettingCurrencyData(this);
		}
	}
	
	public void CANNOT_EXECUTE_ASYNC_SO_CHECK_IS_READY() throws ExchangeProviderNoDataException {
		if(isReady != true)
			throw new ExchangeProviderNoDataException("No Currency Data");
	}
	
	/**
	 * 
	 * 
	 * @param lstCurrencies
	 * @param listener
	 * @param self
	 * @throws IOException
	 * @throws JSONException
	 */
	private void getCurrencies(List<String> lstCurrencies, final ExchangeProviderImplListener listener, Exchanges self) throws IOException, JSONException{
		// TODO - download more than one currency
		String s = "https://api.bitcoinaverage.com/ticker/global/" + lstCurrencies.get(0) +  "/";
		EncodingUtils.readFromUrl(s, new AsyncCompletionHandler<Response>(){
			@Override
			public Response onCompleted(Response arg0) throws Exception {
				String res = arg0.getResponseBody();
				JSONObject json = new JSONObject(res);
				//double last = json.getDouble("last");
				currencies.add(new Exchange(lstCurrencies.get(0), json));
				
				if(listener != null)
					listener.onFinishedGettingCurrencyData(self);
				
				isReady = true;
				
				return null;
			}
		});
	}

	public interface ExchangeProviderImplListener {
		public void onFinishedGettingCurrencyData(Exchanges currencies);
		public void onErrorGettingCurrencyData(Exception e);
	}
	
	public class CurrencyList {
		private List<Exchange> lst;

		public CurrencyList() {
			lst = new ArrayList<Exchange>();
		}
		
		public void add(Exchange c) {
			lst.add(c);
		}
		
		public Exchange get(int index) {
			return lst.get(index);
		}
				
		public Exchange get(String upperCaseCurrencyCode) {
			for(Exchange c: lst) {
				if(c.getCurrencyCode().equals(upperCaseCurrencyCode))
					return c;
			}
			return null;
		}
	}
}
