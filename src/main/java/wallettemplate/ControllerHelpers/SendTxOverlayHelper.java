package wallettemplate.ControllerHelpers;

import static wallettemplate.utils.GuiUtils.informationalAlert;

import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

import authenticator.Authenticator;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import authenticator.walletCore.utils.BAPassword;
import javafx.animation.Animation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import wallettemplate.Controller;
import wallettemplate.Main;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextUtils;

public class SendTxOverlayHelper {
	public Main.OverlayUI<Controller> txOverlay;
	public Pane txOverlayPane;
	public VBox successVbox;
	public VBox authenticatorVbox;
	public VBox transactionOverviewBox;
	
	public Button btnSendTransaction;
	public Button btnSuccessPaneFinish;
	public Button btnAuthorizeTxLater;
	public Button btnCancel;
	public PasswordField pfPassword;
	
	public ImageView ivLogo1;
	
	
	public SendTxOverlayHelper(Transaction tx, 
    		ArrayList<String> OutputAddresses, 
    		List<TransactionOutput> to, 
    		String changeaddr,
    		Coin outAmount,
    		Coin fee,
    		Coin leavingWallet) {
		
		init(tx, 
    		OutputAddresses, 
    		to, 
    		changeaddr,
    		outAmount,
    		fee,
    		leavingWallet);
	}
	
	@SuppressWarnings({ "static-access", "restriction", "rawtypes" })
	private void init(Transaction tx, 
    		ArrayList<String> OutputAddresses, 
    		List<TransactionOutput> to, 
    		String changeaddr,
    		Coin outAmount,
    		Coin fee,
    		Coin leavingWallet) {
		//Display Transaction Overview
		txOverlayPane = new Pane();
    	txOverlay = Main.instance.overlayUI(txOverlayPane, Main.controller);
		
    	txOverlayPane.setMaxSize(600, 360);
    	txOverlayPane.setStyle("-fx-background-color: white;");
    	txOverlayPane.setEffect(new DropShadow());
		
		transactionOverviewBox = new VBox();
		Label lblOverview = new Label("Transaction Overview");
		transactionOverviewBox.setMargin(lblOverview, new Insets(10,0,10,20));
		lblOverview.setFont(Font.font(null, FontWeight.BOLD, 18));
		
		ListView lvTx= new ListView();
		lvTx.setStyle("-fx-background-color: transparent;");
		lvTx.getStyleClass().add("custom-scroll");
		transactionOverviewBox.setMargin(lvTx, new Insets(0,0,0,20));
		lvTx.setPrefSize(560, 270);
		ObservableList<TextFlow> textformatted = FXCollections.<TextFlow>observableArrayList();
		Text inputtext = new Text("Inputs:                     ");
		inputtext.setStyle("-fx-font-weight:bold;");
		Coin inAmount = Coin.valueOf(0);
		TextFlow inputflow = new TextFlow();
		inputflow.getChildren().addAll(inputtext);
		ArrayList<Text> intext = new ArrayList<Text>();
		for (int b=0; b<tx.getInputs().size(); b++){
			Text inputtext2 = new Text("");
			Text inputtext3 = new Text("");
			inputtext3.setFill(Paint.valueOf("#98d947"));
			inputtext2.setText(tx.getInput(b).getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString() + " ");
			intext.add(inputtext2);
			inAmount = inAmount.add(tx.getInputs().get(b).getValue());
			inputtext3.setText(TextUtils.coinAmountTextDisplay(tx.getInput(b).getValue(), Authenticator.getWalletOperation().getAccountUnitFromSettings()));
			if (b<tx.getInputs().size()-1){
				inputtext3.setText(inputtext3.getText() + "\n                                    ");
			}
			intext.add(inputtext3);
		}
		for (Text t : intext){inputflow.getChildren().addAll(t);}
		textformatted.add(inputflow);
		TextFlow spaceflow = new TextFlow();
		Text space = new Text(" ");
		spaceflow.getChildren().addAll(space);
		textformatted.add(spaceflow);
		Text outputtext = new Text("Outputs:                  ");
		outputtext.setStyle("-fx-font-weight:bold;");
		TextFlow outputflow = new TextFlow();
		outputflow.getChildren().addAll(outputtext);
		ArrayList<Text> outtext = new ArrayList<Text>();
		for (int a=0; a < OutputAddresses.size(); a++){
			Text outputtext2 = new Text("");
			Text outputtext3 = new Text("");
			outputtext3.setFill(Paint.valueOf("#f06e6e"));
			outputtext2.setText(OutputAddresses.get(a) + " ");
			outtext.add(outputtext2);
			outputtext3.setText(TextUtils.coinAmountTextDisplay(to.get(a).getValue(), Authenticator.getWalletOperation().getAccountUnitFromSettings()));
			if (a<OutputAddresses.size()-1){
				outputtext3.setText(outputtext3.getText() + "\n                                     ");
			}
			outtext.add(outputtext3);
		}
		for (Text t : outtext){outputflow.getChildren().addAll(t);}
		textformatted.add(outputflow);
		textformatted.add(spaceflow);
		Text changetext = new Text("Change:                   ");
		changetext.setStyle("-fx-font-weight:bold;");
		Text changetext2 = new Text(changeaddr + " ");
		Text changetext3 = new Text(TextUtils.coinAmountTextDisplay((inAmount.subtract(outAmount)).subtract(fee), Authenticator.getWalletOperation().getAccountUnitFromSettings()));

		changetext3.setFill(Paint.valueOf("#98d947"));
		TextFlow changeflow = new TextFlow();
		changeflow.getChildren().addAll(changetext, changetext2,changetext3);
		textformatted.add(changeflow);
		textformatted.add(spaceflow);
		Text feetext = new Text("Fee:                         ");
		feetext.setStyle("-fx-font-weight:bold;");
		Text feetext2 = new Text(TextUtils.coinAmountTextDisplay(fee, Authenticator.getWalletOperation().getAccountUnitFromSettings()));
		feetext2.setFill(Paint.valueOf("#f06e6e"));
		TextFlow feeflow = new TextFlow();
		feeflow.getChildren().addAll(feetext, feetext2);
		textformatted.add(feeflow);
		textformatted.add(spaceflow);
		Text leavingtext = new Text("Leaving Wallet:       ");
		leavingtext.setStyle("-fx-font-weight:bold;");
		Text leavingtext2 = new Text("-" + TextUtils.coinAmountTextDisplay(leavingWallet, Authenticator.getWalletOperation().getAccountUnitFromSettings()));
		leavingtext2.setFill(Paint.valueOf("#f06e6e"));
		TextFlow leavingflow = new TextFlow();
		leavingflow.getChildren().addAll(leavingtext, leavingtext2);
		textformatted.add(leavingflow);
		lvTx.setItems(textformatted);
		btnCancel = new Button("Cancel");
		btnCancel.getStyleClass().add("clear-button");
		btnCancel.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnCancel.setStyle("-fx-background-color: #d7d4d4;");
            }
        });
        btnCancel.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnCancel.setStyle("-fx-background-color: #999999;");
            }
        });
        btnSendTransaction = new Button("Send Transaction");
        btnSendTransaction.getStyleClass().add("custom-button");
        btnSendTransaction.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnSendTransaction.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
        btnSendTransaction.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnSendTransaction.setStyle("-fx-background-color: #199bd6;");
            }
        });
		pfPassword = new PasswordField();
		pfPassword.setPrefWidth(350);
		pfPassword.setStyle("-fx-border-color: #dae0e5; -fx-background-color: white; -fx-border-radius: 2;");
		if (!Main.UI_ONLY_WALLET_PW.hasPassword() || Main.UI_ONLY_IS_WALLET_LOCKED){
			pfPassword.setDisable(false);
			pfPassword.setPromptText("Enter Password");
			}
		else {
			pfPassword.setDisable(true);
			pfPassword.setPromptText("Wallet is unlocked");
		}
		//success pane
		successVbox = new VBox();
		Image rocket = new Image(Main.class.getResource("rocket.png").toString());
		ImageView img = new ImageView(rocket);
		Label txid = new Label();
		Label lblTxid = new Label("Transaction ID:");
		btnSuccessPaneFinish = new Button("Continue");
		btnSuccessPaneFinish.getStyleClass().add("custom-button");
		btnSuccessPaneFinish.setDisable(false);
		btnSuccessPaneFinish.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnSuccessPaneFinish.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
		btnSuccessPaneFinish.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnSuccessPaneFinish.setStyle("-fx-background-color: #199bd6;");
            }
        });
		txid.setText(tx.getHashAsString());
		final ContextMenu contextMenu = new ContextMenu();
		MenuItem item1 = new MenuItem("Copy");
		item1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				Clipboard clipboard = Clipboard.getSystemClipboard();
				ClipboardContent content = new ClipboardContent();
				content.putString(txid.getText().toString());
				clipboard.setContent(content);
			}
		});
		contextMenu.getItems().addAll(item1);
		txid.setContextMenu(contextMenu);
		successVbox.getChildren().add(img);
		successVbox.getChildren().add(lblTxid);
		successVbox.getChildren().add(txid);
		successVbox.getChildren().add(btnSuccessPaneFinish);
		successVbox.setVisible(false);
		successVbox.setAlignment(Pos.CENTER);
		successVbox.setPrefWidth(600);
		successVbox.setPadding(new Insets(15,0,0,0));
		successVbox.setMargin(lblTxid, new Insets(15,0,0,0));
		successVbox.setMargin(btnSuccessPaneFinish, new Insets(30,0,0,0));
		txOverlayPane.getChildren().add(successVbox);
		
		HBox h = new HBox();
		h.setPadding(new Insets(10,0,0,20));
		h.setMargin(btnCancel, new Insets(0,5,0,10));
		h.setMargin(pfPassword, new Insets(-1,0,0,0));
		h.getChildren().add(pfPassword);
		h.getChildren().add(btnCancel);
		h.getChildren().add(btnSendTransaction);
		transactionOverviewBox.getChildren().add(lblOverview);
		transactionOverviewBox.getChildren().add(lvTx);
		transactionOverviewBox.getChildren().add(h);
		txOverlayPane.getChildren().add(transactionOverviewBox);
		
		createAuthenticatorVbox();
	}
	
	@SuppressWarnings("restriction")
	private void createAuthenticatorVbox() {
		authenticatorVbox = new VBox();
		StackPane stk = new StackPane();
		Image phone = new Image(Main.class.getResource("phone.png").toString());
		ImageView imgphone = new ImageView(phone);
		Image auth1 = new Image(Main.class.getResource("auth1.png").toString());
		ivLogo1 = new ImageView(auth1);
		Image auth2 = new Image(Main.class.getResource("auth2.png").toString());
		ImageView imglogo2 = new ImageView(auth2);
		stk.getChildren().add(imgphone);
		stk.getChildren().add(ivLogo1);
		stk.getChildren().add(imglogo2);
		btnAuthorizeTxLater = new Button("Authorize Later");
		Label authinstructions = new Label("The transaction has been sent to your phone for authorization");
		btnAuthorizeTxLater.getStyleClass().add("custom-button");
		btnAuthorizeTxLater.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnAuthorizeTxLater.setStyle("-fx-background-color: #a1d2e7;");
            }
        });
		btnAuthorizeTxLater.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
            	btnAuthorizeTxLater.setStyle("-fx-background-color: #199bd6;");
            }
        });
		authenticatorVbox.getChildren().add(stk);
		authenticatorVbox.getChildren().add(authinstructions);
		authenticatorVbox.getChildren().add(btnAuthorizeTxLater);
		authenticatorVbox.setVisible(false);
		authenticatorVbox.setAlignment(Pos.CENTER);
		authenticatorVbox.setPrefWidth(600);
		authenticatorVbox.setPadding(new Insets(15,0,0,0));
		authenticatorVbox.setMargin(authinstructions, new Insets(15,0,0,0));
		authenticatorVbox.setMargin(btnAuthorizeTxLater, new Insets(30,0,0,0));
		txOverlayPane.getChildren().add(authenticatorVbox);		
	}
}
