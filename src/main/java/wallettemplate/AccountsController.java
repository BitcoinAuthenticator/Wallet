package wallettemplate;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.json.JSONException;

import wallettemplate.controls.DisplayNameCell;
import wallettemplate.controls.ScrollPaneContentManager;
import authenticator.Authenticator;
import authenticator.network.OneName;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.Node;
import javafx.util.Duration;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

public class AccountsController {
	@FXML public ScrollPane scrlPane;
	private ScrollPaneContentManager scrlPaneContentManager;
	
	public void initialize() {
		scrlPaneContentManager = new ScrollPaneContentManager()
									.setSpacingBetweenItems(0)
									.setScrollStyle(scrlPane.getStyle());
		scrlPane.setContent(scrlPaneContentManager);
		setupContent();
	}
	
	private void setupContent(){
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
			
			DisplayNameCell cell = new DisplayNameCell(this.account)
			.setListener(new DisplayNameCell.AccountCellEvents() {
				@Override
				public void onSettingsClick(DisplayNameCell cell) {
					
				}

				@Override
				public void onDeleteAccountRequest(DisplayNameCell cell) {
					Optional<String> response = Dialogs.create()
	            	        .owner(Controller.accountsAppStage)
	            	        .title("Warning !")
	            	        .masthead("You are about to delete an account !\nDeleting the account will delete all information about it.\nDo you wish do continue ?\n")
	            	        .actions(Dialog.Actions.YES, Dialog.Actions.NO)
	            	        .showTextInput("Enter account name to confirm");
					
					if (response.isPresent() && response.get().equals(cell.getAccount().getAccountName())) {
						System.out.println("deleting");
					}
					else
						System.out.println("not deleting");
				}
			});
			
			return cell;
			
		}
	}
}
