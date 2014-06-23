package wallettemplate;

import authenticator.PairedKey;

import com.github.sarxos.webcam.Webcam;
import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.wallet.KeyChain;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import com.sun.javafx.tk.Toolkit.Task;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.KeyUtils;

import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

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
	 @FXML private ChoiceBox AddressBox;
	 @FXML private TextField txMsgLabel;
	 @FXML private TextField txFee;
	 @FXML private Button btnConnection0;
	 @FXML private Button btnConnection1;
	 @FXML private Button btnConnection2;
	 @FXML private Button btnConnection3;
	 @FXML private Button btnTor_grey;
	 @FXML private Button btnTor_color;
	 @FXML private Label lblStatus;
	 ArrayList<ECKey> keypool;
	 private double xOffset = 0;
	 private double yOffset = 0;
	 public ScrollPane scrlpane;
	 private ScrollPaneContentManager scrlContent;
	 public static Stage stage;
	 public Main.OverlayUI overlayUi;
	 private Wallet.SendResult sendResult;
	 TorListener listener = new TorListener();
	 

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
        createReceivePaneButtons();
    }
    
    public void onBitcoinSetup() {
    	bitcoin.wallet().addEventListener(new WalletListener());
    	bitcoin.wallet().freshReceiveAddress();
    	bitcoin.peerGroup().addEventListener(new PeerListener());
    	TorClient tor = bitcoin.peerGroup().getTorClient();
    	tor.addInitializationListener(listener);
        refreshBalanceLabel();
        setAddresses();
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
            Platform.runLater(Controller.this::readyToGoAnimation);
        }
    }

    public void readyToGoAnimation() {
        // Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), SyncPane);
        leave.setByY(80.0);
        leave.setCycleCount(1);
        leave.setInterpolator(Interpolator.EASE_OUT);
        leave.play();
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
    
    public class WalletListener extends AbstractWalletEventListener {
        @Override
        public void onWalletChanged(Wallet wallet) {
            checkGuiThread();
            refreshBalanceLabel();
            setAddresses();
        }
        
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        	List<TransactionOutput> outs = tx.getOutputs();
        	for (TransactionOutput out : outs){
        		if (out.isMine(Main.bitcoin.wallet())){
        			Script scr = out.getScriptPubKey();
        			String addr = scr.getToAddress(Main.params).toString();
        			try {Main.config.removeAddress(addr);} 
        			catch (IOException e) {e.printStackTrace();}
        		}
        	}
        	setAddresses();
        	refreshBalanceLabel();
        	/*try {
        	    File yourFile = new File("coins.wav");
        	    AudioInputStream stream;
        	    AudioFormat format;
        	    DataLine.Info info;
        	    Clip clip;

        	    stream = AudioSystem.getAudioInputStream(yourFile);
        	    format = stream.getFormat();
        	    info = new DataLine.Info(Clip.class, format);
        	    clip = (Clip) AudioSystem.getLine(info);
        	    clip.open(stream);
        	    clip.start();
        	}
        	catch (Exception e) {}*/
        }
        
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
        	refreshBalanceLabel();
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
   
   	//#####################################
 	//
 	//	Overview Pane
 	//
 	//#####################################
   
   @FXML protected void lockWallet(ActionEvent event){
	   
   }
   
   @FXML protected void unlockWallet(ActionEvent event){
	   
   }
    
    void setAvatar(Image img) {
        ivAvatar.setImage(img);
      }
    
    void setName(String name) {
    	lblName.setText("Welcome back, " + name);
    	lblName.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(lblName.getFont(),
                lblName.getText(), 0.0D));
    }
    
    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stg.setX(event.getScreenX() - xOffset);
    	Main.stg.setY(event.getScreenY() - yOffset);
    }
    
    @FXML protected void openOneNameDialog(ActionEvent event){
    	Main.instance.overlayUI("OneName.fxml");
    	/*Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("OneName.fxml"));
            stage = new Stage();
            stage.setTitle("OneName");
            stage.setScene(new Scene(root, 255, 110));
            stage.show();
        } catch (IOException e) {e.printStackTrace();}*/
    }
    
    public void refreshBalanceLabel() {
    	Collection<Transaction> pendingtxs= bitcoin.wallet().getPendingTransactions();
    	Coin unconfirmed = Coin.valueOf(0);
    	for (Transaction tx : pendingtxs){
    		unconfirmed = unconfirmed.add(tx.getValueSentToMe(bitcoin.wallet()));
    	}
        final Coin confirmed = (bitcoin.wallet().getBalance(Wallet.BalanceType.AVAILABLE)).subtract(unconfirmed);
        final Coin watched = bitcoin.wallet().getWatchedBalance();
        final Coin total = confirmed.add(watched);
        lblConfirmedBalance.setText(total.toFriendlyString() + " BTC");
        lblUnconfirmedBalance.setText(unconfirmed.toFriendlyString() + " BTC");
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
    
    @FXML protected void SendTx(ActionEvent event) throws IOException, JSONException{
    	try {
    		Transaction tx = new Transaction(Main.params);
    		Address destination = null;
    		for(Node n:scrlContent.getChildren()) {
    			NewAddress na = (NewAddress)n;
    			if (na.txfAddress.getText().substring(0,1).equals("+")){
    				destination = new Address(Main.params, OneName.getAddress(na.txfAddress.getText().substring(1)));
    			}
    			else {
    				destination = new Address(Main.params, na.txfAddress.getText());
    			}
    			double amount = (double) Double.parseDouble(na.txfAmount.getText())*100000000;
    			long satoshis = (long) amount;
    			if (Coin.valueOf(satoshis).compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
    				//Do something here
    			}
    			tx.addOutput(Coin.valueOf(satoshis), destination);
    		}
            Wallet.SendRequest req = Wallet.SendRequest.forTx(tx);
            if (txFee.getText().equals("")){req.feePerKb = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;}
            else {req.feePerKb = Coin.valueOf((Long.valueOf(txFee.getText()).longValue())*100000000);}
            req.changeAddress = bitcoin.wallet().getChangeAddress();
            sendResult = Main.bitcoin.wallet().sendCoins(req);
            Futures.addCallback(sendResult.broadcastComplete, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction result) {
                    //put something here
                }

                @Override
                public void onFailure(Throwable t) {
                    // We died trying to empty the wallet.
                    crashAlert(t);
                }
            });
        } catch (AddressFormatException e) {
            // Cannot happen because we already validated it when the text field changed.
            throw new RuntimeException(e);
        } catch (InsufficientMoneyException e) {
            informationalAlert("Could not empty the wallet",
                    "You may have too little money left in the wallet to make a transaction.");
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
    		txfAmount.setPromptText("Amount (bits)");
    		txfAmount.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1;");
    		b.setMargin(txfAmount, new Insets(4,200,0,0));
    		b.getChildren().add(txfAmount);
    		amountInSatoshi = new Label();
    		//amountInSatoshi.setText("bits");
    		b.getChildren().add(amountInSatoshi);
    		main.getChildren().add(b);
    		this.getChildren().add(main);
    	}
    }
    
    //#####################################
   	//
   	//	Receive Pane
   	//
   	//#####################################
    
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
            	//copy public key here     
            	ArrayList<ECKey> keypool = null;
            	try {keypool = Main.config.getKeyPool();} 
            	catch (IOException e1) {e1.printStackTrace();}
            	int pos = AddressBox.getItems().indexOf(AddressBox.getValue());
            	String pubkey = KeyUtils.bytesToHex(keypool.get(pos).getPubKey());
            	final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(pubkey);
                clipboard.setContent(content);
            }
        });
        ReceiveHBox.getChildren().add(btnCopy);
        ReceiveHBox.getChildren().add(btnQR);
        ReceiveHBox.getChildren().add(btnKey);
    }
    
    public String uri() throws AddressFormatException {
		Address addr = new Address(Main.params, AddressBox.getValue().toString());
        return BitcoinURI.convertToBitcoinURI(addr, null, Main.APP_NAME, null);
    }
    
    
    private void setAddresses(){
    	keypool = null;
		try {Main.config.fillKeyPool();} 
		catch (IOException e) {e.printStackTrace();}
		try {keypool = Main.config.getKeyPool();} 
		catch (IOException e) {e.printStackTrace();}
		AddressBox.getItems().clear();
		AddressBox.setValue(keypool.get(0).toAddress(Main.params).toString());
    	for (ECKey key : keypool){
    		String addr = key.toAddress(Main.params).toString();
    		AddressBox.getItems().addAll(addr);
    	}                              
    }    
    
    //#####################################
   	//
   	//	Transactions Pane
   	//
   	//#####################################
}
