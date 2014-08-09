package wallettemplate;

import wallettemplate.utils.BaseUI;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;


public class SettingsController  extends BaseUI{
	@FXML private AnchorPane SettingsApp;
	@FXML private Button btnDone;
	@FXML private Button btnRestore;
	@FXML private Button btnChange;
	@FXML private Button btnShow;
	@FXML private Pane settingspane;
	public Main.OverlayUI overlayUi;
	private double xOffset = 0;
	private double yOffset = 0;

	// Called by FXMLLoader
    public void initialize() {
    	super.initialize(SettingsController.class);
    	btnDone.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnDone.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnDone.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnDone.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
        btnRestore.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnRestore.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnRestore.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnRestore.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
        btnShow.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnShow.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnShow.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnShow.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
        btnChange.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnChange.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnChange.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnChange.setStyle("-fx-background-color: #b3b1b1;");
            }
        });
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
