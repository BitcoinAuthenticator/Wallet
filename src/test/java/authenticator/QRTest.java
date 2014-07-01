package authenticator;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Test;

import com.google.zxing.EncodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import authenticator.Utils.BAUtils;
import authenticator.operations.OperationsUtils.QRCode;

public class QRTest {

	@Test
	public void test() throws NoSuchAlgorithmException, NotFoundException, WriterException, IOException {
		String ip = "127.0.0.1:80";
		String localIP = "127.0.0.1:80";
		String wallettype = "blockchain";
		int networkType = 1;
		//Generate 256 bit key.
		 KeyGenerator kgen = KeyGenerator.getInstance("AES");
	     kgen.init(256);
		// AES key
		SecretKey sharedsecret = kgen.generateKey();
		byte[] raw = sharedsecret.getEncoded();
		String key = BAUtils.bytesToHex(raw);
	      
		QRCode qr = new QRCode();
		String qrData = qr.generateQRDataString(ip, localIP, wallettype, key, networkType);
		// hint map
		Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
	    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
	    //
		qr.createQRCode(qrData, 
				new java.io.File( "." ).getCanonicalPath() + "/PairingQRCodeTEST.png", 
				"UTF-8", 
				hintMap,
				350, 350);
		
		String readQRDataFromImage = qr.readQRCode(new java.io.File( "." ).getCanonicalPath() + "/PairingQRCodeTEST.png",
				"UTF-8",
				hintMap);
		
		assertTrue(qrData.equals(readQRDataFromImage));
		
		// Dispose of file 
		File file = new File(new java.io.File( "." ).getCanonicalPath() + "/PairingQRCodeTEST.png");
		assertTrue(file.delete());
	}

}
