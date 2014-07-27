package authenticator.Utils.CurrencyConverter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.Utils.EncodingUtils;
import authenticator.Utils.CurrencyConverter.exceptions.CurrencyConverterSingeltonNoDataException;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class CurrencyConverterSingelton {
	public static Currency USD;
	
	static public boolean isReady = false;
	/**
	 * Requires downloading the currency data in an async process.<br>
	 * Use this method as follows:<br>
	 * <b>new CurrencyConverterSingelton(new CurrencyConverterListener(){ });</b>
	 * 
	 * @param listener
	 */
	public CurrencyConverterSingelton(CurrencyConverterListener listener){
		if(isReady == false){
			USD = new Currency();
			try {
				getCurrencies(listener, this);
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
	
	public static void CANNOT_EXECUTE_ASYNC_SO_CHECK_IS_READY() throws CurrencyConverterSingeltonNoDataException{
		if(CurrencyConverterSingelton.isReady != true)
			throw new CurrencyConverterSingeltonNoDataException("No Currency Data");
	}
	
	private void getCurrencies(final CurrencyConverterListener listener, CurrencyConverterSingelton self) throws IOException, JSONException{
		EncodingUtils.readFromUrl("https://api.bitcoinaverage.com/ticker/global/USD/", new AsyncCompletionHandler<Response>(){
			@Override
			public Response onCompleted(Response arg0) throws Exception {
				String res = arg0.getResponseBody();
				JSONObject json = new JSONObject(res);
				//double last = json.getDouble("last");
				CurrencyConverterSingelton.USD.updateFromBitcoinaverage(json);
				
				if(listener != null)
					listener.onFinishedGettingCurrencyData(self);
				
				CurrencyConverterSingelton.isReady = true;
				
				return null;
			}
		});
	}
	
	public interface CurrencyConverterListener{
		public void onFinishedGettingCurrencyData(CurrencyConverterSingelton currencies);
		public void onErrorGettingCurrencyData(Exception e);
	}
	
	
	/**
	 * 
	 * @author alon
	 *
	 */
	public class Currency{
		final int SATHOSIES_IN_ONE_BTC = 100000000;
		
		/**
		 * bitcoinaverage.com parser
		 * @throws JSONException 
		 * @throws ParseException 
		 */
		public void updateFromBitcoinaverage(JSONObject object) throws JSONException, ParseException{
			ONE_BTC_TO_CURRENCY = object.getDouble("last");
			ONE_CURRENCY_TO_BTC = 1 / ONE_BTC_TO_CURRENCY;
			lastUpdated = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss -0000", Locale.ENGLISH).parse(object.getString("timestamp")); // example Sat, 26 Jul 2014 14:22:27 -0000
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
		public double convertToBTC(double howMuchCurrency){
			return howMuchCurrency * ONE_CURRENCY_TO_BTC * SATHOSIES_IN_ONE_BTC;
		}
		
		double ONE_BTC_TO_CURRENCY;
		public double convertToCurrency(double howMuchBTC){
			return howMuchBTC * ONE_BTC_TO_CURRENCY;
		}
		
		public Date lastUpdated;
	}
}
