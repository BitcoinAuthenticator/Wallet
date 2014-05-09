package authenticator;

import java.net.ServerSocket;
import java.net.Socket;

public class TCPListener extends BASE{
	public static Socket socket;
	private static Thread listenerThread;
	private static UpNp plugnplay;
	private static OnAuthenticatoGUIUpdateListener mListener;
	private boolean shouldStopListener;
	
	public TCPListener(OnAuthenticatoGUIUpdateListener listener){
		super(TCPListener.class);
		mListener = listener;
	}
	
	public void run(String[] args) throws Exception
	{
		shouldStopListener = false;   
	    this.listenerThread = new Thread(){
	    	@Override
		    public void run() {
	    		if(plugnplay != null)
	    		{
	    			//TODO - notify GUI that cannot run listener because one is already running
	    			return;
	    		}
	    		
	    		plugnplay = new UpNp();
	    		int port = Integer.parseInt(args[0]);
	    		try {
					plugnplay.run(null);
					ServerSocket ss;
					ss = new ServerSocket (port);
					ss.setSoTimeout(5000);
					while(true)
		    	    {
						notifyUiAndLog("Listening on port "+port+"...");
						socket = ss.accept();
						if(ss.isBound()){
							notifyUiAndLog("Connected");
							notifyUiAndLog("Processing Incoming Operation ...");
						}
						
						if(shouldStopListener)
							break;
		    	    }
					ss.close();
					plugnplay.removeMapping();
					notifyUiAndLog("Listener Stopped");
				} catch (Exception e1) {
					//TODO - notify gui
					LOG.error(e1.toString());
				}
	    	    //TODO - return to main
	    	    //Main.inputCommand();
	    	}
	    };
	    this.listenerThread.run();
	}
	
	public void stop(){
		shouldStopListener = true;
		notifyUiAndLog("Stopping Listener ... ");
	}
	
	public void notifyUiAndLog(String str)
    {
		mListener.simpleTextMessage(str);
		this.LOG.info(str);
    }
}
