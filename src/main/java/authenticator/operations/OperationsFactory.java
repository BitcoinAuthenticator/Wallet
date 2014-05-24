package authenticator.operations;

import static wallettemplate.utils.GuiUtils.crashAlert;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import javafx.application.Platform;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionInput.ConnectMode;
import com.google.bitcoin.core.TransactionInput.ConnectionResult;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptChunk;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import authenticator.Authenticator;
import authenticator.BASE;
import authenticator.BAUtils;
import authenticator.GCM.dispacher.Device;
import authenticator.GCM.dispacher.Dispacher;
import authenticator.GCM.dispacher.MessageType;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;

public class OperationsFactory extends BASE{
	
	public static Logger staticLooger;
	public OperationsFactory() {
		super(OperationsFactory.class);
		staticLooger = this.LOG;
	}

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
						public void PreExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@SuppressWarnings("static-access")
						@Override
						public void Execute(OnOperationUIUpdate listenerUI, ServerSocket ss, String[] args, OnOperationUIUpdate listener) throws Exception {
							 timeout = ss.getSoTimeout();
							 ss.setSoTimeout(0);
							 socket = ss;
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(ss,args,listener); 
							 //Return to previous timeout
							 ss.setSoTimeout(timeout);
						}

						@Override
						public void PostExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) {
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
					int timeout = 5;
					
					@Override
					public void PreExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception {
						cypherBytes = prepareTX(pairingID);
					}

					@Override
					public void Execute(OnOperationUIUpdate listenerUI, ServerSocket ss, String[] args,
							OnOperationUIUpdate listener) throws Exception {
						//
						timeout = ss.getSoTimeout();
						ss.setSoTimeout(0);
						complete(ss);
						System.out.println("Signed Tx - " + BAUtils.getStringTransaction(tx));
						/*SendResult result = Authenticator.getWalletOperation().pushTxWithWallet(tx);
						Futures.addCallback(result.broadcastComplete, new FutureCallback<Transaction>() {
			                @Override
			                public void onSuccess(Transaction result) {
			                	listenerUI.onFinished("Finished With Success");
			                }

			                @Override
			                public void onFailure(Throwable t) {
			                	listenerUI.onError(null,t);
			                }
			            });*/
						ss.setSoTimeout(timeout);
					}

					@Override
					public void PostExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) {
						// TODO Auto-generated method stub
						
					}
					
					//###########
					// Helpers
					//###########
					 @SuppressWarnings("deprecation")
					byte[] prepareTX(String pairingID) throws Exception {
						//Create the payload
						PairingObject pairingObj = Authenticator.getWalletOperation().getPairingObject(pairingID);
						String formatedTx = BAUtils.getStringTransaction(tx);
						System.out.println("Raw unSigned Tx - " + formatedTx);
						//Get pub keys and indexes
						ArrayList<byte[]> pubKeysArr = new ArrayList<byte[]>();
						ArrayList<Integer> indexArr = new ArrayList<Integer>();
						for(TransactionInput in:tx.getInputs())
							for (KeyObject ko: pairingObj.keys.keys){
								String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
								if(inAddress.equals(ko.address)){
									indexArr.add(ko.index);
									BigInteger priv_key = new BigInteger(1, BAUtils.hexStringToByteArray(ko.priv_key));
									byte[] pubkey = ECKey.publicKeyFromPrivate(priv_key, true);//mpPublickeys.get(pairingID).get(a);
									pubKeysArr.add(pubkey);
									break;
								}
							}
						
						SignMessage signMsgPayload = new SignMessage()
										.setInputNumber(tx.getInputs().size())
										.setTxString(formatedTx)
										.setKeyIndexArray(pubKeysArr, indexArr)
										.setVersion(1)
										;
						byte[] jsonBytes = signMsgPayload.serializeToBytes();
						
						/*ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
						byte[] version = BAUtils.hexStringToByteArray("01");
						outputStream.write(version);
						byte[] tempArr = ByteBuffer.allocate(4).putInt(tx.getInputs().size()).array();
						byte[] numIns = Arrays.copyOfRange(tempArr, 2, 4);
						outputStream.write(numIns);
						// Iterate all inputs and pend their public key + index
						for(TransactionInput in:tx.getInputs())
						for (KeyObject ko: pairingObj.keys.keys){
							String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
							if(inAddress.equals(ko.address)){
								byte[] index = ByteBuffer.allocate(4).putInt(ko.index).array();
								outputStream.write(index);
								//BigInteger priv_key = new BigInteger(ko.priv_key.getBytes());
								BigInteger priv_key = new BigInteger(1, BAUtils.hexStringToByteArray(ko.priv_key));
								byte[] pubkey = ECKey.publicKeyFromPrivate(priv_key, true);//mpPublickeys.get(pairingID).get(a);
								ECKey e = new ECKey(null,pubkey);
								outputStream.write(pubkey);
								break;
							}
						}*/
						
						//Convert tx to byte array for sending.
						byte[] transaction = BAUtils.hexStringToByteArray(formatedTx);
						//outputStream.write(transaction);
						//byte payload[] = outputStream.toByteArray( );
						//Calculate the HMAC and concatenate it to the payload
						Mac mac = Mac.getInstance("HmacSHA256");
						SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(Authenticator.getWalletOperation().getAESKey(pairingID)), "AES");
						mac.init(secretkey);
						byte[] macbytes = mac.doFinal(jsonBytes);
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
						outputStream.write(jsonBytes);
						outputStream.write(macbytes);
						byte payload[] = outputStream.toByteArray( );
						
						//outputStream.write(macbytes);
						//payload = outputStream.toByteArray( );
						
						//Encrypt the payload
						Cipher cipher = null;
						try {cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");} 
						 catch (NoSuchAlgorithmException e) {e.printStackTrace();} 
						 catch (NoSuchPaddingException e) {e.printStackTrace();}
						 try {cipher.init(Cipher.ENCRYPT_MODE, secretkey);} 
						 catch (InvalidKeyException e) {e.printStackTrace();}
						 byte[] cipherBytes = null;
						 try {cipherBytes = cipher.doFinal(payload);} 
						 catch (IllegalBlockSizeException e) {e.printStackTrace();} 
						 catch (BadPaddingException e) {e.printStackTrace();}
						 return cipherBytes;
						/*Cipher cipher = null;
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
						}*/
					}
					
					 void complete(ServerSocket ss) throws JSONException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException
					 {
						// Init dispacher
							byte[] cipherKeyBytes;
							Dispacher disp;
							disp = new Dispacher(null,null);
							
							//Send the encrypted payload over to the Authenticator and wait for the response.
							SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(Authenticator.getWalletOperation().getAESKey(pairingID)), "AES");						
							PairingObject po = Authenticator.getWalletOperation().getPairingObject(pairingID);
							byte[] gcmID = po.GCM.getBytes();
							assert(gcmID != null);
							Device d = new Device(po.chain_code.getBytes(),
									po.master_public_key.getBytes(),
									gcmID,
									pairingID.getBytes(),
									secretkey);
							disp.dispachMessage(MessageType.signTx, d);
							//wait for user response
							staticLooger.info("Listening for Authenticator on port "+ Authenticator.LISTENER_PORT +"...");
							Socket socket = ss.accept();
							staticLooger.info("Connected to Authenticator");
							//send tx for signing 
							DataInputStream inStream = new DataInputStream(socket.getInputStream());
							DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
							outStream.writeInt(cypherBytes.length);
							outStream.write(cypherBytes);
							staticLooger.info("Sent transaction");
							int keysize = inStream.readInt();
							cipherKeyBytes = new byte[keysize];
							inStream.read(cipherKeyBytes);
							inStream.close();
							outStream.close();
							
							//Decrypt the response
						    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						    cipher.init(Cipher.DECRYPT_MODE, secretkey);
						    String message = BAUtils.bytesToHex(cipher.doFinal(cipherKeyBytes));
						    String sig = message.substring(0,message.length()-64);
						    String HMAC = message.substring(message.length()-64,message.length());
						    byte[] testsig = BAUtils.hexStringToByteArray(sig);
						    byte[] hash = BAUtils.hexStringToByteArray(HMAC);
						    //Calculate the HMAC of the message and verify it is valid
						    Mac mac = Mac.getInstance("HmacSHA256");
							mac.init(secretkey);
							byte[] macbytes = mac.doFinal(testsig);
							if (Arrays.equals(macbytes, hash)){
								staticLooger.info("Received Signature: " + BAUtils.bytesToHex(testsig));
								staticLooger.info("Building Transaction...");
							}
							else {
								staticLooger.info("Message authentication code is invalid");
							}
							//Prep the keys needed for signing
							byte[] key = BAUtils.hexStringToByteArray(po.master_public_key);
							byte[] chain = BAUtils.hexStringToByteArray(po.chain_code);
							List<TransactionInput> inputs = tx.getInputs();
							//Break apart the signature array sent over from the authenticator
							String sigstr = BAUtils.bytesToHex(testsig);
							ArrayList<byte[]> AuthSigs = SignMessage.deserializeToBytes(testsig);//=  new ArrayList<byte[]>();
							/*int pos = 4;
							for (int b=0; b<tx.getInputs().size(); b++){
								String strlen = sigstr.substring(pos, pos+2);
								int intlen = Integer.parseInt(strlen, 16)*2;
								pos = pos + 2;
								AuthSigs.add(BAUtils.hexStringToByteArray(sigstr.substring(pos, pos+intlen)));
								pos = pos + intlen;
							}*/
							//Loop to create a signature for each input
							int i =0;
							for(TransactionInput in: tx.getInputs()){
								for (KeyObject ko:po.keys.keys){
									String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
									if(inAddress.equals(ko.address))
									{
										//Authenticator Key
										HDKeyDerivation HDKey = null;
										DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
										DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, ko.index);
										byte[] childpublickey = childKey.getPubKey();
										ECKey authKey = new ECKey(null, childpublickey);
										
										//Wallet key
										BigInteger privatekey = new BigInteger(1, BAUtils.hexStringToByteArray(ko.priv_key));
										byte[] walletPublicKey = ECKey.publicKeyFromPrivate(privatekey, true);
										ECKey walletKey = new ECKey(privatekey, walletPublicKey, true);
										
										// Create Program for the script
										List<ECKey> keys = ImmutableList.of(authKey, walletKey);
										Script scriptpubkey = ScriptBuilder.createMultiSigOutputScript(2,keys);
										byte[] program = scriptpubkey.getProgram();
										
										//Create P2SH
										// IMPORTANT - AuthSigs and the signiture we create here should refer to the same input !!
										TransactionSignature sig1 = TransactionSignature.decodeFromBitcoin(AuthSigs.get(i), true);
										TransactionSignature sig2 = tx.calculateSignature(i, walletKey, scriptpubkey, Transaction.SigHash.ALL, false);
										List<TransactionSignature> sigs = ImmutableList.of(sig1, sig2);
										Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(sigs, program);
										TransactionInput input = inputs.get(i);
										input.setScriptSig(inputScript);
										break;
									}
								}
								i++;
							}
					 }
				});
	}
}
