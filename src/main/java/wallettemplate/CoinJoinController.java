package wallettemplate;

import javafx.fxml.FXML;
import wallettemplate.utils.BaseUI;

public class CoinJoinController extends BaseUI{
	public Main.OverlayUI overlayUi;

		public void initialize() {
	        super.initialize(CoinJoinController.class);
		}
		
		@FXML protected void close(){
			overlayUi.done();
		}
}