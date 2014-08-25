package wallettemplate.ControllerHelpers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;
import org.json.JSONObject;

import wallettemplate.Controller;
import wallettemplate.Main;
import wallettemplate.OneNameControllerDisplay;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.controls.SendToCell;
import authenticator.Authenticator;
import authenticator.Utils.EncodingUtils;
import authenticator.Utils.OneName.OneName;
import authenticator.Utils.OneName.OneName.ONData;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.walletCore.exceptions.AddressWasNotFoundException;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.operations.BAOperation;
import authenticator.operations.OperationsFactory;
import authenticator.operations.listeners.OperationListener;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.params.MainNetParams;
import com.google.common.base.Throwables;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;

public class SendTxHelper {
	@SuppressWarnings("restriction")
	static public boolean ValidateTx(ScrollPaneContentManager scrlContent, Coin fee) throws Exception
    {
    	//Check Outputs
    	if(scrlContent.getCount() == 0)
    		return false;
    	for(Node n:scrlContent.getChildren())
    	{
    		//NewAddress na = (NewAddress)n;
    		SendToCell na = (SendToCell)n;
    		try {Address outAddr = new Address(Authenticator.getWalletOperation().getNetworkParams(), na.getAddress());}
    		catch (AddressFormatException e) {return false;}
    		
    	}
    	//check sufficient funds
    	Coin amount = Coin.ZERO;
    	for(Node n:scrlContent.getChildren())
    	{
    		//NewAddress na = (NewAddress)n;
    		SendToCell na = (SendToCell)n;
    		na.validate();
    		double a;
			if (na.getSelectedCurrency().equals("BTC")){
				a = na.getAmountValue();
			}
			else {
				// TOOD - it is not async !
				JSONObject json = EncodingUtils.readJsonFromUrl("https://api.bitcoinaverage.com/ticker/global/" + na.getSelectedCurrency() + "/");
				double last = json.getDouble("last");
				a = na.getAmountValue();
			}
			long satoshis = (long) a;
    		amount = amount.add(Coin.valueOf((long)a));
    	}
    	amount = amount.add(fee);
    	//
    	Coin confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());   	
    	Coin unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
    	Coin balance = confirmed.add(unconfirmed);
    	if(amount.compareTo(balance) > 0) return false;
    	
    	//Check min dust amount 
    	if(amount.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) return false;
    	
    	return true;
    }
	
	/**
	 * 
	 * @param tx
	 * @param txLabel
	 * @param to
	 * @param WALLET_PW
	 * @param opUpdateListener
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws AddressWasNotFoundException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException
	 * @throws AccountWasNotFoundException 
	 */
	static public boolean broadcastTx (Transaction tx, 
			String txLabel, 
			String to, 
			@Nullable String WALLET_PW,
			OperationListener opUpdateListener) throws NoSuchAlgorithmException, AddressWasNotFoundException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AccountWasNotFoundException {
		// broadcast
		walletDB config = Authenticator.getWalletOperation().configFile;
		if (!txLabel.isEmpty()){
			try {config.writeNextSavedTxData(tx.getHashAsString(), "", txLabel);}
			catch (IOException e) {e.printStackTrace();}
		}
		BAOperation op = null;
		if(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountType() == WalletAccountType.StandardAccount){
			Map<String,ATAddress> keys = new HashMap<String,ATAddress>();
			for(TransactionInput in:tx.getInputs()){				
				// get address
				String add = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
				
				// find key
				ATAddress ca = Authenticator.getWalletOperation().findAddressInAccounts(add);
				
				//add key
				keys.put(add, ca);
			}
			op = OperationsFactory.BROADCAST_NORMAL_TRANSACTION(txLabel, 
					to, 
					Authenticator.getWalletOperation(),
					tx,
					keys,
					WALLET_PW);
		}
		else{
			String pairID = Authenticator.getWalletOperation().getActiveAccount().getPairedAuthenticator().getPairingID();
			op = OperationsFactory.SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION(Authenticator.getWalletOperation(),
					tx, 
					pairID, 
					txLabel,
					to,
					false,
					null,
					null,
					WALLET_PW);
		}
		
		// operation listeners
		op.SetOperationUIUpdate(opUpdateListener);
		if(Authenticator.checkForOperationNetworkRequirements(op) == true)
			return Authenticator.addOperation(op);
		else
			opUpdateListener.onError(new Exception("Cannot add operation to queue, network requirements not available"), null);
		return false;
    }	
	
}
