package org.wallet.controls;

import java.awt.Button;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;
import org.wallet.Main;
import org.wallet.Main.OverlayUI;
import org.bitcoinj.core.Coin;

import org.authenticator.Utils.OneName.OneName;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.image.Image;

public class DisplayAccountCell extends Region{	
	
	private ATAccount account;
	private AccountCellEvents listener;
	
	private boolean isSettingsOpen;
	
	@FXML private Label lblConfirmed;
	@FXML private Label lblUnconfirmed;
	@FXML private Label lblTotal;
	@FXML private Label lblName;
	@FXML private Label lblAccountID;
	@FXML private Pane bgPane;
	
	@SuppressWarnings("restriction")
	public DisplayAccountCell(ATAccount account) {
        this.loadFXML();
        this.setSnapToPixel(true);
        
        this.account = account;
        isSettingsOpen = false;
        updateUI();
        
    }
	
	public void updateUI(){
		setConfirmed(Long.toString(this.account.getConfirmedBalance()));
		setUnonfirmed(Long.toString(this.account.getUnConfirmedBalance()));
		setTotal(Long.toString(this.account.getConfirmedBalance()), Long.toString(this.account.getUnConfirmedBalance()));
		setAccountName(this.account.getAccountName());
		setAccountID(Integer.toString(this.account.getIndex()));
	}
	
	private void setAccountID(String id){
		this.lblAccountID.setText(id);
	}
	
	public ATAccount getAccount(){
		return this.account;
	}
	
	public void setAccount (ATAccount account){
		this.account = account;
	}
	
	public DisplayAccountCell setListener(AccountCellEvents listener){
		this.listener = listener;
		return this;
	}
	
	
	public void setConfirmed(String amount){
		Coin a = Coin.valueOf(Long.parseLong(amount));
		lblConfirmed.setText(a.toFriendlyString());
	}
	
	public void setUnonfirmed(String amount){
		Coin a = Coin.valueOf(Long.parseLong(amount));
		lblUnconfirmed.setText(a.toFriendlyString());
	}
	
	public void setTotal(String amountConfirmed, String amountUnconfirmed) {
		Coin conf = Coin.valueOf(Long.parseLong(amountConfirmed));
		Coin unconf = Coin.valueOf(Long.parseLong(amountUnconfirmed));
		lblTotal.setText(conf.add(unconf).toFriendlyString());
	}
	
	public void setAccountName(String name){
		lblName.setText(name);
	}
	public void setSelected(boolean value) {
		if(!value)
			bgPane.setStyle("-fx-background-color: #ffffff;");
		else
			bgPane.setStyle("-fx-background-color: #f7f8f8;");
	}
	
	@FXML protected void cellClicked(MouseEvent mouseEvent){
    	if(this.listener != null)
    		this.listener.onClick(this);
    }
	
	/*
	 * 
	 * FXML loader
	 * 
	 */
	
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
        return "accounts/account_cell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
	
	public interface AccountCellEvents{
		public void onClick(DisplayAccountCell cell);
	}
	
}
