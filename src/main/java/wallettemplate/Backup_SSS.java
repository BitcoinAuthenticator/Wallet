package wallettemplate;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.json.JSONException;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;

import authenticator.Authenticator;
import authenticator.WalletFile;
import authenticator.WalletOperation;
import authenticator.BipSSS.BipSSS;
import authenticator.BipSSS.BipSSS.EncodingFormat;
import authenticator.BipSSS.BipSSS.IncompatibleSharesException;
import authenticator.BipSSS.BipSSS.InvalidContentTypeException;
import authenticator.BipSSS.BipSSS.NotEnoughSharesException;
import authenticator.BipSSS.BipSSS.Share;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.ui_helpers.ComboBoxHelper;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.PopUpNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Backup_SSS extends BaseUI implements Initializable{
	// Table View
	private ObservableList<Shard> shardData = FXCollections.observableArrayList();
	@FXML private TableView<Shard> tblView;
    @FXML private TableColumn<Shard, String> clmShard;
    @FXML private Label shardLabel;
    
    // Other UI elements
	public Main.OverlayUI overlayUi;
	public Button cancelBtn;


    @FXML
    public void cancel(ActionEvent event) {
    	super.cancel();
        overlayUi.done();
    }

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.init();
        clmShard.setCellValueFactory(new PropertyValueFactory<Shard, String>("Shard"));
        
        try {
			this.populateUI();
			tblView.setItems(shardData);
	        tblView.setEditable(false);
		} catch (IOException | IncompatibleSharesException | NotEnoughSharesException | InvalidContentTypeException e) { e.printStackTrace(); }
        
        
	}
	
	// Population funcitons
	private ArrayList<PairingObject> getUpdatedPairingObjectArray(){
		return Authenticator.getWalletOperation().getAllPairingObjectArray();
	}
	
	private void populateUI() throws IOException, IncompatibleSharesException, NotEnoughSharesException, InvalidContentTypeException
	{
		populateObservableList();
	}
		
	private void populateObservableList() throws IOException, IncompatibleSharesException, NotEnoughSharesException, InvalidContentTypeException
	{
		shardData.clear();
		DeterministicKey key = Authenticator.getWalletOperation().currentReceiveKey();
		String privKey = key.getPrivateKeyEncoded(Authenticator.getWalletOperation().getNetworkParams()).toString();
		List<Share> shares = BipSSS.shard(key, 2, 4, EncodingFormat.COMPACT, Authenticator.getWalletOperation().getNetworkParams());
		for(Share sh:shares)
		{
			String share = sh.toString();
			shardData.add(new Shard(share)); 
			
		}
		//
		ECKey reconKey = BipSSS.combinePrivateKey(shares);
		String reconKeyStr = reconKey.getPrivateKeyEncoded(Authenticator.getWalletOperation().getNetworkParams()).toString();
		assert(reconKeyStr.endsWith(privKey));
	}
    
	public class Shard{
		private String shardStr;
		
		public Shard(){ } 
		public Shard(String shardStr){
			this.shardStr = shardStr;
		}
		
		// getters ans setters
		public String getShard() { return this.shardStr; }
		public void setShard(String shardStr) { this.shardStr = shardStr; }
	}
}
