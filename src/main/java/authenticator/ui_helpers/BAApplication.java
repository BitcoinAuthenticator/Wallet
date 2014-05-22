package authenticator.ui_helpers;

import java.util.List;
import java.util.Map;

import wallettemplate.Main;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The basic BitcoinAuthenticator Javafx application. Covers functionalities like comnad line params and general app instantiations
 * @author alon
 *
 */
public class BAApplication extends Application{
	public  String APP_NAME = "WalletTemplate";

	public BAApplicationParameters ApplicationParams;
    public NetworkParameters networkParams = MainNetParams.get();
    public WalletAppKit bitcoin;
    
	
    public BAApplication(){ }
    
    /**
     * Must be called on start()
     * @param appName
     */
    public boolean BAInit(String appName){
    	APP_NAME = appName;
    	ApplicationParams = new BAApplicationParameters();
    	return this.InitializeApplicationFlags(getParameters().getNamed(),getParameters().getRaw());
    }
    
    //######################
    //
    //	JavaFX Functions
    //
    //######################
    
	@Override
	public void start(Stage primaryStage) throws Exception {
		
	}

	//######################
    //
    //	Application Params
    //
    //######################
	
	/**
	 * 
	 * @param params
	 * @return True: if should continue with UI and Application load. False: if not, for any reason.. for example if user asked only --help
	 */
	public boolean InitializeApplicationFlags(Map<String, String> params, List<String> raw){
		boolean returnValue = true;
		// Help
		if(params.containsKey("help") || raw.contains("-help") || raw.contains("--help")){
			PrintHelp();
			returnValue = false;
		}
		
		// Network Type
		if(params.containsKey("testnet")){
			boolean value = Boolean.parseBoolean(params.get("testnet"));
			if(value)
				ApplicationParams.setBitcoinNetworkType(NetworkType.TEST_NET);
			else
				ApplicationParams.setBitcoinNetworkType(NetworkType.MAIN_NET);
		}
		else
			ApplicationParams.setBitcoinNetworkType(NetworkType.MAIN_NET);
		
		// Debug Tcp Listener
		if(params.containsKey("debuglistener")){
			boolean value = Boolean.parseBoolean(params.get("debuglistener"));
			ApplicationParams.setShouldPrintTCPListenerInfoToConsole(value);
		}
		else
			ApplicationParams.setShouldPrintTCPListenerInfoToConsole(false);
		
		return returnValue;
	}
	
	private void PrintHelp(){
		String[][] help = {
				{"Parameters Available For This Wallet",""},{"",""},
				
				{"--help","Print Help"},
				{"--testnet","If =true will use testnet parameters, else mainnet parameters"},
				{"--debuglistener","If =true will print tcp listener info. False by default"},
		};
		
		for (String[] kv : help) {
			if(kv[0].length() >0)
		        System.out.println(
		            String.format("%-30s: %10s", kv[0], kv[1])
		        );
			else
				System.out.println("\n");
	    }
	}
	
	public class BAApplicationParameters{
		NetworkType bitcoinNetworkType;
		public NetworkType getBitcoinNetworkType(){ return bitcoinNetworkType; }
		public BAApplicationParameters setBitcoinNetworkType(NetworkType bitcoinNetworkType) {
			this.bitcoinNetworkType = bitcoinNetworkType;
			return this;
		}
		
		boolean shouldPrintTCPListenerInfoToConsole;
		public boolean getShouldPrintTCPListenerInfoToConsole(){ return shouldPrintTCPListenerInfoToConsole; }
		public BAApplicationParameters setShouldPrintTCPListenerInfoToConsole(boolean value) {
			this.shouldPrintTCPListenerInfoToConsole = value;
			return this;
		}
	}
	
	public enum NetworkType{
		MAIN_NET,
		TEST_NET;
	}
}