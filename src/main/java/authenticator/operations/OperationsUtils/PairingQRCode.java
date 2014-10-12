package authenticator.operations.OperationsUtils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.bitcoinj.core.AddressFormatException;

import authenticator.Authenticator;
import authenticator.Utils.EncodingUtils;
import authenticator.network.BANetworkInfo;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * This class creates a new QR code containing the external IP, local IP, and type of wallet.
 * It crops the QR code and saves it to file. 
 */
public class PairingQRCode {
		
/**
 * 
 * 
 * 
 * @param ip
 * @param localip
 * @param wallettype
 * @param key
 * @param networkType - 1 for main net, 0 for testnet
 * @throws WriterException
 * @throws IOException
 * @throws NotFoundException
 */
  public PairingQRCode(){}
  public byte[] generateQRImageBytes (BANetworkInfo ni, String wallettype, String key, int networkType, byte[] authWalletIndex) throws NotFoundException, WriterException, IOException{
	  return generateQRImageBytes(ni.EXTERNAL_IP, ni.INTERNAL_IP, wallettype, key, networkType, authWalletIndex);
  }
  public byte[] generateQRImageBytes (String ip, String localip, String wallettype, String key, int networkType, byte[] authWalletIndex) throws WriterException, IOException,
      NotFoundException {
	  // Build the string to display in the QR.
	  String qrCodeData = generateQRDataString(ip,
											  localip,
											  wallettype,
											  key,
											  networkType,
											  authWalletIndex);
	  
	  //Create the QR code
	  byte[] ret = createQRCode(qrCodeData, 350, 350);
	  System.out.println("QR Code image created successfully!");
	  return ret;
  }
  
  public String generateQRDataString(String ip, String localip, String wallettype, String key, int networkType, byte[] authWalletIndex){
	  String qrCodeData = "AESKey=" + key + 
			  "&PublicIP=" + ip + 
			  "&LocalIP=" + localip + 
			  "&WalletType=" + wallettype +
			  "&NetworkType=" + networkType +
			  "&index=" + EncodingUtils.bytesToHex(authWalletIndex);
	  return qrCodeData;
  }

  	/**
  	 * 
  	 * @param qrCodeData
  	 * @param qrCodeheight
  	 * @param qrCodewidth
  	 */
  	@SuppressWarnings({ "unchecked", "rawtypes" })
  	public byte[] createQRCode(String qrCodeData, int qrCodeheight,  int qrCodewidth) {
  		byte[] imageBytes = QRCode
		        .from(qrCodeData)
		        .withSize(qrCodewidth, qrCodeheight)
		        .to(ImageType.PNG)
		        .stream()
		        .toByteArray();
  		return imageBytes;
   }
}