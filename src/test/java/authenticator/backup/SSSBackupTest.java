package authenticator.backup;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import authenticator.BipSSS.BipSSS;
import authenticator.BipSSS.SSSUtils;
import authenticator.BipSSS.BipSSS.EncodingFormat;
import authenticator.BipSSS.BipSSS.Share;

public class SSSBackupTest {

	@Test
	public void splitingTest() throws IOException {
		byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
		
		assertTrue(splitAndCheck(entropy, entropy, 6, 5));
		assertTrue(splitAndCheck(entropy, entropy, 6, 4));
		assertTrue(splitAndCheck(entropy, entropy, 6, 3));
		assertTrue(splitAndCheck(entropy, entropy, 6, 2));
		assertTrue(splitAndCheck(entropy, entropy, 6, 1));
	}
	
	@Test
	public void splitingInvalidTest() throws IOException, NoSuchAlgorithmException {
		byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
		
		//Generate 256 bit key.
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
	    kgen.init(256);
		// AES key
		SecretKey sharedsecret = kgen.generateKey();
		byte[] expectedWrong = sharedsecret.getEncoded();
		
		assertFalse(splitAndCheck(entropy, expectedWrong, 6, 5));
		assertFalse(splitAndCheck(entropy, expectedWrong, 6, 4));
		assertFalse(splitAndCheck(entropy, expectedWrong, 6, 3));
		assertFalse(splitAndCheck(entropy, expectedWrong, 6, 2));
		assertFalse(splitAndCheck(entropy, expectedWrong, 6, 1));
	}
	
	private boolean splitAndCheck(byte[] entropy, byte[] expected, int numberOfPieces, int threshold) throws IOException {
		List<Share> shares = splitToShares(entropy, numberOfPieces, threshold);
			
	    /* 
	     * test pieces
	     */
		
		// get all combinations
		SSSUtils ut = new SSSUtils();
		BipSSS sss = new BipSSS();
		List<Share[]> combinations = ut.getAllPossibleCombinations(shares, threshold);
		
		for(Share[] com:combinations){
    		boolean result = false;
    		try {
    			// res should be mnemonic entropy
				byte[] res = sss.combineSeed(Arrays.asList(com));
				result = Arrays.equals(expected, res);
				
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
    		
    		if(result == false)
    			return false;
    	}
		
		return true;
	}

	private List<Share> splitToShares(byte[] entropy, int numberOfPieces, int threshold) throws IOException {
		List<Share> shares;
		NetworkParameters params = MainNetParams.get(); 
		BipSSS sss = new BipSSS();
		shares = sss.shard(entropy, threshold, numberOfPieces, EncodingFormat.SHORT, params);
		
		return shares;
	}
	
	@Test
	public void testIllegalPiecesAndThresholds() {
		byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");

		List<Share> shares;
		NetworkParameters params = MainNetParams.get(); 
		BipSSS sss = new BipSSS();
		
		int threshold = 6;
		int numberOfPieces = 5;
		
		Exception caught = null;
		try {
			shares = sss.shard(entropy, threshold, numberOfPieces, EncodingFormat.SHORT, params);
		} catch (Exception t) {
		   caught = t;
		}
		assertNotNull(caught);
		assertSame(IllegalArgumentException.class, caught.getClass());
	}
}
