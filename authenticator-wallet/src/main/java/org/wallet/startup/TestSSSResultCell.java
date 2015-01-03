package org.wallet.startup;

import java.awt.Button;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;
import org.wallet.Main;
import org.wallet.Main.OverlayUI;
import org.bitcoinj.core.Coin;

import org.authenticator.Utils.OneName.OneName;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.image.Image;

public class TestSSSResultCell extends Region{	
	@FXML private Label lbl;
	@FXML private ImageView imgView;
	
		
	@SuppressWarnings("restriction")
	public TestSSSResultCell() {
        this.loadFXML();
        this.setSnapToPixel(true);
      }
	
	public String getResult(){ return lbl.getText(); }
	public void setResult(String value){
		lbl.setText(value);
	}
	
	public void setIcon(Image value){
		imgView.setImage(value);
	}
	
	@SuppressWarnings("restriction")
	private void loadFXML() {
        FXMLLoader loader = new FXMLLoader();
        loader.setController(this);
        loader.setLocation(this.getViewURL());
        try {
            Node root = (Node) loader.load();
            this.getChildren().add(root);
        }
        catch (IOException ex) {
           ex.printStackTrace();
        }    
    }
	
	private String getViewPath() {
        return "startup/TestSSSResultCell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
}
