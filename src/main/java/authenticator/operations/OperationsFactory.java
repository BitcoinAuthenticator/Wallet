package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;

import authenticator.operations.OperationsUtils.PairingProtocol;

public class OperationsFactory {
	
	static public ATOperation PAIRING_OPERATION(){
		return new ATOperation(ATOperationType.Pairing)
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetOperationAction(new OperationActions(){

						@Override
						public void PreExecution(String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void Execute(ServerSocket ss, String[] args) throws Exception {
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(ss, args[0]); //TODO
						}

						@Override
						public void PostExecution(String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
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
						
					}});
	}
}
