package authenticator.operations;


public enum ATOperationType {
	Pairing(1 << 0), // 1
	Unpair(1 << 1), // 2
	SignTx (1 << 2), // 4
	updateIpAddressesForPreviousMessage ( 1 << 3 ) // 8
	;
	private final int id;
	ATOperationType(int id) { this.id = id; }
    public int getValue() { return id; }
}
