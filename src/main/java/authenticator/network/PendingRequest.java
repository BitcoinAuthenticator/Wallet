package authenticator.network;

import com.google.bitcoin.core.Transaction;

import authenticator.operations.ATOperationType;

public class PendingRequest {
	public String pairingID;
	public String requestID;
	public ATOperationType operationType;
	// It seems too specific to put a Transaction variable here, but
	// most of the communication will involve a Tx
	public Transaction rawTx;
	public byte[] payloadIncoming;
	public byte[] payloadToSendInCaseOfConnection;
	public Contract contract;
	
	public PendingRequest(){
		contract = new Contract();
		contract.SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION = false;
		contract.SHOULD_SEND_PAYLOAD_ON_CONNECTION = false;
	}
	
	/**
	 * Essentially a bunch of flags to indicate what should we do in case of a connection
	 * 
	 * @author alon
	 *
	 */
	public class Contract{
		public boolean SHOULD_SEND_PAYLOAD_ON_CONNECTION;
		public boolean SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION;
	}
}
