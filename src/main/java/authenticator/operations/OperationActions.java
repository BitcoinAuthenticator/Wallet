package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;

import authenticator.network.BANeworkInfo;

public interface OperationActions {
	public void PreExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception ;
	public void Execute(OnOperationUIUpdate listenerUI, ServerSocket ss, BANeworkInfo netInfo, String[] args, OnOperationUIUpdate listener)  throws Exception ;
	public void PostExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception ;
	public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) ;
}
