package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.json.JSONException;

import wallettemplate.controls.DisplayAccountCell;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.BaseUI;
import authenticator.Authenticator;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.Utils.OneName.OneName;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.protobuf.ProtoConfig.ATAccount;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import authenticator.walletCore.exceptions.NoWalletPasswordException;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.Node;
import javafx.util.Duration;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import org.bitcoinj.core.Transaction;

public class AccountsController  extends BaseUI{
	@FXML public ScrollPane scrlPane;
	
	private ScrollPaneContentManager scrlPaneContentManager;
	
	public void initialize() {
		super.initialize(AccountsController.class);
		scrlPaneContentManager = new ScrollPaneContentManager()
									.setSpacingBetweenItems(0)
									.setScrollStyle(scrlPane.getStyle());
		scrlPane.setContent(scrlPaneContentManager);
		setupContent();
	}
	
	private void setupContent(){
		scrlPaneContentManager.clearAll();
		List<ATAccount> all = Authenticator.getWalletOperation().getAllAccounts();
		for(ATAccount acc:all){
			AccountUI ui = new AccountUI(acc);
			scrlPaneContentManager.addItem(ui.getNode());
		}
	}
		
	
	private class AccountUI{
		private ATAccount account;
		public AccountUI(ATAccount account){
			this.account = account;
		}
		
		@SuppressWarnings("restriction")
		public Node getNode(){
			
			DisplayAccountCell c = new DisplayAccountCell(this.account)
			.setListener(new DisplayAccountCell.AccountCellEvents() {
				@Override
				public void onSettingsClick(DisplayAccountCell cell) { }

				@Override
				public void onDeleteAccountRequest(DisplayAccountCell cell) {
					/**
					 * check at least one account remains
					 */
					if(Authenticator.getWalletOperation().getAllAccounts().size() < 2){
						Dialogs.create()
				        .owner(Controller.accountsAppStage)
				        .title("Error")
				        .masthead("Cannot Remove account !")
				        .message("At least one active account should remain.")
				        .showError();
						
						cell.setSettingsClose();
						return;
					}
					
					/**
					 * Check is not the active account
					 */
					if(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex() == cell.getAccount().getIndex()){
						Dialogs.create()
				        .owner(Controller.accountsAppStage)
				        .title("Error")
				        .masthead("Cannot Remove account !")
				        .message("The account for removal is the active wallet's account.\n"
				        		+ "Change the wallet's active account and try again.")
				        .showError();
						
						cell.setSettingsClose();
						return;
					}
					
					
					Optional<String> response = Dialogs.create()
	            	        .owner(Controller.accountsAppStage)
	            	        .title("Warning !")
	            	        .masthead("You are about to delete the \"" + cell.getAccount().getAccountName() + "\" account !\n"
	            	        		+ "Deleting the account will delete all information about it.\n"
	            	        		+ "Do you wish do continue ?\n")
	            	        .actions(Dialog.Actions.YES, Dialog.Actions.NO)
	            	        .showTextInput("Enter account name to confirm");
					
					if (response.isPresent() && response.get().equals(cell.getAccount().getAccountName())) {
						try {
							Authenticator.getWalletOperation().removeAccount(cell.getAccount().getIndex());
							setupContent();
							Dialogs.create()
					        .owner(Controller.accountsAppStage)
					        .title("Operation Complete")
					        .masthead("Account was deleted")
					        .message("")
					        .showInformation();
						} catch (IOException e) { 
							e.printStackTrace();
							Dialogs.create()
					        .owner(Controller.accountsAppStage)
					        .title("Error !")
					        .masthead("Error occured while deleting the account.")
					        .message("")
					        .showInformation();
						}
					}
					else
						System.out.println("not deleting");
				}

				@Override
				public void onChangeNameRequest(DisplayAccountCell cell) {
					Optional<String> response = null;
					response = Dialogs.create()
	            	        .owner(Controller.accountsAppStage)
	            	        .title("Change Account Name")
	            	        .masthead("Please enter a new name:")
	            	        .actions(Dialog.Actions.YES, Dialog.Actions.NO)
	            	        .showTextInput("Enter new account name");
					if (response.isPresent()) {
						try {
							ATAccount t = Authenticator.getWalletOperation().setAccountName(response.get(), cell.getAccount().getIndex());
							cell.setAccount(t);
							cell.updateUI();
							cell.setSettingsClose();
						} catch (CannotWriteToConfigurationFileException e) { 
							e.printStackTrace(); 
							Dialogs.create()
					        .owner(Controller.accountsAppStage)
					        .title("Error !")
					        .masthead("Error occured while changing account's name.")
					        .message("")
					        .showInformation();
						}
						
					}
					
				}
			});
			
			return c;
			
		}
	}
	
	@FXML protected void addAccount(ActionEvent event) throws NoWalletPasswordException{
		if(!Main.UI_ONLY_WALLET_PW.hasPassword() && Authenticator.getWalletOperation().isWalletEncrypted()) {
			informationalAlert("Your wallet is locked",
					"Please unlock the wallet to add an account");
			return;
		}
		try {
			ATAccount newAcc = Authenticator.getWalletOperation().generateNewStandardAccount(NetworkType.MAIN_NET, "XXX", Main.UI_ONLY_WALLET_PW);
			setupContent();
		
		} catch (IOException e) {
			e.printStackTrace();
			Dialogs.create()
	        .owner(Controller.accountsAppStage)
	        .title("Error !")
	        .masthead("Error occured while creating the account.")
	        .message("")
	        .showInformation();
		}
	}
	
	@FXML protected void close(ActionEvent event){
		Controller.accountsAppStage.close();
	}
	
	
	private double xOffset = 0;
	private double yOffset = 0;
	@FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Controller.accountsAppStage.setX(event.getScreenX() - xOffset);
    	Controller.accountsAppStage.setY(event.getScreenY() - yOffset);
    }
}
