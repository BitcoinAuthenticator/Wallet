package wallettemplate;

import wallettemplate.utils.BaseUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;


public class SettingsController  extends BaseUI{
	@FXML private Button done;
	@FXML private Pane settingspane;
	public Main.OverlayUI overlayUi;
	private double xOffset = 0;
	private double yOffset = 0;

	// Called by FXMLLoader
    public void initialize() {
    	super.initialize(SettingsController.class);
    }
    
    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stage.setX(event.getScreenX() - xOffset);
    	Main.stage.setY(event.getScreenY() - yOffset);
    }

	public void exit(ActionEvent event) {
        overlayUi.done();
    }
}
