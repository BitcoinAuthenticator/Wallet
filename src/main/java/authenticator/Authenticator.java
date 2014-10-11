package authenticator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import javafx.scene.image.Image;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.DeterministicKey;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;

import authenticator.Utils.SafeList;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.listeners.BAGeneralEventsListener;
import authenticator.listeners.BAGeneralEventsListener.AccountModificationType;
import authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import authenticator.listeners.BAGeneralEventsListener.PendingRequestUpdateType;
import authenticator.network.BANetworkInfo;
import authenticator.network.TCPListener;
import authenticator.network.TCPListener.TCPListenerExecutionDataBinder;
import authenticator.operations.BAOperation;
import authenticator.operations.OperationsFactory;
import authenticator.operations.OperationsUtils.SignProtocol;
import authenticator.operations.listeners.OperationListener;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.walletCore.WalletOperation;
import authenticator.walletCore.exceptions.CannotGetPendingRequestsException;
import authenticator.walletCore.exceptions.CannotRemovePendingRequestException;

/**
 * <p>The main building block of the BitocinAuthenticator wallet.<br>
 * Covers every aspect of the operations and the only object that should be accessed from the UI.<br></p>
 * <b>Main components are:</b> 
 * <ol><li>TCPListener - basically a thread that polls operations that require communication with the Authenticator app.</li>
 * <li>OnAuthenticatoGUIUpdateListener - a general pupose UI listener.</li>
 * <li>operationsQueue - all operations regarding communication with the Authenticators are added to this queue and executed by the
 * 	  TCPListener.</li>
 * <li>{@link authenticator.walletCore.WalletOperation}</li>
 * <li>{@link authenticator.protobuf.ProtoConfig.ActiveAccountType} - Current active account. Will effect operations that depend on the active account</li>
 * <li>{@link authenticator.listeners.BAGeneralEventsListener} - General events listener for the authenticatro. For Example: a new paired Authenticator was added</li>
 * </ol>
 * <br>
 * @author alon
 *
 */
public class Authenticator extends BASE{
	private static TCPListener mTCPListener;
	private static WalletOperation mWalletOperation;
	private static BAApplicationParameters mApplicationParams;
	
	// Listeners
	private static List<BAGeneralEventsListener> generalEventsListeners;

	public Authenticator(){ 
		super(Authenticator.class);
	}
	
	/**
	 * Instantiate without bitcoinj's wallet 
	 * 
	 * @param wallet
	 * @param appParams
	 */
	public Authenticator(DeterministicKey mpubkey, BAApplicationParameters appParams){
		super(Authenticator.class);
		init(appParams);
		if(mWalletOperation == null){
			mWalletOperation = new WalletOperation(appParams);
			
			try {
				initPendingRequests();
			} catch (AccountWasNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		init2();
	}
	
	/**
	 * Full instantiation
	 * 
	 * @param wallet
	 * @param peerGroup
	 * @param appParams
	 * @throws IOException
	 * @throws AccountWasNotFoundException 
	 */
	public Authenticator(Wallet wallet, PeerGroup peerGroup, BAApplicationParameters appParams) {
		super(Authenticator.class);
		try {
			init(appParams);
			if(mWalletOperation == null){
				mWalletOperation = new WalletOperation(wallet,peerGroup,appParams);
				
				initPendingRequests();
			}
			
			init2();	
		}
		catch (Exception e) { 
			e.printStackTrace(); 
			throw new RuntimeException("Could not instantiate Authenticator");
		}
			
	}
	
	private void init(BAApplicationParameters appParams){
		if(mApplicationParams == null){
			mApplicationParams = appParams;
		}
		if(generalEventsListeners == null)
			generalEventsListeners = new ArrayList<BAGeneralEventsListener>();
		
		new OperationsFactory(); // to instantiate various things			
	}
	
	private void init2(){
		if(mTCPListener == null)
			mTCPListener = new TCPListener(getWalletOperation(), 
					mApplicationParams.getIsManuallyPortForwarded(),
					new String[]{Integer.toString(getApplicationParams().getNetworkPort())});		
	}
	
	public static void disposeOfAuthenticator(){
		mWalletOperation.dispose();
		mWalletOperation = null;
		mApplicationParams = null;
		generalEventsListeners = null;
		mTCPListener = null;
	}
	
	//#####################################
	//
	//		TCPListener Control
	//
	//#####################################
	
	/**
	 * Add operation to the operation queue. The wallet will execute the operation asynchronously
	 * 
	 * @param operation
	 */
	public static boolean addOperation(BAOperation operation)
	{
		return mTCPListener.addOperation(operation);
	}
	
	public static boolean checkForOperationNetworkRequirements(BAOperation operation){
		return mTCPListener.checkForOperationNetworkRequirements(operation);
	}
	
	public static int getQueuePendingOperations(){
		return mTCPListener.getQueuePendingOperations();
	}
	
	public static boolean areAllNetworkRequirementsAreFullyRunning(){
		return mTCPListener.areAllNetworkRequirementsAreFullyRunning();
	}
	
	/**
	 * see {@link authenticator.network.TCPListener#longLivingOperationsListener TCPListener#longLivingOperationsListener}
	 * @param listener
	 */
	public void setOperationsLongLivingListener(OperationListener listener) {
		mTCPListener.setOperationListener(listener);
	}
	
	//#####################################
	//
	//		Pending Requests Control
	//
	//#####################################
	
	@SuppressWarnings("static-access")
	public static void initPendingRequests() throws AccountWasNotFoundException {
		List<PendingRequest> pending = new ArrayList<PendingRequest>();
		String pendingStr = "No pending requests in wallet";
		try {
			pending = getWalletOperation().getPendingRequests();
			if(pending.size() > 0)
				pendingStr = "";
		} catch (CannotGetPendingRequestsException e) { e.printStackTrace(); }
		for(PendingRequest pr:pending){
			//addPendingRequestToList(pr);
			pendingStr += "Pending Request: " + getWalletOperation().pendingRequestToString(pr) + "\n";
		}
		
		pendingStr = "\n\n\n\n\n" + pendingStr + "\n\n\n\n\n";
		
		System.out.println(pendingStr);
	}
	
	//#####################################
	//
	//		Getters & Setter
	//
	//#####################################
	/**
	 * Get the Authenticator instance of the {@link authenticator.walletCore.WalletOperation} object.<br>
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
	
	public static TCPListener Net(){
		return mTCPListener;
	}
	
	public void setTCPListenerDataBinder(TCPListenerExecutionDataBinder binder){
		mTCPListener.setExecutionDataBinder(binder);
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
		try { 
			mTCPListener.startAsync();
			mTCPListener.addListener(new Service.Listener() {
				@Override public void running() {
					assert(mTCPListener.isRunning() == true);
					finishStartup();
		         }
			}, MoreExecutors.sameThreadExecutor());
		} 
		catch (Exception e) { e.printStackTrace(); }
	}

	private void finishStartup(){
		notifyStarted();
	}
	
	@Override
	protected void doStop() {
		if(mTCPListener.isRunning()){ // in case the tcp crashed 
			mTCPListener.stopAsync();
			mTCPListener.addListener(new Service.Listener() {
				@Override public void terminated(State from) {
					LOG.info("Authenticator Stopped");
					notifyStopped();
		         }
			}, MoreExecutors.sameThreadExecutor());	
		}
		else{
			LOG.info("Authenticator Stopped");
			notifyStopped();
		}
			
	}

	//#####################################
	//
	//		General Events Listener
	//
	//#####################################
	
	public static void addGeneralEventsListener(BAGeneralEventsListener listener){
		generalEventsListeners.add(listener);
	}
	
	public static void fireOnAccountsModified(AccountModificationType type, int accountIndex){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onAccountsModified(type, accountIndex);
	}
	
	public static void fireonNewOneNameIdentitySelection(ConfigOneNameProfile profile, @Nullable Image profileImage){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onNewOneNameIdentitySelection(profile, profileImage);
	}
	
	public static void fireOnBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onBalanceChanged(tx, howBalanceChanged, confidence);
	}
	
	public static void fireOnAuthenticatorSigningResponse(Transaction tx, 
			String pairingID, 
			PendingRequest pendingReq, 
			SignProtocol.AuthenticatorAnswerType answerType,
			String str){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onAuthenticatorSigningResponse(tx, pairingID, pendingReq, answerType, str);
	}
	
	public static void fireOnAddressMarkedAsUsed(ATAddress address){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onAddressMarkedAsUsed(address);
	}
	
	public static void fireOnBlockchainDownloadChange(float progress){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onBlockchainDownloadChange(progress);
	}
	
	public static void fireOnWalletSettingsChange(){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onWalletSettingsChange();
	}
	
	public static void fireOnAuthenticatorNetworkStatusChange(BANetworkInfo info){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onAuthenticatorNetworkStatusChange(info);
	}
	
	public static void fireOnPendingRequestUpdate(List<PendingRequest> request, PendingRequestUpdateType updateType){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onPendingRequestUpdate(request, updateType);
	}
}
