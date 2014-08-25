package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.util.List;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;

import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.base.Joiner;

import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.utils.BaseUI;
import authenticator.Authenticator;
import authenticator.Utils.OneName.OneName;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Label;

public class ShowSeedController  extends BaseUI{
	@FXML private Label lblSeed;
	@FXML private Button done;
	public  Main.OverlayUI overlayUi;


	// Called by FXMLLoader
    @SuppressWarnings("restriction")
	public void initialize() {
    	super.initialize(ShowSeedController.class);
    	try {
			DeterministicSeed seed = Authenticator.getWalletOperation().mWalletWrapper.getWalletSeed();
			List<String> mnemonic = seed.getMnemonicCode();
			String seedStr = Joiner.on(" ").join(mnemonic);
			lblSeed.setText(seedStr);
		} catch (Exception e) {
			informationalAlert("Unfortunately, you messed up.",
 					 "You need to enter the correct password.");
		}
    	
    	// right click copy context menu
		final ContextMenu contextMenu = new ContextMenu();
		MenuItem item1 = new MenuItem("Copy");
		item1.setOnAction(new EventHandler<ActionEvent>() {
			 @SuppressWarnings("restriction")
			 public void handle(ActionEvent e) {
			 @SuppressWarnings("restriction")
				Clipboard clipboard = Clipboard.getSystemClipboard();
				 ClipboardContent content = new ClipboardContent();
				 content.putString(lblSeed.getText().toString());
				 clipboard.setContent(content);
			 }
		 });
		contextMenu.getItems().addAll(item1);
		lblSeed.setContextMenu(contextMenu);
    }
      
    @FXML protected void exit(MouseEvent event) {
        overlayUi.done();
    }
}
