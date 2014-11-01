package wallettemplate.startup;

import static org.bitcoinj.core.Utils.HEX;
import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

import wallettemplate.Main;
import wallettemplate.SettingsController;
import wallettemplate.PairWallet.PairingWalletControllerListener;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.startup.RestoreAccountCell.AccountCellListener;
import wallettemplate.startup.backup.PaperWalletController;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.FileUtils;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.ImageUtils;
import wallettemplate.utils.dialogs.BADialog;
import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.BipSSS.BipSSS;
import authenticator.BipSSS.BipSSS.EncodingFormat;
import authenticator.BipSSS.BipSSS.IncompatibleSharesException;
import authenticator.BipSSS.BipSSS.InvalidContentTypeException;
import authenticator.BipSSS.BipSSS.NotEnoughSharesException;
import authenticator.BipSSS.BipSSS.Share;
import authenticator.Utils.EncodingUtils;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.listeners.BAGeneralEventsAdapter;
import authenticator.network.BANetworkInfo;
import authenticator.operations.BAOperation;
import authenticator.operations.BAWalletRestorer;
import authenticator.operations.OperationsFactory;
import authenticator.operations.BAWalletRestorer.WalletRestoreListener;
import authenticator.operations.OperationsUtils.PairingQRCode;
import authenticator.operations.OperationsUtils.PaperWalletQR;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStage;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStageUpdater;
import authenticator.operations.listeners.OperationListenerAdapter;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;
import authenticator.walletCore.exceptions.NoWalletPasswordException;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.WalletProtobufSerializer;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Protos;

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
import com.google.zxing.WriterException;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
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
	@FXML private Pane SetPasswordPane;
	@FXML private Pane BackupNewWalletPane;
	@FXML private Pane ExplanationPane1;
	@FXML private Pane Pane6;
	@FXML private Pane SSSBackupPane;
	@FXML private Pane MainRestorePane;
	@FXML private Pane RestoreFromMnemonicPane;
	@FXML private Pane RestoreFromQRPane;
	@FXML private Pane RestoreFromSSSPane;
	@FXML private Pane RestoreFromSSSDatePane;
	@FXML private Pane RestoreProcessPane;
	@FXML private Pane RestoreAccountsPane;
	@FXML private Pane SetPasswordAfterRestorePane;
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
	@FXML private Button btnBackFromSeedFromSSSRestore;
	@FXML private Button btnRestoreFromSeedFromSSSContinue;
	@FXML private Button btnBackFromSeedFromSSSRestoreDatePicker;
	@FXML private Button btnRestoreFromSeedFromSSSContinueDatePicker;
	@FXML private Button btnCancelRestoreProcess;
	@FXML private Button btnFinishRestoreProcess;
	@FXML private Button btnBackFromAccountRestore;
	@FXML private Button btnAccountRestoreContinue;
	@FXML private Button btnBackFromSetPasswordAfterRestore;
	@FXML private Button btnContinueAfterSetPasswordAfterRestore;
	@FXML private Button btnPlayStore;
	@FXML private Button btnStandard;
	@FXML private Label lblMinimize;
	@FXML private Label lblClose;
	@FXML private Button btnDone;
	@FXML private TextField txAccount;
	@FXML private PasswordField txPW1;
	@FXML private PasswordField txPW2;
	@FXML private PasswordField txRestorePW1;
	@FXML private PasswordField txRestorePW2;
	@FXML private Label lblSeed;
	@FXML private Label lbld1;
	@FXML private Label lbld2;
	@FXML private CheckBox ckSeed;
	@FXML private Button btnSave;
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
	@FXML private Label lblLoadginQR;
	@FXML private ImageView imgSSS;
	@FXML private Button btnSSS;
	@FXML private ProgressBar syncProgress;
	@FXML private Label lblRestoreProcessStatus;
	@FXML private Label lblScan;
	@FXML private Label lbl2fa;
	@FXML private TextField lblSeedRestorer;
	@FXML private DatePicker seedCreationDatePicker;
	
	@FXML private Label lblSeedFromQR;
	@FXML private Button btnStartWebCamQRScan;
	
	@FXML private TextField txPiecesSSSRestore;
	@FXML private TextField txThresholdSSSRestore;
	@FXML private ScrollPane scrlSSSRestoreShares;
	private ScrollPaneContentManager restoreSSSScrllContent;
	@FXML private Label lblSeedFromSSS;
	@FXML private Button btnRestoreSSSConbineShares;
	@FXML private DatePicker seedSSSRestoreCreationDatePicker;
	
	@FXML private ChoiceBox accountTypeBox;
	@FXML private Label lblLoading;
	@FXML private ScrollPane restoreProcessScrll;
	private ScrollPaneContentManager restoreProcessScrllContent;
	@FXML private ScrollPane restoreAccountsScrll;
	private ScrollPaneContentManager restoreAccountsScrllContent;
	private DeterministicSeed walletSeed;
	NetworkParameters params;
	Authenticator auth;
	Wallet wallet;
	
	/**
	 *  CreateAccountPane variables.
	 *  We keep them here because the user needs to decide at the end of the startup process at the end of
	 *  the explanation pane if he wants that the created account should be paired or standard.
	 */
	@FXML private ImageView   ivFirstAccountPairingQR;
	@FXML private Hyperlink hlFinished;
	private String 			  encryptionPassword = null;
	private String 			  firstAccountName;
	private WalletAccountType firstAccountType = WalletAccountType.StandardAccount;
	private boolean 		  shouldCreateNewAccountOnFinish = false;
	
	
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
		 Label labelackFromSeedSSSRestore = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelackFromSeedSSSRestore.setPadding(new Insets(0,6,0,0));
		 btnBackFromSeedFromSSSRestore.setGraphic(labelackFromSeedSSSRestore);
		 //
		 Label labeRestoreFromSeedSSSContinue = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeRestoreFromSeedSSSContinue.setPadding(new Insets(0,6,0,0));
		 btnRestoreFromSeedFromSSSContinue.setGraphic(labeRestoreFromSeedSSSContinue);
		 //
		 Label labelackFromSeedSSSRestoreDatePicker = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelackFromSeedSSSRestoreDatePicker.setPadding(new Insets(0,6,0,0));
		 btnBackFromSeedFromSSSRestoreDatePicker.setGraphic(labelackFromSeedSSSRestoreDatePicker);
		 //
		 Label labeRestoreFromSeedSSSContinueDatePicker = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeRestoreFromSeedSSSContinueDatePicker.setPadding(new Insets(0,6,0,0));
		 btnRestoreFromSeedFromSSSContinueDatePicker.setGraphic(labeRestoreFromSeedSSSContinueDatePicker);
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
		 Label labelBackFromSetPasswordAfterRestore = AwesomeDude.createIconLabel(AwesomeIcon.CARET_LEFT, "45");
		 labelBackFromSetPasswordAfterRestore.setPadding(new Insets(0,6,0,0));
		 btnBackFromSetPasswordAfterRestore.setGraphic(labelBackFromSetPasswordAfterRestore);
		 //
		 Label labeContinueAfterSetPasswordAfterRestore = AwesomeDude.createIconLabel(AwesomeIcon.CARET_RIGHT, "45");
		 labeContinueAfterSetPasswordAfterRestore.setPadding(new Insets(0,6,0,0));
		 btnContinueAfterSetPasswordAfterRestore.setGraphic(labeContinueAfterSetPasswordAfterRestore);
		 
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
		 
		 /**
		  * SSS Backup validators 
		  */
		 txPieces.lengthProperty().addListener(new ChangeListener<Number>(){
             @Override
             public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
            	 StartupControllerHelper.SSSValidator(txPieces, observable, oldValue, newValue);
             }
		 });
		 txThreshold.lengthProperty().addListener(new ChangeListener<Number>(){
             @Override
             public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
            	 StartupControllerHelper.SSSValidator(txThreshold, observable, oldValue, newValue);
             }
		 });
		 
		 /**
		  * SSS Restore validators and listeners
		  */
		 
		 txPiecesSSSRestore.lengthProperty().addListener(new ChangeListener<Number>(){
             @Override
             public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
            	 StartupControllerHelper.SSSValidator(txPiecesSSSRestore, observable, oldValue, newValue);
             }
		 });
		 txPiecesSSSRestore.focusedProperty().addListener(new ChangeListener<Boolean>()
		 {
 		    @Override
 		    public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
 		    {
 		    	if(txThresholdSSSRestore.getText().length() > 0)
 		    		createSSSRestorePieces(Integer.parseInt(txThresholdSSSRestore.getText()));
 		    }
		 });
		 txThresholdSSSRestore.lengthProperty().addListener(new ChangeListener<Number>(){
             @Override
             public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
            	 StartupControllerHelper.SSSValidator(txThresholdSSSRestore, observable, oldValue, newValue);
             }
		 });
		 txThresholdSSSRestore.focusedProperty().addListener(new ChangeListener<Boolean>()
		 {
 		    @Override
 		    public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
 		    {
 		    	if(txThresholdSSSRestore.getText().length() > 0)
 		    		createSSSRestorePieces(Integer.parseInt(txThresholdSSSRestore.getText()));
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
		 
		 // seed mnemonic context menu
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
		 
		 shouldCreateNewAccountOnFinish = true;
		 
		 // update params in main
		 Main.returnedParamsFromSetup = appParams;
		 
		 for (String word : walletSeed.getMnemonicCode()){mnemonic = mnemonic + word + " ";}
		 lblSeed.setText(mnemonic);
		 
		 Animation ani = GuiUtils.fadeOut(MainPane);
		 GuiUtils.fadeIn(SetPasswordPane);
		 MainPane.setVisible(false);
		 SetPasswordPane.setVisible(true);
	 }
	
	 
	 @FXML protected void toMainPane(ActionEvent event) {
		 shouldCreateNewAccountOnFinish = false;
		 Animation ani = GuiUtils.fadeOut(MainRestorePane);
		 Animation ani2 = GuiUtils.fadeOut(SetPasswordPane);
		 GuiUtils.fadeIn(MainPane);
		 SetPasswordPane.setVisible(false);
		 MainRestorePane.setVisible(false);
		 MainPane.setVisible(true);
	 }
	 
	 @FXML protected void toCreateAccountPane(ActionEvent event) {
		 Animation ani = GuiUtils.fadeOut(BackupNewWalletPane);
		 GuiUtils.fadeIn(SetPasswordPane);
		 BackupNewWalletPane.setVisible(false);
		 SetPasswordPane.setVisible(true);
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
		 
		 hlFinished.setDisable(true);
		 		 
		 auth.getWalletOperation().setTrackedWallet(wallet);
		 auth.startAsync();
		 auth.addListener(new Service.Listener() {
				@Override public void running() {
					Platform.runLater(new Runnable() { 
						  @Override
						  public void run() {							  
							//prepare pairing
							 playPairingOperation(firstAccountName, 
									 auth.getApplicationParams().getBitcoinNetworkType(), 
									 new PairingStageUpdater(){
										@Override
										public void onPairingStageChanged(PairingStage stage, @Nullable byte[] qrImageBytes) {
											if(stage == PairingStage.FINISHED){
												firstAccountType = WalletAccountType.AuthenticatorAccount;
												finishsetup();
											}
											else if(stage == PairingStage.FAILED) {
												Platform.runLater(() -> GuiUtils.informationalAlert("Error !", "We could not create the pairing QR code, please restart wallet."));
											}
											else if(stage == PairingStage.WAITING_FOR_SCAN) {
												if(qrImageBytes != null && qrImageBytes.length > 0) {
													try {
														Image qrImg = ImageUtils.javaFXImageFromBytes(qrImageBytes);
														Platform.runLater(() -> {
															ivFirstAccountPairingQR.setImage(qrImg);
															hlFinished.setDisable(false);
														});
													} catch (IOException e) {
														e.printStackTrace();
														Platform.runLater(() -> {
															BADialog.info(Main.class, "Error!", "Could not display QR code").show();
														} );
													}
												}
												else
													Platform.runLater(() -> {
														BADialog.info(Main.class, "Error!", "Could not display QR code").show();
													} );
												
											}
										}

										@Override
										public void pairingData(PairedAuthenticator data) {
											try {
												auth.getWalletOperation().setActiveAccount(data.getWalletAccountIndex());
											} catch (AccountWasNotFoundException e) { e.printStackTrace(); }
										}
							 });
						  }
					});
		         }
			}, MoreExecutors.sameThreadExecutor());
		 
	 }
	 
	 @FXML protected void finished(ActionEvent event){
		 hlFinished.setDisable(true);
		 finishsetup();
	 }
	 
	public void finishsetup(){
		 
		if(shouldCreateNewAccountOnFinish) {
			// create the first account
			 if(firstAccountType  == WalletAccountType.StandardAccount)
				try {
					auth.Net().INTERRUPT_CURRENT_OUTBOUND_OPERATION();
					createNewStandardAccount(firstAccountName);
				} catch (IOException | AccountWasNotFoundException | NoWalletPasswordException e1) { e1.printStackTrace(); }
			 else
			 {
				 // do nothing
			 }
			 
		}
		 
		if(auth != null) {
			auth.addListener(new Service.Listener() {
				@Override public void terminated(State from) {
					 Platform.runLater(() -> {
						 	auth.disposeOfAuthenticator();
							Main.startup.hide();
							Main.stage.show();
							if(encryptionPassword != null && encryptionPassword.length() > 0)
								wallet.encrypt(encryptionPassword);
							Main.finishLoading();
					 });
			     }
			}, MoreExecutors.sameThreadExecutor());
			auth.stopAsync();
		}
		else {
			Platform.runLater(() -> {
				Main.startup.hide();
				Main.stage.show();
				if(encryptionPassword != null && encryptionPassword.length() > 0)
					wallet.encrypt(encryptionPassword);
				Main.finishLoading();
			});
		}
		 	
	 }
	 
	 @FXML protected void openPlayStore(ActionEvent event) throws IOException{
		 String url = "https://play.google.com/store/apps/details?id=org.bitcoin.authenticator";
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
			 ivFirstAccountPairingQR.setVisible(true);
			 Animation ani2 = GuiUtils.fadeOut(btnContinue3);
			 GuiUtils.fadeIn(btnStandard);
			 btnStandard.setVisible(true);
			 lbl2fa.setText("Choose Account Type");
			 //hlFinished.setVisible(true);
			 btnContinue3.setVisible(false);
			 //lblScan.setVisible(true);
			 lbld1.setVisible(true);
			 lbld2.setVisible(true);
			 btnPlayStore.setVisible(false);
			 btnBack5.setDisable(true);
			 lblLoadginQR.setVisible(true);
		 } 
	 }
	 
	 @FXML protected void backToBackupNewWalletPane(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(ExplanationPane1);
		 GuiUtils.fadeIn(BackupNewWalletPane);
		 ExplanationPane1.setVisible(false);
		 BackupNewWalletPane.setVisible(true);
	 }
	 	 
	 private boolean handlePasswordPane(String pw1, String pw2) {
		 if (pw1.equals("") || pw2.equals("")){
			 informationalAlert("Unfortunately, you messed up.",
					 "You need to enter a password");
		 }
		 else if (!pw1.equals(pw2)){
			 informationalAlert("Unfortunately, you messed up.",
					 "Your passwords don't match");
		 }
		 else {
			 encryptionPassword =  pw2;
			 return true;
		 }
		 
		 return false;
	 }
	 
	 @FXML protected void openWeb(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(SetPasswordPane);
		 GuiUtils.fadeIn(Pane6);
		 SetPasswordPane.setVisible(false);
		 Pane6.setVisible(true);
		 browser.autosize();
		 URL location = Main.class.getResource("passwords.html");
		 browser.getEngine().load(location.toString());
	 }
	 
	 @FXML protected void webFinished(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(Pane6);
		 GuiUtils.fadeIn(SetPasswordPane);
		 Pane6.setVisible(false);
		 SetPasswordPane.setVisible(true);
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
	 public void setBackupMode(Wallet wallet, DeterministicSeed seed){
		 backupMode = true;
		 
		 this.wallet= wallet;
		 walletSeed = seed;
		 List<String> mnemoniclst = walletSeed.getMnemonicCode();
		 mnemonic = Joiner.on(" ").join(mnemoniclst);
		 lblSeed.setText(mnemonic);
		 
		 MainPane.setVisible(false);
		 SetPasswordPane.setVisible(false);
		 BackupNewWalletPane.setVisible(true);
		 
		 btnBack2.setVisible(false);
		 btnContinue2.setVisible(false);
		 ckSeed.setVisible(false);
		 btnSave.setDisable(false);
	 }
	 
	 @FXML protected void toBackupNewWalletPane(ActionEvent event) {
		 if (txAccount.getText().toString().equals("")){
			 informationalAlert("Unfortunately, you messed up.",
					 "You need to enter a name for your account");
		 }
		 
		 if(handlePasswordPane(txPW1.getText(), txPW2.getText()))
		 {
			 try {
				firstAccountName = txAccount.getText();
				//createNewStandardAccount(txAccount.getText());
				
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
				 Animation ani = GuiUtils.fadeOut(SetPasswordPane);
				 GuiUtils.fadeIn(BackupNewWalletPane);
				 SetPasswordPane.setVisible(false);
				 BackupNewWalletPane.setVisible(true);
				 //Main.bitcoin.wallet().encrypt(txPW1.getText().toString());
			} catch (Exception e) { 
				e.printStackTrace();
				GuiUtils.informationalAlert("Cannot Create account !", "Please try again");
			}
			 
		 }
	 }
	 
	 @FXML protected void saveWalletFolderAsZip(ActionEvent event) throws IOException{
		 FileChooser fileChooser = new FileChooser();
		 fileChooser.setTitle("Save Wallet");
		 fileChooser.setInitialFileName(appParams.getAppName() + ".zip");
		 File destination = fileChooser.showSaveDialog(Main.startup);
		 File walletFolder = new File(appParams.getApplicationDataFolderAbsolutePath());
		 if(!walletFolder.isDirectory() || !walletFolder.exists()) {
			 Platform.runLater(() -> GuiUtils.informationalAlert("Error !", "Could not load wallet data directory"));
			 return;
		 }
		 
		 if(this.backupMode) {
			 if(FileUtils.ZipHelper.zipDir(walletFolder.getAbsolutePath(), destination.getAbsolutePath()))
				 Platform.runLater(() -> GuiUtils.informationalAlert("Success !", "Saved wallet files to:\n" + destination.getAbsolutePath()));
			 else {
				 Platform.runLater(() -> GuiUtils.informationalAlert("Error !", "Could not save wallet files"));
			 }
		 }
		 else {
			 Main.destination = destination;
			 Main.walletFolder = walletFolder;
			 GuiUtils.informationalAlert("Take note !", "The backup files will be saved to:\n" + destination.getAbsolutePath() + " after you complete the setup.");
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
	 
	 List<Share> shares;
	 byte[] mnemonicEntropy;
	 @FXML protected void split(ActionEvent event) throws IOException{
		 // get the mnemonic hex
		 String qrCodeData = null;
		 MnemonicCode ms = null;
		 try {
 		 	ms = new MnemonicCode();
 			List<String> mnemonic = walletSeed.getMnemonicCode();
 			mnemonicEntropy = ms.toEntropy(mnemonic);
 			
 			BipSSS sss = new BipSSS();
 			shares = sss.shard(mnemonicEntropy, 
 					 Integer.parseInt(txThreshold.getText().toString()), Integer.parseInt(txPieces.getText().toString()), EncodingFormat.SHORT, params);
 			final ObservableList list = FXCollections.observableArrayList();
 			for (Share share: shares){list.add(share.toString());}
 			lvSSS.setItems(list);
 		 } catch (Exception e) {
 			e.printStackTrace();
 	     }
		 
	 }
	 
	 @FXML protected void testSSS(ActionEvent event){
		 if(shares != null && shares.size() > 0 && mnemonicEntropy != null){
			 TestSSSWindow w = new TestSSSWindow(shares, 
					 mnemonicEntropy,
					 Integer.parseInt(txThreshold.getText().toString()));
			 w.show();
		 }
	 }
	 
	 @FXML protected void returntoBackupNewWalletPane(ActionEvent event){
		 Animation ani = GuiUtils.fadeOut(SSSBackupPane);
		 GuiUtils.fadeIn(BackupNewWalletPane);
		 SSSBackupPane.setVisible(false);
		 BackupNewWalletPane.setVisible(true);
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
		 DeterministicSeed seed = reconstructSeed(lblSeedRestorer.getText(), seedCreationDatePicker.getValue());
		 if(seed != null){
			 try {
				createWallet(seed);
				RestoreFromMnemonicPane.setVisible(false);
				launchRestoreAccoutns(RestoreFromMnemonicPane);
			} catch (IOException e) {
				e.printStackTrace();
				GuiUtils.informationalAlert("Cannot Restore from Mnemonic Seed", "Please try again");
			}
		 }
		 else
			 GuiUtils.informationalAlert("Cannot Restore from Mnemonic Seed", "Please try again");
	 }
	 
	 private DeterministicSeed reconstructSeed(String mnemonicStr, LocalDate date){
		 //String mnemonicStr = lblSeedRestorer.getText();
		 List<String>mnemonicArr = Lists.newArrayList(Splitter.on(" ").split(mnemonicStr));
		 //LocalDate date = seedCreationDatePicker.getValue();
		 long unix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
		 
		 return new DeterministicSeed(mnemonicArr, null,"", unix);//reconstructSeedFromStringMnemonics(mnemonicArr, unix);
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
				GuiUtils.informationalAlert("Cannot Restore from Mnemonic Seed", "Please try again");
			}
		 }
		 else
			 GuiUtils.informationalAlert("Cannot Restore from Mnemonic Seed", "Please try again");
	 }
	 
	 //##############################
	 //
	 //		Restore from SSS 
	 //
	 //##############################
	 
	 @FXML protected void btnRestoreSSS(ActionEvent event){
		 restoreSSSScrllContent = new ScrollPaneContentManager()
			.setSpacingBetweenItems(20)
			.setScrollStyle(scrlSSSRestoreShares.getStyle());
		 scrlSSSRestoreShares.setContent(restoreSSSScrllContent);
		 scrlSSSRestoreShares.setHbarPolicy(ScrollBarPolicy.NEVER);
		 
		 MainRestorePane.setVisible(false);
		 RestoreFromSSSPane.setVisible(true);
		 btnRestoreFromSeedFromSSSContinue.setDisable(true);
	 }
	 
	 @FXML protected void returnFromSSSRestore(ActionEvent event){
		 MainRestorePane.setVisible(true);
		 RestoreFromSSSPane.setVisible(false);
	 }
	 
	 @FXML protected void goRestoreFromSSS(ActionEvent event){
		 RestoreFromSSSPane.setVisible(false);
		 RestoreFromSSSDatePane.setVisible(true);
	 }
	 
	 @FXML protected void returnFromSSSRestoreDatePicker(ActionEvent event){
		 RestoreFromSSSPane.setVisible(true);
		 RestoreFromSSSDatePane.setVisible(false);
	 }
	 
	 @FXML protected void goRestoreFromSSSDatePicker(ActionEvent event){
		 LocalDate date = seedSSSRestoreCreationDatePicker.getValue();
		 if(date != null){
			 try {
				 long unix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
				 walletSeed = reconstructSeed(lblSeedFromSSS.getText(), date);
				 createWallet(walletSeed);
				 RestoreFromSSSDatePane.setVisible(false);		 
				 launchRestoreAccoutns(RestoreFromSSSDatePane);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		 }
		 else
		 {
			 GuiUtils.informationalAlert("Error", "Please Choose Seed Creation time");
		 }
	 }
	 
	 @FXML protected void combineSSSShares(ActionEvent event){
		 try {
			 List<Share> shares = new ArrayList<Share>();
			 for(Node n:restoreSSSScrllContent.getChildren()){
				 SSSRestoreCell c = (SSSRestoreCell)n;
				 String shareStr = c.getShareStr();
				 Share s = Share.fromString(shareStr, params);
				 shares.add(s);
			 }
			 
			 // combine shares
			 byte[] entropy = BipSSS.combineSeed(shares);
			 MnemonicCode ms = new MnemonicCode();
			 List<String> mnemonic = ms.toMnemonic(entropy);
			 String mnemonicStr = Joiner.on(" ").join(mnemonic);
			 lblSeedFromSSS.setText(mnemonicStr);
			 
			 btnRestoreFromSeedFromSSSContinue.setDisable(false);
			 
		  } catch (Exception e) {
			e.printStackTrace();
			GuiUtils.informationalAlert("Cannot Restore from SSS Shares", "Please make sure you typed the correct share strings");
		  }
	 }
	 
	 private void createSSSRestorePieces(int threshHold){
		 restoreSSSScrllContent.clearAll();
		 for(int i=1; i <= threshHold; i++)
		 {
			 SSSRestoreCell c = new SSSRestoreCell();
			 c.setLabel("Share #" + i);
			 restoreSSSScrllContent.addItem(c);
		 }
	 }
	 
	 //##############################
	 //
	 //		Restore from file 
	 //
	 //##############################
	 
	 @FXML protected void btnRestoreFromFile(ActionEvent event){
		 FileChooser fileChooser = new FileChooser();
		 fileChooser.setTitle("Save Wallet");
		 fileChooser.setInitialFileName(appParams.getAppName() + ".zip");
		 File file = fileChooser.showOpenDialog(Main.startup);
		 
		 if(FileUtils.ZipHelper.unZip(file.getAbsolutePath(), appParams.getApplicationDataFolderPath()))
			 finishsetup();
		 else
			 Platform.runLater(() -> GuiUtils.informationalAlert("Error !", "Could not restore from file"));
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
						ATAccountAddressHierarchy ext = auth.getWalletOperation().getAccountAddressHierarchy(acc.accountAccountID, 
								HierarchyAddressTypes.External, 
								null);
				 		ATAccountAddressHierarchy intr = auth.getWalletOperation().getAccountAddressHierarchy(acc.accountAccountID, 
				 				HierarchyAddressTypes.Internal, 
				 				null);
						
						ATAccount newAcc = auth.getWalletOperation().completeAccountObject(appParams.getBitcoinNetworkType(),
								acc.accountAccountID, 
								acc.accountName, 
								WalletAccountType.StandardAccount,
								ext,
								intr);
						auth.getWalletOperation().addNewAccountToConfigAndHierarchy(newAcc);
					}
					else
						;/**
						 * Authenticator account is created in the pairing operation
						 */
					

					if(restoreAccountsScrllContent.getCount() == 1)
						auth.getWalletOperation().setActiveAccount(acc.accountAccountID);
					
				} catch (IOException | AccountWasNotFoundException | NoWalletPasswordException e) {
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
		 
		 
		 
		 auth.startAsync();
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
	  
	 @FXML protected void toSetPasswordAfterResotre(ActionEvent event){
		 RestoreProcessPane.setVisible(false);
		 SetPasswordAfterRestorePane.setVisible(true);
	 }
	 
	//##############################
	 //
	 //	Set password after restore process 
	 //
	 //##############################
	 
	 @FXML protected void toRestoreProcess(ActionEvent event) {
		 RestoreProcessPane.setVisible(true);
		 SetPasswordAfterRestorePane.setVisible(false);
	 }
	 
	 @FXML protected void finishRestoreProcess(ActionEvent event){
		 SetPasswordAfterRestorePane.setVisible(false);
		 if(handlePasswordPane(txRestorePW1.getText(), txRestorePW2.getText()))
			 finishsetup();
	 }
	 
	//##############################
	 //
	 //		Wallet specific methods 
	 //
	 //##############################
	 
	 private void createWallet(@Nullable DeterministicSeed seed) throws IOException{
		 String filePath = appParams.getApplicationDataFolderAbsolutePath() + appParams.getAppName() + ".wallet";
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
		// add listener
		auth.addGeneralEventsListener(new BAGeneralEventsAdapter(){
			@Override
			public void onAuthenticatorNetworkStatusChange(BANetworkInfo info) {
				Platform.runLater(() -> {
					if(info.PORT_FORWARDED == false)
						BADialog.info(Main.class, "Warning !", "We were unable to map your port using Universal Plug and Play. If you plan to use this wallet with the Bitcoin Authenticator Android app, you can still use it over your local WiFi network. However, to use it over 3G/4G you will need to manually configure port forwarding in your router. ").show();
	            });
			}
		});
	}
	
	private void playPairingOperation(String pairName, NetworkType nt, PairingStageUpdater stageListener){
		BAOperation op = OperationsFactory.PAIRING_OPERATION(Authenticator.getWalletOperation(),
    			pairName, 
    			null,
    			nt,
    			0,
    			stageListener,
    			null);
		
		op.SetOperationUIUpdate(new OperationListenerAdapter() {
			@Override
			public void onError(BAOperation operation, Exception e, Throwable t) {
				Platform.runLater(() -> GuiUtils.informationalAlert("Error !", "We could not create the pairing QR code, please restart wallet."));
			}
		});
		
		boolean result = auth.addOperation(op);
    	if(!result){ 
    		GuiUtils.informationalAlert("Error !", "Could not add operation");
    	}
	}
	 
	 private void createNewStandardAccount(String accountName) throws IOException, AccountWasNotFoundException, NoWalletPasswordException{
		 ATAccount acc = auth.getWalletOperation().generateNewStandardAccount(appParams.getBitcoinNetworkType(), accountName, null);
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
