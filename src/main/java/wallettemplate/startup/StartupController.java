package wallettemplate.startup;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.swing.JFrame;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.dialog.Dialogs;

import wallettemplate.Main;
import wallettemplate.SettingsController;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.startup.RestoreAccountCell.AccountCellListener;
import wallettemplate.startup.backup.PaperWalletController;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.BipSSS.BipSSS;
import authenticator.BipSSS.BipSSS.EncodingFormat;
import authenticator.BipSSS.BipSSS.Share;
import authenticator.Utils.EncodingUtils;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.operations.BAWalletRestorer;
import authenticator.operations.BAWalletRestorer.WalletRestoreListener;
import authenticator.operations.OperationsUtils.PaperWalletQR;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javafx.geometry.Pos;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.util.Duration;

public class StartupController  extends BaseUI{
	
	@FXML private Pane MainPane;
	@FXML private Pane CreateAccountPane;
	@FXML private Pane BackupNewWalletPane;
	@FXML private Pane ExplanationPane1;
	@FXML private Pane Pane6;
	@FXML private Pane SSSBackupPane;
	@FXML private Pane MainRestorePane;
	@FXML private Pane RestoreFromMnemonicPane;
	@FXML private Pane RestoreFromQRPane;
	@FXML private Pane RestoreProcessPane;
	@FXML private Pane RestoreAccountsPane;
	@FXML private Pane LoadingPane;
	@FXML private Hyperlink hlpw;
	@FXML private WebView browser;
	@FXML private Button btnNewWallet;
	@FXML private Button btnContinue1;
	@FXML private Button btnBack1;
	@FXML private Button btnContinue2;
	@FXML private Button btnContinue3;
	@FXML private Button btnBack2;
	@FXML private Button btnBack3;
	@FXML private Button btnBack4;
	@FXML private Button btnBack5;
	@FXML private Button btnBackFromSeedRestore;
	@FXML private Button btnRestoreFromSeedContinue;
	@FXML private Button btnBackFromSeedFromQRRestore;
	@FXML private Button btnRestoreFromSeedFromQRContinue;
	@FXML private Button btnCancelRestoreProcess;
	@FXML private Button btnFinishRestoreProcess;
	@FXML private Button btnBackFromAccountRestore;
	@FXML private Button btnAccountRestoreContinue;
	@FXML private Label lblMinimize;
	@FXML private Label lblClose;
	@FXML private Button btnDone;
	@FXML private TextField txAccount;
	@FXML private PasswordField txPW1;
	@FXML private PasswordField txPW2;
	@FXML private Label lblSeed;
	@FXML private CheckBox ckSeed;
	@FXML private CheckBox chkTestNet;
	private double xOffset = 0;
	private double yOffset = 0;
	private String mnemonic = "";
	@FXML private TextField txPieces;
	@FXML private TextField txThreshold;
	@FXML private ListView lvSSS;
	@FXML private Button btnMnemonic;
	@FXML private Button btnScanQR;
	@FXML private Pane BA1;
	@FXML private Pane BA2;
	@FXML private Pane BA3;
	@FXML private Pane BA4;
	@FXML private Hyperlink hlFinished;
	@FXML private ImageView imgSSS;
	@FXML private Button btnSSS;
	@FXML private ProgressBar syncProgress;
	@FXML private Label lblRestoreProcessStatus;
	@FXML private TextField lblSeedRestorer;
	@FXML private DatePicker seedCreationDatePicker;
	@FXML private Label lblSeedFromQR;
	@FXML private Button btnStartWebCamQRScan;
	@FXML private ChoiceBox accountTypeBox;
	@FXML private Label lblLoading;
	@FXML private ScrollPane restoreProcessScrll;
	private ScrollPaneContentManager restoreProcessScrllContent;
	@FXML private ScrollPane restoreAccountsScrll;
	private ScrollPaneContentManager restoreAccountsScrllContent;
	private DeterministicSeed walletSeed;
	String encryptionPassword = null;
	NetworkParameters params;
	Authenticator auth;
	Wallet wallet;
	
	/**
	 * Will be set before the Stage is launched so we could define the wallet files.
	 */
	static public BAApplicationParameters appParams;
	

	 public void initialize() {
		 super.initialize(StartupController.class);
		 if(appParams != null){
			// set testnet checkbox
			 if(appParams.getBitcoinNetworkType() == NetworkType.MAIN_NET){
				 chkTestNet.setSelected(false);
				 params = MainNetParams.get();
			 }
			 else{
				 chkTestNet.setSelected(true);
				 params = TestNet3Params.get();
			 }
			 chkTestNet.setDisable(true);
		 }
		 
		 btnSSS.setPadding(new Insets(-4,0,0,0));
		 //
		 Label labelforward = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labelforward.setPadding(new Insets(0,0,0,6));
		 btnContinue1.setGraphic(labelforward);
		 //
		 Label labelback = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelback.setPadding(new Insets(0,6,0,0));
		 btnBack1.setGraphic(labelback);
		 //
		 Label labelforward2 = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labelforward2.setPadding(new Insets(0,0,0,6));
		 btnContinue2.setGraphic(labelforward2);
		 //
		 Label labelforward3 = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labelforward3.setPadding(new Insets(0,0,0,6));
		 btnContinue3.setGraphic(labelforward3);
		 //
		 Label labelback2 = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelback2.setPadding(new Insets(0,6,0,0));
		 btnBack2.setGraphic(labelback2);
		 //
		 Label labelback3 = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelback3.setPadding(new Insets(0,6,0,0));
		 btnBack3.setGraphic(labelback3);
		 //
		 Label labelback4 = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelback4.setPadding(new Insets(0,6,0,0));
		 btnBack4.setGraphic(labelback4);
		 //
		 Label labelback5 = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelback5.setPadding(new Insets(0,6,0,0));
		 btnBack5.setGraphic(labelback5);
		 //
		 Label labelackFromSeedRestore = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelackFromSeedRestore.setPadding(new Insets(0,6,0,0));
		 btnBackFromSeedRestore.setGraphic(labelackFromSeedRestore);
		 //
		 Label labeRestoreFromSeedContinue = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeRestoreFromSeedContinue.setPadding(new Insets(0,6,0,0));
		 btnRestoreFromSeedContinue.setGraphic(labeRestoreFromSeedContinue);
		 //
		 Label labelbackFromQRRestore = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelbackFromQRRestore.setPadding(new Insets(0,6,0,0));
		 btnBackFromSeedFromQRRestore.setGraphic(labelbackFromQRRestore);
		 //
		 Label labeRestoreFromQRContinue = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeRestoreFromQRContinue.setPadding(new Insets(0,6,0,0));
		 btnRestoreFromSeedFromQRContinue.setGraphic(labeRestoreFromQRContinue);
		 Label lblStartQRScan = AwesomeDude.createIconLabel(AwesomeIcon.QRCODE, "90");
		 btnStartWebCamQRScan.setGraphic(lblStartQRScan);
		 //
		 Label labelBackFromAccountRestore = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelBackFromAccountRestore.setPadding(new Insets(0,6,0,0));
		 btnBackFromAccountRestore.setGraphic(labelBackFromAccountRestore);
		 //
		 Label labeAccountRestoreContinue = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeAccountRestoreContinue.setPadding(new Insets(0,6,0,0));
		 btnAccountRestoreContinue.setGraphic(labeAccountRestoreContinue);
		 //
		 Label labeCancelRestoreProcess = AwesomeDude.createIconLabel(AwesomeIcon.STOP, "20");
		 labeCancelRestoreProcess.setPadding(new Insets(0,6,0,0));
		 btnCancelRestoreProcess.setGraphic(labeCancelRestoreProcess);
		 //
		 Label labeFinishRestoreProcess = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeFinishRestoreProcess.setPadding(new Insets(0,6,0,0));
		 btnFinishRestoreProcess.setGraphic(labeFinishRestoreProcess);
		 //
		 Label lblMnemonic = AwesomeDude.createIconLabel(AwesomeIcon.KEYBOARD_ALT, "90");
		 btnMnemonic.setGraphic(lblMnemonic);
		 //
		 Label lblScanQR = AwesomeDude.createIconLabel(AwesomeIcon.QRCODE, "90");
		 btnScanQR.setGraphic(lblScanQR);
		 //
		 
		 lblMinimize.setPadding(new Insets(0,20,0,0));
		 // Pane Control
		 Tooltip.install(lblMinimize, new Tooltip("Minimize Window"));
		 lblMinimize.setOnMousePressed(new EventHandler<MouseEvent>(){
			 @Override
			 public void handle(MouseEvent t) {
				 Main.startup.setIconified(true);
			 }
		 });
		 Tooltip.install(lblClose, new Tooltip("Close Window"));
		 lblClose.setOnMousePressed(new EventHandler<MouseEvent>(){
			 @Override
			 public void handle(MouseEvent t) {
				 handleExitButtonPressed();
			 }
		 });
		 btnContinue2.setStyle("-fx-background-color: #badb93;");
		 lblSeed.setFont(Font.font(null, FontWeight.BOLD, 18));
		 lblSeed.setPadding(new Insets(0,6,0,6));
		 txPieces.lengthProperty().addListener(new ChangeListener<Number>(){
             @Override
             public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
                   if(newValue.intValue() > oldValue.intValue()){
                       char ch = txPieces.getText().charAt(oldValue.intValue());  
                       //Check if the new character is the number or other's
                       if(!(ch >= '0' && ch <= '9')){       
                            //if it's not number then just setText to previous one
                            txPieces.setText(txPieces.getText().substring(0,txPieces.getText().length()-1)); 
                       }
                  }
             }
		 });
		 txThreshold.lengthProperty().addListener(new ChangeListener<Number>(){
             @Override
             public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
                   if(newValue.intValue() > oldValue.intValue()){
                       char ch = txThreshold.getText().charAt(oldValue.intValue());  
                       //Check if the new character is the number or other's
                       if(!(ch >= '0' && ch <= '9')){       
                            //if it's not number then just setText to previous one
                            txThreshold.setText(txThreshold.getText().substring(0,txThreshold.getText().length()-1)); 
                       }
                  }
             }
		 });
		 final ContextMenu contextMenu2 = new ContextMenu();
		 MenuItem item12 = new MenuItem("Copy");
		 item12.setOnAction(new EventHandler<ActionEvent>() {
			 public void handle(ActionEvent e) {
				 Clipboard clipboard = Clipboard.getSystemClipboard();
				 ClipboardContent content = new ClipboardContent();
				 content.putString(lvSSS.getSelectionModel().getSelectedItem().toString());
				 clipboard.setContent(content);
			 }
		 });
		 contextMenu2.getItems().addAll(item12);
		 lvSSS.setContextMenu(contextMenu2);
		 
		 // accounts restore
		 accountTypeBox.getItems().clear();
		 accountTypeBox.getItems().add("Standard Account");
		 accountTypeBox.getItems().add("Paired Bitcoin Authenticator Account");
		 accountTypeBox.setTooltip(new Tooltip("Select Account Type"));
		 accountTypeBox.setValue("Standard Account");
		 
		 // restore accounts scroll
		 restoreAccountsScrllContent = new ScrollPaneContentManager().setSpacingBetweenItems(15)
				 							.setScrollStyle(restoreAccountsScrll.getStyle());
		 restoreAccountsScrll.setContent(restoreAccountsScrllContent);
		 restoreAccountsScrll.setHbarPolicy(ScrollBarPolicy.NEVER);
		 
		 // restore prcess scroll
		 restoreProcessScrllContent = new ScrollPaneContentManager().setSpacingBetweenItems(15)
				 							.setScrollStyle(restoreProcessScrll.getStyle());
		 restoreProcessScrll.setContent(restoreProcessScrllContent);
		 restoreProcessScrll.setHbarPolicy(ScrollBarPolicy.NEVER);
		 
		 // loading pane
		 Label labelLoading = AwesomeDude.createIconLabel(AwesomeIcon.GEAR, "45");
		 lblLoading.setAlignment(Pos.CENTER);
		 lblLoading.setGraphic(labelLoading);
		 RotateTransition rt = new RotateTransition(Duration.millis(2000),lblLoading);
         rt.setByAngle(360);
         rt.setCycleCount(10000);
         rt.play();
	 }
	 
	 @FXML protected void drag1(MouseEvent event) {
		 xOffset = event.getSceneX();
		 yOffset = event.getSceneY();
	 }

	 @FXML protected void drag2(MouseEvent event) {
		 Main.startup.setX(event.getScreenX() - xOffset);
		 Main.startup.setY(event.getScreenY() - yOffset);
	 }
	 
	 @FXML protected void restoreFromSeed(ActionEvent event) {
		 Animation ani = GuiUtils.fadeOut(MainPane);
		 GuiUtils.fadeIn(MainRestorePane);
		 MainPane.setVisible(false);
		 MainRestorePane.setVisible(true);
	 }
	 
	 @FXML protected void newWallet(ActionEvent event) throws IOException {

		 createWallet(null);
		 createAuthenticatorObject();
		 
		 // update params in main
		 Main.returnedParamsFromSetup = appParams;
		 
		 for (String word : walletSeed.getMnemonicCode()){mnemonic = mnemonic + word + " ";}
		 lblSeed.setText(mnemonic);
		 final ContextMenu contextMenu = new ContextMenu();
		 MenuItem item1 = new MenuItem("Copy");
		 item1.setOnAction(new EventHandler<ActionEvent>() {
			 public void handle(ActionEvent e) {
				 Clipboard clipboard = Clipboard.getSystemClipboard();
				 ClipboardContent content = new ClipboardContent();
				 content.putString(lblSeed.getText().toString());
				 clipboard.setContent(content);
			 }
		 });
		 contextMenu.getItems().addAll(item1);
		 lblSeed.setContextMenu(contextMenu);
		 
		 Animation ani = GuiUtils.fadeOut(MainPane);
		 GuiUtils.fadeIn(CreateAccountPane);
		 MainPane.setVisible(false);
		 CreateAccountPane.setVisible(true);
	 }
	
	 
	 @FXML protected void toMainPane(ActionEvent event) {
		 Animation ani = GuiUtils.fadeOut(MainRestorePane);
		 Animation ani2 = GuiUtils.fadeOut(CreateAccountPane);
		 GuiUtils.fadeIn(MainPane);
		 CreateAccountPane.setVisible(false);
		 MainRestorePane.setVisible(false);
		 MainPane.setVisible(true);
	 }
	 
	 @FXML protected void toCreateAccountPane(ActionEvent event) {
		 Animation ani = GuiUtils.fadeOut(BackupNewWalletPane);
		 GuiUtils.fadeIn(CreateAccountPane);
		 BackupNewWalletPane.setVisible(false);
		 CreateAccountPane.setVisible(true);
	 }
	 
	 @FXML protected void toExplanationPane1(ActionEvent event) {
		 if (ckSeed.isSelected()){
			 displayExplanationPane();
		 }
	 }
	 
	 private void displayExplanationPane(){
		 Animation ani = GuiUtils.fadeOut(BackupNewWalletPane);
		 GuiUtils.fadeIn(ExplanationPane1);
		 BackupNewWalletPane.setVisible(false);
		 ExplanationPane1.setVisible(true);
	 }
	 
	 @FXML protected void finished(ActionEvent event){
		 hlFinished.setDisable(true);
		 auth.addListener(new Service.Listener() {
				@Override public void terminated(State from) {
					 Platform.runLater(() -> {
						 	auth.disposeOfAuthenticator();
							Main.startup.hide();
							Main.stage.show();
							if(encryptionPassword != null && encryptionPassword.length() > 0)
								wallet.encrypt(encryptionPassword);
							try {
								Main.finishLoading();
							} catch (IOException | AccountWasNotFoundException e) { e.printStackTrace(); }
					 });
		         }
			}, MoreExecutors.sameThreadExecutor());
         auth.stopAsync();
		 
	 }
	 
	 @FXML protected void openPlayStore(ActionEvent event) throws IOException{
		 String url = "https://play.google.com/";
         java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
	 }
	 
	 @FXML protected void baNext(ActionEvent event){
		 if (BA1.isVisible()){
			 Animation ani = GuiUtils.fadeOut(BA1);
			 GuiUtils.fadeIn(BA2);
			 BA1.setVisible(false);
			 BA2.setVisible(true);
		 } else if (BA2.isVisible()){
			 Animation ani = GuiUtils.fadeOut(BA2);
			 GuiUtils.fadeIn(BA3);
			 BA2.setVisible(false);
			 BA3.setVisible(true);
		 } else if (BA3.isVisible()){
			 Animation ani = GuiUtils.fadeOut(BA3);
			 GuiUtils.fadeIn(BA4);
			 BA3.setVisible(false);
			 BA4.setVisible(true);
			 Animation ani2 = GuiUtils.fadeOut(btnContinue3);
			 GuiUtils.fadeIn(hlFinished);
			 btnContinue3.setVisible(false);
			 hlFinished.setVisible(true);
		 } 
	 }
	 
	 @FXML protected void backToBackupNewWalletPane(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(ExplanationPane1);
		 GuiUtils.fadeIn(BackupNewWalletPane);
		 ExplanationPane1.setVisible(false);
		 BackupNewWalletPane.setVisible(true);
	 }
	 
	 @FXML protected void toBackupNewWalletPane(ActionEvent event) {
		 if (txAccount.getText().toString().equals("")){
			 informationalAlert("Unfortunately, you messed up.",
					 "You need to enter a name for your account");
		 }
		 else if (!txPW1.getText().toString().equals(txPW2.getText().toString())){
			 informationalAlert("Unfortunately, you messed up.",
					 "Your passwords don't match");
		 }
		 else if (txPW1.getText().toString().equals("") && txPW2.getText().toString().equals("")){
			 informationalAlert("Unfortunately, you messed up.",
					 "You need to enter a password");
		 }
		 else {
			 try {
				encryptionPassword =  txPW2.getText();
				 
				createNewStandardAccount(txAccount.getText());
				
				ckSeed.selectedProperty().addListener(new ChangeListener<Boolean>() {
					 public void changed(ObservableValue<? extends Boolean> ov,
							 Boolean old_val, Boolean new_val) {
						 if (ckSeed.isSelected()){
							 btnContinue2.setStyle("-fx-background-color: #95d946;");
						 }
						 else {
							 btnContinue2.setStyle("-fx-background-color: #badb93;");
						 }
					 }
				 });
				 Animation ani = GuiUtils.fadeOut(CreateAccountPane);
				 GuiUtils.fadeIn(BackupNewWalletPane);
				 CreateAccountPane.setVisible(false);
				 BackupNewWalletPane.setVisible(true);
				 //Main.bitcoin.wallet().encrypt(txPW1.getText().toString());
			} catch (Exception e) { 
				e.printStackTrace();
				
				Dialogs.create()
		        .owner(Main.startup)
		        .title("Error")
		        .masthead("Cannot Create account !")
		        .message("Please try again")
		        .showError();
			}
			 
		 }
	 }
	 
	 @FXML protected void saveWallet(ActionEvent event) throws IOException{
		 String filepath = new java.io.File( "." ).getCanonicalPath() + "/" + appParams.getAppName() + ".wallet";
		 File wallet = new File(filepath);
		 FileChooser fileChooser = new FileChooser();
		 fileChooser.setTitle("Save Wallet");
		 fileChooser.setInitialFileName(appParams.getAppName() + ".wallet");
		 File file = fileChooser.showSaveDialog(Main.startup);
		 FileChannel source = null;
		 FileChannel destination = null;
		 if (file != null) {
			 try {
				 source = new FileInputStream(wallet).getChannel();
				 destination = new FileOutputStream(file).getChannel();
				 destination.transferFrom(source, 0, source.size());
			 }	
			 finally {
				 if(source != null) {
					 source.close();
				 }
				 if(destination != null) {
					 destination.close();
				 }
			 }	 
		 }
	 }
	 
	 @FXML protected void printPaperWallet(ActionEvent event) throws IOException{
		 PaperWalletController c = new PaperWalletController();
		 c.createPaperWallet(mnemonic, walletSeed, walletSeed.getCreationTimeSeconds());
	 }
	 
	 @FXML protected void openSSS(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(BackupNewWalletPane);
		 GuiUtils.fadeIn(SSSBackupPane);
		 BackupNewWalletPane.setVisible(false);
		 SSSBackupPane.setVisible(true); 
	 }
	 
	 @FXML protected void split(ActionEvent event) throws IOException{
		 BipSSS sss = new BipSSS();
		 List<Share> shares = sss.shard(EncodingUtils.hexStringToByteArray(walletSeed.toHexString()), 
				 Integer.parseInt(txThreshold.getText().toString()), Integer.parseInt(txPieces.getText().toString()), EncodingFormat.SHORT, params);
		 final ObservableList list = FXCollections.observableArrayList();
		 for (Share share: shares){list.add(share.toString());}
		 lvSSS.setItems(list);
		 
	 }
	 
	 @FXML protected void returntoBackupNewWalletPane(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(SSSBackupPane);
		 GuiUtils.fadeIn(BackupNewWalletPane);
		 SSSBackupPane.setVisible(false);
		 BackupNewWalletPane.setVisible(true);
	 }
	 
	 @FXML protected void openWeb(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(CreateAccountPane);
		 GuiUtils.fadeIn(Pane6);
		 CreateAccountPane.setVisible(false);
		 Pane6.setVisible(true);
		 browser.autosize();
		 URL location = Main.class.getResource("passwords.html");
		 browser.getEngine().load(location.toString());
	 }
	 
	 @FXML protected void webFinished(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(Pane6);
		 GuiUtils.fadeIn(CreateAccountPane);
		 Pane6.setVisible(false);
		 CreateAccountPane.setVisible(true);
	 }
	 
	 @FXML protected void btnBackPressed(MouseEvent event) {
		 btnBack1.setStyle("-fx-background-color: #d7d4d4;");
		 btnBack2.setStyle("-fx-background-color: #d7d4d4;");
		 btnBack3.setStyle("-fx-background-color: #d7d4d4;");
		 btnBack4.setStyle("-fx-background-color: #d7d4d4;");
		 btnBack5.setStyle("-fx-background-color: #d7d4d4;");
	 }
	    
	 @FXML protected void btnBackReleased(MouseEvent event) {
		 btnBack1.setStyle("-fx-background-color: #808080;");
		 btnBack2.setStyle("-fx-background-color: #808080;");
		 btnBack3.setStyle("-fx-background-color: #808080;");
		 btnBack4.setStyle("-fx-background-color: #808080;");
		 btnBack5.setStyle("-fx-background-color: #808080;");
	 }
	    
	 @FXML protected void btnContinuePressed(MouseEvent event) {
		 btnContinue1.setStyle("-fx-background-color: #badb93;");
		 if (ckSeed.isSelected()){btnContinue2.setStyle("-fx-background-color: #badb93;");}
		 btnContinue3.setStyle("-fx-background-color: #badb93;");
		 btnDone.setStyle("-fx-background-color: #badb93; -fx-text-fill: white;");
	 }
	    
	 @FXML protected void btnContinueReleased(MouseEvent event) {
		 btnContinue1.setStyle("-fx-background-color: #95d946;");
		 if (ckSeed.isSelected()){btnContinue2.setStyle("-fx-background-color: #95d946;");}
		 btnContinue3.setStyle("-fx-background-color: #95d946;");
		 btnDone.setStyle("-fx-background-color: #95d946; -fx-text-fill: white;");
	 } 
	 
	//##############################
	 //
	 //		Backup
	 //
	 //##############################
	 
	 private boolean backupMode;
	 public void setBackMode(DeterministicSeed seed){
		 backupMode = true;
		 
		 walletSeed = seed;
		 List<String> mnemoniclst = walletSeed.getMnemonicCode();
		 mnemonic = Joiner.on(" ").join(mnemoniclst);
		 lblSeed.setText(mnemonic);
		 
		 MainPane.setVisible(false);
		 CreateAccountPane.setVisible(false);
		 BackupNewWalletPane.setVisible(true);
		 
		 btnBack2.setVisible(false);
		 btnContinue2.setVisible(false);
		 ckSeed.setVisible(false);
	 }
	 
	 //##############################
	 //
	 //		Restore from seed
	 //
	 //##############################
	 
	 @FXML protected void btnRestoreMnemonic(ActionEvent event){
		 MainRestorePane.setVisible(false);
		 RestoreFromMnemonicPane.setVisible(true);
	 }
	 
	 @FXML protected void returnFromSeedRestore(ActionEvent event){
		 MainRestorePane.setVisible(true);
		 RestoreFromMnemonicPane.setVisible(false);
	 }
	 
	 @FXML protected void goRestoreFromSeed(ActionEvent event){
		 DeterministicSeed seed = reconstructSeed();
		 if(seed != null){
			 try {
				createWallet(seed);
				RestoreFromMnemonicPane.setVisible(false);
				launchRestoreAccoutns(RestoreFromMnemonicPane);
			} catch (IOException e) {
				e.printStackTrace();
				Dialogs.create()
		        .owner(Main.startup)
		        .title("Error")
		        .masthead("Cannot Restore from Mnemonic Seed")
		        .message("Please try again")
		        .showError();
			}
		 }
		 else
			 Dialogs.create()
		        .owner(Main.startup)
		        .title("Error")
		        .masthead("Cannot Restore from Mnemonic Seed")
		        .message("Please try again")
		        .showError();
	 }
	 
	 private DeterministicSeed reconstructSeed(){
		 String mnemonicStr = lblSeedRestorer.getText();
		 List<String>mnemonicArr = Lists.newArrayList(Splitter.on(" ").split(mnemonicStr));
		 LocalDate date = seedCreationDatePicker.getValue();
		 long unix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
		 
		 return new DeterministicSeed(mnemonicArr, "", unix);//reconstructSeedFromStringMnemonics(mnemonicArr, unix);
	 }
	 	 
	//##############################
	 //
	 //		Restore from QR 
	 //
	 //##############################
	 
	 @FXML protected void btnRestoreQR(ActionEvent event){
		 MainRestorePane.setVisible(false);
		 RestoreFromQRPane.setVisible(true);
		 btnRestoreFromSeedFromQRContinue.setDisable(true);
	 }
	 
	 @FXML protected void returnFromQRRestore(ActionEvent event){
		 MainRestorePane.setVisible(true);
		 RestoreFromQRPane.setVisible(false);
	 }
	 
	 Webcam webcam;
	 JFrame camFrame;
	 @FXML protected void startQRScan(ActionEvent event){
		 btnStartWebCamQRScan.setText("Starting...");
		 new Thread(){
			 @Override
             public void run() {
				 camFrame  = new JFrame();
				 
				 Dimension size = WebcamResolution.QVGA.getSize();
				 webcam = Webcam.getDefault();
				 webcam.setViewSize(size);
				 WebcamPanel panel = new WebcamPanel(webcam);
				 
				 Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                	camFrame.add(panel);
	                	camFrame.pack();
	                	
	                	camFrame.setVisible(true);
	                	camFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	                	    @Override
	                	    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
	                	    	webcam.close();
	                	    }
	                	});
	                	btnStartWebCamQRScan.setText("");
	                }
	            });
				 
				tryAndReadQR(webcam);
			 }
		 }.start();
		 
	 }
	 
	 private void tryAndReadQR(Webcam webcam){
		 String qrDataString;
		 do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Result result = null;
				BufferedImage image = null;

				if (webcam.isOpen()) {

					if ((image = webcam.getImage()) == null) {
						continue;
					}

					LuminanceSource source = new BufferedImageLuminanceSource(image);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

					try {
						result = new MultiFormatReader().decode(bitmap);
					} catch (NotFoundException e) {
						// fall thru, it means there is no QR code in image
					}
				}

				if (result != null) {
					 LOG.info("Data from QR {}",result.getText());
					 qrDataString = result.getText();
					 Platform.runLater(new Runnable() {
			                @Override
			                public void run() {
			                	btnStartWebCamQRScan.setText("Processing ..");
			                	
			                }
					 });
					webcam.close();
	                camFrame.setVisible(false);
					break;
				}

			} while (true);
		 
		 PaperWalletQR paperWalletQR = new PaperWalletQR();
		 PaperWalletQR.SeedQRData data = paperWalletQR.parseSeedQR(qrDataString);
		 walletSeed = data.seed;
		 mnemonic = Joiner.on(" ").join(walletSeed.getMnemonicCode());
		 
		 Platform.runLater(new Runnable() {
             @Override
             public void run() {
             	btnStartWebCamQRScan.setText("");
             	lblSeedFromQR.setText(mnemonic);
             	btnRestoreFromSeedFromQRContinue.setDisable(false);
             }
		 });
	 }
	 
	 @FXML protected void goRestoreFromQR(ActionEvent event){
		 if(walletSeed != null){
			 try {
				createWallet(walletSeed);
				RestoreFromQRPane.setVisible(false);
				launchRestoreAccoutns(RestoreFromMnemonicPane);
			} catch (IOException e) {
				e.printStackTrace();
				Dialogs.create()
		        .owner(Main.startup)
		        .title("Error")
		        .masthead("Cannot Restore from Mnemonic Seed")
		        .message("Please try again")
		        .showError();
			}
		 }
		 else
			 Dialogs.create()
		        .owner(Main.startup)
		        .title("Error")
		        .masthead("Cannot Restore from Mnemonic Seed")
		        .message("Please try again")
		        .showError();
	 }
	 
	 
	 //##############################
	 //
	 //		Restore accounts 
	 //
	 //##############################
	 
	 @FXML protected void goRestoreAccounts(ActionEvent event){
		 launchRestoreProcess();
	 }
	 
	 
	 @FXML protected void returnToBeforeRestoreAccounts(ActionEvent event){
		 previousNode.setVisible(true);
		 RestoreAccountsPane.setVisible(false);
	 }
	 
	 @FXML protected void addAccount(ActionEvent event){
		 WalletAccountType type;
		 if(accountTypeBox.getValue().toString().equals("Standard Account"))
			 type = WalletAccountType.StandardAccount;
		 else
			 type = WalletAccountType.AuthenticatorAccount;
		 AddAccountWindow w = new AddAccountWindow(type, new AddAccountListener(){
			@Override
			public void addedAccount(AddedAccountObject acc) {
				RestoreAccountCell cell = new RestoreAccountCell(type, new AccountCellListener(){
					@Override
					public void close(RestoreAccountCell cell) {
						try {
							auth.getWalletOperation().removeAccount(cell.getAccountID());
							restoreAccountsScrllContent.removeNode(cell);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
				});
				cell.setAccountTypeName(accountTypeBox.getValue().toString());
				cell.setAccountID(Integer.toString(acc.accountAccountID));
				cell.setAccountName(acc.accountName);
				restoreAccountsScrllContent.addItem(cell);
				
				try {
					if(type == WalletAccountType.StandardAccount){
						ATAccount newAcc = auth.getWalletOperation().completeAccountObject(appParams.getBitcoinNetworkType(),
								acc.accountAccountID, 
								acc.accountName, 
								WalletAccountType.StandardAccount);
						auth.getWalletOperation().addNewAccountToConfigAndHierarchy(newAcc);
					}
					else
						;/**
						 * Authenticator account is created in the pairing operation
						 */
					

					if(restoreAccountsScrllContent.getCount() == 1)
						auth.getWalletOperation().setActiveAccount(acc.accountAccountID);
					
				} catch (IOException | AccountWasNotFoundException e) {
					e.printStackTrace();
				}
			}
		 });
		 w.show();
	 }
	 
	 private Node previousNode;
	 private void launchRestoreAccoutns(Node node){
		 createAuthenticatorObject();
		 auth.getWalletOperation().setTrackedWallet(wallet);
		 
		 node.setVisible(false);
		 previousNode = node;
		 LoadingPane.setVisible(true);
		 auth.startAsync();
		 auth.addListener(new Service.Listener() {
				@Override public void running() {
					Platform.runLater(new Runnable() { 
						  @Override
						  public void run() {
							  RestoreAccountsPane.setVisible(true);
							  LoadingPane.setVisible(false);
						  }
					});
		         }
			}, MoreExecutors.sameThreadExecutor());
	 }
	 
	 public static interface AddAccountListener{
		 public void addedAccount(AddedAccountObject acc);
	 }
	 
	 public static class AddedAccountObject{
		 WalletAccountType type;
		 String accountName;
		 int accountAccountID;
		 
		 public AddedAccountObject(WalletAccountType type, String accountName, int accountAccountID){
			 this.type = type;
			 this.accountName = accountName;
			 this.accountAccountID = accountAccountID;
		 }
	 }
	 
	 //##############################
	 //
	 //		Restore process 
	 //
	 //##############################
	 
	 private BAWalletRestorer restorer;
	 private void launchRestoreProcess(){
		 RestoreProcessPane.setVisible(true);
		 btnFinishRestoreProcess.setDisable(true);
		 RestoreAccountsPane.setVisible(false);
		 syncProgress.setProgress(0);
		 restorer = new BAWalletRestorer(auth, new WalletRestoreListener(){
			@Override
			public void onProgress(double pct, int blocksSoFar, Date date) {
				float completion = (float) (pct / 100.0);
				Platform.runLater(() -> syncProgress.setProgress(completion));
			}

			@Override
			public void onTxFound(Transaction Tx, Coin received, Coin sent) {
				addFoundTxFromRestore(Tx, received, sent);
			}

			@Override
			public void onStatusChange(String newStatus) {
				Platform.runLater(() -> lblRestoreProcessStatus.setText(newStatus));
			}

			@Override
			public void onDiscoveryDone() {
				Platform.runLater(() -> btnFinishRestoreProcess.setDisable(false));
			}
			 
		 });
		 restorer.startAsync();
	 }
	 
	 private void addFoundTxFromRestore(Transaction tx, Coin received, Coin sent){
		 Platform.runLater(() -> {
			 RestoreProcessCell c = new RestoreProcessCell();
			 c.setTxID(tx.getHashAsString() + "(" + tx.getUpdateTime().toGMTString() + ")");
			 c.setCoinsReceived(received.toFriendlyString());
			 c.setCoinsSent(sent.toFriendlyString());			
			 c.setConfidence(tx.getConfidence().getConfidenceType().toString());
			 String status = tx.isEveryOwnedOutputSpent(wallet)? "Spent":"At least one output is unspent";
			 c.setStatus(status);
			 restoreProcessScrllContent.addItem(c);
		 });
	 }
	 
	 @FXML protected void returnBackFromRestoreProcess(ActionEvent event){
		 restorer.stopAsync();
		 restorer.addListener(new Service.Listener(){
	        	@Override public void terminated(State from) {
	        		Platform.runLater(() -> {
	        			RestoreProcessPane.setVisible(false);
		       		 	RestoreAccountsPane.setVisible(true);
	        		});
	        	}
	        }, MoreExecutors.sameThreadExecutor());
	 }
	  
	 @FXML protected void finishRestoreProcess(ActionEvent event){
		 RestoreProcessPane.setVisible(false);
		 displayExplanationPane();
	 }
	 
	//##############################
	 //
	 //		Wallet specific methods 
	 //
	 //##############################
	 
	 private void createWallet(@Nullable DeterministicSeed seed) throws IOException{
		 String filePath = new java.io.File( "." ).getCanonicalPath() + "/" + appParams.getAppName() + ".wallet";
		 File f = new File(filePath);
		 assert(f.exists() == false);
		 if(seed == null){
			//Generate a new Seed
			 SecureRandom secureRandom = null;
			 try {secureRandom = SecureRandom.getInstance("SHA1PRNG");} 
			 catch (NoSuchAlgorithmException e) {e.printStackTrace();}
			 //byte[] bytes = new byte[16];
			 //secureRandom.nextBytes(bytes);
			 walletSeed = new DeterministicSeed(secureRandom, 8 * 16, "", Utils.currentTimeSeconds());
		 }
		 else
			 walletSeed = seed;
		  
		 // set wallet
		 wallet = Wallet.fromSeed(params,walletSeed);
		 wallet.setKeychainLookaheadSize(0);
		 wallet.autosaveToFile(f, 200, TimeUnit.MILLISECONDS, null);
	 }
	 
	private void createAuthenticatorObject(){
		// create master public key
		DeterministicKey masterPubKey = HDKeyDerivation.createMasterPrivateKey(walletSeed.getSecretBytes()).getPubOnly();
	    // set Authenticator wallet
		auth = new Authenticator(masterPubKey, appParams);
	}
	 
	 private void createNewStandardAccount(String accountName) throws IOException, AccountWasNotFoundException{
		 ATAccount acc = auth.getWalletOperation().generateNewStandardAccount(appParams.getBitcoinNetworkType(), accountName);
		 auth.getWalletOperation().setActiveAccount(acc.getIndex());
	 }
	 
	 /*private DeterministicSeed reconstructSeedFromStringMnemonics(List<String> mnemonic, long creationTimeSeconds){
		 if(validateSeedInputs(mnemonic, creationTimeSeconds) == false)
			 return null;
		 byte[] seed = MnemonicCode.toSeed(mnemonic, "");
		 DeterministicSeed deterministicSeed = new DeterministicSeed(seed, "", creationTimeSeconds);
		 return deterministicSeed;
	 }*/
	 
	 private boolean validateSeedInputs(List<String> mnemonic, long creationTimeSeconds){
		 if (mnemonic.size() <= 10)
			 return false;
		for(String word:mnemonic) 
			if(word.length() <= 1 ||
				word.equals(" ") == true)
				return false;
		if(creationTimeSeconds <= 1388534400) // 1.1.2014 00:00:00
			return false;
		
		return true;
	 }
	 
	 
	 
	 
	 
	 
	 
	 private void handleExitButtonPressed(){
		 if(!backupMode){
			 com.sun.javafx.application.PlatformImpl.tkExit();
			 Platform.exit();
		 }
		 else
			 SettingsController.backupPane.close();
	 }
	
}
