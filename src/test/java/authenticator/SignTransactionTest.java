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

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;

public class SignTransactionTest {
	
	Transaction tx;
	String pairingID = "1";
	//SecretKey aes;
	String aes = "A2F72940109899C1708511B4867727E507299E276B2E44B5D48BBFE8689C17F0";
	ECKey inPubKey;
	Address inAddress;
	ATAddress.Builder bAddress;
	WalletOperation wallet;
	
	private Address getP2SHAddress(){
		//network params
		NetworkParameters params = MainNetParams.get();
		ECKey k1 = new ECKey();
		ECKey k2 = new ECKey();
		//Create a 2-of-2 multisig output script.
		List<ECKey> keys = ImmutableList.of(k1,k2);//childPubKey, walletKey);
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		
		return multisigaddr;
	}

	/*private SecretKey getAESKey() throws NoSuchAlgorithmException{
		//Generate 256 bit key.
		 KeyGenerator kgen = KeyGenerator.getInstance("AES");
	     kgen.init(256);	   
	     SecretKey sharedsecret = kgen.generateKey();
	     System.out.println(BAUtils.bytesToHex(sharedsecret.getEncoded()));
	     return sharedsecret;
	}*/
	
	private Transaction preparePayToHashTransaction() throws Exception{
		Coin outAmpunt = Coin.valueOf(100000000 - 10000);
		tx = Mockito.mock(Transaction.class);
		
		// wallet 
		pairingID = "1";
		wallet = Mockito.mock(WalletOperation.class);
		Mockito.when(wallet.getNetworkParams()).thenReturn(MainNetParams.get());
		
		// inputs
		inPubKey =  ECKey.fromPrivate("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855".getBytes());
		inAddress = inPubKey.toAddress(MainNetParams.get());
		assert(inAddress.toString().equals("1F3sAm6ZtwLAUnj7d38pGFxtP3RVEvtsbV"));
		bAddress = ATAddress.newBuilder();
			  bAddress.setAccountIndex(1);
			  bAddress.setAddressStr(inAddress.toString());
			  bAddress.setIsUsed(false);
			  bAddress.setKeyIndex(1);
			  bAddress.setType(HierarchyAddressTypes.External);
		TransactionOutput out = new TransactionOutput(MainNetParams.get(), 
					null, 
					outAmpunt.add(Coin.valueOf(10000)), 
					inAddress);
		TransactionInput inMock  = Mockito.mock(TransactionInput.class);
		Mockito.when(inMock.getConnectedOutput()).thenReturn(out);
		ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
		inputs.add(inMock);
		Mockito.when(tx.getInputs()).thenReturn(inputs);
		Mockito.when(wallet.findAddressInAccounts(inAddress.toString())).thenReturn(bAddress.build()); 
		Mockito.when(wallet.getECKeyFromAccount(bAddress.getAccountIndex(),
			bAddress.getType(),
			bAddress.getKeyIndex())).thenReturn(inPubKey);
		Mockito.when(wallet.getAESKey(pairingID)).thenReturn(aes);
		
		return tx;
		
	}
	
	@Test
	public void prepareTxTest() {
		byte[] result = null;
		try {
			tx = preparePayToHashTransaction();
			result = SignProtocol.prepareTX(wallet, tx, pairingID);
			byte[] expected = BAUtils.hexStringToByteArray("93FBE01C0681D4E88405062FA3C8BEC20414591E006336D8731E643CD055AF750C3A0E478C5E8DC186D57F94FD310C454B40DF676A4531E5AC1B6744BE6DA1142AEB042C048123BD8579570D587CBC9574132594D3E335972E66E6039A60109DE96BC3ADBCA3C2DCF5CE174E5DE87885B674FEE05B5CEF2DC5B93861BE94983AB20FE2FF8AEDE8744CE56F7027237246977725E8538E36EF2239D35DE219FA76588CD3A237E7E5D61DC636E46BB6052FE91CC14CA22D348EEBFB3187B9DAF8AE");//prepareTx();
			assertTrue(Arrays.areEqual(result, expected));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
