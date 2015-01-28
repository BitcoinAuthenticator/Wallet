package org.wallet.apps;

import static org.wallet.utils.GuiUtils.informationalAlert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.authenticator.Utils.ExchangeProvider.ExchangeProvider;
import org.bitcoinj.core.Coin;
import org.bitcoinj.wallet.DeterministicSeed;
import org.wallet.ControllerHelpers.AsyncTask;
import org.wallet.Main;
import org.wallet.startup.StartupController;
import org.wallet.utils.BaseUI;
import org.wallet.utils.TextFieldValidator;
import org.wallet.utils.TextUtils;
import org.wallet.utils.dialogs.BADialog;
import org.wallet.utils.dialogs.BADialog.BADialogResponse;
import org.wallet.utils.dialogs.BADialog.BADialogResponseListner;

import com.google.protobuf.Descriptors.EnumValueDescriptor;

import org.authenticator.Authenticator;
import org.authenticator.Utils.FileUtils;
import org.authenticator.listeners.BAGeneralEventsAdapter;
import org.authenticator.protobuf.ProtoSettings;
import org.authenticator.protobuf.ProtoConfig.ATOperationType;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.protobuf.ProtoSettings.BitcoinUnit;
import org.authenticator.protobuf.ProtoSettings.Languages;
import org.authenticator.walletCore.exceptions.CannotGetPendingRequestsException;
import org.authenticator.walletCore.exceptions.CannotRemovePendingRequestException;
import org.authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import org.authenticator.walletCore.exceptions.WrongWalletPasswordException;
import org.authenticator.walletCore.utils.BAPassword;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class SettingsAppController extends BaseUI{
	@FXML private Pane settingspane;
	@FXML private Pane pendingRequestsPane;
	
	@FXML private Label lblAppVersion;
	
	@FXML private AnchorPane SettingsApp;
	@FXML private Button btnSave;
	@FXML private Button btnRestore;
	@FXML private Button btnBackup;
	@FXML private Button btnShowSeed;

	@FXML private Button btnChange;
	@FXML private PasswordField changePWOriginal;
	@FXML private TextField changePWNewFirst;
	@FXML private TextField changePWNewSecond;
	
	@FXML private ChoiceBox cbCurrency;
	@FXML private ChoiceBox cbDecimal;
	@FXML private ChoiceBox cbBitcoinUnit;
	@FXML private ChoiceBox cbLanguage;
	@FXML private Slider slBloom;
	@FXML private Label lblBloomFilterRate;
	@FXML private CheckBox ckTor;
	@FXML private CheckBox ckLocalHost;
	@FXML private CheckBox ckPortForwarding;
	@FXML private CheckBox ckTrustedPeer;
	@FXML private TextField txFee;
	@FXML private TextField txPeerIP;
	@FXML private PasswordField txfShowSeedPassword;
	
	@FXML private Button btnDeleteWallet;
	
	@FXML private TableView tblViewPendingRequests;
	@FXML private TableColumn colRequestID;
	@FXML private TableColumn colOperationType;
	@FXML private TableColumn colAccount;
	
	public Main.OverlayUI overlayUi;
	private double xOffset = 0;
	private double yOffset = 0;
	
	private BitcoinUnit unit;
	private int intDecimal;
	private String strCurrency;
	private String strLanguage;
	private boolean useTor;
	private boolean localHost;
	private boolean portForwarding;
	private boolean TrustedPeer;
	private String strFee;
	private double falsePositiveRate;
	private Coin fee;

	// Called by FXMLLoader
    @SuppressWarnings("restriction")
	public void initialize() {     
    	super.initialize(SettingsAppController.class);
    	 initUI();
    }
    
    private void initUI() {    	
    	lblAppVersion.setText(Authenticator.getApplicationParams().getFriendlyAppVersion());
    	
    	this.intDecimal 		= Authenticator.getWalletOperation().getDecimalPointFromSettings();
    	this.strCurrency 		= Authenticator.getWalletOperation().getLocalCurrencySymbolFromSettings();
    	this.strLanguage 		= Authenticator.getWalletOperation().getLanguageFromSettings().toString();
    	this.useTor 			= Authenticator.getWalletOperation().getIsUsingTORFromSettings();
    	this.localHost 			= Authenticator.getWalletOperation().getIsConnectingToLocalHostFromSettings();
    	this.portForwarding 	= Authenticator.getWalletOperation().getIsPortForwarding();
    	this.TrustedPeer 		= Authenticator.getWalletOperation().getIsConnectingToTrustedPeerFromSettings();
    	this.falsePositiveRate 	= Authenticator.getWalletOperation().getBloomFilterFalsePositiveRateFromSettings();
    	
    	Tooltip.install(slBloom, new Tooltip(String.valueOf(slBloom.getValue())));
    	this.lblBloomFilterRate.setText(String.format( "%.5f", falsePositiveRate ));
    	
    	unit = Authenticator
    			.getWalletOperation()
    			.getAccountUnitFromSettings();
    	String unitStr = unit.getValueDescriptor()
			    			.getOptions()
			    			.getExtension(ProtoSettings.bitcoinUnitName);
    	cbBitcoinUnit.setValue(unitStr);
    	
    	cbCurrency.getItems().clear();
    	for(String s: ExchangeProvider.AVAILBLE_CURRENCY_CODES)
    		cbCurrency.getItems().add(s);
    	cbCurrency.setValue(strCurrency);
    	cbCurrency.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
  	      @Override
  	      public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
  	    	if((Integer) number2 > 0) {
    			strCurrency = cbCurrency.getItems().get((Integer) number2).toString();
    			setSaveButtonDisabled(false);
  	    	}
  	      }
  	    });
    	
    	fee = Authenticator.getWalletOperation().getDefaultFeeFromSettings();
    	txFee.setPromptText(fee.toFriendlyString());
       	txFee.focusedProperty().addListener(new ChangeListener<Boolean>()
    			{
					@Override
					public void changed(ObservableValue<? extends Boolean> arg0,Boolean arg1, Boolean arg2) {
						try {
							float inFraction = Float.parseFloat(txFee.getText());
							fee = Coin.valueOf(TextUtils.bitcoinUnitToSatoshies(inFraction, unit));
						}
						catch(Exception e) {
							fee = Authenticator.getWalletOperation().getDefaultFeeFromSettings();
						}
						
				    	String strFee = TextUtils.coinAmountTextDisplay(fee, unit);
				    	txFee.clear();
				    	txFee.setPromptText(strFee);
				    	
				    	setSaveButtonDisabled(false);
					}
    			});
    	
    	
    	//Still need to round decimal values in the prompt text.
    	cbBitcoinUnit.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
    	      @Override
    	      public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
    	    	  	if ((Integer) number2 == 0)
    	    	  		unit = BitcoinUnit.BTC;
    	    		else if ((Integer) number2 == 1)
    	    			unit = BitcoinUnit.Millibits;
    	    		else
    	    			unit = BitcoinUnit.Microbits;
    	    	  	
    	    	  	txFee.setPromptText("Fee: " + TextUtils.coinAmountTextDisplay(fee, unit) + " " + TextUtils.getAbbreviatedUnit(unit));
    	    	  	
    	    	  	setSaveButtonDisabled(false);
    	      }
    	    });
    	cbDecimal.setValue(String.valueOf(intDecimal));
    	cbDecimal.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
    	      @Override
    	      public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
    	    		intDecimal = Integer.parseInt(cbDecimal.getItems().get((Integer) number2).toString());
    	      
    	    		setSaveButtonDisabled(false);
    	      }
    	    });
    	
    	cbLanguage.setValue(strLanguage);
    	
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
				 
				 setSaveButtonDisabled(false);
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
				 
				 setSaveButtonDisabled(false);
			 }
		 });
    	ckPortForwarding.setSelected(portForwarding);
    	ckPortForwarding.selectedProperty().addListener(new ChangeListener<Boolean>() {
			 public void changed(ObservableValue<? extends Boolean> ov,
					 Boolean old_val, Boolean new_val) {
					 portForwarding = ckPortForwarding.isSelected();
					 
					 setSaveButtonDisabled(false);
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
    	slBloom.valueProperty().addListener((observable, oldValue, newValue) -> {
    		falsePositiveRate = ((double)newValue / 100000);
    		lblBloomFilterRate.setText(String.format( "%.5f", falsePositiveRate ));
    		
    		setSaveButtonDisabled(false);
    	});
    	
    	
    	
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
				 
				 setSaveButtonDisabled(false);
			 }
		 });
    	btnSave.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnSave.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
    	
    	
    	
    	btnSave.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnSave.setStyle("-fx-background-color: #b3b1b1;");
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
    	Authenticator.addGeneralEventsListener(new BAGeneralEventsAdapter() {
    		@Override
    		public void onPendingRequestUpdate(List<PendingRequest> requests, PendingRequestUpdateType updateType) {
    			new PendingRquestsUpdater(tblViewPendingRequests, 
    	    			colRequestID,
    	    			colOperationType,
    	    			colAccount).execute();
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
    
    private void setSaveButtonDisabled(boolean value) {
    	this.btnSave.setDisable(value);
    }

    public void exit(ActionEvent event) {
    	overlayUi.done();
    }
    
	public void save(ActionEvent event) throws CannotWriteToConfigurationFileException {
		setSaveButtonDisabled(true);

		//String strV = (String)cbBitcoinUnit.getSelectionModel().getSelectedItem();
		EnumValueDescriptor desc = BitcoinUnit
				.getDescriptor()
				.findValueByNumber(cbBitcoinUnit.getSelectionModel().getSelectedIndex());
		BitcoinUnit carmodel =   BitcoinUnit.valueOf(desc);
		Authenticator.getWalletOperation().setAccountUnitInSettings(carmodel);

		Authenticator.getWalletOperation().setDefaultFeeInSettings(fee.getValue());

		Authenticator.getWalletOperation().setDecimalPointInSettings(intDecimal);

		Authenticator.getWalletOperation().setLocalCurrencySymbolInSettings(strCurrency);

		Authenticator.getWalletOperation().setLanguageInSettings(Languages.English);

		Authenticator.getWalletOperation().setIsUsingTORInSettings(useTor);

		Authenticator.getWalletOperation().setIsConnectingToLocalHostInSettings(localHost);

		if(Authenticator.getWalletOperation().getIsPortForwarding() != portForwarding)
			 Platform.runLater(() -> {
				 informationalAlert("Important !",
							"Please restart your wallet so the settings will take effect");
			 } );
		Authenticator.getWalletOperation().setIsPortForwarding(portForwarding);

		Authenticator.getWalletOperation().setIsConnectingToTrustedPeerInSettings(TrustedPeer, txPeerIP.getText());

		Authenticator.getWalletOperation().setBloomFilterFalsePositiveRateInSettings(falsePositiveRate);

		Authenticator.fireOnWalletSettingsChange();
    }
	
	public static Stage backupPane;
	@SuppressWarnings("restriction")
	public void launchBackup(ActionEvent event) throws WrongWalletPasswordException {
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
        	URL location = getClass().getResource("/org/wallet/startup/walletstartup.fxml");
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
            controller.setBackupMode(Authenticator.getWalletOperation().getTrackedWallet(), seed);
            backupPane.show();
        } catch (IOException e) {e.printStackTrace();}
    }
	
	@FXML protected void showSeed(ActionEvent event){
		if(Main.UI_ONLY_IS_WALLET_LOCKED) {
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
	
	@FXML protected void changePassword(ActionEvent event){
		if(Main.UI_ONLY_WALLET_PW.hasPassword()) {
			if(Main.UI_ONLY_WALLET_PW.compareTo(changePWOriginal.getText())) {
				changePassword(changePWOriginal.getText(), 
						changePWNewFirst.getText(), 
						changePWNewSecond.getText());
			}
			else
				informationalAlert("Unfortunately, you messed up.",
	 					 "You need to enter the correct original password.");
		}
		
		else if(Authenticator.getWalletOperation().isWalletEncrypted()) {
			BAPassword pw = new BAPassword(changePWOriginal.getText());
			try {
				Authenticator.getWalletOperation().decryptWallet(pw);
				Authenticator.getWalletOperation().encryptWallet(pw);
				
				changePassword(changePWOriginal.getText(), 
						changePWNewFirst.getText(), 
						changePWNewSecond.getText());
			} catch (WrongWalletPasswordException e) {
				informationalAlert("Unfortunately, you messed up.",
	 					 "You need to enter the correct original password.");
			}
		}
		else {
			changePassword(changePWOriginal.getText(), 
					changePWNewFirst.getText(), 
					changePWNewSecond.getText());
		}
	}
	
	private void changePassword(String original, String pw1, String pw2) {
		try {
			//check both passwrods match
			if(!pw1.equals(pw2)) {
				informationalAlert("Unfortunately, you messed up.",
						 "New passwords do not match.");
				return;
			}
				
			// decrypt wallet
			if(Authenticator.getWalletOperation().isWalletEncrypted()) {
				BAPassword originalPW = new BAPassword(original);
				Authenticator.getWalletOperation().decryptWallet(originalPW);
			}
			
			// encrypt with new password
			BAPassword newPW = new BAPassword(pw1);
			Authenticator.getWalletOperation().encryptWallet(newPW);
			
			//set new password
			Main.UI_ONLY_WALLET_PW.setPassword(pw1);
			
			// set to locked
			Main.UI_ONLY_IS_WALLET_LOCKED = true;	
			
			informationalAlert("Success !",
					 "Changed to new password");
		}
		catch (Exception e) {
			informationalAlert("Unfortunately, we messed up.",
					 "Cannot change password.");
			
			// make sure wallet is encrypted with original PW
			if(!Authenticator.getWalletOperation().isWalletEncrypted()) {
				BAPassword originalPW = new BAPassword(original);
				try {
					Authenticator.getWalletOperation().decryptWallet(originalPW);
				} catch (WrongWalletPasswordException e1) { e1.printStackTrace(); }
			}
		}
		
	}
	
	@FXML protected void deleteWallet(ActionEvent event){
		BADialog.confirm(Main.class, "Warning !",
				"You are about to delete your wallet !!!\n"
        		+ "Deleting the wallet will cause you to loose all related data and the ability to access your coins\n\n"
        		+ "Are you sure your wallet is backed up and you wish to continue ?\n",
        		new BADialogResponseListner(){
						@Override
						public void onResponse(BADialogResponse response,String input) {
							if(response == BADialogResponse.Yes)
							{
								if(FileUtils.deleteDirectory(new File(Authenticator.getApplicationParams().getApplicationDataFolderAbsolutePath()))) {
									informationalAlert("Deleted wallet",
											 "Will shut down.");
									
									Runtime.getRuntime().exit(0);
								}
								else
									informationalAlert("Failed !",
											 "Could not delete wallet\n" + "Try and launching the wallet with the right permissions");
							}
						}
					}).show();
	}
		
	@FXML protected void restoreSettingsToDefault(ActionEvent event){
		try {
			Authenticator.getWalletOperation().resotreSettingsToDefault();
			this.initUI();
			informationalAlert("Success !",
					 "Restored Settings To Default");
		} catch (CannotWriteToConfigurationFileException e) {
			e.printStackTrace();
			informationalAlert("Failed !",
					 "Could not restore settings to default");
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
