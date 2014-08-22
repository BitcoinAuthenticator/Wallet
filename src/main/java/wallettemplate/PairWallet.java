package wallettemplate;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import javafx.scene.image.Image;
import javafx.util.Duration;
import authenticator.Authenticator;
import authenticator.operations.BAOperation;
import authenticator.operations.OperationsFactory;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStage;
import authenticator.operations.OperationsUtils.PairingProtocol.PairingStageUpdater;
import authenticator.operations.listeners.OperationListener;
import authenticator.operations.listeners.OperationListenerAdapter;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import wallettemplate.utils.BaseUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

public class PairWallet extends BaseUI{
	
	@FXML private Button cancelBtn;
	@FXML private Button doneBtn;
	@FXML private Button runBtn;
	@FXML private ProgressIndicator prgIndicator;
	@FXML private TextField textfield;
	@FXML private Label lblStatus;
	
	@FXML private HBox qrBox;
	@FXML private ImageView imgViewQR;
	public Main.OverlayUI overlayUi;
    
    PairingWalletControllerListener listener;
    
    Runnable animDisplay;
    Runnable animAfterPairing;
    
    public void initialize() {
        super.initialize(PairWallet.class);
        doneBtn.setDisable(true);
        qrBox.setVisible(false);
    }
    
    @SuppressWarnings("restriction")
	@Override
    public void updateUIBecauseForParams(){
    	if(hasParameters()){
        	textfield.setText(arrParams.get(0).toString());
        	textfield.setEditable(false);
        	textfield.setDisable(true);
        }
	}
    
    private OperationListenerAdapter opListener = new OperationListenerAdapter(){
    	@SuppressWarnings("restriction")
		@Override
		public void onBegin(String str) {
//			Platform.runLater(new Runnable() {
//		        @Override
//		        public void run() {
//		        	textarea.appendText(str + "\n------------------------------------------------\n");
//		        }
//			});
		}
    	
    	@SuppressWarnings("restriction")
		@Override
		public void onFinished(String str) {
			if( listener != null)
				listener.onPairedWallet();
			Platform.runLater(new Runnable() {
		        @Override
		        public void run() {
//		        	textarea.appendText("=============================\n" +
//		        						str);
//		        	
		        	cancelBtn.setDisable(true);
					doneBtn.setDisable(false);
					qrBox.setVisible(false);
					Authenticator.fireOnNewPairedAuthenticator();
		        }
			});
		}
    	
    	@SuppressWarnings("restriction")
		@Override
		public void onError(@Nullable Exception e, @Nullable Throwable t) {
			if( listener != null)
				listener.onFailed(e);
			Platform.runLater(new Runnable() {
				@Override
		        public void run() {
					qrBox.setVisible(false);
//		        	textarea.appendText("--------------------------\n" + 
//		        						"Error: + " + e.toString() + "\n" + 
//		        						e.getMessage());
					Dialogs.create()
		        	        .owner(Main.stage)
		        	        .title("Error !")
		        	        .masthead("Failed to pair the wallet and the Authenticator")
		        	        .message(e.getMessage())
		        	        .showError();
		        }
			});
		}
    };
    
    private void runPairing(String pairName, @Nullable Integer accountID) throws IOException
    {    	    	
    	BAOperation op = OperationsFactory.PAIRING_OPERATION(Authenticator.getWalletOperation(),
    			pairName, 
    			accountID,
    			Authenticator.getApplicationParams().getBitcoinNetworkType(), 
    			animDisplay, 
    			animAfterPairing,
    			new PairingStageUpdater(){
					@Override
					public void onPairingStageChanged(PairingStage stage) {
						handlePairingStage(stage);
					}

					@Override
					public void pairingData(PairedAuthenticator data) { }
    			}).SetOperationUIUpdate(opListener);
    	
    	boolean result = Authenticator.addOperation(op);
    	if(!result){
    		Dialogs.create()
		        .owner(Main.stage)
		        .title("Error !")
		        .masthead("Could not add operation")
		        .showInformation();   
    	}
    	else
    		qrBox.setVisible(true);
    }
    
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
	    		break;
	    	case FAILED:
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
				    	
				    	  TranslateTransition move = new TranslateTransition(Duration.millis(400), imgViewQR);
				    	  move.setByX(-130.0);
				    	  move.setCycleCount(1);
				    	  move.play();
							//
							File file;
							try {
								file = new File(new java.io.File( "." ).getCanonicalPath() + "/cached_resources/PairingQRCode.png");
								Image img = new Image(file.toURI().toString());
								imgViewQR.setImage(img);
							} catch (IOException e) { e.printStackTrace();
							}
							
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
				    	  move.setByX(130.0);
				    	  move.setCycleCount(1);
				    	  move.play();
				    	  imgViewQR.setImage(null);
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
}
