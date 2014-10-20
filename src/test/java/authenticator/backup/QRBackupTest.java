package authenticator.backup;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import com.google.common.base.Joiner;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import authenticator.operations.OperationsUtils.PaperWalletQR;

public class QRBackupTest {

	@Test
	public void qrSeedDataStringTest() throws NoSuchAlgorithmException, IOException, MnemonicLengthException {
		/*
		 * Check validity
		 */
		byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
		MnemonicCode ms = new MnemonicCode();
		List<String> mnemonic = ms.toMnemonic(entropy);
		
		long creationTime = 1413794360;
		DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);
		
		String expected = "Seed=55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d&Time=1413794360";
		String result;
		
		PaperWalletQR qr = new PaperWalletQR();
		result = qr.generateQRSeedDataString(seed, creationTime);

		assertTrue(expected.equals(result));
		
		/*
		 * Check invalidity, creation time changed
		 */
		long creationTimeWrong = 1413794361;
		result = qr.generateQRSeedDataString(seed, creationTimeWrong);

		assertFalse(expected.equals(result));
		
		/*
		 * Check invalidity, entropy changed
		 */
		//Generate 256 bit key.
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
	    kgen.init(256);
		// AES key
		SecretKey sharedsecret = kgen.generateKey();
		byte[] entropyWrong = sharedsecret.getEncoded();
		List<String> mnemonicWrong = ms.toMnemonic(entropyWrong);
		DeterministicSeed seedWrong = new DeterministicSeed(mnemonicWrong, null,"", creationTime);
		
		result = qr.generateQRSeedDataString(seedWrong, creationTime);
		assertFalse(expected.equals(result));
	}

	
	@Test
	public void qrImageDataTest() throws IOException, MnemonicLengthException {
		/*
		 * Valid 
		 */
		
		byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
		MnemonicCode ms = new MnemonicCode();
		List<String> mnemonic = ms.toMnemonic(entropy);
		
		long creationTime = 1413794360;
		DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);
		
		String expected = "Seed=55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d&Time=1413794360";
		String result;
		
		//get data from qr
		PaperWalletQR qr = new PaperWalletQR();
		BufferedImage bi = qr.generatePaperWallet(Arrays.toString(mnemonic.toArray()), seed, creationTime);
		Result qrResult = null;
		LuminanceSource source = new BufferedImageLuminanceSource(bi);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			qrResult = new MultiFormatReader().decode(bitmap);
		} catch (NotFoundException e) {
			assertTrue(false);
		}
		if (qrResult == null) assertTrue(false);
		result = qrResult.getText();
		
		assertFalse(expected.equals(result));
		
		/*
		 * just checks the qr generation, the data itself should be tested elsewhere 
		 */
	}
	
	@Test
	public void qrImageDataParsingTest() throws IOException, MnemonicLengthException {
		/*
		 * Valid 
		 */
		
		byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
		MnemonicCode ms = new MnemonicCode();
		List<String> mnemonic = ms.toMnemonic(entropy);
		
		long creationTime = 1413794360;
		DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);
		
		String expected = "Seed=55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d&Time=1413794360";
		String result;
		
		//get data from qr
		PaperWalletQR qr = new PaperWalletQR();
		BufferedImage bi = qr.generatePaperWallet(Arrays.toString(mnemonic.toArray()), seed, creationTime);
		Result qrResult = null;
		LuminanceSource source = new BufferedImageLuminanceSource(bi);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			qrResult = new MultiFormatReader().decode(bitmap);
		} catch (NotFoundException e) {
			assertTrue(false);
		}
		if (qrResult == null) assertTrue(false);
		
		PaperWalletQR.SeedQRData data = qr.parseSeedQR(qrResult.getText());
		
		/*
		 * mnemonic
		 */
		String mnemonicResultString = Joiner.on(" ").join(data.seed.getMnemonicCode());
		assertTrue(mnemonicResultString.equals(Arrays.toString(mnemonic.toArray())));
		
		/*
		 * creation time
		 */
		assertTrue(creationTime == data.creationTime);
		
		/*
		 * just checks the qr generation, the data itself should be tested elsewhere 
		 */
	}
}
