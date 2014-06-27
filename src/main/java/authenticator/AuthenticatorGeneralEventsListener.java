package authenticator;

import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;

public interface AuthenticatorGeneralEventsListener {
	public void onNewPairedAuthenticator();
	public void onNewUserNamecoinIdentitySelection(AuthenticatorConfiguration.ConfigOneNameProfile profile);
	public void onFinishedBuildingWalletHierarchy();
}

