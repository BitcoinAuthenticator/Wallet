package authenticator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Wallet;
import com.google.common.util.concurrent.AbstractService;

import authenticator.Utils.SafeList;
import authenticator.db.ConfigFile;
import authenticator.network.TCPListener;
import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;

/**
 * <p>The main building block of the BitocinAuthenticator wallet.<br>
 * Covers every aspect of the operations and the only object that should be accessed from the UI.<br></p>
 * <b>Main components are:</b> 
 * <ol><li>TCPListener - basically a thread that polls operations that require communication with the Authenticator app.</li>
 * <li>OnAuthenticatoGUIUpdateListener - a general pupose UI listener.</li>
 * <li>operationsQueue - all operations regarding communication with the Authenticators are added to this queue and executed by the
 * 	  TCPListener.</li>
 * <li>{@link authenticator.WalletOperation}</li>
 * <li>{@link authenticator.protobuf.ProtoConfig.ActiveAccountType} - Current active account. Will effect operations that depend on the active account</li>
 * <li>{@link authenticator.AuthenticatorGeneralEventsListener} - General events listener for the authenticatro. For Example: a new paired Authenticator was added</li>
 * </ol>
 * <br>
 * @author alon
 *
 */
public class Authenticator extends AbstractService{
	final static public int LISTENER_PORT = 1234;
	
	private static TCPListener mTCPListener;
	public static ConcurrentLinkedQueue<ATOperation> operationsQueue;
	//private static SafeList pendingRequests;
	private static WalletOperation mWalletOperation;
	private static BAApplicationParameters mApplicationParams;
	// Listeners
	private static List<AuthenticatorGeneralEventsListener> generalEventsListeners;

	public Authenticator(){ }
	
	/**
	 * Instantiate without bitcoinj's wallet 
	 * 
	 * @param wallet
	 * @param appParams
	 */
	public Authenticator(Wallet wallet, BAApplicationParameters appParams){
		init(appParams);
		if(mWalletOperation == null){
			try {
				mWalletOperation = new WalletOperation(appParams, wallet.getKeyChainSeed());
			} catch (IOException e) { e.printStackTrace(); }
			
			initPendingRequests();
		}
	}
	
	/**
	 * Full instantiation
	 * 
	 * @param wallet
	 * @param peerGroup
	 * @param appParams
	 * @throws IOException
	 */
	public Authenticator(Wallet wallet, PeerGroup peerGroup, BAApplicationParameters appParams) throws IOException
	{
		init(appParams);
		if(mWalletOperation == null){
			try {
				mWalletOperation = new WalletOperation(wallet,peerGroup,appParams, wallet.getKeyChainSeed());
			} catch (IOException e) { e.printStackTrace(); }
			
			initPendingRequests();
		}
		/*if(pendingRequests == null){
			pendingRequests = new SafeList();
			initPendingRequests();
		}*/
		new OperationsFactory(); // to instantiate various things
		//verifyWalletIsWatchingAuthenticatorAddresses();
		//setFirstAccountTEST();
	}
	
	private void init(BAApplicationParameters appParams){
		if(mApplicationParams == null)
			mApplicationParams = appParams;
		if(generalEventsListeners == null)
			generalEventsListeners = new ArrayList<AuthenticatorGeneralEventsListener>();
		if(mTCPListener == null)
			mTCPListener = new TCPListener();
		if(operationsQueue == null)
			operationsQueue = new ConcurrentLinkedQueue<ATOperation>();
	}
	
	public static void disposeOfAuthenticator(){
		mWalletOperation.dispose();
		mWalletOperation = null;
		mApplicationParams = null;
		generalEventsListeners = null;
		mTCPListener = null;
		operationsQueue = null;
	}
	
	//#####################################
	//
	//		Operations Queue Control
	//
	//#####################################
	
	/**
	 * Add operation to the operation queue. The wallet will execute the operation asynchronously
	 * 
	 * @param operation
	 */
	public void addOperation(ATOperation operation)
	{
		if(this.isRunning())
			operationsQueue.add(operation);
	}
	
	//#####################################
	//
	//		Pending Requests Control
	//
	//#####################################
	
	@SuppressWarnings("static-access")
	public static void initPendingRequests(){
		List<PendingRequest> pending = new ArrayList<PendingRequest>();
		String pendingStr = "No pending requests in wallet";
		try {
			pending = getWalletOperation().getPendingRequests();
			if(pending.size() > 0)
				pendingStr = "";
		} catch (IOException e) { e.printStackTrace(); }
		for(PendingRequest pr:pending){
			//addPendingRequestToList(pr);
			pendingStr += "Pending Request: " + getWalletOperation().pendingRequestToString(pr) + "\n";
		}
		
		pendingStr = "\n\n\n\n\n" + pendingStr + "\n\n\n\n\n";
		
		System.out.println(pendingStr);
	}
	
	/*@SuppressWarnings("static-access")
	public static void addPendingRequestToFile(PendingRequest pr) throws FileNotFoundException, IOException {
		getWalletOperation().addPendingRequest(pr);
		pendingRequests.add(pr);
	}
	
	public static void addPendingRequestToList(PendingRequest pr) {
		pendingRequests.add(pr);
	}
	
	public static void removePendingRequest(PendingRequest pr) throws FileNotFoundException, IOException {
		pendingRequests.remove(pr);
		getWalletOperation().removePendingRequest(pr);
	}*/
	
	//#####################################
	//
	//		Getters & Setter
	//
	//#####################################
	/**
	 * Get the Authenticator instance of the {@link authenticator.WalletOperation} object.<br>
	 * Used for all funds management. 
	 * 
	 * @return
	 */
	public static WalletOperation getWalletOperation()
	{
		if(mWalletOperation == null)
			mWalletOperation = new WalletOperation();
		return mWalletOperation;
	}
	
	//#####################################
	//
	//			General
	//
	//#####################################
	
	/**
	 * An init function to verify all P2SH addresses generated between the wallet and the various Authenticators are watched by the bitcoinj engine
	 * 
	 */
	/*private void verifyWalletIsWatchingAuthenticatorAddresses()
	{
		@SuppressWarnings("static-access")
		List<PairedAuthenticator> all = null;
		try {
			all = this.getWalletOperation().getAllPairingObjectArray();
		} catch (Exception e1) { e1.printStackTrace(); }
		if(all != null)
		for(PairedAuthenticator po: all)
		for(PairedAuthenticator.KeysObject ko: po.getGeneratedKeysList())
		{
			try {
				@SuppressWarnings("static-access")
				boolean isWatched = this.getWalletOperation().isWatchingAddress(ko.getAddress());
				if(!isWatched)
					getWalletOperation().addAddressToWatch(ko.getAddress());
			} catch (AddressFormatException e) {
				e.printStackTrace();
			}
		}
	}*/
	
	/**
	 * Does what it says
	 */
	/**
	 * Does what it says
	 */
	@SuppressWarnings("static-access")
	/*private void setFirstAccountTEST()
	{
		
		if(getWalletOperation().getAllAccounts().size() == 0)
		{
			try {
				
				ATAccount b2 = getWalletOperation().generateNewStandardAccount(getApplicationParams().getBitcoinNetworkType(),
						"Default");
				getWalletOperation().setActiveAccount(b2.getIndex());

			} catch (IOException e) { e.printStackTrace(); }
		}	

	}*/
	
	//#####################################
	//
	//		Getters and Setters
	//
	//#####################################
	
	/**
	 * Get {@link authenticator.ui_helpers.BAApplication.BAApplicationParameters} object.<br>
	 * Object is populated when the wallet is luanched.
	 * 
	 * 
	 * @return
	 */
	public static BAApplicationParameters getApplicationParams(){
		return mApplicationParams;
	}
	
	public Authenticator setApplicationParams(BAApplicationParameters params){
		mApplicationParams = params;
		return this;
	}
	
	//#####################################
	//
	//		Service Functions
	//
	//#####################################
	
	@SuppressWarnings("static-access")
	@Override
	protected void doStart() {
		assert(this.getWalletOperation() != null);
		assert(mTCPListener != null);
		assert(mApplicationParams != null);
		assert(operationsQueue != null);
		//assert(pendingRequests != null);
		try { 
			mTCPListener.run(new String[]{Integer.toString(LISTENER_PORT)}); 
			notifyStarted();
		} 
		catch (Exception e) { e.printStackTrace(); }
	}

	@Override
	protected void doStop() {
		try 
		{
			mTCPListener.stop(); 
			notifyStopped();
		} catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	
	//#####################################
	//
	//		General Events Listener
	//
	//#####################################
	
	public static void addGeneralEventsListener(AuthenticatorGeneralEventsListener listener){
		generalEventsListeners.add(listener);
	}
	
	public static void fireOnNewPairedAuthenticator(){
		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onNewPairedAuthenticator();
	}
	
	public static void fireonNewUserNamecoinIdentitySelection(AuthenticatorConfiguration.ConfigOneNameProfile profile){
		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onNewUserNamecoinIdentitySelection(profile);
	}
	
	public static void fireOnFinishedDiscoveringWalletHierarchy(){
		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onFinishedBuildingWalletHierarchy();
	}
	
	public static void fireOnBalanceChanged(int walletID){
		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onBalanceChanged(walletID);
	}
	
	public static void fireOnNewStandardAccountAdded(){
		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onNewStandardAccountAdded();
	}

	public static void fireOnAccountDeleted(int accountIndex){
		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onAccountDeleted(accountIndex);
	}

	public static void fireOnAccountBeenModified(int accountIndex){
		/**
		 * update in case the active account was updated
		 */
		if(getWalletOperation().getActiveAccount().getActiveAccount().getIndex() == accountIndex)
			getWalletOperation().setActiveAccount(accountIndex); // just to update the active account

		for(AuthenticatorGeneralEventsListener l:generalEventsListeners)
			l.onAccountBeenModified(accountIndex);
	}
}
