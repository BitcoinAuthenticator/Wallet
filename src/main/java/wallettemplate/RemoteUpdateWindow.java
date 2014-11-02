package wallettemplate;

import java.io.IOException;
import java.net.URL;

import wallettemplate.utils.TextFieldValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class RemoteUpdateWindow {	
	private Stage dialogStage;
	private Class<?> resourceLoaderClass;
	
	private RemoteUpdateWindowListener listener;
	
	@FXML public VBox progressBox;
	@FXML public VBox okBox;
	
	@FXML public Label lblTitle;
	@FXML public Label lblDetails;
	@FXML public ProgressBar progressBar;
	
	public RemoteUpdateWindow(Class<?> loaderClss) {
		resourceLoaderClass = loaderClss;
    }
	
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
		return "RemoteUpdateWindow.fxml";
	}
	
	private void initialize() {
		dialogStage.setTitle("Updates");
		
		this.lblDetails.setVisible(false);
		this.okBox.setVisible(false);
	}
	
	public void show() {
		loadFXML();
		dialogStage.show();
		initialize();
	}
	
	public void close() {
		dialogStage.close();
	}
	
	public ProgressBar getProgressBar() {
		return progressBar;
	}
	
	public void setToFailedConnectionMode(String details) {
		this.lblDetails.setVisible(true);
		this.okBox.setVisible(true);
		this.progressBox.setVisible(false);
		
		this.lblTitle.setText("Failed Downloading Updates");
		this.lblDetails.setText(details);
	}
	
	public void setListener(RemoteUpdateWindowListener l) {
		this.listener = l;
	}
	
	/*
	 * 
	 * 	Button Actions
	 * 
	 */
	
	@FXML protected void done(ActionEvent event){
		dialogStage.close();
		if(listener != null)
			listener.UserPressedOk(RemoteUpdateWindow.this);
	}
	
	public interface RemoteUpdateWindowListener {
		public void UserPressedOk(RemoteUpdateWindow window);
	}
}
