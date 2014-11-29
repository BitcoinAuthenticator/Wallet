package authenticator;

import static org.junit.Assert.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import authenticator.Utils.CryptoUtils;
import authenticator.Utils.CryptoUtils.CannotDecryptMessageException;
import authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;

public class CryptoUtilsTest {

	@Test
	public void encryptPayloadWithChecksumTest() {
		String pl = "I am the payload";
		byte[] plBytes = pl.getBytes();
		
		String aes1 = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		SecretKey key1 = CryptoUtils.secretKeyFromHexString(aes1);
		assertTrue(key1 != null);
		
		String aes2 = "7d2db9d447f41cd16b936ac177f7eacbbad378f53b92dc6445924dd8c700e231";
		SecretKey key2 = CryptoUtils.secretKeyFromHexString(aes2);
		assertTrue(key2 != null);
		
		String expectedStr1 = "c558a0c22e542153111e81db37456900ee821764667de99ffa5217c016ddd43aa6e5836f457a9b788302fcc0cc11b943ae622310ebff850848a31ce1d3fd587d";
		byte[] expected1 = Hex.decode(expectedStr1);
		
		String expectedStr2 = "2d8cb51f2363873502804cbdccb9da6a69f58ad7ee3c8adffc6ca5045066905d60fa859e919b92d55d6b7c1ba7b01bb9caa6d8503f8219d9f6eaba8e0f3dffe6";
		byte[] expected2 = Hex.decode(expectedStr2);
		
		byte[] result1 = null;
		try {
			result1 = CryptoUtils.encryptPayloadWithChecksum(plBytes, key1);
		} catch (CouldNotEncryptPayload e) { }
		
		byte[] result2 = null;
		try {
			result2 = CryptoUtils.encryptPayloadWithChecksum(plBytes, key2);
		} catch (CouldNotEncryptPayload e) { }
		
		assertTrue(Arrays.areEqual(result1, expected1));
		assertTrue(Arrays.areEqual(result2, expected2));
		
		assertFalse(Arrays.areEqual(result1, expected2));
		assertFalse(Arrays.areEqual(result2, expected1));
	}
	
	@Test
	public void payloadWithChecksumTest() {
		String pl = "I am the payload";
		byte[] plBytes = pl.getBytes();
		
		String aes = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		SecretKey key = new SecretKeySpec(Hex.decode(aes), "AES");
		assertTrue(key != null);
		
		String expectedStr = "4920616d20746865207061796c6f61642c1624c2fdb59ff7b8bff02e84a11fb9ee82037383b2ca942185d5665d769a1e";
		byte[] expected = Hex.decode(expectedStr);
		
		byte[] result = CryptoUtils.payloadWithChecksum(plBytes, key);
		assertTrue(result != null);
				
		assertTrue(Arrays.areEqual(result, expected));
	}

	
	@Test
	public void decryptPayloadWithChecksumTest() {
		String plStr1 = "c558a0c22e542153111e81db37456900ee821764667de99ffa5217c016ddd43aa6e5836f457a9b788302fcc0cc11b943ae622310ebff850848a31ce1d3fd587d";
		byte[] plBytes1 = Hex.decode(plStr1);
		
		String plStr2 = "2d8cb51f2363873502804cbdccb9da6a69f58ad7ee3c8adffc6ca5045066905d60fa859e919b92d55d6b7c1ba7b01bb9caa6d8503f8219d9f6eaba8e0f3dffe6";
		byte[] plBytes2 = Hex.decode(plStr2);
		
		byte[] expectedBytes = "I am the payload".getBytes();
		
		String aes1 = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		SecretKey key1 = CryptoUtils.secretKeyFromHexString(aes1);
		assertTrue(key1 != null);
		
		String aes2 = "7d2db9d447f41cd16b936ac177f7eacbbad378f53b92dc6445924dd8c700e231";
		SecretKey key2 = CryptoUtils.secretKeyFromHexString(aes2);
		assertTrue(key2 != null);		
		
		byte[] result1 = null;
		try {
			result1 = CryptoUtils.decryptPayloadWithChecksum(plBytes1, key1);
		} catch (CannotDecryptMessageException e) { }
		
		byte[] result2 = null;
		try {
			result2 = CryptoUtils.decryptPayloadWithChecksum(plBytes2, key2);
		} catch (CannotDecryptMessageException e) { }
		
		assertTrue(Arrays.areEqual(result1, expectedBytes));
		assertTrue(Arrays.areEqual(result2, expectedBytes));
		
		assertFalse(Arrays.areEqual(result1, "I am not the payload".getBytes()));
		assertFalse(Arrays.areEqual(result2, "I am not the payload".getBytes()));
	}
	
	@Test
	public void secretKeyFromHexStringTest() {
		String expected = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		
		SecretKey key = CryptoUtils.secretKeyFromHexString(expected);
		byte[] raw = key.getEncoded();
		String result = Hex.toHexString(raw);
		
		assertTrue(expected.equals(result));
	}
}
