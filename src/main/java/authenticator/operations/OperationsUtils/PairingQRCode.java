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
	
	public static final String QR_IMAGE_RELATIVE_PATH = "cached_resources/PairingQRCode.png"; 
	
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
  public PairingQRCode (BANetworkInfo ni, String wallettype, String key, int networkType, byte[] authWalletIndex) throws NotFoundException, WriterException, IOException{
	  this(ni.EXTERNAL_IP, ni.INTERNAL_IP, wallettype, key, networkType, authWalletIndex);
  }
  public PairingQRCode (String ip, String localip, String wallettype, String key, int networkType, byte[] authWalletIndex) throws WriterException, IOException,
      NotFoundException {
	  // Build the string to display in the QR.
	  String qrCodeData = generateQRDataString(ip,
											  localip,
											  wallettype,
											  key,
											  networkType,
											  authWalletIndex);
	  
	  String filePath = Authenticator.getApplicationParams().getApplicationDataFolderAbsolutePath() + QR_IMAGE_RELATIVE_PATH;
	  String charset = "UTF-8"; // or "ISO-8859-1"
	  Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
	  hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
	  //Create the QR code
	  createQRCode(qrCodeData, filePath, charset, hintMap, 350, 350);
	  System.out.println("QR Code image created successfully!");
	  System.out.println("Data read from QR Code: "
			  + readQRCode(filePath, charset, hintMap));
	  //Crop the QR Code so it looks better when displayed to the user.
	  Image orig = null ;
	  try {
		  orig = ImageIO.read(new File(filePath));
	  } catch (IOException e2) {
		  e2.printStackTrace();
	  }
	  int x = 20, y = 20, w = 310, h = 310;
	  BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	  bi.getGraphics().drawImage(orig, 0, 0, w, h, x, y, x + w, y + h, null);
	  //Save the QR to file
	  try {
		  ImageIO.write(bi, "png", new File(filePath));
	  } catch (IOException e1) {
		  e1.printStackTrace();
	  }
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
  	 * @param filePath
  	 * @param charset
  	 * @param hintMap
  	 * @param qrCodeheight
  	 * @param qrCodewidth
  	 * @throws WriterException
  	 * @throws IOException
  	 */
  	@SuppressWarnings({ "unchecked", "rawtypes" })
  	public  void createQRCode(String qrCodeData, String filePath, 
		  String charset, 
		  Map hintMap, 
		  int qrCodeheight, 
		  int qrCodewidth) throws WriterException, IOException 
  	{
  		BitMatrix matrix = new MultiFormatWriter().encode( new String(qrCodeData.getBytes(charset), charset),
  														BarcodeFormat.QR_CODE, qrCodewidth, qrCodeheight, hintMap);
  		MatrixToImageWriter.writeToFile(matrix, filePath.substring(filePath.lastIndexOf('.') + 1), new File(filePath));
   }

  	/**
  	 * 
  	 * @param filePath
  	 * @param charset
  	 * @param hintMap
  	 * @return
  	 * @throws FileNotFoundException
  	 * @throws IOException
  	 * @throws NotFoundException
  	 */
  	@SuppressWarnings({ "rawtypes", "unchecked" })
  	public String readQRCode(String filePath, String charset, Map hintMap) throws FileNotFoundException, 
  			IOException, NotFoundException {
  		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
        new BufferedImageLuminanceSource(ImageIO.read(new FileInputStream(filePath)))));
  		Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap,hintMap);
  		return qrCodeResult.getText();
  	}
  
}