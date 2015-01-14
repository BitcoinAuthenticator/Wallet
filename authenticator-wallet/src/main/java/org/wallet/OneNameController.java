package org.wallet;

import static org.wallet.utils.GuiUtils.informationalAlert;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.wallet.utils.BaseUI;
import org.wallet.utils.GuiUtils;

import org.authenticator.Authenticator;
import org.authenticator.Utils.OneName.OneName;
import org.authenticator.Utils.OneName.OneNameAdapter;
import org.authenticator.Utils.OneName.OneNameListener;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class OneNameController  extends BaseUI{
	@FXML private TextField txtOneName;
	@FXML private AnchorPane AnchorPane;
	@FXML private Pane Pane1;
	@FXML private Pane Pane2;
	public Main.OverlayUI overlayUi;
	@FXML private Button btnOK;
	@FXML private Button btnCancel;
	@FXML private Button btnSignUp;
	@FXML private Button btnBack;
	@FXML private Button btnDelete;
	
	public void initialize(){
		AnchorPane.setEffect(new DropShadow());
		
		if(Authenticator.getWalletOperation().isOnenameAvatarSet())
			btnDelete.setDisable(false);
		else
			btnDelete.setDisable(true);
	}
	
	@FXML protected void onename (ActionEvent event){
		setAllComponentsEnabled(false);
		btnOK.setText("Loading");

		OneName.getOneNameData(txtOneName.getText(), Authenticator.getWalletOperation(), new OneNameAdapter() {
			@SuppressWarnings("restriction")
			@Override
			public void getOneNameData(ConfigOneNameProfile data) {
				if(data == null) {
					Platform.runLater(new Runnable() { 
						  @Override
						  public void run() {
							  GuiUtils.informationalAlert("Could not find OneName User", "");
							  btnOK.setText("Ok");
							  setAllComponentsEnabled(true);
						  }
						});
				}
				else {
					downloadImage(data);
				}
			}
			
			private void downloadImage(ConfigOneNameProfile data) {
				OneName.downloadAvatarImage(data, Authenticator.getWalletOperation(), new OneNameAdapter() {
					@SuppressWarnings("restriction")
					@Override
					public void getOneNameAvatarImage(ConfigOneNameProfile one, Image img) {
						if(one == null || img == null) {
							Platform.runLater(new Runnable() { 
								  @Override
								  public void run() {
									  GuiUtils.informationalAlert("Could not download OneName Image", "");
									  btnOK.setText("Ok");
									  setAllComponentsEnabled(true);
								  }
								});
						}
						else {
							//write one name profile
							try {
								Authenticator.getWalletOperation().writeOnename(one);
							} catch (IOException e) {
								e.printStackTrace();
								Platform.runLater(() -> {
									GuiUtils.informationalAlert("Could not save OneName profile", "");
									btnOK.setText("Ok");
									setAllComponentsEnabled(true);
								});
							}
							Authenticator.fireonOneNameIdentityChanged(one, img);
						}
						
						Platform.runLater(new Runnable() { 
						  @Override
						  public void run() {
							  btnOK.setText("Ok");
							  btnCancel.setText("Done");
							  btnCancel.setDisable(false);
						  }
						});
					}
				});
			}
		});		
	}
	
	@FXML protected void done (){
		overlayUi.done();
	}
	
	@FXML protected void deleteAvatar (ActionEvent event){
		if(Authenticator.getWalletOperation().isOnenameAvatarSet()) {
			boolean result = false;
			try {
				result = Authenticator.getWalletOperation().deleteOneNameAvatar();
			} catch (IOException e) {
				e.printStackTrace();
				GuiUtils.informationalAlert("Could not delete OneName profile", "");
			}
			if(result == false) {
				GuiUtils.informationalAlert("Could not delete OneName profile", "");
			}
			else {
				Authenticator.fireonOneNameIdentityChanged(null, null);
				done();
			}
		}
	}
	
	@SuppressWarnings("restriction")
	private void setAllComponentsEnabled(boolean value) {
		btnOK.setDisable(!value);;
		btnCancel.setDisable(!value);
	}
	
	@FXML protected void learnMore(){
		Animation ani = GuiUtils.fadeOut(Pane1);
		GuiUtils.fadeIn(Pane2);
		Pane1.setVisible(false);
		Pane2.setVisible(true);
	}
	
	@FXML protected void goBack(){
		Animation ani = GuiUtils.fadeOut(Pane2);
		GuiUtils.fadeIn(Pane1);
		Pane2.setVisible(false);
		Pane1.setVisible(true);
	}
	
	@FXML protected void openapp(){
		overlayUi.done();
		
		ArrayList<Object> l = new ArrayList<Object>();
		l.add("register");
		Main.instance.overlayUI("OneNameApp.fxml", l);
	}
	
	@FXML protected void OKpressed(){
		btnOK.setStyle("-fx-background-color: #57cbfb;");
	}
	@FXML protected void OKreleased(){
		btnOK.setStyle("-fx-background-color: #4db2dc;");
	}
	@FXML protected void Cancelpressed(){
		btnCancel.setStyle("-fx-background-color: #57cbfb;");
	}
	@FXML protected void Cancelreleased(){
		btnCancel.setStyle("-fx-background-color: #4db2dc;");
	}
	@FXML protected void Backpressed(){
		btnBack.setStyle("-fx-background-color: #57cbfb;");
	}
	@FXML protected void Backreleased(){
		btnBack.setStyle("-fx-background-color: #4db2dc;");
	}
	@FXML protected void SignUppressed(){
		btnSignUp.setStyle("-fx-background-color: #57cbfb;");
	}
	@FXML protected void SignUpreleased(){
		btnSignUp.setStyle("-fx-background-color: #4db2dc;");
	}
}
