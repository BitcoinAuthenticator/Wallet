package authenticator;

import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;

public interface AuthenticatorGeneralEventsListener {
	public void onNewPairedAuthenticator();
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

