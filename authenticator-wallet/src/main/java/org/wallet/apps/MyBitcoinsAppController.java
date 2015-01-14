package org.wallet.apps;

import javafx.fxml.FXML;
import org.wallet.Main;
import org.wallet.utils.BaseUI;

/**
 * Created by alonmuroch on 1/14/15.
 */
public class MyBitcoinsAppController extends BaseUI {
    public Main.OverlayUI overlayUi;

    public void initialize() {
        super.initialize(MyBitcoinsAppController.class);
    }

    @FXML
    protected void close(){
        overlayUi.done();
    }
}
