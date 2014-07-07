
package authenticator.ui_helpers;

import java.util.List;
import java.util.Map;

import wallettemplate.Main;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * The basic BitcoinAuthenticator Javafx application. Covers functionalities like command line params and general app instantiations
 * @author alon
 *
 */
public class BAApplication extends Application{
	public static String APP_NAME = "WalletTemplate";

	public static BAApplicationParameters ApplicationParams;
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
		
		//App Name
		if(ApplicationParams.getBitcoinNetworkType() == NetworkType.TEST_NET){
			ApplicationParams.setAppName(APP_NAME + "_TestNet");
			APP_NAME += "_TestNet";
		}
		else
			ApplicationParams.setAppName(APP_NAME);
		
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
	
	/**
	 * An Application parameters specific to the wallet object.<br>
	 * Is initialized when wallet is launched.
	 * 
	 * @author alon
	 *
	 */
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
		
		String APP_NAME = "";
		public String getAppName(){
			return APP_NAME;
		}
		public void setAppName(String name){
			APP_NAME = name;
		}
	}
	
	public enum NetworkType{
		TEST_NET (0),
		MAIN_NET (1);
		
		private int value;
        NetworkType(int value) {
                this.value = value;
        }
        public int getValue() { return this.value; }
        
        public static NetworkType fromString(String s){
        	switch(s)
        	{
        	case "1":
        		return MAIN_NET;
			case "MAIN_NET":
        		return MAIN_NET;
			case "0":
				return TEST_NET;
			case "TEST_NET":
				return TEST_NET;
			default:
				return MAIN_NET;
        	}
        }
	}
}
