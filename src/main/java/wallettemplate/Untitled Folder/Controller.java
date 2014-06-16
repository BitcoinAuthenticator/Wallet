package wallettemplate;

import authenticator.Authenticator;
import authenticator.WalletOperation;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.db.WalletFile;
import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;
import authenticator.ui_helpers.PopUpNotification;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import wallettemplate.controls.ClickableBitcoinAddress;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.BaseUI;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;

import static wallettemplate.utils.GuiUtils.checkGuiThread;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class Controller extends BaseUI{
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
	 @FXML private ProgressBar syncProgress;
	 @FXML private Button btnAvatar;
	 @FXML private Button btnAdd;
	 @FXML private Button btnSend;
	 @FXML private HBox ReceiveHBox;
	 @FXML private Label lblConfirmedBalance;
	 @FXML private Label lblUnconfirmedBalance;
	 @FXML private ChoiceBox AddressBox;
	 @FXML ImageView ivAvatar;
	 @FXML Label lblName;
	 private double xOffset = 0;
	 private double yOffset = 0;
	 public ScrollPane scrlpane;
	 private ScrollPaneContentManager scrlContent;
	 public static Stage stage;
	
	//from old controller 
    public VBox syncBox;
    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;
    public ArrayList<ClickableBitcoinAddress> addressArr;

    // Called by FXMLLoader.
    public void initialize() {
        syncProgress.setProgress(-1);
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
    
    void setAvatar(Image img) {
        ivAvatar.setImage(img);
      }
    
    void setName(String name) {
    	lblName.setText("Welcome back, " + name);
    	lblName.setPrefWidth(wallettemplate.utils.TextUtils.computeTextWidth(lblName.getFont(),
                lblName.getText(), 0.0D));
    }
    
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
    
    @FXML protected void drag1(MouseEvent event) {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        }
    
    @FXML protected void drag2(MouseEvent event) {
        Main.stg.setX(event.getScreenX() - xOffset);
        Main.stg.setY(event.getScreenY() - yOffset);
    }
    
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
    
    @FXML protected void add(){
    	addOutput();
    }
    
    private void addOutput()
    {
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
    public void removeOutput(int index)
    {
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
    
    public class NewAddress extends HBox
    {
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
    
    @FXML protected void openOneNameDialog(ActionEvent event){
    	Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("OneName.fxml"));
            stage = new Stage();
            stage.setTitle("OneName");
            stage.setScene(new Scene(root, 255, 110));
            stage.show();
        } catch (IOException e) {e.printStackTrace();}
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
        ReceiveHBox.getChildren().add(btnCopy);
        ReceiveHBox.getChildren().add(btnQR);
        ReceiveHBox.getChildren().add(btnKey);
    }

    public void onBitcoinSetup() {
        Authenticator.getWalletOperation().addEventListener(new BalanceUpdater());
        Main.mainController.loadAddresses();
        refreshBalanceLabel();
    }
    
    public void refreshUI(){
    	refreshBalanceLabel();
    	Main.mainController.loadAddresses();
    }

    public class ProgressBarUpdater extends DownloadListener {
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
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
        // Buttons slide in and clickable address appears simultaneously.
        /*TranslateTransition arrive = new TranslateTransition(Duration.millis(600), controlsBox);
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(500), scrlContent);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        // Slide out happens then slide in/fade happens.
        SequentialTransition both = new SequentialTransition(leave, group);*/
        leave.setCycleCount(1);
        leave.setInterpolator(Interpolator.EASE_OUT);
        leave.play();
    }

    public ProgressBarUpdater progressBarUpdater() {
        return new ProgressBarUpdater();
    }

    public class BalanceUpdater extends AbstractWalletEventListener {
        @Override
        public void onWalletChanged(Wallet wallet) {
            checkGuiThread();
            Main.mainController.refreshBalanceLabel();
            Main.mainController.refreshPairedWalletsBalance();
        }
    }

    public void refreshBalanceLabel() {
        @SuppressWarnings("static-access")
		final Coin confirmedAmount = Authenticator.getWalletOperation().getAccountConfirmedBalance();//bitcoin.wallet().getBalance(Wallet.BalanceType.ESTIMATED);
        lblConfirmedBalance.setText(confirmedAmount.toFriendlyString() + " BTC");
        final Coin unconfirmedAmount = Authenticator.getWalletOperation().getAccountUnconfirmedBalance();
        lblUnconfirmedBalance.setText(unconfirmedAmount.toFriendlyString() + " BTC");
    }
    
    @FXML
    private void handlePairDeviceAction(ActionEvent event) { 
    	Main.instance.overlayUI("Pair_wallet.fxml",null);
    }
    
    @FXML
    private void handleCreateP2ShAddress(ActionEvent event) throws IOException { 
    	Main.instance.overlayUI("New_p2sh_address.fxml",null);
    }
    
    @FXML
    private void handleBackupAddress(ActionEvent event) throws IOException { 
    	Main.instance.overlayUI("Backup_SSS.fxml",null);
    }
    
    
    @SuppressWarnings("static-access")
	private void loadAddresses()
    {
    	AddressBox.getItems().addAll(Authenticator.getWalletOperation().freshReceiveKey().toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
    	AddressBox.setValue(Authenticator.getWalletOperation().currentReceiveKey().toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
    	for (int i=0; i<9; i++){
    		AddressBox.getItems().addAll(Authenticator.getWalletOperation().freshReceiveKey().toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
    	}
    	//scrlContent.clearAll();
    	addressArr = new ArrayList<ClickableBitcoinAddress>();
    	//Normal P2PSH from wallet
    	/*ClickableBitcoinAddress normalAddressControl = new ClickableBitcoinAddress(false);
    	normalAddressControl.setAddress(Authenticator.getWalletOperation().currentReceiveKey().toAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
    	normalAddressControl.setPairName("Pay-To-Pub-Hash");
    	normalAddressControl.setBalance(Utils.bitcoinValueToFriendlyString(Authenticator.getWalletOperation().getGeneralWalletEstimatedBalance()));
    	scrlContent.addItem(normalAddressControl);
    	addressArr.add(normalAddressControl);*/
    	
    	//Load P2SH addresses
    	ArrayList<PairingObject> arr = Authenticator.getWalletOperation().getAllPairingObjectArray();
    	if(arr != null)
    	for(PairingObject po:arr)
    	{
    		// get address or generate it
    		String add = null;
    		if(po.keys_n > 0){
    			KeyObject ko = po.keys.keys.get(po.keys_n-1);
    			add = ko.address;
    		}
    		else
    			add = generateFreshAuthenticatorP2SHAddress(po.pairingID);
    		
    		//add to scroll content
    		if(add != null)
    		{
    			ClickableBitcoinAddress addressControl = new ClickableBitcoinAddress(true);
        		//addressControl.setAddress(bitcoin.wallet().currentReceiveKey().toAddress(Main.params).toString());
        		addressControl.setAddress(add);
        		addressControl.setPairName(po.pairingName);
        		addressControl.setPairID(po.pairingID);
        		Coin balance = null;
        		try {
					balance = Authenticator.getWalletOperation().getBalance(po.pairingID);
				} catch (ScriptException | UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
        		if (balance==null){addressControl.setBalance("0");}
        		else {addressControl.setBalance(balance.toFriendlyString());}
        		//add to content
        		//scrlContent.addItem(addressControl);
        		// add to addresses array
        		addressArr.add(addressControl);
    		}
    	}
    }
    
    private void refreshPairedWalletsBalance()
    {
    	Main.mainController.loadAddresses();
    }
    
    public String generateFreshAuthenticatorP2SHAddress(String pairID) {
    	String ret = null;
		try {
			ret = Authenticator.getWalletOperation().genP2SHAddress(pairID);
		} catch (AddressFormatException | NoSuchAlgorithmException | JSONException e) {
			Authenticator.getWalletOperation().LOG.info(e.toString());
			PopUpNotification p = new PopUpNotification("Something Went Wrong ...","");
			p.showPopup();
			e.printStackTrace();
		} 
		return ret;
    }
}
