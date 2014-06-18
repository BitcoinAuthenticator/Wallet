package wallettemplate;

import java.io.IOException;

import org.json.JSONException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import wallettemplate.OneName;

public class OneNameController {
	@FXML private TextField txtOneName;
	public Main.OverlayUI overlayUi;
	
	@FXML protected void onename (ActionEvent event) throws IOException, JSONException{
		OneName on = new OneName();
		on.getAvatar(txtOneName.getText());
		overlayUi.done();
	}
}
