package authenticator.GCM.dispacher;

public enum MessageType {
	test (1 << 0), // 1
	signTx (1 << 1), // 2
	updateIpAddressesForPreviousMessage ( 1 << 2 ) // 4
	;
	private final int id;
	MessageType(int id) { this.id = id; }
    public int getValue() { return id; }
}
