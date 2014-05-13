package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.SocketException;

import authenticator.operations.OperationsUtils.PairingProtocol;

public class OperationsFactory {
	
	static public ATOperation PAIRING_OPERATION(){
		return new ATOperation(ATOperationType.Pairing)
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetArguments(new String[]{"blockchain"})
					.SetOperationAction(new OperationActions(){
						int timeout = 5;
						ServerSocket socket = null;
						@Override
						public void PreExecution(String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@SuppressWarnings("static-access")
						@Override
						public void Execute(ServerSocket ss, String[] args) throws Exception {
							 timeout = ss.getSoTimeout();
							 ss.setSoTimeout(0);
							 socket = ss;
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(ss,args[0]); 
							 //Return to previous timeout
							 ss.setSoTimeout(timeout);
						}

						@Override
						public void PostExecution(String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void OnExecutionError(Exception e) {
							try {
								socket.setSoTimeout(timeout);
							} catch (SocketException e1) {
							
							}
						}
						
					});
	}

	static public ATOperation SIGN_TX_OPERATION(){
		return new ATOperation(ATOperationType.SignTx)
				.SetDescription("Sign Raw Transaction By Authenticator device")
				.SetOperationAction(new OperationActions(){

					@Override
					public void PreExecution(String[] args) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void Execute(ServerSocket ss, String[] args) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void PostExecution(String[] args) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void OnExecutionError(Exception e) {
						// TODO Auto-generated method stub
						
					}});
	}
}
