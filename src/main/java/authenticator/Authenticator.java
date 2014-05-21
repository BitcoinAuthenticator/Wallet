package authenticator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Wallet;

import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.operations.ATOperation;
import authenticator.operations.OperationsFactory;

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
public class Authenticator extends BASE{
	final static public int LISTENER_PORT = 1234;
	
	private static TCPListener mTCPListener;
	private static OnAuthenticatoGUIUpdateListener mListener;
	public static ConcurrentLinkedQueue<ATOperation> operationsQueue;
	private static WalletOperation mWalletOperation;

	public Authenticator(Wallet wallet, PeerGroup peerGroup, OnAuthenticatoGUIUpdateListener listener) throws IOException
	{
		super(Authenticator.class);
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
		
		new OperationsFactory(); // to instantiate various things
		verifyWalletIsWatchingAuthenticatorAddresses();
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
					this.getWalletOperation().addP2ShAddressToWatch(ko.address);
			} catch (AddressFormatException e) {
				e.printStackTrace();
			}
		}
	}
}
