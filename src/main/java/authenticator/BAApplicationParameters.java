package authenticator;

import java.util.List;
import java.util.Map;

/**
 * An Application parameters specific to the wallet object.<br>
 * Is initialized when wallet is launched.
 * 
 * @author alon
 *
 */
public class BAApplicationParameters{
	NetworkType bitcoinNetworkType;
	public boolean shouldLaunchProgram = false;
	boolean shouldPrintTCPListenerInfoToConsole;
	String APP_NAME = "AuthenticatorWallet";
	
	public BAApplicationParameters(Map<String, String> params, List<String> raw){
		InitializeApplicationFlags(params, raw);
	}
	
	/**
	 * 
	 * @param params
	 * @return True: if should continue with UI and Application load. False: if not, for any reason.. for example if user asked only --help
	 */
	public void InitializeApplicationFlags(Map<String, String> params, List<String> raw){
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
				setBitcoinNetworkType(NetworkType.TEST_NET);
			else
				setBitcoinNetworkType(NetworkType.MAIN_NET);
		}
		else
			setBitcoinNetworkType(NetworkType.MAIN_NET);
		
		// Debug Tcp Listener
		if(params.containsKey("debuglistener")){
			boolean value = Boolean.parseBoolean(params.get("debuglistener"));
			setShouldPrintTCPListenerInfoToConsole(value);
		}
		else
			setShouldPrintTCPListenerInfoToConsole(false);
		
		//App Name
		if(getBitcoinNetworkType() == NetworkType.TEST_NET){
			setAppName(APP_NAME + "_TestNet");
		}
		else
			setAppName(APP_NAME);
		
		shouldLaunchProgram = returnValue;
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
	
	public NetworkType getBitcoinNetworkType(){ 
		return bitcoinNetworkType; 
	}
	
	public BAApplicationParameters setBitcoinNetworkType(NetworkType bitcoinNetworkType) {
		this.bitcoinNetworkType = bitcoinNetworkType;
		return this;
	}
	
	
	public boolean getShouldPrintTCPListenerInfoToConsole(){ return shouldPrintTCPListenerInfoToConsole; }
	public BAApplicationParameters setShouldPrintTCPListenerInfoToConsole(boolean value) {
		this.shouldPrintTCPListenerInfoToConsole = value;
		return this;
	}
	
	public String getAppName(){
		return APP_NAME;
	}
	public void setAppName(String name){
		APP_NAME = name;
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