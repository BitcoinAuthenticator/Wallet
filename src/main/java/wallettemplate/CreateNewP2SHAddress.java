package wallettemplate;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.json.JSONException;

import com.google.bitcoin.core.AddressFormatException;

import authenticator.Authenticator;
import authenticator.WalletFile;
import authenticator.WalletOperation;
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

public class CreateNewP2SHAddress extends BaseUI implements Initializable{
	// Table View
	private ObservableList<Pair> personData = FXCollections.observableArrayList();
	@FXML private TableView<Pair> tblView;
    @FXML private TableColumn<Pair, String> clmName;
    @FXML private TableColumn<Pair, String> clmPairID;
    @FXML private TableColumn<Pair, String> clmAddress;
    @FXML private Label nameLabel;
    @FXML private Label pairIDLabel;
    @FXML private Label addressLabel;
	
    //ComboBox
    @FXML private ComboBox cmbPairings;
    private Map<String,String>pairNameToId;
    public Button btnAddKey;
    
    // Other UI elements
	public Main.OverlayUI overlayUi;
	public Button cancelBtn;


    @FXML
    public void cancel(ActionEvent event) {
    	super.cancel();
        overlayUi.done();
    }
    
    @FXML
    public void addKey(ActionEvent event) throws IOException, NoSuchAlgorithmException, JSONException {
    	if(pairNameToId.containsKey((String)cmbPairings.getValue()))
    	{
    		try {
				Authenticator.getWalletOperation().genP2SHAddress(pairNameToId.get((String)cmbPairings.getValue()));
			} catch (AddressFormatException e) {
				Authenticator.getWalletOperation().LOG.info(e.toString());
				PopUpNotification p = new PopUpNotification("Something Went Wrong ...","");
				p.showPopup();
				e.printStackTrace();
			}
    		populateUI();
    	}
    	else
    	{
    		// What ?
    	}
    }

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.init();
		clmName.setCellValueFactory(new PropertyValueFactory<Pair, String>("pairingName"));
        clmPairID.setCellValueFactory(new PropertyValueFactory<Pair, String>("pairingID"));
        clmAddress.setCellValueFactory(new PropertyValueFactory<Pair, String>("address"));
        
        this.populateUI();
        tblView.setItems(personData);
        tblView.setEditable(false);
        
	}
	
	// Population funcitons
	private ArrayList<PairingObject> getUpdatedPairingObjectArray(){
		return Authenticator.getWalletOperation().getAllPairingObjectArray();
	}
	
	private void populateUI()
	{
		ArrayList<PairingObject> arr = this.getUpdatedPairingObjectArray();
		populateObservableList(arr);
		pairNameToId = ComboBoxHelper.populateComboWithPairingNames(cmbPairings);
	}
		
	private void populateObservableList(ArrayList<PairingObject> arr)
	{
		personData.clear();
		for(PairingObject po:arr)
		{
			for(KeyObject ko:po.keys.keys)
				personData.add(new Pair(po,ko.priv_key,ko.address,0,ko.index)); //TODO amount
			
		}
	}
    
	public class Pair{
		private String aes_key;
		private String master_public_key;
		private String chain_code;
		private String GCM;
		private String pairingID;
		private boolean testnet; 
		private int keys_n;
		private String pairingName = "";
		//keys
		private String priv_key;
		private String address;
		private double amount;
		private int index;
		
		public Pair(){ } 
		public Pair(PairingObject po,
				String priv_key, 
				String address, 
				double amount,
				int index){ 
			this(po.aes_key,
					po.master_public_key,
					po.chain_code,
					po.GCM,
					po.pairingID,
					po.testnet,
					po.keys_n,
					po.pairingName,
					//keys
					priv_key,
					address,
					amount,
					index);
		}
		public Pair(String aes_key,
				String master_public_key,
				String chain_code,
				String GCM,
				String pairID, 
				boolean testnet,
				int keys_n,
				String pairName,
				//keys
				String priv_key, 
				String address, 
				double amount,
				int index){
			
			this.aes_key = aes_key;
			this.master_public_key = master_public_key;
			this.chain_code = chain_code;
			this.GCM = GCM;
			this.pairingID = pairID;
			this.testnet = testnet;
			this.keys_n = keys_n;
			this.pairingName = pairName;
			//keys
			this.priv_key = priv_key;
			this.address = address;
			this.amount = amount;
			this.index = index;
		}
		
		// getters ans setters
		public String getAes_key() { return this.aes_key; }
		public void setAes_key(String value) { this.aes_key = value; } 
		
		public String getMaster_public_key() { return this.master_public_key; }
		public void setMaster_public_key(String value) { this.master_public_key = value; }
		
		public String getChain_code() { return this.chain_code; }
		public void setChain_code(String value) { this.chain_code = value; }
		
		public String getGCM() { return this.GCM; }
		public void setGCM(String value) { this.GCM = value; }
		
		public String getPairingID() { return this.pairingID; }
		public void setPairingID(String value) { this.pairingID = value; } 
		
		public boolean getTestnet() { return this.testnet; }
		public void setTestnet(boolean value) { this.testnet = value; }
		
		public int getKeys_n() { return this.keys_n; }
		public void setKeys_n(int value) { this.keys_n = value; }
		
		public String getPairingName() { return this.pairingName; }
		public void setPairingName(String value) { this.pairingName = value; } 
		
		//keys
		public String getPriv_key() { return this.priv_key; }
		public void setPriv_key(String privkey) { this.priv_key = privkey; } 
		
		public String getAddress() { return this.address; }
		public void setAddress(String add) { this.address = add; }
		
		public double getAmount() { return this.amount; }
		public void setAmount(double value) { this.amount = value; }
		
		public int getIndex() { return this.index; }
		public void setIndex(int indx) { this.index = indx; }
	
	}
}
