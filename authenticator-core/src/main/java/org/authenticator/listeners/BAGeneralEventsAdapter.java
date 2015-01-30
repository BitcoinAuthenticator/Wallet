package org.authenticator.listeners;

import java.util.List;

import javax.annotation.Nullable;

import javafx.scene.image.Image;
import org.authenticator.network.BANetworkInfo;
import org.authenticator.operations.operationsUtils.SignProtocol.AuthenticatorAnswerType;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;

public class BAGeneralEventsAdapter implements BAGeneralEventsListener{

	@Override
	public void onBalanceChanged(Transaction tx,
			HowBalanceChanged howBalanceChanged) {
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

	@Override
	public void onOneNameIdentityChanged(@Nullable ConfigOneNameProfile profile, @Nullable Image profileImage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBlockchainDownloadChange(float progress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWalletSettingsChange() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccountsModified(AccountModificationType type, int accountIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAuthenticatorNetworkStatusChange(BANetworkInfo info) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPendingRequestUpdate(List<PendingRequest> requests,
			PendingRequestUpdateType updateType) {
		// TODO Auto-generated method stub
		
	}

}
