package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.base.Joiner;

import authenticator.Authenticator;
import authenticator.db.settingsDB;
import authenticator.db.walletDB;
import authenticator.protobuf.ProtoConfig.ATOperationType;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoSettings.BitcoinUnit;
import authenticator.protobuf.ProtoSettings.Languages;
import authenticator.walletCore.exceptions.CannotGetAccountFilteredTransactionsException;
import authenticator.walletCore.exceptions.CannotGetPendingRequestsException;
import authenticator.walletCore.exceptions.CannotRemovePendingRequestException;
import authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import authenticator.walletCore.exceptions.NoWalletPasswordException;
import wallettemplate.ControllerHelpers.AsyncTask;
import wallettemplate.startup.StartupController;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.TextFieldValidator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class SettingsController  extends BaseUI{
	@FXML private Pane settingspane;
	@FXML private Pane pendingRequestsPane;
	
	@FXML private AnchorPane SettingsApp;
	@FXML private Button btnDone;
	@FXML private Button btnRestore;
	@FXML private Button btnBackup;
	@FXML private Button btnChange;
	@FXML private Button btnShowSeed;
	
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
	
	@FXML private TableView tblViewPendingRequests;
	@FXML private TableColumn colRequestID;
	@FXML private TableColumn colOperationType;
	@FXML private TableColumn colAccount;
	
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

	// Called by FXMLLoader
    @SuppressWarnings("restriction")
	public void initialize() {        	
    	String unit = Authenticator.getWalletOperation().getAccountUnitFromSettings().toString();
    	if (unit.equals("BTC")){this.strBitcoinUnit="BTC";}
    	else if (unit.equals("Bits")){this.strBitcoinUnit="bits";}
    	else if (unit.equals("Millibits")){this.strBitcoinUnit="mBTC";}
    	else {this.strBitcoinUnit="µBTC";}
    	this.intDecimal = Authenticator.getWalletOperation().getDecimalPointFromSettings();
    	this.strCurrency = Authenticator.getWalletOperation().getLocalCurrencySymbolFromSettings();
    	this.strLanguage = Authenticator.getWalletOperation().getLanguageFromSettings().toString();
    	this.useTor = Authenticator.getWalletOperation().getIsUsingTORFromSettings();
    	this.localHost = Authenticator.getWalletOperation().getIsConnectingToLocalHostFromSettings();
    	this.TrustedPeer = Authenticator.getWalletOperation().getIsConnectingToTrustedPeerFromSettings();
    	this.falsePositiveRate = Authenticator.getWalletOperation().getBloomFilterFalsePositiveRateFromSettings();
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
    	fee = (double) Authenticator.getWalletOperation().getDefaultFeeFromSettings();
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
    		txPeerIP.setText(Authenticator.getWalletOperation().getTrustedPeerIPFromSettings());
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

        // pending requests table
        tblViewPendingRequests.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    	colRequestID.setCellValueFactory(new PropertyValueFactory<TablePendingReq,String>("reqID"));
    	colOperationType.setCellValueFactory(new PropertyValueFactory<TablePendingReq,ImageView>("OpType"));
    	colAccount.setCellValueFactory(new PropertyValueFactory<TablePendingReq,String>("accountName"));
    	new PendingRquestsUpdater(tblViewPendingRequests, 
    			colRequestID,
    			colOperationType,
    			colAccount).execute();
    }
    
    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stage.setX(event.getScreenX() - xOffset);
    	Main.stage.setY(event.getScreenY() - yOffset);
    }

	public void exit(ActionEvent event) throws CannotWriteToConfigurationFileException {
		overlayUi.done();
		// bitcoin units
		Authenticator.getWalletOperation().setAccountUnitInSettings(BitcoinUnit.Bits);
		if (strBitcoinUnit.equals("BTC")){Authenticator.getWalletOperation().setAccountUnitInSettings(BitcoinUnit.BTC);}
    	else if (strBitcoinUnit.equals("bits")){Authenticator.getWalletOperation().setAccountUnitInSettings(BitcoinUnit.Bits);}
    	else if (strBitcoinUnit.equals("mBTC")){Authenticator.getWalletOperation().setAccountUnitInSettings(BitcoinUnit.Millibits);}
    	else {Authenticator.getWalletOperation().setAccountUnitInSettings(BitcoinUnit.NanoBits);}
		
		Authenticator.getWalletOperation().setDecimalPointInSettings(intDecimal);
		
		Authenticator.getWalletOperation().setLocalCurrencySymbolInSettings(strCurrency);
		
		Authenticator.getWalletOperation().setLanguageInSettings(Languages.English);
		
		Authenticator.getWalletOperation().setIsUsingTORInSettings(useTor);
		
		Authenticator.getWalletOperation().setIsConnectingToLocalHostInSettings(localHost);
		
		Authenticator.getWalletOperation().setIsConnectingToTrustedPeerInSettings(TrustedPeer, peerIP);

		Authenticator.getWalletOperation().setBloomFilterFalsePositiveRateInSettings((float)slBloom.getValue());
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
	
	@FXML protected void goToPendingRequests(ActionEvent event){
		this.settingspane.setVisible(false);
		this.pendingRequestsPane.setVisible(true);
	}
    
	@FXML protected void backToSettingsPane(ActionEvent event){
		this.settingspane.setVisible(true);
		this.pendingRequestsPane.setVisible(false);
	}
	
	public void deleteSelected(ActionEvent event) {
		Set<TablePendingReq> selection = new HashSet<TablePendingReq>(tblViewPendingRequests.getSelectionModel().getSelectedItems());
		if(selection.size() == 0)
			return;
		
		
		try {
			List<PendingRequest> prs = new ArrayList<PendingRequest> ();
			for(TablePendingReq d: selection) {
				prs.add(d.getPendingRequest());
			}
			Authenticator.getWalletOperation().removePendingRequest(prs);
			
			new PendingRquestsUpdater(tblViewPendingRequests, 
	    			colRequestID,
	    			colOperationType,
	    			colAccount).execute();
		} catch (CannotRemovePendingRequestException e) {
			e.printStackTrace();
			informationalAlert("Cannot delete selected",
					"Try again");
		}
	}
	
	//################################
	//
	//	Pending transactions data
	//
	//################################
	
	public class TablePendingReq {
		private PendingRequest pr;
		private String reqID;
		private String OpType;
		private String accountName;
		private Boolean isSelected; 
		
		public TablePendingReq(PendingRequest pr, String reqID, String OpType, String accountName) {
			this.pr = pr;
			this.reqID = reqID;
			this.OpType = OpType;
			this.accountName = accountName;
			this.isSelected = false;
		}
		
		public PendingRequest getPendingRequest() {
			return pr;
		}
		
		public String getReqID() {
			return reqID;
		}
		
		public String getOpType() {
			return OpType;
		}
		
		public String getAccountName() {
			return accountName;
		}
		
		public Boolean getIsSelected() {
			return isSelected;
		}
	}
	
	private class PendingRquestsUpdater extends AsyncTask{
				
		ObservableList<TablePendingReq> data;


		TableView table;
		TableColumn colReqID;
		TableColumn colOpType;
		TableColumn colAccountName;
		
		public PendingRquestsUpdater(TableView table, 
				TableColumn colReqID, 
				TableColumn colOpType,
				TableColumn colAccountName){
			this.table = table;
			this.colReqID = colReqID;
			this.colOpType = colOpType;
			this.colAccountName = colAccountName;
		}

		protected void doInBackground() {
			try {
				data = getTxData();
			} catch (Exception e) { e.printStackTrace(); }
		}
		
		@SuppressWarnings({ "deprecation", "restriction" })
		private ObservableList<TablePendingReq> getTxData() throws CannotGetPendingRequestsException {
			ObservableList<TablePendingReq> ret = FXCollections.observableArrayList();
			List<PendingRequest> all = Authenticator.getWalletOperation().getPendingRequests();
	    	for (PendingRequest pr : all){
	    		try {
	    			int accountIndex = Authenticator.getWalletOperation().getPairingObject(pr.getPairingID()).getWalletAccountIndex();
	    			String accountName = Authenticator.getWalletOperation().getAccount(accountIndex).getAccountName();
	    			TablePendingReq n = new TablePendingReq(pr, pr.getRequestID(), operationTypeToString(pr.getOperationType()), accountName);
	    			ret.add(n);
	    		}
	    		catch (Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
			
	    	return ret;
		}

		private String operationTypeToString(ATOperationType type) {
			switch(type) {
			case Pairing:
				return "Pairing Operation";
			case Unpair:
				return "Unpair Operation";
			case SignAndBroadcastAuthenticatorTx:
				return "Sign and Broadcast paired Tx Operation";
			case BroadcastNormalTx:
				return "Broadcast Normal Operation";
			case updateIpAddressesForPreviousMessage:
				return "Update IPs Operation";
			}
			return "---";
		}
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void onPostExecute() {
			table.setItems(data);
		}

		@Override
		protected void progressCallback(Object... params) {
			// TODO Auto-generated method stub
			
		}

	}
}
