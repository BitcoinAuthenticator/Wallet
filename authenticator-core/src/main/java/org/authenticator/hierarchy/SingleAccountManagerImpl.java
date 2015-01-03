package org.authenticator.hierarchy;

import org.authenticator.hierarchy.exceptions.NoUnusedKeyException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.bitcoinj.crypto.ChildNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * A single account manager implementation, implementing {@link BAAccountHierarchyManager}
 *
 * Created by alonmuroch on 1/3/15.
 */
public class SingleAccountManagerImpl implements BAAccountHierarchyManager {
    private int accountIndex;
    private List<Integer> usedExternalKeys;
    private List<Integer> usedInternalKeys;

    /**
     * Those trackers are to return up to #keyLookAhead fresh keys
     */
    private List<ChildNumber> returnedExternalKeys;
    private List<ChildNumber> returnedInternalKeys;

    private int keyLookAhead;

    public SingleAccountManagerImpl(int accountIndex, int keyLookAhead){
        this(accountIndex, keyLookAhead, new ArrayList<Integer>(), new ArrayList<Integer>());
    }

    public SingleAccountManagerImpl(int accountIndex, int keyLookAhead, List<Integer> usedExternalKeys, List<Integer> usedInternalKeys){
        this.accountIndex = accountIndex;
        setKeylookahead(keyLookAhead);

        this.usedExternalKeys = new ArrayList<Integer>(usedExternalKeys);
        this.usedInternalKeys = new ArrayList<Integer>(usedInternalKeys);

        this.returnedExternalKeys = new ArrayList<ChildNumber>();
        this.returnedInternalKeys = new ArrayList<ChildNumber>();
    }

    private ChildNumber getUnusedKeyIndex(List<Integer> usedKeys, List<ChildNumber> alreadyReturnedKey) throws NoUnusedKeyException{
        // TODO - i have no idea what was i thinking doing this loop !
        // 		  we should find a better way for doing it

        for(int i=0; i< Math.pow(2, 31); i++){
            if(!usedKeys.contains(i) && !alreadyReturnedKey.contains( new ChildNumber(i, false))){
                return new ChildNumber(i, false);
            }
        }

        throw new NoUnusedKeyException("No unused key could be found or derived");
    }

    private ChildNumber getUnusedExternalKey() throws NoUnusedKeyException {
        return handleGetKey(usedExternalKeys, returnedExternalKeys);
    }

    private ChildNumber getUnusedInternalKey() throws NoUnusedKeyException{
        return handleGetKey(usedInternalKeys, returnedInternalKeys);
    }

    private ChildNumber handleGetKey(List<Integer> usedKeyList, List<ChildNumber> returnedKeyList) throws NoUnusedKeyException{
        ChildNumber ret;
        /**
         * In case we reached our lookahead limit, clean the returned keys
         * and start again. We empty them because some keys could be
         * marked as used and make room for new keys
         */
        if(returnedKeyList.size() > keyLookAhead)
            returnedKeyList.clear();

        ret = getUnusedKeyIndex(usedKeyList, returnedKeyList);
        returnedKeyList.add(ret);

        return ret;
    }

    //###############################
    //
    //		API
    //
    //###############################
    @Override
    public void setKeylookahead(int value){
        this.keyLookAhead = value;
    }

    @Override
    public ChildNumber getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes type) throws NoUnusedKeyException {
        return type == AuthWalletHierarchy.HierarchyAddressTypes.External? getUnusedExternalKey():getUnusedInternalKey();
    }

    @Override
    public void setKeyAsUsed(int keyIndex, AuthWalletHierarchy.HierarchyAddressTypes type) {
        if(type == AuthWalletHierarchy.HierarchyAddressTypes.External){
            usedExternalKeys.add( new Integer(keyIndex));
        }
        else{
            usedInternalKeys.add( new Integer(keyIndex));
        }
    }

    public int getAccountIndex(){ return accountIndex; }
}
