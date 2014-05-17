package wallettemplate;

import wallettemplate.utils.BaseUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class CreateNewP2SHAddress extends BaseUI{
	public Main.OverlayUI overlayUi;
	public Button cancelBtn;
	
	@FXML
    public void initialize() {
        super.init();
    }

    @FXML
    public void cancel(ActionEvent event) {
    	super.cancel();
        overlayUi.done();
    }
}
