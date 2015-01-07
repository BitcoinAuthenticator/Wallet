package org.authenticator.walletCore.exceptions;

public class WrongWalletPasswordException extends Exception{
	public WrongWalletPasswordException(String message) {
        super(message);
    }
}
