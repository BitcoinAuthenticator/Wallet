package authenticator.operations.OperationsUtils;

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

import wallettemplate.Main;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import authenticator.Utils.EncodingUtils;

import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.subgraph.orchid.encoders.Hex;

public class PaperWalletQR {
	public PaperWalletQR(){
		
	}
	
	public BufferedImage generatePaperWallet(String mnemonic, DeterministicSeed seed, long creationTime) throws IOException{
		Image qrSeed = createQRSeedImage(seed, creationTime);
		Image qrMPubKey = createMnemonicStringImage(mnemonic, seed);
		return completePaperWallet(mnemonic, qrSeed, qrMPubKey);
	}
	
	@SuppressWarnings("restriction")
	private Image createQRSeedImage(DeterministicSeed seed, long creationTime){
		byte[] imageBytes = null;
		imageBytes = QRCode
				        .from(generateQRSeedDataString(seed, creationTime))
				        .withSize(170, 170)
				        .to(ImageType.PNG)
				        .stream()
				        .toByteArray();
        Image qrSeed = new Image(new ByteArrayInputStream(imageBytes), 153, 153, true, false);
        return qrSeed;
	}
	
	private String generateQRSeedDataString(DeterministicSeed seed, long creationTime)
	{
		String qrCodeData = "Seed=" + seed.toHexString() + 
				  			"&Time=" + creationTime;
		
		String h = seed.toHexString();
		byte[] barrO = seed.getSecretBytes();
		byte[] barr = Hex.decode(h.getBytes());
		
		DeterministicSeed s = new DeterministicSeed(barr, "", creationTime);
		
		return qrCodeData;
	}
	
	public SeedQRData parseSeedQR(String data){
		String seedStr = data.substring(data.indexOf("Seed=") + 5, data.indexOf("&Time="));
		String creationTimeStr = data.substring(data.indexOf("&Time=") + 6, data.length());
		return new SeedQRData(seedStr, creationTimeStr);
	}
	
	public class SeedQRData{
		public DeterministicSeed seed;
		public long creationTime;
		
		public SeedQRData(String seedHex, String creationTimeStr){
			creationTime =  (long)Double.parseDouble(creationTimeStr);
			byte[] seedArr = Hex.decode(seedHex.getBytes());//EncodingUtils.hexStringToByteArray(seedHex);
			System.out.println("seed byte array size " + seedArr.length);
			seed = new DeterministicSeed(seedArr, "", creationTime);
		}
	}
	
	private Image createMnemonicStringImage(String mnemonic, DeterministicSeed seed){
		byte[] imageBytes = null;
		DeterministicKey mprivkey = HDKeyDerivation.createMasterPrivateKey(seed.getSecretBytes());
        DeterministicKey mpubkey = mprivkey.getPubOnly();
        imageBytes = QRCode
			        .from(mpubkey.toString())
			        .withSize(160, 160)
			        .to(ImageType.PNG)
			        .stream()
			        .toByteArray();
        Image qrMPubKey = new Image(new ByteArrayInputStream(imageBytes), 122,122, true, false);
        return qrMPubKey;
	}
	
	private BufferedImage completePaperWallet(String mnemonic, Image qrSeed, Image qrMPubKey) throws IOException{
		String path3 = null;
        URL location = Main.class.getResource("/wallettemplate/startup/PaperWallet.png");
        try {path3 = new java.io.File( "." ).getCanonicalPath() + "/paperwallet.png";} 
        catch (IOException e1) {e1.printStackTrace();}
        BufferedImage a = ImageIO.read(location);
        BufferedImage b = SwingFXUtils.fromFXImage(qrSeed, null);
        BufferedImage c = SwingFXUtils.fromFXImage(qrMPubKey, null);
        BufferedImage d = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
		 
        //Crop the QR code images
        int x = (int) (qrSeed.getWidth()*.055), y = (int) (qrSeed.getHeight()*.055), w = (int) (qrSeed.getWidth()*.90), h = (int) (qrSeed.getHeight()*.90);
        BufferedImage b2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        b2.getGraphics().drawImage(b, 0, 0, w, h, x, y, x + w, y + h, null);
        x = x = (int) (qrSeed.getWidth()*.05); y = (int) (qrSeed.getHeight()*.05); w = (int) (qrMPubKey.getWidth()*.90); h = (int) (qrMPubKey.getHeight()*.9);
        BufferedImage c2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        c2.getGraphics().drawImage(c, 0, 0, w, h, x, y, x + w, y + h, null);
        
        //Add the QR codes to the paper wallet
        Graphics g = d.getGraphics();
        g.drawImage(a, 0, 0, null);
        g.drawImage(b2, 401, 112, null);
        g.drawImage(c2, 26, 61, null);
		
        // Get the graphics instance of the buffered image
        Graphics2D gBuffImg = d.createGraphics();

        // Draw the string
        Color aColor = new Color(0x8E6502);
        gBuffImg.setColor(aColor);
        gBuffImg.setFont(new Font("Ubuntu", Font.PLAIN, 12));
        gBuffImg.drawString(mnemonic, 62, 280);

        // Draw the buffered image on the output's graphics object
        g.drawImage(d, 0, 0, null);
        gBuffImg.dispose();
        
        return d;
	}
}
