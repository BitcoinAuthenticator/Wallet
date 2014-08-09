package authenticator.walletCore.exceptions;

public class EmptyWalletPasswordException extends Exception{
	public EmptyWalletPasswordException(String message) {
        super(message);
    }
}
