package wallettemplate;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.db.ConfigFile;
import authenticator.helpers.BAApplication;
import authenticator.helpers.exceptions.AccountWasNotFoundException;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.utils.Threading;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import wallettemplate.startup.StartupController;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextFieldValidator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Optional;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;

import static wallettemplate.utils.GuiUtils.*;

public class Main extends BAApplication {
    public static WalletAppKit bitcoin;
    public static Main instance;
    private StackPane uiStack;
    private AnchorPane mainUI;
    public static Controller controller;
    public static Stage stage;
    public static Authenticator auth;
    public static Stage startup;
    public static BAApplicationParameters returnedParamsFromSetup;

    @Override
    public void start(Stage mainWindow) throws Exception {
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();
        
        if(super.BAInit())
	        try {
	            init(mainWindow);
	        } catch (Throwable t) {
	            // Nicer message for the case where the block store file is locked.
	            if (Throwables.getRootCause(t) instanceof BlockStoreException) {
	                GuiUtils.informationalAlert("Already running", "This application is already running and cannot be started twice.");
	            } else {
	                throw t;
	            }
	        }
        else
        	Runtime.getRuntime().exit(0);
    }

    @SuppressWarnings("restriction")
	private void init(Stage mainWindow) throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            //AquaFx.style();
        }
              
        // Load the GUI. The Controller class will be automagically created and wired up.
        mainWindow.initStyle(StageStyle.UNDECORATED);
        URL location = getClass().getResource("gui.fxml");
        FXMLLoader loader = new FXMLLoader(location);
		mainUI = (AnchorPane) loader.load();
        controller = loader.getController();
        // Configure the window with a StackPane so we can overlay things on top of the main UI.
        uiStack = new StackPane(mainUI);
        mainWindow.setTitle(BAApplication.ApplicationParams.getAppName());
        final Scene scene = new Scene(uiStack, 850, 483);
        final String file = TextFieldValidator.class.getResource("GUI.css").toString();
        scene.getStylesheets().add(file);  // Add CSS that we need.
        mainWindow.setScene(scene);
        stage = mainWindow;
        
        String filePath1 = new java.io.File( "." ).getCanonicalPath() + "/" + ApplicationParams.getAppName() + ".wallet";
        File f1 = new File(filePath1);
        if(!f1.exists()) { 
        	Parent root;
            try {
            	StartupController.appParams = ApplicationParams;
                root = FXMLLoader.load(Main.class.getResource("/wallettemplate/startup/walletstartup.fxml"));
                startup = new Stage();
                startup.setTitle("Setup");
                startup.initStyle(StageStyle.UNDECORATED);
                Scene scene1 = new Scene(root, 607, 400);
                final String file1 = TextFieldValidator.class.getResource("GUI.css").toString();
                scene1.getStylesheets().add(file1);  // Add CSS that we need.
                startup.setScene(scene1);
                startup.show();
            } catch (IOException e) {e.printStackTrace();}
        } else {finishLoading();}
    }
    
    @SuppressWarnings("restriction")
	public static void finishLoading() throws IOException, AccountWasNotFoundException{
    	/**
    	 * If we get returned params from startup, use that
    	 */
    	BAApplicationParameters params = returnedParamsFromSetup == null? BAApplication.ApplicationParams: returnedParamsFromSetup;
    	
    	// Make log output concise.
        BriefLogFormatter.init();
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        NetworkParameters np = null;
        if(params.getBitcoinNetworkType() == NetworkType.MAIN_NET){
        	np = MainNetParams.get();
        	bitcoin = new WalletAppKit(np, new File("."), params.getAppName());
            // Checkpoints are block headers that ship inside our app: for a new user, we pick the last header
            // in the checkpoints file and then download the rest from the network. It makes things much faster.
            // Checkpoint files are made using the BuildCheckpoints tool and usually we have to download the
            // last months worth or more (takes a few seconds).
            bitcoin.setCheckpoints(Main.class.getResourceAsStream("checkpoints"));
            // As an example!
            bitcoin.useTor();
        }
        else if(params.getBitcoinNetworkType() == NetworkType.TEST_NET){
        	np = TestNet3Params.get();
        	bitcoin = new WalletAppKit(np, new File("."), params.getAppName());
        	bitcoin.useTor();
        }

        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.
        bitcoin.setAutoSave(true);
        bitcoin.setDownloadListener(controller.progressBarUpdater())
               .setBlockingStartup(false)
               .setUserAgent(params.getAppName(), "1.0");
        bitcoin.startAsync();
        bitcoin.awaitRunning();
        // Don't make the user wait for confirmations for now, as the intention is they're sending it their own money!
        bitcoin.wallet().allowSpendingUnconfirmedTransactions();
        bitcoin.peerGroup().setMaxConnections(11);
        bitcoin.wallet().setKeychainLookaheadSize(0);
        
        System.out.println(bitcoin.wallet());
        
        controller.onBitcoinSetup();
        
        /**
         * Authenticator Operation Setup
         */
        
        ConfigFile config = new ConfigFile(BAApplication.ApplicationParams.getAppName());
    	auth = new Authenticator(bitcoin.wallet(), bitcoin.peerGroup(), params, config.getHierarchyPubKey());
    	auth.startAsync();
    	controller.onAuthenticatorSetup();
    
    	stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
    		@SuppressWarnings("static-access")
			@Override
    		public void handle(WindowEvent e) {
    			Action response = null;
    			if(Authenticator.getWalletOperation().getPendingRequestSize() > 0 || Authenticator.getQueuePendingOperations() > 0){
    				response = Dialogs.create()
            	        .owner(stage)
            	        .title("Warning !")
            	        .masthead("Pending Requests/ Operations")
            	        .message("Exiting now will cancell all pending requests and operations.\nDo you want to continue?")
            	        .actions(Dialog.Actions.YES, Dialog.Actions.NO)
            	        .showConfirm();
    			}
    			
	        	// Or no conditioning needed or user pressed Ok
	        	if (response == null || (response != null && response == Dialog.Actions.YES)) {
	        		handleStopRequest();
	        	}
	        	
    		}
    	});
        	
        // start UI
        stage.show();
    }
    
    @SuppressWarnings("restriction")
	public static void handleStopRequest(){    	
    	// Pop a "Shutting Down" window
    	Stage stageNotif = new Stage();
        Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  stage.hide();
				  Parent root;
			        try {
			            root = FXMLLoader.load(Main.class.getResource("ShutDownWarning.fxml"));
			            stageNotif.setTitle("Important !");
			            stageNotif.setScene(new Scene(root, 576, 110));
			            stageNotif.show();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			  }
		});
    	
    	
    	bitcoin.addListener(new Service.Listener() {
			@Override public void terminated(State from) {
				System.out.println("Bitcoin Wallet terminated");
				if(auth == null || auth.state() == State.TERMINATED){
					close();
				}
				else{
					while(true){
						if(auth.state() == State.TERMINATED){
							close();
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) { e.printStackTrace(); }
					}
				}
	         }
			
			private void close(){
				Platform.runLater(new Runnable() { 
					  @Override
					  public void run() {
						  stageNotif.close();
					  }
				 });
				Runtime.getRuntime().exit(0);
			}
		}, MoreExecutors.sameThreadExecutor());
		bitcoin.stopAsync();
    
		if(auth != null)
	        auth.stopAsync();        
    }

    public class OverlayUI<T> {
        public Node ui;
        public T controller;

        public OverlayUI(Node ui, T controller) {
            this.ui = ui;
            this.controller = controller;
        }

        public void show() {
            blurOut(mainUI);
            uiStack.getChildren().add(ui);
            fadeIn(ui);
        }

        public void done() {
            checkGuiThread();
            fadeOutAndRemove(ui, uiStack);
            blurIn(mainUI);
            this.ui = null;
            this.controller = null;
        }
    }

    public <T> OverlayUI<T> overlayUI(Node node, T controller) {
        checkGuiThread();
        OverlayUI<T> pair = new OverlayUI<T>(node, controller);
        // Auto-magically set the overlayUi member, if it's there.
        try {
            controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        pair.show();
        return pair;
    }

    /** Loads the FXML file with the given name, blurs out the main UI and puts this one on top. */
    public <T> OverlayUI<T> overlayUI(String name) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = getClass().getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            OverlayUI<T> pair = new OverlayUI<T>(ui, controller);
            // Auto-magically set the overlayUi member, if it's there.
            try {
                controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
            }
            pair.show();
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    @Override
    public void stop() throws Exception {
      
    }

    @SuppressWarnings("restriction")
	public static void main(String[] args) {
        launch(args);
    }
}
