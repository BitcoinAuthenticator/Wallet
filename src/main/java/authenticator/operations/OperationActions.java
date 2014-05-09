package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface OperationActions {
	public void PreExecution(String[] args);
	public void Execute(DataInputStream inputStream, DataOutputStream outPutStream, String[] args);
	public void PostExecution(String[] args);
}
