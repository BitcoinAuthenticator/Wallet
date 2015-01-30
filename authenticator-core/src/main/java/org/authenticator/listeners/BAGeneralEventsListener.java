package org.authenticator.listeners;

import java.util.List;

import javax.annotation.Nullable;

import javafx.scene.image.Image;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;

import org.authenticator.network.BANetworkInfo;
import org.authenticator.operations.operationsUtils.SignProtocol;
import org.authenticator.protobuf.ProtoConfig.ATAddress;
import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import org.authenticator.protobuf.ProtoConfig.PendingRequest;

public interface BAGeneralEventsListener {
	/**
	 * Will be called if something changed in the wallet's accounts or a new active account is been set.<br>
	 * accountIndex could be -1 if cannot pass account index
	 * 
	 * @param type
	 * @param accountIndex
	 */
	public void onAccountsModified(AccountModificationType type, int accountIndex);
	public enum AccountModificationType{
		ActiveAccountChanged,
		NewAccount,
		AccountDeleted,
		AccountBeenModified;
	};
	
	/**
	 * The profile param will be null if the identity was deleted.
	 * @param profile
	 * @param profileImage
	 */
	public void onOneNameIdentityChanged(@Nullable ConfigOneNameProfile profile, @Nullable Image profileImage);


	/**
	 * Will update when:<br>
	 * <ol>
	 * <li>Account spent funds</li>
	 * <li>Account received funds</li>
	 * <li>Account's confirmed/ unconfirmed balance is changed</li>
	 * </ol>
	 * <b> May be called multiple times if tx contains several changes</b><br>
	 *
	 * @param tx
	 * @param howBalanceChanged
	 */
	public void onBalanceChanged(@Nullable Transaction tx, HowBalanceChanged howBalanceChanged);
	public enum HowBalanceChanged{
		ReceivedCoins,
		SentCoins,
		WalletChange;
	};
	
	/**
	 * Will be fired when the authenticator returns a signing answer (could be ok, nothing or did not accept) completes
	 * @param tx
	 * @param pairingID
	 * @param pendingReq
	 * @param answerType
	 * @param str
	 */
	public void onAuthenticatorSigningResponse(Transaction tx, 
			String pairingID, 
			@Nullable PendingRequest pendingReq, 
			SignProtocol.AuthenticatorAnswerType answerType,
			@Nullable String str);
	
	/**
	 * Will be fired whenever an address is marked as used (coins received to it)
	 * 
	 * @param address
	 */
	public void onAddressMarkedAsUsed(ATAddress address);
	
	/**
	 * will pass a value between 0 and 1 indicating the download progress.<br>
	 * Will pass 1 when finished.
	 * @param progress
	 */
	public void onBlockchainDownloadChange(float progress);

	/**
	 * Called whenever the UI changed the settings. This call should be handled by you in your UI implementation.<br>
	 * Call {@link authenticator.Authenticator#fireOnWalletSettingsChange fireOnWalletSettingsChange}
	 */
	public void onWalletSettingsChange();
	
	/**
	 * Will be called after the {@link authenticator.network.TCPListener TCPListener} tried its startup process.
	 * 
	 * @param info
	 */
	public void onAuthenticatorNetworkStatusChange(BANetworkInfo info);
	
	/**
	 * Will be called whenever a {@link authenticator.protobuf.ProtoConfig.PendingRequest PendingRequest} is been added or removed.
	 */
	public void onPendingRequestUpdate(List<PendingRequest> requests, PendingRequestUpdateType updateType);
	public enum PendingRequestUpdateType{
		RequestAdded,
		RequestDeleted;
	};
}