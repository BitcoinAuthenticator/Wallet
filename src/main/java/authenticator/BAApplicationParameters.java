package authenticator;

import java.io.File;
import java.nio.file.Files;
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
	
	boolean isTestMode = false;
	boolean shouldLaunchProgram = false;
	boolean shouldPrintTCPListenerInfoToConsole;
	boolean isManuallyPortForwarded = false;
	
	int NETWORK_PORT = 1234;
	
	String APP_NAME = "AuthenticatorWallet";
	
	OS_TYPE osType;
	String APPLICATION_DATA_FOLDER ;
	
	public BAApplicationParameters(Map<String, String> params, List<String> raw) throws WrongOperatingSystemException{
		InitializeApplicationFlags(params, raw);
	}
	
	/**
	 * 
	 * @param params
	 * @return True: if should continue with UI and Application load. False: if not, for any reason.. for example if user asked only --help
	 * @throws WrongOperatingSystemException 
	 */
	public void InitializeApplicationFlags(Map<String, String> params, List<String> raw) throws WrongOperatingSystemException{
		boolean returnValue = true;
		// Help
		if(params.containsKey("help") || raw.contains("-help") || raw.contains("--help")){
			PrintHelp();
			returnValue = false;
		}
		
		// test mode 
		if(params.containsKey("testermode")){
			boolean value = Boolean.parseBoolean(params.get("testermode"));
			setIsTestMode(value);
		}
		else
			setIsTestMode(false);
		
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
		
		// Port Forwarding
		if(params.containsKey("portforwarding")){
			boolean value = Boolean.parseBoolean(params.get("portforwarding"));
			setIsManuallyPortForwarded(value);
		}
		else
			setIsManuallyPortForwarded(false);
		
		// Port
		if(params.containsKey("port")){
			int value = Integer.parseInt(params.get("port"));
			setNetworkPort(value);
		}
		else
			setNetworkPort(1234);
		
		//App Name
		if(getBitcoinNetworkType() == NetworkType.TEST_NET){
			setAppName(APP_NAME + "_TestNet");
		}
		else
			setAppName(APP_NAME);
		
		//Application data folder
		osType =  OS_TYPE.operationSystemFromString(System.getProperty("os.name"));
		String tmp = System.getProperty("user.home");
		switch(osType) {
		case WINDOWS:
			tmp += "";
			break;
		case LINUX:
			tmp += "";
			break;
		case OSX:
			tmp += "/Library/Application Support/";
			break;
		}
		
		setApplicationDataFolderPath(tmp);
		setAllNecessaryApplicationDataSubFolders();
		
		shouldLaunchProgram = returnValue;
	}
	
	private void PrintHelp(){
		String[][] help = {
				{"Parameters Available For This Wallet",""},{"",""},
				
				{"--help","Print Help"},
				{"--testnet"			,"If =true will use testnet parameters, else mainnet parameters"},
				{"--debuglistener"		,"If =true will print tcp listener info. False by default"},
				{"--testermode"			,"Testing mode, if true will not send bitcoins. False by default"},
				{"--portforwarding"		,"Manual Port Forwarding mode, if true will not use UPNP to map ports (Port:1234)"},
				{"--port"				,"Port Number, default is 1234"}
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
	
	public boolean getIsTestMode(){ return isTestMode; }
	public BAApplicationParameters setIsTestMode(boolean value) {
		this.isTestMode = value;
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
	
	public boolean getIsManuallyPortForwarded(){ return isManuallyPortForwarded; }
	public BAApplicationParameters setIsManuallyPortForwarded(boolean value) {
		this.isManuallyPortForwarded = value;
		return this;
	}
	
	public int getNetworkPort(){ return NETWORK_PORT; }
	public BAApplicationParameters setNetworkPort(int value) {
		this.NETWORK_PORT = value;
		return this;
	}
	
	public boolean getShouldLaunchProgram(){ return shouldLaunchProgram; }
	
	/**
	 * Without the folder itself
	 * @param value
	 */
	public String getApplicationDataFolderPath(){
		return this.APPLICATION_DATA_FOLDER;
	}
	/**
	 * With an '/'
	 * @return
	 */
	public String getApplicationDataFolderAbsolutePath(){
		return this.getApplicationDataFolderPath() + APP_NAME + "/";
	}
	/**
	 * Without the folder itself
	 * @param value
	 */
	public void setApplicationDataFolderPath(String value){
		APPLICATION_DATA_FOLDER = value;
	}
	private void setAllNecessaryApplicationDataSubFolders() {
		// check the app folder exists 
		File f1 = new File(getApplicationDataFolderAbsolutePath());
		if (!(f1.exists() && f1.isDirectory())) {
		   f1.mkdir();
		}
		
		// check cached_resources exists
		File f2 = new File(getApplicationDataFolderAbsolutePath() + "cached_resources");
		if (!(f2.exists() && f2.isDirectory())) {
		   f2.mkdir();
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
	
	public enum OS_TYPE {
		WINDOWS,
		LINUX,
		OSX;
		
		public static OS_TYPE operationSystemFromString(String OS) throws WrongOperatingSystemException {
			if(isWindows(OS))
				return OS_TYPE.WINDOWS;
			if(isLinux(OS))
				return OS_TYPE.LINUX;
			if(isMac(OS))
				return OS_TYPE.OSX;
			throw new WrongOperatingSystemException();
			
		}
		
		public static boolean isWindows(String OS) {
			 
			return (OS.indexOf("win") >= 0 || OS.indexOf("Win") >= 0);
	 
		}
	 
		public static boolean isMac(String OS) {
	 
			return (OS.indexOf("mac") >= 0 || OS.indexOf("Mac") >= 0);
	 
		}
	 
		public static boolean isLinux(String OS) {
	 
			return (OS.indexOf("linux") >= 0 || OS.indexOf("Linux") >= 0);
	 
		}
	}
	
	public static class WrongOperatingSystemException extends Exception {
		public WrongOperatingSystemException() {
			super();
		}
	}
}