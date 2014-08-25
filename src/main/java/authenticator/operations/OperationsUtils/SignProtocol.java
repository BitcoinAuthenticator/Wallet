package authenticator.operations.OperationsUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;

import authenticator.Authenticator;
import authenticator.GCM.dispacher.Device;
import authenticator.GCM.dispacher.Dispacher;
import authenticator.Utils.EncodingUtils;
import authenticator.walletCore.exceptions.UnableToCompleteTxSigningException;
import authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import authenticator.protobuf.ProtoConfig.ATOperationType;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.walletCore.WalletOperation;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

public class SignProtocol {
	
	/**
	 * 
	 * @param wallet
	 * @param WALLET_PW
	 * @param tx
	 * @param pairingID
	 * @return
	 * @throws Exception
	 */
	static public byte[] prepareTX(WalletOperation wallet, 
			@Nullable String WALLET_PW, 
			Transaction tx,  
			String pairingID) throws Exception {
		//Create the payload
		//PairedAuthenticator  pairingObj = Authenticator.getWalletOperation().getPairingObject(pairingID);
		String formatedTx = EncodingUtils.getStringTransaction(tx);
		System.out.println("Raw unSigned Tx - " + formatedTx);
		//Get pub keys and indexes
		ArrayList<byte[]> pubKeysArr = new ArrayList<byte[]>();
		ArrayList<Integer> indexArr = new ArrayList<Integer>();
		for(TransactionInput in:tx.getInputs()){
			String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(wallet.getNetworkParams()).toString();
			ATAddress atAdd = wallet.findAddressInAccounts(inAddress);
			ECKey pubkey = wallet.getPrivECKeyFromAccount(atAdd.getAccountIndex(),
																	atAdd.getType(),
																	atAdd.getKeyIndex(),
																	WALLET_PW,
																	true);
			
			pubKeysArr.add(pubkey.getPubKey());
			indexArr.add(atAdd.getKeyIndex());
		}
		
		SignMessage signMsgPayload = new SignMessage()
						.setInputNumber(tx.getInputs().size())
						.setTxString(formatedTx)
						.setKeyIndexArray(pubKeysArr, indexArr)
						.setVersion(1)
						.setTestnet(false);
						;
		byte[] jsonBytes = signMsgPayload.serializeToBytes();
		
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKey secretkey = new SecretKeySpec(EncodingUtils.hexStringToByteArray(wallet.getAESKey(pairingID)), "AES");
		mac.init(secretkey);
		byte[] macbytes = mac.doFinal(jsonBytes);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		outputStream.write(jsonBytes);
		outputStream.write(macbytes);
		byte payload[] = outputStream.toByteArray( );
		
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
	}

	/**
	 * 
	 * @param wallet
	 * @param WALLET_PW
	 * @param tx
	 * @param AuthSigs
	 * @param po
	 * @throws UnableToCompleteTxSigningException
	 */
	@SuppressWarnings({ "static-access", "deprecation", "unused" })
	static public void complete(WalletOperation wallet, 
			@Nullable String WALLET_PW,
			Transaction tx, 
			ArrayList<byte[]> AuthSigs, 
			PairedAuthenticator po) throws UnableToCompleteTxSigningException
	 {
		try{
			//Prep the keys needed for signing
			byte[] key = EncodingUtils.hexStringToByteArray(po.getMasterPublicKey());
			byte[] chain = EncodingUtils.hexStringToByteArray(po.getChainCode());
			
			// we rebuild the Tx from a raw string so we need to reconnect the inputs
			wallet.connectInputs(tx.getInputs());
			
			//Loop to create a signature for each input
			int i = 0;							
			for(TransactionInput in: tx.getInputs()){
				String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(wallet.getNetworkParams()).toString();
				ATAddress atAdd = wallet.findAddressInAccounts(inAddress);
				//Authenticator Key
				ECKey authKey = wallet.getPairedAuthenticatorKey(po, atAdd.getKeyIndex());
				
				//Wallet key
				if (Authenticator.getWalletOperation().isWalletEncrypted()){
					//Display dialog asking user to input password
				}
				ECKey walletKey = wallet.getPrivECKeyFromAccount(atAdd.getAccountIndex(),
																					atAdd.getType(),
																					atAdd.getKeyIndex(),
																					WALLET_PW,
																					true);
				
				// Create Program for the script
				List<ECKey> keys = ImmutableList.of(authKey, walletKey);
				Script scriptpubkey = ScriptBuilder.createMultiSigOutputScript(2,keys);
				//byte[] program = scriptpubkey.getProgram();
				
				//Create P2SH
				// IMPORTANT - AuthSigs and the signiture we create here should refer to the same input !!
				TransactionSignature sig1 = TransactionSignature.decodeFromBitcoin(AuthSigs.get(i), true);
				TransactionSignature sig2 = tx.calculateSignature(i, walletKey, scriptpubkey, Transaction.SigHash.ALL, false);
				List<TransactionSignature> sigs = ImmutableList.of(sig1, sig2);
				Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(sigs, scriptpubkey);
				
				//TransactionInput input = inputs.get(i);
				//input.setScriptSig(inputScript);
				in.setScriptSig(inputScript);
				
				//check signature
				/*try{
					in.getScriptSig().correctlySpends(tx, i, scriptpubkey, true);
				} catch (ScriptException e) {
					// disconnect input to not get the wallet to crash on startup
					// Caused by bitcoinj WalletProtobufSerializer.java:585
					// Exception: UnreadableWalletException
					Authenticator.getWalletOperation().disconnectInputs(tx.getInputs());
		            throw e;
		        }*/
			
				//break;
				i++;
			}
		}
		catch (Exception e){
			wallet.disconnectInputs(tx.getInputs());
			e.printStackTrace();
			throw new UnableToCompleteTxSigningException("Unable to finish transaction signing");
		}
	 }

	/**
	 * 
	 * 
	 * @param pairingID
	 * @param txMessage
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	static public String sendGCM(WalletOperation wallet,
			String pairingID,
			@Nullable String txMessage,
			String extIP,
			String intIP) throws JSONException, IOException{
		Dispacher disp;
		disp = new Dispacher(null,null);
		//Send the encrypted payload over to the Authenticator and wait for the response.
		SecretKey secretkey = new SecretKeySpec(EncodingUtils.hexStringToByteArray(wallet.getAESKey(pairingID)), "AES");						
		PairedAuthenticator  po = wallet.getPairingObject(pairingID);
		byte[] gcmID = po.getGCM().getBytes();
		assert(gcmID != null);
		Device d = new Device(po.getChainCode().getBytes(),
				po.getMasterPublicKey().getBytes(),
				gcmID,
				pairingID.getBytes(),
				secretkey);
		
		// returns the request ID
		return disp.dispachMessage(ATGCMMessageType.SignTX, d, new String[]{ txMessage, extIP, intIP });
	 }
	
	static public PendingRequest generatePendingRequest(Transaction tx, 
			byte[] cypherBytes, 
			String pairingID,
			String reqID, 
			@Nullable String txLabel,
			@Nullable String destinationDescription){
		PendingRequest.Builder pr = PendingRequest.newBuilder();
		   pr.setPairingID(pairingID);
		   pr.setRequestID(reqID);
		   pr.setOperationType(ATOperationType.SignAndBroadcastAuthenticatorTx);
		   pr.setPayloadToSendInCaseOfConnection(ByteString.copyFrom(cypherBytes));
		   pr.setRawTx(EncodingUtils.getStringTransaction(tx));
		   pr.setTxLabel(txLabel);
		   pr.setTxDestinationDescription(destinationDescription);
		   PendingRequest.Contract.Builder cb = PendingRequest.Contract.newBuilder();
					cb.setShouldSendPayloadOnConnection(true);
					cb.setShouldReceivePayloadAfterSendingPayloadOnConnection(true);
					cb.setShouldLetPendingRequestHandleRemoval(true);
		   pr.setContract(cb.build());
		   
		   return pr.build();
	}
	
	
	public enum AuthenticatorAnswerType{
		Authorized,
		NotAuthorized,
		/**
		 * Possible when the user only watches the transaction but doesn't approve or disapproves
		 */
		DoNothing
	}
}
