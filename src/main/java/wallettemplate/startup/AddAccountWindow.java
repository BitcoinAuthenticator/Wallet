package wallettemplate.startup;

import java.awt.Button;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.json.JSONException;
import org.bitcoinj.core.Coin;

import wallettemplate.Controller;
import wallettemplate.Main;
import wallettemplate.Main.OverlayUI;
import wallettemplate.PairWallet;
import wallettemplate.PairWallet.PairingWalletControllerListener;
import wallettemplate.startup.StartupController.AddAccountListener;
import wallettemplate.startup.StartupController.AddedAccountObject;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextFieldValidator;
import authenticator.Utils.OneName.OneName;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
					PairWallet w = loader.<PairWallet>getController();
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
    			getViewURL("/wallettemplate/pairing/BAApp.fxml"),
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
