package org.wallet.controls;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import org.authenticator.Utils.ExchangeProvider.Currency;
import org.authenticator.Utils.ExchangeProvider.Exchange;
import org.authenticator.Utils.ExchangeProvider.Exchanges;
import org.json.JSONException;
import org.wallet.Main;
import org.wallet.utils.GuiUtils;
import org.wallet.utils.TextUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import org.authenticator.Authenticator;
import org.authenticator.Utils.OneName.OneName;
import org.authenticator.Utils.OneName.OneNameAdapter;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.protobuf.ProtoSettings.BitcoinUnit;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
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
	
	public void initGUI(String[] lstCurrencies, String setValue){
		initGUI(Arrays.asList(lstCurrencies), setValue);
	}
	
	
	@SuppressWarnings("restriction")
	public void initGUI(List<String> lstCurrencies, String setValue){
		Platform.runLater(() -> {
			initCellGUI(lstCurrencies, setValue);
		});
	}
	
	@SuppressWarnings({ "restriction" })
	public void initCellGUI(List<String> lstCurrencies, String setValue) {
		// delete icon 
		AwesomeDude.setIcon(lblDelete, AwesomeIcon.TIMES_CIRCLE);
        Tooltip.install(lblDelete, new Tooltip("Remove"));
        
        // scan QR label
        Tooltip.install(lblScanQR, new Tooltip("Scan QR code"));
		AwesomeDude.setIcon(lblScanQR, AwesomeIcon.QRCODE);
		lblScanQR.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	runQRCamera();
            }
        });
		
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
		
		this.updateCurrencyChoiceBox(lstCurrencies, setValue);
		
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
		        	                						   ArrayList<Object> l = new ArrayList<Object>();
		        	                						   l.add(onenameID);
		        	                						   Main.instance.overlayUI("OneNameApp.fxml", l);
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
		                             		        				txfAddress.setText(data.getBitcoinAddress());
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
	
	Webcam webcam;
	JFrame camFrame;
	private void runQRCamera() {
		
		new Thread(){
			 @Override
            public void run() {
				 camFrame  = new JFrame();
				 
				 Dimension size = WebcamResolution.QVGA.getSize();
				 webcam = Webcam.getDefault();
				 webcam.setViewSize(size);
				 WebcamPanel panel = new WebcamPanel(webcam);
				 
				 Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                	camFrame.add(panel);
	                	camFrame.pack();
	                	
	                	camFrame.setVisible(true);
	                	camFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	                	    @Override
	                	    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
	                	    	webcam.close();
	                	    }
	                	});
	                }
	            });
				 
				tryAndReadQR(webcam);
			 }
		 }.start();
	}
	
	private void tryAndReadQR(Webcam webcam){
		 String qrDataString;
		 do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Result result = null;
				BufferedImage image = null;

				if (webcam.isOpen()) {

					if ((image = webcam.getImage()) == null) {
						continue;
					}

					LuminanceSource source = new BufferedImageLuminanceSource(image);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

					try {
						result = new MultiFormatReader().decode(bitmap);
					} catch (NotFoundException e) {
						// fall thru, it means there is no QR code in image
					}
				}

				if (result != null) {
					qrDataString = result.getText();
					 
					webcam.close();
	                camFrame.setVisible(false);
					break;
				}

			} while (true);
		 
		 System.out.println("Data read from qr: " + qrDataString);
		 
		 Platform.runLater(() -> {
			 int addressStartIndex = qrDataString.indexOf("bitcoin:")+8;
			 int addressEndIndex = qrDataString.indexOf("?amount=") != -1? qrDataString.indexOf("?amount="): qrDataString.length();			 
			 String address = qrDataString.substring(addressStartIndex, addressEndIndex);
			 txfAddress.setText(address);
			 
			 String amountStr = null;
			 if(qrDataString.indexOf("?amount=") != -1) {
				 int amountBeginIndex = qrDataString.indexOf("?amount=") + 8;
				 int amountEndIndex = qrDataString.length();
				 amountStr = qrDataString.substring(amountBeginIndex, amountEndIndex);
				 
				 float amount = Float.parseFloat(amountStr); // the URI passes the amount in BTC
				 amount *= 100000000; // convert amount to satoshies
				 amount = TextUtils.satoshiesToBitcoinUnit((long)amount,
						 Authenticator.getWalletOperation().getAccountUnitFromSettings());
				 
				 txfAmount.setText(Long.toString((long)amount));
			 }
		 });
	 }
		
	@SuppressWarnings({ "restriction", "unchecked" })
	public void updateCurrencyChoiceBox(List<String> lstCurrencies, String setValue) {
		// currency choice box
		cbCurrency.getItems().clear();
		for(String s: lstCurrencies)
			cbCurrency.getItems().add(s);
		cbCurrency.setValue(setValue);
		
		cleanAll();
	}
	
	private void cleanAll() {
		txfAmount.setText("");
		txfAddress.setText("");
		
		ivAvatar.setImage(null);
		avatarBox.setVisible(false);
		spinner.setProgress(0);
		loadingAvatarBox.setVisible(false);
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
    	Coin am = Coin.valueOf((long) getAmountValue());
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
	
	/**
	 * return the amount in satoshies depending on the currency selected
	 * 
	 * @return
	 */
	@SuppressWarnings("restriction")
	public double getAmountValue(){
		String a = TextUtils.getAbbreviatedUnit(BitcoinUnit.BTC);
		if(getSelectedCurrency().equals(a)) {
			return TextUtils.bitcoinUnitToSatoshies(Float.parseFloat(txfAmount.getText()), BitcoinUnit.BTC);//(double) Double.parseDouble(txfAmount.getText())*100000000;
		}
		
		a = TextUtils.getAbbreviatedUnit(BitcoinUnit.Millibits);
		if(getSelectedCurrency().equals(a)) {
			return TextUtils.bitcoinUnitToSatoshies(Float.parseFloat(txfAmount.getText()), BitcoinUnit.Millibits);//return (double) Double.parseDouble(txfAmount.getText())*100000;
		}	
		a = TextUtils.getAbbreviatedUnit(BitcoinUnit.Microbits);
		if(getSelectedCurrency().equals(a)) {
			return TextUtils.bitcoinUnitToSatoshies(Float.parseFloat(txfAmount.getText()), BitcoinUnit.Microbits);//return (double) Double.parseDouble(txfAmount.getText())*100;
		}
		else {
			Exchange e = Exchanges.getInstance().currencies.get(getSelectedCurrency());
			if(e != null)
			{
				Long amount = Long.parseLong(txfAmount.getText());
				return e.convertToBitcoin(Currency.valueOf(amount).multiply(Currency.ONE.getValue())).getValue();
			}
			else {
				return -1;// notify user
			}
		}		
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
