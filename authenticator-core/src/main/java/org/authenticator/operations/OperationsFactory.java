package org.authenticator.operations;

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
import org.spongycastle.util.encoders.Hex;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.TransactionInput.ConnectMode;
import org.bitcoinj.core.TransactionInput.ConnectionResult;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet.SendResult;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ByteString;

import org.authenticator.Authenticator;
import org.authenticator.BAApplicationParameters.NetworkType;
import org.authenticator.BASE;
import org.authenticator.GCM.dispacher.Device;
import org.authenticator.GCM.dispacher.Dispacher;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.Utils.EncodingUtils;
import org.authenticator.db.walletDB;
import org.authenticator.network.BANetworkInfo;
import org.authenticator.operations.BAOperation.BANetworkRequirement;
import org.authenticator.operations.BAOperation.BAOperationActions;
import org.authenticator.operations.OperationsUtils.PairingProtocol;
import org.authenticator.operations.OperationsUtils.PairingProtocol.PairingStageUpdater;
import org.authenticator.operations.OperationsUtils.SignProtocol;
import org.authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;
import org.authenticator.operations.listeners.OperationListener;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.ATOperationType;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.exceptions.CannotRemovePendingRequestException;
import org.authenticator.walletCore.utils.BAPassword;

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
	 * @param accountID
	 * @param secretKey
	 * @param networkType
	 * @param timeout
	 * @param isRepairingAccount
	 * @param statusListener
	 * @param walletPW
	 * @return
	 */
	static public BAOperation PAIRING_OPERATION(WalletOperation wallet,
			String pairingName, 
			@Nullable Integer accountID,
			@Nullable String secretKey,
			NetworkType networkType, 
			int timeout,
			boolean isRepairingAccount,
			@Nullable PairingStageUpdater statusListener,
			@Nullable BAPassword walletPW){
		BAOperation op = new BAOperation(ATOperationType.Pairing);
					op.setOperationNetworkRequirements(BANetworkRequirement.SOCKET)	
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetBeginMsg("Pairing Started ...")
					.SetFinishedMsg("Finished pairing")
					.SetArguments(new String[]{ pairingName, 
												accountID == null? "":Integer.toString(accountID), 
												"authenticator", 
												Integer.toString(networkType.getValue()),
												secretKey == null? "":secretKey})
					.SetOperationAction(new BAOperationActions(){
						int tempTimeout = 5;
						ServerSocket socket = null;
						@Override
						public void PreExecution(OperationListener listenerUI, String[] args)  throws Exception { }

						@SuppressWarnings("static-access")
						@Override
						public void Execute(OperationListener listenerUI,
								ServerSocket ss, BANetworkInfo netInfo,
								String[] args, OperationListener listener)
								throws Exception {
							tempTimeout = ss.getSoTimeout();
							 ss.setSoTimeout(0);
							 socket = ss;
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(wallet,
									 ss,
									 timeout,
									 netInfo, 
									 args,
									 listener,
									 statusListener,
									 isRepairingAccount,
									 walletPW); 
							 //Return to previous timeout
							 if(!ss.isClosed())
								 ss.setSoTimeout(tempTimeout);
						}

						@Override
						public void PostExecution(OperationListener listenerUI, String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void OnExecutionError(OperationListener listenerUI, Exception e) {
							try {socket.setSoTimeout(timeout); } catch(Exception e1) {}
							if(listenerUI != null)
								listenerUI.onError(op, e, null);
						}

					});
		return op;
	}

	/**
	 * Responsible for the complete process of taking a raw transaction, singing it, make the authenticator sign it and then broadcast
	 * <br>
	 * Because the signing operations can, and will be, not immediate. The operation will prepare the payload for signing and then create
	 * a pending request + broadcast a GCM sign request to the Authenticator app.<br>
	 * When the Authenticator will ask for the payload, the {@link org.authenticator.network.TCPListener TCPListener} will reference the 
	 * generated payload for signing with the pending request ID. After matching the pending reuqest ID, the operation will continue and finalize.
	 * <br><br>
	 * <b>onlyComplete is True if should only complete the the deciphering of a received Authenticator signiture <b>
	 * 
	 * @param wallet
	 * @param tx
	 * @param pairingID
	 * @param txLabel
	 * @param to
	 * @param onlyComplete
	 * @param authenticatorByteResponse
	 * @param pendigReq
	 * @param WALLET_PW
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
																@Nullable BAPassword WALLET_PW){
		BAOperation op = new BAOperation(ATOperationType.SignAndBroadcastAuthenticatorTx);
				op.setOperationNetworkRequirements(BANetworkRequirement.SOCKET)
				.SetDescription("Sign Raw Transaction By Authenticator device")
				.SetOperationAction(new BAOperationActions(){
					//int timeout = 5;
					//
					
					@Override
					public void PreExecution(OperationListener listenerUI, String[] args) throws Exception {
					
					}

					@Override
					public void Execute(OperationListener listenerUI,
							ServerSocket ss, BANetworkInfo netInfo,
							String[] args, OperationListener listener) throws Exception {
						//
						if (!onlyComplete){
							byte[] cypherBytes = SignProtocol.prepareTX(wallet, 
									WALLET_PW, tx,
									pairingID);
							String reqID = SignProtocol.sendGCM(wallet, 
									pairingID,
									txLabel,
									netInfo.EXTERNAL_IP,
									netInfo.INTERNAL_IP,
									WALLET_PW);
							
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
							SecretKey secretkey = CryptoUtils.secretKeyFromHexString(wallet.getAESKey(pairingID, WALLET_PW));
							byte[] testsig = CryptoUtils.decryptPayloadWithChecksum(authenticatorByteResponse, secretkey);
							
							//Break apart the signature array sent over from the authenticator
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
								if (pendigReq.hasTxLabel() && pendigReq.hasTxDestinationDescription()){
									wallet.writeNextSavedTxData(tx.getHashAsString(), pendigReq.getTxDestinationDescription(), pendigReq.getTxLabel());
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
						                	listenerUI.onError(op, null,t);
						                }
						            });
								}
								else{
									//wallet.disconnectInputs(tx.getInputs());
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
					public void PostExecution(OperationListener listenerUI, String[] args) throws Exception { }

					@Override
					public void OnExecutionError(OperationListener listenerUI, Exception e) { 
						try { wallet.removePendingRequest(pendigReq); } catch (CannotRemovePendingRequestException e1) { e1.printStackTrace(); }
						if(listenerUI != null)
							listenerUI.onError(op, e, null);
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
		BAOperation op = new BAOperation(ATOperationType.updateIpAddressesForPreviousMessage);
					op.setOperationNetworkRequirements(BANetworkRequirement.NONE)
					.SetDescription("Update Authenticator's wallet IPs")
					.SetBeginMsg("Updating Authenticator's wallet IPs ...")
					.SetFinishedMsg("Finished IPs updates")
					.SetOperationAction(new BAOperationActions(){
						@Override
						public void PreExecution(OperationListener listenerUI, String[] args)  throws Exception { }

						@Override
						public void Execute(OperationListener listenerUI,
								ServerSocket ss, BANetworkInfo netInfo,
								String[] args, OperationListener listener) throws Exception {
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
						public void PostExecution(OperationListener listenerUI, String[] args)  throws Exception { }

						@Override
						public void OnExecutionError(OperationListener listenerUI, Exception e) { 
							if(listenerUI != null)
								listenerUI.onError(op, e, null);
						}
						
					});
			return op;
	}

	/**
	 * 
	 * @param txLabel
	 * @param to
	 * @param wallet
	 * @param tx
	 * @param keys
	 * @param WALLET_PW
	 * @return
	 */
	static public BAOperation BROADCAST_NORMAL_TRANSACTION(String txLabel, 
			String to, 
			WalletOperation wallet, 
			Transaction tx, Map<String,ATAddress> keys,
			@Nullable BAPassword WALLET_PW){
		BAOperation op = new BAOperation(ATOperationType.BroadcastNormalTx);
				op.SetDescription("Send normal bitcoin Tx")
				.SetFinishedMsg("Tx Broadcast complete")
				.SetOperationAction(new BAOperationActions(){
					@Override
					public void PreExecution(OperationListener listenerUI, String[] args) throws Exception { 
		
					}
		
					@Override
					public void Execute(OperationListener listenerUI,
							ServerSocket ss, BANetworkInfo netInfo,
							String[] args, OperationListener listener)
							throws Exception {
						try{
							Transaction signedTx = wallet.signStandardTxWithAddresses(tx, keys, WALLET_PW);
							if (!txLabel.isEmpty()){
								wallet.writeNextSavedTxData(signedTx.getHashAsString(), to, txLabel);
							}
							if(signedTx == null){
								listenerUI.onError(op, new ScriptException("Failed to sign Tx"), null);
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
						                	listenerUI.onError(op, null,t);
						                }
						            });
								}
								else{
									//wallet.disconnectInputs(tx.getInputs());
									/**
				                	 * No need for UI notification here, will be handled by AuthenticatorGeneralEvents#OnBalanceChanged
				                	 */
								}
								
							}
							
						}
						catch (Exception e){
							if(listenerUI != null)
								listenerUI.onError(op, e, null);
						}
					}
		
					@Override
					public void PostExecution(OperationListener listenerUI, String[] args) throws Exception { }
		
					@Override
					public void OnExecutionError(OperationListener listenerUI, Exception e) { 
						if(listenerUI != null)
							listenerUI.onError(op, e, null);
					}
				});
		return op;
	}
}
