package wallettemplate.startup;

import java.awt.Button;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;

import com.google.bitcoin.core.Coin;

import wallettemplate.Main;
import wallettemplate.Main.OverlayUI;
import authenticator.network.OneName;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
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

public class RestoreAccountCell extends Region{	
	@FXML private Label lblAccountType;
	@FXML private Label lblAccountID;
	@FXML private Label lblAccountName;
	@FXML private ImageView ivAuth;
	@FXML private ImageView ivBitcoin;
	
	@SuppressWarnings("restriction")
	public RestoreAccountCell() {
        this.loadFXML();
        this.setSnapToPixel(true);
      }
	
	public void setAccountTypeName(String value){
		lblAccountType.setText(value);
	}
	
	public void setAccountID(String value){
		lblAccountID.setText(value);
	}
	
	public void setAccountName(String value){
		lblAccountName.setText(value);
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
        return "startup/restore_account_cell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
}
