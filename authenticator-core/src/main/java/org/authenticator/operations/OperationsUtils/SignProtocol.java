package org.authenticator.operations.operationsUtils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import org.authenticator.walletCore.exceptions.WrongWalletPasswordException;
import org.spongycastle.util.encoders.Hex;
import org.authenticator.GCM.dispacher.Device;
import org.authenticator.GCM.dispacher.Dispacher;
import org.authenticator.GCM.exceptions.GCMSendFailedException;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.Utils.EncodingUtils;
import org.authenticator.walletCore.exceptions.UnableToCompleteTransactionException;
import org.authenticator.walletCore.utils.BAPassword;
import org.authenticator.operations.operationsUtils.CommunicationObjects.SignMessage;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.authenticator.protobuf.ProtoConfig.ATOperationType;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.walletCore.WalletOperation;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

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
			@Nullable BAPassword WALLET_PW, 
			Transaction tx,  
			String pairingID) throws Exception {
		//Create the payload
		//PairedAuthenticator  pairingObj = org.authenticator.getWalletOperation().getPairingObject(pairingID);
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
		
		SecretKey secretkey = CryptoUtils.secretKeyFromHexString(wallet.getAESKey(pairingID, WALLET_PW));
		return CryptoUtils.encryptPayloadWithChecksum(jsonBytes, secretkey);
	}

	/**
	 * 
	 * @param wallet
	 * @param WALLET_PW
	 * @param tx
	 * @param AuthSigs
	 * @param po
	 * @throws org.authenticator.walletCore.exceptions.UnableToCompleteTransactionException
	 */
	@SuppressWarnings({ "static-access", "deprecation", "unused" })
	static public void complete(WalletOperation wallet, 
			@Nullable BAPassword WALLET_PW,
			Transaction tx, 
			ArrayList<byte[]> AuthSigs, 
			PairedAuthenticator po) throws UnableToCompleteTransactionException
	 {
		try{
			//Prep the keys needed for signing
			byte[] key = Hex.decode(po.getMasterPublicKey());
			byte[] chain = Hex.decode(po.getChainCode());
			
			//Loop to create a signature for each input
			int i = 0;							
			for(TransactionInput in: tx.getInputs())  {
				// We search the TransactionOutput because we build the Tx from raw hex data, not parent Tx is set.
				TransactionOutPoint txOp = in.getOutpoint();
				TransactionOutput out = wallet.findTransactionOutpointByHash(txOp.getHash().toString(), txOp.getIndex()); 
				if(out == null)
					throw new UnableToCompleteTransactionException("Cannot find corresponding transaction outout for input:\n " + in.toString());
				
				String inAddress = out.getScriptPubKey().getToAddress(wallet.getNetworkParams()).toString();
				ATAddress atAdd = wallet.findAddressInAccounts(inAddress);
				//Authenticator Key
				ECKey authKey = wallet.getPairedAuthenticatorKey(po, atAdd.getKeyIndex());
				
				ECKey walletKey = wallet.getPrivECKeyFromAccount(atAdd.getAccountIndex(),
																					atAdd.getType(),
																					atAdd.getKeyIndex(),
																					WALLET_PW,
																					true);
				
				// Create Program for the script
				List<ECKey> keys = ImmutableList.of(authKey, walletKey);
				Script scriptpubkey = ScriptBuilder.createMultiSigOutputScript(2,keys);
				
				//Create P2SH
				// IMPORTANT - AuthSigs and the signiture we create here should refer to the same input !!
				TransactionSignature sig1 = TransactionSignature.decodeFromBitcoin(AuthSigs.get(i), true);
				TransactionSignature sig2 = tx.calculateSignature(i, walletKey, scriptpubkey, Transaction.SigHash.ALL, false);
				List<TransactionSignature> sigs = ImmutableList.of(sig1, sig2);
				Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(sigs, scriptpubkey);

				in.setScriptSig(inputScript);
				
				i++;
			}
		}
		catch (Exception e){
			//wallet.disconnectInputs(tx.getInputs());
			e.printStackTrace();
			throw new UnableToCompleteTransactionException("Unable to finish transaction signing");
		}
	 }

	/**
	 * 
	 * 
	 * @param pairingID
	 * @param txMessage
	 * @return
	 * @throws GCMSendFailedException 
	 */
	static public String sendGCM(WalletOperation wallet,
			String pairingID,
			@Nullable String txMessage,
			String extIP,
			String intIP,
			@Nullable BAPassword WALLET_PW) throws GCMSendFailedException, WrongWalletPasswordException, CryptoUtils.CannotDecryptMessageException {
		Dispacher disp;
		disp = new Dispacher(null,null);
		//Send the encrypted payload over to the Authenticator and wait for the response.
		SecretKey secretkey = CryptoUtils.secretKeyFromHexString(wallet.getAESKey(pairingID, WALLET_PW));
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
