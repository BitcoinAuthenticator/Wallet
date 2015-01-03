package org.authenticator.listeners;

import org.authenticator.walletCore.utils.BAPassword;

/**
 * An interface that implements necessary methods that get critical data to the listener at runtime.<br>
 * This interface is critical for the correct operation and execution of runtime operations in the wallet
 * that were not initiated by the user.<br>
 * <b>For Example:</b>
 * <ol>
 * <li>Authenticator authorized a Tx, will require the wallets password to complete the Tx signing in the background.<br>
 *     This can happen without the user knowing (if the user created the Tx but it was authorized later on)</li>
 * <li>Coins received notification will require the get the pairing's AES key to encrypt the payload.<br>
 *     This in turn requires the wallet's password. see {@link org.authenticator.walletCore.WalletOperation#getAESKey(String, BAPassword) getAESKey(String, BAPassword)}</li>
 * </ol>
 *
 *
 * @author Alon Muroch
 *
 */
public interface BAWalletExecutionDataBinder {
    public BAPassword getWalletPassword();
}
