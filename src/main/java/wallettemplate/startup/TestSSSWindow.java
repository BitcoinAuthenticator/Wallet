package wallettemplate.startup;

import java.awt.Button;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;

import authenticator.BipSSS.BipSSS;
import authenticator.BipSSS.BipSSS.IncompatibleSharesException;
import authenticator.BipSSS.BipSSS.InvalidContentTypeException;
import authenticator.BipSSS.BipSSS.NotEnoughSharesException;
import authenticator.BipSSS.BipSSS.Share;
import authenticator.BipSSS.SSSUtils;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

import org.bitcoinj.core.Coin;

import wallettemplate.Main;
import wallettemplate.controls.ScrollPaneContentManager;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.TextFieldValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;

public class TestSSSWindow extends BaseUI{	
	@FXML private ScrollPane scrlPieces;
	private ScrollPaneContentManager contentManager;
	
	@FXML private ScrollPane scrlResults;
	private ScrollPaneContentManager contentManagerResults;
	
	@FXML private TextArea txfAccountName;
	
	static List<Share> shares;
	static byte[] mnemonicEntropy;
	static int threshhold;
	static Stage stage;
	
	public TestSSSWindow(){ 
		super(TestSSSWindow.class);
	}

	public TestSSSWindow(List<Share> s, byte[] me, int t){ 
		shares = s;
		mnemonicEntropy = me;
		threshhold = t;
		stage = loadFXML(stage, getViewURL(getViewPath()), 881,543);
	}
	
	public void initialize() {
		setPiecesCells(shares);
		
		contentManagerResults = new ScrollPaneContentManager();
		scrlResults.setContent(contentManagerResults);
		scrlResults.setHbarPolicy(ScrollBarPolicy.NEVER);
	}
	
	@SuppressWarnings("restriction")
	private void setPiecesCells(List<Share> shares){
		contentManager = new ScrollPaneContentManager();
		scrlPieces.setContent(contentManager);
		scrlPieces.setHbarPolicy(ScrollBarPolicy.NEVER);
		for(Share s: shares){
			TestSSSCell c = new TestSSSCell();
			c.setPiece(s.toString());
			contentManager.addItem(c);
		}
	}
	
	@SuppressWarnings("restriction")
	private void addResult(boolean didPass, List<Share> shares){
		String lblStr = didPass? "Passed !\n":"Failed !\n";
		String piecesDesc = "Pieces: ";
		for(Share s: shares)
			piecesDesc += s.shareNumber + ", ";
		lblStr += piecesDesc;
		
		TestSSSResultCell c = new TestSSSResultCell();
		c.setResult(lblStr);
		if(didPass)
        	c.setIcon(new Image("wallettemplate/startup/success_icon.png"));
        else
        	c.setIcon(new Image("/wallettemplate/startup/failed_icon.png"));
		contentManagerResults.addItem(c);
	}
	
	@SuppressWarnings("restriction")
	private Stage loadFXML(Stage s, URL url, int width, int height) {    	
		s = new Stage();
		try {
			FXMLLoader loader = new FXMLLoader(url);
			s.setTitle("Test SSS Spliting");
	    	Scene scene;
			scene = new Scene((AnchorPane) loader.load(), width, height);
			final String file = TextFieldValidator.class.getResource("GUI.css").toString();
	        scene.getStylesheets().add(file); 
	        s.setScene(scene);	
	        return s;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	private String getViewPath() {
		return "startup/TestSSS.fxml";
    }

    private URL getViewURL(String path) {
        return Main.class.getResource(path);
    }
    
    public void show(){
    	stage.show();
    }
        
    @FXML protected void done(ActionEvent event){
    	stage.close();
	}
    
    @FXML protected void testSSS(ActionEvent event){
    	SSSUtils ut = new SSSUtils();
    	List<Share[]> combinations = ut.getAllPossibleCombinations(shares, threshhold);
    	BipSSS tester = null;
    	for(Share[] com:combinations){
    		boolean result = false;
    		try {
    			// res should be mnemonic entropy
				byte[] res = tester.combineSeed(Arrays.asList(com));
				result = Arrays.equals(mnemonicEntropy, res);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
    		
    		addResult(result, Arrays.asList(com));
    	}
	}
    
}
