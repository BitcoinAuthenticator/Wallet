package authenticator.operations;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;

import authenticator.Authenticator;
import authenticator.BAUtils;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.operations.OperationsUtils.PairingProtocol;

public class OperationsFactory {
	
	static public ATOperation PAIRING_OPERATION(String pairingName){
		return new ATOperation(ATOperationType.Pairing)
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetBeginMsg("Pairing Started ...")
					.SetFinishedMsg("Finished pairing")
					.SetArguments(new String[]{pairingName, "blockchain"})
					.SetOperationAction(new OperationActions(){
						int timeout = 5;
						ServerSocket socket = null;
						@Override
						public void PreExecution(String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@SuppressWarnings("static-access")
						@Override
						public void Execute(ServerSocket ss, String[] args, OnOperationUIUpdate listener) throws Exception {
							 timeout = ss.getSoTimeout();
							 ss.setSoTimeout(0);
							 socket = ss;
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(ss,args,listener); 
							 //Return to previous timeout
							 ss.setSoTimeout(timeout);
						}

						@Override
						public void PostExecution(String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void OnExecutionError(Exception e) {
							try {
								socket.setSoTimeout(timeout);
							} catch (SocketException e1) {
							
							}
						}
						
					});
	}

	static public ATOperation SIGN_AND_BROADCAST_TX_OPERATION(Transaction tx, String pairingID){
		return new ATOperation(ATOperationType.SignTx)
				.SetDescription("Sign Raw Transaction By Authenticator device")
				.SetOperationAction(new OperationActions(){
					byte[] cypherBytes;
					
					@Override
					public void PreExecution(String[] args) throws Exception {
						cypherBytes = sendTX(pairingID,tx);
					}

					@Override
					public void Execute(ServerSocket ss, String[] args,
							OnOperationUIUpdate listener) throws Exception {
					}

					@Override
					public void PostExecution(String[] args) throws Exception {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void OnExecutionError(Exception e) {
						// TODO Auto-generated method stub
						
					}
					
					//###########
					// Helpers
					//###########
					 byte[] sendTX(String pairingID, Transaction tx) throws Exception {
						//Create the payload
						PairingObject pairingObj = Authenticator.getWalletOperation().getPairingObject(pairingID);
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
						byte[] version = BAUtils.hexStringToByteArray("01");
						outputStream.write(version);
						byte[] tempArr = ByteBuffer.allocate(4).putInt(tx.getInputs().size()).array();
						byte[] numIns = Arrays.copyOfRange(tempArr, 2, 4);
						outputStream.write(numIns);
						for (KeyObject ko: pairingObj.keys.keys){
							byte[] index = ByteBuffer.allocate(4).putInt(ko.index).array();
							outputStream.write(index);
							BigInteger priv_key = new BigInteger(ko.priv_key.getBytes());
							byte[] pubkey = ECKey.publicKeyFromPrivate(priv_key, true);//mpPublickeys.get(pairingID).get(a);
							outputStream.write(pubkey);
						}
						
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
						    System.out.println("Raw Unsigned Transaction: " + sb.toString());
						    formatedTx = sb.toString();
						}catch (IOException e) {
							System.out.println("Couldn't serialize to hex string.");
						} finally {
						    formatter.close();
						}
						byte[] transaction = BAUtils.hexStringToByteArray(formatedTx);
						outputStream.write(transaction);
						byte payload[] = outputStream.toByteArray( );
						//Calculate the HMAC and concatenate it to the payload
						Mac mac = Mac.getInstance("HmacSHA256");
						SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(Authenticator.getWalletOperation().getAESKey(pairingID)), "AES");
						mac.init(secretkey);
						byte[] macbytes = mac.doFinal(payload);
						outputStream.write(macbytes);
						payload = outputStream.toByteArray( );
						//Encrypt the payload
						Cipher cipher = null;
						try {
							cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						} catch (NoSuchPaddingException e) {
							e.printStackTrace();
						}
				      try {
							cipher.init(Cipher.ENCRYPT_MODE, secretkey);
						} catch (InvalidKeyException e) {
							e.printStackTrace();
						}
				      byte[] cipherBytes = null;
						try {
							cipherBytes = cipher.doFinal(payload);
						} catch (IllegalBlockSizeException e) {
							e.printStackTrace();
						} catch (BadPaddingException e) {
							e.printStackTrace();
						}
						
						return cipherBytes;
					}
					
				});
	}
}
