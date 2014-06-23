package authenticator;

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
import authenticator.network.TCPListener;
import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.ConfigAuthenticatorWallet.PendingRequest;
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
	
	@SuppressWarnings("static-access")
	public static void initPendingRequests(){
		List<PendingRequest> pending = new ArrayList<PendingRequest>();
		try {
			pending = getWalletOperation().getPendingRequests();
		} catch (IOException e) { e.printStackTrace(); }
		for(PendingRequest pr:pending)
			addPendingRequestToList(pr);
	}
	
	@SuppressWarnings("static-access")
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
		List<PairedAuthenticator> all = null;
		try {
			all = this.getWalletOperation().getAllPairingObjectArray();
		} catch (IOException e1) { e1.printStackTrace(); }
		if(all != null)
		for(PairedAuthenticator po: all)
		for(PairedAuthenticator.KeysObject ko: po.getGeneratedKeysList())
		{
			try {
				@SuppressWarnings("static-access")
				boolean isWatched = this.getWalletOperation().isWatchingAddress(ko.getAddress());
				if(!isWatched)
					getWalletOperation().addP2ShAddressToWatch(ko.getAddress());
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
