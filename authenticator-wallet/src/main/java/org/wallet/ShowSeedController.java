package org.wallet;

import static org.wallet.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.wallet.controls.BitcoinAddressValidator;
import org.wallet.utils.BaseUI;
import org.bitcoinj.wallet.DeterministicSeed;

import com.google.common.base.Joiner;

import org.authenticator.Authenticator;
import org.authenticator.Utils.OneName.OneName;
import org.authenticator.walletCore.exceptions.NoWalletPasswordException;
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
			DeterministicSeed seed = Authenticator.getWalletOperation().getWalletSeed(Main.UI_ONLY_WALLET_PW);
			List<String> mnemonic = seed.getMnemonicCode();
			String seedStr = Joiner.on(" ").join(mnemonic);
			lblSeed.setText(seedStr);
		} catch (NoWalletPasswordException e) {
			informationalAlert("Unfortunately, you messed up.",
 					 "You need to enter the correct password.");
			return;
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
