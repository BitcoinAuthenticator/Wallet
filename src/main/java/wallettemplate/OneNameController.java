package wallettemplate;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.io.IOException;

import org.json.JSONException;

import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import authenticator.Authenticator;
import authenticator.Utils.OneName.OneName;
import authenticator.Utils.OneName.OneNameAdapter;
import authenticator.Utils.OneName.OneNameListener;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
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
	
	public void initialize(){
		 //super(AccountsController.class);
		AnchorPane.setEffect(new DropShadow());
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
					OneName.downloadAvatarImage(data, Authenticator.getWalletOperation(), new OneNameAdapter() {
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
							else
								Authenticator.fireonNewOneNameIdentitySelection(one, img);
							
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
			}
		});		
	}
	
	@FXML protected void done (){
		overlayUi.done();
	}
	
	@SuppressWarnings("restriction")
	private void setAllComponentsEnabled(boolean value) {
		//txtOneName.setDisable(!value);
		btnOK.setDisable(!value);;
		btnCancel.setDisable(!value);
		//btnSignUp.setDisable(!value);
		//btnBack.setDisable(!value);
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
		Main.instance.overlayUI("DisplayOneName.fxml");
		OneNameControllerDisplay.loadOneName("register");
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
