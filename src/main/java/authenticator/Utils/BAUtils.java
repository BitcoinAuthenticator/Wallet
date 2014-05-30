package authenticator.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Formatter;

import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.crypto.DeterministicKey;

public class BAUtils {

	static public String getAbsolutePathForFile(String fileName) throws IOException
	{
		return new java.io.File( "." ).getCanonicalPath() + "/" + fileName;
	}
	
	/**Hex encodes a DeterministicKey object*/
	private static String hexEncodePub(DeterministicKey pubKey) {
        return hexEncode(pubKey.getPubKey());
    }
    private static String hexEncode(byte[] bytes) {
        return new String(Hex.encode(bytes));
    }
    
    /**Converts a byte array to a hex string*/
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	/**Converts a hex string to a byte array*/
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
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
