package wallettemplate.ControllerHelpers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import wallettemplate.Controller;
import wallettemplate.Main;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextUtils;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;

import authenticator.Authenticator;
import authenticator.Utils.CurrencyConverter.CurrencyConverterSingelton;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoSettings.BitcoinUnit;
import authenticator.walletCore.exceptions.AddressNotWatchedByWalletException;
import authenticator.walletCore.exceptions.CannotGetAccountFilteredTransactionsException;
import authenticator.walletCore.exceptions.CannotReadFromConfigurationFileException;
import authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

public class UIUpdateHelper extends BaseUI{
	
	public UIUpdateHelper() {
		super(UIUpdateHelper.class);
	}
	
	public static class ReceiveAddressesUpdater extends AsyncTask{
		ChoiceBox box;
		ArrayList<String> addresses;
		
		public ReceiveAddressesUpdater(ChoiceBox b){
			box = b;
		}


		@Override
		protected void doInBackground() {
			
	    	int accountIdx = Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex();
	    	addresses = new ArrayList<String>();
			try {
				for (int i=0; i<10; i++){
					ATAddress newAdd = Authenticator.getWalletOperation()
								.getNextExternalAddress(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex())
								;
					String newAddStr = newAdd.getAddressStr();
					addresses.add(newAddStr);	
				}
					
				
			} catch (Exception e) { e.printStackTrace(); }
		}

		@Override
		protected void onPostExecute() {
			box.getItems().clear();
			
			for (String address : addresses){
				box.getItems().add(address);
	    	}
			box.setValue(addresses.get(0));
		}


		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			
		}


		@Override
		protected void progressCallback(Object... params) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static class  TxPaneHistoryUpdater extends AsyncTask{
		ObservableList<TableTx> txdata;
		ArrayList<String> savedTXIDs;
		//
		TableView txTable;
		TableColumn colToFrom;
		TableColumn colDescription;
		TableColumn colConfirmations;
		
		public TxPaneHistoryUpdater(TableView txTable, TableColumn colToFrom, TableColumn colDescription, TableColumn colConfimrations){
			this.txTable = txTable;
			this.colToFrom = colToFrom;
			this.colDescription = colDescription;
		}
		
		@Override
		protected void onPreExecute() { }

		@SuppressWarnings({ "restriction", "unchecked" })
		@Override
		protected void doInBackground() {
			try {
				getTxData();
			} catch (Exception e) { e.printStackTrace(); }
			
			colToFrom.setOnEditCommit(
		    	    new EventHandler<CellEditEvent<TableTx, String>>() {
		    	        @Override
		    	        public void handle(CellEditEvent<TableTx, String> t) {
		    	            ((TableTx) t.getTableView().getItems().get(
		    	                t.getTablePosition().getRow())
		    	                ).setToFrom(t.getNewValue());
		    	            String txid = ((TableTx) t.getTableView().getItems().get(
		        	                t.getTablePosition().getRow())
		        	                ).getTxid();
		    	            String desc = ((TableTx) t.getTableView().getItems().get(
		        	                t.getTablePosition().getRow())
		        	                ).getDescription();
		    	            String toFrom = ((TableTx) t.getTableView().getItems().get(
		        	                t.getTablePosition().getRow())
		        	                ).getToFrom();
		    	            if (savedTXIDs.size()==0){
		    	            	try {
									Authenticator.getWalletOperation().writeNextSavedTxData(txid, toFrom, desc);
								} catch (CannotWriteToConfigurationFileException e) {
									e.printStackTrace();
								}
		    	            }
		    	            else {
		    	            	if (savedTXIDs.contains(txid)){
		    	            		for (int i=0; i<savedTXIDs.size(); i++){
		    	            			if (savedTXIDs.get(i).equals(txid)){
		    	            				try {Authenticator.getWalletOperation().writeSavedTxData(i, txid, toFrom, desc);} 
		    	            				catch (CannotWriteToConfigurationFileException e) {e.printStackTrace();}
		    	            			}
		    	            		}
		    	            	}
		    	            	else {
		    	            		try {Authenticator.getWalletOperation().writeNextSavedTxData(txid, toFrom, desc);} 
		    	            		catch (CannotWriteToConfigurationFileException e) {e.printStackTrace();}
		    	            	}   
		    	            }
		    	        }
		    	    }
		    	);
			
			
			/**
			 * 
			 */
			colDescription.setOnEditCommit(
		    	    new EventHandler<CellEditEvent<TableTx, String>>() {
		    	        @Override
		    	        public void handle(CellEditEvent<TableTx, String> t) {
		    	            ((TableTx) t.getTableView().getItems().get(
		    	                t.getTablePosition().getRow())
		    	                ).setDescription(t.getNewValue());
		    	            String txid = ((TableTx) t.getTableView().getItems().get(
		        	                t.getTablePosition().getRow())
		        	                ).getTxid();
		    	            String desc = ((TableTx) t.getTableView().getItems().get(
		        	                t.getTablePosition().getRow())
		        	                ).getDescription();
		    	            String toFrom = ((TableTx) t.getTableView().getItems().get(
		        	                t.getTablePosition().getRow())
		        	                ).getToFrom();
		    	            if (savedTXIDs.size()==0){
		    	            	try {Authenticator.getWalletOperation().writeNextSavedTxData(txid, toFrom, desc);} 
		    	            	catch (CannotWriteToConfigurationFileException e) {e.printStackTrace();}
		    	            }
		    	            else {
		    	            	if (savedTXIDs.contains(txid)){
		    	            		for (int i=0; i<savedTXIDs.size(); i++){
		    	            			if (savedTXIDs.get(i).equals(txid)){
		    	            				try {Authenticator.getWalletOperation().writeSavedTxData(i, txid, toFrom, desc);} 
		    	            				catch (CannotWriteToConfigurationFileException e) {e.printStackTrace();}
		    	            			}
		    	            		}
		    	            	}
		    	            	else {
		    	            		try {Authenticator.getWalletOperation().writeNextSavedTxData(txid, toFrom, desc);} 
		    	            		catch (CannotWriteToConfigurationFileException e) {e.printStackTrace();}
		    	            	}   
		    	            }
		    	            //try {setTxHistoryContent();} 
		    	            //catch (Exception e) {e.printStackTrace();}
		    	        }
		    	    }
		    	);
			
		}
		
		@SuppressWarnings({ "deprecation", "restriction" })
		private void getTxData() throws CannotGetAccountFilteredTransactionsException{
			ArrayList<Transaction> history = Authenticator.getWalletOperation().filterTransactionsByAccount(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
	    	savedTXIDs = Authenticator.getWalletOperation().getSavedTxidList();
	    	txdata = FXCollections.observableArrayList();
	    	for (Transaction tx : history){
	    		try {
	    			Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(tx);
		    		Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(tx);
		    		Image in = new Image(Main.class.getResourceAsStream("in.png"));
		    		Image out = new Image(Main.class.getResourceAsStream("out.png"));
		    		ImageView arrow = null;
		    		Text amount = new Text();
		    		String toFrom = "multiple";
		    		if (exit.compareTo(Coin.ZERO) > 0){ // means i sent coins
		    			arrow = new ImageView(out);
		    			amount.setFill(Paint.valueOf("#f06e6e"));
		    			BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
		    			amount.setText(TextUtils.coinAmountTextDisplay(exit.subtract(enter),u));
		    			if (tx.getOutputs().size()==1){
		    				toFrom = tx.getOutput(0).getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
		    			}
		    			else {
		    				int leavingaddrs=0;
		    				ATAddress ca = null;
		    				String o = null;
		    				for (TransactionOutput output : tx.getOutputs()){
		    					ca = Authenticator.getWalletOperation().findAddressInAccounts(output.getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString());
		    					if (ca==null){
		    						leavingaddrs++;
		    						o = ca.getAddressStr();
		    					}
		    				}
		    				if (leavingaddrs==1){toFrom = o;}
		    			}
		    		}
		    		else { // i only received coins
		    			arrow = new ImageView(in);
		    			BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
		    			amount.setText(TextUtils.coinAmountTextDisplay(enter,u));
		    			amount.setFill(Paint.valueOf("#98d947"));
		    			if (tx.getInputs().size()==1){
		    				toFrom = tx.getInput(0).getFromAddress().toString();
		    			}
		    		}
		    		VBox v = new VBox();
		    		v.getChildren().add(arrow);
		    		v.setAlignment(Pos.CENTER);
		    		String desc = tx.getHashAsString();
		    		String date = tx.getUpdateTime().toLocaleString();
		    		for (int i=0; i<savedTXIDs.size(); i++){
		    			if (savedTXIDs.get(i).equals(tx.getHashAsString())){
		    				desc = Authenticator.getWalletOperation().getSavedDescription(i);
		    			}
		    		}
		    		for (int i=0; i<savedTXIDs.size(); i++){
		    			if (savedTXIDs.get(i).equals(tx.getHashAsString())){
		    				toFrom = Authenticator.getWalletOperation().getSavedToFrom(i);
		    			}
		    		}
		    		Label confirmations = new Label();
	    			confirmations.setText(Integer.toString(tx.getConfidence().getDepthInBlocks()));
		    		if (tx.getConfidence().getDepthInBlocks() == 0){
		    			confirmations.setStyle("-fx-background-color: #f06e6e;");
		    			confirmations.setPadding(new Insets(1,4,1,4));
		    		}
		    		else if (tx.getConfidence().getDepthInBlocks() > 0 && tx.getConfidence().getDepthInBlocks() < 6){
		    			confirmations.setStyle("-fx-background-color: #f8fb1a;");
		    			confirmations.setPadding(new Insets(1,4,1,4));
		    		}
		    		else if (tx.getConfidence().getDepthInBlocks() > 5){
		    			confirmations.setStyle("-fx-background-color: #98d947;");
		    			confirmations.setPadding(new Insets(1,4,1,4));
		    		}
		    		VBox v2 = new VBox();
		    		v2.getChildren().add(confirmations);
		    		v2.setAlignment(Pos.CENTER);
		    		TableTx transaction = new TableTx(tx, tx.getHashAsString(), v2, v, date, toFrom, desc, amount);
		    		txdata.add(transaction);
	    		}
	    		catch (Exception e) {
	    			// If in any case one transaction throws an exception, continue with the Tx iteration.
	    			e.printStackTrace();
	    			System.out.println("TX: " + tx.toString());
	    		}
	    		
	    	}
		}

		@SuppressWarnings("restriction")
		@Override
		protected void onPostExecute() {
			txTable.setItems(txdata);
	    	txTable.setEditable(true);
	    	txTable.setOnMouseClicked(new EventHandler<MouseEvent>(){
	    		@Override
	    		public void handle(MouseEvent event) {
	    			if (event.getClickCount()==2){
	    			 @SuppressWarnings("rawtypes")
	    			  ObservableList<TablePosition> cells = txTable.getSelectionModel().getSelectedCells();
	    			  for( TablePosition< TableTx, ? > cell : cells )
	    			  {
	    			     if(!(cell.getColumn()==3) && !(cell.getColumn()==4)){
	    			    	 TableTx txObj= (TableTx)txTable.getItems().get(cell.getRow());
	    			    	 Transaction tx = txObj.getTransaction();
	    			    	 Pane pane = new Pane();
	    			    	 final Main.OverlayUI<Controller> overlay = Main.instance.overlayUI(pane, Main.controller);
	    			    	 pane.setMaxSize(600, 360);
	    			    	 pane.setStyle("-fx-background-color: white;");
	    			    	 pane.setEffect(new DropShadow());
	    			    	 Button btnClose = new Button("Close");
	    			    	 HBox hbclose = new HBox();
	    			    	 hbclose.getChildren().add(btnClose);
	    			    	 hbclose.setAlignment(Pos.CENTER);
	    			    	 VBox v = new VBox();
	    			    	 Label lblOverview = new Label("Transaction Details");
	    			    	 v.setMargin(lblOverview, new Insets(10,0,10,15));
	    			    	 lblOverview.setFont(Font.font(null, FontWeight.BOLD, 18));
	    			    	 ListView lvTx= new ListView();
	    			    	 lvTx.getStyleClass().add("custom-scroll");
	    			    	 v.setMargin(lvTx, new Insets(0,0,10,15));
	    			    	 lvTx.setPrefSize(570, 270);
	    			    	 ObservableList<TextFlow> textformatted = FXCollections.<TextFlow>observableArrayList();
	    			    	 //Transaction ID
	    			    	 Text idtext = new Text("ID:  ");
	    			    	 Text idtext2 = new Text(tx.getHashAsString());
	    			    	 idtext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow idflow = new TextFlow();
	    			    	 idflow.getChildren().addAll(idtext);
	    			    	 idflow.getChildren().addAll(idtext2);
	    			    	 textformatted.add(idflow);
	    			    	 TextFlow spaceflow = new TextFlow();
	    			    	 Text space = new Text(" ");
	    			    	 spaceflow.getChildren().addAll(space);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Date and Time
	    			    	 Text datetext = new Text("Date & Time:          ");
	    			    	 Text datetext2 = new Text(tx.getUpdateTime().toLocaleString());
	    			    	 datetext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow dateflow = new TextFlow();
	    			    	 dateflow.getChildren().addAll(datetext);
	    			    	 dateflow.getChildren().addAll(datetext2);
	    			    	 textformatted.add(dateflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Confirmations
	    			    	 Text conftext = new Text("Confirmations:        ");
	    			    	 Text conftext2 = new Text("");
	    			    	 if (tx.getConfidence().getDepthInBlocks()==0){
	    			    		 conftext2.setFill(Paint.valueOf("#f06e6e"));
	    			    		 conftext2.setText("Unconfirmed");
	    			    	 }
	    			    	 else if (tx.getConfidence().getDepthInBlocks()>0 && tx.getConfidence().getDepthInBlocks()<6){
	    			    		 conftext2.setFill(Paint.valueOf("#f8fb1a"));
	    			    		 conftext2.setText(String.valueOf(tx.getConfidence().getDepthInBlocks()));
	    			    	 }
	    			    	 else {
	    			    		 conftext2.setFill(Paint.valueOf("#98d947"));
	    			    		 conftext2.setText(String.valueOf(tx.getConfidence().getDepthInBlocks()));
	    			    	 }
	    			    	 conftext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow confflow = new TextFlow();
	    			    	 confflow.getChildren().addAll(conftext);
	    			    	 confflow.getChildren().addAll(conftext2);
	    			    	 textformatted.add(confflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Appears in Block
	    			    	 Text blocktext = new Text("Included In Block:  ");
	    			    	 Text blocktext2 = new Text();
	    			    	 try {blocktext2.setText("#" + String.valueOf(tx.getConfidence().getAppearedAtChainHeight()));}
	    			    	 catch (IllegalStateException e){blocktext2.setText("None");}
	    			    	 blocktext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow blockflow = new TextFlow();
	    			    	 blockflow.getChildren().addAll(blocktext);
	    			    	 blockflow.getChildren().addAll(blocktext2);
	    			    	 textformatted.add(blockflow);
	    			    	 textformatted.add(spaceflow); 
	    			    	 //Peers
	    			    	 Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(tx);
	    			    	 Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(tx);	
	    			    	 Text peertext = new Text();
	    			    	 if (exit.compareTo(Coin.ZERO) > 0){ peertext.setText("Seen By:                 ");}
	    			    	 else { peertext.setText("Relayed By:             ");}
	    			    	 Text peertext2 = new Text("");
	    			    	 peertext2.setText(String.valueOf(tx.getConfidence().numBroadcastPeers() + " peers"));
	    			    	 peertext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow peerflow = new TextFlow();
	    			    	 peerflow.getChildren().addAll(peertext);
	    			    	 peerflow.getChildren().addAll(peertext2);
	    			    	 textformatted.add(peerflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Inputs
	    			    	 Text inputtext = new Text("Inputs:                    ");
	    			    	 inputtext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow inputflow = new TextFlow();
	    			    	 inputflow.getChildren().addAll(inputtext);
	    			    	 ArrayList<Text> intext = new ArrayList<Text>();
	    			    	 Coin inAmount = Coin.ZERO;
	    			    	 BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
	    			    	 for (int b=0; b<tx.getInputs().size(); b++){
	    			    		 Text inputtext2 = new Text("");
	    			    		 Text inputtext3 = new Text("");
	    			    		 inputtext3.setFill(Paint.valueOf("#98d947"));
	    			    		 inputtext2.setText(tx.getInput(b).getFromAddress().toString() + " ");
	    			    		 intext.add(inputtext2);
	    			    		 try { 
	    			    			 inputtext3.setText(TextUtils.coinAmountTextDisplay(tx.getInput(b).getValue(),u));
	    			    			 inAmount = inAmount.add(tx.getInput(b).getValue());
	    			    		 } catch (NullPointerException e) {inputtext3.setText("unavailable");}
	    			    		 if (b<tx.getInputs().size()-1){
	    			    			 inputtext3.setText(inputtext3.getText() + "\n                                   ");
	    			    		 }
	    			    		 intext.add(inputtext3);
	    			    	 }
	    			    	 for (Text t : intext){inputflow.getChildren().addAll(t);}
	    			    	 textformatted.add(inputflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Total Inputs
	    			    	 Text intotaltext = new Text("Total Inputs:           ");
	    			    	 Text intotaltext2 = new Text("");
	    			    	 if(tx.getInput(0).getConnectedOutput()!=null){intotaltext2.setText(TextUtils.coinAmountTextDisplay(inAmount, u));}
	    			    	 else {intotaltext2.setText("unavailable");}
	    			    	 intotaltext2.setFill(Paint.valueOf("#98d947"));
	    			    	 intotaltext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow intotalflow = new TextFlow();
	    			    	 intotalflow.getChildren().addAll(intotaltext);
	    			    	 intotalflow.getChildren().addAll(intotaltext2);
	    			    	 textformatted.add(intotalflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Outputs
	    			    	 Text outputtext = new Text("Outputs:                 ");
	    			    	 outputtext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow outputflow = new TextFlow();
	    			    	 outputflow.getChildren().addAll(outputtext);
	    			    	 ArrayList<Text> outtext = new ArrayList<Text>();
	    			    	 Coin outAmount = Coin.ZERO;
	    			    	 for (int a=0; a < tx.getOutputs().size(); a++){
	    			    		 Text outputtext2 = new Text("");
	    			    		 Text outputtext3 = new Text("");
	    			    		 outputtext3.setFill(Paint.valueOf("#f06e6e"));
	    			    		 outputtext2.setText(tx.getOutput(a).getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()) + " ");
	    			    		 outtext.add(outputtext2);
	    			    		 outAmount = outAmount.add(tx.getOutput(a).getValue());
	    			    		 outputtext3.setText(TextUtils.coinAmountTextDisplay(tx.getOutput(a).getValue(),u));
	    			    		 if (a<tx.getOutputs().size()-1){
	    			    			 outputtext3.setText(outputtext3.getText() + "\n                                   ");
	    			    		 }
	    			    		 outtext.add(outputtext3);
	    			    	 }
	    			    	 for (Text t : outtext){outputflow.getChildren().addAll(t);}
	    			    	 textformatted.add(outputflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Total outputs
	    			    	 Text outtotaltext = new Text("Total Outputs:        ");
	    			    	 Text outtotaltext2 = new Text("");
	    			    	 outtotaltext2.setFill(Paint.valueOf("#f06e6e"));
	    			    	 outtotaltext2.setText(TextUtils.coinAmountTextDisplay(outAmount, u));
	    			    	 outtotaltext.setStyle("-fx-font-weight:bold;");
	    			    	 TextFlow outtotalflow = new TextFlow();
	    			    	 outtotalflow.getChildren().addAll(outtotaltext);
	    			    	 outtotalflow.getChildren().addAll(outtotaltext2);
	    			    	 textformatted.add(outtotalflow);
	    			    	 textformatted.add(spaceflow);
	    			    	 //Transaction Fee
	    			    	 Text feetext = new Text("Fee:                        ");
	    			    	 Text feetext2 = new Text("");
	    			    	 try {feetext2.setText(TextUtils.coinAmountTextDisplay(tx.getFee(),u));}
	    			    	 catch (NullPointerException e) {feetext2.setText("unavailable");}
	    			    	 feetext2.setFill(Paint.valueOf("#f06e6e"));
	    			    	 TextFlow feeflow = new TextFlow();
	    			    	 feetext.setStyle("-fx-font-weight:bold;");
	    			    	 feeflow.getChildren().addAll(feetext);
	    			    	 feeflow.getChildren().addAll(feetext2);
	    			    	 textformatted.add(feeflow);
	    			    	 lvTx.setItems(textformatted);
	    			    	 lvTx.setStyle("-fx-background-color: transparent;");
	    			    	 v.getChildren().add(lblOverview);
	    			    	 v.getChildren().add(lvTx);
	    			    	 v.getChildren().add(hbclose);
	    			    	 pane.getChildren().add(v);
	    			    	 btnClose.getStyleClass().add("custom-button");
	    			    	 btnClose.setOnMousePressed(new EventHandler<MouseEvent>(){
	    	            			@Override
	    	            			public void handle(MouseEvent t) {
	    	            				btnClose.setStyle("-fx-background-color: #a1d2e7;");
	    	            			}
	    	            		});
	    	            		btnClose.setOnMouseReleased(new EventHandler<MouseEvent>(){
	    	            			@Override
	    	            			public void handle(MouseEvent t) {
	    	            				btnClose.setStyle("-fx-background-color: #199bd6;");
	    	            			}
	    	            		});
	    			    	 btnClose.setPrefWidth(150);
	    			    	 btnClose.setOnMouseClicked(new EventHandler<MouseEvent>() {
	    			    		 @Override
	    			    		 public void handle(MouseEvent event) {
	    			    			 overlay.done();
	    			    		 }
	    			    	 });
	    			    	 final ContextMenu contextMenu2 = new ContextMenu();
	    					 MenuItem item12 = new MenuItem("Copy");
	    					 item12.setOnAction(new EventHandler<ActionEvent>() {
	    						 public void handle(ActionEvent e) {
	    							 Clipboard clipboard = Clipboard.getSystemClipboard();
	    							 ClipboardContent content = new ClipboardContent();
	    							 String stringToCopy = "";
	    							 switch (lvTx.getSelectionModel().getSelectedIndex()) {
	    					            case 0:  stringToCopy = tx.getHashAsString();
	    					                     break;
	    					            case 1:  break;
	    					            case 2:  stringToCopy = tx.getUpdateTime().toLocaleString();
	    					                     break;
	    					            case 3:  break;
	    					            case 4:  stringToCopy = String.valueOf(tx.getConfidence().getDepthInBlocks());
	    					                     break;
	    					            case 5:  break;
	    					            case 6:  stringToCopy = blocktext2.getText();
	    					                     break;
	    					            case 7:  break;
	    					            case 8:  stringToCopy = String.valueOf(tx.getConfidence().numBroadcastPeers() + " peers");
	    					                     break;
	    					            case 9:  break;
	    					            case 10: for (TransactionInput in : tx.getInputs()){stringToCopy = stringToCopy + "TX: " + in.getOutpoint().toString() + "; ";}
	    					                     break;
	    					            case 11: break;
	    					            case 12: stringToCopy = intotaltext2.getText();
	    					                     break;
	    					            case 13: break;
	    					            case 14: for (TransactionOutput out : tx.getOutputs()){stringToCopy = stringToCopy + out.getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()) + "; ";}
	    					            		 break;
	    					            case 15: break;
	    					            case 16: stringToCopy = outtotaltext2.getText();
	    					            		 break;
	    					            case 17: break;
	    					            case 18: stringToCopy = feetext2.getText();
	    					        }
	    							 content.putString(stringToCopy);
	    							 clipboard.setContent(content);
	    						 }
	    					 });
	    					 contextMenu2.getItems().addAll(item12);
	    					 lvTx.setContextMenu(contextMenu2);
	    			     }
	    			  }
	    			}
	    		}
	    	});
		}

		@Override
		protected void progressCallback(Object... params) { }
		
	}

	public static class TxHistoryContentUpdater  extends AsyncTask{
		private ScrollPaneContentManager scrlViewTxHistoryContentManager;
		ArrayList<Transaction> txAll;
		Map<HBox, String> entries;
		
		public TxHistoryContentUpdater(ScrollPaneContentManager scrlViewTxHistoryContentManager){
			this.scrlViewTxHistoryContentManager = scrlViewTxHistoryContentManager;
		}
		
		@Override
		protected void onPreExecute() {
			if(scrlViewTxHistoryContentManager.getCount() == 0)
			Platform.runLater(new Runnable() {
	            @Override
	            public void run() {
	            	showNoTxHistory();
	            }
	        });
		}

		@Override
		protected void doInBackground() {
	    	try {
				txAll = Authenticator.getWalletOperation().filterTransactionsByAccount(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
			} catch (Exception e) { e.printStackTrace(); }
	    	
	    	if (txAll.size()==0)
	    		return;
	    	
	    	ArrayList<String> savedTXIDs = Authenticator.getWalletOperation().getSavedTxidList();
	    	int size = txAll.size();
	    	int n;
	    	if (size < 10) {n=size;}
	    	else {size =  10;}
	    	entries = new HashMap<HBox, String>();
	    	for(int i=0; i<size; i++){
	    		String txid = txAll.get(i).getHashAsString();
	    		String tip = "";
	    		// build node 
	    		HBox mainNode = new HBox();
	        		
	    		// left box
	    		VBox leftBox = new VBox();
	    		Label l1 = new Label();
	    		l1.setStyle("-fx-font-weight: SEMI_BOLD;");
	    		l1.setTextFill(Paint.valueOf("#6e86a0"));
	    		l1.setFont(Font.font(13));
	    		l1.setText(txAll.get(i).getUpdateTime().toLocaleString()); 
	    		tip += "Txid: " + txid + "\n";
	    		leftBox.getChildren().add(l1);
	    		
	    		Label l2 = new Label();
	    		l2.setStyle("-fx-font-weight: SEMI_BOLD;");
	    		l2.setTextFill(Paint.valueOf("#6e86a0"));
	    		l2.setFont(Font.font(11));
	    		for (int a=0; a<savedTXIDs.size(); a++){
	    			if (savedTXIDs.get(a).equals(txAll.get(i).getHashAsString())){
	    				if(!Authenticator.getWalletOperation().getSavedDescription(a).equals("")){txid = Authenticator.getWalletOperation().getSavedDescription(a);}
	    			}
	    		}
	    		if (txid.length()>20){txid = txid.substring(0, 20) + "...";}
	    		l2.setText(txid); 
	    		tip += "When: " + txAll.get(i).getUpdateTime().toLocaleString() + "\n";
	    		leftBox.getChildren().add(l2);
	    		
	    		mainNode.getChildren().add(leftBox);
	    		
	    		// right box
	    		VBox rightBox = new VBox();
	    		HBox content = new HBox();
	    		rightBox.setPadding(new Insets(0,0,0,40));
	    		Label l3 = new Label();
	    		l3.setStyle("-fx-font-weight: SEMI_BOLD;");
	    		l3.setPadding(new Insets(0,5,0,0));
	    		//check is it receiving or sending
	    		Coin enter = Authenticator.getWalletOperation().getTxValueSentToMe(txAll.get(i));//txAll.get(i).getValueSentToMe(Main.bitcoin.wallet());
	    		Coin exit = Authenticator.getWalletOperation().getTxValueSentFromMe(txAll.get(i));//txAll.get(i).getValueSentFromMe(Main.bitcoin.wallet());
	    		Image in = new Image(Main.class.getResourceAsStream("in.png"));
	    		Image out = new Image(Main.class.getResourceAsStream("out.png"));
	    		ImageView arrow = null;
	    		if (exit.compareTo(Coin.ZERO) > 0){ // means i sent coins
	    			l3.setTextFill(Paint.valueOf("#ea4f4a"));
	    			BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
	    			l3.setText("-" + TextUtils.coinAmountTextDisplay(exit.subtract(enter),u)); // get total out minus enter to subtract change amount
	    			tip += "Amount: -" + exit.subtract(enter).toFriendlyString() + "\n";	
	    			arrow = new ImageView(out);
	    		}
	    		else { // i only received coins
	    			l3.setTextFill(Paint.valueOf("#98d947"));
	    			BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
	    			l3.setText(TextUtils.coinAmountTextDisplay(enter, u));
	    			tip+= "Amount: " + enter.toFriendlyString() + "\n";
	    			arrow = new ImageView(in);
	    		}
	     		l3.setFont(Font.font(13));
	    		content.getChildren().add(l3);
	    		content.getChildren().add(arrow);
	    		rightBox.getChildren().add(content);
	    		mainNode.getChildren().add(rightBox);
	    		
	    		entries.put(mainNode, tip);
	    	}

		}

		@Override
		protected void onPostExecute() {
			scrlViewTxHistoryContentManager.clearAll();
			if (txAll.size()==0){
				showNoTxHistory();
	    		return;
	    	}
			
			for(HBox m: entries.keySet()){
				Tooltip.install(m, new Tooltip(entries.get(m)));
				scrlViewTxHistoryContentManager.addItem(m);	
			}
		}
		
		private void showNoTxHistory(){
			scrlViewTxHistoryContentManager.clearAll();
			HBox mainNode = new HBox();
    		Label l = new Label("                    No transaction history   ");
    		l.setStyle("-fx-font-weight: SEMI_BOLD;");
    		l.setTextFill(Paint.valueOf("#6e86a0"));
    		l.setFont(Font.font(13));
    		mainNode.getChildren().add(l);
    		Image inout = new Image(Main.class.getResourceAsStream("in-out.png"));
    		ImageView arrows = new ImageView(inout);
    		mainNode.getChildren().add(arrows);
    		scrlViewTxHistoryContentManager.addItem(mainNode);	
		}

		@Override
		protected void progressCallback(Object... params) { }
		
	}

	public static class BalanceUpdater extends AsyncTask{
		Coin unconfirmed;
		Coin confirmed;
		
		Label lblConfirmedBalance;
		Label lblUnconfirmedBalance;
		
		public BalanceUpdater(Label lblConfirmedBalance, Label lblUnconfirmedBalance){
			this.lblConfirmedBalance = lblConfirmedBalance;
			this.lblUnconfirmedBalance = lblUnconfirmedBalance;
		}
		
		@Override
		protected void onPreExecute() { }

		@Override
		protected void doInBackground() {
			unconfirmed = Coin.ZERO;
			confirmed = Coin.ZERO;
	    	try {
				unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
				confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
			} catch (CannotReadFromConfigurationFileException e) { e.printStackTrace(); }
	  
		}

		@Override
		protected void onPostExecute() {
			BitcoinUnit u = Authenticator.getWalletOperation().getAccountUnitFromSettings();
			lblConfirmedBalance.setText(TextUtils.coinAmountTextDisplay(confirmed, u));
	        lblUnconfirmedBalance.setText(TextUtils.coinAmountTextDisplay(unconfirmed, u));
	        
	        new CurrencyConverterSingelton(new CurrencyConverterSingelton.CurrencyConverterListener(){
				@Override
				public void onFinishedGettingCurrencyData(CurrencyConverterSingelton currencies) {
					Platform.runLater(new Runnable() {
					      @Override public void run() {
								try {
									attachBalanceToolTip();
								} catch (CannotReadFromConfigurationFileException e) {
									e.printStackTrace();
								}
					      }
					    });
				}

				@Override
				public void onErrorGettingCurrencyData(Exception e) {
					Platform.runLater(() -> GuiUtils.informationalAlert("Cannot Download Currency Data","Some functionalities may be compromised"));
				}
	        });
		}
		
		private void attachBalanceToolTip() throws CannotReadFromConfigurationFileException{
	    	  Coin unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
		      Coin confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
			  
			  // Confirmed
			  //double conf = Double.parseDouble(confirmed.toPlainString())*last;
			  double conf = CurrencyConverterSingelton.USD.convertToCurrency(Double.parseDouble(confirmed.toPlainString()));
			  Tooltip.install(lblConfirmedBalance, new Tooltip(String.valueOf(conf) + " USD"));
			  
			  // Unconfirmed
			  //double unconf = Double.parseDouble(unconfirmed.toPlainString())*last;
			  double unconf = CurrencyConverterSingelton.USD.convertToCurrency(Double.parseDouble(unconfirmed.toPlainString()));
			  Tooltip.install(lblUnconfirmedBalance, new Tooltip(String.valueOf(unconf) + " USD"));
	    }

		@Override
		protected void progressCallback(Object... params) { }
		
	}
}
