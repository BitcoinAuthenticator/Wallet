package authenticator.operations;

import java.net.ServerSocket;

import javax.annotation.Nullable;

import authenticator.network.BANeworkInfo;
import authenticator.protobuf.ProtoConfig.ATOperationType;

/**
 * Describes a complete operation for the authenticator.
 * 
 * @author alon
 *
 */
public class BAOperation {
	private OperationActions mOperationActions;
	private OnOperationUIUpdate listener;
	private String operationDescription;
	private ATOperationType mOperationType;
	private BANetworkRequirement mATNetworkRequirement = BANetworkRequirement.NONE;
	private String[] args = null;
	
	
	public BAOperation(ATOperationType type)
	{
		mOperationType = type;
	}
	
	public BAOperation (ATOperationType type, 
			BANetworkRequirement networkRequirements,
			OperationActions action, 
			String desc,
			String[] ar){
		mOperationType			 = type;
		mATNetworkRequirement	 = networkRequirements;
		mOperationActions 		 = action;
		operationDescription 	 = desc;
		args 					 = ar;
	}
	
	/**
	 * 
	 * @param ss
	 * @param netInfo
	 * @throws Exception
	 */
	public void run(ServerSocket ss, @Nullable BANeworkInfo netInfo)  throws Exception 
	{
		if(this.listener != null)
			this.listener.onBegin(beginMsg);
		mOperationActions.PreExecution(listener,args);
		mOperationActions.Execute( listener, ss, netInfo, args, this.listener);
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
	public BAOperation SetDescription(String desc)
	{
		this.operationDescription = desc;
		return this;
	}
	
	public BAOperation SetOperationAction(OperationActions action)
	{
		this.mOperationActions = action;
		return this;
	}
	
	public BAOperation SetOperationUIUpdate(OnOperationUIUpdate listener)
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
		PORT_MAPPING (1 << 0);		// 1
		
		private int value;
		BANetworkRequirement(int value) {
	            this.value = value;
	    }
	    public int getValue() { return this.value; }
	}
	
}
