package wallettemplate;

import authenticator.Authenticator;
import authenticator.OnAuthenticatoGUIUpdateListener;
import authenticator.ui_helpers.BAApplication;

import com.aquafx_project.AquaFx;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.params.TestNet2Params;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.utils.Threading;
import com.google.common.base.Throwables;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import wallettemplate.utils.BaseUI;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextFieldValidator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static wallettemplate.utils.GuiUtils.*;

public class Main extends BAApplication {

	public static String APP_NAME = "WalletTemplate";
    public static Main instance;
    public Controller mainController;
    private StackPane uiStack;
    private Pane mainUI;

    @Override
    public void start(Stage mainWindow) throws Exception {
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();
     
        if(super.BAInit(APP_NAME))
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

    private void init(Stage mainWindow) throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            AquaFx.style();
        }
        // Load the GUI. The Controller class will be automagically created and wired up.
        URL location = getClass().getResource("main.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        mainUI = loader.load();

        mainController = loader.getController();
        // Configure the window with a StackPane so we can overlay things on top of the main UI.
        uiStack = new StackPane(mainUI);
        mainWindow.setTitle(APP_NAME);
        final Scene scene = new Scene(uiStack);
        TextFieldValidator.configureScene(scene);   // Add CSS that we need.
        mainWindow.setScene(scene);
        
        // Make log output concise.
        BriefLogFormatter.init();
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        NetworkParameters np = null;
        if(this.ApplicationParams.getBitcoinNetworkType() == NetworkType.MAIN_NET){
        	np = MainNetParams.get();
        	bitcoin = new WalletAppKit(np, new File("."), APP_NAME);
            // Checkpoints are block headers that ship inside our app: for a new user, we pick the last header
            // in the checkpoints file and then download the rest from the network. It makes things much faster.
            // Checkpoint files are made using the BuildCheckpoints tool and usually we have to download the
            // last months worth or more (takes a few seconds).
            bitcoin.setCheckpoints(getClass().getResourceAsStream("checkpoints"));
            // As an example!
            bitcoin.useTor();
        }
        else if(this.ApplicationParams.getBitcoinNetworkType() == NetworkType.TEST_NET){
        	np = TestNet3Params.get();
        	bitcoin = new WalletAppKit(np, new File("."), APP_NAME+"_testnet");
        	bitcoin.useTor();
        }        

        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.
        bitcoin.setDownloadListener(mainController.progressBarUpdater())
               .setBlockingStartup(false)
               .setUserAgent(APP_NAME, "1.0");
        bitcoin.startAsync();
        bitcoin.awaitRunning();
        // Don't make the user wait for confirmations for now, as the intention is they're sending it their own money!
        bitcoin.wallet().allowSpendingUnconfirmedTransactions();
        bitcoin.peerGroup().setMaxConnections(11);
        System.out.println(bitcoin.wallet());
                
        /**
         * Authenticator Operation Setup
         */
        new Authenticator(bitcoin.wallet(), bitcoin.peerGroup(), new OnAuthenticatoGUIUpdateListener(){

			@Override
			public void simpleTextMessage(String msg) {
				
			}
        	
        })
        .setApplicationParams(ApplicationParams)
        .start();

        mainController.onBitcoinSetup();
        mainWindow.show();
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
            mainController.refreshUI();
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
    public <T extends BaseUI> OverlayUI<T> overlayUI(String name, ArrayList<Object> params) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = getClass().getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            if(params != null && params.size() >0){
            	controller.setParams(params);
            	controller.loadParams();
            }
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
        bitcoin.stopAsync();
        bitcoin.awaitTerminated();
        
        // Waits until all threads are stopped
        Authenticator.stop();
        
        // Forcibly terminate the JVM because Orchid likes to spew non-daemon threads everywhere.
        Runtime.getRuntime().exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
