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
import authenticator.WalletOperation;
import authenticator.GCM.dispacher.Device;
import authenticator.GCM.dispacher.Dispacher;
import authenticator.Utils.BAUtils;
import authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import authenticator.protobuf.ProtoConfig.ATOperationType;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;

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
	 * 
	 * @param tx
	 * @param pairingID
	 * @return
	 * @throws Exception
	 */
	static public byte[] prepareTX(WalletOperation wallet, Transaction tx,  String pairingID) throws Exception {
		//Create the payload
		//PairedAuthenticator  pairingObj = Authenticator.getWalletOperation().getPairingObject(pairingID);
		String formatedTx = BAUtils.getStringTransaction(tx);
		System.out.println("Raw unSigned Tx - " + formatedTx);
		//Get pub keys and indexes
		ArrayList<byte[]> pubKeysArr = new ArrayList<byte[]>();
		ArrayList<Integer> indexArr = new ArrayList<Integer>();
		for(TransactionInput in:tx.getInputs()){
			String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(wallet.getNetworkParams()).toString();
			ATAddress atAdd = wallet.findAddressInAccounts(inAddress);
			ECKey pubkey = wallet.getECKeyFromAccount(atAdd.getAccountIndex(),
																	atAdd.getType(),
																	atAdd.getKeyIndex());
			
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
		SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(wallet.getAESKey(pairingID)), "AES");
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
	 * 
	 * @param tx
	 * @param AuthSigs
	 * @param po
	 * @throws Exception
	 */
	@SuppressWarnings({ "static-access", "deprecation", "unused" })
	static public void complete(WalletOperation wallet, Transaction tx, ArrayList<byte[]> AuthSigs, PairedAuthenticator po) throws Exception
	 {
			//Prep the keys needed for signing
			byte[] key = BAUtils.hexStringToByteArray(po.getMasterPublicKey());
			byte[] chain = BAUtils.hexStringToByteArray(po.getChainCode());
			
			// we rebuild the Tx from a raw string so we need to reconnect the inputs
			wallet.connectInputs(tx.getInputs());
			
			//Loop to create a signature for each input
			int i = 0;							
			for(TransactionInput in: tx.getInputs()){
				String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(wallet.getNetworkParams()).toString();
				ATAddress atAdd = wallet.findAddressInAccounts(inAddress);
				//Authenticator Key
				HDKeyDerivation HDKey = null;
				DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
				int indexInAuth = atAdd.getKeyIndex(); // the same ass the address index in the wallet
				DeterministicKey childKey = HDKey.deriveChildKey(mPubKey,indexInAuth);
				byte[] childpublickey = childKey.getPubKey();
				ECKey authKey = new ECKey(null, childpublickey);
				
				//Wallet key
				ECKey walletKey = wallet.getECKeyFromAccount(atAdd.getAccountIndex(),
																					atAdd.getType(),
																					atAdd.getKeyIndex());
				
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

	/**
	 * 
	 * 
	 * @param pairingID
	 * @param txMessage
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	static public String sendGCM(WalletOperation wallet, String pairingID, @Nullable String txMessage) throws JSONException, IOException{
		Dispacher disp;
		disp = new Dispacher(null,null);
		//Send the encrypted payload over to the Authenticator and wait for the response.
		SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(wallet.getAESKey(pairingID)), "AES");						
		PairedAuthenticator  po = wallet.getPairingObject(pairingID);
		byte[] gcmID = po.getGCM().getBytes();
		assert(gcmID != null);
		Device d = new Device(po.getChainCode().getBytes(),
				po.getMasterPublicKey().getBytes(),
				gcmID,
				pairingID.getBytes(),
				secretkey);
		
		// returns the request ID
		return disp.dispachMessage(new Authenticator(),ATGCMMessageType.SignTX, d, new String[]{ txMessage });
	 }
	
	static public PendingRequest generatePendingRequest(Transaction tx, byte[] cypherBytes, String pairingID, String reqID){
		PendingRequest.Builder pr = PendingRequest.newBuilder();
		   pr.setPairingID(pairingID);
		   pr.setRequestID(reqID);
		   pr.setOperationType(ATOperationType.SignAndBroadcastAuthenticatorTx);
		   pr.setPayloadToSendInCaseOfConnection(ByteString.copyFrom(cypherBytes));
		   pr.setRawTx(BAUtils.getStringTransaction(tx));
		   PendingRequest.Contract.Builder cb = PendingRequest.Contract.newBuilder();
					cb.setShouldSendPayloadOnConnection(true);
					cb.setShouldReceivePayloadAfterSendingPayloadOnConnection(true);
		   pr.setContract(cb.build());
		   
		   return pr.build();
	}
}
