package wallettemplate;

import java.math.BigInteger;
import java.util.Map;

import com.google.bitcoin.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
    	newAddress na = new newAddress();
    	scrlContent.addItem(na);
    }
    
    public void cancel(ActionEvent event) {
        overlayUi.done();
    }
    
    public void addOutput(ActionEvent event) {
    	addOutput();
    }
    
    
    public class newAddress extends VBox
    {
    	public newAddress()
    	{
    		TextField txf = new TextField();
    		txf.setPromptText("Recipient Address ... ");
    		txf.setPrefWidth(500);
    		this.getChildren().add(txf);
    	}
    }
}
