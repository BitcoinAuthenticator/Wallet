package authenticator.network;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.application.Platform;
import javafx.scene.image.Image;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.protobuf.ByteString;

import wallettemplate.Main;
import authenticator.Authenticator;
import authenticator.BASE;
import authenticator.WalletOperation;
import authenticator.Utils.EncodingUtils;
import authenticator.network.exceptions.TCPListenerCouldNotStartException;
import authenticator.operations.ATOperation;
import authenticator.operations.ATOperation.ATNetworkRequirement;
import authenticator.operations.ATOperationNetworkRequirementsNotAvailable;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

/**
 * <b>The heart of the wallet operation.</b><br>
 * <br>
 * <b>When does it start ?</b><br>
 * This listener is launched on wallet startup by {@link authenticator.Authenticator#doStart()} function.<br>
 * <br>
 * <b>What does it do ?</b><br>
 * The listener is responsible for 2 main operations:<br>
 * <ol>
 * <li>Inbound Operations - The listener will listen on the socket for any inbound communication. When it hooks to another socket, it will expect a requestID for reference.<br>
 * 		When the requestID is received, it will search it in the pending requests file. When the pending operation is found, the TCPListener will follow the
 * 		{@link authenticator.protobuf.ProtoConfig.PendingRequest.Contract Contract} to complete the reques.</li>
 * <li>Outbound Operations - When calling {@link authenticator.Authenticator#addOperation(ATOperation operation) addOperation}, the authenticator will add
 * 	   an operation to the operation queue. Here, the TCPListener will look for operations to execute from the said queue.</li>
 * <ol>
 * 
 * @author alon
 *
 */
public class TCPListener extends BASE{
	public static Socket socket;
	private static ConcurrentLinkedQueue<ATOperation> operationsQueue;
	private static Thread listenerThread;
	private static UpNp plugnplay;
	private static boolean isManuallyPortForwarded;
	private static int forwardedPort;
	private static BANeworkInfo vBANeworkInfo;
	private String[] args;
	private ServerSocket ss = null;
	/**
	 * Network Requirements flags
	 */
	public  boolean PORT_FORWARDED = false;
	public  boolean SOCKET_OPERATIONAL = false;
	
	WalletOperation wallet;
	
	/**
	 * Flags
	 */
	private boolean shouldStopListener;
	
	/**
	 * args:<br>
	 * [0] - port number<br>
	 * @param wallet
	 * @param isManuallyPortForwarded
	 * @param args
	 */
	public TCPListener(WalletOperation wallet, boolean isManuallyPortForwarded,String[] args){
		super(TCPListener.class);
		this.wallet = wallet;
		this.args = args;
		this.isManuallyPortForwarded = isManuallyPortForwarded;
		if(operationsQueue == null)
			operationsQueue = new ConcurrentLinkedQueue<ATOperation>();
	}
	
	public void runListener(final String[] args) throws Exception
	{
		shouldStopListener = false;   
	    this.listenerThread = new Thread(){
	    	@SuppressWarnings("static-access")
			@Override
			public void run() {
	    		try{
	    			try{
	    				startup();
	    			}
	    			catch (TCPListenerCouldNotStartException e){
	    				logAsInfo("Something went wrong with the TCPLIstener startup, some operations may not work\n");
						e.printStackTrace();
	    			}
	    			
	    			assert(operationsQueue != null);
	    			notifyStarted();
	    			
	    			looper();
				}
				catch (Exception e1) {
					logAsInfo("Fatal Error, Authenticator Operation ShutDown Because Of: \n");
					e1.printStackTrace();
				}
				finally
				{
					try { ss.close(); }  catch (Exception e) { }
					try { plugnplay.removeMapping(); } catch (Exception e) { } 
					LOG.info("Listener Stopped");
					notifyStopped();
				}
	    	}
	    	
	    	/**
	    	 * Setup all necessary things for the TCP looper to run.<br>
	    	 * Test:
	    	 * <ol>
	    	 * <li>UPNP</li>
	    	 * <li>Socket listening</li>
	    	 * </ol>
	    	 * 
	    	 * @throws TCPListenerCouldNotStartException
	    	 */
	    	@SuppressWarnings("static-access")
			private void startup() throws TCPListenerCouldNotStartException{
	    		forwardedPort = Integer.parseInt(args[0]);
	    		if(!isManuallyPortForwarded){
	    			if(plugnplay != null)
		    		{
		    			throw new TCPListenerCouldNotStartException("Could not start TCPListener");
		    		}
		    		
		    		plugnplay = new UpNp();
		    		try {
						plugnplay.run(new String[]{args[0]});
						if(plugnplay.isPortMapped(Integer.parseInt(args[0])) == true){
							PORT_FORWARDED = true;
							vBANeworkInfo = new BANeworkInfo(plugnplay.getExternalIP(), plugnplay.getLocalIP());
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new TCPListenerCouldNotStartException("Could not start TCPListener");
					}
	    		}
	    		// TODO - check if is truly forwarded
	    		else{
	    			PORT_FORWARDED = true;
	    			try {
						vBANeworkInfo = new BANeworkInfo(getExternalIp(), InetAddress.getLocalHost().getHostAddress());
					} catch (IOException e) {
						e.printStackTrace();
						throw new TCPListenerCouldNotStartException("Could not start TCPListener");
					}
	    		}
	    		
	    		if(PORT_FORWARDED)
    			try {
					ss = new ServerSocket (forwardedPort);
					ss.setSoTimeout(5000);
					SOCKET_OPERATIONAL = true;
				} catch (IOException e) {
					e.printStackTrace();
					try { plugnplay.removeMapping(); } catch (IOException | SAXException e1) { }
					throw new TCPListenerCouldNotStartException("Could not start TCPListener");
				}
	    	}
	    	
	    	private void looper() throws FileNotFoundException, IOException{
	    		boolean isConnected;
				sendUpdatedIPsToPairedAuthenticators();
				while(true)
	    	    {
					isConnected = false;
					if(PORT_FORWARDED && SOCKET_OPERATIONAL)
						try{
							socket = ss.accept();
							isConnected = true;
						}
						catch (SocketTimeoutException e){ isConnected = false; }
					else
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					
					//#################################
					//
					//		Inbound
					//
					//#################################
					PendingRequest pendingReq = null;
					try{
						if(isConnected){
							/**
							 * Check for network requirements availability
							 */
							// if we have an inbound operation that means upnp and socket work
							
							logAsInfo("Processing Pending Operation ...");
							DataInputStream inStream = new DataInputStream(socket.getInputStream());
							DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
							//get request ID
							String requestID = "";
							int keysize = inStream.readInt();
							byte[] reqIdPayload = new byte[keysize];
							inStream.read(reqIdPayload);
							JSONObject jo = new JSONObject(new String(reqIdPayload));
							requestID = jo.getString("requestID");
							//
							for(Object o:wallet.getPendingRequests()){
								PendingRequest po = (PendingRequest)o;
								if(po.getRequestID().equals(requestID))
								{
									pendingReq = po;
									break;
								}
							}
							//
							if(pendingReq != null){ 
								// Should we send something on connection ? 
								if(pendingReq.getContract().getShouldSendPayloadOnConnection()){
									byte[] p = pendingReq.getPayloadToSendInCaseOfConnection().toByteArray();
									outStream.writeInt(p.length);
									outStream.write(p);
									logAsInfo("Sent transaction");
								}
								
								// Should we receive something ?
								if(pendingReq.getContract().getShouldReceivePayloadAfterSendingPayloadOnConnection()){
									keysize = inStream.readInt();
									byte[] in = new byte[keysize];
									inStream.read(in);
									PendingRequest.Builder b = PendingRequest.newBuilder(pendingReq);
									b.setPayloadIncoming(ByteString.copyFrom(in));
									pendingReq = b.build();
								}
								
								//cleanup
								inStream.close();
								outStream.close();
								
								// Complete Operation ?
								switch(pendingReq.getOperationType()){
								case SignAndBroadcastAuthenticatorTx:
									byte[] txBytes = EncodingUtils.hexStringToByteArray(pendingReq.getRawTx());
									Transaction tx = new Transaction(wallet.getNetworkParams(),txBytes);
									 
									ATOperation op = OperationsFactory.SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION(wallet,
											tx,
											pendingReq.getPairingID(), 
											null,
											null,
											true,
											pendingReq.getPayloadIncoming().toByteArray(),
											pendingReq);
									op.SetOperationUIUpdate(new OnOperationUIUpdate(){

										@Override
										public void onBegin(String str) { }

										@Override
										public void statusReport( String report) { }

										@SuppressWarnings("restriction")
										@Override
										public void onFinished( String str) { 
											if(str != null)
											Platform.runLater(new Runnable() {
											      @Override public void run() {
											    	  Dialogs.create()
												        .owner(Main.stage)
												        .title("New Info.")
												        .masthead(null)
												        .message(str)
												        .showInformation();
											      }
											    });
											
										}

										@Override
										public void onError( Exception e, Throwable t) { }
										
									});
									operationsQueue.add(op);
									break;
								}
								
								if(!pendingReq.getContract().getShouldLetPendingRequestHandleRemoval())
									wallet.removePendingRequest(pendingReq);
							}
							else{ // pending request not found
								logAsInfo("No Pending Request Found");
							}
						}
						else{
							//notifyUiAndLog("Timed-out, Not Connections");
						}
					}
					catch(Exception e){
						wallet.removePendingRequest(pendingReq);
						logAsInfo("Error Occured while executing Inbound operation:\n"
								+ e.toString());
					}
					
					//#################################
					//
					//		Outbound
					//
					//#################################
					
					if(operationsQueue.size() > 0)
					{
						logAsInfo("Found " + operationsQueue.size() + " Operations in queue");
						while (operationsQueue.size() > 0){
							ATOperation op = operationsQueue.poll();
							if (op == null){
								break;
							}
							/**
							 * Check for network requirements availability
							 */
							logAsInfo("Checking network requirements availability for outbound operation");
							if(checkForOperationNetworkRequirements(op) == false )
							{
								op.OnExecutionError(new ATOperationNetworkRequirementsNotAvailable("Port mapping not available"));
								break;
							}
									
							
							logAsInfo("Executing Operation: " + op.getDescription());
							try{
								op.run(ss, vBANeworkInfo);
							}
							catch (Exception e)
							{
								logAsInfo("Error Occured while executing Outbound operation:\n"
										+ e.toString());
								op.OnExecutionError(e);
								e.printStackTrace();
							}
						}
					}
					else
						logAsInfo("No Outbound Operations Found.");
					
					if(shouldStopListener)
						break;
	    	    }
	    	}
	    };
	    listenerThread.start();
	}
	
	public boolean checkForOperationNetworkRequirements(ATOperation op){
		if((op.getOperationNetworkRequirements().getValue() & ATNetworkRequirement.PORT_MAPPING.getValue()) > 0){
			if(!PORT_FORWARDED || !SOCKET_OPERATIONAL){
				return false;
			}
		}
		
		return true;
	}
	
	//#####################################
	//
	//		Queue
	//
	//#####################################
	
	public boolean addOperation(ATOperation operation)
	{
		checkForOperationNetworkRequirements(operation);
		if(this.isRunning())
			operationsQueue.add(operation);
		return true;
	}

	public int getQueuePendingOperations(){
		return operationsQueue.size();
	}
	
	//#####################################
	//
	//		General
	//
	//#####################################
	
	public void logAsInfo(String str)
    {
		if(Authenticator.getApplicationParams().getShouldPrintTCPListenerInfoToConsole())
			LOG.info(str);
    }
	
	/**
	 * Check if the TCPListener has all the various operation network requirements
	 */
	public void sendUpdatedIPsToPairedAuthenticators(){
		for(ATAccount acc:wallet.getAllAccounts())
			if(acc.getAccountType() == WalletAccountType.AuthenticatorAccount){
				PairedAuthenticator  po = wallet.getPairingObjectForAccountIndex(acc.getIndex());
				ATOperation op = OperationsFactory.UPDATE_PAIRED_AUTHENTICATORS_IPS(wallet,
																			po.getPairingID());
				operationsQueue.add(op);
			}
	}
	
	public boolean areAllNetworkRequirementsAreFullyRunning(){
		if(!PORT_FORWARDED || !SOCKET_OPERATIONAL)
			return false;
		return true;
	}
	
	private String getExternalIp() throws IOException{
	    URL whatismyip = new URL("http://icanhazip.com");
	    BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
	    return in.readLine();
	}
	
	//#####################################
	//
	//		Service methods
	//
	//#####################################
	
	protected void doStart() {
		try {
			runListener(args);
		} catch (Exception e) {
			e.printStackTrace();
			notifyFailed(new Throwable("Failed to run TCPListener"));
		}
		
	}

	@Override
	protected void doStop() {
		shouldStopListener = true;
		LOG.info("Stopping Listener ... ");
	}
	
	
}
