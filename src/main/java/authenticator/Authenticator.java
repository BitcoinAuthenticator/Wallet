package authenticator;

import java.util.concurrent.ConcurrentLinkedQueue;

import authenticator.operation.ATOperation;

public class Authenticator extends BASE{
	private static TCPListener mTCPListener;
	private static OnAuthenticatoGUIUpdateListener mListener;
	public static ConcurrentLinkedQueue<ATOperation> operationsQueue;

	public Authenticator(OnAuthenticatoGUIUpdateListener listener) {
		super(Authenticator.class);
		if(mListener == null)
			mListener = listener;
		if(mTCPListener == null)
			mTCPListener = new TCPListener(mListener);
		if(operationsQueue == null)
			operationsQueue = new ConcurrentLinkedQueue<ATOperation>();
	}
	
	//#####################################
	//
	// 		Authenticator Control
	//
	//#####################################

	public void start() throws Exception{
		mTCPListener.run(new String[]{"1234"});
	}
	
	public void stop() throws InterruptedException
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
}
