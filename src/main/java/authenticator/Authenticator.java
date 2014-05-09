package authenticator;

public class Authenticator extends BASE{
	private static TCPListener mTCPListener;
	private static OnAuthenticatoGUIUpdateListener mListener;
	private 

	public Authenticator(OnAuthenticatoGUIUpdateListener listener) {
		super(Authenticator.class);
		mListener = listener;
		mTCPListener = new TCPListener(mListener);
	}

	public void start() throws Exception{
		mTCPListener.run(new String[]{"1234"});
	}
	
	public void stop() throws InterruptedException
	{
		//Wait until stops
		mTCPListener.stop();
	}
}
