package org.authenticator.backup;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;

import com.google.common.io.Resources;
import org.authenticator.BipSSS.BipSSS;
import org.authenticator.BipSSS.BipSSS.EncodingFormat;
import org.authenticator.BipSSS.BipSSS.Share;
import org.authenticator.operations.operationsUtils.PaperSSSQR;
import org.authenticator.operations.operationsUtils.PaperWalletQR;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.DeterministicSeed;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import com.google.common.base.Joiner;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class QRBackupTest {

	/*
	 * Seed QR 
	 */
	
	@Test
	public void qrSeedDataStringTest() {
		try {

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
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
	
	@Test
	public void seedQRImageDataTest() {
		try {
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
			BufferedImage bi = null;
			byte[] imgBytes = qr.createQRSeedImageBytes(seed, creationTime, 170, 170);
			InputStream in = new ByteArrayInputStream(imgBytes);
			bi = ImageIO.read(in);
			Result qrResult = null;
			LuminanceSource source = new BufferedImageLuminanceSource(bi);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			QRCodeReader reader = new QRCodeReader();
			try {
				qrResult = reader.decode(bitmap);
			} catch (ReaderException e) {
				assertTrue(false);
			}

			if (qrResult == null) assertTrue(false);
			result = qrResult.getText();

			assertTrue(expected.equals(result));

		/*
		 * just checks the qr generation, the data itself should be tested elsewhere
		 */
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
	
	@Test
	public void seedQRImageDataParsingTest() {
		try {
		/*
		 * Valid
		 */

			byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
			MnemonicCode ms = new MnemonicCode();
			List<String> mnemonic = ms.toMnemonic(entropy);

			long creationTime = 1413794360;
			DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);

			//get data from qr
			PaperWalletQR qr = new PaperWalletQR();
			BufferedImage bi = null;
			byte[] imgBytes = qr.createQRSeedImageBytes(seed, creationTime, 170, 170);
			InputStream in = new ByteArrayInputStream(imgBytes);
			bi = ImageIO.read(in);
			Result qrResult = null;
			LuminanceSource source = new BufferedImageLuminanceSource(bi);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			QRCodeReader reader = new QRCodeReader();
			try {
				qrResult = reader.decode(bitmap);
			} catch (ReaderException e) {
				assertTrue(false);
			}

			if (qrResult == null) assertTrue(false);

			PaperWalletQR.SeedQRData data = qr.parseSeedQR(qrResult.getText());

		/*
		 * mnemonic
		 */
			String mnemonicResultString = Joiner.on(" ").join(data.seed.getMnemonicCode());
			String expected = Joiner.on(" ").join(mnemonic);
			assertTrue(mnemonicResultString.equals(expected));

		/*
		 * creation time
		 */
			assertTrue(creationTime == data.creationTime);

		/*
		 * just checks the qr generation, the data itself should be tested elsewhere
		 */
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}

	/*
	 * Shamir Secret Sharing QR 
	 */
	
	@Test
	public void sssQRDataStringTest() {
		try {
			/*
			 * Check validity
			 */
			byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
			MnemonicCode ms = new MnemonicCode();
			List<String> mnemonic = ms.toMnemonic(entropy);

			long creationTime = 1413794360;
			DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);

			BipSSS sss = new BipSSS();
			List<Share> shares = sss.shard(ms.toEntropy(mnemonic), 4, 6, EncodingFormat.SHORT, MainNetParams.get());

			String expected;
			String result;

			for(Share s: shares) {
				expected = "Piece=" + s.toString() + "&Time=1413794360";
				PaperSSSQR qr = new PaperSSSQR();
				result = qr.generateQRDataString(s, creationTime);
				assertTrue(expected.equals(result));
			}


		/*
		 * Check invalidity, creation time changed
		 */
			long creationTimeWrong = 1413794361;
			shares = sss.shard(ms.toEntropy(mnemonic), 4, 6, EncodingFormat.SHORT, MainNetParams.get());

			for(Share s: shares) {
				expected = "Piece=" + s.toString() + "&Time=1413794360";
				PaperSSSQR qr = new PaperSSSQR();
				result = qr.generateQRDataString(s, creationTimeWrong);
				assertFalse(expected.equals(result));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void sssQRImageDataTest() {
		try {
		/*
		 * Valid
		 */

			byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
			MnemonicCode ms = new MnemonicCode();
			List<String> mnemonic = ms.toMnemonic(entropy);

			long creationTime = 1413794360;
			DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);

			BipSSS sss = new BipSSS();
			List<Share> shares = sss.shard(ms.toEntropy(mnemonic), 4, 6, EncodingFormat.SHORT, MainNetParams.get());

			String[] expectedArr = {"Piece=SS9ChG74DYAaMD7HR79EUzoM9GDp5MirRTQRF9irbUMwtYFuauVJ4&Time=1413794360",
					"Piece=SS9Dg8wajSdF7DFwzQipg4bCnGoQwZq8fvzxNfPqBMdNu9TNCeo6f&Time=1413794360",
					"Piece=SS9EexyvDgbUhWNLrjnCRkt13nmTGaMNTABVNCF1WbMeqNJSgXUGz&Time=1413794360",
					"Piece=SS9Gwk6PetEKHbjnC1whiXb9K1xMBn6Su1UWvqgddhwr3tEt7z5sV&Time=1413794360",
					"Piece=SS9K5h7hyDNM5AxsaaJKGofXMAYW8WmxJLYAN1Ek2eDrj7iB83YdG&Time=1413794360",
					"Piece=SS9NGBhUZXcWKwmmeXHZF6f9FyVxhu4Vr1WSFCr7t13S5H2y85uCH&Time=1413794360"};

			for(Share s: shares) {
				String expected = expectedArr[s.shareNumber - 1];
				String result;

				//get data from qr
				PaperSSSQR qr = new PaperSSSQR();
				BufferedImage bi = null;
				byte[] imgBytes = qr.createQRSSSImageBytes(s, creationTime, 170, 170);
				InputStream in = new ByteArrayInputStream(imgBytes);
				bi = ImageIO.read(in);
				Result qrResult = null;
				LuminanceSource source = new BufferedImageLuminanceSource(bi);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				QRCodeReader reader = new QRCodeReader();
				try {
					qrResult = reader.decode(bitmap);
				} catch (ReaderException e) {
					assertTrue(false);
				}

				if (qrResult == null) assertTrue(false);
				result = qrResult.getText();

				assertTrue(expected.equals(result));
			}

		/*
		 * just checks the qr generation, the data itself should be tested elsewhere
		 */
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
	
	@Test
	public void sssQRImageDataParsingTest() {
		try {
			/*
		 	* Valid
			*/

			byte[] entropy = Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
			MnemonicCode ms = new MnemonicCode();
			List<String> mnemonic = ms.toMnemonic(entropy);

			long creationTime = 1413794360;
			DeterministicSeed seed = new DeterministicSeed(mnemonic, null,"", creationTime);

			BipSSS sss = new BipSSS();
			List<Share> shares = sss.shard(ms.toEntropy(mnemonic), 4, 6, EncodingFormat.SHORT, MainNetParams.get());

			String[] expectedArr = {"SS9ChG74DYAaMD7HR79EUzoM9GDp5MirRTQRF9irbUMwtYFuauVJ4",
					"SS9Dg8wajSdF7DFwzQipg4bCnGoQwZq8fvzxNfPqBMdNu9TNCeo6f",
					"SS9EexyvDgbUhWNLrjnCRkt13nmTGaMNTABVNCF1WbMeqNJSgXUGz",
					"SS9Gwk6PetEKHbjnC1whiXb9K1xMBn6Su1UWvqgddhwr3tEt7z5sV",
					"SS9K5h7hyDNM5AxsaaJKGofXMAYW8WmxJLYAN1Ek2eDrj7iB83YdG",
					"SS9NGBhUZXcWKwmmeXHZF6f9FyVxhu4Vr1WSFCr7t13S5H2y85uCH"};

			for(Share s: shares) {
				//get data from qr
				PaperSSSQR qr = new PaperSSSQR();
				BufferedImage bi = null;
				byte[] imgBytes = qr.createQRSSSImageBytes(s, creationTime, 170, 170);
				InputStream in = new ByteArrayInputStream(imgBytes);
				bi = ImageIO.read(in);
				Result qrResult = null;
				LuminanceSource source = new BufferedImageLuminanceSource(bi);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				QRCodeReader reader = new QRCodeReader();
				try {
					qrResult = reader.decode(bitmap);
				} catch (ReaderException e) {
					assertTrue(false);
				}

				if (qrResult == null) assertTrue(false);

				PaperSSSQR.SSSQRData data = qr.parseSSSQR(qrResult.getText(), MainNetParams.get());

			/*
			 * mnemonic
			 */
				String pieceResultString = data.share.toString();
				String expected = expectedArr[s.shareNumber - 1];
				assertTrue(pieceResultString.equals(expected));

			/*
			 * creation time
			 */
				assertTrue(creationTime == data.creationTime);
			}

		/*
		 * just checks the qr generation, the data itself should be tested elsewhere
		 */
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
}
