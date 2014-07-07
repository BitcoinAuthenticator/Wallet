package wallettemplate;

import authenticator.Authenticator;
import authenticator.AuthenticatorGeneralEventsListener;
import authenticator.Utils.KeyUtils;
import authenticator.network.OneName;
import authenticator.operations.ATOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPrefixedAccountIndex;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.ActiveAccountType;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.CachedExternalSpending;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.ui_helpers.BAApplication.NetworkType;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.*;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.AlertWindowController;
import wallettemplate.utils.TextFieldValidator;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

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
public class Controller {
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
	 @FXML private ImageView ivOverview;
	 @FXML private Pane OverviewPane;
	 @FXML private Pane SendPane;
	 @FXML private Pane ReceivePane;
	 @FXML private Pane AppsPane;
	 @FXML private Pane SyncPane;
	 @FXML private ImageView ivSync;
	 @FXML private ProgressBar syncProgress;
	 @FXML private Button btnAvatar;
	 @FXML private Button btnAdd;
	 @FXML private Button btnSend;
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
	 @FXML private Button btnClear;
	 @FXML public ChoiceBox AccountBox;
	 @FXML private TextField txReqLabel;
	 @FXML private TextField txReqAmount;
	 @FXML private TextArea txReqMemo;
	 @FXML private ChoiceBox reqCurrencyBox;
	 @FXML public ScrollPane scrlViewTxHistory;
	 @FXML private ImageView ivLogo;
	 private ScrollPaneContentManager scrlViewTxHistoryContentManager;
	 private double xOffset = 0;
	 private double yOffset = 0;
	 public ScrollPane scrlpane;
	 private ScrollPaneContentManager scrlContent;
	 public static Stage stage;
	 public Main.OverlayUI overlayUi;
	 private Wallet.SendResult sendResult;
	 TorListener listener = new TorListener();
	 public static Stage teststage;
	 

	//#####################################
	//
	//	Initialization Methods
	//
	//#####################################
	 
    public void initialize() {
        syncProgress.setProgress(-1);
        RotateTransition rt = new RotateTransition(Duration.millis(3000),ivSync);
        rt.setByAngle(360);
        rt.setCycleCount(10000); // to infinite ?
        rt.play();
        
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
        
        //Startup loader test
        ivLogo.setOnMouseClicked(new EventHandler<MouseEvent>() {
        	
    		public void handle(MouseEvent event) {
    			Parent root;
    	        try {
    	            root = FXMLLoader.load(Main.class.getResource("walletstartup.fxml"));
    	            teststage = new Stage();
    	            teststage.setTitle("My New Stage Title");
    	            teststage.initStyle(StageStyle.UNDECORATED);
    	            Scene scene = new Scene(root, 607, 400);
    	            final String file = TextFieldValidator.class.getResource("GUI.css").toString();
    	            scene.getStylesheets().add(file);  // Add CSS that we need.
    	            teststage.setScene(scene);
    	            teststage.show();

    	        } catch (IOException e) {
    	            e.printStackTrace();
    	        }
    			
    		}
       });
        
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
		AddressBox.valueProperty().addListener(new ChangeListener<String>() {
    		@Override public void changed(ObservableValue ov, String t, String t1) {
    			if(t1 != null && t1.length() > 0)
    			if (t1.substring(0,1).equals(" ")){
    				if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending){
    					try 
    					{
    						ArrayList<String> newAdd = Authenticator.getWalletOperation().addMoreExternalSpendingAddresses();
    						AddressBox.getItems().remove(Main.controller.AddressBox.getItems().indexOf("                                More"));
    						for(String s:newAdd)
    							AddressBox.getItems().addAll(s);
    						Main.controller.AddressBox.getItems().addAll("                                More");
    					} 
        				catch (IOException | AddressFormatException e) {e.printStackTrace();}
    				}
    				else
    				{
    					String newAdd = null;
    					try {
    						if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings){
    							newAdd = Authenticator.getWalletOperation()
    									.getNextExternalAddress(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE)
    									.getAddressStr();
    						}
    						else
    							newAdd = Authenticator.getWalletOperation()
    													.getNextExternalAddress(Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex())
						    							.getAddressStr();
    						AddressBox.getItems().add(0, newAdd);
    					} catch (Exception e) { e.printStackTrace(); }
    				}
    			}
    		}    
    	});
    }
    
    public void onBitcoinSetup() {
    	bitcoin.wallet().freshReceiveAddress();
    	bitcoin.peerGroup().addEventListener(new PeerListener());
    	TorClient tor = bitcoin.peerGroup().getTorClient();
    	tor.addInitializationListener(listener);
    	setupOneName(Authenticator.getWalletOperation().getOnename());
        refreshBalanceLabel();
        setReceiveAddresses();
        setTxHistoryContent();
        
        Authenticator.addGeneralEventsListener(new AuthenticatorGeneralEvents());
        
        // Account choicebox
        setAccountChoiceBox();
    }
    
    public class AuthenticatorGeneralEvents implements AuthenticatorGeneralEventsListener{

		@Override
		public void onNewPairedAuthenticator() {
			setAccountChoiceBox();
		}

		@Override
		public void onNewUserNamecoinIdentitySelection(AuthenticatorConfiguration.ConfigOneNameProfile profile) {
			setupOneName(profile);
		}

		@Override
		public void onFinishedBuildingWalletHierarchy() { }

		@Override
		public void onBalanceChanged(int walletID) {
			refreshBalanceLabel();
	        setReceiveAddresses();
	        setTxHistoryContent();
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
    }
   
   @SuppressWarnings("unchecked")
   public void setAccountChoiceBox(){
	   List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
	   try {
		all = Authenticator.getWalletOperation().getAllPairingObjectArray();
	   } catch (IOException e) { }
	   
	   AccountBox.getItems().clear();
	   AccountBox.getItems().add("Spending");
	   AccountBox.getItems().add("Savings");
	   AccountBox.setTooltip(new Tooltip("Select active account"));
	   AccountBox.valueProperty().addListener(new ChangeListener<String>() {
   			@Override 
   			public void changed(ObservableValue ov, String t, String t1) {
   				if(t1 != null && t1.length() > 0){
					AccountBox.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(new Font("Arial", 14),t1, 0.0D)+45);
					changeAccount(t1);
   				}
   			}    
	   });
	   for(PairedAuthenticator po:all){
		   AccountBox.getItems().add(po.getPairingName());
	   }
	   
	   if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending)
		   AccountBox.setValue("Spending");
	   else if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings)
		   AccountBox.setValue("Savings");
	   else
		   AccountBox.setValue(Authenticator.getActiveAccount().getPairedAuthenticator().getPairingName());
	
	   AccountBox.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(new Font("Arial", 14),AccountBox.getValue().toString(), 0.0D)+45);
   }
   
   	//#####################################
 	//
 	//	Overview Pane
 	//
 	//#####################################
   
   @FXML protected void lockWallet(ActionEvent event){
	   
   }
   
   @FXML protected void unlockWallet(ActionEvent event){
	   
   }
   
   public void setupOneName(AuthenticatorConfiguration.ConfigOneNameProfile one){
	   
	   // get image
	   File imgFile = null;
	   BufferedImage bimg = null;
	   Image img = null;
	   try {
		    imgFile = new File(one.getOnenameAvatarFilePath());
			bimg = ImageIO.read(imgFile);
			img = OneName.createImage(bimg);
	   } catch (Exception e) { e.printStackTrace(); }
	   
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
	public void refreshBalanceLabel() {
    	Coin unconfirmed = Coin.ZERO;
    	Coin confirmed = Coin.ZERO;
    	if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending)
    	{
	    	
			unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE);
			
	    	confirmed = Authenticator.getWalletOperation().getConfirmedBalance(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE);
    	}
    	else if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings){
    		unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE);
    		confirmed = Authenticator.getWalletOperation().getConfirmedBalance(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE);
    	}
    	else{
    		unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex());
    		confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex());
    	}    	
        //final Coin total = confirmed.add(unconfirmed);
        lblConfirmedBalance.setText(confirmed.toFriendlyString() + " BTC");
        lblUnconfirmedBalance.setText(unconfirmed.toFriendlyString() + " BTC");
    }
    
    @SuppressWarnings("unused")
	public void setTxHistoryContent(){
    	scrlViewTxHistoryContentManager.clearAll();
    	List<Transaction> txAll = Authenticator.getWalletOperation().getRecentTransactions();
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
    		l2.setText(txid.substring(0, 20) + "...");
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
    		Coin enter = txAll.get(i).getValueSentToMe(Main.bitcoin.wallet());
    		Coin exit = txAll.get(i).getValueSentFromMe(Main.bitcoin.wallet());
    		Image in = new Image(Main.class.getResourceAsStream("in.png"));
    		Image out = new Image(Main.class.getResourceAsStream("out.png"));
    		ImageView arrow = null;
    		if (exit.compareTo(Coin.ZERO) > 0){ // means i sent coins
    			l3.setTextFill(Paint.valueOf("#ea4f4a"));
    			l3.setText("-" + exit.subtract(enter).toFriendlyString() + " BTC"); // get total out minus enter to subtract change amount
    			tip += "Amount: -" + exit.subtract(enter).toFriendlyString() + " BTC\n";	
    			arrow = new ImageView(out);
    		}
    		else { // i only received coins
    			l3.setTextFill(Paint.valueOf("#98d947"));
    			l3.setText(enter.toFriendlyString() + " BTC");
    			tip+= "Amount: " + enter.toFriendlyString() + " BTC\n";
    			arrow = new ImageView(in);
    		}
     		l3.setFont(Font.font(13));
    		content.getChildren().add(l3);
    		content.getChildren().add(arrow);
    		rightBox.getChildren().add(content);
    		
    		mainNode.getChildren().add(rightBox);
    		mainNode.setOnMouseClicked(new EventHandler<MouseEvent>() {
    		    @Override
    		    public void handle(MouseEvent mouseEvent) {
                   if(!mouseEvent.isPrimaryButtonDown()){
                	   final ContextMenu contextMenu = new ContextMenu();
                	   MenuItem openExplore;
                	   if(Authenticator.getApplicationParams().getBitcoinNetworkType() == NetworkType.MAIN_NET){
                		   openExplore = new MenuItem("Open In blockchain.info");
                		   openExplore.setOnAction(new EventHandler<ActionEvent>() {
                    		    @Override
                    		    public void handle(ActionEvent event) {
                    		    	String url = "https://blockchain.info/tx/" + txid; 
                             	   // open the default web browser for the HTML page
             	            	   try {
             							Desktop.getDesktop().browse(java.net.URI.create(url));
             	            	   } catch (IOException e) { e.printStackTrace(); }
                    		    }
                    		});
                	   }
                	   else
                	   {
                		   openExplore = new MenuItem("Open In blockexplorer.com");
                		   openExplore.setOnAction(new EventHandler<ActionEvent>() {
                    		    @Override
                    		    public void handle(ActionEvent event) {
                    		    	String url = "http://blockexplorer.com/testnet/tx/" + txid; 
                             	   // open the default web browser for the HTML page
             	            	   try {
             							Desktop.getDesktop().browse(java.net.URI.create(url));
             	            	   } catch (IOException e) { e.printStackTrace(); }
                    		    }
                    		});
                	   }
                	   
                	   MenuItem cancel = new MenuItem("Cancel");
                	   cancel.setOnAction(new EventHandler<ActionEvent>() {
               		    @Override
               		    public void handle(ActionEvent event) {
               		    	contextMenu.hide();
               		    }
               		});
                	contextMenu.getItems().addAll(openExplore, cancel);
                	contextMenu.show(Main.stage);
                	
                   }
    		    }
    		});
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
    
    @FXML protected void btnAddPressed(MouseEvent event) {
    	btnAdd.setStyle("-fx-background-color: #a1d2e7;");
    }
    
    @FXML protected void btnAddReleased(MouseEvent event) {
    	btnAdd.setStyle("-fx-background-color: #199bd6;");
    }
    
    @FXML protected void btnSendPressed(MouseEvent event) {
    	btnSend.setStyle("-fx-background-color: #a1d2e7;");
    }
    
    @FXML protected void btnSendReleased(MouseEvent event) {
    	btnSend.setStyle("-fx-background-color: #199bd6;");
    } 
    
    private boolean ValidateTx() throws NoSuchAlgorithmException, JSONException, AddressFormatException
    {
    	//Check tx message
    	if(txMsgLabel.getText().length() < 3)
    		return false;
    	//Check Outputs
    	if(scrlContent.getCount() == 0)
    		return false;
    	for(Node n:scrlContent.getChildren())
    	{
    		NewAddress na = (NewAddress)n;
    		if(!na.validate())
    			return false;
    	}
    	//check sufficient funds
    	Coin amount = Coin.ZERO;
    	for(Node n:scrlContent.getChildren())
    	{
    		NewAddress na = (NewAddress)n;
    		double a = (double) Double.parseDouble(na.txfAmount.getText())*100000000;
    		amount = amount.add(Coin.valueOf((long)a));
    	}
    	// fee
    	Coin fee = Coin.ZERO;
		if (txFee.getText().equals("")){fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
        else 
        {
        	double f = (double) Double.parseDouble(txFee.getText())*100000000;
        	fee = Coin.valueOf((long)f);
        }
    	amount = amount.add(fee);
    	//
    	Coin confirmed = Coin.ZERO;

    	if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending ){
    		HierarchyAddressTypes addType = HierarchyAddressTypes.External; // TODO - internal also
    		confirmed = Authenticator.getWalletOperation().getConfirmedBalance(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE);
    	}	
    	else if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings)
    		confirmed = Authenticator.getWalletOperation().getConfirmedBalance(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE);
    	else
    		confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex());   	
    	
    	if(amount.compareTo(confirmed) > 0) return false;
    	
    	//Check min dust amount 
    	if(amount.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) return false;
    	
    	return true;
    }
    
    @FXML protected void SendTx(ActionEvent event) throws Exception{
    	
    	if(!ValidateTx()){
    		Dialogs.create()
	        .owner(Main.stage)
	        .title("Error")
	        .masthead("Something Is not Right ...")
	        .message("Make Sure:\n" +
					"  1) You entered correct values in all fields\n"+
					"  2) You have sufficient funds to cover your outputs\n"+
					"  3) Outputs amount to at least the dust value(" + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.toString() + ")\n" +
					"  4) A Tx message, at least 3 characters long\n")
	        .showError();  
    	}
    	else{
    		//
    		setActivitySpinner("Sending Tx ..");
    		// collect Tx outputs
    		ArrayList<TransactionOutput> to = new ArrayList<TransactionOutput>();
        	for(Node n:scrlContent.getChildren())
        	{
        		NewAddress na = (NewAddress)n;
        		Address add;
				try {
					add = new Address(Authenticator.getWalletOperation().getNetworkParams(), na.txfAddress.getText());
					double amount = (double) Double.parseDouble(na.txfAmount.getText())*100000000;
					long satoshis = (long) amount;
					if (Coin.valueOf(satoshis).compareTo(Transaction.MIN_NONDUST_OUTPUT) > 0){
						TransactionOutput out = new TransactionOutput(Authenticator.getWalletOperation().getNetworkParams(),
											        				null, 
											        				Coin.valueOf(satoshis), 
											        				add);
						to.add(out);
					}
					
				} catch (AddressFormatException e) { 
					Platform.runLater(new Runnable() {
					      @Override public void run() {
					    	  Dialogs.create()
						        .owner(Main.stage)
						        .title("Error !")
						        .masthead("Wrong Address")
						        .message("")
						        .showConfirm();   
					      }
					    });
				}
        		
        	}
        	
        	//	get fee
        	Coin fee = Coin.ZERO;
    		if (txFee.getText().equals("")){fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
            else 
            {
            	double f = (double) Double.parseDouble(txFee.getText())*100000000;
            	fee = Coin.valueOf((long)f);
            }
    		
    		// get input addresses / change address
    		ArrayList<String> addresses;
    		String changeaddr = null;
    		String pairID = Authenticator.getActiveAccount().getPairedAuthenticator().getPairingID();
    		if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending ){
    			// inputs
    			addresses = Authenticator.getWalletOperation().getAccountAddresses(HierarchyPrefixedAccountIndex.PrefixSpending_VALUE, HierarchyAddressTypes.External, -1);
    			// change address
    			try {
					changeaddr = Authenticator.getWalletOperation().getNewSpendingExternalAddress(true).getAddressStr();
				} catch (AddressFormatException e) { e.printStackTrace(); }
    		}
    		else if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings){
    			// inputs
    			addresses = Authenticator.getWalletOperation().getAccountAddresses(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE, HierarchyAddressTypes.External, -1);
    			// change address
    			try {
					changeaddr = Authenticator.getWalletOperation()
							.getNextExternalAddress(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE)
							.getAddressStr();
				} catch (AddressFormatException e) { e.printStackTrace(); }
    		}
    		else{
    			int accountID = Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex();
    			// inputs
    			addresses = Authenticator.getWalletOperation().getAccountAddresses(accountID, HierarchyAddressTypes.External, -1);
    			// change address
    			try {
					changeaddr = Authenticator.getWalletOperation()
							.getNextExternalAddress(accountID)
							.getAddressStr();
				} catch (AddressFormatException e) { e.printStackTrace(); }
    		}
    		   
    		// complete Tx
    		Transaction tx;
			try {
				tx = Authenticator.getWalletOperation().mkUnsignedTx(addresses, to,fee,changeaddr);
				
				// broadcast
	    		ATOperation op = null;
	    		if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending ||
	    				Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings){
	    			Map<String,ATAddress> keys = new HashMap<String,ATAddress>();
	    			for(TransactionInput in:tx.getInputs()){
	    				int accountID = Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex();
	    				
	    				// get address
	    				String add = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
	    				
	    				// find key
	    				ATAddress ca = Authenticator.getWalletOperation().findAddressInAccounts(add);
	    				
	    				//add key
	    				keys.put(add, ca);
	    			}
	    			op = OperationsFactory.BROADCAST_NORMAL_TRANSACTION(tx,keys);
	    		}
	    		else
	    			op = OperationsFactory.SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION(tx, pairID, txMsgLabel.getText(),false,null);
	    		
	    		// operation listeners
	    		op.SetOperationUIUpdate(new OnOperationUIUpdate(){
					@Override
					public void onBegin(String str) { }

					@Override
					public void statusReport(String report) {

					}

					@Override
					public void onFinished(String str) {
							removeActivitySpinner();
							Platform.runLater(new Runnable() {
						      @Override public void run() {
						    	  Dialogs.create()
							        .owner(Main.stage)
							        .title("Success !")
							        .masthead("Broadcasting Completed")
							        .message("If this is a multisig P2SH transaction, we are waiting for the Authenticator to sign")
							        .showInformation();  
						      }
						    });
					}

					@Override
					public void onError(Exception e, Throwable t) {
						removeActivitySpinner();
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

					@Override
					public void onUserCancel(String reason) {
						removeActivitySpinner();
						Platform.runLater(new Runnable() {
						      @Override public void run() {
						    	  Dialogs.create()
							        .owner(Main.stage)
							        .title("Error")
							        .masthead("Authenticator Refused The Transaction")
							        .message(reason)
							        .showError();   
						      }
						    });
					}

					@Override
					public void onUserOk(String msg) { }

				});
				Authenticator.operationsQueue.add(op);
			} catch (NoSuchAlgorithmException | AddressFormatException e1) { e1.printStackTrace(); }
    	}
    		
    }
    
    @FXML protected void add() {
    	addOutput();
    }
    
    private void addOutput() {
    	Class[] parameterTypes = new Class[1];
        parameterTypes[0] = int.class;
        Method removeOutput;
		try {
			removeOutput = Controller.class.getMethod("removeOutput", parameterTypes);
			NewAddress na = new NewAddress(scrlContent.getCount()).setCancelOnMouseClick(this,removeOutput);
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
    
    public class NewAddress extends HBox {
    	TextField txfAddress;
    	TextField txfAmount;
    	Label lblScanQR;
    	Label lblContacts;
    	Label amountInSatoshi;
    	Label cancelLabel;
    	int index;
    	Method onCancel;
    	public NewAddress setCancelOnMouseClick(Object object, Method method){
    		cancelLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					Object[] parameters = new Object[1];
			        parameters[0] = index;
			        try {
						method.invoke(object, parameters);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}
				}
            });
    		return this;
    	}
    	
    	public NewAddress(int index)
    	{
    		this.index = index;
    		VBox main = new VBox();
    		VBox cancel = new VBox();
    		//Cancel
    		cancelLabel = new Label();
    		//cancelLabel.setText("<X>");
    		cancelLabel.setPadding(new Insets(0,5,0,0));
    		cancelLabel.setFont(new Font("Arial", 18));
    		AwesomeDude.setIcon(cancelLabel, AwesomeIcon.TIMES_CIRCLE);
            Tooltip.install(cancelLabel, new Tooltip("Remove Output"));
            cancel.setAlignment(Pos.TOP_CENTER);
            cancel.setMargin(cancelLabel, new Insets(2,0,0,0));
            cancel.getChildren().add(cancelLabel);
            this.getChildren().add(cancel);
    		
    		//Text Fields
            HBox a = new HBox();
    		txfAddress = new TextField();
    		txfAddress.setPromptText("Recipient Address or +OneName");
    		txfAddress.setPrefWidth(400);
    		txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1;");
    		a.getChildren().add(txfAddress);
    		lblScanQR = new Label();
    		lblScanQR.setFont(new Font("Arial", 18));
    		lblScanQR.setPrefSize(25, 25);
    		Tooltip.install(lblScanQR, new Tooltip("Scan QR code"));
    		a.setMargin(lblScanQR, new Insets(1,0,0,8));
    		AwesomeDude.setIcon(lblScanQR, AwesomeIcon.QRCODE);
    		a.getChildren().add(lblScanQR);
    		lblScanQR.setOnMouseClicked(new EventHandler<MouseEvent>() {
    		    @Override
    		    public void handle(MouseEvent mouseEvent) {
                   
    		    }
    		});
    		lblContacts = new Label();
    		lblContacts.setFont(new Font("Arial", 18));
    		lblContacts.setPrefSize(25, 25);
    		Tooltip.install(lblContacts, new Tooltip("Select from address book"));
    		a.setMargin(lblContacts, new Insets(1,0,0,0));
    		AwesomeDude.setIcon(lblContacts, AwesomeIcon.USERS);
    		a.getChildren().add(lblContacts);
    		main.getChildren().add(a);

    		HBox b = new HBox();
    		txfAmount = new TextField();
    		txfAmount.setPromptText("Amount (BTC)");
    		txfAmount.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1;");
    		txfAmount.lengthProperty().addListener(new ChangeListener<Number>(){
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
                      if(newValue.intValue() > oldValue.intValue()){
                          char ch = txfAmount.getText().charAt(oldValue.intValue());  
                          //Check if the new character is the number or other's
                          if(!(ch >= '0' && ch <= '9') && ch != '.'){       
                               //if it's not number then just setText to previous one
                               txfAmount.setText(txfAmount.getText().substring(0,txfAmount.getText().length()-1)); 
                          }
                     }
                }
    		});
    		b.setMargin(txfAmount, new Insets(4,200,0,0));
    		b.getChildren().add(txfAmount);
    		amountInSatoshi = new Label();
    		//amountInSatoshi.setText("bits");
    		b.getChildren().add(amountInSatoshi);
    		main.getChildren().add(b);
    		this.getChildren().add(main);
    	}
    	
    	public boolean validate()
        {
        	if(txfAddress.getText().length() == 0)
        		return false;
        	if(txfAmount.getText().length() == 0)
        		return false;
        	if(txfAmount.getText().matches("[a-zA-Z]+"))
        		return false;
        	// check dust amount 
        	double fee = (double) Double.parseDouble(txfAmount.getText())*100000000;
        	Coin am = Coin.valueOf((long)fee);
        	if(am.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0)
        		return false;
        	
        	return true;
        }
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
    
    @FXML protected void btnClearPressed(MouseEvent event) {
    	btnClear.setStyle("-fx-background-color: #a1d2e7;");
    }
    
    @FXML protected void btnClearReleased(MouseEvent event) {
    	btnClear.setStyle("-fx-background-color: #199bd6;");
    } 
    
    void createReceivePaneButtons(){
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
					add = Authenticator.getWalletOperation().findAddressInAccounts(AddressBox.valueProperty().getValue().toString());
					final Clipboard clipboard = Clipboard.getSystemClipboard();
	                final ClipboardContent content = new ClipboardContent();
	                content.putString(add.getAddressStr());
	                clipboard.setContent(content);
				} catch (Exception e1) { }
            	
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
        	btnClear.setOnAction(new EventHandler<ActionEvent>() {
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
    	AddressBox.getItems().clear();
    	if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Spending){
    		ArrayList<String> keypool = null;
        	try { Authenticator.getWalletOperation().fillExternalSpendingKeyPool(); } 
        	catch (IOException e) {e.printStackTrace();} catch (AddressFormatException e) { e.printStackTrace(); }
        	try {keypool = Authenticator.getWalletOperation().getNotUsedExternalSpendingAddressStringPool(10); }  
        	catch (IOException e) {e.printStackTrace();}
        	
        	AddressBox.setValue(keypool.get(0));
        	for(String s:keypool)
        		AddressBox.getItems().addAll(s);
    	}
    	else{
    		int accountIdx = Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings? 
    					HierarchyPrefixedAccountIndex.PrefixSavings_VALUE:
    					Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex();
    		
    		ArrayList<String> add = new ArrayList<String> ();
			try {
				add = Authenticator.getWalletOperation().
									getAccountNotUsedAddress(accountIdx,
					    			HierarchyAddressTypes.External,
					    			10);
			} catch (NoSuchAlgorithmException | JSONException
					| AddressFormatException e1) { }
			
    		if(add.size() == 0){
    			String newAdd = null;
				try {
					if(Authenticator.getActiveAccount().getActiveAccountType() == ActiveAccountType.Savings){
						newAdd = Authenticator.getWalletOperation()
								.getNextExternalAddress(HierarchyPrefixedAccountIndex.PrefixSavings_VALUE)
								.getAddressStr();
					}
					else
						newAdd = Authenticator.getWalletOperation()
												.getNextExternalAddress(Authenticator.getActiveAccount().getPairedAuthenticator().getWalletAccountIndex())
												.getAddressStr();
					add.add(newAdd);
				} catch (Exception e) { e.printStackTrace(); }
    			
    		}
    		for (String address : add){
        		AddressBox.getItems().addAll(address);
        	}
    		AddressBox.setValue(add.get(0));
    		
    	}
    	
    	AddressBox.getItems().addAll("                                More");
    }    
    
    //#####################################
   	//
   	//	Transactions Pane
   	//
   	//#####################################
    
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
    
    //#####################################
   	//
   	//	Change account
   	//
   	//#####################################
    
    public void changeAccount(String toValue){
    	List<PairedAuthenticator> all = new ArrayList<PairedAuthenticator>();
    	try {
    		all = Authenticator.getWalletOperation().getAllPairingObjectArray();
		} catch (IOException e) { e.printStackTrace(); }
    	
    	// find selected paired authenticator
    	PairedAuthenticator selectedAuth = null;
    	for(PairedAuthenticator po:all){
    		if(po.getPairingName().equals(toValue))
    		{
    			selectedAuth = po;
    			break;
    		}
    	}
    	
    	// change account
    	AuthenticatorConfiguration.ConfigActiveAccount.Builder b = AuthenticatorConfiguration.ConfigActiveAccount.newBuilder();
    	if(selectedAuth != null){
    		b.setActiveAccountType(ActiveAccountType.Authenticator);
    		b.setPairedAuthenticator(selectedAuth);
    	}
    	else {
    		if(AccountBox.getValue().toString().equals("Savings"))
    			b.setActiveAccountType(ActiveAccountType.Savings);
    		else
    			b.setActiveAccountType(ActiveAccountType.Spending);
    	}
    	if( Authenticator.setActiveAccount(b.build()))
    		updateUIForNewActiveAccount();
    }
    
    public void updateUIForNewActiveAccount(){
    	refreshBalanceLabel();
    	setReceiveAddresses();
    	setTxHistoryContent();
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
    public void readyToGoAnimation(int direction, @Nullable EventHandler<ActionEvent> listener) {
    	// Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), SyncPane);
        if(listener != null)
        	leave.setOnFinished(listener);
        leave.setByY(direction==1? 80.0:-80.0);
        leave.setCycleCount(1);
        leave.setInterpolator(direction==1? Interpolator.EASE_OUT:Interpolator.EASE_IN);
        leave.play();
        if(direction == 1)
        	isSpinnerVisible = false;
    }
    
    public void setActivitySpinner(String text){
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
	    	/*Platform.runLater(new Runnable(){
				@Override
				public void run() {
					
				}
	        });*/
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
    }
    
}
