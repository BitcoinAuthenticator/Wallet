package org.wallet.apps;

import static org.wallet.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.authenticator.Utils.CryptoUtils;
import org.wallet.Main;
import org.wallet.apps.AuthenticatorAppController.PairingWalletControllerListener;
import org.wallet.controls.DisplayAccountCell;
import org.wallet.controls.ScrollPaneContentManager;
import org.wallet.controls.DisplayAccountCell.AccountCellEvents;
import org.wallet.utils.BaseUI;
import org.wallet.utils.GuiUtils;
import org.wallet.utils.TextFieldValidator;
import org.wallet.utils.dialogs.BADialog;
import org.wallet.utils.dialogs.BADialog.BADialogResponse;
import org.wallet.utils.dialogs.BADialog.BADialogResponseListner;

import org.authenticator.Authenticator;
import org.authenticator.BAApplicationParameters.NetworkType;
import org.authenticator.protobuf.ProtoConfig.ATAccount;
import org.authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import org.authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import org.authenticator.walletCore.exceptions.WrongWalletPasswordException;
import org.authenticator.walletCore.utils.BAPassword;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.bitcoinj.core.Coin;

public class AccountsAppController extends BaseUI{
	@FXML public ScrollPane scrlPane;
	@FXML public TextField txfNewAccountName;
	@FXML public TextField txfPassword;
	@FXML public Button btnDeleteAccount;
	@FXML public Button btnRenameAccount;
	@FXML public Button btnRepairAccount;
	@FXML public Label lblTotal;
	
	private DisplayAccountCell currentSelectedCell = null;
	
	public Main.OverlayUI overlayUi;
	
	private ScrollPaneContentManager scrlPaneContentManager;
	
	public void initialize() {
		super.initialize(AccountsAppController.class);
		scrlPaneContentManager = new ScrollPaneContentManager()
									.setSpacingBetweenItems(0)
									.setScrollStyle(scrlPane.getStyle());
		scrlPane.setContent(scrlPaneContentManager);
		setupContent();
	}
	
	private void setupContent(){
		scrlPaneContentManager.clearAll();
		List<ATAccount> all = Authenticator.getWalletOperation().getAllAccounts();
		Coin tot = Coin.ZERO;
		for(ATAccount acc:all){
			AccountUI ui = new AccountUI(acc);
			scrlPaneContentManager.addItem(ui.getNode());
			
			tot = tot.add(Coin.valueOf(acc.getUnConfirmedBalance()).add(Coin.valueOf(acc.getConfirmedBalance())));
		}
		
		//
		lblTotal.setText(tot.toFriendlyString());
		
		// clear txt fields
		txfNewAccountName.clear();
		txfPassword.clear();
		
		// delete and rename buttons
		if(currentSelectedCell == null) 
		{
			btnDeleteAccount.setDisable(true);
			btnRenameAccount.setDisable(true);
		}
	}
		
	
	private class AccountUI{
		private ATAccount account;
		public AccountUI(ATAccount account){
			this.account = account;
		}
		
		private class EventHandler implements AccountCellEvents
		{
			@SuppressWarnings("restriction")
			@Override
			public void onClick(DisplayAccountCell cell) {
				// in case a selected cell is clicked
				if(currentSelectedCell != null && cell.getAccount().getIndex() == currentSelectedCell.getAccount().getIndex()) {
					Platform.runLater(() -> {
						btnDeleteAccount.setDisable(true);
						btnRenameAccount.setDisable(true);						
						btnRepairAccount.setDisable(true);
					});
					currentSelectedCell.setSelected(false);
					currentSelectedCell = null;
					return;
				}
				
				Platform.runLater(() -> {
					btnDeleteAccount.setDisable(false);
					btnRenameAccount.setDisable(false);
					if(cell.getAccount().getAccountType() == WalletAccountType.AuthenticatorAccount)
						btnRepairAccount.setDisable(false);
					else
						btnRepairAccount.setDisable(true);
				});			
				
				if(currentSelectedCell != null)
					currentSelectedCell.setSelected(false);
				currentSelectedCell = cell;
				currentSelectedCell.setSelected(true);
			}
		}
		
		@SuppressWarnings("restriction")
		public Node getNode(){
			
			DisplayAccountCell c = new DisplayAccountCell(this.account)
			.setListener(new EventHandler());
			return c;
		}
	}
	
	@FXML protected void addAccount(ActionEvent event){
		if((!Main.UI_ONLY_WALLET_PW.hasPassword() && Authenticator.getWalletOperation().isWalletEncrypted())
				&& txfPassword.getText().length() == 0) {
			informationalAlert("Your wallet is locked",
					"Please enter your wallet's password");
			return;
		}
		
		if(txfNewAccountName.getText().length() == 0)
		{
			informationalAlert("Error",
					"Please enter an account name");
			return;
		}
		
		try {
			BAPassword pass = new BAPassword(txfPassword.getText());
			ATAccount newAcc = Authenticator.getWalletOperation().generateNewStandardAccount(NetworkType.MAIN_NET, txfNewAccountName.getText(), pass);
			setupContent();
		
		} catch (IOException | WrongWalletPasswordException e) {
			e.printStackTrace();
			GuiUtils.informationalAlert("Error !", "Error occured while creating the account.\n"
					+ "The password may be wrong");
		}
	}
	
	@FXML protected void renameAccount(ActionEvent event)
	{
		if(currentSelectedCell == null)
			return;
		
		BADialog.input(Main.class, "Change Account Name", "Please enter a new name:",
        		new BADialogResponseListner(){
						@Override
						public void onResponse(BADialogResponse response,String input) {
							if(response == BADialogResponse.Ok)
							{
								try {
									ATAccount t = Authenticator.getWalletOperation().setAccountName(input, currentSelectedCell.getAccount().getIndex());
									currentSelectedCell.setAccount(t);
									currentSelectedCell.updateUI();
								} catch (CannotWriteToConfigurationFileException e) { 
									e.printStackTrace(); 
									GuiUtils.informationalAlert("Error !", "Error occured while changing account's name.");
								}
							}
						}
					}).show();
	}
	
	@FXML protected void deleteAccount(ActionEvent event)
	{
		if(currentSelectedCell == null)
			return;
		
		/**
		 * check at least one account remains
		 */
		if(Authenticator.getWalletOperation().getAllAccounts().size() < 2){
			GuiUtils.informationalAlert("Cannot Remove account !", "At least one active account should remain.");						
			return;
		}
		
		/**
		 * Check is not the active account
		 */
		if(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex() == currentSelectedCell.getAccount().getIndex()){
			GuiUtils.informationalAlert("Cannot Remove account !", "The account for removal is the active wallet's account.\n"
	        		+ "Change the wallet's active account and try again.");
			return;
		}
		
		BADialog.confirm(Main.class, "Warning !","You are about to delete the \"" + currentSelectedCell.getAccount().getAccountName() + "\" account !\n"
        		+ "Deleting the account will delete all information about it.\n"
        		+ "Do you wish do continue ?\n",
        		new BADialogResponseListner(){
						@Override
						public void onResponse(BADialogResponse response,String input) {
							if(response == BADialogResponse.Yes)
							{
								try {
									Authenticator.getWalletOperation().removeAccount(currentSelectedCell.getAccount().getIndex());
									currentSelectedCell = null;
									setupContent();
									GuiUtils.informationalAlert("Operation Complete", "Account was deleted");
									
								} catch (IOException e) { 
									e.printStackTrace();
									GuiUtils.informationalAlert("Error !", "Error occured while deleting the account.");
								}
							}
						}
					}).show();
	}
	
	Stage pairingStage;
	@FXML protected void repairAccount(ActionEvent event)
	{
		if(currentSelectedCell != null) { // repair a selected account
			PairedAuthenticator op = Authenticator.getWalletOperation().getPairingObjectForAccountIndex(currentSelectedCell.getAccount().getIndex());

			String decryptedAES = op.getAesKey();
			if(op.getIsEncrypted()) {
				{
					if((!Main.UI_ONLY_WALLET_PW.hasPassword() && Authenticator.getWalletOperation().isWalletEncrypted())
							&& txfPassword.getText().length() == 0) {
						informationalAlert("Your wallet is locked",
								"Please unlock your wallet");
						return;
					}
				}

				try {
					decryptedAES = Authenticator.getWalletOperation().getAESKey(op.getPairingID(), Main.UI_ONLY_WALLET_PW);
				} catch (WrongWalletPasswordException | CryptoUtils.CannotDecryptMessageException e) {
					e.printStackTrace();
					informationalAlert("Could not complete operation",
							"Please try again");
					return;
				}
			}

			ArrayList<Object> args = new ArrayList<Object>(
					Arrays.asList(	(Object)currentSelectedCell.getAccount().getAccountName(),
									(Object)currentSelectedCell.getAccount().getIndex(),
									(Object)Boolean.toString(true),
									(Object)decryptedAES
								 )
					);
			
			pairingStage = loadPairingFXML(pairingStage, args);
			pairingStage.show();
			
		}
	}
	
	@SuppressWarnings("unused")
	private Stage loadPairingFXML(Stage s, ArrayList<Object> param) {    	
		s = new Stage();
		URL url = Main.class.getResource("/org/wallet/pairing/AuthenticatorApp.fxml");
		int width = 850;
		int height = 484;
		try {
			FXMLLoader loader = new FXMLLoader(url);
			s.setTitle("Add Account");
	    	Scene scene;
			scene = new Scene((AnchorPane) loader.load(), width, height);
			if(param != null){
				BaseUI controller = loader.<BaseUI>getController();
				try{
					AuthenticatorAppController w = loader.<AuthenticatorAppController>getController();
					w.setListener(pairingListener);
				}
				catch (Exception e){ }
				controller.setParams(param);
				controller.updateUIForParams();
			}
			final String file = TextFieldValidator.class.getResource("GUI.css").toString();
	        scene.getStylesheets().add(file); 
	        s.setScene(scene);	
	        return s;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	private PairingWalletControllerListener pairingListener = new PairingWalletControllerListener(){
					@Override
					public void onPairedWallet() { }
			
					@SuppressWarnings("restriction")
					@Override
					public void onFailed(Exception e) {
						Platform.runLater(() -> GuiUtils.informationalAlert("Something is wrong", "Pairing Failed"));
					}
			
					@SuppressWarnings("restriction")
					@Override
					public void closeWindow() {
						Platform.runLater(new Runnable() {
					        @Override
					        public void run() {
					        	if(pairingStage != null)
					        		pairingStage.close();
					        }
						});
						
					}
				};
	
	@FXML protected void close(ActionEvent event){
		overlayUi.done();
	}
	
	private double xOffset = 0;
	private double yOffset = 0;
	 @FXML protected void drag1(MouseEvent event) {
	        xOffset = event.getSceneX();
	        yOffset = event.getSceneY();
	    }

	    @FXML protected void drag2(MouseEvent event) {
	    	Main.stage.setX(event.getScreenX() - xOffset);
	    	Main.stage.setY(event.getScreenY() - yOffset);
	    }
}
