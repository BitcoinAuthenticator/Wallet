package org.wallet;

import com.google.common.util.concurrent.Uninterruptibles;
import com.vinumeris.updatefx.AppDirectory;
import com.vinumeris.updatefx.UpdateFX;
import com.vinumeris.updatefx.UpdateSummary;
import com.vinumeris.updatefx.Updater;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.authenticator.Authenticator;
import org.authenticator.BAApplicationParameters;
import org.authenticator.BAApplicationParameters.NetworkType;
import org.authenticator.BAApplicationParameters.WrongOperatingSystemException;
import org.authenticator.Utils.FileUtils;
import org.authenticator.helpers.BAApplication;
import org.authenticator.network.TCPListener;
import org.authenticator.operations.BAOperation;
import org.authenticator.operations.listeners.OperationListenerAdapter;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.utils.BAPassword;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.wallet.RemoteUpdateWindow.RemoteUpdateWindowListener;
import org.wallet.startup.StartupController;
import org.wallet.utils.BaseUI;
import org.wallet.utils.GuiUtils;
import org.wallet.utils.TextFieldValidator;
import org.wallet.utils.dialogs.BADialog;
import org.wallet.utils.dialogs.BADialog.BADialogResponse;
import org.wallet.utils.dialogs.BADialog.BADialogResponseListner;

import javax.annotation.Nullable;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.wallet.utils.GuiUtils.*;

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
    static Properties config;
    
	 /**
	  * In order to make wallet encryption and decryption smoother, we keep
	  * the wallet's password in memory (ONLY !!) so decryption won't prompt an "Enter password" dialog
	  */
	 public static BAPassword UI_ONLY_WALLET_PW;
	 
	 /**
	  * As seen in {@link org.wallet.Main#UI_ONLY_WALLET_PW UI_ONLY_WALLET_PW}, the wallet's lock
	  * is merely a UI thing cause we keep it locked all the time.<br>
	  * This boolean represents the UI's wallet encrypted state
	  */
	 public static boolean UI_ONLY_IS_WALLET_LOCKED = true;

    @SuppressWarnings("restriction")
	private void init(Stage mainWindow) {
    	String os = System.getProperty("os.name").toLowerCase();
    	if(os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
    		File source = new File("/etc/hosts");
            File target = new File(ApplicationParams.getApplicationDataFolderAbsolutePath() + "hosts");
            if(source.exists()){
            	try {Files.copy(source.toPath(), target.toPath());} 
            	catch (IOException e) {e.printStackTrace();}
            	source.delete();
            }
	    }
    	try {
    		// Load the GUI. The Controller class will be automagically created and wired up.
            mainWindow.initStyle(StageStyle.UNDECORATED);
            URL location = getClass().getResource("gui.fxml");
            FXMLLoader loader = new FXMLLoader(location);
    		mainUI = (AnchorPane) loader.load();
            controller = loader.getController();
            // Configure the window with a StackPane so we can overlay things on top of the main UI.
            uiStack = new StackPane(mainUI);
            mainWindow.setTitle(BAApplication.ApplicationParams.getAppName() + " " + BAApplication.ApplicationParams.getFriendlyAppVersion());
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
                root = FXMLLoader.load(Main.class.getResource("startup/walletstartup.fxml"));
                startup = new Stage();
                startup.setTitle("Setup");
                startup.initStyle(StageStyle.UNDECORATED);
                Scene scene1 = new Scene(root, 607, 400);
                final String file1 = TextFieldValidator.class.getResource("GUI.css").toString();
                scene1.getStylesheets().add(file1);  // Add CSS that we need.
                startup.setScene(scene1);
                startup.show();               
            } else {finishLoading();}
        } 
    	catch (Exception e) {
    		e.printStackTrace();
    		throw new CouldNotIinitializeWalletException("Could Not initialize wallet"); 
    	}
    }
    
    public static String[] loadConfigFile() {
        //Load configuration file
        String filename = "wallet.cfg";
        config = new Properties();
        try {config.load(new FileInputStream(filename));} 
        catch (FileNotFoundException ex) {return null;} 
        catch (IOException ex) {return null;}
        Enumeration en = config.keys();
        int i=0;
        String[] args = new String[config.size()];
        while (en.hasMoreElements()) {
        	String key = (String) en.nextElement();
        	args[i]=(String) (key+"="+config.get(key));
            i++;
        }
        return args;
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
        
        String os = System.getProperty("os.name").toLowerCase();
    	if(os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
    		File target = new File("/etc/hosts");
            File source = new File(ApplicationParams.getApplicationDataFolderAbsolutePath() + "hosts");
            if(source.exists()){
            	try {Files.copy(source.toPath(), target.toPath());} 
            	catch (IOException e) {e.printStackTrace();}
            }
	    }
    	
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
    	auth.setLongLivingDataBinder(new TCPListener().new DataBinderAdapter(){
    		@Override
    		public BAPassword getWalletPassword() {
    			return Main.UI_ONLY_WALLET_PW;
    		}
    	});
    	auth.setLongLivingOperationsListener(new OperationListenerAdapter() {
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
    
    public <T> OverlayUI<T> overlayUI(String name) {
    	return overlayUI(name, null);
    }

    /** Loads the FXML file with the given name, blurs out the main UI and puts this one on top. */
    public <T> OverlayUI<T> overlayUI(String name, @Nullable ArrayList<Object> param) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = getClass().getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            
            if(param != null) {
            	BaseUI baseContr = loader.<BaseUI>getController();
            	baseContr.setParams(param);
            	baseContr.updateUIForParams();
            }
                        
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

    public static void main(String[] args) throws IOException, WrongOperatingSystemException {
   	
    	/*
    	 * We create a BAApplicationParameters instance to get the app data folder
    	 */
    	BAApplicationParameters updateFxAppParams = new BAApplicationParameters(null, Arrays.asList(args));
    	
        // We want to store updates in our app dir so must init that here.
        AppDirectory.initAppDir(updateFxAppParams.getAppName());
        AppDirectory.overrideAppDir(Paths.get(updateFxAppParams.getApplicationDataFolderAbsolutePath(), "updates"));
        
        // re-enter at realMain, but possibly running a newer version of the software i.e. after this point the
        // rest of this code may be ignored.
        UpdateFX.bootstrap(Main.class, AppDirectory.dir(), args);
    }

	public static void realMain(String[] args) {
    	String[] argsfromfile = loadConfigFile();
    	if (argsfromfile != null){launch(argsfromfile);}
    	else {launch(args);}
    }
    
	@Override
    public void start(Stage mainWindow) throws IOException, WrongOperatingSystemException {
			
    	/**
    	 * Entry point for the remote update UI.
    	 */
    	
    	// For some reason the JavaFX launch process results in us losing the thread context class loader: reset it.
        Thread.currentThread().setContextClassLoader(Main.class.getClassLoader());
        // Must be done twice for the times when we come here via realMain.
        // We want to store updates in our app dir so must init that here.
        /*
    	 * We create a BAApplicationParameters instance to get the app data folder
    	 */
    	BAApplicationParameters updateFxAppParams = new BAApplicationParameters(null, null);
        // We want to store updates in our app dir so must init that here.
        AppDirectory.initAppDir(updateFxAppParams.getAppName());
        AppDirectory.overrideAppDir(Paths.get(updateFxAppParams.getApplicationDataFolderAbsolutePath(), "updates"));

        ProgressBar indicator = showUpdateDownloadProgressBar();

        Updater updater = new Updater(updateFxAppParams.getRemoteUpdateBaseURL(), updateFxAppParams.getRemoteUpdateUserAgent(), updateFxAppParams.APP_CODE_VERSION,
                AppDirectory.dir(), UpdateFX.findCodePath(Main.class),
                updateFxAppParams.getRemoteUpdateKeys(), 1) {
            @Override
            protected void updateProgress(long workDone, long max) {
                super.updateProgress(workDone, max);
                // Show the splash screen if an update is found.
                downloadUpdatesWindow.setVisible();
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        };

        indicator.progressProperty().bind(updater.progressProperty());

        updater.setOnSucceeded(event -> {
            try {
                UpdateSummary summary = updater.get();
                if (summary.highestVersion > updateFxAppParams.APP_CODE_VERSION) {
                	System.out.println("Restarting the app to load the new version");
                    if (UpdateFX.getVersionPin(AppDirectory.dir()) == 0)
                        UpdateFX.restartApp();
                }else {
                	System.out.println("Loaded best version, starting wallet ...");
                	downloadUpdatesWindow.close();
                	realStart(mainWindow);
                }                
            } catch (Throwable e) {
               e.printStackTrace();
            }
        });
        
        updater.setOnFailed(event -> {
        	System.out.println("Update error: " + updater.getException());
            updater.getException().printStackTrace();
            
            // load the wallet without applying updates
            Platform.runLater(() -> { 
            	downloadUpdatesWindow.setToFailedConnectionMode("Failed To Connect/ download from server");
            	downloadUpdatesWindow.setListener(new RemoteUpdateWindowListener() {
					@Override
					public void UserPressedOk(RemoteUpdateWindow window) {
						realStart(mainWindow);
					}
            	});
            });
        });

        indicator.setOnMouseClicked(ev -> UpdateFX.restartApp());

        new Thread(updater, "UpdateFX Thread").start();        
    }
    
    public void realStart(Stage mainWindow) {
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
    RemoteUpdateWindow downloadUpdatesWindow;
	private ProgressBar showUpdateDownloadProgressBar() {
		downloadUpdatesWindow = new RemoteUpdateWindow(Main.class);
		downloadUpdatesWindow.init();
        return downloadUpdatesWindow.getProgressBar();
    }
}
