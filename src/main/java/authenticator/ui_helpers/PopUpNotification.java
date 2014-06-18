package authenticator.ui_helpers;

import com.sun.prism.paint.Color;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;


public class PopUpNotification extends Stage {
	@SuppressWarnings("deprecation")
	public PopUpNotification(String head,String message)
	{
    	initModality(Modality.WINDOW_MODAL);
		setHeight(300);
		setWidth(600);
		VBox mainV = new VBox();
		mainV.setSpacing(10);
		mainV.setAlignment(Pos.CENTER);
		//
		Label headLbl = new Label(head);
		headLbl.setFont(new Font(20));
		mainV.getChildren().add(headLbl);
		//
		Label msgLbl = new Label(message);
		msgLbl.setFont(new Font(15));
		mainV.getChildren().add(msgLbl);
		//
		Button btnOK = new Button("Ok.");
		btnOK.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				hidePopup();
			}
			
		});
		mainV.getChildren().add(btnOK);
		
		setScene(new Scene(VBoxBuilder.create().
		    children(mainV).
		    alignment(Pos.CENTER).padding(new Insets(5)).build()));
		
	}
	
	public void showPopup(){
		this.show();
	}
	
	public void hidePopup()
	{
		this.hide();
	}
}
