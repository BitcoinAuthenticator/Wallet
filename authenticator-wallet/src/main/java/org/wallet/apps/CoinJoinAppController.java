package org.wallet.apps;

import org.wallet.Main;
import org.wallet.utils.BaseUI;

import javafx.fxml.FXML;

public class CoinJoinAppController extends BaseUI{
	public Main.OverlayUI overlayUi;

		public void initialize() {
	        super.initialize(CoinJoinAppController.class);
		}
		
		@FXML protected void close(){
			overlayUi.done();
		}
}