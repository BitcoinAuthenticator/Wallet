package org.wallet.startup;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.wallet.Main;
import org.wallet.apps.AuthenticatorAppController;
import org.wallet.apps.AuthenticatorAppController.PairingWalletControllerListener;
import org.wallet.startup.StartupController.AddAccountListener;
import org.wallet.startup.StartupController.AddedAccountObject;
import org.wallet.utils.BaseUI;
import org.wallet.utils.GuiUtils;
import org.wallet.utils.TextFieldValidator;

import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class AddAccountWindow extends BaseUI{	
	@FXML private TextArea txfAccountID;
	@FXML private TextArea txfAccountName;
	//@FXML private TextArea txfAccountName;
	
	static AddAccountListener listener;
	WalletAccountType type;
	
	static Stage stage;
	
	boolean isPaired = false;
	
	public AddAccountWindow(){ super(AddAccountWindow.class); }
	@SuppressWarnings("restriction")
	public AddAccountWindow( WalletAccountType type, AddAccountListener lis) {
		 super(AddAccountWindow.class);
		 listener = lis;
		 this.type = type;
		 stage = loadFXML(stage, getViewURL(getViewPath(type)), 328, 370, null, null);
    }
	
	@SuppressWarnings("restriction")
	private Stage loadFXML(Stage s, URL url, int width, int height, @Nullable ArrayList<Object> param, @Nullable PairingWalletControllerListener l) {    	
		s = new Stage();
		try {
			FXMLLoader loader = new FXMLLoader(url);
			s.setTitle("Add Account");
	    	Scene scene;
			scene = new Scene((AnchorPane) loader.load(), width, height);
			if(param != null){
				BaseUI controller = loader.<BaseUI>getController();
				try{
					AuthenticatorAppController w = loader.<AuthenticatorAppController>getController();
					w.setListener(l);
				}
				catch (Exception e){ }
				controller.setParams(param);
				controller.updateUIForParams();
			}
			final String file = TextFieldValidator.class.getResource("GUI.css").toString();
	        scene.getStylesheets().add(file); 
	        s.setScene(scene);	
	        return s;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	private String getViewPath(WalletAccountType type) {
		if (type == WalletAccountType.StandardAccount)
			return "startup/AddStandardAccount.fxml";
		else
			return "startup/AddPairedAccount.fxml";
    }

    private URL getViewURL(String path) {
        return Main.class.getResource(path);
    }
    
    public void show(){
    	stage.show();
    }
    
    private boolean validateDone(){
    	try{
    		Integer.parseInt(txfAccountID.getText());
    	}
    	catch(Exception e){
    		return false;
    	}
    	
    	if(txfAccountName.getText().length() <= 2)
    		return false;
    	
    	if(!isPaired && type == WalletAccountType.AuthenticatorAccount)
    		return false;
    	
    	return true;
    }
    
    @FXML protected void done(ActionEvent event){
    	if(validateDone()){
    		if(this.listener != null)
    			this.listener.addedAccount(new AddedAccountObject(type, 
    					txfAccountName.getText(), 
    					Integer.parseInt(txfAccountID.getText())));
    		stage.close();
    	}
    	else{
    		GuiUtils.informationalAlert("Something is wrong", "Make sure u entered all fields correctly");
    	}
	 }
    
    Stage pairWindow = null;
    @FXML protected void pair(ActionEvent event){
    	pairWindow = loadFXML(pairWindow, 
    			getViewURL("/org/wallet/apps/authenticator/app/AuthenticatorApp.fxml"),
    			850, 484, 
    			new ArrayList(Arrays.asList((Object)txfAccountName.getText(), (Object)txfAccountID.getText())),
    			new PairingWalletControllerListener(){
					@Override
					public void onPairedWallet() {
						isPaired = true;
					}

					@SuppressWarnings("restriction")
					@Override
					public void onFailed(Exception e) {
						isPaired = false;
						Platform.runLater(() -> GuiUtils.informationalAlert("Something is wrong", "Pairing Failed"));
					}

					@SuppressWarnings("restriction")
					@Override
					public void closeWindow() {
						Platform.runLater(new Runnable() {
					        @Override
					        public void run() {
					        	pairWindow.close();
					        }
						});
						
					}
    			});
    	pairWindow.show();
    }
}
