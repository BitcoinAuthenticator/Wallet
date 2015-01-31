package org.authenticator.operations.operationsUtils;

import java.io.IOException;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.spongycastle.util.encoders.Hex;

import org.authenticator.network.BANetworkInfo;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;

/**
 * This class creates a new QR code containing the external IP, local IP, and type of wallet.
 * It crops the QR code and saves it to file. 
 */
public class PairingQRCode {


  public PairingQRCode(){}

/**
 *
 * @param ni
 * @param pairingName
 * @param wallettype
 * @param key
 * @param networkType- 1 for main net, 0 for testnet
 * @param authWalletIndex
 * @return
 * @throws NotFoundException
 * @throws WriterException
 * @throws IOException
 */
  public byte[] generateQRImageBytes (BANetworkInfo ni, String pairingName, String wallettype, byte[] key, int networkType, byte[] authWalletIndex) throws NotFoundException, WriterException, IOException{
	  return generateQRImageBytes(ni.EXTERNAL_IP, ni.INTERNAL_IP, pairingName, wallettype, key, networkType, authWalletIndex);
  }
  public byte[] generateQRImageBytes (String ip, String localip, String pairingName, String wallettype, byte[] key, int networkType, byte[] authWalletIndex) throws WriterException, IOException,
      NotFoundException {
	  // Build the string to display in the QR.
	  String qrCodeData = generateQRDataString(ip,
											  localip,
											  pairingName,
											  wallettype,
											  key,
											  networkType,
											  authWalletIndex);
	  
	  //Create the QR code
	  byte[] ret = createQRCode(qrCodeData, 350, 350);
	  System.out.println("QR Code image created successfully!");
	  return ret;
  }
  
  public String generateQRDataString(String ip, String localip, String pairingName, String wallettype, byte[] key, int networkType, byte[] authWalletIndex){
	  String qrCodeData = "AESKey=" + Hex.toHexString(key) +
			  "&PublicIP=" + ip + 
			  "&LocalIP=" + localip + 
			  "&pairingName=" + pairingName +
			  "&WalletType=" + wallettype +
			  "&NetworkType=" + networkType +
			  "&index=" + Hex.toHexString(authWalletIndex);
	  return qrCodeData;
  }

  	/**
  	 * 
  	 * @param qrCodeData
  	 * @param qrCodeheight
  	 * @param qrCodewidth
  	 */
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