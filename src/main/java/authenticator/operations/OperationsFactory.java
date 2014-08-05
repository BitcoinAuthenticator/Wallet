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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.TransactionInput.ConnectMode;
import com.google.bitcoin.core.TransactionInput.ConnectionResult;
import com.google.bitcoin.core.TransactionOutPoint;
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
import com.google.protobuf.ByteString;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.BASE;
import authenticator.WalletOperation;
import authenticator.GCM.dispacher.Device;
import authenticator.GCM.dispacher.Dispacher;
import authenticator.Utils.EncodingUtils;
import authenticator.db.ConfigFile;
import authenticator.network.BANeworkInfo;
import authenticator.operations.BAOperation.BANetworkRequirement;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.SignProtocol;
import authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.ATOperationType;
import authenticator.protobuf.ProtoConfig.PendingRequest;

public class OperationsFactory extends BASE{
	
	public static Logger staticLooger;
	public OperationsFactory() {
		super(OperationsFactory.class);
		staticLooger = this.LOG;
	}

	/**
	 * An operation for pairing the wallet with an Authenticator app.<br> 
	 * 
	 * @param wallet
	 * @param pairingName
	 * @param accountID - if != null will force the account ID
	 * @param networkType
	 * @param animation
	 * @param animationAfterPairing
	 * @return
	 */
	static public BAOperation PAIRING_OPERATION(WalletOperation wallet,
			String pairingName, 
			@Nullable Integer accountID,
			NetworkType networkType, 
			Runnable animation,
			Runnable animationAfterPairing){
		return new BAOperation(ATOperationType.Pairing)
					.setOperationNetworkRequirements(BANetworkRequirement.PORT_MAPPING)	
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetBeginMsg("Pairing Started ...")
					.SetFinishedMsg("Finished pairing")
					.SetArguments(new String[]{pairingName, accountID == null? "":Integer.toString(accountID), "blockchain", Integer.toString(networkType.getValue()) })
					.SetOperationAction(new OperationActions(){
						int timeout = 5;
						ServerSocket socket = null;
						@Override
						public void PreExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception { }

						@SuppressWarnings("static-access")
						@Override
						public void Execute(OnOperationUIUpdate listenerUI,
								ServerSocket ss, BANeworkInfo netInfo,
								String[] args, OnOperationUIUpdate listener)
								throws Exception {
							timeout = ss.getSoTimeout();
							 ss.setSoTimeout(0);
							 socket = ss;
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(wallet,ss,netInfo, args,listener,animation, animationAfterPairing); 
							 //Return to previous timeout
							 ss.setSoTimeout(timeout);
						}

						@Override
						public void PostExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) {
							try {socket.setSoTimeout(timeout); } catch(Exception e1) {}
							listenerUI.onError(e, null);
						}

						
						
					});
	}

	/**
	 * Responsible for the complete process of taking a raw transaction, singing it, make the authenticator sign it and then broadcast
	 * <br>
	 * Because the signing operations can, and will be, not immediate. The operation will prepare the payload for signing and then create
	 * a pending request + broadcast a GCM sign request to the Authenticator app.<br>
	 * When the Authenticator will ask for the payload, the {@link authenticator.network.TCPListener TCPListener} will reference the 
	 * generated payload for signing with the pending request ID. After matching the pending reuqest ID, the operation will continue and finalize.
	 * <br><br>
	 * <b>onlyComplete is True if should only complete the the deciphering of a received Authenticator signiture <b>
	 * 
	 * @param tx
	 * @param pairingID
	 * @param txMessage
	 * @param onlyComplete
	 * @return
	 */
	static public BAOperation SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION(WalletOperation wallet, Transaction tx, 
																String pairingID, 
																@Nullable String txLabel,
																@Nullable String to,
																boolean onlyComplete, 
																@Nullable byte[] authenticatorByteResponse,
																/**
																 * will be used to remove the pending request
																 * if necessary
																 */
																@Nullable PendingRequest pendigReq,
																@Nullable String WALLET_PW){
		BAOperation op = new BAOperation(ATOperationType.SignAndBroadcastAuthenticatorTx)
				.setOperationNetworkRequirements(BANetworkRequirement.PORT_MAPPING)
				.SetDescription("Sign Raw Transaction By Authenticator device")
				.SetOperationAction(new OperationActions(){
					//int timeout = 5;
					//
					
					@Override
					public void PreExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception {
					
					}

					@Override
					public void Execute(OnOperationUIUpdate listenerUI,
							ServerSocket ss, BANeworkInfo netInfo,
							String[] args, OnOperationUIUpdate listener) throws Exception {
						//
						if (!onlyComplete){
							byte[] cypherBytes = SignProtocol.prepareTX(wallet, WALLET_PW, tx,pairingID);
							String reqID = SignProtocol.sendGCM(wallet, 
									pairingID,
									txLabel,
									netInfo.EXTERNAL_IP,
									netInfo.INTERNAL_IP);
							PendingRequest pr = SignProtocol.generatePendingRequest(tx, 
									cypherBytes, 
									pairingID, 
									reqID,
									txLabel,
									to);
							
							wallet.addPendingRequest(pr);
						}
						else{
							//Decrypt the response
							SecretKey secretkey = new SecretKeySpec(EncodingUtils.hexStringToByteArray(wallet.getAESKey(pairingID)), "AES");
						    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						    cipher.init(Cipher.DECRYPT_MODE, secretkey);
						    String message = EncodingUtils.bytesToHex(cipher.doFinal(authenticatorByteResponse));
						    String sig = message.substring(0,message.length()-64);
						    String HMAC = message.substring(message.length()-64,message.length());
						    byte[] testsig = EncodingUtils.hexStringToByteArray(sig);
						    byte[] hash = EncodingUtils.hexStringToByteArray(HMAC);
						    //Calculate the HMAC of the message and verify it is valid
						    Mac mac = Mac.getInstance("HmacSHA256");
							mac.init(secretkey);
							byte[] macbytes = mac.doFinal(testsig);
							if (Arrays.equals(macbytes, hash)){
								staticLooger.info("Received Response: " + EncodingUtils.bytesToHex(testsig));
							}
							else {
								staticLooger.info("Message authentication code is invalid");
							}
							//Break apart the signature array sent over from the authenticator
							//String sigstr = BAUtils.bytesToHex(testsig);
							ArrayList<byte[]> AuthSigs = null;
							SignProtocol.AuthenticatorAnswerType answerType = SignProtocol.AuthenticatorAnswerType.DoNothing;
							try{
								/**
								 * try and parse an authorize answer
								 */
								AuthSigs = SignMessage.deserializeToBytes(testsig);
								answerType = SignProtocol.AuthenticatorAnswerType.Authorized;
							}
							catch (Exception e){
								/**
								 * try and parse a not authorized answer
								 */
								try{
									JSONObject obj = SignMessage.deserializeRefuseMessageToBoolean(testsig);
									int result = Integer.parseInt(obj.get("result").toString());
									if(result == 0){
										answerType = SignProtocol.AuthenticatorAnswerType.NotAuthorized;
										Authenticator.fireOnAuthenticatorSigningResponse(tx, 
												pairingID, 
												pendigReq, 
												SignProtocol.AuthenticatorAnswerType.NotAuthorized, 
												obj.get("reason").toString());
										wallet.removePendingRequest(pendigReq);
									}
								}
								catch (Exception e1){ }
							}
							if(answerType == SignProtocol.AuthenticatorAnswerType.Authorized){
								// Complete Signing and broadcast
								PairedAuthenticator po = wallet.getPairingObject(pairingID);
								SignProtocol.complete(wallet, 
										WALLET_PW,
										tx,
										AuthSigs,
										po);
								staticLooger.info("Signed Tx - " + EncodingUtils.getStringTransaction(tx));
								ConfigFile config = Authenticator.getWalletOperation().configFile;
								if (pendigReq.hasTxLabel() && pendigReq.hasTxDestinationDescription()){
									try {config.writeNextSavedTxData(tx.getHashAsString(), pendigReq.getTxDestinationDescription(), pendigReq.getTxLabel());}
									catch (IOException e) {e.printStackTrace();}
								}
								/**
								 * Condition sending by is Test Mode
								 */
								if(wallet.getApplicationParams().getIsTestMode() == false){
									SendResult result = wallet.pushTxWithWallet(tx);
									Futures.addCallback(result.broadcastComplete, new FutureCallback<Transaction>() {
						                @Override
						                public void onSuccess(Transaction result) {
						                	Authenticator.fireOnAuthenticatorSigningResponse(tx, 
													pairingID, 
													pendigReq, 
													SignProtocol.AuthenticatorAnswerType.Authorized, 
													null);
						                }

						                @Override
						                public void onFailure(Throwable t) {
						                	listenerUI.onError(null,t);
						                }
						            });
								}
								else{
									wallet.disconnectInputs(tx.getInputs());
									Authenticator.fireOnAuthenticatorSigningResponse(tx, 
											pairingID, 
											pendigReq, 
											SignProtocol.AuthenticatorAnswerType.Authorized, 
											null);
								}
								
								wallet.removePendingRequest(pendigReq);
							}
							else if(answerType == SignProtocol.AuthenticatorAnswerType.DoNothing){
								// well ... do nothing
							}

						}
						
						
					}

					@Override
					public void PostExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception { }

					@Override
					public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) { 
						try { wallet.removePendingRequest(pendigReq); } catch (IOException e1) { e1.printStackTrace(); }
						if(listenerUI != null)
							listenerUI.onError(e, null);
					}
					
				});
		
		return op;
	}
	
	/**
	 * If a pending request to the Authenticator app was made, change in the wallet IP will break any attempt to finalize the operation.<br>
	 * To resolve the problem, every time the wallet is launched, it will send a GCM notification to the Authenticator to:<br>
	 * <ol>
	 * <li> If there are any pending requests, remind the user</li>
	 * <li> Update the current wallet IPs</li>
	 * <ol>
	 * 
	 * 
	 * @param requestID
	 * @param pairingID
	 * @return
	 */
	static public BAOperation UPDATE_PAIRED_AUTHENTICATORS_IPS(WalletOperation wallet, String pairingID){
		return new BAOperation(ATOperationType.updateIpAddressesForPreviousMessage)
					.setOperationNetworkRequirements(BANetworkRequirement.PORT_MAPPING)
					.SetDescription("Update Authenticator's wallet IPs")
					.SetBeginMsg("Updating Authenticator's wallet IPs ...")
					.SetFinishedMsg("Finished IPs updates")
					.SetOperationAction(new OperationActions(){
						@Override
						public void PreExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception { }

						@Override
						public void Execute(OnOperationUIUpdate listenerUI,
								ServerSocket ss, BANeworkInfo netInfo,
								String[] args, OnOperationUIUpdate listener) throws Exception {
							PairedAuthenticator po = wallet.getPairingObject(pairingID);
							Dispacher disp;
							disp = new Dispacher(null,null);
							byte[] gcmID = po.getGCM().getBytes();
							assert(gcmID != null);
							Device d = new Device(po.getChainCode().getBytes(),
									po.getMasterPublicKey().getBytes(),
									gcmID,
									pairingID.getBytes(),
									null);
							
							// returns the request ID
							disp.dispachMessage(ATGCMMessageType.UpdatePendingRequestIPs, d, new String[]{ netInfo.EXTERNAL_IP, netInfo.INTERNAL_IP });
						}

						@Override
						public void PostExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception { }

						@Override
						public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) { 
							if(listenerUI != null)
								listenerUI.onError(e, null);
						}
						
					});
	}

	static public BAOperation BROADCAST_NORMAL_TRANSACTION(String txLabel, 
			String to, 
			WalletOperation wallet, 
			Transaction tx, Map<String,ATAddress> keys,
			@Nullable String WALLET_PW){
		return new BAOperation(ATOperationType.BroadcastNormalTx)
		.SetDescription("Send normal bitcoin Tx")
		.SetFinishedMsg("Tx Broadcast complete")
		.SetOperationAction(new OperationActions(){
			@Override
			public void PreExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception { 

			}

			@Override
			public void Execute(OnOperationUIUpdate listenerUI,
					ServerSocket ss, BANeworkInfo netInfo,
					String[] args, OnOperationUIUpdate listener)
					throws Exception {
				try{
					Transaction signedTx = wallet.signStandardTxWithAddresses(tx, keys, WALLET_PW);
					ConfigFile config = Authenticator.getWalletOperation().configFile;
					if (!txLabel.isEmpty()){
						try {config.writeNextSavedTxData(signedTx.getHashAsString(), to, txLabel);}
						catch (IOException e) {e.printStackTrace();}
					}
					if(signedTx == null){
						listenerUI.onError(new ScriptException("Failed to sign Tx"), null);
					}
					else{
						/**
						 * Condition sending by is Test Mode
						 */
						if(wallet.getApplicationParams().getIsTestMode() == false){
							SendResult result = wallet.pushTxWithWallet(signedTx);
							Futures.addCallback(result.broadcastComplete, new FutureCallback<Transaction>() {
				                @Override
				                public void onSuccess(Transaction result) {
				                	/**
				                	 * No need for UI notification here, will be handled by AuthenticatorGeneralEvents#OnBalanceChanged
				                	 */
				                }

				                @Override
				                public void onFailure(Throwable t) {
				                	listenerUI.onError(null,t);
				                }
				            });
						}
						else{
							wallet.disconnectInputs(tx.getInputs());
							/**
		                	 * No need for UI notification here, will be handled by AuthenticatorGeneralEvents#OnBalanceChanged
		                	 */
						}
						
					}
					
				}
				catch (Exception e){
					if(listenerUI != null)
						listenerUI.onError(e, null);
				}
			}

			@Override
			public void PostExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception { }

			@Override
			public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) { 
				if(listenerUI != null)
					listenerUI.onError(e, null);
			}
		});
	}
}
