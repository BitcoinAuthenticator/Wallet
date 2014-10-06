package wallettemplate.utils.dialogs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

import javax.annotation.Nullable;

import wallettemplate.Main;
import wallettemplate.PairWallet;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.utils.dialogs.BADialog.BADialogResponse;
import wallettemplate.utils.dialogs.BADialog.BADialogResponseListner;

public class BADialogBase{
	@FXML public Button btnDone;
	@FXML public Button btnOk;
	@FXML public Button btnCancel;
	@FXML public Button btnYes;
	@FXML public Button btnNo;
	@FXML public Label lblTitle;
	@FXML public Label lblDesc;
	@FXML public TextField txfInput;
	
	private Stage dialogStage;
	
	private Class<?> resourceLoaderClass;
	private String resourceViewPath;
	private Runnable doneRunnable = () -> {};
	private String windowTitle = "";
	private String title = "";
	private String description = "";
	private BADialogResponseListner response;

	public BADialogBase() {
	}
	
	@SuppressWarnings("restriction")
	private void loadFXML() { 
		dialogStage = new Stage();
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(this);
			loader.setLocation(getViewURL());
			
			Scene scene;
			scene = new Scene((AnchorPane) loader.load());
			final String file = TextFieldValidator.class.getResource("GUI.css").toString();
	        scene.getStylesheets().add(file); 
	        dialogStage.setScene(scene);	
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	private URL getViewURL() {
        return resourceLoaderClass.getResource(getViewPath());
    }
	
	private String getViewPath() {
		return resourceViewPath;
	}
	
	private String getWindowTitle() {
		return windowTitle;
	}
	
	private void initializeDialog() {
		dialogStage.setTitle(getWindowTitle());
		
		if(this.lblTitle != null)
			this.lblTitle.setText(title);
		
		if(this.lblDesc != null)
			this.lblDesc.setText(description);
	}
	
	/*
	 * 
	 * 	Button Actions
	 * 
	 */
	
	@FXML protected void done(ActionEvent event){
		doneRunnable.run();
		dialogStage.close();
	}
	
	@FXML protected void okAnswer(ActionEvent event){
		doneRunnable.run();
		if(this.response != null)
			this.response.onResponse(BADialogResponse.Ok, txfInput.getText());
		dialogStage.close();
	}
	
	@FXML protected void cancelAnswer(ActionEvent event){
		doneRunnable.run();
		if(this.response != null)
			this.response.onResponse(BADialogResponse.Cancel, null);
		dialogStage.close();
	}
	
	@FXML protected void yesAnswer(ActionEvent event){
		doneRunnable.run();
		if(this.response != null)
			this.response.onResponse(BADialogResponse.Yes, null);
		dialogStage.close();
	}
	
	@FXML protected void noAnswer(ActionEvent event){
		doneRunnable.run();
		if(this.response != null)
			this.response.onResponse(BADialogResponse.No, null);
		dialogStage.close();
	}
	
	/*
	 * 
	 * 	API
	 * 
	 */
	
	public void showDialog() {
		loadFXML();
		dialogStage.show();
		initializeDialog();
	}
	
	public BADialogBase setViewPath(String v) {
		resourceViewPath = v;
		return this;
	}
	
	public BADialogBase setResourceClass(Class<?> T) {
		resourceLoaderClass = T;
		return this;
	}
	
	/**
	 * is triggered when the user closes the dialog
	 * @param r
	 * @return
	 */
	public BADialogBase setOnDone(Runnable r) {
		doneRunnable = r;
		return this;
	}
	
	public BADialogBase setWindowTitle(String t) {
		windowTitle = t;
		return this;
	}
	
	public BADialogBase setTitle(String t) {
		title = t;
		return this;
	}
	
	public BADialogBase setDesc(String d) {
		description = d;
		return this;
	}
	
	public BADialogBase setResponseListener(BADialogResponseListner l) {
		response = l;
		return this;
	}
}
