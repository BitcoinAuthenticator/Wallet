package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;

import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.base.Joiner;

import authenticator.Authenticator;
import authenticator.db.settingsDB;
import authenticator.protobuf.ProtoSettings.BitcoinUnit;
import authenticator.protobuf.ProtoSettings.Languages;
import authenticator.walletCore.exceptions.NoWalletPasswordException;
import wallettemplate.startup.StartupController;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.TextFieldValidator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class SettingsController  extends BaseUI{
	@FXML private AnchorPane SettingsApp;
	@FXML private Button btnDone;
	@FXML private Button btnRestore;
	@FXML private Button btnBackup;
	@FXML private Button btnChange;
	@FXML private Button btnShowSeed;
	@FXML private Pane settingspane;
	@FXML private ChoiceBox cbCurrency;
	@FXML private ChoiceBox cbDecimal;
	@FXML private ChoiceBox cbBitcoinUnit;
	@FXML private ChoiceBox cbLanguage;
	@FXML private Slider slBloom;
	@FXML private CheckBox ckTor;
	@FXML private CheckBox ckLocalHost;
	@FXML private CheckBox ckTrustedPeer;
	@FXML private TextField txFee;
	@FXML private TextField txPeerIP;
	@FXML private PasswordField txfShowSeedPassword;
	public Main.OverlayUI overlayUi;
	private double xOffset = 0;
	private double yOffset = 0;
	private String strBitcoinUnit;
	private int intDecimal;
	private String strCurrency;
	private String strLanguage;
	private boolean useTor;
	private boolean localHost;
	private boolean TrustedPeer;
	private String peerIP;
	private String strFee;
	private double falsePositiveRate;
	private double fee;
	settingsDB set;

	// Called by FXMLLoader
    @SuppressWarnings("restriction")
	public void initialize() throws IOException {        	
    	set = new settingsDB(Authenticator.getWalletOperation().getApplicationParams().getAppName());
    	String unit = set.getAccountUnit().toString();
    	if (unit.equals("BTC")){this.strBitcoinUnit="BTC";}
    	else if (unit.equals("Bits")){this.strBitcoinUnit="bits";}
    	else if (unit.equals("Millibits")){this.strBitcoinUnit="mBTC";}
    	else {this.strBitcoinUnit="µBTC";}
    	this.intDecimal = set.getDecimalPoint();
    	this.strCurrency = set.getLocalCurrencySymbol();
    	this.strLanguage = set.getLanguage().toString();
    	this.useTor = set.getIsUsingTOR();
    	this.localHost = set.getIsConnectingToLocalHost();
    	this.TrustedPeer = set.getIsConnectingToTrustedPeer();
    	this.falsePositiveRate = set.getBloomFilterFalsePositiveRate();
    	Tooltip.install(slBloom, new Tooltip(String.valueOf(slBloom.getValue())));
    	super.initialize(SettingsController.class);
    	cbBitcoinUnit.setValue(strBitcoinUnit);
    	cbCurrency.setValue(strCurrency);
    	cbCurrency.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
  	      @Override
  	      public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
  	    		strCurrency = cbCurrency.getItems().get((Integer) number2).toString();
  	      }
  	    });
    	try {fee = (double) set.getDefaultFee();} 
  	    catch (IOException e) {e.printStackTrace();}
    	//Still need to round decimal values in the prompt text.
    	cbBitcoinUnit.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
    	      @Override
    	      public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
    	    		if ((Integer) number2 == 1){
    	    			strBitcoinUnit="BTC";
    	   				txFee.setPromptText(fee/100000000 + " BTC");
    	    		}
    	    		else if ((Integer) number2 == 0){
    	    			strBitcoinUnit="bits";
    	    			txFee.setPromptText(fee/100 + " bits");
    	    		}
    	    		else if ((Integer) number2 == 2){
    	    			strBitcoinUnit="mBTC";
    	    			txFee.setPromptText(fee/100000 + " mBTC");
    	    			}
    	    		else {
    	    			strBitcoinUnit="µBTC";
    	    			txFee.setPromptText(fee/100 + " µBTC");
    	    		}
    	      }
    	    });
    	cbDecimal.setValue(String.valueOf(intDecimal));
    	cbDecimal.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
    	      @Override
    	      public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
    	    		intDecimal = Integer.parseInt(cbCurrency.getItems().get((Integer) number2).toString());
    	      }
    	    });
    	cbLanguage.setValue(strLanguage);
    	if (this.strBitcoinUnit.equals("BTC")){txFee.setPromptText(fee/100000000 + " BTC");}
    	else if (this.strBitcoinUnit.equals("bits")){txFee.setPromptText(fee/100 + " bits");}
    	else if (this.strBitcoinUnit.equals("mBTC")){txFee.setPromptText(fee/100000 + " mBTC");}
    	else {txFee.setPromptText(fee/100 + "µBTC");}
    	ckTor.setSelected(useTor);
    	ckTor.selectedProperty().addListener(new ChangeListener<Boolean>() {
			 public void changed(ObservableValue<? extends Boolean> ov,
					 Boolean old_val, Boolean new_val) {
				 if (ckTor.isSelected()){
					useTor = true;
				 }
				 else {
					 useTor = false;
				 }
			 }
		 });
    	ckLocalHost.setSelected(localHost);
    	ckLocalHost.selectedProperty().addListener(new ChangeListener<Boolean>() {
			 public void changed(ObservableValue<? extends Boolean> ov,
					 Boolean old_val, Boolean new_val) {
				 if (ckLocalHost.isSelected()){
					 localHost = true;
					 ckTor.setSelected(false);
					 ckTor.setDisable(true);
					 ckTrustedPeer.setDisable(true);
					 ckTrustedPeer.setSelected(false);
				 }
				 else {
					 localHost = false;
					 ckTor.setDisable(false);
					 ckTrustedPeer.setDisable(false);
				 }
			 }
		 });
    	ckTrustedPeer.setSelected(TrustedPeer);
    	if (TrustedPeer) {
    		txPeerIP.setText(set.getTrustedPeerIP());
    		txPeerIP.setDisable(false);
    		ckTor.setDisable(false);
    		ckLocalHost.setSelected(false);
    		ckLocalHost.setDisable(true);
    		ckLocalHost.setSelected(false);
    	}
    	if (localHost){
    		txPeerIP.setText("");
    		txPeerIP.setDisable(false);
    		ckTor.setDisable(false);
    		ckLocalHost.setSelected(false);
    		ckLocalHost.setDisable(true);
    		ckLocalHost.setSelected(false);
    	}
    	slBloom.setValue(falsePositiveRate);
    	if (!ckTrustedPeer.isSelected()){txPeerIP.setDisable(true);}
    	else {txPeerIP.setDisable(false);}
    	ckTrustedPeer.selectedProperty().addListener(new ChangeListener<Boolean>() {
			 public void changed(ObservableValue<? extends Boolean> ov,
					 Boolean old_val, Boolean new_val) {
				 if (ckTrustedPeer.isSelected()){
					 TrustedPeer = true;
					 txPeerIP.setDisable(false);
					 ckTor.setDisable(false);
					 ckLocalHost.setSelected(false);
					 ckLocalHost.setDisable(true);
					 ckLocalHost.setSelected(false);
				 }
				 else {
					TrustedPeer = false;
					txPeerIP.setDisable(true);
					ckLocalHost.setDisable(false);
				 }
			 }
		 });
    	btnDone.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnDone.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
    	
    	
    	
        btnDone.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnDone.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
        btnRestore.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnRestore.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnRestore.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnRestore.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
        btnShowSeed.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnShowSeed.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnShowSeed.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnShowSeed.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
        btnChange.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnChange.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnChange.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnChange.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
    }
    
    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stage.setX(event.getScreenX() - xOffset);
    	Main.stage.setY(event.getScreenY() - yOffset);
    }

	public void exit(ActionEvent event) throws IOException {
		overlayUi.done();
		set.setAccountUnit(BitcoinUnit.Bits);
		if (strBitcoinUnit.equals("BTC")){set.setAccountUnit(BitcoinUnit.BTC);}
    	else if (strBitcoinUnit.equals("bits")){set.setAccountUnit(BitcoinUnit.Bits);}
    	else if (strBitcoinUnit.equals("mBTC")){set.setAccountUnit(BitcoinUnit.Millibits);}
    	else {set.setAccountUnit(BitcoinUnit.NanoBits);}
		set.setDecimalPoint(intDecimal);
		set.setLocalCurrencySymbol(strCurrency);
		set.setLanguage(Languages.English);
		set.setIsUsingTOR(useTor);
		set.setIsConnectingToLocalHost(localHost);
		if (TrustedPeer){set.setIsConnectingToTrustedPeer(true, peerIP);}
		else {set.setNotConnectingToTrustedPeer();}
		set.setBloomFilterFalsePositiveRate((float)slBloom.getValue());
    }
	
	public static Stage backupPane;
	@SuppressWarnings("restriction")
	public void launchBackup(ActionEvent event) throws NoWalletPasswordException {
		if(Authenticator.getWalletOperation().isWalletEncrypted())
		if(Main.UI_ONLY_WALLET_PW == null || Main.UI_ONLY_WALLET_PW.length() == 0)
		{
			informationalAlert("Please Unlock Your Wallet",
 					 "In the main window, unlock your wallet to back it up.");
			return;
		}
		
		Parent root;
        try {
        	StartupController.appParams = Authenticator.getApplicationParams();
        	URL location = getClass().getResource("/wallettemplate/startup/walletstartup.fxml");
        	FXMLLoader loader = new FXMLLoader(location);
            root = loader.load();
            backupPane = new Stage();
            backupPane.setTitle("Backup");
            backupPane.initStyle(StageStyle.UNDECORATED);
            Scene scene1 = new Scene(root, 607, 400);
            final String file1 = TextFieldValidator.class.getResource("GUI.css").toString();
            scene1.getStylesheets().add(file1);  // Add CSS that we need.
            backupPane.setScene(scene1);
            StartupController controller =	loader.getController();
            DeterministicSeed seed = Authenticator.getWalletOperation().getWalletSeed(Main.UI_ONLY_WALLET_PW);
            controller.setBackMode(seed);
            backupPane.show();
        } catch (IOException e) {e.printStackTrace();}
    }
	
	@FXML protected void showSeed(ActionEvent event){
		if(Authenticator.getWalletOperation().isWalletEncrypted()){
			if(txfShowSeedPassword.getText().length() == 0)
			{
				informationalAlert("Unfortunately, you messed up.",
      					 "You need to enter your password to decrypt your wallet.");
				return;
			}
			
			Main.UI_ONLY_WALLET_PW.setPassword(txfShowSeedPassword.getText());
			
		}
		
		Main.instance.overlayUI("show_seed.fxml");
	}
	
    
}
