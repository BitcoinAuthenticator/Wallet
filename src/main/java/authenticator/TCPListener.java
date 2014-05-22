package authenticator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.xml.sax.SAXException;

import authenticator.operations.ATOperation;

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
	
	public void run(final String[] args) throws Exception
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
							//notifyUiAndLog("Listening on port "+port+"...");
							try{
								socket = ss.accept();
								isConnected = true;
							}
							catch (SocketTimeoutException e){ isConnected = false; }
							if(isConnected){
								logAsInfo("Connected");
								logAsInfo("Processing Incoming Operation ...");
							}
							else{
								//notifyUiAndLog("Timed-out, Not Connections");
							}
							
							logAsInfo("Checking For outbound operations...");
							if(Authenticator.operationsQueue.size() > 0)
							{
								while (Authenticator.operationsQueue.size() > 0){
									ATOperation op = Authenticator.operationsQueue.poll();
									if (op == null)
										break;
									logAsInfo("Executing Operation: " + op.getDescription());
									try{
										op.run(ss);
									}
									catch (Exception e)
									{
										logAsInfo("Error Occured while executing operation:\n"
												+ e.toString());
										op.OnExecutionError(e);
									}
								}
							}
							else
								logAsInfo("No Outbound Operations Found.");
							
							if(shouldStopListener)
								break;
			    	    }
					}
					catch (Exception e1) {
						
						//TODO - notify gui
						logAsInfo("Fatal Error, Authenticator Operation ShutDown Because Of: \n" + e1.toString());
					}
					finally
					{
						isRunning = false;
						try { ss.close(); plugnplay.removeMapping(); } catch (IOException | SAXException e) { } 
						logAsInfo("Listener Stopped");
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
			logAsInfo("Stopping Listener ... ");
			this.listenerThread.wait();
		}
		
	}
	
	public boolean isRuning()
	{
		return isRunning ;
	}
	
	public void logAsInfo(String str)
    {
		if(Authenticator.getApplicationParams().getShouldPrintTCPListenerInfoToConsole())
			this.LOG.info(str);
    }
}
