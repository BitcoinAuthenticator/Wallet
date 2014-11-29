package wallettemplate.ControllerHelpers;

import static wallettemplate.utils.GuiUtils.informationalAlert;

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
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.walletCore.exceptions.AddressWasNotFoundException;
import authenticator.walletCore.exceptions.CannotGetAddressException;
import authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import authenticator.walletCore.utils.BAPassword;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.operations.BAOperation;
import authenticator.operations.OperationsFactory;
import authenticator.operations.listeners.OperationListener;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.params.MainNetParams;

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
    		try {
    			String addString = na.getAddress();
    			new Address(Authenticator.getWalletOperation().getNetworkParams(), addString);
    		}
    		catch (AddressFormatException e) {return false;}
    		
    	}
    	//check sufficient funds
    	Coin amount = Coin.ZERO;
    	for(Node n:scrlContent.getChildren())
    	{
    		//NewAddress na = (NewAddress)n;
    		SendToCell na = (SendToCell)n;
    		na.validate();
			long satoshis = (long) na.getAmountValue();
    		amount = amount.add(Coin.valueOf(satoshis));
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
	 * @throws CannotGetAddressException 

	 */
	static public boolean broadcastTx (Transaction tx, 
			String txLabel, 
			String to, 
			@Nullable BAPassword WALLET_PW,
			OperationListener opUpdateListener) throws CannotGetAddressException{
		// broadcast
		if (!txLabel.isEmpty()){
			try {
				Authenticator.getWalletOperation().writeNextSavedTxData(tx.getHashAsString(), "", txLabel);
			} catch (CannotWriteToConfigurationFileException e) {e.printStackTrace(); }
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
		return Authenticator.addOperation(op);
    }	
	
	
	
}
