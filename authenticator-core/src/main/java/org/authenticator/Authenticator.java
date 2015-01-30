package org.authenticator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javafx.scene.image.Image;
import org.authenticator.listeners.BAWalletExecutionDataBinder;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.DeterministicKey;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.listeners.BAGeneralEventsListener;
import org.authenticator.listeners.BAGeneralEventsListener.AccountModificationType;
import org.authenticator.listeners.BAGeneralEventsListener.HowBalanceChanged;
import org.authenticator.listeners.BAGeneralEventsListener.PendingRequestUpdateType;
import org.authenticator.network.BANetworkInfo;
import org.authenticator.network.TCPListener;
import org.authenticator.operations.BAOperation;
import org.authenticator.operations.OperationsFactory;
import org.authenticator.operations.operationsUtils.SignProtocol;
import org.authenticator.operations.listeners.OperationListener;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;
import org.authenticator.walletCore.WalletOperation;
import org.authenticator.walletCore.exceptions.CannotGetPendingRequestsException;

/**
 * <p>The main building block of the BitocinAuthenticator wallet.<br>
 * Covers every aspect of the operations and the only object that should be accessed from the UI.<br></p>
 * <b>Main components are:</b> 
 * <ol><li>TCPListener - basically a thread that polls operations that require communication with the Authenticator app.</li>
 * <li>Long living Operation Listener </li>
 * <li>Long living data binder</li>
 * <li>{@link org.authenticator.walletCore.WalletOperation}</li>
 * <li>{@link org.authenticator.listeners.BAGeneralEventsListener} - General events listener for the authenticatro. For Example: a new paired Authenticator was added</li>
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
	private static OperationListener longLivingOperationsListener;

	// Data binder
	private static BAWalletExecutionDataBinder longLivingDataBinder;

	public Authenticator(){ 
		super(Authenticator.class);
	}
	
	public Authenticator(BAApplicationParameters appParams) {
		super(Authenticator.class);
		init(appParams);
	}

	/**
	 * Instantiate without bitcoinj's wallet
	 *
	 * @param mpubkey
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
		if(mWalletOperation != null)
			mWalletOperation.dispose();
		mWalletOperation = null;
		mApplicationParams = null;
		generalEventsListeners = null;
		mTCPListener = null;
	}
	
	public Authenticator getInstance() {
		return this;
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
	 * Get the Authenticator instance of the {@link org.authenticator.walletCore.WalletOperation} object.<br>
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
	 * Get {@link org.authenticator.BAApplicationParameters BAApplicationParameters} object.<br>
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

	/**
	 * See {@link org.authenticator.listeners.BAWalletExecutionDataBinder}
	 * @param binder
	 */
	public static void setLongLivingDataBinder(BAWalletExecutionDataBinder binder){
		longLivingDataBinder = binder;
		mTCPListener.setExecutionDataBinder(longLivingDataBinder);
	}

	public static BAWalletExecutionDataBinder getLongLivingDataBinder() { return longLivingDataBinder; }

	/**
	 * Will be used for non user initiated operations, e.g., authenticator respose for Tx signing.
	 *
	 * @param listener
	 */
	public static void setLongLivingOperationsListener(OperationListener listener) {
		longLivingOperationsListener = listener;
		mTCPListener.setLongLivingOperationsListener(longLivingOperationsListener);
	}

	public static OperationListener getLongLivingOperationsListener() { return longLivingOperationsListener; }
	
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

	/**
	 * will search the specific listener by its identity hash code, returns null if not found
	 *
	 * @param listener
	 * @return
	 */
	public static BAGeneralEventsListener getGeneralListenerByIdentityHashCode(BAGeneralEventsListener listener) {
		int target = System.identityHashCode(listener);
		for(BAGeneralEventsListener l: generalEventsListeners) {
			int hashCode = System.identityHashCode(l);
			if(hashCode == target)
				return l;
		}
		return null;
	}
	
	public static void removeGeneralListener(BAGeneralEventsListener listener) {
		generalEventsListeners.remove(listener);
	}
	
	public static void fireOnAccountsModified(AccountModificationType type, int accountIndex){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onAccountsModified(type, accountIndex);
	}
	
	public static void fireonOneNameIdentityChanged(@Nullable ConfigOneNameProfile profile, @Nullable Image profileImage){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onOneNameIdentityChanged(profile, profileImage);
	}
	
	public static void fireOnBalanceChanged(Transaction tx, HowBalanceChanged howBalanceChanged){
		for(BAGeneralEventsListener l:generalEventsListeners)
			l.onBalanceChanged(tx, howBalanceChanged);
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
