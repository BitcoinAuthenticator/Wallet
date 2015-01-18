package org.authenticator.Utils.OneName;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;

import org.json.JSONObject;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import org.authenticator.Authenticator;
import org.authenticator.Utils.EncodingUtils;
import org.authenticator.Utils.OneName.exceptions.CannotSetOneNameProfileException;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.walletCore.WalletOperation;

public class OneName {
	
	public static void getOneNameData(String onename, WalletOperation wallet, OneNameListener listener) {
		try {
			System.out.println("Downloading OneName profile data: " + onename);
			EncodingUtils.readFromUrl("https://onename.io/" + onename + ".json", new AsyncCompletionHandler<Response>(){
				@Override
				public Response onCompleted(Response arg0) throws Exception {
					try {
						String res = arg0.getResponseBody();
						JSONObject json = new JSONObject(res);
						
						JSONObject bitcoin = null;
						String address = null;
						if(json.has("bitcoin")) {
							bitcoin = json.getJSONObject("bitcoin");
							if(bitcoin.has("address"))
								address = bitcoin.getString("address");
						}
					   	
					   	//TODO - get facebook, twitter, github data
					   	
					   	//Name Formatted
					   	JSONObject name = json.getJSONObject("name");
					   	String formatted = name.getString("formatted");
					   	//Avatar
					   	JSONObject avatar = null;
					   	String imgURL = null;
					   	if(json.has("avatar")) {
					   		avatar = json.getJSONObject("avatar");
					   		if(avatar.has("url"))
					   			imgURL = avatar.getString("url");
					   	}
					    
					    // build return object
					    ConfigOneNameProfile.Builder onb = ConfigOneNameProfile.newBuilder();
					    if(onename != null)
					    	onb.setOnename(onename);
					    if(formatted != null)
					    	onb.setOnenameFormatted(formatted);
					    if(address != null)
					    	onb.setBitcoinAddress(address);
					    if(imgURL != null)
					    	onb.setOnenameAvatarURL(imgURL);
					    
					    
					    //ConfigOneNameProfile ret = wallet.setOneName(onename, formatted, imgURL, null);
						
					    listener.getOneNameData(onb.build());
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
			System.out.println("Downloading OneName avatar image: " + one.getOnenameAvatarURL());
			EncodingUtils.readFromUrl(one.getOnenameAvatarURL(), new AsyncCompletionHandler<Response>(){
				@Override
				public Response onCompleted(Response arg0) {
					try {
						byte[] res = arg0.getResponseBodyAsBytes();
						InputStream in = new ByteArrayInputStream(res);
						BufferedImage bimage = ImageIO.read(in);
						BufferedImage croppedImage = cropDownloadedAvatarImage(bimage);
						
						// save and set image path
						String fileName = "oneAvatar_" + one.getOnename() + ".png";
						String imagePath = saveImage(croppedImage, 
								Authenticator.getApplicationParams().getApplicationDataFolderAbsolutePath(),fileName);
						
						ConfigOneNameProfile.Builder b = ConfigOneNameProfile.newBuilder(one);
						b.setOnenameAvatarFilePath(imagePath);
						
						File file = new File(imagePath);
					    @SuppressWarnings("restriction")
						Image image = new Image(file.toURI().toString());
					    
					    
					    listener.getOneNameAvatarImage(b.build(), image);
						
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