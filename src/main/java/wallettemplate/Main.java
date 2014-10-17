package wallettemplate;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.BAApplicationParameters.WrongOperatingSystemException;
import authenticator.Utils.EncodingUtils;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.helpers.BAApplication;
import authenticator.network.TCPListener;
import authenticator.network.TrustedPeerNodes;
import authenticator.operations.BAOperation;
import authenticator.operations.listeners.OperationListenerAdapter;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.walletCore.BAPassword;
import authenticator.walletCore.WalletOperation;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

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
import wallettemplate.utils.FileUtils;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.utils.dialogs.BADialog;
import wallettemplate.utils.dialogs.BADialog.BADialogResponse;
import wallettemplate.utils.dialogs.BADialog.BADialogResponseListner;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

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
    public static File destination;
    public static File walletFolder;
    
	 /**
	  * In order to make wallet encryption and decryption smoother, we keep
	  * the wallet's password in memory (ONLY !!) so decryption won't prompt an "Enter password" dialog
	  */
	 public static BAPassword UI_ONLY_WALLET_PW;
	 
	 /**
	  * As seen in {@link wallettemplate.Main#UI_ONLY_WALLET_PW UI_ONLY_WALLET_PW}, the wallet's lock
	  * is merely a UI thing cause we keep it locked all the time.<br>
	  * This boolean represents the UI's wallet encrypted state
	  */
	 public static boolean UI_ONLY_IS_WALLET_LOCKED = true;

    @Override
    public void start(Stage mainWindow) {
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();
        
        UI_ONLY_WALLET_PW = new BAPassword();
        
        
	        try {
	        	if(super.BAInit()) {
	        		System.out.println(toString());
	            	init(mainWindow);
	        	}
	        	else
	            	Runtime.getRuntime().exit(0);
	        } catch (Exception t) {
	            if(t instanceof WrongOperatingSystemException)
	            	GuiUtils.informationalAlert("Error", "Could not find an appropriate OS");
	            
	            else 
	            	Runtime.getRuntime().exit(0);
	        }
    }

    @SuppressWarnings("restriction")
	private void init(Stage mainWindow) {
    	try {
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
            
            String filePath1 = ApplicationParams.getApplicationDataFolderAbsolutePath() + ApplicationParams.getAppName() + ".wallet";
            File f1 = new File(filePath1);
            if(!f1.exists()) { 
            	Parent root;
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
                versionCheck();
               
                
            } else {finishLoading();}
        } 
    	catch (Exception e) {
    		e.printStackTrace();
    		throw new CouldNotIinitializeWalletException("Could Not initialize wallet"); 
    	}
    }
    
    @SuppressWarnings("restriction")
	public static void finishLoading(){
    	/**
    	 * If we get returned params from startup, use that
    	 */
    	BAApplicationParameters AppParams = returnedParamsFromSetup == null? BAApplication.ApplicationParams: returnedParamsFromSetup;
    	
    	// Make log output concise.
        BriefLogFormatter.init();
        Threading.USER_THREAD = Platform::runLater;

        NetworkParameters np = null;
        InputStream inCheckpint = null;
        if(AppParams.getBitcoinNetworkType() == NetworkType.MAIN_NET){
        	np = MainNetParams.get();        	

        	inCheckpint = Main.class.getResourceAsStream("checkpoints");
        }
        else if(AppParams.getBitcoinNetworkType() == NetworkType.TEST_NET){
        	np = TestNet3Params.get();
        	
        	inCheckpint = Main.class.getResourceAsStream("checkpoints.testnet");
        }

        
        bitcoin = new WalletAppKit(np, new File(AppParams.getApplicationDataFolderAbsolutePath()), AppParams.getAppName()){
            @Override
            protected void onSetupCompleted() {
                // Don't make the user wait for confirmations for now, as the intention is they're sending it
                // their own money!
            	bitcoin.peerGroup().setMaxConnections(11);
                bitcoin.wallet().setKeychainLookaheadSize(0);
                bitcoin.wallet().allowSpendingUnconfirmedTransactions();
                bitcoin.peerGroup().setBloomFilterFalsePositiveRate(AppParams.getBloomFilterFalsePositiveRate());
                System.out.println(bitcoin.wallet());
                Platform.runLater(controller::onBitcoinSetup);
                
                /**
                 * Authenticator Setup
                 */
                startAuthenticator(AppParams);
            }
        };
        
        // check single wallet instance
        try {
			if (bitcoin.isChainFileLocked()) {
			    informationalAlert("Already running", "This application is already running and cannot be started twice.");
			    Platform.exit();
			    return;
			}
		} catch (IOException e1) { 
			throw new CouldNotIinitializeWalletException("Could Not verify a single wallet instance");
		}
        
        // check we loaded checkpoints
        if(inCheckpint == null)
    		throw new CouldNotIinitializeWalletException("Could Not load Checkpoints");
        bitcoin.setCheckpoints(inCheckpint);
        
        if(AppParams.getShouldConnectWithTOR())
        	bitcoin.useTor();
        
        if(AppParams.getShouldConnectToLocalHost())
        	bitcoin.connectToLocalHost();
        
        if(AppParams.getShouldConnectToTrustedPeer()) {
        	try {
				bitcoin.setPeerNodes(new PeerAddress[] { new PeerAddress(InetAddress.getByName(AppParams.getTrustedPeer())) });
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
        }
        
        bitcoin.setDownloadListener(new WalletOperation().getDownloadEvenListener());
        bitcoin.setAutoSave(true);
        bitcoin.setAutoStop(true);
        bitcoin.setBlockingStartup(false)
               .setUserAgent(AppParams.getAppName(), "1.0");
        bitcoin.startAsync();      
        
    	
    	/*
    	 * stage close event
    	 */
        hockCloseEvent(stage);
        	
        // start UI
        stage.show();
        versionCheck();
        
        if (destination!=null){
        	FileUtils.ZipHelper.zipDir(walletFolder.getAbsolutePath(), destination.getAbsolutePath());      		
        }
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
    	
		bitcoin.stopAsync();
		
		if(auth != null)
	        auth.stopAsync();      
		
		new Thread(){
			@Override
			public void run() {
				/**
				 * the wallet kit has a weird bug that it doesn't shut down.
				 * if it takes more than 10 seconds force shut it down
				 */
				int cnt = 0;
				while(true){
					// fix for a bug, if bitcoin is not shutting down   OR
					if(cnt > 10 										||
					//  auth not initiated  OR			 auth initiated and terminated             											AND
					((auth == null 			|| (auth != null && auth.state() == com.google.common.util.concurrent.Service.State.TERMINATED)) &&
					//			bitcoin is terminated
					bitcoin.state() == com.google.common.util.concurrent.Service.State.TERMINATED)){
						closeProgramAndClosingStage(stageNotif);
					}
					try {
						cnt ++;
						Thread.sleep(1000);
					} catch (InterruptedException e) { e.printStackTrace(); }
				}
			}
			
		}.start();
		
		
    }
    
    @SuppressWarnings("restriction")
	private static void closeProgramAndClosingStage(Stage s){
		Platform.runLater(new Runnable() { 
			  @Override
			  public void run() {
				  s.close();
			  }
		 });
		Runtime.getRuntime().exit(0);
	}
    
    @SuppressWarnings("restriction")
	private static void hockCloseEvent(Stage stage) {
    	stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
    		@SuppressWarnings("static-access")
			@Override
    		public void handle(WindowEvent e) {
    			if(Authenticator.getWalletOperation().getPendingRequestSize() > 0 || Authenticator.getQueuePendingOperations() > 0)
	    			BADialog.confirm(Main.class, 
	    					"Pending Requests/ Operations", 
	    					"Exiting now will cancell all pending requests and operations.\nDo you want to continue?",
	    					new BADialogResponseListner() {
	
								@Override
								public void onResponse(BADialogResponse response,String input) {
									if(response == BADialogResponse.Yes)
										handleStopRequest();
								}
	    				
	    			}).show();
    			else
    				handleStopRequest();
    			
    		}
    	});
    }

    private static void startAuthenticator(BAApplicationParameters AppParams) {
    	auth = new Authenticator(bitcoin.wallet(), bitcoin.peerGroup(), AppParams);
    	auth.setTCPListenerDataBinder(new TCPListener().new DataBinderAdapter(){
    		@Override
    		public BAPassword getWalletPassword() {
    			return Main.UI_ONLY_WALLET_PW;
    		}
    	});
    	auth.setOperationsLongLivingListener(new OperationListenerAdapter() {
    		@SuppressWarnings("restriction")
			@Override
    		public void onError(BAOperation operation, Exception e, Throwable t) {
    			Platform.runLater(new Runnable() { 
    				  @Override
    				  public void run() {
    					  informationalAlert("Error occured in recent wallet operation",
              					e != null? e.toString():t.toString());
    				  }
    			 });
    		}
    	});
    	
    	/*
    	 * Start bitcoin and authenticator
    	 */
    	controller.onAuthenticatorSetup();
    	auth.startAsync();
    	
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
            
            // could cause exception on multiple overlays
            try{
            	blurIn(mainUI);
            }
            catch(Exception e){ }
            
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
    
    private static class CouldNotIinitializeWalletException extends RuntimeException {
    	public CouldNotIinitializeWalletException(String msg) {
    		super(msg);
    	}
    }

    @SuppressWarnings("restriction")
	public static void main(String[] args) {
        launch(args);
    }
    
    public static void versionCheck(){
    	try {
			EncodingUtils.readFromUrl("https://www.bitcoinauthenticator.org/version.json", new AsyncCompletionHandler<Response>(){
				@Override
				public Response onCompleted(Response arg0) throws Exception {
					try {
						String res = arg0.getResponseBody();
						JSONObject json = new JSONObject(res);
						String version = json.getString("current");
						if (!version.equals("0.1.0")){
							 informationalAlert("A new version is available!",
									   "You can download it at bitcoinauthenticator.org");
						}
					   	
					}
					catch(Exception e) {
					}
				    
					return null;
				}
			});
		}
		catch(IOException e) {
		}
    }
    
}
