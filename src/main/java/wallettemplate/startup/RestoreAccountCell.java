package wallettemplate.startup;

import java.awt.Button;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;

import com.google.bitcoin.core.Coin;

import wallettemplate.Main;
import wallettemplate.Main.OverlayUI;
import authenticator.Utils.OneName.OneName;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
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
	@FXML private ImageView ivLogo;
	
	AccountCellListener listener;
	
	@SuppressWarnings("restriction")
	public RestoreAccountCell(WalletAccountType type, AccountCellListener r) {
		this.listener = r;
        this.loadFXML();
        this.setSnapToPixel(true);
        if(type == WalletAccountType.StandardAccount)
        	ivLogo.setImage(new Image(File.separator + "wallettemplate" + File.separator + "bitcoin_logo_plain_small.png"));
        else
        	ivLogo.setImage(new Image(File.separator + "wallettemplate" + File.separator + "authenticator_logo_plain_small.png"));
      }
	
	public String getAccountTypeName() { return lblAccountType.getText(); }
	public void setAccountTypeName(String value){
		lblAccountType.setText(value);
	}
	
	public int getAccountID(){ return Integer.parseInt(lblAccountID.getText()); }
	public void setAccountID(String value){
		lblAccountID.setText(value);
	}
	
	public String getAccountName(){ return lblAccountName.getText(); }
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
        return "startup" + File.separator + "restore_account_cell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
    
    @FXML protected void close(ActionEvent event){
		 if(this.listener != null)
			 this.listener.close(this);
	 }
    
    public interface AccountCellListener{
    	public void close(RestoreAccountCell cell);
    }
}
