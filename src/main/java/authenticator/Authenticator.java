package authenticator;

public class Authenticator extends BASE{
	static public TCPListener mTCPListener;
	private static OnAuthenticatoGUIUpdateListener mListener;

	public Authenticator(OnAuthenticatoGUIUpdateListener listener) {
		super(Authenticator.class);
		mTCPListener = new TCPListener(mListener);
	}

	public void start() throws Exception{
		mTCPListener.run(new String[]{"1234"});
	}
	
}
