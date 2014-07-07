package authenticator;

import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;

public interface AuthenticatorGeneralEventsListener {
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
	public void onNewUserNamecoinIdentitySelection(AuthenticatorConfiguration.ConfigOneNameProfile profile);
	public void onFinishedBuildingWalletHierarchy();
	/**
	 * Will update when:<br>
	 * <ol>
	 * <li>Account spent funds</li>
	 * <li>Account received funds</li>
	 * <li>Account's confirmed/ unconfirmed balance is changed</li>
	 * </ol>
	 * <b> May be called multiple times if tx contains several changes</b><br>
	 * @param walletID
	 */
	public void onBalanceChanged(int walletID);
}