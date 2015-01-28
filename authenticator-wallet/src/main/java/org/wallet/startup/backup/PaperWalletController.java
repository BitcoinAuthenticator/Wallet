package org.wallet.startup.backup;

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

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.authenticator.operations.operationsUtils.PaperWalletQR;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicSeed;
import org.wallet.Main;
import org.wallet.utils.BaseUI;

public class PaperWalletController  extends BaseUI{
	
	public PaperWalletController(){
		 super(PaperWalletController.class);
	}

	@SuppressWarnings("restriction")
	public static void createPaperWallet(String mnemonic, DeterministicSeed seed, long creationTime) throws IOException{
		PaperWalletQR maker = new PaperWalletQR();
		BufferedImage bi = maker.generatePaperWalletFromTemplate(seed);
			
        // save
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Paper Wallet");
		fileChooser.setInitialFileName("paperwallet.png");
		File outputfile = fileChooser.showSaveDialog(Main.startup);   
		if(outputfile != null)
			ImageIO.write(bi, "png", outputfile);        
       
	}
	
}
