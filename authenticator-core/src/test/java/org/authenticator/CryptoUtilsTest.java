package org.authenticator;

import static org.junit.Assert.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.authenticator.Utils.CryptoUtils;
import org.authenticator.Utils.CryptoUtils.CannotDecryptMessageException;
import org.authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.util.Random;

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
	public void encryptPayloadTest() {
		String pl = "I am the payload";
		byte[] plBytes = pl.getBytes();

		String aes1 = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		SecretKey key1 = CryptoUtils.secretKeyFromHexString(aes1);
		assertTrue(key1 != null);

		String aes2 = "7d2db9d447f41cd16b936ac177f7eacbbad378f53b92dc6445924dd8c700e231";
		SecretKey key2 = CryptoUtils.secretKeyFromHexString(aes2);
		assertTrue(key2 != null);

		String expectedStr1 = "c558a0c22e542153111e81db37456900ae622310ebff850848a31ce1d3fd587d";
		byte[] expected1 = Hex.decode(expectedStr1);

		String expectedStr2 = "2d8cb51f2363873502804cbdccb9da6acaa6d8503f8219d9f6eaba8e0f3dffe6";
		byte[] expected2 = Hex.decode(expectedStr2);

		byte[] result1 = null;
		try {
			result1 = CryptoUtils.encrypt(key1, plBytes);
		} catch (CouldNotEncryptPayload e) { }

		byte[] result2 = null;
		try {
			result2 = CryptoUtils.encrypt(key2, plBytes);
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
	public void decryptPayload() {
		String plStr1 = "c558a0c22e542153111e81db37456900ae622310ebff850848a31ce1d3fd587d";
		byte[] plBytes1 = Hex.decode(plStr1);

		String plStr2 = "2d8cb51f2363873502804cbdccb9da6acaa6d8503f8219d9f6eaba8e0f3dffe6";
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
			result1 = CryptoUtils.decryptPayload(plBytes1, key1);
		} catch (CannotDecryptMessageException e) { }

		byte[] result2 = null;
		try {
			result2 = CryptoUtils.decryptPayload(plBytes2, key2);
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

	@Test
	public void digestSHA256Test() {
		String expected = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";
		String input 	= "hello world";
		String result 	= Hex.toHexString(CryptoUtils.digestSHA256(input));
		assertTrue(result.equals(expected));

		expected 	= "ed12932f3ef94c0792fbc55263968006e867e522cf9faa88274340a2671d4441";
		input 		= "hello world 2";
		result 		= Hex.toHexString(CryptoUtils.digestSHA256(input));
		assertTrue(result.equals(expected));

		expected 	= "4ffabbab4e763202462df1f59811944121588f0567f55bce581a0e99ebcf6606";
		input 		= "hello world 3";
		result 		= Hex.toHexString(CryptoUtils.digestSHA256(input));
		assertTrue(result.equals(expected));
	}

	@Test
	public void authenticatorAESEncryptionTest() {
		String seedHex 	  = "55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d";
		String walletIdex = "1";
		String salt 	  = "d9d2d4fa6780f5b36c3b740a52d0547c6d195dea";
		String AESHex 	  = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		String expected = "a108b45bf3429d139980480739026a34f680856897abcc36603c03bf806c352e190ee11cf1b7a10e3848bd4ab45608f0";
		String result 	= "";
		try {
			result = Hex.toHexString(CryptoUtils.authenticatorAESEncryption(AESHex, salt, walletIdex, seedHex));
			assertTrue(expected.equals(result));
		} catch (CouldNotEncryptPayload couldNotEncryptPayload) {
			couldNotEncryptPayload.printStackTrace();
			assertTrue(false);
		}


		seedHex 	  	= "55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d";
		walletIdex 		= "120";
		salt 	  		= "2b94f795c44a74825dbb3b6c7c564f00cdd87fa4";
		AESHex 	  		= "7d2db9d447f41cd16b936ac177f7eacbbad378f53b92dc6445924dd8c700e231";
		expected 		= "1de8139f7b2dd5ee6435a03416b54856e717220ab83a8a233e7b0ba871dd7e713391fee399afdcc2db52215de31b7662";
		result 			= "";
		try {
			result = Hex.toHexString(CryptoUtils.authenticatorAESEncryption(AESHex, salt, walletIdex, seedHex));
			assertTrue(expected.equals(result));
		} catch (CouldNotEncryptPayload couldNotEncryptPayload) {
			couldNotEncryptPayload.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void authenticatorAESDecryptionTest() {
		String seedHex 	  			= "55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d";
		String walletIdex 			= "1";
		String salt 	  			= "d9d2d4fa6780f5b36c3b740a52d0547c6d195dea";
		String AESEncryptedHex 	  	= "a108b45bf3429d139980480739026a34f680856897abcc36603c03bf806c352e190ee11cf1b7a10e3848bd4ab45608f0";
		String expected 			= "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";
		String result 				= "";
		try {
			result = Hex.toHexString(CryptoUtils.authenticatorAESDecryption(AESEncryptedHex, salt, walletIdex, seedHex));
			assertTrue(expected.equals(result));
		} catch (CannotDecryptMessageException e) {
			e.printStackTrace();
			assertTrue(false);
		}


		seedHex 	  			= "55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d";
		walletIdex 				= "120";
		salt 	  				= "2b94f795c44a74825dbb3b6c7c564f00cdd87fa4";
		AESEncryptedHex 	  	= "1de8139f7b2dd5ee6435a03416b54856e717220ab83a8a233e7b0ba871dd7e713391fee399afdcc2db52215de31b7662";
		expected 				= "7d2db9d447f41cd16b936ac177f7eacbbad378f53b92dc6445924dd8c700e231";
		result 					= "";
		try {
			result = Hex.toHexString(CryptoUtils.authenticatorAESDecryption(AESEncryptedHex, salt, walletIdex, seedHex));
			assertTrue(expected.equals(result));
		} catch (CannotDecryptMessageException e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
}
