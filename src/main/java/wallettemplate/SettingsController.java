package wallettemplate;

import java.io.IOException;

import authenticator.Authenticator;
import authenticator.db.settingsDB;
import wallettemplate.utils.BaseUI;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;


public class SettingsController  extends BaseUI{
	@FXML private AnchorPane SettingsApp;
	@FXML private Button btnDone;
	@FXML private Button btnRestore;
	@FXML private Button btnChange;
	@FXML private Button btnShow;
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
    	cbDecimal.setValue(intDecimal);
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
        btnShow.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnShow.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnShow.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnShow.setStyle("-fx-background-color: #b3b1b1;");
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
    
}
