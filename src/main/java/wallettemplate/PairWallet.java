package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javafx.scene.image.Image;
import javafx.util.Duration;
import authenticator.Authenticator;
import authenticator.operations.BAOperation;
import authenticator.operations.OperationsFactory;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStage;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStageUpdater;
import authenticator.operations.OperationsUtils.PairingQRCode;
import authenticator.operations.listeners.OperationListener;
import authenticator.operations.listeners.OperationListenerAdapter;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

public class PairWallet extends BaseUI{
	
	@FXML private Button cancelBtn;
	@FXML private Button runBtn;
	@FXML private Button btnHelp;
	@FXML private ProgressIndicator prgIndicator;
	@FXML private TextField textfield;
	@FXML private Label lblStatus;
	@FXML private Pane qrPane;
	@FXML private Pane gcmPane;
	@FXML private Hyperlink hlGCM;
	@FXML private Pane pairPane;
	@FXML private Label lblScan;
	@FXML private ImageView imgViewQR;
	public Main.OverlayUI overlayUi;
	@FXML private Button btnBack;
	@FXML private Label lblInfo;
	
	private double xOffset = 0;
	private double yOffset = 0;
    
    PairingWalletControllerListener listener;
    
    Runnable animDisplay;
    Runnable animAfterPairing;
    
    public void initialize() {
        super.initialize(PairWallet.class);
    }
    
    @FXML protected void drag1(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML protected void drag2(MouseEvent event) {
    	Main.stage.setX(event.getScreenX() - xOffset);
    	Main.stage.setY(event.getScreenY() - yOffset);
    }
    
    @SuppressWarnings("restriction")
	@Override
    public void updateUIForParams(){
    	if(hasParameters()){
        	textfield.setText(arrParams.get(0).toString());
        	textfield.setEditable(false);
        	textfield.setDisable(true);
        }
	}
    
    @FXML protected void showGCM() {
    	Animation ani = GuiUtils.fadeOut(pairPane);
		GuiUtils.fadeIn(gcmPane);
    	pairPane.setVisible(false);
    	gcmPane.setVisible(true);
    }
    
    @FXML protected void showPairing() {
    	Animation ani = GuiUtils.fadeOut(gcmPane);
		GuiUtils.fadeIn(pairPane);
    	gcmPane.setVisible(false);
    	pairPane.setVisible(true);
    }
    
    private OperationListenerAdapter opListener = new OperationListenerAdapter(){
    	
    	@SuppressWarnings("restriction")
		@Override
		public void onFinished(BAOperation operation, String str) {
			if( listener != null)
				listener.onPairedWallet();
		}
    	
    	@SuppressWarnings("restriction")
		@Override
		public void onError(BAOperation operation, @Nullable Exception e, @Nullable Throwable t) {
			if( listener != null)
				listener.onFailed(e);
			Platform.runLater(new Runnable() {
				@Override
		        public void run() {
					informationalAlert("Failed to pair the wallet and the Authenticator",
							e.getMessage());
		        }
			});
		}
    };
    
    private void runPairing(String pairName, @Nullable Integer accountID) throws IOException
    {    	    	
    	if(!Main.UI_ONLY_WALLET_PW.hasPassword() && Authenticator.getWalletOperation().isWalletEncrypted()) {
    		informationalAlert("The wallet is locked",
					"Please unlock the wallet to continue");
    		return ;
    	}
    	
    	BAOperation op = OperationsFactory.PAIRING_OPERATION(Authenticator.getWalletOperation(),
    			pairName, 
    			accountID,
    			Authenticator.getApplicationParams().getBitcoinNetworkType(), 
    			60000,
    			animDisplay, 
    			animAfterPairing,
    			new PairingStageUpdater(){
					@Override
					public void onPairingStageChanged(PairingStage stage) {
						handlePairingStage(stage);
					}

					@Override
					public void pairingData(PairedAuthenticator data) { }
    			},
    			Main.UI_ONLY_WALLET_PW).SetOperationUIUpdate(opListener);
    	
    	boolean result = Authenticator.addOperation(op);
    	if(!result){
    		GuiUtils.informationalAlert("Error !", "Could not add operation");
    	}
    	else{}
    }
    
    @SuppressWarnings("restriction")
	private void handlePairingStage(PairingStage stage){
    	switch(stage){
	    	case PAIRING_SETUP:
	    		setProgressIndicator(0, "Setup ..");
	    		break;
	    	case PAIRING_STARTED:
	    		setProgressIndicator(0.1f, "Started");
	    		break;
	    	case WAITING_FOR_SCAN:
	    		setProgressIndicator(0.1f, "Waiting QR Scan ..");
	    		break;
	    	case CONNECTED:
	    		setProgressIndicator(0.4f, "Connected");
	    		break;
	    	case DECRYPTING_MESSAGE:
	    		setProgressIndicator(0.8f, "Decrypting Message");
	    		break;
	    	case FINISHED:
	    		setProgressIndicator(1, "");
	    		break;
	    		
	    		
	    	case CONNECTION_TIMEOUT:
	    	case FAILED:
	    		Platform.runLater(new Runnable() {
	    			@Override
	    	        public void run() {
	    				prgIndicator.setProgress(0);
	    				lblStatus.setText("Connection Timeout");
	    	        }
	    		});
	    		break;
    	}
    }
    
    @SuppressWarnings("restriction")
	private void setProgressIndicator(float state, String status){
    	Platform.runLater(new Runnable() {
			@Override
	        public void run() {
				prgIndicator.setProgress(state);
				lblStatus.setText(status);
	        }
		});
    }
    
    @FXML
    public void run(ActionEvent event) throws IOException {
    	if (animDisplay == null || animAfterPairing == null)
    		prepareAnimations();
    	
    	if(textfield.getText().length() > 0)
    	{
    		// in case any messages are on 
    		if(hasParameters()){
    			String name = arrParams.get(0).toString();
    			Integer accID = Integer.parseInt(arrParams.get(1).toString());
    			this.runPairing(name, accID);
    		}
    		else
    			this.runPairing(textfield.getText(), null);
    	}
    	else
    	{
    		// error 
    	}
    }
    
    @FXML
    public void help(ActionEvent event)
    {
    	
    }
    
    public void setListener(PairingWalletControllerListener l){
    	listener = l;
    }

    @FXML
    public void cancel(ActionEvent event) {
    	if(overlayUi != null)
    		overlayUi.done();
    	else if(listener != null)
    		listener.closeWindow();
    }
    
    @FXML
    public void done(ActionEvent event) {
    	if(overlayUi != null)
    		overlayUi.done();
    	else if(listener != null)
    		listener.closeWindow();
    		
    }
    
    public void prepareAnimations(){
    	animDisplay = getAnimationForDisplayingQR();
    	animAfterPairing = getAnimationForAfterPairing();
    }
        
    public Runnable getAnimationForDisplayingQR(){
    	return new Runnable(){

			@SuppressWarnings("restriction")
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
				      @Override public void run() {
				    	  pairPane.setVisible(false);
				    	  qrPane.setVisible(true);
				    	  TranslateTransition move = new TranslateTransition(Duration.millis(400), imgViewQR);
				    	  move.setByX(437.0);
				    	  move.setCycleCount(1);
				    	  move.play();
							//
							File file;
							file = new File(Authenticator.getApplicationParams().getApplicationDataFolderAbsolutePath() + 
									PairingQRCode.QR_IMAGE_RELATIVE_PATH);
							Image img = new Image(file.toURI().toString());
							imgViewQR.setImage(img);
							lblScan.setVisible(true);
				      }
				});
			}
    		
    	};
    }
    
    public Runnable getAnimationForAfterPairing(){
    	return new Runnable(){

			@SuppressWarnings("restriction")
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
				      @Override public void run() {
				    	  TranslateTransition move = new TranslateTransition(Duration.millis(400), imgViewQR);
				    	  move.setByX(-437.0);
				    	  move.setCycleCount(1);
				    	  move.play();
				    	  imgViewQR.setImage(null);
				    	  pairPane.setVisible(true);
				    	  qrPane.setVisible(false);
				    	  
				    	  runBtn.setDisable(true);
				    	  btnBack.setDisable(true);
				    	  cancelBtn.setText("Done");
				      }
				});
			}
    		
    	};
    }
    
    public interface PairingWalletControllerListener{
    	public void onPairedWallet();
    	public void onFailed(Exception e);
    	public void closeWindow();
    }
    
    
    
    /**
     * Button press + release GUI
     */
    
    @FXML protected void btnPairPressed(MouseEvent event) {
    	runBtn.setStyle("-fx-background-color: #d5d5d5;");
    }
    
    @FXML protected void btnPairReleased(MouseEvent event) {
    	runBtn.setStyle("-fx-background-color: grey; -fx-text-fill: #e3e3e3;");
    }  
    
    @FXML protected void btnClosePressed(MouseEvent event) {
    	cancelBtn.setStyle("-fx-background-color: #d5d5d5;");
    }
    
    @FXML protected void btnCloseReleased(MouseEvent event) {
    	cancelBtn.setStyle("-fx-background-color: grey; -fx-text-fill: #e3e3e3;");
    }  
    
    @FXML protected void btnBackPressed(MouseEvent event) {
    	btnBack.setStyle("-fx-background-color: #d5d5d5;");
    }
    
    @FXML protected void btnBackReleased(MouseEvent event) {
    	btnBack.setStyle("-fx-background-color: grey; -fx-text-fill: #e3e3e3;");
    }  
    
    
    
    /**/
    
}
