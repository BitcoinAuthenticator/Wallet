package authenticator.operations.OperationsUtils;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import authenticator.BAUtils;
import authenticator.UpNp;
import authenticator.WalletFile;
import authenticator.operations.OnOperationUIUpdate;

/**
 * This is the wallet side of the Pairing Protocol. It uses UpNp to map a port on the router if there is one,
 * opens a port for Alice, displays a QR code for the user to scan, and receives the master public key and chaincode.
 */
public class PairingProtocol {
	public  DataInputStream in;
	public  DataOutputStream out;
	/*public static byte[] chaincode;
	public static byte[] mPubKey;
	public static byte[] gcmRegId;
	public static byte[] pairingID;
	public static SecretKey sharedsecret;*/
	public  SecretKey sharedsecret;
	
  /**
   * Run a complete process of pairing with an authenticator device
   * args[] - 
   * 	0 - Pairing name
   * 	1 - Pair type, by default "blockchain"
   * 
   * 
   * @param {@link java.net.ServerSocket} ss
   * @param args
   * @param {@link authenticator.operations.OnOperationUIUpdate} listener
   * @throws Exception
   */
  public void run (ServerSocket ss,String[] args, OnOperationUIUpdate listener) throws Exception {

	  assert(args != null);
	  String walletType = args[1];
	  final int port = 1234;

	  // Open a port and wait for a connection
	  UpNp plugnplay = new UpNp();
	  System.out.println("Listening for Alice on port "+port+"...");
	  postUIStatusUpdate(listener, "Listening for Alice on port "+port+"...");
	  String ip = plugnplay.getExternalIP();
	  String localip = plugnplay.getLocalIP().substring(1);
	  
	  //Generate 256 bit key.
	  KeyGenerator kgen = KeyGenerator.getInstance("AES");
      kgen.init(256);

      // Generate the secret key specs.
      //sharedsecret = kgen.generateKey();
      SecretKey sharedsecret = kgen.generateKey();
      byte[] raw = sharedsecret.getEncoded();
      String key = BAUtils.bytesToHex(raw);
	  
	  //Display a QR code for the user to scan
	  QRCode PairingQR = new QRCode(ip, localip, walletType, key);
	  DisplayQR QR = new DisplayQR();
	  QR.main(null);    
	  Socket socket = ss.accept();
	  QR.CloseWindow();
	  System.out.println("Connected to Alice");
	  postUIStatusUpdate(listener,"Connected to Alice");
    
	  // Receive payload
	  in = new DataInputStream(socket.getInputStream());
	  out = new DataOutputStream(socket.getOutputStream());
	  int keysize = in.readInt();
	  byte[] cipherKeyBytes = new byte[keysize];
	  in.read(cipherKeyBytes);
	  Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
	  cipher.init(Cipher.DECRYPT_MODE, sharedsecret);
	  String payload = BAUtils.bytesToHex(cipher.doFinal(cipherKeyBytes));
    
	  // Verify HMAC
	  byte[] testpayload = BAUtils.hexStringToByteArray(payload.substring(0,payload.length()-64));
	  byte[] hash = BAUtils.hexStringToByteArray(payload.substring(payload.length()-64,payload.length()));
	  Mac mac = Mac.getInstance("HmacSHA256");
	  mac.init(sharedsecret);
	  byte[] macbytes = mac.doFinal(testpayload);
	  if (Arrays.equals(macbytes, hash)){
		  /*mPubKey = hexStringToByteArray(payload.substring(0,66));
		  chaincode = hexStringToByteArray(payload.substring(66,130));
		  pairingID = hexStringToByteArray(payload.substring(130,210));
		  gcmRegId = hexStringToByteArray(payload.substring(210,payload.length()-64));*/
		//Parse the received json object
		  String strJson = new String(testpayload);
		  JSONParser parser=new JSONParser();
		  Object obj = parser.parse(strJson);
		  JSONObject jsonObject = (JSONObject) obj;
		  String mPubKey = (String) jsonObject.get("mpubkey");
		  String chaincode = (String) jsonObject.get("chaincode");
		  String pairingID = (String) jsonObject.get("pairID");
		  String GCM = (String) jsonObject.get("gcmID");
		  System.out.println("Received Master Public Key: " + mPubKey + "\n" +
		  				  			 "chaincode: " +  chaincode + "\n" +
		  				  			 "gcmRegId: " +  GCM + "\n" + 
		  				  			 "pairing ID: " + pairingID);
		  postUIStatusUpdate(listener,"Received Master Public Key: " + mPubKey + "\n" +
		  			 "chaincode: " +  chaincode + "\n" +
		  			 "gcmRegId: " +  GCM + "\n" + 
		  			 "pairing ID: " + pairingID);
		  //Save to file
		  WalletFile file = new WalletFile();
		  file.writePairingData(mPubKey, chaincode, key, GCM, pairingID, args[0]);
	  }
	  else {
		  System.out.println("Message authentication code is invalid");
		  postUIStatusUpdate(listener,"Message authentication code is invalid");
	  }
	  
	  //dispose
	  //in.close();
	  //out.close();
	  //plugnplay.removeMapping();
	  //ss.close();
	  // Return to main

  }
  
  public  void postUIStatusUpdate(OnOperationUIUpdate listener, String str)
  {
	  if(listener != null)
		  listener.statusReport(str);
  }
  
  /*final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}*/
}