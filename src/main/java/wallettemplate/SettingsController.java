package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.base.Joiner;

import authenticator.Authenticator;
import authenticator.db.settingsDB;
import authenticator.walletCore.exceptions.NoWalletPasswordException;
import wallettemplate.startup.StartupController;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.TextFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
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
	@FXML private TextField txfShowSeedPassword;
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
	private String strFee;
	private double falsePositiveRate;
	

	// Called by FXMLLoader
    @SuppressWarnings("restriction")
	public void initialize() throws IOException {        	
    	settingsDB set = new settingsDB(Authenticator.getWalletOperation().getApplicationParams().getAppName());
    	String unit = set.getAccountUnit().toString();
    	if (unit.equals("BTC")){this.strBitcoinUnit="BTC";}
    	else if (unit.equals("Bits")){this.strBitcoinUnit="bits";}
    	else if (unit.equals("Millibits")){this.strBitcoinUnit="mBTC";}
    	else {this.strBitcoinUnit="µBTC";}
    	this.intDecimal = set.getDecimalPoint();
    	this.strCurrency = set.getLocalCurrencySymbol();
    	this.strLanguage = set.getLanguage().toString();
    	this.strFee = String.valueOf(set.getDefaultFee());
    	this.useTor = set.getIsUsingTOR();
    	this.localHost = set.getIsConnectingToLocalHost();
    	this.TrustedPeer = set.getIsConnectingToTrustedPeer();
    	this.falsePositiveRate = set.getBloomFilterFalsePositiveRate();
    	super.initialize(SettingsController.class);
    	cbBitcoinUnit.setValue(strBitcoinUnit);
    	cbCurrency.setValue(strCurrency);
    	cbDecimal.setValue(String.valueOf(intDecimal));
    	cbLanguage.setValue(strLanguage);
    	txFee.setText(strFee + " satoshi"); //need to convert this into the unit in the choice box
    	ckTor.setSelected(useTor);
    	ckLocalHost.setSelected(localHost);
    	ckTrustedPeer.setSelected(TrustedPeer);
    	slBloom.setValue(falsePositiveRate);
    	if (ckTrustedPeer.isSelected()){txPeerIP.setDisable(true);}
    	else {txPeerIP.setDisable(false);}
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

	public void exit(ActionEvent event) {
		overlayUi.done();
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
