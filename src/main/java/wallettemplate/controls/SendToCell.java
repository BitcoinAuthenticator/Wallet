package wallettemplate.controls;

import java.awt.Button;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import org.json.JSONException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import wallettemplate.Main;
import wallettemplate.OneNameControllerDisplay;
import wallettemplate.Main.OverlayUI;
import wallettemplate.utils.GuiUtils;
import authenticator.Authenticator;
import authenticator.Utils.OneName.OneName;
import authenticator.Utils.OneName.OneNameAdapter;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;

public class SendToCell extends Region{	
	@FXML private Label lblDelete;
	@FXML private TextField txfAmount;
	@FXML private TextField txfAddress;
	@FXML private ChoiceBox cbCurrency;
	@FXML private Label lblScanQR;
	@FXML private Label lblContacts;
	@FXML private VBox avatarBox;
	@FXML private ImageView ivAvatar;
	@FXML private Label lblAvatarName;
	@FXML private VBox loadingAvatarBox;
	@FXML private ProgressIndicator spinner;
		
	private int index;
	
	private boolean isAddressFromOneName = false;
	private ConfigOneNameProfile oneNameData;
	
	@SuppressWarnings("restriction")
	public SendToCell(int index) {
		this.index = index;
        this.loadFXML();
        this.setSnapToPixel(true);
      }
	
	@SuppressWarnings({ "restriction", "unchecked" })
	public void initGUI(){
		// delete icon 
		AwesomeDude.setIcon(lblDelete, AwesomeIcon.TIMES_CIRCLE);
        Tooltip.install(lblDelete, new Tooltip("Remove"));
        
        // scan QR label
        Tooltip.install(lblScanQR, new Tooltip("Scan QR code"));
		AwesomeDude.setIcon(lblScanQR, AwesomeIcon.QRCODE);
		
		// address book label
		Tooltip.install(lblContacts, new Tooltip("Select from address book"));
		AwesomeDude.setIcon(lblContacts, AwesomeIcon.USERS);
        
		// amount txf
		txfAmount.lengthProperty().addListener(new ChangeListener<Number>(){
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
                  if(newValue.intValue() > oldValue.intValue()){
                      char ch = txfAmount.getText().charAt(oldValue.intValue());  
                      //Check if the new character is the number or other's
                      if(!(ch >= '0' && ch <= '9') && ch != '.'){       
                           //if it's not number then just setText to previous one
                           txfAmount.setText(txfAmount.getText().substring(0,txfAmount.getText().length()-1)); 
                      }
                 }
            }
		});
		
		// currency choice box
		cbCurrency.getItems().add("BTC");
		cbCurrency.getItems().add("USD");
		cbCurrency.setValue("BTC");
		
        // One name control
        txfAddress.focusedProperty().addListener(new ChangeListener<Boolean>()
        		{
        			private boolean isOneName(String str){
    	        		if (!str.equals("") && !str.substring(0, 1).equals("1") && !str.substring(0, 1).equals("3"))
    	        			return true;
    	        		return false;
        			}
        			
        			private void setAvatarImage(ConfigOneNameProfile onename, String onenameID) throws IOException, JSONException{
        				
        				OneName.downloadAvatarImage(onename,
        						Authenticator.getWalletOperation(),
        						new OneNameAdapter(){
		        					@Override
		        					public void getOneNameAvatarImage(ConfigOneNameProfile one, Image img) {
		        						
		        						Platform.runLater(new Runnable() { 
			        						 @Override
			        						public void run() {
			        							 if(img != null){
			        								spinner.setProgress(70.0);
			        								ivAvatar.setImage(img);
			        								spinner.setProgress(100.0);
		        	        						ivAvatar.setOnMouseClicked(new EventHandler<MouseEvent>() {
		        	                					   @Override
		        	                					   public void handle(MouseEvent event) {
		        	                						   Main.instance.overlayUI("DisplayOneName.fxml");
		        	                						   OneNameControllerDisplay.loadOneName(onenameID);
		        	                					   }
		        	                				   });
		        	        						lblAvatarName.setText(one.getOnenameFormatted());
		        	        						ivAvatar.setStyle("-fx-cursor: hand;");
		        	        						avatarBox.setVisible(true);
		        	        						Rectangle clip = new Rectangle(ivAvatar.getFitWidth()-5, ivAvatar.getFitHeight());
		        	        						clip.setArcWidth(9);
		        	        						clip.setArcHeight(9);
		        	        						ivAvatar.setClip(clip);
		        	        						SnapshotParameters parameters = new SnapshotParameters();
		        	        						parameters.setFill(Color.TRANSPARENT);
		        	        						WritableImage image = ivAvatar.snapshot(parameters, null);
		        	        						ivAvatar.setClip(null);
		        	        						ivAvatar.setEffect(new DropShadow(03, Color.BLACK));
		        	        						ivAvatar.setImage(image);
			        							 }
			        							 loadingAvatarBox.setVisible(false);
			        						 }
		        						});
		        						
		        					}
        				});        				
        			}
        			
        		    @Override
        		    public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
        		    {
        		    	String txfield = txfAddress.getText();
        		        if (!newPropertyValue){
        		        	if (Authenticator.getWalletOperation().getNetworkParams() == MainNetParams.get()){
        		        		if (isOneName(txfAddress.getText())){
        		        			try { 
        		        				ivAvatar.setImage(null);
        		        				avatarBox.setVisible(false);
        		        				
        		        				spinner.setProgress(0);
        		        				
        		        				// start spinner
        		        				loadingAvatarBox.setVisible(true);
        		        				
        		        				OneName.getOneNameData(txfAddress.getText(),
        		        						Authenticator.getWalletOperation(), 
        		        						new OneNameAdapter(){
		        		        					@Override
		        		        					public void getOneNameData(ConfigOneNameProfile data) {
		                		        				if (data != null){
		                		        					oneNameData = data;
		                		        					Platform.runLater(new Runnable() { 
		                		        						 @Override
		                		        						public void run() {
		                		        							txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #98d947;");
		                             		        				txfAddress.setText(data.getOnenameFormatted());
		                             		        				isAddressFromOneName = true;
		                             		        				try {
		                             		        					spinner.setProgress(50.0);
																		setAvatarImage(data, txfield);
																	} catch (Exception e) {
																		e.printStackTrace();
																	}
		                             		        				
		                             		        				
		                		        						}
		                		        				    }); 
		                    		        			}
		                		        				else
		                		        					Platform.runLater(() -> {
		                		        						GuiUtils.informationalAlert("Could not download OneName profile", "");
		                		        					});
		                		        					
		        		        					}
        		        				});
        		        				
        		        			} catch (NullPointerException e){
        		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #f06e6e;");

        		        			}
        		        			
        		        		}
        		        		else {
        		        			isAddressFromOneName = false;
        		        			try {
        		        				new Address(Authenticator.getWalletOperation().getNetworkParams(), txfAddress.getText().toString());
        		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #98d947;");}
        		        			catch (AddressFormatException e) {
        		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #f06e6e;");
        		        			}
        		        		}
        		        	}
        		        	else {
        		        		isAddressFromOneName = false;
        		        		try {
        		        			new Address(Authenticator.getWalletOperation().getNetworkParams(), txfAddress.getText().toString());
        		        			txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #98d947;");}
        		        		catch (AddressFormatException e) {
        		        			txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #f06e6e;");
        		        		}
        		        	}
        		        }
        		    }
        		});
	}
	
	public boolean validate()
    {
    	if(getAddress().length() == 0)
    		return false;
    	if(txfAmount.getText().length() == 0)
    		return false;
    	if(txfAmount.getText().matches("[a-zA-Z]+"))
    		return false;
    	// check dust amount 
    	double fee = (double) Double.parseDouble(txfAmount.getText())*100000000;
    	Coin am = Coin.valueOf((long)fee);
    	if(am.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0)
    		return false;
    	return true;
    }
	
	@SuppressWarnings("restriction")
	public SendToCell setCancelOnMouseClick(Object object, Method method){
		lblDelete.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				Object[] parameters = new Object[1];
		        parameters[0] = index;
		        try {
					method.invoke(object, parameters);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
        });
		return this;
	}
	
	@SuppressWarnings("restriction")
	public String getAddress(){
		if(this.isAddressFromOneName)
			return this.oneNameData.getBitcoinAddress();
		else
			return txfAddress.getText();
	}
	
	@SuppressWarnings("restriction")
	public double getAmountValue(){
		return (double) Double.parseDouble(txfAmount.getText())*100000000;
	}
	public Coin getAmount(){
		return Coin.valueOf((long)getAmountValue());
	}
	
	public String getSelectedCurrency(){
		return cbCurrency.getValue().toString();
	}
	
	public void setIndex(int value){ this.index = value; }
	public int getIndex(){
		return this.index;
	}
	
	@SuppressWarnings("restriction")
	private void loadFXML() {
        FXMLLoader loader = new FXMLLoader();
        loader.setController(this);
        loader.setLocation(this.getViewURL());
        try {
            Node root = (Node) loader.load();
            this.getChildren().add(root);
        }
        catch (IOException ex) {
           ex.printStackTrace();
        }    
    }
	
	private String getViewPath() {
        return "SendToCell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
}
