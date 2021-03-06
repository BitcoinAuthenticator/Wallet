package wallettemplate.startup.backup;

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

import wallettemplate.Main;
import wallettemplate.utils.BaseUI;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import authenticator.operations.OperationsUtils.PaperWalletQR;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicSeed;

public class PaperWalletController  extends BaseUI{
	
	public PaperWalletController(){
		 super(PaperWalletController.class);
	}

	@SuppressWarnings("restriction")
	public static void createPaperWallet(String mnemonic, DeterministicSeed seed, long creationTime) throws IOException{
		PaperWalletQR maker = new PaperWalletQR();
		BufferedImage bi = maker.generatePaperWallet(mnemonic, seed, creationTime);
			
        // save
        String filepath = new java.io.File( "." ).getCanonicalPath() + "/" + "paperwallet" + ".png";
		File wallet = new File(filepath);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Paper Wallet");
		fileChooser.setInitialFileName("paperwallet.png");
		File outputfile = fileChooser.showSaveDialog(Main.startup);   
		if(outputfile != null)
			ImageIO.write(bi, "png", outputfile);        
       
	}
	
}
