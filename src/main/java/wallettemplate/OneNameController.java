package wallettemplate;

import java.io.IOException;

import org.json.JSONException;

import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import authenticator.Authenticator;
import authenticator.network.OneName;
import javafx.animation.Animation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class OneNameController  extends BaseUI{
	@FXML private TextField txtOneName;
	@FXML private AnchorPane AnchorPane;
	@FXML private Pane Pane1;
	@FXML private Pane Pane2;
	public Main.OverlayUI overlayUi;
	
	public void initialize(){
		 //super(AccountsController.class);
		AnchorPane.setEffect(new DropShadow());
	}
	
	@FXML protected void onename (ActionEvent event) throws IOException, JSONException{
		OneName on = new OneName();
		on.getAvatar(new Authenticator(), Authenticator.getWalletOperation(),txtOneName.getText());
		overlayUi.done();
	}
	
	@FXML protected void done (){
		overlayUi.done();
	}
	
	@FXML protected void learnMore(){
		Animation ani = GuiUtils.fadeOut(Pane1);
		GuiUtils.fadeIn(Pane2);
		Pane1.setVisible(false);
		Pane2.setVisible(true);
	}
	
	@FXML protected void goBack(){
		Animation ani = GuiUtils.fadeOut(Pane2);
		GuiUtils.fadeIn(Pane1);
		Pane2.setVisible(false);
		Pane1.setVisible(true);
	}
}
