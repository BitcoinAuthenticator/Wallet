package authenticator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.xml.sax.SAXException;

import authenticator.operation.ATOperation;

public class TCPListener extends BASE{
	public static Socket socket;
	private static Thread listenerThread;
	private static UpNp plugnplay;
	private static OnAuthenticatoGUIUpdateListener mListener;
	
	/**
	 * Flags
	 */
	private boolean shouldStopListener;
	private boolean isRunning;
	
	public TCPListener(OnAuthenticatoGUIUpdateListener listener){
		super(TCPListener.class);
		mListener = listener;
		isRunning = false;
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
	    		ServerSocket ss = null;
	    		boolean canStartLoop = true;
	    		try {
					plugnplay.run(new String[]{args[0]});
				} catch (Exception e) {
					canStartLoop = false;
				}
	    		if(canStartLoop)
	    		{
	    			try {
						ss = new ServerSocket (port);
						ss.setSoTimeout(5000);
					} catch (IOException e) {
						try { plugnplay.removeMapping(); } catch (IOException | SAXException e1) { }
						canStartLoop = false;
					}
					
	    		}
				if(canStartLoop)
					try{
						boolean isConnected;
						isRunning = true;
						while(true)
			    	    {
							isConnected = false;
							notifyUiAndLog("Listening on port "+port+"...");
							try{
								socket = ss.accept();
								isConnected = true;
							}
							catch (SocketTimeoutException e){ isConnected = false; }
							if(isConnected){
								notifyUiAndLog("Connected");
								notifyUiAndLog("Processing Incoming Operation ...");
							}
							else{
								notifyUiAndLog("Timed-out, Not Connections");
							}
							
							notifyUiAndLog("Checking For outbound operations...");
							if(Authenticator.operationsQueue.size() > 0)
							{
								while (Authenticator.operationsQueue.size() > 0){
									ATOperation op = Authenticator.operationsQueue.poll();
									notifyUiAndLog("Executing Operation: " + op.getDescription());
									op.run(null, null);
								}
							}
							else
								notifyUiAndLog("No Outbound Operations Found.");
							
							if(shouldStopListener)
								break;
			    	    }
					}
					catch (Exception e1) {
						
						//TODO - notify gui
						LOG.error(e1.toString());
					}
					finally
					{
						isRunning = false;
						try { ss.close(); plugnplay.removeMapping(); } catch (IOException | SAXException e) { } 
						notifyUiAndLog("Listener Stopped");
						synchronized(this) {notify();}
					}
	    	    //TODO - return to main
	    	    //Main.inputCommand();
	    	}
	    };
	    this.listenerThread.start();
	}
	
	public void stop() throws InterruptedException{
		shouldStopListener = true;
		synchronized(this.listenerThread){
			notifyUiAndLog("Stopping Listener ... ");
			this.listenerThread.wait();
		}
		
	}
	
	public boolean isRuning()
	{
		return isRunning ;
	}
	
	public void notifyUiAndLog(String str)
    {
		mListener.simpleTextMessage(str);
		this.LOG.info(str);
    }
}
