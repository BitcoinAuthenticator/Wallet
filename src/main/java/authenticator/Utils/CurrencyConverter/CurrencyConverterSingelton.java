package authenticator.Utils.CurrencyConverter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.Utils.EncodingUtils;
import authenticator.Utils.CurrencyConverter.exceptions.CurrencyConverterSingeltonNoDataException;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class CurrencyConverterSingelton {
	public static CurrencyList currencies;
	public static List<String> currenciesList;
	
	static public boolean isReady = false;
	/**
	 * Requires downloading the currency data in an async process.<br>
	 * Use this method as follows:<br>
	 * <b>new CurrencyConverterSingelton(new CurrencyConverterListener(){ });</b>
	 * 
	 * @param listener
	 */
	public CurrencyConverterSingelton(String[] lstCurrencies, CurrencyConverterListener listener){
		currenciesList = Arrays.asList(lstCurrencies);
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
	
	public static void CANNOT_EXECUTE_ASYNC_SO_CHECK_IS_READY() throws CurrencyConverterSingeltonNoDataException{
		if(CurrencyConverterSingelton.isReady != true)
			throw new CurrencyConverterSingeltonNoDataException("No Currency Data");
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
	private void getCurrencies(List<String> lstCurrencies, final CurrencyConverterListener listener, CurrencyConverterSingelton self) throws IOException, JSONException{
		// TODO - download more than one currency
		String s = "https://api.bitcoinaverage.com/ticker/global/" + lstCurrencies.get(0) +  "/";
		EncodingUtils.readFromUrl(s, new AsyncCompletionHandler<Response>(){
			@Override
			public Response onCompleted(Response arg0) throws Exception {
				String res = arg0.getResponseBody();
				JSONObject json = new JSONObject(res);
				//double last = json.getDouble("last");
				CurrencyConverterSingelton.currencies.add(new Currency(lstCurrencies.get(0), json));
				
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
	
	public class CurrencyList{
		private List<Currency> lst;

		public CurrencyList() {
			lst = new ArrayList<Currency>();
		}
		
		public void add(Currency c) {
			lst.add(c);
		}
		
		public Currency get(int index) {
			return lst.get(index);
		}
				
		public Currency get(String upperCaseCurrencyCode) {
			for(Currency c: lst) {
				if(c.UPPERCASE_CURRENCY_CODE.equals(upperCaseCurrencyCode))
					return c;
			}
			return null;
		}
	}
}
