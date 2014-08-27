package authenticator.operations.OperationsUtils;

import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import authenticator.BAApplicationParameters.NetworkType;
import authenticator.Utils.EncodingUtils;
import authenticator.db.walletDB;
import authenticator.network.BANetworkInfo;
import authenticator.network.UpNp;
import authenticator.operations.listeners.OperationListener;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.walletCore.WalletOperation;

/**
 * This is the wallet side of the Pairing Protocol. It uses UpNp to map a port on the router if there is one,
 * opens a port for Alice, displays a QR code for the user to scan, and receives the master public key and chaincode.
 */
public class PairingProtocol {
	
  /**
   * Run a complete process of pairing with an authenticator device.<br>
   * args[]:
   * <ol>
   * <li>Pairing name</li>
   * <li>Account ID, could be blank ("") if none is provided</li>
   * <li>Pair type, by default "blockchain"</li>
   * <li>NetworkType - 1 for main net, 0 for testnet</li>
   * </ol>
   * 
   * @param wallet
   * @param ss
   * @param timeout - in miliseconds  (0 for no timeout)
   * @param netInfo
   * @param args
   * @param opListener
   * @param statusListener
   * @param displayQRAnimation
   * @param animationAfterPairing
   * @throws Exception
   */
  public void run (WalletOperation wallet,
		  ServerSocket ss,
		  int timeout,
		  BANetworkInfo netInfo,
		  String[] args, 
		  OperationListener opListener,
		  PairingStageUpdater statusListener,
		  Runnable displayQRAnimation, 
		  Runnable animationAfterPairing) throws Exception {

	  assert(args != null);
	  String walletType = args[2];
	  
	  postUIStatusUpdate(statusListener, PairingStage.PAIRING_SETUP);
	  
	  //UpNp plugnplay = new UpNp();
	  String ip = netInfo.EXTERNAL_IP;//plugnplay.getExternalIP();
	  String localip = netInfo.INTERNAL_IP;//plugnplay.getLocalIP().substring(1);
	  
	  //Generate 256 bit key.
	  KeyGenerator kgen = KeyGenerator.getInstance("AES");
      kgen.init(256);

      // Generate the secret key specs.
      //sharedsecret = kgen.generateKey();
      SecretKey sharedsecret = kgen.generateKey();
      byte[] raw = sharedsecret.getEncoded();
      String key = EncodingUtils.bytesToHex(raw);
	  
	  //Display a QR code for the user to scan
	  PairingQRCode PairingQR = new PairingQRCode(ip, localip, walletType, key, Integer.parseInt(args[3]));
	  Socket socket = dispalyQRAnListenForCommunication(ss, 
			  timeout, 
			  statusListener,
			  displayQRAnimation, 
			  animationAfterPairing);
	  if(socket == null)
		  return;
	  
	  // Receive payload
	  DataInputStream in = new DataInputStream(socket.getInputStream());
	  int keysize = in.readInt();
	  byte[] cipherKeyBytes = new byte[keysize];
	  in.read(cipherKeyBytes);
	  String payload = decipherDataFromAuthenticator(cipherKeyBytes, statusListener, sharedsecret);
    
	  // Verify HMAC
	  JSONObject jsonObject = parseAndVerifyPayload(payload, sharedsecret, statusListener);
	 
	  if (jsonObject != null){
		//Parse the received json object
		  String mPubKey = (String) jsonObject.get("mpubkey");
		  String chaincode = (String) jsonObject.get("chaincode");
		  String pairingID = (String) jsonObject.get("pairID");
		  String GCM = (String) jsonObject.get("gcmID");
		  Integer accID = args[1].length() == 0? null:Integer.parseInt(args[1]);
		  //Save to file
		  PairedAuthenticator  obj = wallet.generatePairing(mPubKey, 
															  chaincode, 
															  key, 
															  GCM, 
															  pairingID, 
															  args[0], 
															  accID,
															  NetworkType.fromString(args[2]));
		  
		  postUIStatusUpdate(statusListener, PairingStage.FINISHED);
		  statusListener.pairingData(obj);
	  }
	  else {
		  System.out.println("Message authentication code is invalid");
		  postUIStatusUpdate(statusListener, PairingStage.FAILED);
	  }

  }
  
  /**
   * 
   * @param ss
   * @param timeout - in miliseconds
   * @param listener
   * @param displayQRAnimation
   * @param animationAfterPairing
   * @return
   */
  public Socket dispalyQRAnListenForCommunication(ServerSocket ss, 
		  int timeout,
		  PairingStageUpdater listener, 
		  Runnable displayQRAnimation, 
		  Runnable animationAfterPairing){
	  //DisplayQR QR = new DisplayQR();
	  //QR.displayQR();   
	  
	  if(displayQRAnimation != null)
		  displayQRAnimation.run();
	  
	  System.out.println("Listening for connection ("  + timeout + " sec timeout) ...");
	  postUIStatusUpdate(listener, PairingStage.WAITING_FOR_SCAN);
	  try {
		ss.setSoTimeout( timeout );
		Socket socket = ss.accept();
		//QR.CloseWindow();
		System.out.println("Connected to Alice");
	    postUIStatusUpdate(listener, PairingStage.CONNECTED);		 
		  
		  return socket;
	} catch (IOException e) {
		System.out.println("Connection timedout");
		postUIStatusUpdate(listener, PairingStage.CONNECTION_TIMEOUT);
	}
	finally{
		 if(animationAfterPairing != null)
			  animationAfterPairing.run();
    }
	  
	return null;
	  
  }
  
  public String decipherDataFromAuthenticator(byte[] cipherKeyBytes, PairingStageUpdater listener, SecretKey sharedsecret) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException{
	  postUIStatusUpdate(listener, PairingStage.DECRYPTING_MESSAGE);
	  Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
	  cipher.init(Cipher.DECRYPT_MODE, sharedsecret);
	  String payload = EncodingUtils.bytesToHex(cipher.doFinal(cipherKeyBytes));
	  return payload;
  }
  
  public JSONObject parseAndVerifyPayload(String payload, SecretKey sharedsecret, PairingStageUpdater listener) throws NoSuchAlgorithmException, InvalidKeyException, ParseException{
	  byte[] testpayload = EncodingUtils.hexStringToByteArray(payload.substring(0,payload.length()-64));
	  byte[] hash = EncodingUtils.hexStringToByteArray(payload.substring(payload.length()-64,payload.length()));
	  Mac mac = Mac.getInstance("HmacSHA256");
	  mac.init(sharedsecret);
	  byte[] macbytes = mac.doFinal(testpayload);
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
		  
//		  postUIStatusUpdate(listener,"Received Master Public Key: " + mPubKey + "\n" +
//		  			 "chaincode: " +  chaincode + "\n" +
//		  			 "gcmRegId: " +  GCM + "\n" + 
//		  			 "pairing ID: " + pairingID);
		  
		  return  jsonObject;
	  }
	
	  return null;
	  
  }
  
  public  void postUIStatusUpdate(PairingStageUpdater listener, PairingStage stage)
  {
	  if(listener != null)
		  listener.onPairingStageChanged(stage);
  }
  
  public interface PairingStageUpdater{
	  public void onPairingStageChanged(PairingStage stage);
	  public void pairingData(PairedAuthenticator data);
  }
  
  public enum PairingStage{
	  PAIRING_SETUP,
	  PAIRING_STARTED,
	  WAITING_FOR_SCAN,
	  CONNECTED,
	  DECRYPTING_MESSAGE,
	  FINISHED,
	  
	  CONNECTION_TIMEOUT,
	  FAILED;
  }
}