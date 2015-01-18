package org.wallet;

import org.authenticator.Authenticator;
import org.authenticator.Utils.EncodingUtils;
import org.authenticator.Utils.CurrencyConverter.Currency;
import org.authenticator.Utils.CurrencyConverter.CurrencyConverterSingelton;
import org.authenticator.Utils.CurrencyConverter.exceptions.CurrencyConverterSingeltonNoDataException;
import org.authenticator.Utils.OneName.OneName;
import org.authenticator.Utils.OneName.OneNameAdapter;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.walletCore.exceptions.CannotGetAddressException;
import org.authenticator.walletCore.exceptions.WrongWalletPasswordException;
import org.authenticator.walletCore.utils.BAPassword;
import org.authenticator.walletCore.utils.BalanceUpdater;
import org.authenticator.listeners.BAGeneralEventsAdapter;
import org.authenticator.network.BANetworkInfo;
import org.authenticator.operations.BAOperation;
import org.authenticator.operations.OperationsUtils.SignProtocol.AuthenticatorAnswerType;
import org.authenticator.operations.listeners.OperationListenerAdapter;
import org.authenticator.protobuf.ProtoSettings;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import org.authenticator.protobuf.ProtoSettings.BitcoinUnit;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.AbstractPeerEventListener;
import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import eu.hansolo.enzo.notification.Notification;
import eu.hansolo.enzo.notification.Notification.Notifier;
import javafx.animation.*;
import javafx.scene.layout.AnchorPane;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBuilder;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;
import org.wallet.ControllerHelpers.SendTxHelper;
import org.wallet.ControllerHelpers.SendTxOverlayHelper;
import org.wallet.ControllerHelpers.TableTx;
import org.wallet.ControllerHelpers.ThrottledRunnableExecutor;
import org.wallet.ControllerHelpers.UIUpdateHelper;
import org.wallet.controls.ScrollPaneContentManager;
import org.wallet.controls.SendToCell;
import org.wallet.utils.AlertWindowController;
import org.wallet.utils.BaseUI;
import org.wallet.utils.GuiUtils;
import org.wallet.utils.TextFieldValidator;
import org.wallet.utils.TextUtils;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import static org.wallet.Main.bitcoin;
import static org.wallet.utils.GuiUtils.checkGuiThread;
import static org.wallet.utils.GuiUtils.crashAlert;
import static org.wallet.utils.GuiUtils.informationalAlert;
/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class Controller  extends BaseUI{
	 @FXML private Label lblMinimize;
	 @FXML private Label lblClose;
	 @FXML private Label lblDateAndTime;
	 @FXML private Button btnOverview_white;
	 @FXML private Button btnOverview_grey;
	 @FXML private Button btnSend_white;
	 @FXML private Button btnSend_grey;
	 @FXML private Button btnReceive_white;
	 @FXML private Button btnReceive_grey;
	 @FXML private Button btnTransactions_grey;
	 @FXML private Button btnTransactions_white;
	 @FXML private Button btnApps_white;
	 @FXML private Button btnApps_grey;
	 @FXML private Button btnLock;
	 @FXML private ImageView ivOverview;
	 @FXML private Pane OverviewPane;
	 @FXML private Pane SendPane;
	 @FXML private Pane ReceivePane;
	 @FXML private Pane AppsPane;
	 @FXML private Pane SyncPane;
	 @FXML private Pane TxPane;
	 @FXML private ImageView ivSync;
	 @FXML private ProgressBar syncProgress;
	 @FXML private Button btnAvatar;
	 @FXML private Button btnAddTxOutput;
	 @FXML private Button btnSendTx;
	 @FXML private Button btnClearSendPane;
	 @FXML private HBox ReceiveHBox;
	 @FXML private Label lblConfirmedBalance;
	 @FXML private Label lblUnconfirmedBalance;
	 @FXML ImageView ivAvatar;
	 @FXML Label lblName;
	 @FXML public ChoiceBox AddressBox;
	 @FXML private TextField txMsgLabel;
	 @FXML private TextField txFee;
	 @FXML private Label lblFeeUnitName;
	 @FXML private Button btnConnection0;
	 @FXML private Button btnConnection1;
	 @FXML private Button btnConnection2;
	 @FXML private Button btnConnection3;
	 @FXML private Button btnTor_grey;
	 @FXML private Button btnTor_color;
	 @FXML private Button btnNet_grey;
	 @FXML private Button btnNet_yellow;
	 @FXML private Button btnNet_green;
	 @FXML private Label lblStatus;
	 @FXML private Button btnRequest;
	 @FXML private Button btnClearReceivePane;
	 @FXML public ChoiceBox AccountBox;
	 @FXML private TextField txReqLabel;
	 @FXML private TextField txReqAmount;
	 @FXML private TextArea txReqMemo;
	 @FXML public ScrollPane scrlViewTxHistory;
	 @FXML private HBox overviewHBox;
	 @FXML private TableView txTable;
	 @FXML private TableColumn colConfirmations;
	 @FXML private TableColumn colInOut;
	 @FXML private TableColumn colDate;
	 @FXML private TableColumn colToFrom;
	 @FXML private TableColumn colDescription;
	 @FXML private TableColumn colAmount;
	 private ScrollPaneContentManager scrlViewTxHistoryContentManager;
	 private double xOffset = 0;
	 private double yOffset = 0;
	 public ScrollPane scrlpane;
	 private ScrollPaneContentManager scrlContent;
	 public static Stage stage;
	 public Main.OverlayUI overlayUi;
	 TorListener listener = new TorListener();
	 
	 private ThrottledRunnableExecutor throttledUIUpdater;
	 

	//#####################################
	//
	//	Initialization Methods
	//
	//#####################################
	
    public void initialize() {
    	super.initialize(Controller.class);
    	startSyncRotation();
        
        scrlContent = new ScrollPaneContentManager().setSpacingBetweenItems(15);
        //scrlpane.setFitToHeight(true);
        //scrlpane.setFitToWidth(true);
        scrlpane.setStyle("-fx-border-color: #dae0e5; -fx-background: white;");
        scrlpane.setPadding(new Insets (5,0,0,5));
        scrlpane.setHbarPolicy(ScrollBarPolicy.NEVER);
        //scrlpane.setFocusTraversable(false);
        //scrlContent.setStyle(scrlpane.getStyle());
        scrlpane.setContent(scrlContent);
        
        syncProgress.setProgress(-1);
        lblName.setFont(Font.font(null, FontWeight.NORMAL, 15));
        lblConfirmedBalance.setFont(Font.font(null, FontWeight.MEDIUM, 14));
        lblUnconfirmedBalance.setFont(Font.font(null, FontWeight.MEDIUM, 14));
        createReceivePaneButtons();
        createSendButtons();
        
        // Transaction pane
        HBox notx = new HBox();
		Label l = new Label("                    No transaction history here yet  ");
		notx.setPadding(new Insets(140,0,0,140));
		l.setStyle("-fx-font-weight: SEMI_BOLD;");
		l.setTextFill(Paint.valueOf("#6e86a0"));
		l.setFont(Font.font(13));
		notx.getChildren().add(l);
		Image inout = new Image(Main.class.getResourceAsStream("in-out.png"));
		ImageView arrows = new ImageView(inout);
		notx.getChildren().add(arrows);
        txTable.setPlaceholder(notx);
        colConfirmations.setCellValueFactory(new PropertyValueFactory<TableTx,String>("confirmations"));
    	colInOut.setCellValueFactory(new PropertyValueFactory<TableTx,ImageView>("inOut"));
    	colDate.setCellValueFactory(new PropertyValueFactory<TableTx,String>("date"));
    	colToFrom.setCellValueFactory(new PropertyValueFactory<TableTx,String>("toFrom"));
    	colToFrom.setCellFactory(TextFieldTableCell.forTableColumn());
    	colDescription.setCellValueFactory(new PropertyValueFactory<TableTx,String>("description"));
    	colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
    	colAmount.setCellValueFactory(new PropertyValueFactory<TableTx,Text>("amount"));
        
        
        // Pane Control
        lblMinimize.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	Main.stage.setIconified(true);
            }
        });
        lblClose.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	Main.stage.getOnCloseRequest().handle(null);
            }
        });
        
        // Peer icon
        Tooltip.install(btnConnection0, new Tooltip("Not connected to any peers"));   
        
        // net icon
        Tooltip.install(btnNet_grey, new Tooltip("Authenticator network off"));
        btnNet_grey.setVisible(false);
        
        // transaction history scrollPane
        scrlViewTxHistoryContentManager = new ScrollPaneContentManager()
        									.setSpacingBetweenItems(20)
        									.setScrollStyle(scrlViewTxHistory.getStyle());
		scrlViewTxHistory.setContent(scrlViewTxHistoryContentManager);
		
		throttledUIUpdater = new ThrottledRunnableExecutor(500, new Runnable(){
			@Override
			public void run() {
				LOG.info("Updating UI");
				 
				 setReceiveAddresses();
			 	 try {setTxPaneHistory();} 
			 	 catch (Exception e1) {e1.printStackTrace();}
			 	 
		    	 try {setTxHistoryContent();} 
		    	 catch (Exception e1) {e1.printStackTrace();}

		    	 try {refreshBalanceLabel();} 
		     	 catch (Exception e) {e.printStackTrace();}
		    	 
		    	 Platform.runLater(() -> setFeeTipText());
			}
    	});
    	throttledUIUpdater.start();

		// accounts choice box
		AccountBox.valueProperty().addListener(new ChangeListener<String>() {
  			@Override 
  			public void changed(ObservableValue ov, String t, String t1) {
  				if(t1 != null && t1.length() > 0){
					AccountBox.setPrefWidth(org.wallet.utils.TextUtils.computeTextWidth(new Font("Arial", 14),t1, 0.0D)+45);
					try {
						changeAccount(t1);
					} catch (AccountWasNotFoundException e) {
						e.printStackTrace();
					}
  				}
  			}    
	   });
		
		// wallet lock/ unlock
		Tooltip.install(btnLock, new Tooltip("Click to Unlock Wallet"));
		
		// date and time label
		lblDateAndTime.setStyle("-fx-font-size: 13; -fx-text-fill: #bfc6ce;");
		final DateFormat format = DateFormat.getInstance();  
		final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {  
		     @Override  
		     public void handle(ActionEvent event) {  
		    	 Platform.runLater(() -> { 
		    		 final Calendar cal = Calendar.getInstance();  
		    		 String txt = format.format(cal.getTime());
			         lblDateAndTime.setText(txt);  
		    	 });
		     }  
		}));  
		timeline.setCycleCount(Animation.INDEFINITE);  
		timeline.play();  
		 
    }
    
    /**
     * will be called after the awaitRunning event is called from the WalletAppKit
     */
    public void onBitcoinSetup() {
    	bitcoin.peerGroup().addEventListener(new PeerListener());
    	TorClient tor = bitcoin.peerGroup().getTorClient();
    	if(tor != null)
    		tor.addInitializationListener(listener);          	    	
    }
    
    /**
     * will be called after the awaitRunning event is called from the authenticator
     */
    public void onAuthenticatorSetup() {
    	Authenticator.addGeneralEventsListener(vBAGeneralEventsAdapter);    	
      	
      	/**
      	 * Read the comments in TCPListener#looper()
     	 */
      	if(Authenticator.getWalletOperation().getPendingRequestSize() > 0)
      		;//TODO
      	
    	Platform.runLater(new Runnable() { 
			 @Override
			public void run() {
				// lock 
		    	Main.UI_ONLY_IS_WALLET_LOCKED = Authenticator.getWalletOperation().isWalletEncrypted();
		      	updateLockIcon();
			      	
				/**
	    	 	 * refreshBalanceLabel will take care of downloading the currency data needed
	    	 	 */
	        	 setupOneName(Authenticator.getWalletOperation().getOnename());

	         	 updateLockIcon();
	         	 
	         	setAccountChoiceBox();
	         	
	         	/*
	        	 * here because it requires the config file to load
	        	 */
	        	addTransactionOutputToPane();
			}
	    });    
    	updateUI();
    	if (Authenticator.getApplicationParams().disableSpinners){stopSyncRotation();}
    	Authenticator.getWalletOperation().sendNotificationToAuthenticatorWhenCoinsReceived();
    }
   
    private void updateUI(){
    	throttledUIUpdater.execute();
    }
    
    private BAGeneralEventsAdapter vBAGeneralEventsAdapter = new BAGeneralEventsAdapter(){
    	
    	@Override
    	public void onAccountsModified(AccountModificationType type, int accountIndex) {
    		Platform.runLater(() -> { 
    			if(type != AccountModificationType.ActiveAccountChanged )
    				setAccountChoiceBox();
    			
    			updateUI();
    		});
    	}
		
		@Override
		public void onOneNameIdentityChanged(@Nullable ConfigOneNameProfile profile, @Nullable Image profileImage) {
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  setupOneName(profile);
			  }
			});
			
		}
		
		@Override
		public void onBalanceChanged(@Nullable Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence) {
			/**
			 * Will pop a notification when received coins are pending and built
			 */
			if(tx != null)
			if(howBalanceChanged == HowBalanceChanged.ReceivedCoins){
				Platform.runLater(new Runnable() { 
					  @Override
					  public void run() {
						  	Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(tx);
				    		Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(tx);
				    		if (exit.compareTo(Coin.ZERO) > 0){} //only show notification on coins received excluding change
				    		else {
				    			Image logo = new Image(Main.class.getResourceAsStream("authenticator_logo_plain_small.png"));
				    			
				    			// Create a custom Notification without icon
				    			Notification info = new Notification("Bitcoin Authenticator Wallet", "Coins Received: " + 
				    					Authenticator.getWalletOperation().getTxValueSentToMe(tx).toFriendlyString() + 
				    					"\n" + 
				    					"Status: " + (confidence == ConfidenceType.PENDING? "Unconfirmed":"Confirmed"), logo);
						
				    			// Show the custom notification
				    			Notifier.INSTANCE.notify(info);
				    		}
					  }
					});
			}
			
			/**
			 * Will pop a notification when sent coins are confirmed
			 */
			if(tx != null)
			if(howBalanceChanged == HowBalanceChanged.SentCoins && confidence == ConfidenceType.BUILDING){
				Platform.runLater(new Runnable() { 
					  @Override
					  public void run() {
							Image logo = new Image(Main.class.getResourceAsStream("authenticator_logo_plain_small.png"));
							// Create a custom Notification without icon
							Notification info = new Notification("Bitcoin Authenticator Wallet", "Coins Sent :" + 
									Authenticator.getWalletOperation().getTxValueSentToMe(tx).toFriendlyString() + 
									"\n" + 
									"Status: " + "Confirmed", logo);
							
							// Show the custom notification
							Notifier.INSTANCE.notify(info);
					  }
					});
			}
			
			updateUI();
		}

		@Override
		public void onAuthenticatorSigningResponse(Transaction tx,
				String pairingID, PendingRequest pendingReq,
				AuthenticatorAnswerType answerType,
				String str) {
			Platform.runLater(new Runnable() { 
				  @Override
				  public void run() {
					    String notifStr = "";
						if(answerType == AuthenticatorAnswerType.Authorized){
							notifStr = "Authenticator Authorized a Transaction:\n";
							
							if(mSendTxOverlayHelper == null)
								return;
							
							if (mSendTxOverlayHelper.authenticatorVbox.isVisible()){
								GuiUtils.fadeOut(mSendTxOverlayHelper.authenticatorVbox);
								GuiUtils.fadeIn(mSendTxOverlayHelper.successVbox);
								mSendTxOverlayHelper.authenticatorVbox.setVisible(false);
								mSendTxOverlayHelper.successVbox.setVisible(true);
							}
						}
							
						else if (answerType == AuthenticatorAnswerType.NotAuthorized){
							notifStr = "Authenticator Refused To Sign a Transaction:\n";}
						else {
							return;
						}
						
						Image logo = new Image(Main.class.getResourceAsStream("authenticator_logo_plain_small.png"));
				    	// Create a custom Notification without icon
				    	Notification info = new Notification("Bitcoin Authenticator Wallet", notifStr , logo);
				    	// Show the custom notification
				    	Notifier.INSTANCE.notify(info);
				  }
				});
			
		}
		
		@Override
		public void onAddressMarkedAsUsed(ATAddress address) {
			Platform.runLater(new Runnable() { 
				  @Override
				  public void run() {
					  updateUI();
				  }
				});
		}
		
		@Override
		public void onBlockchainDownloadChange(float progress) {
			Platform.runLater(new Runnable() { 
				  @Override
				  public void run() {
				     lblStatus.setText("Synchronizing Blockchain (" + String.format("%d",(long)(progress * 100)) + " %)");
				     syncProgress.setProgress(progress);
				  }
				});

            if(progress == 1.0f) {
            	/**
            	 * run an update of balances after we finished syncing
            	 */
            	BalanceUpdater.updateBalaceNonBlocking(Authenticator.getWalletOperation(),
            											Authenticator.getWalletOperation().getTrackedWallet(),
            											new Runnable(){
										    				@Override
										    				public void run() { 
										    					Platform.runLater(new Runnable(){
										    						@Override
										    						public void run() {
										    							 readyToGoAnimation(1, null);
										    						}
										    			        });
										    				}
										        		});
			}
		}
		
		@Override
		public void onWalletSettingsChange() {
			Platform.runLater(() -> {
				updateUI();
				updateAllTransactionOutpointCellsWithNewCurrencies();
			});
		}
		
		@Override
		public void onAuthenticatorNetworkStatusChange(BANetworkInfo info) {
			Platform.runLater(() -> {
				if(info.PORT_FORWARDED == true && info.SOCKET_OPERATIONAL == true) {
					btnNet_grey.setVisible(false);
					//btnNet_green.setVisible(true);
					Tooltip.install(btnNet_green, new Tooltip("Authenticator network is on"));
				}
				else if(info.PORT_FORWARDED == false && info.SOCKET_OPERATIONAL == true) {
					btnNet_grey.setVisible(false);
					//btnNet_yellow.setVisible(true);
					Tooltip.install(btnNet_yellow, new Tooltip("No port-forwarding, can't access from outside your network"));
				}
            });	
		}
    };
    
    public class TorListener implements TorInitializationListener {

		@Override
		public void initializationProgress(String message, int percent) {
			Platform.runLater(() -> {
				lblStatus.setText(message);
				syncProgress.setProgress(percent / 100.0);
            });			
		}

		@Override
		public void initializationCompleted() {
			Platform.runLater(new Runnable() { 
				  @Override
				  public void run() {
					 lblStatus.setText("Connecting To Peers...");
					 syncProgress.setProgress(0);
				     btnTor_grey.setVisible(false);
				     btnTor_color.setVisible(true);
				     Tooltip.install(btnTor_color, new Tooltip("Connected to Tor"));
				  }
				});
		}
    }
    
    public class WalletListener extends AbstractWalletEventListener {

		@Override
		public void onWalletChanged(Wallet wallet) {
			try {setTxPaneHistory();} 
			catch (Exception e) {e.printStackTrace();}	
		}
    }
    
    
    public class PeerListener extends AbstractPeerEventListener {
    	
    	@Override
        public void onPeerConnected(Peer peer, int peerCount) {
    		lblStatus.setText("Connecting To Peers (" + peerCount + ")"); 
    		setToolTip(peerCount);
        }

        @Override
        public void onPeerDisconnected(Peer peer, int peerCount) {
        	lblStatus.setText("Connecting To Peers (" + peerCount + ")");
        	setToolTip(peerCount);
        }
        
        private void setToolTip(int peerCount) {
        	if (peerCount>0 & peerCount<6){
    			btnConnection1.setVisible(true);
    			if (peerCount==1) {Tooltip.install(btnConnection1, new Tooltip("Connected to " + peerCount + " peer"));}
    			else {Tooltip.install(btnConnection1, new Tooltip("Connected to " + peerCount + " peers"));}
    			btnConnection2.setVisible(false);
    			btnConnection3.setVisible(false);
    			btnConnection0.setVisible(false);
    		}
    		if (peerCount>6 & peerCount<10){
    			btnConnection1.setVisible(false);
    			btnConnection2.setVisible(true);
    			Tooltip.install(btnConnection2, new Tooltip("Connected to " + peerCount + " peers"));
    			btnConnection3.setVisible(false);
    			btnConnection0.setVisible(false);
    		}
    		if (peerCount>9){
    			btnConnection1.setVisible(false);
    			btnConnection2.setVisible(false);
    			btnConnection3.setVisible(true);
    			Tooltip.install(btnConnection3, new Tooltip("Connected to " + peerCount + " peers"));
    			btnConnection0.setVisible(false);
    		}
    		if (peerCount==0){
    			btnConnection1.setVisible(false);
    			btnConnection2.setVisible(false);
    			btnConnection3.setVisible(false);
    			btnConnection0.setVisible(true);
    			Tooltip.install(btnConnection0, new Tooltip("Not connected"));
    		}
        }
    }
    
    //#####################################
  	//
  	//	Tab Buttons
  	//
  	//#####################################
    
    
    @FXML protected void actionOverview(ActionEvent event) {
   	     btnOverview_white.setVisible(true);
  	 	 btnOverview_grey.setVisible(false);
  	 	 
  	 	 btnSend_white.setVisible(false);
  	 	 btnSend_grey.setVisible(true);
  	 	 
  	 	 btnReceive_white.setVisible(false);
  	 	 btnReceive_grey.setVisible(true);
  	 	 
	 	 btnTransactions_white.setVisible(false);
	 	 btnTransactions_grey.setVisible(true);
	 	 
  	 	 btnApps_white.setVisible(false);
  	 	 btnApps_grey.setVisible(true);
  	 	 
	 	 OverviewPane.setVisible(true);
	 	 SendPane.setVisible(false);
	 	 ReceivePane.setVisible(false);
	 	 AppsPane.setVisible(false);
	 	 TxPane.setVisible(false);
   }
   
   @FXML protected void actionSend(ActionEvent event) {
	     btnSend_white.setVisible(true);
	     btnSend_grey.setVisible(false);
     	 
     	 btnOverview_white.setVisible(false);
     	 btnOverview_grey.setVisible(true);
     	 
      	 btnReceive_white.setVisible(false);
   	     btnReceive_grey.setVisible(true);
   	     
   	     btnTransactions_white.setVisible(false);
      	 btnTransactions_grey.setVisible(true);
      	 
      	 btnApps_white.setVisible(false);
      	 btnApps_grey.setVisible(true);
      	 
      	 OverviewPane.setVisible(false);
      	 SendPane.setVisible(true);
      	 ReceivePane.setVisible(false);
      	 AppsPane.setVisible(false);
      	 TxPane.setVisible(false);
      }
   
   @FXML protected void actionReceive(ActionEvent event) {
   	 	 btnReceive_white.setVisible(true);
    	 btnReceive_grey.setVisible(false);
    	 
    	 btnSend_white.setVisible(false);
    	 btnSend_grey.setVisible(true);
    	 
    	 btnOverview_white.setVisible(false);
    	 btnOverview_grey.setVisible(true);
    	 
    	 btnTransactions_white.setVisible(false);
     	 btnTransactions_grey.setVisible(true);
     	 
     	 btnApps_white.setVisible(false);
  	     btnApps_grey.setVisible(true);
  	     
  	     OverviewPane.setVisible(false);
  	     SendPane.setVisible(false);
  	     ReceivePane.setVisible(true);
  	     AppsPane.setVisible(false);
  	     TxPane.setVisible(false);
     }
   
   @FXML protected void actionTransactions(ActionEvent event) {
  	 	 btnTransactions_white.setVisible(true);
  	 	 btnTransactions_grey.setVisible(false);
  	 	 
  	 	 btnSend_white.setVisible(false);
  	 	 btnSend_grey.setVisible(true);
  	 	 
  	 	 btnOverview_white.setVisible(false);
  	 	 btnOverview_grey.setVisible(true);
  	 	 
  	 	 btnReceive_white.setVisible(false);
    	 btnReceive_grey.setVisible(true);
    	 
    	 btnApps_white.setVisible(false);
 	     btnApps_grey.setVisible(true);
 	     
 	     OverviewPane.setVisible(false);
 	     SendPane.setVisible(false);
 	     ReceivePane.setVisible(false);
 	     AppsPane.setVisible(false);
 	     TxPane.setVisible(true);
    }
   
   @FXML protected void actionApps(ActionEvent event) {
	   	 btnApps_white.setVisible(true);
	   	 btnApps_grey.setVisible(false);
	   	 
	   	 btnSend_white.setVisible(false);
	   	 btnSend_grey.setVisible(true);
	   	 
	   	 btnOverview_white.setVisible(false);
	     btnOverview_grey.setVisible(true);
	     
	   	 btnTransactions_white.setVisible(false);
    	 btnTransactions_grey.setVisible(true);
    	 
    	 btnReceive_white.setVisible(false);
 	     btnReceive_grey.setVisible(true);
 	     
 	     OverviewPane.setVisible(false);
 	     SendPane.setVisible(false);
 	     ReceivePane.setVisible(false);
 	     AppsPane.setVisible(true);
 	     TxPane.setVisible(false);
    }
   
   
   
   
   @SuppressWarnings("unchecked")
   public void setAccountChoiceBox(){
	   ATAccount active = Authenticator.getWalletOperation().getActiveAccount().getActiveAccount();
	   LOG.info("Setting accounts checkbox");
	   List<ATAccount> all = new ArrayList<ATAccount>();
	   all = Authenticator.getWalletOperation().getAllAccounts();
	   
	   AccountBox.getItems().clear();
	   AccountBox.setValue(active.getAccountName());
	   for(ATAccount acc:all){
		   AccountBox.getItems().add(acc.getAccountName());
	   }
	   
	   AccountBox.setTooltip(new Tooltip("Select active account"));
	   
	   AccountBox.setValue(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountName());
	
	   AccountBox.setPrefWidth(org.wallet.utils.TextUtils.computeTextWidth(new Font("Arial", 14),AccountBox.getValue().toString(), 0.0D)+45);
   }
   
   	//#####################################
 	//
 	//	Overview Pane
 	//
 	//#####################################
  
   /**
    * called when user presses the locked icon 
    * @param event
    */
   @FXML protected void lockControl(ActionEvent event){
	   /**
	    * decrypt
	    */
	   if (Main.UI_ONLY_IS_WALLET_LOCKED){
		   displayLockDialog();
	   }
	   /**
	    * Encrypt
	    */
	   else 
	   {
		   if(Main.UI_ONLY_WALLET_PW == null || Main.UI_ONLY_WALLET_PW.length() == 0) //this shouldn't be possible
			   displayLockDialog(); 
		   else{
			   try {
				   Authenticator.getWalletOperation().encryptWallet(Main.UI_ONLY_WALLET_PW);
				   Main.UI_ONLY_WALLET_PW.cleanPassword();
				   Main.UI_ONLY_IS_WALLET_LOCKED = true;
			   } 
			   catch (WrongWalletPasswordException e) { e.printStackTrace(); }
		   }
		   updateLockIcon();
	   }
	   
   }
   
   private void displayLockDialog(){
	   Pane pane = new Pane();
	   final Main.OverlayUI<Controller> overlay = Main.instance.overlayUI(pane, Main.controller);
	   Image lock = new Image(Main.class.getResourceAsStream("lockbackground.png"));
	   ImageView ivLock = new ImageView(lock);
	   pane.setMaxSize(314, 288);
	   pane.setStyle("-fx-background-color: white;");
	   pane.setEffect(new DropShadow());
	   pane.getChildren().add(ivLock);
	   VBox unlockVBox = new VBox();
	   PasswordField pw = new PasswordField();
	   pw.setPrefWidth(250);
	   pw.setPromptText("Enter Password");
	   Button btnOK = new Button("OK");
	   btnOK.setPrefWidth(123);
	   Button btnCancel = new Button("Cancel");
	   btnCancel.setPrefWidth(123);
	   HBox buttonHBox = new HBox();
	   unlockVBox.getChildren().add(pw);
	   buttonHBox.getChildren().add(btnOK);
	   buttonHBox.getChildren().add(btnCancel);
	   unlockVBox.getChildren().add(buttonHBox);
	   pane.getChildren().add(unlockVBox);
	   unlockVBox.setPadding(new Insets(80,0,0,32));
	   buttonHBox.setPadding(new Insets(148,0,0,0));
	   buttonHBox.setMargin(btnOK, new Insets(0,4,0,0));
	   buttonHBox.setAlignment(Pos.CENTER);
	   btnCancel.setOnMouseClicked(new EventHandler<MouseEvent>() {
		   @Override
		   public void handle(MouseEvent event) {
			   overlay.done();
		   }
	   });
	   btnOK.setOnMouseClicked(new EventHandler<MouseEvent>() {
		   @Override
		   public void handle(MouseEvent event) {
			   /**
			    * In case we decrypt
			    */
			   if(Authenticator.getWalletOperation().isWalletEncrypted()){
				   if (pw.getText().equals("")){
					   informationalAlert("Unfortunately, you messed up.",
							   "You need to enter your password to decrypt your wallet.");
					   return;
				   }
				   else {
					   try{
						   BAPassword temp = new BAPassword(pw.getText());
						   Authenticator.getWalletOperation().decryptWallet(temp);
						   Authenticator.getWalletOperation().encryptWallet(temp);
						   Main.UI_ONLY_WALLET_PW.setPassword(temp.toString());
						   overlay.done();
						   Main.UI_ONLY_IS_WALLET_LOCKED = false;
						   updateLockIcon();
					   }
					   catch(KeyCrypterException | WrongWalletPasswordException  e){
						   informationalAlert("Unfortunately, you messed up.",
								   "Wrong wallet password");
						   return;
					   }
				   }
			   }
			   else{
				   /**
				    * In case we encrypt but we don't have the password
				    */
				   if (pw.getText().equals("")){
					   informationalAlert("Unfortunately, you messed up.",
							   "You need to enter your password to encrypt your wallet.");
					   return;
				   }
				   try {
					   BAPassword temp = new BAPassword(pw.getText());
						Authenticator.getWalletOperation().encryptWallet(temp);
						Main.UI_ONLY_WALLET_PW.setPassword(temp.toString());
					    overlay.done();
					    updateLockIcon();
					} catch (WrongWalletPasswordException e) {
						e.printStackTrace();
					}
				   
			   }
		   }
	   });   
   }
   
   private void updateLockIcon(){
	   if(Main.UI_ONLY_IS_WALLET_LOCKED){
		   Image imglocked = new Image(Main.class.getResource("btnLocked.png").toString());
		   ImageView img = new ImageView(imglocked);
		   img.setFitWidth(25);
		   img.setFitHeight(26);
		   btnLock.setGraphic(img);
		   overviewHBox.setMargin(btnLock, new Insets(0,0,0,0));
		   Tooltip.install(btnLock, new Tooltip("Click to Unlock Wallet"));
	   }
	   else
	   {
		   Image unlocked = new Image(Main.class.getResource("btnUnlocked.png").toString());
		   ImageView img = new ImageView(unlocked);
		   img.setFitWidth(25);
		   img.setFitHeight(26);
		   btnLock.setGraphic(img);
		   overviewHBox.setMargin(btnLock, new Insets(-2,0,0,0));
		   Tooltip.install(btnLock, new Tooltip("Click to Lock Wallet"));
	   }
   }
   
   public void setupOneName(ConfigOneNameProfile one){
	   if(one != null) {
		   LOG.info("Setting oneName avatar image");
		   // get image
		   File imgFile = null;
		   BufferedImage bimg = null;
		   Image img = null;
		   try {
			    imgFile = new File(one.getOnenameAvatarFilePath());
			    if(imgFile.exists()) {
			    	img = new Image(imgFile.toURI().toString());
			    }
			    else {
			    	OneName.downloadAvatarImage(one, Authenticator.getWalletOperation(), new OneNameAdapter() {
						@Override
						public void getOneNameAvatarImage(ConfigOneNameProfile one, Image img) {
							if(img != null && one != null) {
								Platform.runLater(new Runnable() { 
									  @Override
									  public void run() {
										  setUserProfileAvatarAndName(img,one.getOnenameFormatted());
									  }
								});
							}
								   
						}
					});
			    }
				
		   } catch (Exception e) { // do nothing
		   }
		   
		   if(img != null && one != null)
			   setUserProfileAvatarAndName(img,one.getOnenameFormatted());	   
	   }
	   else {
		   lblName.setText("Welcome to Authenticator Wallet");
		   lblName.setPrefWidth(org.wallet.utils.TextUtils.computeTextWidth(lblName.getFont(),
		   lblName.getText(), 0.0D));
		   Image def = new Image(Main.class.getResourceAsStream("DefaultAvatar.png"));
		   ivAvatar.setImage(def);
	   }
   }
   
	public void setUserProfileAvatarAndName(Image img, String name) {
		ivAvatar.setImage(img);
		//
		lblName.setText("Welcome back, " + name + " ");
		lblName.setPrefWidth(org.wallet.utils.TextUtils.computeTextWidth(lblName.getFont(),
		lblName.getText(), 0.0D));
	}

    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stage.setX(event.getScreenX() - xOffset);
    	Main.stage.setY(event.getScreenY() - yOffset);
    }
    
    @FXML protected void openOneNameDialog(ActionEvent event){
    	Main.instance.overlayUI("OneName.fxml");
    	
    	updateUI();
    }
    
    @SuppressWarnings("static-access")
	private void refreshBalanceLabel() throws JSONException, IOException, AccountWasNotFoundException {
    	LOG.info("Refreshing balance");
    	new UIUpdateHelper.BalanceUpdater(lblConfirmedBalance, lblUnconfirmedBalance).execute();
    }
    
    
    
    @SuppressWarnings("unused")
    private void setTxHistoryContent() throws Exception{
    	LOG.info("Setting Tx History in Overview Pane");
    	new UIUpdateHelper.TxHistoryContentUpdater(scrlViewTxHistoryContentManager).execute();
    }
    
    //#####################################
   	//
   	//	Send Pane
   	//
   	//#####################################
    
    public void createSendButtons(){
    	Label labelSend = AwesomeDude.createIconLabel(AwesomeIcon.SEND, "16");
		labelSend.setPadding(new Insets(0,0,0,3));
		btnSendTx.setGraphic(labelSend);
		btnSendTx.setFont(Font.font(null, FontWeight.NORMAL, 14));
		Label labelClear = AwesomeDude.createIconLabel(AwesomeIcon.TRASH_ALT, "18");
		labelClear.setPadding(new Insets(0,0,0,3));
		btnClearSendPane.setGraphic(labelClear);
		btnClearSendPane.setFont(Font.font(null, FontWeight.NORMAL, 14));
		txFee.lengthProperty().addListener(new ChangeListener<Number>(){
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
				if(newValue.longValue() > oldValue.longValue()){
				      char ch = txFee.getText().charAt(oldValue.intValue());  
				      //Check if the new character is the number or other's
					  if(!(ch >= '0' && ch <= '9') && ch != '.'){       
						   //if it's not number then just setText to previous one
						  setFeeTipText(); 
						  return;
					  }
				}
				else
					setFeeTipText();
            }
        });
    }
    
    private void setFeeTipText() {    	 
    	LOG.info("Updating fee text box");
    	double fee = Authenticator.getWalletOperation().getDefaultFeeFromSettings();
    	BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
    	int decimals = Authenticator.getWalletOperation().getDecimalPointFromSettings();
    	double printableFee = TextUtils.satoshiesToBitcoinUnit(fee, u);
    	String strFee = String.format( "%." + decimals + "f", printableFee );
    	txFee.setPromptText("Fee: " + strFee + " " + TextUtils.getAbbreviatedUnit(u));
    	
    	// set unit name
    	BitcoinUnit unit = Authenticator
    			.getWalletOperation()
    			.getAccountUnitFromSettings();
		String unitStr = unit.getValueDescriptor()
    			.getOptions()
    			.getExtension(ProtoSettings.bitcoinUnitName);
    }
    
    @FXML protected void btnAddTxOutputPressed(MouseEvent event) {
    	btnAddTxOutput.setStyle("-fx-background-color: #a1d2e7;");
    }
    
    @FXML protected void btnAddTxOutputReleased(MouseEvent event) {
    	btnAddTxOutput.setStyle("-fx-background-color: #199bd6;");
    }
    
    @FXML protected void btnSendTxPressed(MouseEvent event) {
    	btnSendTx.setStyle("-fx-background-color: #a1d2e7;");
    }
    
    @FXML protected void btnSendTxReleased(MouseEvent event) {
    	btnSendTx.setStyle("-fx-background-color: #199bd6;");
    } 
    
    @FXML protected void btnClearSendPanePressed(MouseEvent event) {
    	btnClearSendPane.setStyle("-fx-background-color: #d7d4d4;");
    	txMsgLabel.clear();
    	scrlContent.clearAll(); addTransactionOutputToPane();
    	
    	txFee.clear();
    	this.setFeeTipText();
    }
    
    @FXML protected void btnClearSendPaneReleased(MouseEvent event) {
    	btnClearSendPane.setStyle("-fx-background-color: #999999;");
    } 
    
    private boolean ValidateTx()
    {
    	// fee
    	Coin cFee = Coin.ZERO;
		if (txFee.getText().length() == 0){cFee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
        else {
        	double fee = Double.parseDouble(txFee.getText());
        	fee = TextUtils.bitcoinUnitToSatoshies(fee, Authenticator.getWalletOperation().getAccountUnitFromSettings());
        	cFee = Coin.valueOf((long)fee);
        }
    	try {
			return SendTxHelper.ValidateTx(scrlContent, cFee);
		} catch (Exception e) { 
			e.printStackTrace();
			return false;
		}
    }
    
    @FXML protected void SendTx(ActionEvent event) throws CurrencyConverterSingeltonNoDataException{
    	if(!ValidateTx()){
    		informationalAlert("Something Is not Right ...",
    				"Make Sure:\n" +
    						"  1) You entered correct values in all fields\n"+
    						"  2) You have sufficient funds to cover your outputs\n"+
    						"  3) Outputs amount to at least the dust value(" + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.toString() + ")\n");
    	}
    	else{
    		Transaction tx = new Transaction(Authenticator.getWalletOperation().getNetworkParams());
    		// collect Tx outputs
    		ArrayList<String> OutputAddresses = new ArrayList<String>();
    		HashMap<String , Coin> to = new HashMap<String , Coin>();
    		Coin outAmount = Coin.valueOf(0);
        	for(Node n:scrlContent.getChildren())
        	{
        		SendToCell na = (SendToCell)n;
   
				Coin v = Coin.valueOf((long) na.getAmountValue());
				if (v.compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
					outAmount = outAmount.add(v);
					OutputAddresses.add(na.getAddress());
					to.put(na.getAddress(), v);
				}
				else 
					;//TODO        		
        	}
        	
        	try{
//        		get fee
            	Coin cFee = Coin.ZERO;
        		if (txFee.getText().length() == 0){cFee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
                else {
                	double fee = Double.parseDouble(txFee.getText());
                	fee = TextUtils.bitcoinUnitToSatoshies(fee, Authenticator.getWalletOperation().getAccountUnitFromSettings());
                	cFee = Coin.valueOf((long)fee);
                }
        	
        		ArrayList<TransactionOutput> outputs = Authenticator
        				.getWalletOperation()
        				.selectOutputsFromAccount(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex(),
        						outAmount.add(cFee));
        		
        		String changeaddr = Authenticator.getWalletOperation()
    								.getNextExternalAddress(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex())
    								.getAddressStr();
        		   
        		// complete Tx
        		tx = Authenticator.getWalletOperation()
        				.mkUnsignedTxWithSelectedInputs(outputs, to, cFee, changeaddr);
    						
    			//
    			displayTxOverview(tx,
						OutputAddresses,
    					to,
    					changeaddr, 
    					outAmount, 
    					cFee, 
    					Authenticator.getWalletOperation().getTxValueSentFromMe(tx).subtract(Authenticator.getWalletOperation().getTxValueSentToMe(tx)));
        	}
        	catch(Exception e){
        		e.printStackTrace();
        		Platform.runLater(new Runnable() {
				      @Override public void run() {
				    	  informationalAlert("Cannot Create Tx",
				    			  "");
				      }
				    });
        	}
        	
    	}
    }
    
    SendTxOverlayHelper mSendTxOverlayHelper;
    private void displayTxOverview(Transaction tx,
								   ArrayList<String> OutputAddresses,
    		HashMap<String, Coin> to,
    		String changeaddr,
    		Coin outAmount,
    		Coin fee,
    		Coin leavingWallet){
    	
    	mSendTxOverlayHelper = new SendTxOverlayHelper(tx,
														OutputAddresses,
										        		to, 
										        		changeaddr,
										        		outAmount,
										        		fee,
										        		leavingWallet);
    	
    	mSendTxOverlayHelper.btnSendTransaction.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	try {
            		if(Main.UI_ONLY_IS_WALLET_LOCKED) {
                		if(!checkIfPasswordDecryptsWallet(new BAPassword(mSendTxOverlayHelper.pfPassword.getText()))){
                			informationalAlert("Unfortunately, you messed up.",
                					"Wrong password");
                    		return;
                		}
                		Main.UI_ONLY_WALLET_PW.setPassword(mSendTxOverlayHelper.pfPassword.getText());
            		}
        			
        			String to = "";
        			if (OutputAddresses.size()==1){
        				if( Authenticator.getWalletOperation().isWatchingAddress(OutputAddresses.get(0))){
        					ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(OutputAddresses.get(0));
        					int index = add.getAccountIndex();
        					if (index==Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex()){
        						to = "Internal Transfer";
        					}
        					else {to = "Transfer to " + Authenticator.getWalletOperation().getAccount(index).getAccountName();}
        				}
        				else {to = OutputAddresses.get(0) ;}	
        			}
        			else {to = "Multiple";}
        			mSendTxOverlayHelper.transactionOverviewBox.setVisible(false);
        			if (broadcast(tx,to,Main.UI_ONLY_WALLET_PW) == true) {
        				// animate success
        				Animation ani = GuiUtils.fadeOut(mSendTxOverlayHelper.transactionOverviewBox);
            			if (Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountType()==WalletAccountType.AuthenticatorAccount){
            				GuiUtils.fadeIn(mSendTxOverlayHelper.authenticatorVbox);
            				mSendTxOverlayHelper.authenticatorVbox.setVisible(true);
            				if (!Authenticator.getApplicationParams().disableSpinners){startAuthRotation(mSendTxOverlayHelper.ivLogo1);}
            			}
            			else {
            				GuiUtils.fadeIn(mSendTxOverlayHelper.successVbox);
            				mSendTxOverlayHelper.successVbox.setVisible(true);
            			}
        			}
        			else {
        				mSendTxOverlayHelper.txOverlay.done();
                    	txMsgLabel.clear();
                    	scrlContent.clearAll(); addTransactionOutputToPane();
                    	
                    	txFee.clear();
                    	setFeeTipText();
                    	
                    	informationalAlert("Could not add operation to queue",
                    			"try to restart the wallet and resend the Tx");
        			}
        				
        		} 
        		catch (Exception e) {
        			informationalAlert("Something went wrong",
                			e.getMessage());
        			e.printStackTrace();
        		}
            }
        });
		
    	mSendTxOverlayHelper.btnSuccessPaneFinish.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	mSendTxOverlayHelper.btnSuccessPaneFinish.setDisable(true);
            	mSendTxOverlayHelper.txOverlay.done();
            	txMsgLabel.clear();
            	scrlContent.clearAll(); addTransactionOutputToPane();
            	txFee.clear();
            	setFeeTipText();
            	
            }
        });
    	
    	mSendTxOverlayHelper.btnAuthorizeTxLater.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	stopAuthRotation();
            	mSendTxOverlayHelper.txOverlay.done();
            	txMsgLabel.clear();
            	scrlContent.clearAll(); addTransactionOutputToPane();
            	txFee.clear();
            	setFeeTipText();
            }
        });
    	mSendTxOverlayHelper.btnCancel.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
				public void handle(MouseEvent event) {
				mSendTxOverlayHelper.txOverlay.done();
			}
		});
    }
    	
    public boolean broadcast (Transaction tx, String to, @Nullable BAPassword WALLET_PW) throws CannotGetAddressException {
    	return SendTxHelper.broadcastTx(tx, 
    			txMsgLabel.getText(), 
    			to,
    			WALLET_PW, 
    			new OperationListenerAdapter(){
					
					@Override
					public void onError(BAOperation operation, Exception e, Throwable t) {
						Platform.runLater(new Runnable() {
						      @Override public void run() {
						    	  String desc = "";
						    	  if(t != null){
						    		  Throwable rootCause = Throwables.getRootCause(t);
						    		  desc = rootCause.toString();
						    		  t.printStackTrace();
						    	  }
						    	  if(e != null){
						    		  desc = e.toString();
						    		  e.printStackTrace();
						    	  }
						    	  //
						    	  informationalAlert("Error broadcasting Tx",
						    			  desc);
						      }
						    });
					}
					
					@Override
					public void onWarning(BAOperation operation, String warning) {
						Platform.runLater(() -> {
						    	  informationalAlert("Warning",
						    			  warning);
						      	}
						);
					}
				});
    }	
    
    
    @FXML protected void addTxOutput() {
    	addTransactionOutputToPane();
    }
    
    private void addTransactionOutputToPane() {
    	Class[] parameterTypes = new Class[1];
        parameterTypes[0] = int.class;
        Method removeOutput;
		try {
			removeOutput = Controller.class.getMethod("removeOutput", parameterTypes);
			//NewAddress na = new SendTxHelper.NewAddress(scrlContent.getCount()).setCancelOnMouseClick(this,removeOutput);
			SendToCell na = new SendToCell(scrlContent.getCount());
			
			// generate currency list
			List<String> l = new ArrayList<String>();
			String a = TextUtils.getAbbreviatedUnit(Authenticator
	    			.getWalletOperation()
	    			.getAccountUnitFromSettings());
			l.add(a);
			for(String s: Currency.AVAILBLE_CURRENCY_CODES)
				l.add(s);
			na.initGUI(l, a);
			
			na.setCancelOnMouseClick(this,removeOutput);
			scrlContent.addItem(na);
			//scrlpane.setContent(scrlContent);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
    }
    
    private void updateAllTransactionOutpointCellsWithNewCurrencies() {
    	for(Node n:scrlContent.getChildren())
    	{
    		SendToCell na = (SendToCell)n;
    		
    		// generate currency list
			List<String> l = new ArrayList<String>();
			String a = TextUtils.getAbbreviatedUnit(Authenticator
	    			.getWalletOperation()
	    			.getAccountUnitFromSettings());
			l.add(a);
			for(String s: Currency.AVAILBLE_CURRENCY_CODES)
				l.add(s);
    		
    		na.updateCurrencyChoiceBox(l, a);
    	}
    }
    
    /**
     * Is public so an added {@link org.wallet.controls.SendToCell SendToCell} would 
     * be able to find it and execute a remove cell action
     * 
     * @param index
     */
    public void removeOutput(int index) {
    	scrlContent.removeNodeAtIndex(index);
    	// TODO - make it better !
    	for(Node n:scrlContent.getChildren()){
    		//NewAddress na = (NewAddress)n;
    		SendToCell na = (SendToCell)n;
    		if(na.getIndex() < index)
    			continue;
    		//na.index--;
    		na.setIndex(na.getIndex() - 1);
    	}
    	//scrlpane.setContent(scrlContent);
    }
    
    //#####################################
   	//
   	//	Receive Pane
   	//
   	//#####################################
    
    @FXML protected void btnRequestPressed(MouseEvent event) {
    	btnRequest.setStyle("-fx-background-color: #a1d2e7;");
    }
    
    @FXML protected void btnRequestReleased(MouseEvent event) {
    	btnRequest.setStyle("-fx-background-color: #199bd6;");
    }
    
    @FXML protected void btnClearReceivePanePressed(MouseEvent event) {
    	btnClearReceivePane.setStyle("-fx-background-color: #d7d4d4;");
    }
    
    @FXML protected void btnClearReceivePaneReleased(MouseEvent event) {
    	btnClearReceivePane.setStyle("-fx-background-color: #999999;");
    } 
    
    void createReceivePaneButtons(){
    	Label labelReq = AwesomeDude.createIconLabel(AwesomeIcon.REPLY, "16");
    	labelReq.setPadding(new Insets(0,0,0,3));
    	btnRequest.setGraphic(labelReq);
    	btnRequest.setFont(Font.font(null, FontWeight.NORMAL, 14));
    	Label labelClear = AwesomeDude.createIconLabel(AwesomeIcon.TRASH_ALT, "18");
    	labelClear.setPadding(new Insets(0,0,0,3));
    	btnClearReceivePane.setGraphic(labelClear);
    	btnClearReceivePane.setFont(Font.font(null, FontWeight.NORMAL, 14));  
    	
    	/*
    	 * Copy address button
    	 */
    	Button btnCopy = new Button();
        ReceiveHBox.setMargin(btnCopy, new Insets(14,0,0,4));
        btnCopy.setFont(new Font("Arial", 18));
        Tooltip.install(btnCopy, new Tooltip("Copy address to clipboard"));
        btnCopy.getStyleClass().add("custom-button");
        btnCopy.setPrefSize(10, 10);
        AwesomeDude.setIcon(btnCopy, AwesomeIcon.COPY);
        btnCopy.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnCopy.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
        btnCopy.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnCopy.setStyle("-fx-background-color: #199bd6;");
            }
        });
        btnCopy.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(AddressBox.getValue().toString());
                clipboard.setContent(content);
            }
        });
        
        /*
         * Display address QR button
         */
        Button btnQR = new Button();
        ReceiveHBox.setMargin(btnQR, new Insets(14,0,0,3));
        btnQR.setFont(new Font("Arial", 18));
        Tooltip.install(btnQR, new Tooltip("Display address in QR code"));
        btnQR.getStyleClass().add("custom-button");
        btnQR.setPrefSize(10, 10);
        AwesomeDude.setIcon(btnQR, AwesomeIcon.QRCODE);
        btnQR.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnQR.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
        btnQR.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnQR.setStyle("-fx-background-color: #199bd6;");
            }
        });
        btnQR.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	 // Serialize to PNG and back into an image. Pretty lame but it's the shortest code to write and I'm feeling
                // lazy tonight.
                byte[] imageBytes = null;
				try {
					imageBytes = QRCode
					        .from(uri())
					        .withSize(360, 360)
					        .to(ImageType.PNG)
					        .stream()
					        .toByteArray();
				} catch (AddressFormatException e1) {e1.printStackTrace();}
                Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
                ImageView view = new ImageView(qrImage);
                view.setEffect(new DropShadow());
                // Embed the image in a pane to ensure the drop-shadow interacts with the fade nicely, otherwise it looks weird.
                // Then fix the width/height to stop it expanding to fill the parent, which would result in the image being
                // non-centered on the screen. Finally fade/blur it in.
                Pane pane = new Pane(view);
                pane.setMaxSize(qrImage.getWidth(), qrImage.getHeight());
                final Main.OverlayUI<Controller> overlay = Main.instance.overlayUI(pane, Main.controller);
                view.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        overlay.done();
                    }
                });
            }
        });
        
        /*
         * Copy public key button
         */
        Button btnKey = new Button();
        btnKey.setFont(new Font("Arial", 18));
        Tooltip.install(btnKey, new Tooltip("Copy public key to clipboard"));
        ReceiveHBox.setMargin(btnKey, new Insets(14,0,0,3));
        btnKey.getStyleClass().add("custom-button");
        btnKey.setPrefSize(10, 10);
        AwesomeDude.setIcon(btnKey, AwesomeIcon.KEY);
        btnKey.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnKey.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
        btnKey.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnKey.setStyle("-fx-background-color: #199bd6;");
            }
        });
        btnKey.setOnAction(new EventHandler<ActionEvent>() {
            @Override 
            public void handle(ActionEvent e) {
            	ATAddress add;
				try {
					/**
					 * Standard Pay-To-PubHash address
					 */
					String outPut = "";
					if(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountType() == WalletAccountType.StandardAccount ){
						int accountID = Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex();
						ATAddress ca = Authenticator.getWalletOperation().findAddressInAccounts(AddressBox.getValue().toString());
						int index = ca.getKeyIndex();
						ECKey key = Authenticator.getWalletOperation().getPubKeyFromAccount(accountID, HierarchyAddressTypes.External, index, false);
						outPut = Hex.toHexString(key.getPubKey());
					}
					else
					{
						/**
						 * P2SH Address
						 */
						PairedAuthenticator po = Authenticator.getWalletOperation().getActiveAccount().getPairedAuthenticator();
						int accountID = Authenticator.getWalletOperation().getActiveAccount().getPairedAuthenticator().getWalletAccountIndex();
						ATAddress ca = Authenticator.getWalletOperation().findAddressInAccounts(AddressBox.getValue().toString());
						int keyIndex = ca.getKeyIndex();
						ECKey authKey = Authenticator.getWalletOperation().getPairedAuthenticatorKey(po, keyIndex);
						ECKey walletKey = Authenticator.getWalletOperation().getPubKeyFromAccount(accountID, 
								HierarchyAddressTypes.External, 
								keyIndex, 
								true); // true cause its a P2SH address
						
						outPut = "Authenticator Pubkey: " + Hex.toHexString(authKey.getPubKey()) + "\n" + 
								 "Wallet Pubkey: " + Hex.toHexString(walletKey.getPubKey());
					}
					final Clipboard clipboard = Clipboard.getSystemClipboard();
	                final ClipboardContent content = new ClipboardContent();
	                content.putString(outPut);
	                clipboard.setContent(content);
					
				} catch (Exception e1) { e1.printStackTrace(); }
            }
        });
        ReceiveHBox.getChildren().add(btnCopy);
        ReceiveHBox.getChildren().add(btnQR);
        ReceiveHBox.getChildren().add(btnKey); 
        
        txReqAmount.lengthProperty().addListener(new ChangeListener<Number>(){
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
                  if(newValue.intValue() > oldValue.intValue()){
                      char ch = txReqAmount.getText().charAt(oldValue.intValue());  
                      //Check if the new character is the number or other's
                      if(!(ch >= '0' && ch <= '9') && ch != '.'){       
                           //if it's not number then just setText to previous one
                           txReqAmount.setText(txReqAmount.getText().substring(0,txReqAmount.getText().length()-1)); 
                      }
                 }
            }
        });
        //Payment Request Window
        btnRequest.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	if (txReqAmount.getText().toString().equals("")){
            		informationalAlert("You messed up!",
                            "A payment request for zero bitcoins?");
            	}
            	else {
            		byte[] imageBytes = null;
            		try {
            			imageBytes = QRCode
            					.from(paymentRequestURI())
            					.withSize(300, 300)
            					.to(ImageType.PNG)
            					.stream()
            					.toByteArray();
            		} catch (AddressFormatException e1) {e1.printStackTrace();}
            		Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
            		ImageView view = new ImageView(qrImage);
            		Button btnCopyURI = new Button("Copy URI");
            		btnCopyURI.setOnAction(new EventHandler<ActionEvent>() {
            			@Override 
            			public void handle(ActionEvent e) {
            				Clipboard clipboard = Clipboard.getSystemClipboard();
            				ClipboardContent content = new ClipboardContent();
            				try {content.putString(paymentRequestURI());} 
            				catch (AddressFormatException e1) {e1.printStackTrace();}
            				try {content.putHtml(String.format("<a href='%s'>%s</a>", paymentRequestURI(), paymentRequestURI()));} 
            				catch (AddressFormatException e1) {e1.printStackTrace();}
            				clipboard.setContent(content);
            			}
            		});
            		btnCopyURI.getStyleClass().add("custom-button");
            		btnCopyURI.setOnMousePressed(new EventHandler<MouseEvent>(){
            			@Override
            			public void handle(MouseEvent t) {
            				btnCopyURI.setStyle("-fx-background-color: #a1d2e7;");
            			}
            		});
            		btnCopyURI.setOnMouseReleased(new EventHandler<MouseEvent>(){
            			@Override
            			public void handle(MouseEvent t) {
            				btnCopyURI.setStyle("-fx-background-color: #199bd6;");
            			}
            		});
            		Button btnSaveImg = new Button("Save Image");
            		btnSaveImg.setOnAction(new EventHandler<ActionEvent>() {
            			@Override 
            			public void handle(ActionEvent e) {
            				FileChooser fileChooser = new FileChooser();
            				fileChooser.setTitle("Save Image");
            				fileChooser.setInitialFileName("PaymentRequest.png");
            				File file = fileChooser.showSaveDialog(Main.stage);
            				if (file != null) {
            					try {
            						ImageIO.write(SwingFXUtils.fromFXImage(qrImage,
            								null), "png", file);
            					} catch (IOException ex) {
            						System.out.println(ex.getMessage());
            					}
            				}
            			}
            		});
            		btnSaveImg.getStyleClass().add("custom-button");
            		btnSaveImg.setOnMousePressed(new EventHandler<MouseEvent>(){
            			@Override
            			public void handle(MouseEvent t) {
            				btnSaveImg.setStyle("-fx-background-color: #a1d2e7;");
            			}
            		});
            		btnSaveImg.setOnMouseReleased(new EventHandler<MouseEvent>(){
            			@Override
            			public void handle(MouseEvent t) {
            				btnSaveImg.setStyle("-fx-background-color: #199bd6;");
            			}
            		});
            		Button btnReqClose = new Button("Close");
            		btnReqClose.getStyleClass().add("custom-button");
            		btnReqClose.setOnMousePressed(new EventHandler<MouseEvent>(){
            			@Override
            			public void handle(MouseEvent t) {
            				btnReqClose.setStyle("-fx-background-color: #a1d2e7;");
            			}
            		});
            		btnReqClose.setOnMouseReleased(new EventHandler<MouseEvent>(){
            			@Override
            			public void handle(MouseEvent t) {
            				btnReqClose.setStyle("-fx-background-color: #199bd6;");
            			}
            		});
            		txReqMemo.setFocusTraversable(false);
            		Label lblReqAddr1 = new Label();
            		Label lblReqAddr2 = new Label();
            		HBox reqAddrHbox = new HBox();
            		reqAddrHbox.setPadding(new Insets(10,0,0,0));
            		reqAddrHbox.getChildren().add(lblReqAddr1);
            		reqAddrHbox.getChildren().add(lblReqAddr2);
            		Label lblReqDetails =  new Label();
            		Label lblReqAmount1 = new Label();
            		Label lblReqAmount2 = new Label();
            		HBox reqAmountHbox = new HBox();
            		reqAmountHbox.setPadding(new Insets(10,0,0,0));
            		reqAmountHbox.getChildren().add(lblReqAmount1);
            		reqAmountHbox.getChildren().add(lblReqAmount2);
            		Label lblReqLabel1 = new Label();
            		Label lblReqLabel2 = new Label();
            		HBox reqLabelHbox = new HBox();
            		reqLabelHbox.setPadding(new Insets(10,0,0,0));
            		reqLabelHbox.getChildren().add(lblReqLabel1);
            		reqLabelHbox.getChildren().add(lblReqLabel2);
            		Label lblReqMemo1 = new Label();
            		Label lblReqMemo2 = new Label();
            		HBox reqMemoHbox = new HBox();
            		reqMemoHbox.setPadding(new Insets(10,0,0,0));
            		reqMemoHbox.getChildren().add(lblReqMemo1);
            		reqMemoHbox.getChildren().add(lblReqMemo2);
            		lblReqDetails.setText("Payment Details");
            		lblReqDetails.setFont(new Font(20));
            		lblReqAmount1.setText("Amount:   ");
            		lblReqAmount2.setText(txReqAmount.getText().toString() + " BTC");
            		lblReqAmount1.setFont(Font.font(null, FontWeight.BOLD, 14));
            		lblReqAddr1.setText("Address:  ");
            		lblReqAddr1.setFont(Font.font(null, FontWeight.BOLD, 14));
            		lblReqAddr2.setText(AddressBox.getValue().toString());
            		lblReqLabel1.setText("Label:    ");
            		lblReqLabel1.setFont(Font.font(null, FontWeight.BOLD, 14));
            		lblReqLabel2.setText(txReqLabel.getText().toString());
            		lblReqMemo1.setText("Memo:     ");
            		lblReqMemo1.setFont(Font.font(null, FontWeight.BOLD, 14));
            		lblReqMemo2.setText(txReqMemo.getText().toString());
            		lblReqMemo2.setWrapText(true);
            		lblReqMemo2.setPrefWidth(300);
            		HBox reqHbox = new HBox();
            		VBox ivVbox = new VBox();
            		ivVbox.getChildren().add(view);
            		ivVbox.setPadding(new Insets(30,0,0,0));
            		reqHbox.getChildren().add(ivVbox);
            		VBox reqVbox = new VBox();
            		reqVbox.getChildren().add(lblReqDetails);
            		reqVbox.getChildren().add(reqLabelHbox);
            		reqVbox.getChildren().add(reqAddrHbox);
            		reqVbox.getChildren().add(reqAmountHbox);
            		reqVbox.getChildren().add(reqMemoHbox);
            		HBox btnHbox = new HBox();
            		btnHbox.setPadding(new Insets(10,0,0,0));
            		btnHbox.setMargin(btnCopyURI, new Insets(0,10,0,0));
            		btnHbox.setMargin(btnSaveImg, new Insets(0,10,0,0));
            		btnHbox.getChildren().add(btnCopyURI);
            		btnHbox.getChildren().add(btnSaveImg);
            		btnHbox.getChildren().add(btnReqClose);
            		reqVbox.getChildren().add(btnHbox);
            		reqVbox.setPadding(new Insets(70,0,0,0));
            		reqHbox.getChildren().add(reqVbox);
            		Pane pane = new Pane(reqHbox);
            		pane.setMaxSize(700, 360);
            		pane.setStyle("-fx-background-color: white;");
            		pane.setEffect(new DropShadow());
            		final Main.OverlayUI<Controller> overlay = Main.instance.overlayUI(pane, Main.controller);
            		btnReqClose.setOnMouseClicked(new EventHandler<MouseEvent>() {
            			@Override
            				public void handle(MouseEvent event) {
            				overlay.done();
            			}
            		});
            	}
            	}
            });
        	btnClearReceivePane.setOnAction(new EventHandler<ActionEvent>() {
        		@Override public void handle(ActionEvent e) {
        			txReqLabel.setText("");
        			txReqMemo.setText("");
        			txReqAmount.setText("");
        		}
        	});
    }
    
    public String uri() throws AddressFormatException {
		Address addr = new Address(Authenticator.getWalletOperation().getNetworkParams(), AddressBox.getValue().toString());
        return BitcoinURI.convertToBitcoinURI(addr, null, Authenticator.getApplicationParams().getAppName(), null);
    }

    public String paymentRequestURI() throws AddressFormatException {
		Address addr = new Address(Authenticator.getWalletOperation().getNetworkParams(), AddressBox.getValue().toString());
		double amount = (double) Double.parseDouble(txReqAmount.getText())*100000000;
		long satoshis = (long) amount;
		Coin reqamount = Coin.valueOf(satoshis);
        return BitcoinURI.convertToBitcoinURI(addr, reqamount, txReqLabel.getText().toString(), txReqMemo.getText().toString());
    }
    	
    @SuppressWarnings("unchecked")
	private void setReceiveAddresses(){
    	LOG.info("Updating received addresses");
    	new UIUpdateHelper.ReceiveAddressesUpdater(AddressBox).execute();
    }       
    
    //#####################################
   	//
   	//	Transactions Pane
   	//
   	//#####################################
    
    public void setTxPaneHistory() {
    	LOG.info("Updating Tx pane");
    	new UIUpdateHelper.TxPaneHistoryUpdater(txTable, colToFrom, colDescription, colConfirmations).execute();
    }
    
    //#####################################
   	//
   	//	Apps Pane
   	//
   	//#####################################
    @FXML protected void btnNotImplementedYet(MouseEvent event) {
    	informationalAlert("Whoa, hold your horses",
				"This is only an alpha release. Check back in a couple weeks and we should have more apps implemented.");
    }
    
    @FXML protected void btnAppAuthenticator(MouseEvent event) {
    	Main.instance.overlayUI("apps/authenticator/app/AuthenticatorApp.fxml");
    }
    
    @FXML protected void btnSettings(MouseEvent event) {
    	Main.instance.overlayUI("apps/SettingsApp.fxml");
    }
    
    @FXML protected void btncoinjoin(MouseEvent event) {
    	Main.instance.overlayUI("apps/CoinjoinApp.fxml");
    }
    
    @FXML protected void btnOneName(MouseEvent event) {
    	if(Authenticator.getWalletOperation().getOnename() != null){
    		ArrayList<Object> l = new ArrayList<Object>();
			   l.add(Authenticator.getWalletOperation().getOnename().getOnename());
			   Main.instance.overlayUI("apps/OneNameApp.fxml", l);
    	}
    	else {
    		ArrayList<Object> l = new ArrayList<Object>();
    		l.add("");
    		Main.instance.overlayUI("apps/OneNameApp.fxml", l);
    	}
    }
    
    @FXML protected void btnAccounts(MouseEvent event) {
    	Main.instance.overlayUI("apps/accounts/app/AccountsApp.fxml");
    }

	@FXML protected void btnMyBitcoins(MouseEvent event) {
		Main.instance.overlayUI("apps/MyBitcoins/app/MyBitcoinsApp.fxml");
	}
    
    //#####################################
   	//
   	//	Change account
   	//
   	//#####################################
    
    public void changeAccount(String toValue) throws AccountWasNotFoundException{
    	LOG.info("Changing Authenticator account to " + toValue);
    	ATAccount acc = Authenticator.getWalletOperation().getAccountByName(toValue);
    	if(acc != null){
    		if(Authenticator.getWalletOperation().setActiveAccount(acc.getIndex()) != null)
    			updateUIForNewActiveAccount();
    	}
    	else{
    		// mmm .... can it be ?
    	}
    }
    
    public void updateUIForNewActiveAccount(){
    	LOG.info("Updating UI because of Authenticator account change");
    	updateUI();
    }
    
    /**
     * Will check if the given password can decrypt the wallet.<br>
     * If wallet is encrypted, at the end of the check will keep the wallet encrypted.<br>
     * If wallet is not encrypted or the password does decrypt the wallet, this will return true;
     * 
     * @param password
     * @return
     * @throws NoWalletPasswordException 
     */
	private boolean checkIfPasswordDecryptsWallet(BAPassword password){
		if(Authenticator.getWalletOperation().isWalletEncrypted()){
    		try{
    			Authenticator.getWalletOperation().decryptWallet(password);
    			Authenticator.getWalletOperation().encryptWallet(password);
    		}
    		catch(KeyCrypterException | WrongWalletPasswordException  e){
    			return false;
    		} 	
    	}
		
		return true;
	}
    
    //#####################################
  	//
  	//	Animations
  	//
  	//#####################################
    
    /**
     * direction should be 1 to slide out, -1 to slide back in
     * 
     * @param direction
     */
    private boolean isSpinnerVisible = true;
    private void readyToGoAnimation(int direction, @Nullable EventHandler<ActionEvent> listener) {
    	// Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), SyncPane);
    	leave.setOnFinished(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent arg0) {
				SyncPane.setVisible(false);
				stopSyncRotation();
				if(listener != null)
					listener.handle(arg0);
			}
    	});
        leave.setByY(direction==1? 80.0:-80.0);
        leave.setCycleCount(1);
        leave.setInterpolator(direction==1? Interpolator.EASE_OUT:Interpolator.EASE_IN);
        leave.play();
        if(direction == 1)
        	isSpinnerVisible = false;
    }
    
    private RotateTransition rt;
    private void startSyncRotation(){
    	if(rt == null){
    		syncProgress.setProgress(-1);
            rt = new RotateTransition(Duration.millis(3000),ivSync);
            rt.setByAngle(360);
            rt.setCycleCount(10000);
    	}
        rt.play();
    }
    private void stopSyncRotation(){
    	rt.stop();
    }
    
    private RotateTransition rt2;
    private void startAuthRotation(ImageView img){
    	rt2 = new RotateTransition(Duration.millis(3000),img);
    	rt2.setByAngle(360);
    	rt2.setCycleCount(10000);
        rt2.play();
    }
    private void stopAuthRotation(){
    	if (!Authenticator.getApplicationParams().disableSpinners){rt2.stop();}
    }
    
}