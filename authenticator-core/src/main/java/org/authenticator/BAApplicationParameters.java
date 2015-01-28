package org.authenticator;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.math.ec.ECPoint;

import com.vinumeris.updatefx.Crypto;

import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.WalletOperation.BAOperationState;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An Application parameters specific to the wallet object.<br>
 * Is initialized when wallet is launched.
 * 
 * @author alon
 *
 */
public class BAApplicationParameters{
	String APP_NAME = "AuthenticatorWallet";
	
	/*
	 * Remote update vars
	 */
	final static public int APP_VERSION 	= 2;
	private String remoteUpdateBaseURL 		= "http://www.bitcoinauthenticator.org/updates/";
	private String remoteUpdateUserAgent 	= "AuthenticatorWallet/" + APP_VERSION;
	private List<ECPoint> remoteUpdateKeys 	= Crypto.decode("03D862C94F031037DF0AE56603092990D623BAAB0811953134569ACD5AC7CFBAB2");
	
	/*
	 * flags
	 */
	boolean isTestMode = false;
	boolean shouldLaunchProgram = false;	
	/** 
	 * ###########################################<br>
	 * a temporary flag to disable spinners because they crash on some platforms<br>
	 * ############################################
	 */
	public boolean disableSpinners = false;
	
	/*
	 * Network
	 */
	int NETWORK_PORT = 8222;
	NetworkType bitcoinNetworkType;
	
	/*
	 * Data folder
	 */
	OS_TYPE osType;
	String APPLICATION_DATA_FOLDER ;
	
	/*
	 * Global vars
	 */
	private BAOperationState operationalState = BAOperationState.NOT_SYNCED;
	public BAApplicationParameters() {}
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
		Map<String, String> paramsFinal = null;
		{
			/*
			 * generate params map from both inputs
			 */
			if(params != null)
				paramsFinal = params;
			else
				paramsFinal = new HashMap<String, String>();
			
			if(raw !=null) {
				for(String s:raw) {
					String[] divided = s.split("=");
					String key = divided[0].replaceAll("-", "");
					if(!paramsFinal.containsKey(key))
						paramsFinal.put(key, divided[1]);
				}
			}
		}
		
		// #####################################
		//
		//	a temporary flag to disable spinners because they crash on some platforms
		//
		// #####################################
		if(paramsFinal.containsKey("disablespinners")){
			boolean value = Boolean.parseBoolean(paramsFinal.get("disablespinners"));
			disableSpinners = value;
		}
		
		// Help
		if(paramsFinal.containsKey("help")){
			PrintHelp();
			returnValue = false;
		}
		
		// test mode 
		if(paramsFinal.containsKey("testermode")){
			boolean value = Boolean.parseBoolean(paramsFinal.get("testermode"));
			setIsTestMode(value);
		}
		else
			setIsTestMode(false);
		
		// Network Type
		if(paramsFinal.containsKey("testnet")){
			boolean value = Boolean.parseBoolean(paramsFinal.get("testnet"));
			if(value)
				setBitcoinNetworkType(NetworkType.TEST_NET);
			else
				setBitcoinNetworkType(NetworkType.MAIN_NET);
		}
		else
			setBitcoinNetworkType(NetworkType.MAIN_NET);
				
		// Port
		if(paramsFinal.containsKey("port")){
			int value = Integer.parseInt(paramsFinal.get("port"));
			setNetworkPort(value);
		}
		else
			;// keep default
			
		//remote update base URL
		if(paramsFinal.containsKey("remoteUpdateBaseURL")){
			setRemoteUpdateBaseURL(paramsFinal.get("remoteUpdateBaseURL"));
		}
		else
			; // keep default
		
		//remote update user agent
		if(paramsFinal.containsKey("remoteUpdateUserAgent")){
			setRemoteUpdateUserAgent(paramsFinal.get("remoteUpdateUserAgent"));
		}
		else
			; // keep default
		
		//remote update keys
		if(paramsFinal.containsKey("remoteUpdateKeys")){
			String r = paramsFinal.get("remoteUpdateKeys");
			List<String> rl = Arrays.asList(r.split(","));
			List<ECPoint> keys = new ArrayList<ECPoint>();
			for(String p: rl) {
				List<ECPoint> tmp = Crypto.decode(p);
				keys.add(tmp.get(0));
			}
			setRemoteUpdateKeys(keys);
		}
		else
			; // keep default
		
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
			tmp = System.getenv("APPDATA") + File.separator;
			break;
		case LINUX:
			tmp += File.separator;
			break;
		case OSX:
			tmp += File.separator + "Library" + File.separator + "Application Support" + File.separator + "";
			break;
		}
		
		setApplicationDataFolderPath(tmp);
		setAllNecessaryApplicationDataSubFolders();
		
		
		/*
		 * Settings from file
		 */
		WalletOperation wo 				= new WalletOperation(this);
		shouldConnectWithTOR 			= wo.getIsUsingTORFromSettings();
		shouldConnectToLocalHost		= wo.getIsConnectingToLocalHostFromSettings();
		shouldConnectToTrustedPeer 		= wo.getIsConnectingToTrustedPeerFromSettings();
		isManuallyPortForwarded 		= wo.getIsPortForwarding();
		if(shouldConnectToTrustedPeer)
			trustedPeerIP 				= wo.getTrustedPeerIPFromSettings();
		bloomFilterFalsePositiveRate 	= wo.getBloomFilterFalsePositiveRateFromSettings();
		wo.dispose();
		
		shouldLaunchProgram = returnValue;
	}
	
	private void PrintHelp(){
		System.out.println(helpString());
	}
	
	private String helpString() {
		String[][] help = {
				{"Parameters Available For This Wallet",""},{"",""},
				
				{"--help","Print Help"},
				{"--testnet"				,"If =true will use testnet parameters, else mainnet parameters"},
				{"--testermode"				,"Testing mode, if true will not send bitcoins. False by default"},
				{"--port"					,"Port Number, default is 8222"},
				{"--remoteUpdateBaseURL"	,"Base URL for remote updates"},
				{"--remoteUpdateUserAgent"	,"User agent for remote updates"},
				{"--remoteUpdateKeys"		,"Validation keys for remote updates (e.g. --remoteUpdateKeys=k1,k2,k3..."}
		};
		
		String ret = "";
		for (String[] kv : help) {
			if(kv[0].length() >0)
				ret += String.format("%-30s: %10s\n", kv[0], kv[1]);
			else	
				ret += "\n";
	    }
		
		return ret;
	}
	
	/**
	 * Returns a readable user friendly app version based on the current {@link BAApplicationParameters#APP_VERSION app code version}
	 * @return
	 */
	public String getFriendlyAppVersion() {
		if(APP_VERSION <= 10)
			return "V0.1." + APP_VERSION;
		else
			return "XXX";
	}
	
	public boolean getDisableSpinners(){
		return disableSpinners;
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
	
	public String getAppName(){
		return APP_NAME;
	}
	public void setAppName(String name){
		APP_NAME = name;
	}
	
	public int getNetworkPort(){ return NETWORK_PORT; }
	public BAApplicationParameters setNetworkPort(int value) {
		this.NETWORK_PORT = value;
		return this;
	}
	
	public boolean getShouldLaunchProgram(){ return shouldLaunchProgram; }
	
	/**
	 * Without the application folder itself, just the path
	 * @param value
	 */
	public String getApplicationDataFolderPath(){
		return this.APPLICATION_DATA_FOLDER;
	}
	/**
	 * The full application's data folder 
	 * With an path seperator at the end
	 * @return
	 */
	public String getApplicationDataFolderAbsolutePath(){
		return this.getApplicationDataFolderPath() + APP_NAME + File.separator;
	}
	/**
	 * Without the folder itself
	 * @param value
	 */
	public void setApplicationDataFolderPath(String value){
		APPLICATION_DATA_FOLDER = value;
	}
	public void setAllNecessaryApplicationDataSubFolders() {
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
		
		// check updates exists
		File f3 = new File(getApplicationDataFolderAbsolutePath() + "updates");
		if (!(f3.exists() && f3.isDirectory())) {
		   f3.mkdir();
		}
	}
	
	public BAOperationState getOperationalState() {
		return operationalState;
	}
	
	public void setOperationalState(BAOperationState newState) {
		operationalState = newState;
	}
	
	public String getRemoteUpdateBaseURL() {
		return remoteUpdateBaseURL;
	}
	
	public void setRemoteUpdateBaseURL(String v) {
		remoteUpdateBaseURL = v;
	}
	
	public String getRemoteUpdateUserAgent() {
		return remoteUpdateUserAgent;
	}
	
	public void setRemoteUpdateUserAgent(String v) {
		remoteUpdateUserAgent = v;
	}
	
	public List<ECPoint> getRemoteUpdateKeys() {
		return remoteUpdateKeys;
	}
	
	public void setRemoteUpdateKeys(List<ECPoint> keys) {
		remoteUpdateKeys = keys;
	}
	
	/*
	 * 
	 * The methods below are used only on initial wallet setup to retrieve settings from the 
	 * config file. After the wallet is started those methods will not be available, the variables should
	 * be retrieved from WalletOperation
	 * 
	 */
	boolean shouldConnectWithTOR = true;
	boolean shouldConnectToLocalHost = false;
	double  bloomFilterFalsePositiveRate = 0.00001;
	boolean shouldConnectToTrustedPeer = false;
	boolean isManuallyPortForwarded = false;
	String trustedPeerIP = null;
	
	public boolean getShouldConnectWithTOR() {
		checkState(getOperationalState() != BAOperationState.NOT_SYNCED,"Please retreive variable through WalletOperation");
		return this.shouldConnectWithTOR;
	}
	
	public boolean getShouldConnectToLocalHost() {
		checkState(getOperationalState() != BAOperationState.NOT_SYNCED,"Please retreive variable through WalletOperation");
		return this.shouldConnectToLocalHost;
	}
	
	public boolean getShouldConnectToTrustedPeer() {
		checkState(getOperationalState() != BAOperationState.NOT_SYNCED,"Please retreive variable through WalletOperation");
		return this.shouldConnectToTrustedPeer;
	}
	
	public boolean getIsManuallyPortForwarded() {
		checkState(getOperationalState() != BAOperationState.NOT_SYNCED,"Please retreive variable through WalletOperation");
		return isManuallyPortForwarded;
	}
	
	public String getTrustedPeer() {
		checkState(getOperationalState() != BAOperationState.NOT_SYNCED,"Please retreive variable through WalletOperation");
		checkState(getShouldConnectToTrustedPeer() == true, "Not connecting through trusted peer");
		return this.trustedPeerIP;
	}
	
	public double getBloomFilterFalsePositiveRate() {
		checkState(getOperationalState() != BAOperationState.NOT_SYNCED,"Please retreive variable through WalletOperation");
		return this.bloomFilterFalsePositiveRate;
	}
	
	/*
	 * general
	 */
	@Override
	public String toString() {
		return "Help:" + "\n" + helpString() + "\n" +
			   "Wallet Data Folder Path: " + getApplicationDataFolderAbsolutePath() + "\n" +
			   "Application Parameter values: \n" + 
			   String.format("%-30s: %10s\n", "testermode", (isTestMode? "True":"False")) + 
			   String.format("%-30s: %10s\n","testnet", (bitcoinNetworkType == NetworkType.TEST_NET? "True":"False")) +
			   String.format("%-30s: %10s\n", "portforwarding", (isManuallyPortForwarded? "True":"False")) +
			   String.format("%-30s: %10s\n", "port", NETWORK_PORT) +
			   "\nApplication Defaults:\n"			+
			   String.format("%-30s: %10s\n", "TOR", (shouldConnectWithTOR? "True":"False")) +
			   String.format("%-30s: %10s\n", "Connect to localhost", (shouldConnectToLocalHost? "True":"False")) +
			   String.format("%-30s: %10s\n", "Connect to trusted peer", (shouldConnectToTrustedPeer? "True":"False") + ", IP: " + trustedPeerIP) +
			   String.format("%-30s: %10s\n", "BloomFilter FalsePositive", String.format( "%.6f", bloomFilterFalsePositiveRate ));
	}
	
	/*
	 *	Enums 
	 */
	
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
	    
	    public static NetworkType fromIndex(Integer i){
	    	switch(i)
	    	{
	    	case 0:
	    		return TEST_NET;
			case 1:
	    		return MAIN_NET;
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