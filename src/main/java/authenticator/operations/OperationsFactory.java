package authenticator.operations;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class OperationsFactory {
	
	static public ATOperation PAIRING_OPERATION(){
		return new ATOperation(ATOperationType.Pairing)
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetOperationAction(new OperationActions(){

						@Override
						public void PreExecution(String[] args) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void Execute(DataInputStream inputStream,
								DataOutputStream outPutStream, String[] args) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void PostExecution(String[] args) {
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
					public void Execute(DataInputStream inputStream,
							DataOutputStream outPutStream, String[] args) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void PostExecution(String[] args) {
						// TODO Auto-generated method stub
						
					}
					
				});
	}
}
