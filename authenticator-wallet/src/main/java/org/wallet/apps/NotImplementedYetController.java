package org.wallet.apps;

import org.wallet.Main;
import org.wallet.utils.BaseUI;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

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
