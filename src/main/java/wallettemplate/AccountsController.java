package wallettemplate;

import java.io.IOException;
import java.util.List;
import org.json.JSONException;

import wallettemplate.controls.ScrollPaneContentManager;
import authenticator.Authenticator;
import authenticator.network.OneName;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.Node;

public class AccountsController {
	@FXML public ScrollPane scrlPane;
	private ScrollPaneContentManager scrlPaneContentManager;
	
	public void initialize() {
		scrlPaneContentManager = new ScrollPaneContentManager().setSpacingBetweenItems(15);
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
			HBox mainNode = new HBox();
			
			VBox leftBox = new VBox();
			Label l1 = new Label();
    		l1.setStyle("-fx-font-weight: SEMI_BOLD;");
    		l1.setTextFill(Paint.valueOf("#6e86a0"));
    		l1.setFont(Font.font(13));
    		l1.setText(this.account.getAccountName());
    		leftBox.getChildren().add(l1);
    		
    		VBox rightBox = new VBox();
			Label l2 = new Label();
			l2.setStyle("-fx-font-weight: SEMI_BOLD;");
			l2.setTextFill(Paint.valueOf("#6e86a0"));
			l2.setFont(Font.font(13));
			l2.setText(Long.toString(this.account.getConfirmedBalance()));
			rightBox.getChildren().add(l2);
    		
			mainNode.getChildren().add(leftBox);
			mainNode.getChildren().add(rightBox);
			
			return (Node) mainNode;
		}
	}
}
