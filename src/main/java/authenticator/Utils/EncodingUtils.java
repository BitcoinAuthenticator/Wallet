package authenticator.Utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Formatter;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class EncodingUtils {
	
	/**For reading the JSON*/
	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }

	/**Reads JSON object from a URL*/
	public static void readFromUrl(String url, AsyncCompletionHandler<Response> listener) throws IOException { 
		AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>(){

			@Override
			public Response onCompleted(Response response) throws Exception {
				listener.onCompleted(response);
				asyncHttpClient.closeAsynchronously();
				return null;
			}
			
		});
		
	}

	static public String getAbsolutePathForFile(String fileName) throws IOException
	{
		return new java.io.File( "." ).getCanonicalPath() + "/" + fileName;
	}
	
	public static String getStringTransaction(Transaction tx)
	{
		//Convert tx to byte array for sending.
		String formatedTx = null;
		final StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		try {
		    ByteArrayOutputStream os = new ByteArrayOutputStream();
		    tx.bitcoinSerialize(os);
		    byte[] bytes = os.toByteArray();
		    for (byte b : bytes) {
		        formatter.format("%02x", b);  
		    }
		    formatedTx = sb.toString();
		}catch (IOException e) {
		} finally {
		    formatter.close();
		}
		return formatedTx;
	}
	

}
