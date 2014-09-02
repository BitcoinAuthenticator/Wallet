package authenticator.Utils.OneName;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import authenticator.Authenticator;
import authenticator.Utils.EncodingUtils;
import authenticator.Utils.CurrencyConverter.CurrencyConverterSingelton;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.walletCore.WalletOperation;
import wallettemplate.Main;

public class OneName {
	
	public static void getOneNameData(String onename, OneNameListener listener) throws IOException, JSONException {
		EncodingUtils.readFromUrl("https://onename.io/" + onename + ".json", new AsyncCompletionHandler<Response>(){
			@Override
			public Response onCompleted(Response arg0) throws Exception {
				String res = arg0.getResponseBody();
				JSONObject json = new JSONObject(res);
				
				JSONObject bitcoin = json.getJSONObject("bitcoin");
			   	String address = bitcoin.getString("address");
			   	//Name Formatted
			   	JSONObject name = json.getJSONObject("name");
			   	String formatted = name.getString("formatted");
			   	//Avatar
			   	JSONObject avatar = json.getJSONObject("avatar");
			    String imgURL = avatar.getString("url");
			    BufferedImage image =null;
			    URL url = new URL(imgURL);
			    ONData ret = new ONData(formatted, address, url);
				
			    listener.getOneNameData(ret);
			    
				return null;
			}
		});
	}
	
	public static void downloadAvatarImage(URL url, OneNameListener listener) throws IOException, JSONException{
		EncodingUtils.readFromUrl(url.toString(), new AsyncCompletionHandler<Response>(){
			@Override
			public Response onCompleted(Response arg0) throws Exception {
				byte[] res = arg0.getResponseBodyAsBytes();
				InputStream in = new ByteArrayInputStream(res);
				BufferedImage image = ImageIO.read(in);
			    Image ret = prepareDownloadedAvatarImage(image);
				
			    listener.getOneNameAvatarImage(ret);
				
				return null;
			}
		});
		
	}
	
	private static Image prepareDownloadedAvatarImage(BufferedImage image) throws IOException{
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
	    BufferedImage croppedImage = scaledImage.getSubimage(x, y, 73, 73);
	    Image avi = createImage(croppedImage);
		return avi;
	}
	
	/**Downloads the OneName avatar, scales and crops it.*/
	public boolean getAvatar(Authenticator Auth, WalletOperation wallet,String onename) throws IOException, JSONException {
		//Get the url for the image from the onename json
		JSONObject json = readJsonFromUrl("https://onename.io/" + onename + ".json");
	   	JSONObject avatar = json.getJSONObject("avatar");
	    JSONObject name = json.getJSONObject("name");
	    String formattedname = name.getString("formatted");
	    String imgURL = avatar.getString("url");
	    //Download the image
	    BufferedImage image =null;
	    URL url =new URL(imgURL);
	    try{
	    	image = ImageIO.read(url);
	    	
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
		        File outputfile = new File("cached_resources/oneAvatar.png");
		        ImageIO.write(croppedImage, "png", outputfile);
		        imgPath = outputfile.getAbsolutePath();
		    } catch (IOException e) {  e.printStackTrace(); }
		    
		    AuthenticatorConfiguration.ConfigOneNameProfile.Builder onb = AuthenticatorConfiguration.ConfigOneNameProfile.newBuilder();
		    onb.setOnename(onename);
		    onb.setOnenameFormatted(formattedname);
		    onb.setOnenameAvatarURL(imgURL);
		    onb.setOnenameAvatarFilePath(imgPath);
		    wallet.writeOnename(onb.build());
		    Auth.fireonNewUserNamecoinIdentitySelection(onb.build());
	    }
	    catch(IOException e){
	    	e.printStackTrace();
	    	return false;
	    }
	    return true;
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
	
	
	public static class ONData {

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

}