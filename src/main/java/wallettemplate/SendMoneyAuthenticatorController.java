package wallettemplate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;

import com.google.bitcoin.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import authenticator.Authenticator;
import authenticator.operations.ATOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.ui_helpers.ComboBoxHelper;
import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.PopUpNotification;
import static wallettemplate.utils.GuiUtils.crashAlert;
import static wallettemplate.utils.GuiUtils.informationalAlert;

public class SendMoneyAuthenticatorController extends SendMoneyController{
  
	public Main.OverlayUI overlayUi;
	
	@FXML private Button btnAdd;
	@FXML private Button btnSend;
	//Combo
	// Scroll pane
    public ScrollPane scrl;
    private ScrollPaneContentManager scrlContent;
    //
    private String pairID;
    private String pairName;
    private BigInteger balance;
    //
    @FXML Label walletNameLabl;
    @FXML Label balanceLabl;
    
	// Called by FXMLLoader
    public void initialize() {
        scrlContent = new ScrollPaneContentManager()
        					.setSpacingBetweenItems(15);
        
        //
        scrl.setFitToHeight(true);
        scrl.setFitToWidth(true);
        addOutput();
        scrl.setContent(scrlContent);
    }
    
    @Override
    public void loadParams()
	{
    	// get params
        this.pairID = (String) this.arrParams.get(0);
        this.pairName = (String) this.arrParams.get(1);
        this.balance = (BigInteger) this.arrParams.get(2);
        // Update UI
        walletNameLabl.setText(this.pairName);
        balanceLabl.setText(Utils.bitcoinValueToFriendlyString(this.balance));
	}
    
    /**
     * Has a very simple job of validating what the user entered as his transaction, but in a very preliminary way.
     * @return
     */
    private boolean ValidateTx()
    {
    	//Check Outputs
    	if(scrlContent.getCount() == 0)
    		return false;
    	for(Node n:scrlContent.getChildren())
    	{
    		NewAddress na = (NewAddress)n;
    		if(!na.validate())
    			return false;
    	}
    	//check sufficient funds
    	BigInteger amount = null;
    	for(Node n:scrlContent.getChildren())
    	{
    		NewAddress na = (NewAddress)n;
    		if(amount == null)
    			amount = Utils.toNanoCoins(na.txfAmount.getText());
    		amount.add(Utils.toNanoCoins(na.txfAmount.getText()));
    	}
    	if(amount.compareTo(this.balance) > 0) return false;
    	return true;
    }
    public void send(ActionEvent event) {
    	Transaction tx;
        if(ValidateTx()){
        	ArrayList<TransactionOutput> to = new ArrayList<TransactionOutput>();
        	for(Node n:scrlContent.getChildren())
        	{
        		NewAddress na = (NewAddress)n;
        		Address add;
				try {
					add = new Address(Authenticator.getWalletOperation().getNetworkParams(), na.txfAddress.getText());
					TransactionOutput out = new TransactionOutput(Authenticator.getWalletOperation().getNetworkParams(),
	        				null, 
	        				Utils.toNanoCoins(na.txfAmount.getText()), 
	        				add);
					to.add(out);
				} catch (AddressFormatException e) { e.printStackTrace(); }
        		
        	}
        	try {
				tx = Authenticator.getWalletOperation().mktx(pairID, to);
				ATOperation op = OperationsFactory.SIGN_AND_BROADCAST_TX_OPERATION(tx, pairID);
				op.SetOperationUIUpdate(new OnOperationUIUpdate(){
					@Override
					public void onBegin(String str) { }

					@Override
					public void statusReport(String report) {
						
					}

					@Override
					public void onFinished(String str) {
						
					}

					@Override
					public void onError(Exception e, Throwable t) {
						crashAlert(t);
					}
					
				});
				Authenticator.operationsQueue.add(op);
				
			} catch (NoSuchAlgorithmException | AddressFormatException
					| JSONException | IOException e) {
				PopUpNotification p = new PopUpNotification("Something Is not Right ...","Make Sure:\n" +
						"  1) You entered correct values in all fields\n"+
						"  2) You have sufficient funds to cover your outputs\n"+
						"  3) Outputs amount to at least the dust value(" + Utils.bitcoinValueToFriendlyString(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) + ")");
				p.showPopup();
				e.printStackTrace();
			}
        }
        else
        {
        	PopUpNotification p = new PopUpNotification("Something Is not Right ...","Make Sure:\n" +
        																				"  1) You entered correct values in all fields\n"+
        																				"  2) You have sufficient funds to cover your outputs\n"+
        																				"  3) Outputs amount to at least the dust value(" + Utils.bitcoinValueToFriendlyString(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) + ")");
        	p.showPopup();
        }
    }

    private void addOutput()
    {
    	Class[] parameterTypes = new Class[1];
        parameterTypes[0] = int.class;
        Method removeOutput;
		try {
			removeOutput = SendMoneyAuthenticatorController.class.getMethod("removeOutput", parameterTypes);
			NewAddress na = new NewAddress(scrlContent.getCount()).setCancelOnMouseClick(this,removeOutput);
			scrlContent.addItem(na);
			scrl.setContent(scrlContent);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
    	
    	
    }
    public void removeOutput(int index)
    {
    	scrlContent.removeNodeAtIndex(index);
    	// TODO - make it better !
    	for(Node n:scrlContent.getChildren()){
    		NewAddress na = (NewAddress)n;
    		if(na.index < index)
    			continue;
    		na.index--;
    	}
    	scrl.setContent(scrlContent);
    }
    
    public void cancel(ActionEvent event) {
        overlayUi.done();
    }
    
    public void addOutput(ActionEvent event) {
    	addOutput();
    }
    
    public class NewAddress extends HBox
    {
    	TextField txfAddress;
    	TextField txfAmount;
    	Label amountInSatoshi;
    	Label cancelLabel;
    	int index;
    	Method onCancel;
    	public NewAddress setCancelOnMouseClick(Object object, Method method){
    		cancelLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					Object[] parameters = new Object[1];
			        parameters[0] = index;
			        try {
						method.invoke(object, parameters);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}
				}
            });
    		return this;
    	}
    	public NewAddress(int index)
    	{
    		this.index = index;
    		VBox main = new VBox();
    		VBox cancel = new VBox();
    		//Cancel
    		cancelLabel = new Label();
    		cancelLabel.setText("<X>");
    		cancelLabel.setPadding(new Insets(0,5,0,0));
    		
    		AwesomeDude.setIcon(cancelLabel, AwesomeIcon.REMOVE);
            Tooltip.install(cancelLabel, new Tooltip("Delete Output"));
            cancel.setAlignment(Pos.TOP_LEFT);
            cancel.getChildren().add(cancelLabel);
            this.getChildren().add(cancel);
    		
    		//Text Fields
    		txfAddress = new TextField();
    		txfAddress.setPromptText("Recipient Address");
    		txfAddress.setPrefWidth(500);
    		main.getChildren().add(txfAddress);
    		
    		HBox b = new HBox();
    		txfAmount = new TextField();
    		txfAmount.setPromptText("Amount");
    		txfAmount.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed( ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if(txfAmount.getText().length() > 0 && !txfAmount.getText().matches("[a-zA-Z]+"))
						amountInSatoshi.setText(Utils.toNanoCoins(txfAmount.getText()).toString() + " Satoshies");
					else
						amountInSatoshi.setText("Error");
				}
    		});
    		b.setMargin(txfAmount, new Insets(0,200,0,0));
    		b.getChildren().add(txfAmount);
    		amountInSatoshi = new Label();
    		amountInSatoshi.setText("0 Satoshies");
    		b.getChildren().add(amountInSatoshi);
    		main.getChildren().add(b);
    		this.getChildren().add(main);
    	}
   
    	public boolean validate()
        {
        	if(txfAddress.getText().length() == 0)
        		return false;
        	if(txfAmount.getText().length() == 0)
        		return false;
        	if(txfAmount.getText().matches("[a-zA-Z]+"))
        		return false;
        	// check dust amount 
        	BigInteger bi = Utils.toNanoCoins(txfAmount.getText());
        	if(bi.compareTo(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) < 0)
        		return false;
        	
        	return true;
        }
    }
    
    
}
