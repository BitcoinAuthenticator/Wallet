package authenticator.network;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import authenticator.Authenticator;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import wallettemplate.Main;

public class OneName {
	
	public static String getAddress(String onename) throws IOException, JSONException {
		JSONObject json = readJsonFromUrl("https://onename.io/" + onename + ".json");
	   	JSONObject bitcoin = json.getJSONObject("bitcoin");
	   	String address = bitcoin.getString("address");
	   	return address;
	}

	/**Downloads the OneName avatar, scales and crops it.*/
	public void getAvatar(String onename) throws IOException, JSONException {
		//Get the url for the image from the onename json
		JSONObject json = readJsonFromUrl("https://onename.io/" + onename + ".json");
	   	JSONObject avatar = json.getJSONObject("avatar");
	    JSONObject name = json.getJSONObject("name");
	    String formattedname = name.getString("formatted");
	    String imgURL = avatar.getString("url");
	    //Download the image
	    BufferedImage image =null;
	    URL url =new URL(imgURL);
	    try{image = ImageIO.read(url);}
	    catch(IOException e){e.printStackTrace();}
	    //Scale the image
	    int imageWidth  = image.getWidth();
	    int imageHeight = image.getHeight();
	    int width;
	    int height;
	    if (imageWidth>imageHeight){
	    	Double temp = (73/ (double)imageHeight)*(double)imageWidth;
	        width = temp.intValue();
	        height = 73;
	    } 
	    else {
	        Double temp = (73/ (double) imageWidth)* (double) imageHeight;
	       	width = 73;
	       	height = temp.intValue();
	    }
	    BufferedImage scaledImage = new BufferedImage(width, height, image.getType());
	    Graphics2D g = scaledImage.createGraphics();
	    g.drawImage(image, 0, 0, width, height, null);
	    g.dispose();
	    //Crop the image
	    int x,y;
	    if (width>height){
	        y=0;
	       	x=(width-73)/2;
	    }
	    else {
	    	x=0;
	    	y=(height-73)/2;
	    }
	    
	    // Save image
	    //Image img = createImage(croppedImage);
	    String imgPath = "";
	    try {
	        // retrieve image
	    	BufferedImage croppedImage = scaledImage.getSubimage(x, y, 73, 73);
	        File outputfile = new File("oneAvatar.png");
	        ImageIO.write(croppedImage, "png", outputfile);
	        imgPath = outputfile.getAbsolutePath();
	    } catch (IOException e) {  e.printStackTrace(); }
	    
	    AuthenticatorConfiguration.ConfigOneNameProfile.Builder onb = AuthenticatorConfiguration.ConfigOneNameProfile.newBuilder();
	    onb.setOnename(onename);
	    onb.setOnenameFormatted(formattedname);
	    onb.setOnenameAvatarURL(imgURL);
	    onb.setOnenameAvatarFilePath(imgPath);
	    Authenticator.getWalletOperation().writeOnename(onb.build());
	    Authenticator.fireonNewUserNamecoinIdentitySelection(onb.build());
	}
	/**For reading the JSON*/
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	/**Reads JSON object from a URL*/
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException { 
		URL urladdr = new URL(url);
		URLConnection conn = urladdr.openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			rd.close();
		}
	}

	public static javafx.scene.image.Image createImage(BufferedImage image) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write((RenderedImage) image, "png", out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		return new javafx.scene.image.Image(in);
	}

}