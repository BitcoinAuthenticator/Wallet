package org.wallet.ControllerHelpers;

import org.bitcoinj.core.Transaction;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class TableTx {
	
	private Transaction T;
	private String txid;
	private VBox confirmations;
	private VBox inOut;
	private String date;
	private String toFrom;
	private String description;
	private Text amount;
	
	public TableTx (Transaction pT, String ptxid, VBox pConfirmations, VBox pInOut, String pDate, String pToFrom, String pDescription, Text pAmount){
		this.T = pT;
		this.txid = ptxid;
		this.confirmations = pConfirmations;
		this.inOut = pInOut;
		this.date = pDate;
		this.toFrom = pToFrom;
		this.description = pDescription;
		this.amount = pAmount;
	}
	
	public Transaction getTransaction(){
		return T;
	}
	
	public String getTxid(){
		return txid;
	}
	
	public VBox getConfirmations(){
		return confirmations;
	}
	
	public void setConfirmations(VBox pConfirmations){
		this.confirmations = pConfirmations;
	}
	
	public VBox getInOut(){
		return inOut;
	}
	
	public String getDate(){
		return date;
	}
	
	public String getToFrom(){
		return toFrom;
	}
	
	public void setToFrom(String pToFrom){
		this.toFrom = pToFrom;
	}
	
	public String getDescription(){
		return description;
	}
	
	public void setDescription(String pDescription){
		this.description = pDescription;
	}
	
	public Text getAmount(){
		return amount;
	}
	
}
