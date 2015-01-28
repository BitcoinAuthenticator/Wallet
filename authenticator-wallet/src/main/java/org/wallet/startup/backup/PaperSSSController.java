package org.wallet.startup.backup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.wallet.Main;
import org.wallet.utils.BaseUI;
import org.authenticator.BipSSS.BipSSS.Share;
import org.authenticator.operations.operationsUtils.PaperSSSQR;

public class PaperSSSController  extends BaseUI{
	
	public PaperSSSController(){
		 super(PaperSSSController.class);
	}

	public static void createAndSavePaperSSS(Share share, long creationTime, File destinationFolder) throws IOException{
		PaperSSSQR maker = new PaperSSSQR();
		BufferedImage bi = maker.generatePaperWalletFromTemplate(share, creationTime);
			
        // save
		String fileName = "paper share number " + share.shareNumber + ".png";
		File outputfile = new File(destinationFolder.getAbsolutePath() + File.separator + fileName);   
		if(outputfile != null)
			ImageIO.write(bi, "png", outputfile);        
       
	}
	
}
