package wallettemplate;

import java.io.IOException;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;

import wallettemplate.controls.BitcoinAddressValidator;
import authenticator.Authenticator;
import authenticator.network.OneName;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

public class OneNameControllerDisplay {
	@FXML private WebView webView;
	WebEngine engine;
	@FXML private Button done;
	public Main.OverlayUI overlayUi;
	
	// Called by FXMLLoader
    public void initialize() {
    	engine = webView.getEngine();
		engine.load("https://onename.io/" + Authenticator.getWalletOperation().getOnename().getOnename());
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
