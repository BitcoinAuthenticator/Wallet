package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;

public interface OperationActions {
	public void PreExecution(String[] args)  throws Exception ;
	public void Execute(ServerSocket ss, String[] args)  throws Exception ;
	public void PostExecution(String[] args)  throws Exception ;
}
