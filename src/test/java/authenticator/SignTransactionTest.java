package authenticator;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.junit.Test;
import org.mockito.Mockito;
import org.spongycastle.util.Arrays;

import authenticator.Utils.BAUtils;
import authenticator.operations.OperationsUtils.SignProtocol;
import authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.crypto.MnemonicException.MnemonicLengthException;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;

public class SignTransactionTest {
	
	
	//Transaction tx;
	String pairingID = "1";
	String aes = "A2F72940109899C1708511B4867727E507299E276B2E44B5D48BBFE8689C17F0";
	String authenticatorSeed = "3701720EA7EB2CC7861DC0F0A6A8DB3AAF736C6139CC0F2BF72D410388D360C1E1FE0651B3DB554F51DC8C9093030A17216187E9FFC861A9E970A5EF485AC49F";
	PairedAuthenticator.Builder po;
	WalletOperation wallet;
	
	// outputs
	/*ECKey outKey1;
	ECKey outKey2;
	Address outAddress;*/
	
	// inputs 
	/*ECKey inKey;
	Address inAddress;
	ATAddress.Builder bAddress;*/
	
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
		ECKey inKey =  ECKey.fromPrivate(BAUtils.hexStringToByteArray(privateKeyStringHex));
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
		Mockito.when(wallet.getECKeyFromAccount(bAddress.getAccountIndex(),
			bAddress.getType(),
			bAddress.getKeyIndex())).thenReturn(inKey);
		
		return inMock;
	}
	
	private Transaction preparePayToHashTransaction() throws Exception{
		Coin outAmpunt = Coin.valueOf(100000000 - 10000);
		Transaction tx = Mockito.mock(Transaction.class);
		
		// general 
		po = PairedAuthenticator.newBuilder();
		po.setAesKey(aes);
		po.setGCM("some gcm");
		po.setPairingID(pairingID);
		po.setTestnet(false);
		
		pairingID = "1";
		wallet = Mockito.mock(WalletOperation.class);
		Mockito.when(wallet.getNetworkParams()).thenReturn(MainNetParams.get());
		Mockito.when(wallet.getAESKey(pairingID)).thenReturn(aes);
		
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
		DeterministicKey mPrivKey = HDKeyDerivation.createMasterPrivateKey(BAUtils.hexStringToByteArray(authenticatorSeed));
		
		TransactionOutput out = addOutPut(outAmpunt, mPrivKey, 1);
		outputs.add(out);
		
		po.setMasterPublicKey(BAUtils.bytesToHex(mPrivKey.getPubKey()));
		po.setChainCode(BAUtils.bytesToHex(mPrivKey.getChainCode()));
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
	
	/*@Test
	public void prepareTxTest() {
		byte[] result = null;
		try {
			Transaction tx = preparePayToHashTransaction();
			result = SignProtocol.prepareTX(wallet, tx, pairingID);
			byte[] expected = BAUtils.hexStringToByteArray("93FBE01C0681D4E88405062FA3C8BEC20414591E006336D8731E643CD055AF750C3A0E478C5E8DC186D57F94FD310C454B40DF676A4531E5AC1B6744BE6DA1142AEB042C048123BD8579570D587CBC95EB2221BAA25FCE2359F61C73F2F2981C791C09D7A3636E67AAC922E4DF7870D32CCBD8AF4552838D24227243C60D2D358FA0768ECE3C025B81AA273ADB98CF92832FCA49E28AFFF7F22542565F5AA409EB785800C022059DA4E02BB66D923FF07E29B44B75E27860F798723760DC2D3E");//prepareTx();
			// check expected output
			assertTrue(Arrays.areEqual(result, expected));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}*/

}
