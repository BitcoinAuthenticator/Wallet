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
import authenticator.Utils.OneName.exceptions.CannotSetOneNameProfileException;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.walletCore.WalletOperation;
import wallettemplate.Main;

public class OneName {
	
	public static void getOneNameData(String onename, WalletOperation wallet, OneNameListener listener) {
		try {
			EncodingUtils.readFromUrl("https://onename.io/" + onename + ".json", new AsyncCompletionHandler<Response>(){
				@Override
				public Response onCompleted(Response arg0) throws Exception {
					try {
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
					    
					    //ONData ret = new ONData(formatted, address, url);
					    
					    ConfigOneNameProfile ret = wallet.setOneName(onename, formatted, imgURL, null);
						
					    listener.getOneNameData(ret);
					}
					catch(Exception e) {
						e.printStackTrace();
						listener.getOneNameData(null);
					}
				    
					return null;
				}
			});
		}
		catch(IOException e) {
			e.printStackTrace();
			listener.getOneNameData(null);
		}
	}
	
	public static void downloadAvatarImage(ConfigOneNameProfile one, WalletOperation wallet, OneNameListener listener) {
		try {
			EncodingUtils.readFromUrl(one.getOnenameAvatarURL(), new AsyncCompletionHandler<Response>(){
				@Override
				public Response onCompleted(Response arg0) {
					try {
						byte[] res = arg0.getResponseBodyAsBytes();
						InputStream in = new ByteArrayInputStream(res);
						BufferedImage bimage = ImageIO.read(in);
						BufferedImage croppedImage = cropDownloadedAvatarImage(bimage);
						
						// save and set image path
						String imagePath = saveImage(croppedImage, Authenticator.getApplicationParams().getApplicationDataFolderAbsolutePath(),"oneAvatar.png");
						ConfigOneNameProfile oneRet = wallet.setOneName(one.getOnename(), 
								one.getOnenameFormatted(), 
								one.getOnenameAvatarURL(), 
								imagePath);
						
						File file = new File(imagePath);
					    @SuppressWarnings("restriction")
						Image image = new Image(file.toURI().toString());
					    
					    
					    listener.getOneNameAvatarImage(oneRet, image);
						
					}
					catch(Exception e) {
						e.printStackTrace();
						listener.getOneNameAvatarImage(null, null);
					}
					
					return null;
				}
			});
		}
		catch (IOException e) {
			e.printStackTrace();
			listener.getOneNameAvatarImage(null, null);
		}
		
	}
	
	private static String saveImage(BufferedImage img, String filePath, String fileName) throws CannotSetOneNameProfileException{
		 try {
			 ByteArrayOutputStream out = new ByteArrayOutputStream();
			 File outputfile = new File(filePath + "cached_resources" + File.separator + fileName);//oneAvatar.png");
			 ImageIO.write(img, "png", outputfile);
			 return outputfile.getAbsolutePath();
		    } catch (IOException e) {  
		    	e.printStackTrace(); 
		    	throw new CannotSetOneNameProfileException("Failed to save one name avatar image");
		    }
	}
	
//	public static javafx.scene.image.Image createImage(BufferedImage image) throws IOException {
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		ImageIO.write((RenderedImage) image, "png", out);
//		out.flush();
//		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//		return new javafx.scene.image.Image(in);
//	}
	
	private static BufferedImage cropDownloadedAvatarImage(BufferedImage image) throws IOException{
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
	    return  scaledImage.getSubimage(x, y, 73, 73);
//	    Image avi = createImage(croppedImage);
//		return avi;
	}
}