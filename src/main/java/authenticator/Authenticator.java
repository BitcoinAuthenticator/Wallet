package authenticator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

import authenticator.Utils.SafeList;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.db.PendingRequestsFile;
import authenticator.network.PendingRequest;
import authenticator.network.TCPListener;
import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;
import authenticator.ui_helpers.PopUpNotification;
import authenticator.ui_helpers.BAApplication.BAApplicationParameters;

/**
 * <p>The main building block of the BitocinAuthenticator wallet.<br>
 * Covers every aspect of the operations and the only object that should be accessed from the UI.<br></p>
 * <b>Main components are:</b> 
 * <ol><li>TCPListener - basically a thread that polls operations that require communication with the Authenticator app.</li>
 * <li>OnAuthenticatoGUIUpdateListener - a general pupose UI listener.</li>
 * <li>operationsQueue - all operations regarding communication with the Authenticators are added to this queue and executed by the
 * 	  TCPListener.</li>
 * <li>{@link authenticator.WalletOperation}</li>
 * </ol>
 * <br>
 * @author alon
 *
 */
public class Authenticator extends AbstractService{
	final static public int LISTENER_PORT = 1234;
	
	private static TCPListener mTCPListener;
	private static OnAuthenticatoGUIUpdateListener mListener;
	public static ConcurrentLinkedQueue<ATOperation> operationsQueue;
	private static SafeList pendingRequests;
	private static WalletOperation mWalletOperation;
	private static BAApplicationParameters mApplicationParams;

	public Authenticator(){}
	public Authenticator(Wallet wallet, PeerGroup peerGroup, OnAuthenticatoGUIUpdateListener listener) throws IOException
	{
		//super(Authenticator.class);
		if(mListener == null)
			mListener = listener;
		if(mTCPListener == null)
			mTCPListener = new TCPListener(mListener);
		if(operationsQueue == null)
			operationsQueue = new ConcurrentLinkedQueue<ATOperation>();
		if(mWalletOperation == null)
			try {
				mWalletOperation = new WalletOperation(wallet,peerGroup);
			} catch (IOException e) { e.printStackTrace(); }
		if(pendingRequests == null){
			pendingRequests = new SafeList();
			initPendingRequests();
		}
		new OperationsFactory(); // to instantiate various things
		verifyWalletIsWatchingAuthenticatorAddresses();
	}
	
	//#####################################
	//
	// 		Authenticator Control
	//
	//#####################################

	
	//#####################################
	//
	//		Operations Queue Control
	//
	//#####################################
	
	public void addOperation(ATOperation operation)
	{
		if(this.isRunning())
			operationsQueue.add(operation);
		else{
			mListener.simpleTextMessage("Queue is not running, Cannot add operation");
			//LOG.error("Queue is not running, Cannot add operation");
		}
	}
	
	//#####################################
	//
	//		Pending Requests Control
	//
	//#####################################
	
	public static void addPendingRequest(PendingRequest req, boolean writeToFile){
		if(writeToFile){
			PendingRequestsFile file = new PendingRequestsFile();
			file.writeNewPendingRequest(req);
		}
		pendingRequests.add(req);
	}
	
	public static void removePendingRequest(PendingRequest req){
		PendingRequestsFile file = new PendingRequestsFile();
		file.removePendingRequest(req);
		pendingRequests.remove(req);
	}
	
	public static int getPendingRequestSize(){
		return pendingRequests.size();
	}
	
	public static ArrayList<Object> getPendingRequests(){
		return pendingRequests.getAll();
	}
	
	public static void initPendingRequests(){
		PendingRequestsFile file = new PendingRequestsFile();
		ArrayList<PendingRequest> pending = file.getPendingRequests();
		for(PendingRequest pr:pending)
			addPendingRequest(pr,false);
	}
	
	//#####################################
	//
	//		Getters & Setter
	//
	//#####################################
	public static WalletOperation getWalletOperation()
	{
		return mWalletOperation;
	}
	
	//#####################################
	//
	//			General
	//
	//#####################################
	private void verifyWalletIsWatchingAuthenticatorAddresses()
	{
		@SuppressWarnings("static-access")
		ArrayList<PairingObject> all = this.getWalletOperation().getAllPairingObjectArray();
		if(all != null)
		for(PairingObject po: all)
		for(KeyObject ko: po.keys.keys)
		{
			try {
				@SuppressWarnings("static-access")
				boolean isWatched = this.getWalletOperation().isWatchingAddress(ko.address);
				if(!isWatched)
					getWalletOperation().addP2ShAddressToWatch(ko.address);
			} catch (AddressFormatException e) {
				e.printStackTrace();
			}
		}
	}
	
	//#####################################
	//
	//		Getters and Setters
	//
	//#####################################
	
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
		assert(mListener != null);
		assert(mTCPListener != null);
		assert(mApplicationParams != null);
		assert(operationsQueue != null);
		assert(pendingRequests != null);
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
	
	
	
	
	

	/*@Override
	public ListenableFuture<State> start() {
		
		return null;
	}

	@Override
	public State startAndWait() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Service startAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State state() {
		if(!mTCPListener.isRuning())
			return State.TERMINATED;
		return State.RUNNING;
	}

	@Override
	public ListenableFuture<State> stop() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State stopAndWait() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public Service stopAsync() {
		try { mTCPListener.stop(); } catch (InterruptedException e) { e.printStackTrace(); }
		return null;
	}

	@Override
	public void awaitRunning() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void awaitRunning(long timeout, TimeUnit unit)
			throws TimeoutException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void awaitTerminated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void awaitTerminated(long timeout, TimeUnit unit)
			throws TimeoutException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Throwable failureCause() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addListener(Listener listener, Executor executor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRunning() {
		if(!mTCPListener.isRuning())
			return false;
		return false;
	}*/
}
