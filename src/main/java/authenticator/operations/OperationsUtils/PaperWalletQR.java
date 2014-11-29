package authenticator.operations.OperationsUtils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import wallettemplate.Main;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import authenticator.BASE;
import authenticator.Utils.EncodingUtils;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.bitcoinj.wallet.DeterministicSeed;
import com.google.common.base.Joiner;

import static org.bitcoinj.core.Utils.HEX;

public class PaperWalletQR extends BASE{
	public PaperWalletQR(){
		super(PaperWalletQR.class);
	}
	
	public BufferedImage generatePaperWallet(String mnemonic, DeterministicSeed seed, long creationTime) throws IOException{
		Image qrSeed = createQRSeedImage(seed, creationTime);
		Image qrMPubKey = createMnemonicStringImage(mnemonic, seed);
		return completePaperWallet(mnemonic, qrSeed, qrMPubKey);
	}
	
	@SuppressWarnings("restriction")
	public Image createQRSeedImage(DeterministicSeed seed, long creationTime){
		byte[] imageBytes = this.createQRSeedImageBytes(seed, creationTime, 170, 170);
        Image qrSeed = new Image(new ByteArrayInputStream(imageBytes), 153, 153, true, false);
        return qrSeed;
	}
	
	public byte[] createQRSeedImageBytes(DeterministicSeed seed, long creationTime, int w, int h){
		byte[] imageBytes = null;
		imageBytes = QRCode
				        .from(generateQRSeedDataString(seed, creationTime))
				        .withSize(w, h)
				        .to(ImageType.PNG)
				        .stream()
				        .toByteArray();
        return imageBytes;
	}
	
	public String generateQRSeedDataString(DeterministicSeed seed, long creationTime)
	{
		String qrCodeData = null;
		MnemonicCode ms = null;
		try {
 			ms = new MnemonicCode();
 			List<String> mnemonic = seed.getMnemonicCode();
 			byte[] entropy = ms.toEntropy(mnemonic);
 			String entropyHex = HEX.encode(entropy);
 			qrCodeData = "Seed=" + entropyHex + 
		  			"&Time=" + creationTime;
 		} catch (Exception e) {
 			e.printStackTrace();
 		}

		
		try {
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
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
		
		public SeedQRData(String entropyHex, String creationTimeStr){
			creationTime =  (long)Double.parseDouble(creationTimeStr);
			
			// get mnemonic seed
			MnemonicCode ms = null;
			try {
	 			ms = new MnemonicCode();
	 			byte[] entropy = HEX.decode(entropyHex);
	 			List<String> mnemonic = ms.toMnemonic(entropy);
	 			seed = new DeterministicSeed(mnemonic, null,"", creationTime);
	 			String mnemonicStr = Joiner.on(" ").join(seed.getMnemonicCode());
	 			LOG.info("Restored seed from QR: " + mnemonicStr);
	 		} catch (Exception e) {
	 			e.printStackTrace();
	 		}
			
		}
	}
	
	private Image createMnemonicStringImage(String mnemonic, DeterministicSeed seed){
		byte[] imageBytes = null;
		DeterministicKey mprivkey = HDKeyDerivation.createMasterPrivateKey(seed.getSecretBytes());
        DeterministicKey mpubkey = mprivkey.getPubOnly();
        imageBytes = QRCode
			        .from(mpubkey.toString())
			        .withSize(120, 120)
			        .to(ImageType.PNG)
			        .stream()
			        .toByteArray();
        Image qrMPubKey = new Image(new ByteArrayInputStream(imageBytes));
        return qrMPubKey;
	}
	
	private BufferedImage completePaperWallet(String mnemonic, Image qrSeed, Image qrMPubKey) throws IOException{
        URL location = Main.class.getResource("/wallettemplate/startup/PaperWallet.png");
        BufferedImage a = ImageIO.read(location);
        BufferedImage b = SwingFXUtils.fromFXImage(qrSeed, null);
        BufferedImage c = SwingFXUtils.fromFXImage(qrMPubKey, null);
        BufferedImage d = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
		 
        //Crop the QR code images
        int x = (int) (qrSeed.getWidth()*.055), y = (int) (qrSeed.getHeight()*.055), w = (int) (qrSeed.getWidth()*.90), h = (int) (qrSeed.getHeight()*.90);
        BufferedImage b2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        b2.getGraphics().drawImage(b, 0, 0, w, h, x, y, x + w, y + h, null);
        x = x = (int) (qrMPubKey.getWidth()*.05); y = (int) (qrMPubKey.getHeight()*.05); w = (int) (qrMPubKey.getWidth()*.9); h = (int) (qrMPubKey.getHeight()*.9);
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
