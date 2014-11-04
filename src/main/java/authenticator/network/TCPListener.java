package authenticator.network;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import javafx.application.Platform;
import javafx.scene.image.Image;

import org.json.JSONObject;
import org.xml.sax.SAXException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;

import com.google.protobuf.ByteString;
import com.subgraph.orchid.encoders.Hex;

import wallettemplate.Main;
import authenticator.Authenticator;
import authenticator.BASE;
import authenticator.Utils.EncodingUtils;
import authenticator.network.exceptions.TCPListenerCouldNotStartException;
import authenticator.operations.BAOperation;
import authenticator.operations.BAOperation.BANetworkRequirement;
import authenticator.operations.exceptions.BAOperationNetworkRequirementsNotAvailableException;
import authenticator.operations.listeners.OperationListener;
import authenticator.operations.listeners.OperationListenerAdapter;
import authenticator.operations.OperationsFactory;
import authenticator.protobuf.ProtoConfig.ATAccount;
import authenticator.protobuf.ProtoConfig.PairedAuthenticator;
import authenticator.protobuf.ProtoConfig.PendingRequest;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import authenticator.walletCore.BAPassword;
import authenticator.walletCore.WalletOperation;
import authenticator.walletCore.exceptions.CannotRemovePendingRequestException;

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
 * <li>Outbound Operations - When calling {@link authenticator.Authenticator#addOperation(BAOperation operation) addOperation}, the authenticator will add
 * 	   an operation to the operation queue. Here, the TCPListener will look for operations to execute from the said queue.</li>
 * <ol>
 * 
 * @author alon
 *
 */
public class TCPListener extends BASE{
	final int LOOPER_BLOCKING_TIMEOUT = 3000;
	
	private Socket socket;
	private ConcurrentLinkedQueue<BAOperation> operationsQueue;
	private Thread listenerThread;
	private UpNp plugnplay;
	private boolean isManuallyPortForwarded;
	private int forwardedPort;
	private BANetworkInfo vBANeworkInfo;
	private String[] args;
	private ServerSocket ss = null;
	
	/**
	 * No all operations will have a UI listener, specially those that are created by the system itself 
	 * (like the operation created in a paired Tx signing with the Authenticator app.<br>
	 * This long living listener will provide a default way to notify the UI of important information in 
	 * case an operation specific listener was not found.
	 */
	private OperationListener longLivingOperationsListener;
	
	private BAOperation CURRENT_OUTBOUND_OPERATION = null;
	
	/**
	 * Data binder
	 */
	private TCPListenerExecutionDataBinder dataBinder = new DataBinderAdapter();
	public class DataBinderAdapter implements TCPListenerExecutionDataBinder{
		@Override
		public BAPassword getWalletPassword() {
			return new BAPassword();
		}
	}
	
	WalletOperation wallet;
	
	/**
	 * Flags
	 */
	private boolean shouldStopListener;
	
	public TCPListener(){ super(TCPListener.class); }
	
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
			operationsQueue = new ConcurrentLinkedQueue<BAOperation>();
	}
	
	public void runListener(final String[] args) throws Exception
	{
		shouldStopListener = false;   
	    this.listenerThread = new Thread(){
			@Override
			public void run() {
	    		try{
	    			startup();	    			
	    			
	    			assert(operationsQueue != null);
	    			notifyStarted();
	    			Authenticator.fireOnAuthenticatorNetworkStatusChange(getNetworkInfo());
	    			System.out.println(TCPListener.this.toString());
	    			
	    			looper();
				}
				catch (Exception e1) {
					LOG.info("Fatal Error, TCPListener ShutDown Because Of: \n");
					e1.printStackTrace();
					System.out.println("\n\n" + toString());
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
	    		/**
	    		 * In any case, port forwarded manually/ upnp or not, get Ips and open socket.
	    		 * 
	    		 */
	    		if(!isManuallyPortForwarded){
	    			if(plugnplay != null)
		    		{
		    			throw new TCPListenerCouldNotStartException("Could not start TCPListener");
		    		}
		    		
		    		plugnplay = new UpNp();
		    		try {
						plugnplay.run(new String[]{args[0]});
						if(plugnplay.isPortMapped(Integer.parseInt(args[0])) == true){
							vBANeworkInfo = new BANetworkInfo(plugnplay.getExternalIP(), plugnplay.getLocalIP());
							vBANeworkInfo.PORT_FORWARDED = true;
							LOG.info("Successfuly map ported port: " + forwardedPort);
						}
						else {
							vBANeworkInfo = new BANetworkInfo(getExternalIp(), getInternalIp());
							vBANeworkInfo.PORT_FORWARDED = false;
							LOG.info("Failed to map port");
						}
					} catch (Exception e) {
						e.printStackTrace();
						vBANeworkInfo = new BANetworkInfo(getExternalIp(), getInternalIp());
						vBANeworkInfo.PORT_FORWARDED = false;
						LOG.info("Failed to map port");
					}
	    		}
	    		else
	    			try {
						vBANeworkInfo = new BANetworkInfo(getExternalIp(), InetAddress.getLocalHost().getHostAddress());
						vBANeworkInfo.PORT_FORWARDED = true;
						LOG.info("Marked port " + forwardedPort + " as forwarded");
					} catch (Exception e) {
						e.printStackTrace();
						throw new TCPListenerCouldNotStartException("Could not start TCPListener");
					}
	    			    		
	    		//if(PORT_FORWARDED)
    			try {
					ss = new ServerSocket (forwardedPort);
					ss.setSoTimeout(LOOPER_BLOCKING_TIMEOUT);
					vBANeworkInfo.SOCKET_OPERATIONAL = true;
					LOG.info("Socket operational");
				} catch (Exception e) {
					e.printStackTrace();
					try { plugnplay.removeMapping(); } catch (IOException | SAXException e1) { }
					throw new TCPListenerCouldNotStartException("Could not start TCPListener");
				}
	    	}
	    	
	    	/**
	    	 * IMPORTANT !
	    	 * 
	    	 * Some operations require the Authenticator the set its AUTHENTICATOR_PW variable for pending requests.
	    	 * One example is the SignAndBroadcastAuthenticatorTx operation which has 2 parts, the first send a Tx to be
	    	 * signed by the Authenticator app and the second part which completes the operation. This second part requires 
	    	 * signing the Tx and therefore a valid AUTHENTICATOR_PW.
	    	 * 
	    	 * @throws FileNotFoundException
	    	 * @throws IOException
	    	 */
	    	@SuppressWarnings("unused")
			private void looper() throws FileNotFoundException, IOException{
	    		boolean isConnected;
				sendUpdatedIPsToPairedAuthenticators();
				while(true)
	    	    {
					isConnected = false;
					try{
						socket = ss.accept();
						isConnected = true;
					}
					catch (SocketTimeoutException | java.net.SocketException e){ isConnected = false; }
					
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
							
							LOG.info("Processing Pending Operation ...");
							DataInputStream inStream = new DataInputStream(socket.getInputStream());
							DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
							
							/*
							 * Send a pong message to confirm
							 */
							PongPayload pp = new PongPayload();
							outStream.writeInt(pp.getPayloadSize());
							outStream.write(pp.getBytes());
							LOG.info("Sent a pong answer");
							
							//get request ID
							String requestID = "";
							int keysize = inStream.readInt();
							byte[] reqIdPayload = new byte[keysize];
							inStream.read(reqIdPayload);
							JSONObject jo = new JSONObject(new String(reqIdPayload));
							requestID = jo.getString("requestID");
							String pairingID = jo.getString("pairingID");	
							//
							LOG.info("Looking for pending request ID: " + requestID);
							for(Object o:wallet.getPendingRequests()){
								PendingRequest po = (PendingRequest)o;
								if(po.getRequestID().equals(requestID))
								{
									pendingReq = po;
									break;
								}
							}
							//
							if(pendingReq == null){
								SecretKey secretkey = new SecretKeySpec(Hex.decode(wallet.getAESKey(pairingID)), "AES");
								CannotProcessRequestPayload p = new CannotProcessRequestPayload("Cannot find pending request\nPlease resend operation",
										secretkey);
								outStream.writeInt(p.getPayloadSize());
								outStream.write(p.toEncryptedBytes());
								LOG.info("No Pending Request Found, aborting inbound operation");
								
								if(longLivingOperationsListener != null)
									longLivingOperationsListener.onError(null, new Exception("Authenticator tried to complete a pending request but the request was not found, please try again"), null);
							}
							else{
								// Should we send something on connection ? 
								if(pendingReq.getContract().getShouldSendPayloadOnConnection()){
									byte[] p = pendingReq.getPayloadToSendInCaseOfConnection().toByteArray();
									outStream.writeInt(p.length);
									outStream.write(p);
									LOG.info("Sent transaction");
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
									byte[] txBytes = Hex.decode(pendingReq.getRawTx());
									Transaction tx = new Transaction(wallet.getNetworkParams(),txBytes);
									 
									BAOperation op = OperationsFactory.SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION(wallet,
											tx,
											pendingReq.getPairingID(), 
											null,
											null,
											true,
											pendingReq.getPayloadIncoming().toByteArray(),
											pendingReq, 
											dataBinder.getWalletPassword());
									operationsQueue.add(op);
									break;
								}
								
								if(!pendingReq.getContract().getShouldLetPendingRequestHandleRemoval())
									wallet.removePendingRequest(pendingReq);
							}
						}
						else
							;
						
					}
					catch(Exception e){
						if(pendingReq != null)
							try {
								wallet.removePendingRequest(pendingReq);
							} catch (CannotRemovePendingRequestException e1) {
								e1.printStackTrace();
							}
						e.printStackTrace();
						LOG.info("Error Occured while executing Inbound operation:\n"
								+ e.toString());
					}
					
					//#################################
					//
					//		Outbound
					//
					//#################################
					
					if(operationsQueue.size() > 0)
					{
						LOG.info("Found " + operationsQueue.size() + " Operations in queue");
						while (operationsQueue.size() > 0){
							BAOperation op = operationsQueue.poll();
							if (op == null){
								break;
							}
							/**
							 * Check for network requirements availability
							 */
							LOG.info("Checking network requirements availability for outbound operation");
							if(checkForOperationNetworkRequirements(op) == false )
							{
								op.OnExecutionError(new BAOperationNetworkRequirementsNotAvailableException("Required Network requirements not available"));
								break;
							}
									
							
							LOG.info("Executing Operation: " + op.getDescription());
							CURRENT_OUTBOUND_OPERATION = op;
							try{
								op.run(ss, vBANeworkInfo);
								CURRENT_OUTBOUND_OPERATION = null;
							}
							catch (Exception e)
							{
								e.printStackTrace();
								LOG.info("Error Occured while executing Outbound operation:\n"
										+ e.toString());
								
								if(op.getOperationListener() != null)
									op.OnExecutionError(e);
								else
									/*
									 * we still need to notify user even if we don't have an operation listener 
									 * (like in a paired account tx signing) 
									 */
									if(longLivingOperationsListener != null)
										longLivingOperationsListener.onError(op, e, null); 
							}
						}
					}
					else
						; // do nothing
					
					if(shouldStopListener)
						break;
	    	    }
	    	}
	    };
	    listenerThread.start();
	}
	
	public boolean checkForOperationNetworkRequirements(BAOperation op){
		if(vBANeworkInfo == null) // in case the setup process is not finished yet
			return false;
		
		if((op.getOperationNetworkRequirements().getValue() & BANetworkRequirement.PORT_MAPPING.getValue()) > 0){
			if(! vBANeworkInfo.PORT_FORWARDED || !vBANeworkInfo.SOCKET_OPERATIONAL){
				return false;
			}
		}
		
		if((op.getOperationNetworkRequirements().getValue() & BANetworkRequirement.SOCKET.getValue()) > 0){
			if(!vBANeworkInfo.SOCKET_OPERATIONAL){
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
	
	public boolean addOperation(BAOperation operation)
	{
		checkForOperationNetworkRequirements(operation);
		if(isRunning()){
			operationsQueue.add(operation);
			return true;
		}
		return false;
	}

	public int getQueuePendingOperations(){
		return operationsQueue.size();
	}
	
	//#####################################
	//
	//		General
	//
	//#####################################
	@Override
	public String toString() {
		return "Authenticator TCPListener: \n" + 
				String.format("%-30s: %10s\n", "State", this.isRunning()? "Running":"Not Running") + 
				String.format("%-30s: %10s\n", "External IP", this.getNetworkInfo().EXTERNAL_IP) + 
				String.format("%-30s: %10s\n", "Internal IP", this.getNetworkInfo().INTERNAL_IP) + 
				
				"Network Requirements: \n" + 
					String.format("   %-30s: %10s\n", "Port Mapped/ Forwarded", this.getNetworkInfo().PORT_FORWARDED? "True":"False") + 
					String.format("   %-30s: %10s\n", "Socket", this.getNetworkInfo().SOCKET_OPERATIONAL? "Operational":"Not Operational");
	}
	
	/**
	 * Check if the TCPListener has all the various operation network requirements
	 */
	public void sendUpdatedIPsToPairedAuthenticators(){
		for(ATAccount acc:wallet.getAllAccounts())
			if(acc.getAccountType() == WalletAccountType.AuthenticatorAccount){
				PairedAuthenticator  po = wallet.getPairingObjectForAccountIndex(acc.getIndex());
				BAOperation op = OperationsFactory.UPDATE_PAIRED_AUTHENTICATORS_IPS(wallet,
																			po.getPairingID());
				operationsQueue.add(op);
			}
	}
	
	public boolean areAllNetworkRequirementsAreFullyRunning(){
		
		if(!vBANeworkInfo.PORT_FORWARDED || !vBANeworkInfo.SOCKET_OPERATIONAL)
			return false;
		return true;
	}
	
	/**
	 * Used in case UPNP mapping doesn't work
	 * 
	 * @return
	 * @throws IOException
	 */
	private String getExternalIp(){
	    URL whatismyip;
		try {
			whatismyip = new URL("http://icanhazip.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
		    return in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return null;
	}
	
	/**
	 * Used in case UPNP mapping doesn't work
	 * 
	 * @return
	 * @throws UnknownHostException
	 */
	private String getInternalIp() {
		InetAddress i;
		try {
			i = InetAddress.getLocalHost();
			return i.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
        return null;
	}
	
	public void INTERRUPT_CURRENT_OUTBOUND_OPERATION() throws IOException{
		INTERRUPT_OUTBOUND_OPERATION(CURRENT_OUTBOUND_OPERATION);
	}
	public void INTERRUPT_OUTBOUND_OPERATION(BAOperation op) throws IOException{
		if(CURRENT_OUTBOUND_OPERATION == null)
			return;
		
		
		if(CURRENT_OUTBOUND_OPERATION.getOperationID().equals(op.getOperationID())){
			LOG.info("Interrupting Operation with ID " + op.getOperationID());
			CURRENT_OUTBOUND_OPERATION.interruptOperation();
			
			// restore socket
			vBANeworkInfo.SOCKET_OPERATIONAL = false;
			ss = new ServerSocket (forwardedPort);
			ss.setSoTimeout(LOOPER_BLOCKING_TIMEOUT);
			vBANeworkInfo.SOCKET_OPERATIONAL = true;
		}
		else
			LOG.info("Operation Not Found: Cannot Interrupt Operation with ID " + op.getOperationID());
	}
	
	public BANetworkInfo getNetworkInfo() {
		return vBANeworkInfo;
	}
	
	//#####################################
	//
	//		Data Binder
	//
	//#####################################
	
	/**
	 * An interface that implements necessary methods that get critical data to the listener at runtime.<br>
	 * This interface is critical for the correct operation and execution of a {@link authenticator.operations.BAOperation BAOperation}.<br>
	 * <b>Failing to implement this listener could cause operations to crash on execution</b>
	 * 
	 * @author Alon Muroch
	 *
	 */
	public interface TCPListenerExecutionDataBinder{
		public BAPassword getWalletPassword();
	}
	
	public void setExecutionDataBinder(TCPListenerExecutionDataBinder b){
		dataBinder = b;
	}
	
	//#####################################
	//
	//	Long living operations listener
	//
	//#####################################
	
	/**
	 * see {@link authenticator.network.TCPListener#longLivingOperationsListener TCPListener#longLivingOperationsListener}
	 * @param listener
	 */
	public void setOperationListener(OperationListener listener) {
		this.longLivingOperationsListener = listener;
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
