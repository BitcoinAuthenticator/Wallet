package org.wallet;

import org.wallet.utils.BaseUI;

import javafx.fxml.FXML;

public class CoinJoinController extends BaseUI{
	public Main.OverlayUI overlayUi;

		public void initialize() {
	        super.initialize(CoinJoinController.class);
		}
		
		@FXML protected void close(){
			overlayUi.done();
		}
}