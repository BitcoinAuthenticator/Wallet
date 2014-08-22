package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;

import authenticator.network.BANeworkInfo;
import authenticator.operations.listeners.OperationListener;

public interface OperationActions {
	public void PreExecution(OperationListener listenerUI, String[] args)  throws Exception ;
	public void Execute(OperationListener listenerUI, ServerSocket ss, BANeworkInfo netInfo, String[] args, OperationListener listener)  throws Exception ;
	public void PostExecution(OperationListener listenerUI, String[] args)  throws Exception ;
	public void OnExecutionError(OperationListener listenerUI, Exception e) ;
}
