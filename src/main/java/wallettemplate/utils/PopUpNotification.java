package wallettemplate.utils;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.stage.Stage;


public class PopUpNotification extends Popup {
	public PopUpNotification(String message)
	{
	    this.setAutoFix(true);
	    this.setAutoHide(true);
	    this.setHideOnEscape(true);
	    Label label = new Label(message);
	    label.setOnMouseReleased(new EventHandler<MouseEvent>() {
	        @Override
	        public void handle(MouseEvent e) {
	        	hidePopup();
	        }
	    });
	    label.getStylesheets().add("/css/styles.css");
	    label.getStyleClass().add("popup");
	    this.getContent().add(label);
	}
	
	public void showPopup() {
	    this.show();
	}
	public void hidePopup()
	{
		this.hide();
	}
}
