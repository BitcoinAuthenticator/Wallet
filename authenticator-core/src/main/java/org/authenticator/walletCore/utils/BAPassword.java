package org.authenticator.walletCore.utils;

public class BAPassword{
	private String pw;
	
	public BAPassword(){ 
		this("");
	}
	
	public BAPassword(BAPassword o){
		this(o.toString());
	}
	
	public BAPassword(String pw){
		this.pw = pw;
	}
	
	public void setPassword(String pw){
		this.pw = pw;
	}
	
	public void cleanPassword(){
		this.pw = "";
	}
	
	public boolean hasPassword(){
		if(this.pw == null)
			return false;
		if(this.pw.length() == 0)
			return false;
		return true;
	}
	
	public String toString(){
		if(!hasPassword())
			return null;
		return pw;
	}
	
	public int length(){
		if(pw != null)
			return pw.length();
		return 0;
	}
	
	public boolean compareTo(String other) {
		if(this.hasPassword())
			return this.toString().equals(other);
		
		return false;
	}
}
