package authenticator.operation;

public class ATOperation {
	private OperationActions mOperationActions;
	private String operationDescription;
	private ATOperationType mOperationType;
	
	public ATOperation(ATOperationType type){mOperationType = type;}
	public ATOperation (ATOperationType type, OperationActions action, String desc){
		mOperationType = type;
		mOperationActions = action;
		operationDescription = desc;
	}
	
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
}
