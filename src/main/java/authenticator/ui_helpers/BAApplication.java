package authenticator.ui_helpers;

import java.util.Map;

import wallettemplate.Main;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;

import javafx.application.Application;
import javafx.stage.Stage;

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
    	return this.InitializeApplicationFlags(getParameters().getNamed());
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
	public boolean InitializeApplicationFlags(Map<String, String> params){
		// Help
		if(params.containsKey("help")){
			PrintHelp();
			return false;
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
		
		return true;
	}
	
	private void PrintHelp(){
		String[][] help = {
				{"Parameters Available For This Wallet",""},{"",""},
				
				{"--help","Print Help"},
				{"--testnet","If =true will use testnet parameters, else mainnet parameters"},
		};
		
		for (String[] kv : help) {
			if(kv[0].length() >0)
		        System.out.println(
		            String.format("%-10s:%10s", kv[0], kv[1])
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
	}
	
	public enum NetworkType{
		MAIN_NET,
		TEST_NET;
	}
}
