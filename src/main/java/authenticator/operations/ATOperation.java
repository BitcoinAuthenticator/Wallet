package authenticator.operations;

import java.net.ServerSocket;
import authenticator.protobuf.ProtoConfig.ATOperationType;

/**
 * Describes a complete operation for the authenticator.
 * 
 * @author alon
 *
 */
public class ATOperation {
	private OperationActions mOperationActions;
	private OnOperationUIUpdate listener;
	private String operationDescription;
	private ATOperationType mOperationType;
	private ATNetworkRequirement mATNetworkRequirement = ATNetworkRequirement.NONE;
	private String[] args = null;
	
	
	public ATOperation(ATOperationType type)
	{
		mOperationType = type;
	}
	
	public ATOperation (ATOperationType type, 
			ATNetworkRequirement networkRequirements,
			OperationActions action, 
			String desc,
			String[] ar){
		mOperationType			 = type;
		mATNetworkRequirement	 = networkRequirements;
		mOperationActions 		 = action;
		operationDescription 	 = desc;
		args 					 = ar;
	}
	
	public void run(ServerSocket ss)  throws Exception 
	{
		if(this.listener != null)
			this.listener.onBegin(beginMsg);
		mOperationActions.PreExecution(listener,args);
		mOperationActions.Execute( listener, ss, args, this.listener);
		mOperationActions.PostExecution(listener, args);
		if(this.listener != null)
			this.listener.onFinished(finishedMsg);
	}
	
	public void OnExecutionError(Exception e){
		mOperationActions.OnExecutionError(listener ,e);
		if(this.listener != null)
			this.listener.onError(e,null);
	}
	
	//#####################################
	//
	// 		Getter and Setters
	//
	//#####################################
	
	public String getDescription() { return this.operationDescription; }
	public ATOperation SetDescription(String desc)
	{
		this.operationDescription = desc;
		return this;
	}
	
	public ATOperation SetOperationAction(OperationActions action)
	{
		this.mOperationActions = action;
		return this;
	}
	
	public ATOperation SetOperationUIUpdate(OnOperationUIUpdate listener)
	{
		this.listener = listener;
		return this;
	}
	
	public ATOperation SetArguments(String[] ar)
	{
		this.args = ar;
		return this;
	}
	
	private String beginMsg;
	public ATOperation SetBeginMsg(String msg)
	{
		this.beginMsg = msg;
		return this;
	}
	
	private String finishedMsg;
	public ATOperation SetFinishedMsg(String msg)
	{
		this.finishedMsg = msg;
		return this;
	}
	
	public ATNetworkRequirement getOperationNetworkRequirements(){ return mATNetworkRequirement; }
	public ATOperation setOperationNetworkRequirements(ATNetworkRequirement value){
		mATNetworkRequirement = value;
		return this;
	}
	
	public enum ATNetworkRequirement{
		NONE		 (0),
		PORT_MAPPING (1 << 0);		// 1
		
		private int value;
		ATNetworkRequirement(int value) {
	            this.value = value;
	    }
	    public int getValue() { return this.value; }
	}
	
}
