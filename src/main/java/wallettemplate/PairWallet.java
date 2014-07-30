package wallettemplate;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.controlsfx.dialog.Dialogs;

import javafx.scene.image.Image;
import javafx.util.Duration;
import authenticator.Authenticator;
import authenticator.operations.BAOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.operations.OperationsUtils.PairingProtocol;
import wallettemplate.utils.BaseUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;

public class PairWallet extends BaseUI{
	
	public Button cancelBtn;
	public Button doneBtn;
	public Button runBtn;
	public TextArea textarea;
	public TextField textfield;
	public ImageView imgViewQR;
    public Main.OverlayUI overlayUi;
    
    PairingWalletControllerListener listener;
    
    Runnable animDisplay;
    Runnable animAfterPairing;
    
    public void initialize() {
        super.initialize(PairWallet.class);
        doneBtn.setDisable(true);
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
    
    private void runPairing(String pairName, @Nullable Integer accountID) throws IOException
    {    	
    	textarea.appendText("Pairing setup ..\n");
    	
    	BAOperation op = OperationsFactory.PAIRING_OPERATION(Authenticator.getWalletOperation(),
    			pairName, 
    			accountID,
    			Authenticator.getApplicationParams().getBitcoinNetworkType(), 
    			animDisplay, 
    			animAfterPairing).SetOperationUIUpdate(new OnOperationUIUpdate(){

			@SuppressWarnings("restriction")
			@Override
			public void onBegin(String str) {
				Platform.runLater(new Runnable() {
			        @Override
			        public void run() {
			        	textarea.appendText(str + "\n------------------------------------------------\n");
			        }
				});
			}

			@SuppressWarnings("restriction")
			@Override
			public void statusReport(String report) {
				Platform.runLater(new Runnable() {
			        @Override
			        public void run() {
			        	textarea.appendText(report + "\n");
			        }
				});
				
			}

			@SuppressWarnings("restriction")
			@Override
			public void onFinished(String str) {
				if( listener != null)
					listener.onPairedWallet();
				Platform.runLater(new Runnable() {
			        @Override
			        public void run() {
			        	textarea.appendText("=============================\n" +
			        						str);
			        	
			        	cancelBtn.setDisable(true);
						doneBtn.setDisable(false);
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
			        	textarea.appendText("--------------------------\n" + 
			        						"Error: + " + e.toString() + "\n" + 
			        						e.getMessage());
			        }
				});
			}
        	
        });
    	Authenticator.addOperation(op);
    }
    
    @FXML
    public void run(ActionEvent event) throws IOException {
    	if (animDisplay == null || animAfterPairing == null)
    		prepareAnimations();
    	
    	if(textfield.getText().length() > 0)
    	{
    		// in case any messages are on 
    		textarea.clear();
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
    		textarea.appendText("===========================================\n"+
    							"   Please Enter a Pairing Name In The Box\n" + 
    							"===========================================\n");
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
				    	
				    	  TranslateTransition move = new TranslateTransition(Duration.millis(400), textarea);
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
				    	
				    	  TranslateTransition move = new TranslateTransition(Duration.millis(400), textarea);
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
