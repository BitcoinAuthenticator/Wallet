package wallettemplate;

import java.io.IOException;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;

import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.utils.BaseUI;
import authenticator.Authenticator;
import authenticator.network.OneName;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

public class OneNameControllerDisplay  extends BaseUI{
	@FXML private WebView webView;
	WebEngine engine;
	@FXML private Button done;
	public Main.OverlayUI overlayUi;
	 private double xOffset = 0;
	 private double yOffset = 0;

	// Called by FXMLLoader
    public void initialize() {
    	super.initialize(OneNameControllerDisplay.class);
    	engine = webView.getEngine();
		engine.load("https://onename.io/" + Authenticator.getWalletOperation().getOnename().getOnename());
    }
    
    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stage.setX(event.getScreenX() - xOffset);
    	Main.stage.setY(event.getScreenY() - yOffset);
    }
    
    public void btnBack(ActionEvent event) { goBack(); }
    public String goBack()
    {    
      final WebHistory history = engine.getHistory();
      ObservableList<WebHistory.Entry> entryList=history.getEntries();
      int currentIndex=history.getCurrentIndex();
//      Out("currentIndex = "+currentIndex);
//      Out(entryList.toString().replace("],","]\n"));

      Platform.runLater(new Runnable() { public void run() { history.go(-1); } });
      return entryList.get(currentIndex>0?currentIndex-1:currentIndex).getUrl();
    }

    public void btnForward(ActionEvent event) { goForward(); }
    public String goForward()
    {    
      final WebHistory history=engine.getHistory();
      ObservableList<WebHistory.Entry> entryList=history.getEntries();
      int currentIndex=history.getCurrentIndex();
//      Out("currentIndex = "+currentIndex);
//      Out(entryList.toString().replace("],","]\n"));

      Platform.runLater(new Runnable() { public void run() { history.go(1); } });
      return entryList.get(currentIndex<entryList.size()-1?currentIndex+1:currentIndex).getUrl();
    }

	public void exit(ActionEvent event) {
        overlayUi.done();
    }
}
