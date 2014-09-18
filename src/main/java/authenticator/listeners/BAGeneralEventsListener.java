package authenticator.listeners;

import javax.annotation.Nullable;
import javafx.scene.image.Image;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;

import authenticator.operations.OperationsUtils.SignProtocol;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import authenticator.protobuf.ProtoConfig.PendingRequest;

public interface BAGeneralEventsListener {
	/**
	 * Called when a new pairing is completed
	 */
	public void onNewPairedAuthenticator();
	/**
	 * Called when a standard account is created.<br>
	 */
	public void onNewStandardAccountAdded();
	/**
	 * Called when an account was deleted, both an Authenticator and Standard account. 
	 * @param accountIndex
	 */
	public void onAccountDeleted(int accountIndex);
	/**
	 * Will be called if account has been modified.<br>
	 * Includes all events except the creation of a new account or pairing.
	 * @param accountIndex
	 */
	public void onAccountBeenModified(int accountIndex);
	public void onNewOneNameIdentitySelection(ConfigOneNameProfile profile, @Nullable Image profileImage);
	/**
	 * Will update when:<br>
	 * <ol>
	 * <li>Account spent funds</li>
	 * <li>Account received funds</li>
	 * <li>Account's confirmed/ unconfirmed balance is changed</li>
	 * </ol>
	 * <b> May be called multiple times if tx contains several changes</b><br>
	 * 
	 * @param walletID
	 * @param @Nullable tx - could be null to just update the UI
	 * @param howBalanceChanged
	 * @param confidence
	 */
	public void onBalanceChanged(@Nullable Transaction tx, HowBalanceChanged howBalanceChanged, ConfidenceType confidence);
	public enum HowBalanceChanged{
		ReceivedCoins,
		SentCoins;
	};
	
	/**
	 * Will be fired when the {@link authenticator.operations.OperationsFactory#SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION Authenticator Tx Sign Operation} completes
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
}