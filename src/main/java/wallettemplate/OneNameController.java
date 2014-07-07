package wallettemplate;

import java.io.IOException;

import org.json.JSONException;

import authenticator.Authenticator;
import authenticator.network.OneName;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class OneNameController {
	@FXML private TextField txtOneName;
	public Main.OverlayUI overlayUi;
	
	@FXML protected void onename (ActionEvent event) throws IOException, JSONException{
		OneName on = new OneName();
		on.getAvatar(new Authenticator(), Authenticator.getWalletOperation(),txtOneName.getText());
		overlayUi.done();
	}
}
