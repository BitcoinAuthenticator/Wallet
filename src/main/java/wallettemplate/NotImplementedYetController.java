package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import javafx.scene.image.Image;
import javafx.util.Duration;
import authenticator.Authenticator;
import authenticator.operations.BAOperation;
import authenticator.operations.OperationsFactory;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStage;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStageUpdater;
import authenticator.operations.OperationsUtils.PairingQRCode;
import authenticator.operations.listeners.OperationListener;
import authenticator.operations.listeners.OperationListenerAdapter;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

public class NotImplementedYetController extends BaseUI{
	
	@FXML private Label lblClose;
	
	public Main.OverlayUI overlayUi;
	
	private double xOffset = 0;
	private double yOffset = 0;
    
    @SuppressWarnings("restriction")
	public void initialize() {
        super.initialize(NotImplementedYetController.class);
        
        Tooltip.install(lblClose, new Tooltip("Close Window"));
        lblClose.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	done();
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
        
    public void done() {
    	if(overlayUi != null)
    		overlayUi.done();
    		
    }
}
