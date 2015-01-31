package org.authenticator.operations;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.authenticator.GCM.dispacher.MessageBuilder;
import org.authenticator.operations.operationsUtils.SignProtocol;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.walletCore.WalletOperation;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import com.google.common.collect.ImmutableList;

public class SignTransactionTest {
	
	
	//Transaction tx;
	String pairingID = "1";
	String aes = "A2F72940109899C1708511B4867727E507299E276B2E44B5D48BBFE8689C17F0";
	String authenticatorSeed = "3701720EA7EB2CC7861DC0F0A6A8DB3AAF736C6139CC0F2BF72D410388D360C1E1FE0651B3DB554F51DC8C9093030A17216187E9FFC861A9E970A5EF485AC49F";
	PairedAuthenticator.Builder po;
	WalletOperation wallet;
	
	private Address getP2SHAddress(DeterministicKey authMKey, int childIndex){
		//network params
		NetworkParameters params = MainNetParams.get();
		
		// derive auth child key
		DeterministicKey childKey = HDKeyDerivation.deriveChildKey(authMKey, childIndex);
		ECKey outKey1 = new ECKey(childKey.getPrivKeyBytes(), childKey.getPubKey());
		
		ECKey outKey2 = new ECKey();
		//Create a 2-of-2 multisig output script.
		List<ECKey> keys = ImmutableList.of(outKey1,outKey2);//childPubKey, walletKey);
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		
		return multisigaddr;
	}
	
	private TransactionOutput addOutPut(Coin amount, DeterministicKey authMKey, int childIndex){
		Address outAddress = getP2SHAddress(authMKey, childIndex);
		TransactionOutput out = new TransactionOutput(MainNetParams.get(), 
				null, 
				amount, 
				outAddress);
		
		return out;
	}
	
	private TransactionInput addInput(Coin amount, String privateKeyStringHex, String addressString, int keyIndex) throws Exception{
		ECKey inKey =  ECKey.fromPrivate(Hex.decode(privateKeyStringHex));
		Address inAddress = inKey.toAddress(MainNetParams.get());
		assertTrue(inAddress.toString().equals(addressString));
		ATAddress.Builder bAddress = ATAddress.newBuilder();
						  bAddress.setAccountIndex(0);
						  bAddress.setAddressStr(inAddress.toString());
						  bAddress.setIsUsed(false);
						  bAddress.setKeyIndex(keyIndex);
						  bAddress.setType(HierarchyAddressTypes.External);
		TransactionOutput in = new TransactionOutput(MainNetParams.get(), 
																	null, 
																	amount, 
																	inAddress);
		TransactionInput inMock  = Mockito.mock(TransactionInput.class);
		Mockito.when(inMock.getConnectedOutput()).thenReturn(in);
		
		Mockito.when(wallet.findAddressInAccounts(inAddress.toString())).thenReturn(bAddress.build()); 
		Mockito.when(wallet.getPrivECKeyFromAccount(bAddress.getAccountIndex(),
			bAddress.getType(),
			bAddress.getKeyIndex(),
			null,
			true)).thenReturn(inKey);
		
		return inMock;
	}
	
	private Transaction preparePayToHashTransaction() throws Exception{
		Coin outAmpunt = Coin.valueOf(100000000 - 10000);
		Transaction tx = Mockito.mock(Transaction.class);
		
		// general 
		po = PairedAuthenticator.newBuilder();
		po.setAesKey(ByteString.copyFrom(Hex.decode(aes)));
		po.setGCM("some gcm");
		po.setPairingID(pairingID);
		po.setTestnet(false);
		
		pairingID = "1";
		wallet = Mockito.mock(WalletOperation.class);
		Mockito.when(wallet.getNetworkParams()).thenReturn(MainNetParams.get());
		Mockito.when(wallet.getAESKey(pairingID, null)).thenReturn(Hex.decode(aes));
		
		// inputs
		ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
		TransactionInput inMock = addInput(outAmpunt.add(Coin.valueOf(10000)), 
											"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85501", 
											"15SoHG382McMfyUT9We4mJou7eRGEVXQQj",
											1);
		inputs.add(inMock);
		Mockito.when(tx.getInputs()).thenReturn(inputs);
				
		// outputs
		ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
		DeterministicKey mPrivKey = HDKeyDerivation.createMasterPrivateKey(Hex.decode(authenticatorSeed));
		
		TransactionOutput out = addOutPut(outAmpunt, mPrivKey, 1);
		outputs.add(out);
		
		po.setMasterPublicKey(Hex.toHexString(mPrivKey.getPubKey()));
		po.setChainCode(Hex.toHexString(mPrivKey.getChainCode()));
		po.setKeysN(1);
		
		Mockito.when(tx.getOutputs()).thenReturn(outputs);
		
		return tx;
		
	}

	
	/**
	 * Comment out this method for changes in the test
	 * 
	 */
	/*private byte[] prepareTx() throws IOException, NoSuchAlgorithmException, InvalidKeyException{
		ArrayList<byte[]> pubKeysArr = new ArrayList<byte[]>(); pubKeysArr.add(inPubKey.getPubKey());
		ArrayList<Integer> indexArr = new ArrayList<Integer>(); indexArr.add(bAddress.build().getKeyIndex());
		
		SignMessage signMsgPayload = new SignMessage()
			.setInputNumber(tx.getInputs().size())
			.setTxString(BAUtils.getStringTransaction(tx))
			.setKeyIndexArray(pubKeysArr, indexArr)
			.setVersion(1)
			.setTestnet(false);
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
		 
		 String hexResult = BAUtils.bytesToHex(cipherBytes);
		 System.out.println("prepareTx() Test new result: " + hexResult);
		 
		 return cipherBytes;
	}*/
	
	@Test
	public void prepareTxTest() {
		byte[] result = null;
		try {
			Transaction tx = preparePayToHashTransaction();
			result = SignProtocol.prepareTX(wallet, null, tx, pairingID);
			byte[] expected = Hex.decode("93fbe01c0681d4e88405062fa3c8bec20414591e006336d8731e643cd055af750c3a0e478c5e8dc186d57f94fd310c454b40df676a4531e5ac1b6744be6da1142aeb042c048123bd8579570d587cbc955fed127a57addfb0ac60dcbebd8e9591c181666e19b86bfac2a3e6582addaf3a38bd32a839afca771beae5ae4883d8994c6927eecde5ec65d762f00da02c55cc0d3eb7ee2ece943f964995992dffe1da22c2ad180403f7aea3fb4df7686100799f160da3fc478b39c5d37be137e42559");
			// check expected output
			assertTrue(Arrays.areEqual(result, expected));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void payloadTest(){
		try{
			MessageBuilder b = new MessageBuilder(ATGCMMessageType.SignTX,
												new String[]{"pairing id",
												"external ip",
											    "local ip", 
											    "custom msg"});
			String result = b.toString();
			JSONObject objResult = new JSONObject(result);
			assertTrue(objResult.getString("WalletID").equals("pairing id"));
			
			assertTrue(objResult.getInt("RequestType") == ATGCMMessageType.SignTX_VALUE);
			JSONObject payload = new JSONObject(objResult.getString("ReqPayload"));
			assertTrue(payload.getString("ExternalIP").equals("external ip"));
			assertTrue(payload.getString("LocalIP").equals("local ip"));
			
			assertTrue(objResult.getString("CustomMsg").equals("custom msg"));
		}
		catch (JSONException e){
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
