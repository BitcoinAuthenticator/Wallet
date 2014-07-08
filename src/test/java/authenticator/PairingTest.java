package authenticator;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.mockito.stubbing.Answer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.wallet.DeterministicSeed;

import authenticator.Utils.BAUtils;
import authenticator.hierarchy.BAHierarchy;
import authenticator.operations.OperationsUtils.PairingProtocol;

public class PairingTest {
	@SuppressWarnings("unchecked")
	@Test
	public void test() throws NoSuchAlgorithmException, InvalidKeyException, IOException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		
		/**
		 * 
		 * Prepare payload
		 * 
		 */
		
		//create HD key
		//Generate a new Seed
		SecureRandom secureRandom = null;
		try {secureRandom = SecureRandom.getInstance("SHA1PRNG");} 
		catch (NoSuchAlgorithmException e) {e.printStackTrace();}
		byte[] bytes = new byte[16];
		secureRandom.nextBytes(bytes);
		DeterministicSeed Dseed = new DeterministicSeed(bytes, Utils.currentTimeSeconds());
		byte[] seed = Dseed.getSecretBytes();
		HDKeyDerivation HDKey = null;
		DeterministicKey m = HDKey.createMasterPrivateKey(seed);
		DeterministicKey pub = m.derive(0);
		
		// AES Key
		//Generate 256 bit key.
		 KeyGenerator kgen = KeyGenerator.getInstance("AES");
	     kgen.init(256);
		// AES key
		SecretKey sharedsecret = kgen.generateKey();
		byte[] raw = sharedsecret.getEncoded();
		String key = BAUtils.bytesToHex(raw);
		//
		
		JSONObject payloadObj = new JSONObject();
				   payloadObj.put("mpubkey", BAUtils.bytesToHex(pub.getPubKey()));
				   payloadObj.put("chaincode", BAUtils.bytesToHex(pub.getChainCode()));
				   MessageDigest md = MessageDigest.getInstance("SHA-1");
				   payloadObj.put("pairID",  BAUtils.bytesToHex(md.digest(("Some crazy thing").getBytes())));
				   payloadObj.put("gcmID", "some registration id");
				   
		String payloadUnencryptedStr = payloadObj.toString();   
		byte[] payloadUnencryptedBytes = payloadUnencryptedStr.getBytes();
		
		// get HMAC 
		//Calculate the HMAC
    	Mac mac = Mac.getInstance("HmacSHA256");
    	mac.init(sharedsecret);
    	byte[] macbytes = mac.doFinal(payloadUnencryptedBytes);
    	
    	//Concatenate with the JSON object
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
    	outputStream.write(payloadUnencryptedBytes);
    	outputStream.write(macbytes);
    	byte payloadWithHMAC[] = outputStream.toByteArray();
    	
    	// Encrypt
    	Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    	cipher.init(Cipher.ENCRYPT_MODE, sharedsecret);
    	byte[] payloadEncrypted = cipher.doFinal(payloadWithHMAC);
    	
    	/**
    	 * 
    	 * 
    	 * 
    	 */
    	PairingProtocol pp = new PairingProtocol();    	
    	String decipheredDataFromAuth = pp.decipherDataFromAuthenticator(payloadEncrypted, null, sharedsecret);
    	JSONObject jsonPayloadFromAuth = pp.parseAndVerifyPayload(decipheredDataFromAuth, sharedsecret, null);
    	// check 
    	String mPubKeyRec = (String) jsonPayloadFromAuth.get("mpubkey");
		String chaincodeRec = (String) jsonPayloadFromAuth.get("chaincode");
	    String pairingIDRec = (String) jsonPayloadFromAuth.get("pairID");
	    String GCMRec = (String) jsonPayloadFromAuth.get("gcmID");
	    assert(mPubKeyRec.equals(BAUtils.bytesToHex(pub.getPubKey())));
	    assert(chaincodeRec.equals(BAUtils.bytesToHex(pub.getChainCode())));
	    assert(pairingIDRec.equals(BAUtils.bytesToHex(md.digest(("Some crazy thing").getBytes()))));
	    assert(GCMRec.equals("some registration id"));
	}

}
