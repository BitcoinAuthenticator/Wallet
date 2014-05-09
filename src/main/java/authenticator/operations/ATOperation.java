package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class ATOperation {
	private OperationActions mOperationActions;
	private String operationDescription;
	private ATOperationType mOperationType;
	private String[] args = null;
	
	public ATOperation(ATOperationType type){mOperationType = type;}
	public ATOperation (ATOperationType type, 
			OperationActions action, 
			String desc,
			String[] ar){
		mOperationType = type;
		mOperationActions = action;
		operationDescription = desc;
		args = ar;
	}
	
	public void run(DataInputStream inputStream, DataOutputStream outPutStream)
	{
		mOperationActions.PreExecution(args);
		mOperationActions.Execute(inputStream, outPutStream, args);
		mOperationActions.PostExecution(args);
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
	
	public ATOperation SetArguments(String[] ar)
	{
		this.args = ar;
		return this;
	}
}
