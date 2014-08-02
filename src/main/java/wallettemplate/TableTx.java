package wallettemplate;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class TableTx {
	
	private String txid;
	private String confirmations;
	private ImageView inOut;
	private String date;
	private String toFrom;
	private String description;
	private String amount;
	
	public TableTx (String ptxid, String pConfirmations, ImageView pInOut, String pDate, String pToFrom, String pDescription, String pAmount){
		this.txid = ptxid;
		this.confirmations = pConfirmations;
		this.inOut = pInOut;
		this.date = pDate;
		this.toFrom = pToFrom;
		this.description = pDescription;
		this.amount = pAmount;
	}
	
	public String getTxid(){
		return txid;
	}
	
	public String getConfirmations(){
		return confirmations;
	}
	
	public void setConfirmations(String pConfirmations){
		this.confirmations = pConfirmations;
	}
	
	public ImageView getInOut(){
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
	
	public String getAmount(){
		return amount;
	}
	
}
