package authenticator.operations;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.mockito.stubbing.Answer;
import org.spongycastle.util.encoders.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicSeed;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import authenticator.Utils.CryptoUtils;
import authenticator.Utils.EncodingUtils;
import authenticator.hierarchy.BAHierarchy;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.PairingQRCode;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStage;

public class PairingQRTest {
	@SuppressWarnings("unchecked")
	@Test
	public void generateQRImageTest() {
		String walletType = "authenticator";
		String pairingName = "my pairing";
		String ip = "5.28.176.129:8222";
		String localip = "192.168.1.100:8222";
		String aes = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";		
		byte[] authWalletIndex = Hex.decode("35363265");
				
		PairingQRCode pQR = new PairingQRCode();
		
		String expected = pQR.generateQRDataString(ip, localip, pairingName, walletType, aes, 1, authWalletIndex);
		
		BufferedImage bi = null;
		byte[] imgBytes = null;
		String result;
		try {
			imgBytes = pQR.generateQRImageBytes(ip, localip, pairingName, walletType, aes, 1, authWalletIndex);			
		} catch (NotFoundException | WriterException | IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
		if(imgBytes == null)
			assertTrue(false);
		
		// read QR
		try {
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
		catch(IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void generateQRDataStringTest() {
		String walletType = "authenticator";
		String pairingName = "my pairing";
		String ip = "5.28.176.129:8222";
		String localip = "192.168.1.100:8222";
		String aes = "d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a";		
		byte[] authWalletIndex = Hex.decode("35363265");
				
		PairingQRCode pQR = new PairingQRCode();
		
		String result = pQR.generateQRDataString(ip, localip, pairingName, walletType, aes, 1, authWalletIndex);
		String expected =  "AESKey=d8d2b7a00a615ead144bcb02abe325fb955415484003eee969339d7d32f8ca3a&PublicIP=5.28.176.129:8222&LocalIP=192.168.1.100:8222&pairingName=my pairing&WalletType=authenticator&NetworkType=1&index=35363265";
		
		assertTrue(expected.equals(result));
	}
}
