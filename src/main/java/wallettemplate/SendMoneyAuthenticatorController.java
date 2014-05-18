package wallettemplate;

import java.math.BigInteger;
import java.util.Map;

import com.google.bitcoin.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import authenticator.ui_helpers.ComboBoxHelper;
import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.controls.ScrollPaneContentManager;
import static wallettemplate.utils.GuiUtils.crashAlert;
import static wallettemplate.utils.GuiUtils.informationalAlert;

public class SendMoneyAuthenticatorController extends SendMoneyController{
  
	public Main.OverlayUI overlayUi;
	
	@FXML private Button btnAdd;
	//Combo
	@FXML private ComboBox cmb;
    private Map<String,String>pairNameToId;
	// Scroll pane
    public ScrollPane scrl;
    private ScrollPaneContentManager scrlContent;
    
	// Called by FXMLLoader
    public void initialize() {
        pairNameToId = ComboBoxHelper.populateComboWithPairingNames(cmb);
        scrlContent = new ScrollPaneContentManager()
        					.setSpacingBetweenItems(15);
        scrl.setContent(scrlContent);
    }

    private void addOutput()
    {
    	final int index = scrlContent.getCount();
    	newAddress na = new newAddress().setCancelOnMouseClick(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				scrlContent.removeNodeAtIndex(index);
				scrl.setContent(scrlContent);
			}
        });
    	scrlContent.addItem(na);
    }
    
    public void cancel(ActionEvent event) {
        overlayUi.done();
    }
    
    public void addOutput(ActionEvent event) {
    	addOutput();
    }
    
    
    public class newAddress extends HBox
    {
    	TextField txfAddress;
    	TextField txfAmount;
    	Label cancelLabel;
    	public newAddress setCancelOnMouseClick(EventHandler<? super MouseEvent> listener){
    		/*cancelLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					// TODO Auto-generated method stub
					
				}
            });;*/
    		cancelLabel.setOnMouseClicked(listener);
    		return this;
    	}
    	public newAddress()
    	{
    		VBox main = new VBox();
    		VBox cancel = new VBox();
    		//Cancel
    		cancelLabel = new Label();
    		cancelLabel.setText("<X>");
    		cancelLabel.setPadding(new Insets(0,5,0,0));
    		
    		AwesomeDude.setIcon(cancelLabel, AwesomeIcon.REMOVE);
            Tooltip.install(cancelLabel, new Tooltip("Delete Output"));
            cancel.setAlignment(Pos.TOP_LEFT);
            cancel.getChildren().add(cancelLabel);
            this.getChildren().add(cancel);
    		
    		//Text Fields
    		txfAddress = new TextField();
    		txfAddress.setPromptText("Recipient Address");
    		txfAddress.setPrefWidth(500);
    		main.getChildren().add(txfAddress);
    		
    		HBox b = new HBox();
    		txfAmount = new TextField();
    		txfAmount.setPromptText("Amount");
    		txfAmount.setPrefWidth(300);
    		b.setMargin(txfAmount, new Insets(0,200,0,0));
    		b.getChildren().add(txfAmount);
    		main.getChildren().add(b);
    		this.getChildren().add(main);
    	}
    }
}
