package authenticator.operations.OperationsUtils;

import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import authenticator.Utils.BAUtils;
import authenticator.db.ConfigFile;
import authenticator.network.UpNp;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.ui_helpers.BAApplication.NetworkType;

/**
 * This is the wallet side of the Pairing Protocol. It uses UpNp to map a port on the router if there is one,
 * opens a port for Alice, displays a QR code for the user to scan, and receives the master public key and chaincode.
 */
public class PairingProtocol {
	/*public  DataInputStream in;
	public  DataOutputStream out;
	public static byte[] chaincode;
	public static byte[] mPubKey;
	public static byte[] gcmRegId;
	public static byte[] pairingID;
	public static SecretKey sharedsecret;*/
	//public  SecretKey sharedsecret;
	
  /**
   * Run a complete process of pairing with an authenticator device.<br>
   * args[]:
   * <ol>
   * <li>Pairing name</li>
   * <li>Pair type, by default "blockchain"</li>
   * <li>NetworkType - 1 for main net, 0 for testnet</li>
   * </ol>
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
	  QRCode PairingQR = new QRCode(ip, localip, walletType, key, Integer.parseInt(args[2]));
	  Socket socket = dispalyQRAnListenForCommunication(ss, listener);
    
	  // Receive payload
	  String payload = receivePairingDataFromAuthenticator(socket, listener, sharedsecret);
    
	  // Verify HMAC
	  JSONObject jsonObject = parseAndVerifyPayload(payload, sharedsecret, listener);
	 
	  if (jsonObject != null){
		//Parse the received json object
		  String mPubKey = (String) jsonObject.get("mpubkey");
		  String chaincode = (String) jsonObject.get("chaincode");
		  String pairingID = (String) jsonObject.get("pairID");
		  String GCM = (String) jsonObject.get("gcmID");
		  //Save to file
		  authenticator.Authenticator.getWalletOperation().generateNewPairing(mPubKey, 
				  chaincode, 
				  key, 
				  GCM, 
				  pairingID, 
				  args[0], 
				  NetworkType.fromString(args[2]));
	  }
	  else {
		  System.out.println("Message authentication code is invalid");
		  postUIStatusUpdate(listener,"Message authentication code is invalid");
	  }

  }
  
  public Socket dispalyQRAnListenForCommunication(ServerSocket ss, OnOperationUIUpdate listener) throws IOException{
	  DisplayQR QR = new DisplayQR();
	  QR.main(null);    
	  Socket socket = ss.accept();
	  QR.CloseWindow();
	  System.out.println("Connected to Alice");
	  postUIStatusUpdate(listener,"Connected to Alice");
	  return socket;
  }
  
  public String receivePairingDataFromAuthenticator(Socket socket, OnOperationUIUpdate listener, SecretKey sharedsecret) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException{
	  postUIStatusUpdate(listener,"Decrypting message ...");
	  DataInputStream in = new DataInputStream(socket.getInputStream());
	  DataOutputStream out = new DataOutputStream(socket.getOutputStream());
	  int keysize = in.readInt();
	  byte[] cipherKeyBytes = new byte[keysize];
	  in.read(cipherKeyBytes);
	  Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
	  cipher.init(Cipher.DECRYPT_MODE, sharedsecret);
	  String payload = BAUtils.bytesToHex(cipher.doFinal(cipherKeyBytes));
	  return payload;
  }
  
  public JSONObject parseAndVerifyPayload(String payload, SecretKey sharedsecret, OnOperationUIUpdate listener) throws NoSuchAlgorithmException, InvalidKeyException, ParseException{
	  byte[] testpayload = BAUtils.hexStringToByteArray(payload.substring(0,payload.length()-64));
	  byte[] hash = BAUtils.hexStringToByteArray(payload.substring(payload.length()-64,payload.length()));
	  Mac mac = Mac.getInstance("HmacSHA256");
	  mac.init(sharedsecret);
	  byte[] macbytes = mac.doFinal(testpayload);
	  postUIStatusUpdate(listener,"Parsing message ...");
	  if (Arrays.equals(macbytes, hash)){
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
		  
		  return  jsonObject;
	  }
	
	  return null;
	  
  }
  
  public  void postUIStatusUpdate(OnOperationUIUpdate listener, String str)
  {
	  if(listener != null)
		  listener.statusReport(str);
  }

}