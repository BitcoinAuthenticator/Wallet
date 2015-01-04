package org.authenticator.hierarchy;

import org.authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import org.authenticator.hierarchy.exceptions.NoUnusedKeyException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.bitcoinj.crypto.ChildNumber;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Created by alonmuroch on 1/3/15.
 */
public class BAHierarchyTest {
    @Test
    public void generateNewAccountAndAddAccountToHierarchyTest() {
        BAHierarchy H = new BAHierarchy();

        // fresh hierarchy
        for(int i=0 ; i < 100; i++ ) {
            SingleAccountManagerImpl acc = H.generateNewAccount();
            assertTrue(acc.getAccountIndex() == i);
        }

        // with previously generated accounts
        List<SingleAccountManagerImpl> accountTrackers = new ArrayList<SingleAccountManagerImpl>();
        for(int i=0 ; i < 14; i ++) {
            SingleAccountManagerImpl at =   new SingleAccountManagerImpl(i,
                    BAHierarchy.keyLookAhead,
                    new ArrayList<Integer>(),
                    new ArrayList<Integer>());
            accountTrackers.add(at);
        }
        H = new BAHierarchy();
        H.buildWalletHierarchyForStartup(accountTrackers);

        for(int i=14 ; i < 100; i++ ) {
            SingleAccountManagerImpl acc = H.generateNewAccount();
            assertTrue(acc.getAccountIndex() == i);
        }

        // with previously generated accounts with gaps
        accountTrackers = new ArrayList<SingleAccountManagerImpl>();
        for(int i=0 ; i < 5; i ++) {
            SingleAccountManagerImpl at =   new SingleAccountManagerImpl(i,
                    BAHierarchy.keyLookAhead,
                    new ArrayList<Integer>(),
                    new ArrayList<Integer>());
            accountTrackers.add(at);
        }
        SingleAccountManagerImpl at =   new SingleAccountManagerImpl(65,
                BAHierarchy.keyLookAhead,
                new ArrayList<Integer>(),
                new ArrayList<Integer>());
        accountTrackers.add(at);

        H = new BAHierarchy();
        H.buildWalletHierarchyForStartup(accountTrackers);

        for(int i=5 ; i < 100; i++ ) {
            SingleAccountManagerImpl acc = H.generateNewAccount();
            if(i < 65)
                assertTrue(acc.getAccountIndex() == i);
            else
                assertTrue(acc.getAccountIndex() == i + 1);
        }
    }

    @Test
    public void getNextPubKeyTest() {
        // just test that it thorws NoAccountCouldBeFoundException because other components are tested elsewhere
        boolean didThrow = false;
        BAHierarchy H = new BAHierarchy();
        try {
            H.getNextPubKey(0, null, null);
        } catch (Exception e) {
            assertTrue(e instanceof NoAccountCouldBeFoundException);
            didThrow = true;
        }
        assertTrue(didThrow);


        // with accounts
        List<SingleAccountManagerImpl> accountTrackers = new ArrayList<SingleAccountManagerImpl>();
        for(int i=0 ; i < 14; i ++) {
            SingleAccountManagerImpl at =   new SingleAccountManagerImpl(i,
                    BAHierarchy.keyLookAhead,
                    new ArrayList<Integer>(),
                    new ArrayList<Integer>());
            accountTrackers.add(at);
        }
        H = new BAHierarchy();
        H.buildWalletHierarchyForStartup(accountTrackers);

        didThrow = false;
        H = new BAHierarchy();
        try {
            H.getNextPubKey(14, null, null);
        } catch (Exception e) {
            assertTrue(e instanceof NoAccountCouldBeFoundException);
            didThrow = true;
        }
        assertTrue(didThrow);
    }
}
