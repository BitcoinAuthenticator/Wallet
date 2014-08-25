package authenticator.operations;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.security.SecureRandom;

import javax.annotation.Nullable;

import authenticator.network.BANetworkInfo;
import authenticator.operations.listeners.OperationListener;
import authenticator.protobuf.ProtoConfig.ATOperationType;

/**
 * Describes a complete operation for the authenticator.
 * 
 * @author alon
 *
 */
public class BAOperation {
	private String OPERATION_ID;
	private BAOperationActions mOperationActions;
	private OperationListener listener;
	private String operationDescription;
	private ATOperationType mOperationType;
	private BANetworkRequirement mATNetworkRequirement = BANetworkRequirement.NONE;
	private String[] args = null;
	
	private boolean CAN_CONTINUE_WITH_OPERAITON;
	
	public BAOperation(ATOperationType type)
	{
		this(type, BANetworkRequirement.NONE, null, null, null);
	}
	
	public BAOperation (ATOperationType type, 
			BANetworkRequirement networkRequirements,
			BAOperationActions action, 
			String desc,
			String[] ar){
		// operation id
		SecureRandom random = new SecureRandom();
		OPERATION_ID = new BigInteger(130, random).toString(32);
		
		// set params
		mOperationType			 = type;
		mATNetworkRequirement	 = networkRequirements;
		mOperationActions 		 = action;
		operationDescription 	 = desc;
		args 					 = ar;
		
		CAN_CONTINUE_WITH_OPERAITON = true;
	}
	
	/**
	 * 
	 * @param ss
	 * @param netInfo
	 * @throws Exception
	 */
	ServerSocket vServerSocket;
	public void run(ServerSocket ss, @Nullable BANetworkInfo netInfo)  throws Exception 
	{
		vServerSocket = ss;
		
		if(!CAN_CONTINUE_WITH_OPERAITON)
			return;
		
		if(this.listener != null)
			this.listener.onBegin(beginMsg);
		
		if(!CAN_CONTINUE_WITH_OPERAITON)
			return;
		
		mOperationActions.PreExecution(listener,args);
		
		if(!CAN_CONTINUE_WITH_OPERAITON)
			return;
		
		mOperationActions.Execute( listener, vServerSocket, netInfo, args, this.listener);
		
		if(!CAN_CONTINUE_WITH_OPERAITON)
			return;
		
		mOperationActions.PostExecution(listener, args);
		
		if(this.listener != null)
			this.listener.onFinished(finishedMsg);
	}
	
	public void OnExecutionError(Exception e){
		mOperationActions.OnExecutionError(listener ,e);
		if(this.listener != null)
			this.listener.onError(e,null);
	}
	
	/**
	 * Will Interrupt operation execution flow, completes/ fails current {@link BAOperationActions Action}<br>
	 * Will close the provided server socket in order to interrupt inbount communication listening 
	 * 
	 * @throws IOException
	 */
	public void interruptOperation() throws IOException{
		CAN_CONTINUE_WITH_OPERAITON = true;
		
		/**
		 * in case we stuck listening for incoming communication
		 */
		vServerSocket.close();
	}
	
	//#####################################
	//
	// 		Getter and Setters
	//
	//#####################################
	
	public String getOperationID(){
		return OPERATION_ID;
	}
	
	public String getDescription() { return this.operationDescription; }
	public BAOperation SetDescription(String desc)
	{
		this.operationDescription = desc;
		return this;
	}
	
	public BAOperation SetOperationAction(BAOperationActions action)
	{
		this.mOperationActions = action;
		return this;
	}
	
	public BAOperation SetOperationUIUpdate(OperationListener listener)
	{
		this.listener = listener;
		return this;
	}
	
	public BAOperation SetArguments(String[] ar)
	{
		this.args = ar;
		return this;
	}
	
	private String beginMsg;
	public BAOperation SetBeginMsg(String msg)
	{
		this.beginMsg = msg;
		return this;
	}
	
	private String finishedMsg;
	public BAOperation SetFinishedMsg(String msg)
	{
		this.finishedMsg = msg;
		return this;
	}
	
	public BANetworkRequirement getOperationNetworkRequirements(){ return mATNetworkRequirement; }
	public BAOperation setOperationNetworkRequirements(BANetworkRequirement value){
		mATNetworkRequirement = value;
		return this;
	}
	
	public enum BANetworkRequirement{
		NONE		 (0),
		PORT_MAPPING (1 << 0),	// 1
		SOCKET		 (1 << 1);	// 2
		
		private int value;
		BANetworkRequirement(int value) {
	            this.value = value;
	    }
	    public int getValue() { return this.value; }
	}
	
	public interface BAOperationActions {
		public void PreExecution(OperationListener listenerUI, String[] args)  throws Exception ;
		public void Execute(OperationListener listenerUI, ServerSocket ss, BANetworkInfo netInfo, String[] args, OperationListener listener)  throws Exception ;
		public void PostExecution(OperationListener listenerUI, String[] args)  throws Exception ;
		public void OnExecutionError(OperationListener listenerUI, Exception e) ;
	}
}
