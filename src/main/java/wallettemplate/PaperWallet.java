package wallettemplate;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;

import wallettemplate.utils.BaseUI;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.wallet.DeterministicSeed;

public class PaperWallet  extends BaseUI{
	
	public PaperWallet(){
		 super(PaperWallet.class);
	}

	public static void createPaperWallet(String mnemonic, DeterministicSeed seed) throws IOException{
		byte[] imageBytes = null;
		 imageBytes = QRCode
				        .from(seed.toHexString())
				        .withSize(170, 170)
				        .to(ImageType.PNG)
				        .stream()
				        .toByteArray();
        Image qrSeed = new Image(new ByteArrayInputStream(imageBytes), 153, 153, true, false);
       
        DeterministicKey mprivkey = HDKeyDerivation.createMasterPrivateKey(seed.getSecretBytes());
        DeterministicKey mpubkey = mprivkey.getPubOnly();
        imageBytes = QRCode
			        .from(mpubkey.toString())
			        .withSize(160, 160)
			        .to(ImageType.PNG)
			        .stream()
			        .toByteArray();
        Image qrMPubKey = new Image(new ByteArrayInputStream(imageBytes), 122,122, true, false);
      
        String path3 = null;
        URL location = Main.class.getResource("PaperWallet.png");
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
			
        // save
        String filepath = new java.io.File( "." ).getCanonicalPath() + "/" + "paperwallet" + ".png";
		File wallet = new File(filepath);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Paper Wallet");
		fileChooser.setInitialFileName("paperwallet.png");
		File outputfile = fileChooser.showSaveDialog(Main.startup);   
		if(outputfile != null)
			ImageIO.write(d, "png", outputfile);        
       
	}
	
}
