package wallettemplate;

import authenticator.Authenticator;
import authenticator.WalletFile;
import authenticator.WalletOperation;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import wallettemplate.controls.ClickableBitcoinAddress;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.PopUpNotification;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;

import static wallettemplate.Main.bitcoin;
import static wallettemplate.utils.GuiUtils.checkGuiThread;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class Controller {
    public ProgressBar syncProgress;
    public VBox syncBox;
    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;
    public ArrayList<ClickableBitcoinAddress> addressArr;
    //scroll pane
    public ScrollPane scrl;
    private ScrollPaneContentManager scrlContent;

    // Called by FXMLLoader.
    public void initialize() {
        syncProgress.setProgress(-1);
        scrlContent = new ScrollPaneContentManager().setSpacingBetweenItems(10);
        scrlContent.setOpacity(0.0);
    }

    public void onBitcoinSetup() {
        bitcoin.wallet().addEventListener(new BalanceUpdater());
        loadAddresses();
        scrl.setContent(scrlContent);
        refreshBalanceLabel();
    }

    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.instance.overlayUI("send_money.fxml");
    }

    public class ProgressBarUpdater extends DownloadListener {
        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
            super.progress(pct, blocksSoFar, date);
            Platform.runLater(() -> syncProgress.setProgress(pct / 100.0));
        }

        @Override
        protected void doneDownload() {
            super.doneDownload();
            Platform.runLater(Controller.this::readyToGoAnimation);
        }
    }

    public void readyToGoAnimation() {
        // Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), syncBox);
        leave.setByY(80.0);
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(600), controlsBox);
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(500), scrlContent);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        // Slide out happens then slide in/fade happens.
        SequentialTransition both = new SequentialTransition(leave, group);
        both.setCycleCount(1);
        both.setInterpolator(Interpolator.EASE_BOTH);
        both.play();
    }

    public ProgressBarUpdater progressBarUpdater() {
        return new ProgressBarUpdater();
    }

    public class BalanceUpdater extends AbstractWalletEventListener {
        @Override
        public void onWalletChanged(Wallet wallet) {
            checkGuiThread();
            refreshBalanceLabel();
        }
    }

    public void refreshBalanceLabel() {
        final BigInteger amount = Authenticator.getWalletOperation().getGeneralWalletEstimatedBalance();//bitcoin.wallet().getBalance(Wallet.BalanceType.ESTIMATED);
        balance.setText(Utils.bitcoinValueToFriendlyString(amount));
    }
    
    @FXML
    private void handlePairDeviceAction(ActionEvent event) { 
    	Main.instance.overlayUI("Pair_wallet.fxml");
    }
    
    @FXML
    private void handleCreateP2ShAddress(ActionEvent event) throws IOException { 
    	Main.instance.overlayUI("New_p2sh_address.fxml");
    }
    
    private void loadAddresses()
    {
    	scrlContent.clearAll();
    	addressArr = new ArrayList<ClickableBitcoinAddress>();
    	//Normal P2PSH from wallet
    	ClickableBitcoinAddress normalAddressControl = new ClickableBitcoinAddress();
    	normalAddressControl.setAddress(bitcoin.wallet().currentReceiveKey().toAddress(Main.params).toString());
    	normalAddressControl.setPairName("Pay-To-Pub-Hash");
    	normalAddressControl.setBalance(Utils.bitcoinValueToFriendlyString(Authenticator.getWalletOperation().getGeneralWalletEstimatedBalance()));
    	scrlContent.addItem(normalAddressControl);
    	addressArr.add(normalAddressControl);
    	
    	//Load P2SH addresses
    	ArrayList<PairingObject> arr = Authenticator.getWalletOperation().getAllPairingObjectArray();
    	for(PairingObject po:arr)
    	{
    		// get address or generate it
    		String add = null;
    		if(po.keys_n > 0){
    			KeyObject ko = po.keys.keys.get(po.keys_n-1);
    			add = ko.address;
    		}
    		else
    			add = generateFreshAuthenticatorP2SHAddress(po.pairingID);
    		
    		//add to scroll content
    		if(add != null)
    		{
    			ClickableBitcoinAddress addressControl = new ClickableBitcoinAddress();
        		//addressControl.setAddress(bitcoin.wallet().currentReceiveKey().toAddress(Main.params).toString());
        		addressControl.setAddress(add);
        		addressControl.setPairName(po.pairingName);
        		BigInteger balance = null;
        		try {
					balance = Authenticator.getWalletOperation().getBalance(po.pairingID);
				} catch (ScriptException | UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
        		addressControl.setBalance(Utils.bitcoinValueToFriendlyString(balance==null? BigInteger.ZERO:balance));
        		//add to content
        		scrlContent.addItem(addressControl);
        		// add to addresses array
        		addressArr.add(addressControl);
    		}
    	}
    }
    
    public String generateFreshAuthenticatorP2SHAddress(String pairID) {
    	String ret = null;
		try {
			ret = Authenticator.getWalletOperation().genAddress(pairID);
		} catch (AddressFormatException | NoSuchAlgorithmException | JSONException e) {
			Authenticator.getWalletOperation().LOG.info(e.toString());
			PopUpNotification p = new PopUpNotification("Something Went Wrong ...");
			p.showPopup();
			e.printStackTrace();
		} 
		return ret;
    }
}
