package authenticator;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Wallet;

import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;

public class Authenticator extends BASE{
	final static public int LISTENER_PORT = 1234;
	
	private static TCPListener mTCPListener;
	private static OnAuthenticatoGUIUpdateListener mListener;
	public static ConcurrentLinkedQueue<ATOperation> operationsQueue;
	
	private static WalletOperation mWalletOperation;

	public Authenticator(OnAuthenticatoGUIUpdateListener listener, Wallet wallet, PeerGroup peerGroup) throws IOException
	{
		this(listener);
		this.setWallet(wallet,peerGroup);
	}
	public Authenticator(OnAuthenticatoGUIUpdateListener listener) throws IOException {
		super(Authenticator.class);
		if(mListener == null)
			mListener = listener;
		if(mTCPListener == null)
			mTCPListener = new TCPListener(mListener);
		if(operationsQueue == null)
			operationsQueue = new ConcurrentLinkedQueue<ATOperation>();
		new OperationsFactory(); // to instantiate various things
	}
	
	public Authenticator setWallet(Wallet wallet, PeerGroup peerGroup)
	{
		if(mWalletOperation == null)
			try {
				mWalletOperation = new WalletOperation(wallet,peerGroup);
			} catch (IOException e) { e.printStackTrace(); }
		return this;
	}
	
	//#####################################
	//
	// 		Authenticator Control
	//
	//#####################################

	public void start() throws Exception{
		assert(this.getWalletOperation() != null);
		assert(mListener != null);
		assert(mTCPListener != null);
		mTCPListener.run(new String[]{Integer.toString(LISTENER_PORT)});
	}
	
	static public void stop() throws InterruptedException
	{
		//Wait until stops
		mTCPListener.stop();
	}
	
	public boolean isRunning()
	{
		if(!mTCPListener.isRuning())
			return false;
		return true;
	}
	
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
			LOG.error("Queue is not running, Cannot add operation");
		}
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
}
