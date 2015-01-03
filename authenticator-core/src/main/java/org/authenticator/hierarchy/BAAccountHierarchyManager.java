package org.authenticator.hierarchy;

import org.authenticator.hierarchy.exceptions.NoUnusedKeyException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.bitcoinj.crypto.ChildNumber;

/**
 * Manages an account (following BIP44 accounts) keys by their type to be as efficient as possible.<br>
 * Efficiency is making sure previously given keys are used and marked as used before new keys are returned.
 *
 * Created by alonmuroch on 1/3/15.
 */
public interface BAAccountHierarchyManager {
    /**
     * Implementation will keep returning fresh keys up until the lookahead value from the last used key index.<br>
     * Example:<br>
     * last used key index = 12<br>
     * lookahead = 10<br>
     * <b>Will return keys 13-22</b>
     *
     * @param value
     */
    public void setKeylookahead(int value);
    public ChildNumber getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes type) throws NoUnusedKeyException;
    public void setKeyAsUsed(int keyIndex, AuthWalletHierarchy.HierarchyAddressTypes type);

}
