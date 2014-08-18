package authenticator.listeners;

import authenticator.operations.OperationsUtils.SignProtocol.AuthenticatorAnswerType;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.protobuf.ProtoConfig.PendingRequest;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;

public class BAGeneralEventsAdapter implements BAGeneralEventsListener{

	@Override
	public void onNewPairedAuthenticator() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNewStandardAccountAdded() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccountDeleted(int accountIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccountBeenModified(int accountIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNewUserNamecoinIdentitySelection(ConfigOneNameProfile profile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBalanceChanged(Transaction tx,
			HowBalanceChanged howBalanceChanged, ConfidenceType confidence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAuthenticatorSigningResponse(Transaction tx,
			String pairingID, PendingRequest pendingReq,
			AuthenticatorAnswerType answerType, String str) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddressMarkedAsUsed(ATAddress address) {
		// TODO Auto-generated method stub
		
	}

}
