package authenticator.operations;

import java.net.ServerSocket;

public class ATOperation {
	private OperationActions mOperationActions;
	private OnOperationUIUpdate listener;
	private String operationDescription;
	private ATOperationType mOperationType;
	private String[] args = null;
	
	
	public ATOperation(ATOperationType type)
	{
		mOperationType = type;
	}
	
	public ATOperation (ATOperationType type, 
			OperationActions action, 
			String desc,
			String[] ar){
		mOperationType = type;
		mOperationActions = action;
		operationDescription = desc;
		args = ar;
	}
	
	public void run(ServerSocket ss)  throws Exception 
	{
		if(this.listener != null)
			this.listener.onBegin(beginMsg);
		mOperationActions.PreExecution(args);
		mOperationActions.Execute( ss, args, this.listener);
		mOperationActions.PostExecution(args);
		if(this.listener != null)
			this.listener.onFinished(finishedMsg);
	}
	
	public void OnExecutionError(Exception e){
		mOperationActions.OnExecutionError(e);
		if(this.listener != null)
			this.listener.onError(e);
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
}
