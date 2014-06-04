package wallettemplate;

import java.io.IOException;

import org.json.JSONException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class OneNameController {
	@FXML private TextField txtOneName;
	
	@FXML protected void onename (ActionEvent event) throws IOException, JSONException{
		OneName on = new OneName();
		on.getOneName(txtOneName.getText());
		Controller.stage.close();
	}
	
}
