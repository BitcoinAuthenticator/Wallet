package authenticator.network;

import java.net.URL;

import javafx.scene.image.Image;

public class ONData {

	private String nameFormatted;
	private String bitcoinAddress;
	private URL aviURL;
	
	public ONData(String name, String addr, URL avi){
		this.nameFormatted = name;
		this.bitcoinAddress = addr;
		this.aviURL = avi;
	}
	
	public String getNameFormatted(){
		return nameFormatted;
	}
	
	public String getBitcoinAddress(){
		return bitcoinAddress;
	}
	
	public URL getAvatarURL(){
		return aviURL;
	}
	
}
