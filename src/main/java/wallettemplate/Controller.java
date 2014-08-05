package wallettemplate;

import authenticator.Authenticator;
import authenticator.BAGeneralEventsListener;
import authenticator.BAGeneralEventsListener.HowBalanceChanged;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.Utils.EncodingUtils;
import authenticator.Utils.KeyUtils;
import authenticator.Utils.CurrencyConverter.CurrencyConverterSingelton;
import authenticator.Utils.CurrencyConverter.exceptions.CurrencyConverterSingeltonNoDataException;
import authenticator.db.ConfigFile;
import authenticator.helpers.exceptions.AddressNotWatchedByWalletException;
import authenticator.helpers.exceptions.AddressWasNotFoundException;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.network.OneName;
import authenticator.operations.BAOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.operations.OperationsUtils.SignProtocol.AuthenticatorAnswerType;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.uri.BitcoinURI;
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
import wallettemplate.ControllerHelpers.SendTxHelper;
import wallettemplate.ControllerHelpers.SendTxHelper.NewAddress;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.AlertWindowController;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextFieldValidator;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;
import org.json.JSONObject;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import static wallettemplate.Main.bitcoin;
import static wallettemplate.utils.GuiUtils.checkGuiThread;
import static wallettemplate.utils.GuiUtils.crashAlert;
import static wallettemplate.utils.GuiUtils.informationalAlert;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class Controller  extends BaseUI{
	 @FXML private Label lblMinimize;
	 @FXML private Label lblClose;
	 @FXML private Button btnOverview_white;
	 @FXML private Button btnOverview_grey;
	 @FXML private Button btnSend_white;
	 @FXML private Button btnSend_grey;
	 @FXML private Button btnReceive_white;
	 @FXML private Button btnReceive_grey;
	 @FXML private Button btnTransactions_white;
	 @FXML private Button btnTransactions_grey;
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
	 @FXML private Button btnConnection0;
	 @FXML private Button btnConnection1;
	 @FXML private Button btnConnection2;
	 @FXML private Button btnConnection3;
	 @FXML private Button btnTor_grey;
	 @FXML private Button btnTor_color;
	 @FXML private Label lblStatus;
	 @FXML private Button btnRequest;
	 @FXML private Button btnClearReceivePane;
	 @FXML public ChoiceBox AccountBox;
	 @FXML private TextField txReqLabel;
	 @FXML private TextField txReqAmount;
	 @FXML private TextArea txReqMemo;
	 @FXML private ChoiceBox reqCurrencyBox;
	 @FXML public ScrollPane scrlViewTxHistory;
	 @FXML private ImageView ivLogo;
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
	 private boolean locked;
	 
	 

	//#####################################
	//
	//	Initialization Methods
	//
	//#####################################
	
    public void initialize() {
    	super.initialize(Controller.class);
    	startSyncRotation();
        
        scrlContent = new ScrollPaneContentManager().setSpacingBetweenItems(15);
        scrlpane.setFitToHeight(true);
        scrlpane.setFitToWidth(true);
        addOutput();
        scrlpane.setContent(scrlContent);
        scrlpane.setStyle("-fx-border-color: #dae0e5; -fx-background: white;");
        scrlpane.setPadding(new Insets (5,0,0,5));
        scrlpane.setFocusTraversable(false);
        syncProgress.setProgress(-1);
        lblName.setFont(Font.font(null, FontWeight.BOLD, 15));
        lblConfirmedBalance.setFont(Font.font(null, FontWeight.BOLD, 14));
        lblUnconfirmedBalance.setFont(Font.font(null, FontWeight.BOLD, 14));
        createReceivePaneButtons();
        createSendButtons();
        
        
        // Pane Control
        Tooltip.install(lblMinimize, new Tooltip("Minimize Window"));
        lblMinimize.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	Main.stage.setIconified(true);
            }
        });
        //
        Tooltip.install(lblClose, new Tooltip("Close Window"));
        lblClose.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	Main.stage.getOnCloseRequest().handle(null);
            }
        });
        
        // Peer icon
        Tooltip.install(btnConnection0, new Tooltip("Not connected to any peers"));      
        
        // transaction history scrollPane
        scrlViewTxHistoryContentManager = new ScrollPaneContentManager()
        									.setSpacingBetweenItems(20)
        									.setScrollStyle(scrlViewTxHistory.getStyle());
		scrlViewTxHistory.setContent(scrlViewTxHistoryContentManager);
		
		//Receive address
		/*AddressBox.valueProperty().addListener(new ChangeListener<String>() {
    		@Override public void changed(ObservableValue ov, String t, String t1) {
    			if(t1 != null && t1.length() > 0)
    			if (t1.substring(0,1).equals(" ")){
    				String newAdd = null;
    				AddressBox.getItems().clear();
    				for (int i=0; i<10; i++){
					try {
						newAdd = Authenticator.getWalletOperation()
								.getNextExternalAddress(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex())
								.getAddressStr();
						AddressBox.getItems().add(0,newAdd);
					} catch (Exception e) { e.printStackTrace(); }
    				}
    				AddressBox.setValue(AddressBox.getItems().get(0).toString());
    				AddressBox.getItems().addAll("                                More");
    			}
    		}    
    	});*/

		// accounts choice box
		AccountBox.valueProperty().addListener(new ChangeListener<String>() {
  			@Override 
  			public void changed(ObservableValue ov, String t, String t1) {
  				if(t1 != null && t1.length() > 0){
					AccountBox.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(new Font("Arial", 14),t1, 0.0D)+45);
					changeAccount(t1);
  				}
  			}    
	   });
		
		// wallet lock/ unlock
		Tooltip.install(btnLock, new Tooltip("Click to Unlock Wallet"));
    }
    
    /**
     * will be called after the awaitRunning event is called from the WalletAppKit
     */
    public void onBitcoinSetup() {
    	bitcoin.wallet().freshReceiveAddress();
    	bitcoin.peerGroup().addEventListener(new PeerListener());
    	TorClient tor = bitcoin.peerGroup().getTorClient();
    	tor.addInitializationListener(listener);       
    	
    }
    
   /**
     * will be called after the awaitRunning event is called from the authenticator
     */
    public void onAuthenticatorSetup() {
    	Platform.runLater(new Runnable() { 
			 @Override
			public void run() {
				/**
	    	 	 * refreshBalanceLabel will take care of downloading the currency data needed
	    	 	 */
	    	 	 setReceiveAddresses();
	    	 	 try {setTxPaneHistory();} 
	     	 	 catch (NoSuchAlgorithmException | JSONException
	 				| AddressFormatException | KeyIndexOutOfRangeException
	 				| AddressNotWatchedByWalletException e1) {e1.printStackTrace();}
	         	 try {setTxHistoryContent();} 
	        	 catch (NoSuchAlgorithmException | JSONException
					| AddressFormatException | KeyIndexOutOfRangeException
					| AddressNotWatchedByWalletException e1) {e1.printStackTrace();}
	         
	        	 setupOneName(Authenticator.getWalletOperation().getOnename());
	        	 try {refreshBalanceLabel();} 
	         	 catch (JSONException | IOException e) {e.printStackTrace();}
	         
	        	 Authenticator.addGeneralEventsListener(new AuthenticatorGeneralEvents());
	         	//Authenticator.getWalletOperation().addEventListener(new WalletListener());
	         
	        	 // Account choicebox
	        	 setAccountChoiceBox();
	         
	         	/**
	         	 * Read the comments in TCPListener#looper()
	        	 */
	         	if(Authenticator.getWalletOperation().getPendingRequestSize() > 0)
	        	 if(Authenticator.getWalletOperation().isWalletEncrypted())
	        		 lockControl(null);
	         	
	         	// lock 
	         	locked = Authenticator.getWalletOperation().isWalletEncrypted();
	         	updateLockIcon();
			}
	    });    	
    }
    
    public class AuthenticatorGeneralEvents implements BAGeneralEventsListener{
		
		@Override
		public void onNewPairedAuthenticator() {
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  setAccountChoiceBox();
			  }
			});
		}

		@Override
		public void onNewUserNamecoinIdentitySelection(AuthenticatorConfiguration.ConfigOneNameProfile profile) {
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  setupOneName(profile);
			  }
			});
			
		}
		
		@Override
		public void onBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence) {
			/**
			 * Will pop a notification when received coins are pending and built
			 */
			if(howBalanceChanged == HowBalanceChanged.ReceivedCoins){
				Platform.runLater(new Runnable() { 
					  @Override
					  public void run() {
							Image logo = new Image(Main.class.getResourceAsStream("bitcoin_logo_plain_small.png"));
							// Create a custom Notification without icon
							Notification info = new Notification("Bitcoin Authenticator Wallet", "Coins Received: " + 
									Authenticator.getWalletOperation().getTxValueSentToMe(tx).toFriendlyString() + 
									"\n" + 
									"Status: " + (confidence == ConfidenceType.PENDING? "Pending":"Confirmed"), logo);
							
							// Show the custom notification
							Notifier.INSTANCE.notify(info);
					  }
					});
			}
			
			/**
			 * Will pop a notification when sent coins are confirmed
			 */
			if(howBalanceChanged == HowBalanceChanged.SentCoins && confidence == ConfidenceType.BUILDING){
				Platform.runLater(new Runnable() { 
					  @Override
					  public void run() {
							Image logo = new Image(Main.class.getResourceAsStream("bitcoin_logo_plain_small.png"));
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
			
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				try {refreshBalanceLabel();} 
				catch (JSONException | IOException e) {e.printStackTrace();}
		        setReceiveAddresses();
		        try {setTxPaneHistory();} 
		    	catch (NoSuchAlgorithmException | JSONException
						| AddressFormatException | KeyIndexOutOfRangeException
						| AddressNotWatchedByWalletException e1) {e1.printStackTrace();}
		        try {setTxHistoryContent();} 
		        catch (NoSuchAlgorithmException | JSONException
						| AddressFormatException | KeyIndexOutOfRangeException
						| AddressNotWatchedByWalletException e) {e.printStackTrace();}
			  }
			});
			
		}

		@Override
		public void onNewStandardAccountAdded() {
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  setAccountChoiceBox();
			  }
			});
		}

		@Override
		public void onAccountDeleted(int accountIndex) {
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  setAccountChoiceBox();
			  }
			});
		}

		@Override
		public void onAccountBeenModified(int accountIndex) {
			Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  setAccountChoiceBox();
			  }
			});
			
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
						if(answerType == AuthenticatorAnswerType.Authorized)
							notifStr = "Authenticator Authorized a Transaction:\n";
						else if(answerType == AuthenticatorAnswerType.NotAuthorized)
							notifStr = "Authenticator Refused To Sign a Transaction:\n";
						else
							return;
						
						Image logo = new Image(Main.class.getResourceAsStream("bitcoin_logo_plain_small.png"));
				    	// Create a custom Notification without icon
				    	Notification info = new Notification("Bitcoin Authenticator Wallet", notifStr , logo);
				    	// Show the custom notification
				    	Notifier.INSTANCE.notify(info);
				  }
				});
			
		}
    }
    
    public class ProgressBarUpdater extends DownloadListener {
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
        	Platform.runLater(new Runnable() { 
				  @Override
				  public void run() {
				     lblStatus.setText("Synchronizing Blockchain");
				  }
				});
            super.progress(pct, blocksSoFar, date);
            Platform.runLater(() -> syncProgress.setProgress(pct / 100.0));
        }

        @Override
        protected void doneDownload() {
            super.doneDownload();
            Platform.runLater(new Runnable(){
				@Override
				public void run() {
					 readyToGoAnimation(1, null);
				}
	        });
           
        }
    }

    public ProgressBarUpdater progressBarUpdater() {
        return new ProgressBarUpdater();
    }
    
    public class TorListener implements TorInitializationListener {

		@Override
		public void initializationProgress(String message, int percent) {
		}

		@Override
		public void initializationCompleted() {
			Platform.runLater(new Runnable() { 
				  @Override
				  public void run() {
					 lblStatus.setText("Connecting To Network");
				     btnTor_grey.setVisible(false);
				     btnTor_color.setVisible(true);
				     Tooltip.install(btnTor_color, new Tooltip("Connected to Tor"));
				  }
				});
		}
    }
    
    /*public class WalletListener extends AbstractWalletEventListener {

		@Override
		public void onWalletChanged(Wallet wallet) {
			try {setTxPaneHistory();} 
			catch (NoSuchAlgorithmException | JSONException
					| AddressFormatException
					| KeyIndexOutOfRangeException
					| AddressNotWatchedByWalletException e) {e.printStackTrace();}	
		}
    }*/
    
    
    public class PeerListener extends AbstractPeerEventListener {
    	
    	@Override
        public void onPeerConnected(Peer peer, int peerCount) {
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

        @Override
        public void onPeerDisconnected(Peer peer, int peerCount) {
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
	   if (Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountType() == WalletAccountType.AuthenticatorAccount){
		   if(Authenticator.areAllNetworkRequirementsAreFullyRunning() == false){
			   Platform.runLater(new Runnable() { 
				   @Override
				   public void run() {
					   Dialogs.create()
					   .owner(Main.stage)
					   .title("Error !")
					   .masthead("Some network requirements are not available, some functionalities may be compromised")
					   .message("If this problem repeats, your router may not allow port mapping.")
					   .showInformation();   
				   }
			   });
	   		}
	   }
	   LOG.info("Setting accounts checkbox");
	   List<ATAccount> all = new ArrayList<ATAccount>();
	   all = Authenticator.getWalletOperation().getAllAccounts();
	   
	   AccountBox.getItems().clear();
	   for(ATAccount acc:all){
		   AccountBox.getItems().add(acc.getAccountName());
	   }
	   AccountBox.setTooltip(new Tooltip("Select active account"));
	   
	   AccountBox.setValue(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountName());
	
	   AccountBox.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(new Font("Arial", 14),AccountBox.getValue().toString(), 0.0D)+45);
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
	   if (locked){
		   displayLockDialog();
	   }
	   /**
	    * Encrypt
	    */
	   else 
	   {
		   //Authenticator.AUTHENTICATOR_PW="";
		   if(Authenticator.AUTHENTICATOR_PW.length() == 0)
			   displayLockDialog();
		   else
			   Authenticator.getWalletOperation().encryptWallet(Authenticator.AUTHENTICATOR_PW);
		   locked = true;
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
						   Authenticator.getWalletOperation().decryptWallet(pw.getText());
						   //Authenticator.getWalletOperation().encryptWallet(pw.getText());
						   Authenticator.AUTHENTICATOR_PW = pw.getText();
						   overlay.done();
						   locked = false;
						   updateLockIcon();
					   }
					   catch(KeyCrypterException  e){
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
				   if(Authenticator.AUTHENTICATOR_PW.length() == 0){
					   Authenticator.getWalletOperation().encryptWallet(pw.getText());
					   Authenticator.AUTHENTICATOR_PW = pw.getText();
					   overlay.done();
					   locked = true;
					   updateLockIcon();
				   }
			   }
		   }
	   });   
   }
   
   private void updateLockIcon(){
	   if(locked){
		   Image imglocked = new Image(Main.class.getResource("btnLocked.png").toString());
		   ImageView img = new ImageView(imglocked);
		   btnLock.setGraphic(img);
		   overviewHBox.setMargin(btnLock, new Insets(0,0,0,0));
		   Tooltip.install(btnLock, new Tooltip("Click to Unlock Wallet"));
	   }
	   else
	   {
		   Image unlocked = new Image(Main.class.getResource("btnUnlocked.png").toString());
		   ImageView img = new ImageView(unlocked);
		   btnLock.setGraphic(img);
		   overviewHBox.setMargin(btnLock, new Insets(-2,0,0,0));
		   Tooltip.install(btnLock, new Tooltip("Click to Lock Wallet"));
	   }
   }
   
   public void setupOneName(AuthenticatorConfiguration.ConfigOneNameProfile one){
	   LOG.info("Setting oneName avatar image");
	   // get image
	   File imgFile = null;
	   BufferedImage bimg = null;
	   Image img = null;
	   try {
		    imgFile = new File(one.getOnenameAvatarFilePath());
			bimg = ImageIO.read(imgFile);
			img = OneName.createImage(bimg);
	   } catch (Exception e) { // do nothing
	   }
	   
	   if(img != null && one != null)
		   setUserProfileAvatarAndName(img,one.getOnenameFormatted());	   
   }
   
	public void setUserProfileAvatarAndName(Image img, String name) {
		ivAvatar.setImage(img);
		//
		lblName.setText("Welcome back, " + name);
		lblName.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(lblName.getFont(),
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
    }
    
    @SuppressWarnings("static-access")
	public void refreshBalanceLabel() throws JSONException, IOException {
    	LOG.info("Refreshing balance");
    	Coin unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
    	Coin confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
  
        //final Coin total = confirmed.add(unconfirmed);
        lblConfirmedBalance.setText(confirmed.toFriendlyString());
        lblUnconfirmedBalance.setText(unconfirmed.toFriendlyString());
        new CurrencyConverterSingelton(new CurrencyConverterSingelton.CurrencyConverterListener(){
			@Override
			public void onFinishedGettingCurrencyData(CurrencyConverterSingelton currencies) {
				Platform.runLater(new Runnable() {
				      @Override public void run() {
							attachBalanceToolTip();
				      }
				    });
			}

			@Override
			public void onErrorGettingCurrencyData(Exception e) {
				Platform.runLater(new Runnable() {
				      @Override public void run() {
				    	  Dialogs.create()
					        .owner(Main.stage)
					        .title("Error !")
					        .masthead("Cannot Download Currency Data")
					        .message("Some functionalities may be compromised")
					        .showInformation();   
				      }
				    });
			}
        });
        
    }
    
    private void attachBalanceToolTip(){
    	  Coin unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
	      Coin confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
		  
		  // Confirmed
		  //double conf = Double.parseDouble(confirmed.toPlainString())*last;
		  double conf = CurrencyConverterSingelton.USD.convertToCurrency(Double.parseDouble(confirmed.toPlainString()));
		  Tooltip.install(lblConfirmedBalance, new Tooltip(String.valueOf(conf) + " USD"));
		  
		  // Unconfirmed
		  //double unconf = Double.parseDouble(unconfirmed.toPlainString())*last;
		  double unconf = CurrencyConverterSingelton.USD.convertToCurrency(Double.parseDouble(unconfirmed.toPlainString()));
		  Tooltip.install(lblUnconfirmedBalance, new Tooltip(String.valueOf(unconf) + " USD"));
    }
    
    @SuppressWarnings("unused")
	public void setTxHistoryContent() throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException{
    	LOG.info("Setting Tx History in Overview Pane");
    	scrlViewTxHistoryContentManager.clearAll();
    	//List<Transaction> txAll = Authenticator.getWalletOperation().getRecentTransactions();
    	ArrayList<Transaction> txAll = Authenticator.getWalletOperation().filterTransactionsByAccount(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
    	if (txAll.size()==0){
    		HBox mainNode = new HBox();
    		Label l = new Label("                    No transaction history   ");
    		l.setStyle("-fx-font-weight: SEMI_BOLD;");
    		l.setTextFill(Paint.valueOf("#6e86a0"));
    		l.setFont(Font.font(13));
    		mainNode.getChildren().add(l);
    		Image inout = new Image(Main.class.getResourceAsStream("in-out.png"));
    		ImageView arrows = new ImageView(inout);
    		mainNode.getChildren().add(arrows);
    		scrlViewTxHistoryContentManager.addItem(mainNode);	
    	}
    	ConfigFile config = Authenticator.getWalletOperation().configFile;
    	ArrayList<String> savedTXIDs = config.getSavedTxidList();
    	int size = txAll.size();
    	int n;
    	if (size < 10) {n=size;}
    	else {size =  10;}
    	for(int i=0; i<size; i++){
    		String txid = txAll.get(i).getHashAsString();
    		String tip = "";
    		// build node 
    		HBox mainNode = new HBox();
        		
    		// left box
    		VBox leftBox = new VBox();
    		Label l1 = new Label();
    		l1.setStyle("-fx-font-weight: SEMI_BOLD;");
    		l1.setTextFill(Paint.valueOf("#6e86a0"));
    		l1.setFont(Font.font(13));
    		l1.setText(txAll.get(i).getUpdateTime().toLocaleString()); 
    		tip += "Txid: " + txid + "\n";
    		leftBox.getChildren().add(l1);
    		
    		Label l2 = new Label();
    		l2.setStyle("-fx-font-weight: SEMI_BOLD;");
    		l2.setTextFill(Paint.valueOf("#6e86a0"));
    		l2.setFont(Font.font(11));
    		for (int a=0; a<savedTXIDs.size(); a++){
    			if (savedTXIDs.get(a).equals(txAll.get(i).getHashAsString())){
    				if(!config.getSavedDescription(a).equals("")){txid = config.getSavedDescription(a);}
    			}
    		}
    		if (txid.length()>20){txid = txid.substring(0, 20) + "...";}
    		l2.setText(txid); 
    		tip += "When: " + txAll.get(i).getUpdateTime().toLocaleString() + "\n";
    		leftBox.getChildren().add(l2);
    		
    		mainNode.getChildren().add(leftBox);
    		
    		// right box
    		VBox rightBox = new VBox();
    		HBox content = new HBox();
    		rightBox.setPadding(new Insets(0,0,0,40));
    		Label l3 = new Label();
    		l3.setStyle("-fx-font-weight: SEMI_BOLD;");
    		l3.setPadding(new Insets(0,5,0,0));
    		//check is it receiving or sending
    		Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(txAll.get(i));//txAll.get(i).getValueSentToMe(Main.bitcoin.wallet());
    		Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(txAll.get(i));//txAll.get(i).getValueSentFromMe(Main.bitcoin.wallet());
    		Image in = new Image(Main.class.getResourceAsStream("in.png"));
    		Image out = new Image(Main.class.getResourceAsStream("out.png"));
    		ImageView arrow = null;
    		if (exit.compareTo(Coin.ZERO) > 0){ // means i sent coins
    			l3.setTextFill(Paint.valueOf("#ea4f4a"));
    			l3.setText("-" + exit.subtract(enter).toFriendlyString()); // get total out minus enter to subtract change amount
    			tip += "Amount: -" + exit.subtract(enter).toFriendlyString() + "\n";	
    			arrow = new ImageView(out);
    		}
    		else { // i only received coins
    			l3.setTextFill(Paint.valueOf("#98d947"));
    			l3.setText(enter.toFriendlyString());
    			tip+= "Amount: " + enter.toFriendlyString() + "\n";
    			arrow = new ImageView(in);
    		}
     		l3.setFont(Font.font(13));
    		content.getChildren().add(l3);
    		content.getChildren().add(arrow);
    		rightBox.getChildren().add(content);
    		mainNode.getChildren().add(rightBox);
    		Tooltip.install(mainNode, new Tooltip(tip));
    		
    		// add to scroll
    		scrlViewTxHistoryContentManager.addItem(mainNode);		
    	}
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
                  if(newValue.intValue() > oldValue.intValue()){
                      char ch = txFee.getText().charAt(oldValue.intValue());  
                      //Check if the new character is the number or other's
                      if(!(ch >= '0' && ch <= '9') && ch != '.'){       
                           //if it's not number then just setText to previous one
                           txFee.setText(txFee.getText().substring(0,txFee.getText().length()-1)); 
                      }
                 }
            }
        });
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
    	scrlContent.clearAll(); addOutput();
    	txFee.clear();
    }
    
    @FXML protected void btnClearSendPaneReleased(MouseEvent event) {
    	btnClearSendPane.setStyle("-fx-background-color: #999999;");
    } 
    
    private boolean ValidateTx()
    {
    	// fee
    	Coin fee = Coin.ZERO;
		if (txFee.getText().equals("")){fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
        else 
        {
        	double f = (double) Double.parseDouble(txFee.getText())*100000000;
        	fee = Coin.valueOf((long)f);
        }
    	try {
			return SendTxHelper.ValidateTx(scrlContent, fee);
		} catch (NoSuchAlgorithmException | JSONException
				| AddressFormatException | IOException e) { 
			e.printStackTrace();
			return false;
		}
    }
    
    @FXML protected void SendTx(ActionEvent event) throws CurrencyConverterSingeltonNoDataException{
    	if(!ValidateTx()){
    		Dialogs.create()
	        .owner(Main.stage)
	        .title("Error")
	        .masthead("Something Is not Right ...")
	        .message("Make Sure:\n" +
					"  1) You entered correct values in all fields\n"+
					"  2) You have sufficient funds to cover your outputs\n"+
					"  3) Outputs amount to at least the dust value(" + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.toString() + ")\n")
	        .showError();  
    	}
    	else{
    		// collect Tx outputs
    		ArrayList<String> OutputAddresses = new ArrayList<String>();
    		ArrayList<TransactionOutput> to = new ArrayList<TransactionOutput>();
    		Coin outAmount = Coin.valueOf(0);
        	for(Node n:scrlContent.getChildren())
        	{
        		NewAddress na = (NewAddress)n;
        		Address add;
				try {
					add = new Address(Authenticator.getWalletOperation().getNetworkParams(), na.txfAddress.getText());
					OutputAddresses.add(add.toString());
					double amount;
					if (na.cbCurrency.getValue().toString().equals("BTC")){
						amount = (double) Double.parseDouble(na.txfAmount.getText())*100000000;
					}
					else {		
						CurrencyConverterSingelton.CANNOT_EXECUTE_ASYNC_SO_CHECK_IS_READY();
						amount = CurrencyConverterSingelton.USD.convertToBTC(Double.parseDouble(na.txfAmount.getText()));
					}
					long satoshis = (long) amount;
					if (Coin.valueOf(satoshis).compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
						outAmount = outAmount.add(Coin.valueOf(satoshis));
						TransactionOutput out = new TransactionOutput(Authenticator.getWalletOperation().getNetworkParams(),
											        				null, 
											        				Coin.valueOf(satoshis), 
											        				add);
						to.add(out);
					}
					else 
						;//TODO
					
				} catch (AddressFormatException e) { 
					e.printStackTrace();
					Platform.runLater(new Runnable() {
					      @Override public void run() {
					    	  Dialogs.create()
						        .owner(Main.stage)
						        .title("Error !")
						        .masthead("Cannot Create Tx")
						        .message("")
						        .showInformation();   
					      }
					    });
				}        		
        	}
        	
        	try{
//        		get fee
            	Coin fee = Coin.ZERO;
        		if (txFee.getText().equals("")){fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
                else 
                {
                	double f = (double) Double.parseDouble(txFee.getText())*100000000;
                	fee = Coin.valueOf((long)f);
                }
        		
        		// get input outputs / change address
        		ArrayList<TransactionOutput> outputs = Authenticator.
        				getWalletOperation().
        				getUnspentOutputsForAccount(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
        		String changeaddr = Authenticator.getWalletOperation()
    								.getNextExternalAddress(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex())
    								.getAddressStr();
        		   
        		// complete Tx
        		Transaction tx = Authenticator.getWalletOperation().mkUnsignedTxWithSelectedInputs(outputs, 
    						to,
    						fee,
    						changeaddr,
    						Authenticator.getWalletOperation().getNetworkParams());
    						
    			//
    			displayTxOverview(tx,
    					OutputAddresses, 
    					to,
    					changeaddr, 
    					outAmount, 
    					fee, 
    					Authenticator.getWalletOperation().getTxValueSentFromMe(tx).subtract(Authenticator.getWalletOperation().getTxValueSentToMe(tx)));
        	}
        	catch(Exception e){
        		e.printStackTrace();
        		Platform.runLater(new Runnable() {
				      @Override public void run() {
				    	  Dialogs.create()
					        .owner(Main.stage)
					        .title("Error !")
					        .masthead("Cannot Create Tx")
					        .message("")
					        .showInformation();   
				      }
				    });
        	}
        	
    	}
    }
    
    private void displayTxOverview(Transaction tx, 
    		ArrayList<String> OutputAddresses, 
    		ArrayList<TransactionOutput> to, 
    		String changeaddr,
    		Coin outAmount,
    		Coin fee,
    		Coin leavingWallet){
    	//Display Transaction Overview
		Pane pane = new Pane();
		final Main.OverlayUI<Controller> overlay = Main.instance.overlayUI(pane, Main.controller);
		pane.setMaxSize(600, 360);
		pane.setStyle("-fx-background-color: white;");
		pane.setEffect(new DropShadow());
		VBox v = new VBox();
		Label lblOverview = new Label("Transaction Overview");
		v.setMargin(lblOverview, new Insets(10,0,10,20));
		lblOverview.setFont(Font.font(null, FontWeight.BOLD, 18));
		ListView lvTx= new ListView();
		lvTx.setStyle("-fx-background-color: transparent;");
		v.setMargin(lvTx, new Insets(0,0,0,20));
		lvTx.setPrefSize(560, 270);
		ObservableList<TextFlow> textformatted = FXCollections.<TextFlow>observableArrayList();
		Text inputtext = new Text("Inputs:                     ");
		inputtext.setStyle("-fx-font-weight:bold;");
		Coin inAmount = Coin.valueOf(0);
		TextFlow inputflow = new TextFlow();
		inputflow.getChildren().addAll(inputtext);
		ArrayList<Text> intext = new ArrayList<Text>();
		for (int b=0; b<tx.getInputs().size(); b++){
			Text inputtext2 = new Text("");
			Text inputtext3 = new Text("");
			inputtext3.setFill(Paint.valueOf("#98d947"));
			inputtext2.setText(tx.getInput(b).getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString() + " ");
			intext.add(inputtext2);
			inAmount = inAmount.add(tx.getInputs().get(b).getValue());
			inputtext3.setText(tx.getInput(b).getValue().toFriendlyString());
			if (b<tx.getInputs().size()-1){
				inputtext3.setText(inputtext3.getText() + "\n                                    ");
			}
			intext.add(inputtext3);
		}
		for (Text t : intext){inputflow.getChildren().addAll(t);}
		textformatted.add(inputflow);
		TextFlow spaceflow = new TextFlow();
		Text space = new Text(" ");
		spaceflow.getChildren().addAll(space);
		textformatted.add(spaceflow);
		Text outputtext = new Text("Outputs:                  ");
		outputtext.setStyle("-fx-font-weight:bold;");
		TextFlow outputflow = new TextFlow();
		outputflow.getChildren().addAll(outputtext);
		ArrayList<Text> outtext = new ArrayList<Text>();
		for (int a=0; a < OutputAddresses.size(); a++){
			Text outputtext2 = new Text("");
			Text outputtext3 = new Text("");
			outputtext3.setFill(Paint.valueOf("#f06e6e"));
			outputtext2.setText(OutputAddresses.get(a) + " ");
			outtext.add(outputtext2);
			outputtext3.setText(to.get(a).getValue().toFriendlyString());
			if (a<OutputAddresses.size()-1){
				outputtext3.setText(outputtext3.getText() + "\n                                     ");
			}
			outtext.add(outputtext3);
		}
		for (Text t : outtext){outputflow.getChildren().addAll(t);}
		textformatted.add(outputflow);
		textformatted.add(spaceflow);
		Text changetext = new Text("Change:                   ");
		changetext.setStyle("-fx-font-weight:bold;");
		Text changetext2 = new Text(changeaddr + " ");
		Text changetext3 = new Text(((inAmount.subtract(outAmount)).subtract(fee)).toFriendlyString());
		changetext3.setFill(Paint.valueOf("#98d947"));
		TextFlow changeflow = new TextFlow();
		changeflow.getChildren().addAll(changetext, changetext2,changetext3);
		textformatted.add(changeflow);
		textformatted.add(spaceflow);
		Text feetext = new Text("Fee:                         ");
		feetext.setStyle("-fx-font-weight:bold;");
		Text feetext2 = new Text(fee.toFriendlyString());
		feetext2.setFill(Paint.valueOf("#f06e6e"));
		TextFlow feeflow = new TextFlow();
		feeflow.getChildren().addAll(feetext, feetext2);
		textformatted.add(feeflow);
		textformatted.add(spaceflow);
		Text leavingtext = new Text("Leaving Wallet:       ");
		leavingtext.setStyle("-fx-font-weight:bold;");
		Text leavingtext2 = new Text("-" + leavingWallet.toFriendlyString());
		leavingtext2.setFill(Paint.valueOf("#f06e6e"));
		TextFlow leavingflow = new TextFlow();
		leavingflow.getChildren().addAll(leavingtext, leavingtext2);
		textformatted.add(leavingflow);
		lvTx.setItems(textformatted);
		Button btnCancel = new Button("Cancel");
		btnCancel.getStyleClass().add("clear-button");
		btnCancel.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnCancel.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnCancel.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnCancel.setStyle("-fx-background-color: #999999;");
            }
        });
		Button btnConfirm = new Button("Send Transaction");
		btnConfirm.getStyleClass().add("custom-button");
        btnConfirm.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnConfirm.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
        btnConfirm.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnConfirm.setStyle("-fx-background-color: #199bd6;");
            }
        });
		PasswordField password = new PasswordField();
		password.setPrefWidth(350);
		password.setStyle("-fx-border-color: #dae0e5; -fx-background-color: white; -fx-border-radius: 2;");
		if (Authenticator.AUTHENTICATOR_PW.equals("")){
			password.setDisable(false);
			password.setPromptText("Enter Password");
			}
		else {
			password.setDisable(true);
			password.setPromptText("Wallet is unlocked");
		}
		VBox successVbox = new VBox();
		Image rocket = new Image(Main.class.getResource("rocket.png").toString());
		ImageView img = new ImageView(rocket);
		Label txid = new Label();
		Label lblTxid = new Label("Transaction ID:");
		Button btnContinue = new Button("Continue");
		btnContinue.getStyleClass().add("custom-button");
        btnContinue.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnContinue.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
        btnContinue.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnContinue.setStyle("-fx-background-color: #199bd6;");
            }
        });
		txid.setText(tx.getHashAsString());
		final ContextMenu contextMenu = new ContextMenu();
		MenuItem item1 = new MenuItem("Copy");
		item1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				ClipboardContent content = new ClipboardContent();
				content.putString(txid.getText().toString());
				clipboard.setContent(content);
			}
		});
		contextMenu.getItems().addAll(item1);
		txid.setContextMenu(contextMenu);
		successVbox.getChildren().add(img);
		successVbox.getChildren().add(lblTxid);
		successVbox.getChildren().add(txid);
		successVbox.getChildren().add(btnContinue);
		successVbox.setVisible(false);
		successVbox.setAlignment(Pos.CENTER);
		successVbox.setPrefWidth(600);
		successVbox.setPadding(new Insets(15,0,0,0));
		successVbox.setMargin(lblTxid, new Insets(15,0,0,0));
		successVbox.setMargin(btnContinue, new Insets(15,0,0,0));
		pane.getChildren().add(successVbox);
		btnConfirm.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	if(!checkIfPasswordDecryptsWallet(password.getText()))
            		return;
            	try {
        			Animation ani = GuiUtils.fadeOut(v);
        			GuiUtils.fadeIn(successVbox);
        			String to = "";
        			if (OutputAddresses.size()==1){
        				if (Authenticator.getWalletOperation().isWatchingAddress(OutputAddresses.get(0))){
        					ATAddress add = Authenticator.getWalletOperation().findAddressInAccounts(OutputAddresses.get(0));
        					int index = add.getAccountIndex();
        					if (index==Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex()){
        						to = "Internal Transfer";
        					}
        					else {to = "Transfer to " + Authenticator.getWalletOperation().getAccount(index).getAccountName();}
        				}
        				else {
        					to = OutputAddresses.get(0) ;
        				}
        			}
        			else {to = "Multiple";}
        			v.setVisible(false);
        			if (broadcast(tx,to, Authenticator.AUTHENTICATOR_PW) == true) {
        				successVbox.setVisible(true);
        			}
        			else {
        				overlay.done();
                    	txMsgLabel.clear();
                    	scrlContent.clearAll(); addOutput();
                    	txFee.clear();
                    	// Notify user
                    	Dialogs.create()
        		        .owner(Main.stage)
        		        .title("Error !")
        		        .masthead("Could not add operation")
        		        .showInformation();
        			}
        				
        		} 
        		catch (NoSuchAlgorithmException
        				| AddressWasNotFoundException | JSONException
        				| AddressFormatException
        				| KeyIndexOutOfRangeException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
            }
        });
		HBox h = new HBox();
		h.setPadding(new Insets(10,0,0,20));
		h.setMargin(btnCancel, new Insets(0,5,0,10));
		h.setMargin(password, new Insets(-1,0,0,0));
		h.getChildren().add(password);
		h.getChildren().add(btnCancel);
		h.getChildren().add(btnConfirm);
		v.getChildren().add(lblOverview);
		v.getChildren().add(lvTx);
		v.getChildren().add(h);
		pane.getChildren().add(v);
		btnContinue.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	/*if(!Authenticator.getWalletOperation().isWalletEncrypted()){
            		if (Authenticator.AUTHENTICATOR_PW.equals("")){Authenticator.getWalletOperation().encryptWallet(password.getText());}
            		else {Authenticator.getWalletOperation().encryptWallet(Authenticator.AUTHENTICATOR_PW);}
            	}*/
            	overlay.done();
            	txMsgLabel.clear();
            	scrlContent.clearAll(); addOutput();
            	txFee.clear();
            	
            }
        });
		btnCancel.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
				public void handle(MouseEvent event) {
				overlay.done();
			}
		});
    }
    	
    public boolean broadcast (Transaction tx, String to, String WALLET_PW) throws NoSuchAlgorithmException, AddressWasNotFoundException, JSONException, AddressFormatException, KeyIndexOutOfRangeException {
    	return SendTxHelper.broadcastTx(tx, txMsgLabel.getText(), to, WALLET_PW,new OnOperationUIUpdate(){
			@Override
			public void onBegin(String str) { }

			@Override
			public void statusReport(String report) {

			}

			@Override
			public void onFinished(String str) {
				/**
				 * will notify user in AuthenticatorGeneralEvents#onBalanceChange
				 */
			}

			@Override
			public void onError(Exception e, Throwable t) {
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
				    	  Dialogs.create()
					        .owner(Main.stage)
					        .title("Error")
					        .masthead("Error broadcasting Tx")
					        .message(desc)
					        .showError();   
				      }
				    });
			}
		});
    }	
    
    
    @FXML protected void addTxOutput() {
    	addOutput();
    }
    
    private void addOutput() {
    	Class[] parameterTypes = new Class[1];
        parameterTypes[0] = int.class;
        Method removeOutput;
		try {
			removeOutput = Controller.class.getMethod("removeOutput", parameterTypes);
			NewAddress na = new SendTxHelper.NewAddress(scrlContent.getCount()).setCancelOnMouseClick(this,removeOutput);
			scrlContent.addItem(na);
			scrlpane.setContent(scrlContent);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
    }
    
    public void removeOutput(int index) {
    	scrlContent.removeNodeAtIndex(index);
    	// TODO - make it better !
    	for(Node n:scrlContent.getChildren()){
    		NewAddress na = (NewAddress)n;
    		if(na.index < index)
    			continue;
    		na.index--;
    	}
    	scrlpane.setContent(scrlContent);
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
						int accountID = Authenticator.getWalletOperation().getActiveAccount().getPairedAuthenticator().getWalletAccountIndex();
						ATAddress ca = Authenticator.getWalletOperation().findAddressInAccounts(AddressBox.getValue().toString());
						int index = ca.getKeyIndex();
						ECKey key = Authenticator.getWalletOperation().getPubKeyFromAccount(accountID, HierarchyAddressTypes.External, index, false);
						outPut = EncodingUtils.bytesToHex(key.getPubKey());
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
						
						outPut = "Authenticator Pubkey: " + EncodingUtils.bytesToHex(authKey.getPubKey()) + "\n" + 
								 "Wallet Pubkey: " + EncodingUtils.bytesToHex(walletKey.getPubKey());
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
    	Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				 AddressBox.getItems().clear();
		    	int accountIdx = Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex();
		    	ArrayList<String> add = new ArrayList<String>();
				try {
					for (int i=0; i<10; i++){
						String newAdd = Authenticator.getWalletOperation()
									.getNextExternalAddress(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex())
									.getAddressStr();
						
						add.add(newAdd);	
					}
						
					for (String address : add){
			    		AddressBox.getItems().add(address);
			    	}
					AddressBox.setValue(add.get(0));
				} catch (NoSuchAlgorithmException | JSONException
						| AddressFormatException e) { e.printStackTrace(); } catch (Exception e) { e.printStackTrace(); }
			  }
		});
    }       
    
    //#####################################
   	//
   	//	Transactions Pane
   	//
   	//#####################################
    
    public void setTxPaneHistory() throws NoSuchAlgorithmException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AddressNotWatchedByWalletException{
    	ArrayList<Transaction> history = Authenticator.getWalletOperation().filterTransactionsByAccount(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
    	ConfigFile config = Authenticator.getWalletOperation().configFile;
    	ArrayList<String> savedTXIDs = config.getSavedTxidList();
    	final ObservableList<TableTx> txdata = FXCollections.observableArrayList();
    	for (Transaction tx : history){
    		Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(tx);
    		Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(tx);
    		Image in = new Image(Main.class.getResourceAsStream("in.png"));
    		Image out = new Image(Main.class.getResourceAsStream("out.png"));
    		ImageView arrow = null;
    		String amount = "";
    		String toFrom = "multiple";
    		if (exit.compareTo(Coin.ZERO) > 0){ // means i sent coins
    			arrow = new ImageView(out);
    			amount = exit.subtract(enter).toFriendlyString();
    		}
    		else { // i only received coins
    			arrow = new ImageView(in);
    			amount = enter.toFriendlyString();
    			if (tx.getInputs().size()==1){
    				toFrom = tx.getInput(0).getFromAddress().toString();
    			}
    		}
    		String desc = tx.getHashAsString();
    		String date = tx.getUpdateTime().toLocaleString();
    		for (int i=0; i<savedTXIDs.size(); i++){
    			if (savedTXIDs.get(i).equals(tx.getHashAsString())){
    				desc = config.getSavedDescription(i);
    			}
    		}
    		for (int i=0; i<savedTXIDs.size(); i++){
    			if (savedTXIDs.get(i).equals(tx.getHashAsString())){
    				toFrom = config.getSavedToFrom(i);
    			}
    		}
    		String confirmations = String.valueOf(tx.getConfidence().getDepthInBlocks());
    		TableTx transaction = new TableTx(tx, tx.getHashAsString(), confirmations, arrow, date, toFrom, desc, amount);
    		txdata.add(transaction);
    	}
    	colConfirmations.setCellValueFactory(new PropertyValueFactory<TableTx,String>("confirmations"));
    	colInOut.setCellValueFactory(new PropertyValueFactory<TableTx,ImageView>("inOut"));
    	colDate.setCellValueFactory(new PropertyValueFactory<TableTx,String>("date"));
    	colToFrom.setCellValueFactory(new PropertyValueFactory<TableTx,String>("toFrom"));
    	colToFrom.setCellFactory(TextFieldTableCell.forTableColumn());
    	colToFrom.setOnEditCommit(
    	    new EventHandler<CellEditEvent<TableTx, String>>() {
    	        @Override
    	        public void handle(CellEditEvent<TableTx, String> t) {
    	            ((TableTx) t.getTableView().getItems().get(
    	                t.getTablePosition().getRow())
    	                ).setToFrom(t.getNewValue());
    	            String txid = ((TableTx) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).getTxid();
    	            String desc = ((TableTx) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).getDescription();
    	            String toFrom = ((TableTx) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).getToFrom();
    	            if (savedTXIDs.size()==0){
    	            	try {config.writeNextSavedTxData(txid, toFrom, desc);} 
    	            	catch (IOException e) {e.printStackTrace();}
    	            }
    	            else {
    	            	if (savedTXIDs.contains(txid)){
    	            		for (int i=0; i<savedTXIDs.size(); i++){
    	            			if (savedTXIDs.get(i).equals(txid)){
    	            				try {config.writeSavedTxData(i, txid, toFrom, desc);} 
    	            				catch (IOException e) {e.printStackTrace();}
    	            			}
    	            		}
    	            	}
    	            	else {
    	            		try {config.writeNextSavedTxData(txid, toFrom, desc);} 
    	            		catch (IOException e) {e.printStackTrace();}
    	            	}   
    	            }
    	        }
    	    }
    	);
    	colDescription.setCellValueFactory(new PropertyValueFactory<TableTx,String>("description"));
    	colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
    	colDescription.setOnEditCommit(
    	    new EventHandler<CellEditEvent<TableTx, String>>() {
    	        @Override
    	        public void handle(CellEditEvent<TableTx, String> t) {
    	            ((TableTx) t.getTableView().getItems().get(
    	                t.getTablePosition().getRow())
    	                ).setDescription(t.getNewValue());
    	            String txid = ((TableTx) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).getTxid();
    	            String desc = ((TableTx) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).getDescription();
    	            String toFrom = ((TableTx) t.getTableView().getItems().get(
        	                t.getTablePosition().getRow())
        	                ).getToFrom();
    	            if (savedTXIDs.size()==0){
    	            	try {config.writeNextSavedTxData(txid, toFrom, desc);} 
    	            	catch (IOException e) {e.printStackTrace();}
    	            }
    	            else {
    	            	if (savedTXIDs.contains(txid)){
    	            		for (int i=0; i<savedTXIDs.size(); i++){
    	            			if (savedTXIDs.get(i).equals(txid)){
    	            				try {config.writeSavedTxData(i, txid, toFrom, desc);} 
    	            				catch (IOException e) {e.printStackTrace();}
    	            			}
    	            		}
    	            	}
    	            	else {
    	            		try {config.writeNextSavedTxData(txid, toFrom, desc);} 
    	            		catch (IOException e) {e.printStackTrace();}
    	            	}   
    	            }
    	            try {setTxHistoryContent();} 
    	            catch (NoSuchAlgorithmException | JSONException
							| AddressFormatException
							| KeyIndexOutOfRangeException
							| AddressNotWatchedByWalletException e) {e.printStackTrace();}
    	        }
    	    }
    	);
    	colAmount.setCellValueFactory(new PropertyValueFactory<TableTx,String>("amount"));
    	txTable.setItems(txdata);
    	txTable.setEditable(true);
    	txTable.setOnMouseClicked(new EventHandler<MouseEvent>(){
    		@Override
    		public void handle(MouseEvent event) {
    			if (event.getClickCount()==2){
    			 @SuppressWarnings("rawtypes")
    			  ObservableList<TablePosition> cells = txTable.getSelectionModel().getSelectedCells();
    			  for( TablePosition< TableTx, ? > cell : cells )
    			  {
    			     if(!(cell.getColumn()==3) && !(cell.getColumn()==4)){
    			    	 TableTx txObj= (TableTx)txTable.getItems().get(cell.getRow());
    			    	 Transaction tx = txObj.getTransaction();
    			    	 Pane pane = new Pane();
    			    	 final Main.OverlayUI<Controller> overlay = Main.instance.overlayUI(pane, Main.controller);
    			    	 pane.setMaxSize(600, 360);
    			    	 pane.setStyle("-fx-background-color: white;");
    			    	 pane.setEffect(new DropShadow());
    			    	 Button btnClose = new Button("Close");
    			    	 HBox hbclose = new HBox();
    			    	 hbclose.getChildren().add(btnClose);
    			    	 hbclose.setAlignment(Pos.CENTER);
    			    	 VBox v = new VBox();
    			    	 Label lblOverview = new Label("Transaction Details");
    			    	 v.setMargin(lblOverview, new Insets(10,0,10,15));
    			    	 lblOverview.setFont(Font.font(null, FontWeight.BOLD, 18));
    			    	 ListView lvTx= new ListView();
    			    	 v.setMargin(lvTx, new Insets(0,0,10,15));
    			    	 lvTx.setPrefSize(570, 270);
    			    	 ObservableList<TextFlow> textformatted = FXCollections.<TextFlow>observableArrayList();
    			    	 //Transaction ID
    			    	 Text idtext = new Text("ID:  ");
    			    	 Text idtext2 = new Text(tx.getHashAsString());
    			    	 idtext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow idflow = new TextFlow();
    			    	 idflow.getChildren().addAll(idtext);
    			    	 idflow.getChildren().addAll(idtext2);
    			    	 textformatted.add(idflow);
    			    	 TextFlow spaceflow = new TextFlow();
    			    	 Text space = new Text(" ");
    			    	 spaceflow.getChildren().addAll(space);
    			    	 textformatted.add(spaceflow);
    			    	 //Date and Time
    			    	 Text datetext = new Text("Date & Time:          ");
    			    	 Text datetext2 = new Text(tx.getUpdateTime().toLocaleString());
    			    	 datetext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow dateflow = new TextFlow();
    			    	 dateflow.getChildren().addAll(datetext);
    			    	 dateflow.getChildren().addAll(datetext2);
    			    	 textformatted.add(dateflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Confirmations
    			    	 Text conftext = new Text("Confirmations:        ");
    			    	 Text conftext2 = new Text("");
    			    	 if (tx.getConfidence().getDepthInBlocks()==0){
    			    		 conftext2.setFill(Paint.valueOf("#f06e6e"));
    			    		 conftext2.setText("Unconfirmed");
    			    	 }
    			    	 else if (tx.getConfidence().getDepthInBlocks()>0 && tx.getConfidence().getDepthInBlocks()<6){
    			    		 conftext2.setFill(Paint.valueOf("#f8fb1a"));
    			    		 conftext2.setText(String.valueOf(tx.getConfidence().getDepthInBlocks()));
    			    	 }
    			    	 else {
    			    		 conftext2.setFill(Paint.valueOf("#98d947"));
    			    		 conftext2.setText(String.valueOf(tx.getConfidence().getDepthInBlocks()));
    			    	 }
    			    	 conftext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow confflow = new TextFlow();
    			    	 confflow.getChildren().addAll(conftext);
    			    	 confflow.getChildren().addAll(conftext2);
    			    	 textformatted.add(confflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Appears in Block
    			    	 Text blocktext = new Text("Included In Block:  ");
    			    	 Text blocktext2 = new Text();
    			    	 try {blocktext2.setText("#" + String.valueOf(tx.getConfidence().getAppearedAtChainHeight()));}
    			    	 catch (IllegalStateException e){blocktext2.setText("None");}
    			    	 blocktext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow blockflow = new TextFlow();
    			    	 blockflow.getChildren().addAll(blocktext);
    			    	 blockflow.getChildren().addAll(blocktext2);
    			    	 textformatted.add(blockflow);
    			    	 textformatted.add(spaceflow); 
    			    	 //Peers
    			    	 Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(tx);
    			    	 Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(tx);	
    			    	 Text peertext = new Text();
    			    	 if (exit.compareTo(Coin.ZERO) > 0){ peertext.setText("Seen By:                 ");}
    			    	 else { peertext.setText("Relayed By:             ");}
    			    	 Text peertext2 = new Text("");
    			    	 peertext2.setText(String.valueOf(tx.getConfidence().numBroadcastPeers() + " peers"));
    			    	 peertext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow peerflow = new TextFlow();
    			    	 peerflow.getChildren().addAll(peertext);
    			    	 peerflow.getChildren().addAll(peertext2);
    			    	 textformatted.add(peerflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Inputs
    			    	 Text inputtext = new Text("Inputs:                    ");
    			    	 inputtext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow inputflow = new TextFlow();
    			    	 inputflow.getChildren().addAll(inputtext);
    			    	 ArrayList<Text> intext = new ArrayList<Text>();
    			    	 Coin inAmount = Coin.ZERO;
    			    	 for (int b=0; b<tx.getInputs().size(); b++){
    			    		 Text inputtext2 = new Text("");
    			    		 Text inputtext3 = new Text("");
    			    		 inputtext3.setFill(Paint.valueOf("#98d947"));
    			    		 inputtext2.setText(tx.getInput(b).getFromAddress().toString() + " ");
    			    		 intext.add(inputtext2);
    			    		 try { 
    			    			 inputtext3.setText(tx.getInput(b).getValue().toFriendlyString());
    			    			 inAmount = inAmount.add(tx.getInput(b).getValue());
    			    		 } catch (NullPointerException e) {inputtext3.setText("unavailable");}
    			    		 if (b<tx.getInputs().size()-1){
    			    			 inputtext3.setText(inputtext3.getText() + "\n                                   ");
    			    		 }
    			    		 intext.add(inputtext3);
    			    	 }
    			    	 for (Text t : intext){inputflow.getChildren().addAll(t);}
    			    	 textformatted.add(inputflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Total Inputs
    			    	 Text intotaltext = new Text("Total Inputs:           ");
    			    	 Text intotaltext2 = new Text("");
    			    	 if(tx.getInput(0).getConnectedOutput()!=null){intotaltext2.setText(inAmount.toFriendlyString());}
    			    	 else {intotaltext2.setText("unavailable");}
    			    	 intotaltext2.setFill(Paint.valueOf("#98d947"));
    			    	 intotaltext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow intotalflow = new TextFlow();
    			    	 intotalflow.getChildren().addAll(intotaltext);
    			    	 intotalflow.getChildren().addAll(intotaltext2);
    			    	 textformatted.add(intotalflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Outputs
    			    	 Text outputtext = new Text("Outputs:                 ");
    			    	 outputtext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow outputflow = new TextFlow();
    			    	 outputflow.getChildren().addAll(outputtext);
    			    	 ArrayList<Text> outtext = new ArrayList<Text>();
    			    	 Coin outAmount = Coin.ZERO;
    			    	 for (int a=0; a < tx.getOutputs().size(); a++){
    			    		 Text outputtext2 = new Text("");
    			    		 Text outputtext3 = new Text("");
    			    		 outputtext3.setFill(Paint.valueOf("#f06e6e"));
    			    		 outputtext2.setText(tx.getOutput(a).getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()) + " ");
    			    		 outtext.add(outputtext2);
    			    		 outAmount = outAmount.add(tx.getOutput(a).getValue());
    			    		 outputtext3.setText(tx.getOutput(a).getValue().toFriendlyString());
    			    		 if (a<tx.getOutputs().size()-1){
    			    			 outputtext3.setText(outputtext3.getText() + "\n                                   ");
    			    		 }
    			    		 outtext.add(outputtext3);
    			    	 }
    			    	 for (Text t : outtext){outputflow.getChildren().addAll(t);}
    			    	 textformatted.add(outputflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Total outputs
    			    	 Text outtotaltext = new Text("Total Outputs:        ");
    			    	 Text outtotaltext2 = new Text("");
    			    	 outtotaltext2.setFill(Paint.valueOf("#f06e6e"));
    			    	 outtotaltext2.setText(outAmount.toFriendlyString());
    			    	 outtotaltext.setStyle("-fx-font-weight:bold;");
    			    	 TextFlow outtotalflow = new TextFlow();
    			    	 outtotalflow.getChildren().addAll(outtotaltext);
    			    	 outtotalflow.getChildren().addAll(outtotaltext2);
    			    	 textformatted.add(outtotalflow);
    			    	 textformatted.add(spaceflow);
    			    	 //Transaction Fee
    			    	 Text feetext = new Text("Fee:                        ");
    			    	 Text feetext2 = new Text("");
    			    	 try {feetext2.setText(tx.getFee().toFriendlyString());}
    			    	 catch (NullPointerException e) {feetext2.setText("unavailable");}
    			    	 feetext2.setFill(Paint.valueOf("#f06e6e"));
    			    	 TextFlow feeflow = new TextFlow();
    			    	 feetext.setStyle("-fx-font-weight:bold;");
    			    	 feeflow.getChildren().addAll(feetext);
    			    	 feeflow.getChildren().addAll(feetext2);
    			    	 textformatted.add(feeflow);
    			    	 lvTx.setItems(textformatted);
    			    	 lvTx.setStyle("-fx-background-color: transparent;");
    			    	 v.getChildren().add(lblOverview);
    			    	 v.getChildren().add(lvTx);
    			    	 v.getChildren().add(hbclose);
    			    	 pane.getChildren().add(v);
    			    	 btnClose.getStyleClass().add("custom-button");
    			    	 btnClose.setOnMousePressed(new EventHandler<MouseEvent>(){
    	            			@Override
    	            			public void handle(MouseEvent t) {
    	            				btnClose.setStyle("-fx-background-color: #a1d2e7;");
    	            			}
    	            		});
    	            		btnClose.setOnMouseReleased(new EventHandler<MouseEvent>(){
    	            			@Override
    	            			public void handle(MouseEvent t) {
    	            				btnClose.setStyle("-fx-background-color: #199bd6;");
    	            			}
    	            		});
    			    	 btnClose.setPrefWidth(150);
    			    	 btnClose.setOnMouseClicked(new EventHandler<MouseEvent>() {
    			    		 @Override
    			    		 public void handle(MouseEvent event) {
    			    			 overlay.done();
    			    		 }
    			    	 });
    			    	 final ContextMenu contextMenu2 = new ContextMenu();
    					 MenuItem item12 = new MenuItem("Copy");
    					 item12.setOnAction(new EventHandler<ActionEvent>() {
    						 public void handle(ActionEvent e) {
    							 Clipboard clipboard = Clipboard.getSystemClipboard();
    							 ClipboardContent content = new ClipboardContent();
    							 String stringToCopy = "";
    							 switch (lvTx.getSelectionModel().getSelectedIndex()) {
    					            case 0:  stringToCopy = tx.getHashAsString();
    					                     break;
    					            case 1:  break;
    					            case 2:  stringToCopy = tx.getUpdateTime().toLocaleString();
    					                     break;
    					            case 3:  break;
    					            case 4:  stringToCopy = String.valueOf(tx.getConfidence().getDepthInBlocks());
    					                     break;
    					            case 5:  break;
    					            case 6:  stringToCopy = blocktext2.getText();
    					                     break;
    					            case 7:  break;
    					            case 8:  stringToCopy = String.valueOf(tx.getConfidence().numBroadcastPeers() + " peers");
    					                     break;
    					            case 9:  break;
    					            case 10: for (TransactionInput in : tx.getInputs()){stringToCopy = stringToCopy + in.getFromAddress().toString() + "; ";}
    					                     break;
    					            case 11: break;
    					            case 12: stringToCopy = intotaltext2.getText();
    					                     break;
    					            case 13: break;
    					            case 14: for (TransactionOutput out : tx.getOutputs()){stringToCopy = stringToCopy + out.getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()) + "; ";}
    					            		 break;
    					            case 15: break;
    					            case 16: stringToCopy = outtotaltext2.getText();
    					            		 break;
    					            case 17: break;
    					            case 18: stringToCopy = feetext2.getText();
    					        }
    							 content.putString(stringToCopy);
    							 clipboard.setContent(content);
    						 }
    					 });
    					 contextMenu2.getItems().addAll(item12);
    					 lvTx.setContextMenu(contextMenu2);
    			     }
    			  }
    			}
    		}
    	});
    }
    
    //#####################################
   	//
   	//	Apps Pane
   	//
   	//#####################################
    
    @FXML protected void btnAppAuthenticator(MouseEvent event) {
    	Main.instance.overlayUI("Pair_wallet.fxml");
    }
    
    @FXML protected void btnOneName(MouseEvent event) {
    	if(Authenticator.getWalletOperation().getOnename() != null)
    		Main.instance.overlayUI("DisplayOneName.fxml");
    	else
    		Dialogs.create()
		        .owner(Main.stage)
		        .title("Cannot display your OneName account")
		        .message("Please press on your avatr picture on the overview panel to set your OneName account")
		        .showWarning();
    }
    
    public static Stage accountsAppStage;
    @FXML protected void btnAccounts(MouseEvent event) {
        try {
        	URL location = getClass().getResource("display_accounts/display_accounts.fxml");
        	FXMLLoader loader = new FXMLLoader(location);
        	accountsAppStage = new Stage();
        	accountsAppStage.setTitle("Accounts");
        	accountsAppStage.initStyle(StageStyle.UNDECORATED);
        	Scene scene = new Scene((AnchorPane) loader.load(), 436, 516);
        	final String file = TextFieldValidator.class.getResource("GUI.css").toString();
            scene.getStylesheets().add(file);  // Add CSS that we need.
        	accountsAppStage.setScene(scene);
        	accountsAppStage.show();
		} catch (IOException e) { e.printStackTrace(); }
    }
    
    //#####################################
   	//
   	//	Change account
   	//
   	//#####################################
    
    public void changeAccount(String toValue){
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
    	try {refreshBalanceLabel();}
    	catch (JSONException | IOException e) {e.printStackTrace();}
    	setReceiveAddresses();
    	try {setTxPaneHistory();} 
    	catch (NoSuchAlgorithmException | JSONException
				| AddressFormatException | KeyIndexOutOfRangeException
				| AddressNotWatchedByWalletException e1) {e1.printStackTrace();}
    	try {setTxHistoryContent();} 
    	catch (NoSuchAlgorithmException | JSONException
				| AddressFormatException | KeyIndexOutOfRangeException
				| AddressNotWatchedByWalletException e) {e.printStackTrace();}
    }
    
    /**
     * Will check if the given password or the chached Authenticator password can decrypt the wallet.<br>
     * If wallet is encrypted, at the end of the check will keep the wallet encrypted.<br>
     * If wallet is not encrypted or the password does decrypt the wallet, this will return true;
     * 
     * @param password
     * @return
     */
	private boolean checkIfPasswordDecryptsWallet(String password){
		if(Authenticator.getWalletOperation().isWalletEncrypted()){
    		try{
    			if (Authenticator.AUTHENTICATOR_PW.equals("")){
    				if (password.equals("")){
                		informationalAlert("Unfortunately, you messed up.",
           					 "You need to enter your password to decrypt your wallet.");
                		return false;
                	}
    				else {
    					Authenticator.getWalletOperation().decryptWallet(password);
    					Authenticator.getWalletOperation().encryptWallet(password);
    					Authenticator.AUTHENTICATOR_PW = password;
    				}
    			}
    			else {
    				Authenticator.getWalletOperation().decryptWallet(Authenticator.AUTHENTICATOR_PW);
    				Authenticator.getWalletOperation().encryptWallet(Authenticator.AUTHENTICATOR_PW);
    			}
    		}
    		catch(KeyCrypterException  e){
    			informationalAlert("Unfortunately, you messed up.",
    					"Wrong wallet password");
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
    
    /*public void setActivitySpinner(String text){
    	if(isSpinnerVisible == false){
    		syncProgress.setVisible(false);
			lblStatus.setText(text);
	    	readyToGoAnimation(-1,new EventHandler<ActionEvent>(){
				@Override
				public void handle(ActionEvent arg0) {
					// Sync progress bar slides out ...
			        TranslateTransition leave = new TranslateTransition(Duration.millis(600), lblStatus);
			        leave.setByX(300.0);
			        leave.setCycleCount(1);
			        leave.play();
				}
	        });
	    	isSpinnerVisible = true;
    	}
    	
    }
    
    public void removeActivitySpinner(){
    	if(isSpinnerVisible == true){
	    
    		// Sync progress bar slides out ...
	        TranslateTransition leave = new TranslateTransition(Duration.millis(600), lblStatus);
	        leave.setByX(-300.0);
	        leave.setCycleCount(1);
	        leave.play();
	        leave.setOnFinished(new EventHandler<ActionEvent>(){
				@Override
				public void handle(ActionEvent arg0) {
					readyToGoAnimation(1, new EventHandler<ActionEvent>(){
						@Override
						public void handle(ActionEvent arg0) {
							syncProgress.setVisible(true);
							lblStatus.setText("No Activity");
						}
					});	
				}
	        });
	        isSpinnerVisible = false;  
	    }
    }*/
    
}
