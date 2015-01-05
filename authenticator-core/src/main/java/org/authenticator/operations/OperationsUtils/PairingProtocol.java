package org.authenticator.operations.OperationsUtils;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.crypto.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.encoders.Hex;

import org.authenticator.BAApplicationParameters.NetworkType;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.network.BANetworkInfo;
import org.authenticator.network.messages.PongPayload;
import org.authenticator.operations.listeners.OperationListener;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.utils.BAPassword;

/**
 * <b>Pairing Protocol</b><br>
 * <ol>
 * <li>Generate a secret AES key that will be used in all encrypted communications</li>
 * <li>Generate the Authenticator's pairing index by hashing (SHA2) this wallet's seed + the account index and taking the right most 31 bits, H(Seed | Index)</li>
 * <li>Display a QR for the pairing</li>
 * <li>On connection from the Authenticator, send a pong message letting it know its connected with the wallet</li>
 * <li>Receive the encrypted pairing payload that can be:
 * 	<ol>
 * 		<li>pairing parameters:
 * 			<ol>
 * 				<li>Master public key</li>
 * 				<li>chain code</li>
 * 				<li>Pairing ID</li>
 * 				<li>GCM registration id</li>
 * 			</ol>
 * 		</li>
 * 	</ol>
 * </li>
 * <li>Create a paired account</li>
 * <li>Fire a new paired account event</li>
 * 
 * </ol>
 */
public class PairingProtocol {
	
  /**
   * Run a complete process of pairing with an authenticator device.<br>
   * args[]:
   * <ol start="0">
   * <li>Pairing name</li>
   * <li>Account ID, could be blank ("") if none is provided</li>
   * <li>Pair type, by default "blockchain"</li>
   * <li>NetworkType - 1 for main net, 0 for testnet</li>
   * <li>Secret Key - could be blank ("") if none is provided</li>
   * </ol>
   * 
   * @param wallet
   * @param ss
   * @param timeout
   * @param netInfo
   * @param args
   * @param opListener
   * @param statusListener
   * @param isRepairingAccount
   * @param walletPW
   * @throws Exception
   */
  public void run (WalletOperation wallet,
		  ServerSocket ss,
		  int timeout,
		  BANetworkInfo netInfo,
		  String[] args, 
		  OperationListener opListener,
		  PairingStageUpdater statusListener,
		  boolean isRepairingAccount,
		  @Nullable BAPassword walletPW) throws Exception {

	  // get all args
	  assert(args != null);
	  String argPairingName 			= args[0];
	  Integer argAccountID 				= args[1].length() == 0? wallet.whatIsTheNextAvailableAccountIndex():Integer.parseInt(args[1]);
	  String argWalletType 				= args[2];
	  Integer argNetworkType 			= Integer.parseInt(args[3]);
	  String argKeyHex 					= args[4].length() == 0? null:args[4];
	  
	  postUIStatusUpdate(statusListener, PairingStage.PAIRING_SETUP, null);
	  
	  String ip = netInfo.EXTERNAL_IP;
	  String localip = netInfo.INTERNAL_IP;
	  
	  String key = null;
	  SecretKey sharedsecret = null;
	  if(argKeyHex == null) {
		  sharedsecret = CryptoUtils.generateNewSecretAESKey();
	      byte[] raw = sharedsecret.getEncoded();
	      key = Hex.toHexString(raw);
	  }
	  else {
		  sharedsecret = CryptoUtils.secretKeyFromHexString(argKeyHex);
		  key = argKeyHex;
	  }
      
      // generate Authenticator's account number
      byte[] seed = wallet.getWalletSeedBytes(walletPW);
	  byte[] authWalletIndex = generateAuthenticatorsWalletIndex(seed, argAccountID);
      
	  //Display a QR code for the user to scan
	  PairingQRCode PairingQR = new PairingQRCode();
	  byte[] qr = PairingQR.generateQRImageBytes(ip, localip, argPairingName, argWalletType, key, argNetworkType, authWalletIndex);
	  Socket socket = dispalyQRAnListenForCommunication(qr,
			  ss, 
			  timeout, 
			  statusListener);
	  if(socket == null)
		  return;
	  
	  /*
	   * Send data
	   */
	  DataInputStream in = new DataInputStream(socket.getInputStream());
	  DataOutputStream out = new DataOutputStream(socket.getOutputStream());
	  // 1) send pong to authenticator
	  PongPayload pp = new PongPayload();
	  out.writeInt(pp.getPayloadSize());
	  out.write(pp.getBytes());
	  
	  // 2) Receive payload
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
		  String pairingID = Hex.toHexString(authWalletIndex);
		  String GCM = (String) jsonObject.get("gcmID");
		  
		  if(isRepairingAccount) {
			  wallet.updatePairingGCMRegistrationID(pairingID, GCM);
			  
		  }
		  else {
			//Save to file
			  PairedAuthenticator  obj = wallet.generatePairing(mPubKey, 
																  chaincode, 
																  key, 
																  GCM, 
																  pairingID, 
																  argPairingName, 
																  argAccountID,
																  NetworkType.fromIndex(argNetworkType),
					  											  true,
																  walletPW);
			  statusListener.pairingData(obj);
		  }
		  
		  
		  System.out.println("Pairing Details:" +
		  			 " Master Public Key: " + mPubKey + "\n" +
		  			 "chaincode: " +  chaincode + "\n" +
		  			 "gcmRegId: " +  GCM + "\n" + 
		  			 "pairing ID: " + pairingID);		 
		  postUIStatusUpdate(statusListener, PairingStage.FINISHED, null);
		  
	  }
	  else {
		  System.out.println("Message authentication code is invalid");
		  postUIStatusUpdate(statusListener, PairingStage.FAILED, null);
	  }

  }

	/**
	 *
	 * @param qrImage
	 * @param ss
	 * @param timeout
	 * @param listener
	 * @return
	 */
  public Socket dispalyQRAnListenForCommunication(byte[] qrImage,
		  ServerSocket ss, 
		  int timeout,
		  PairingStageUpdater listener){
	  System.out.println("Listening for connection ("  + timeout + " sec timeout) ...");
	  postUIStatusUpdate(listener, PairingStage.WAITING_FOR_SCAN, qrImage);
	  try {
		ss.setSoTimeout( timeout );
		Socket socket = ss.accept();
		//QR.CloseWindow();
		System.out.println("Connected to Alice");
	    postUIStatusUpdate(listener, PairingStage.CONNECTED, null);		 
		  
		  return socket;
	} catch (IOException e) {
		System.out.println("Connection timedout");
		postUIStatusUpdate(listener, PairingStage.CONNECTION_TIMEOUT, null);
	}	  
	return null;
	  
  }
  
  public String decipherDataFromAuthenticator(byte[] cipherKeyBytes, PairingStageUpdater listener, SecretKey sharedsecret) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException{
	  postUIStatusUpdate(listener, PairingStage.DECRYPTING_MESSAGE, null);
	  Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
	  cipher.init(Cipher.DECRYPT_MODE, sharedsecret);
	  String payload = Hex.toHexString(cipher.doFinal(cipherKeyBytes));
	  return payload;
  }
  
  public JSONObject parseAndVerifyPayload(String payload, SecretKey sharedsecret, PairingStageUpdater listener) throws NoSuchAlgorithmException, InvalidKeyException, ParseException{
	  byte[] testpayload = Hex.decode(payload.substring(0,payload.length()-64));
	  byte[] hash = Hex.decode(payload.substring(payload.length()-64,payload.length()));
	  Mac mac = Mac.getInstance("HmacSHA256");
	  mac.init(sharedsecret);
	  byte[] macbytes = mac.doFinal(testpayload);
	  if (Arrays.equals(macbytes, hash)){
		//Parse the received json object
		  String strJson = new String(testpayload);
		  JSONParser parser=new JSONParser();
		  Object obj = parser.parse(strJson);
		  JSONObject jsonObject = (JSONObject) obj;	  
		  return  jsonObject;
	  }
	
	  return null;
	  
  }
  
  public byte[] generateAuthenticatorsWalletIndex(byte[] seed, int walletIndex) {
	MessageDigest md = null;
	try {md = MessageDigest.getInstance("SHA-256");}
	catch(NoSuchAlgorithmException e) {e.printStackTrace();} 
	byte[] digest = md.digest((Hex.toHexString(seed) + "_" + Integer.toString(walletIndex)).getBytes());
	byte[] complete = new BigInteger(1, digest).toString(16).getBytes();
	// copy the right most 31 bits
	byte[] ret = new byte[4];
	System.arraycopy(complete, complete.length - 5, ret, 0, 4);

	return ret;
  }
  
  public  void postUIStatusUpdate(PairingStageUpdater listener, PairingStage stage, @Nullable byte[] qrImageBytes)
  {
	  if(listener != null)
		  listener.onPairingStageChanged(stage, qrImageBytes);
  }
  
  public interface PairingStageUpdater{
	  /**
	   * Will update on the various pairing stages, will pass a QR image as bytes on WAITING_FOR_SCAN.
	   * 
	   * @param stage
	   * @param qrImageBytes
	   */
	  public void onPairingStageChanged(PairingStage stage, @Nullable byte[] qrImageBytes);
	  
	  /**
	   * Will pass a paired authenticator object when it is created at the end of the pairing process
	   * 
	   * @param data
	   */
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