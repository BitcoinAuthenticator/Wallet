package authenticator.walletCore.exceptions;

public class CannotBroadcastTransactionException extends Exception{
	public CannotBroadcastTransactionException(String message) {
        super(message);
    }
}
