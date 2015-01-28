package org.authenticator.operations.operationsUtils;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.authenticator.BASE;
import org.authenticator.BipSSS.BipSSS.Share;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;

import static com.google.common.base.Preconditions.checkNotNull;

public class PaperSSSQR extends BASE{
	public PaperSSSQR(){
		super(PaperSSSQR.class);
	}

	public BufferedImage generatePaperWalletFromTemplate(Share share, long creationTime) throws IOException {
		URL url = getClass().getResource("/org/authenticator/backup/PaperWalletSSS.png");
		return generatePaperWallet(url, share, creationTime);
	}

	public BufferedImage generatePaperWallet(URL paperWalletTemplateURL, Share share, long creationTime) throws IOException {
		Image qrSSS = createQRSSSImage(share, creationTime);
		return completePaperWallet(paperWalletTemplateURL, share.toString(), qrSSS);
	}
	
	@SuppressWarnings("restriction")
	public Image createQRSSSImage(Share share, long creationTime){
		byte[] imageBytes = createQRSSSImageBytes(share, creationTime, 170, 170);
        Image qrSeed = new Image(new ByteArrayInputStream(imageBytes), 153, 153, true, false);
        return qrSeed;
	}
	
	public byte[] createQRSSSImageBytes(Share share, long creationTime, int w, int h){
		byte[] imageBytes = null;
		imageBytes = QRCode
				        .from(generateQRDataString(share, creationTime))
				        .withSize(w, h)
				        .to(ImageType.PNG)
				        .stream()
				        .toByteArray();
        return imageBytes;
	}
	
	public String generateQRDataString(Share share, long creationTime)
	{
		return "Piece=" + share.toString() + 
	  			"&Time=" + creationTime;
	}
	
	public SSSQRData parseSSSQR(String data, NetworkParameters params) throws AddressFormatException{
		String SSSStr = data.substring(data.indexOf("Piece=") + 6, data.indexOf("&Time="));
		String creationTimeStr = data.substring(data.indexOf("&Time=") + 6, data.length());
		return new SSSQRData(SSSStr, params, creationTimeStr);
	}
	
	
	
	private BufferedImage completePaperWallet(URL paperWalletTemplateURL, String pieceHex, Image qrSSS) throws IOException{
		checkNotNull(paperWalletTemplateURL);
		BufferedImage a = ImageIO.read(paperWalletTemplateURL);
        BufferedImage b = SwingFXUtils.fromFXImage(qrSSS, null);
        BufferedImage d = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
		 
        //Crop the QR code images
        int x = (int) (qrSSS.getWidth()*.055), y = (int) (qrSSS.getHeight()*.055), w = (int) (qrSSS.getWidth()*.90), h = (int) (qrSSS.getHeight()*.90);
        BufferedImage b2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        b2.getGraphics().drawImage(b, 0, 0, w, h, x, y, x + w, y + h, null);
        
        //Add the QR codes to the paper wallet
        Graphics g = d.getGraphics();
        g.drawImage(a, 0, 0, null);
        g.drawImage(b2, 225, 112, null);
		
        // Get the graphics instance of the buffered image
        Graphics2D gBuffImg = d.createGraphics();

        // Draw the string
        Color aColor = new Color(0x8E6502);
        gBuffImg.setColor(aColor);
        gBuffImg.setFont(new Font("Ubuntu", Font.PLAIN, 12));
        gBuffImg.drawString(pieceHex, 62, 280);

        // Draw the buffered image on the output's graphics object
        g.drawImage(d, 0, 0, null);
        gBuffImg.dispose();
        
        return d;
	}
	
	public class SSSQRData{
		public Share share;
		public long creationTime;
		
		public SSSQRData(String shareHex, NetworkParameters params, String creationTimeStr) throws AddressFormatException{
			creationTime =  (long)Double.parseDouble(creationTimeStr);
			share = Share.fromString(shareHex, params);
		}
	}
}
