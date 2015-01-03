package org.authenticator.hierarchy;

import com.google.common.collect.ImmutableList;
import org.authenticator.hierarchy.exceptions.IncorrectPathException;
import org.authenticator.hierarchy.exceptions.NoUnusedKeyException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by alonmuroch on 1/3/15.
 */
public class SingleAccountManagerImplTest {
    @Test
    public void getUnusedKeyTest() {
        // not exceeds the lookahead
        SingleAccountManagerImpl impl = new SingleAccountManagerImpl(10, 10);
        try {
            for(int i=0; i < 11; i++) {
                ChildNumber ret1 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
                ChildNumber ret2 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.Internal);
                assertTrue(ret1.getI() == i);
                assertTrue(ret2.getI() == i);
            }
        } catch (NoUnusedKeyException e) {
            e.printStackTrace();
        }

        //  exceeds the lookahead
        impl = new SingleAccountManagerImpl(10, 10);
        try {
            for(int i=0; i < 100; i++) {
                ChildNumber ret1 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
                ChildNumber ret2 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.Internal);
                int expected = i % 11;
                assertTrue(ret1.getI() == expected);
                assertTrue(ret2.getI() == expected);
            }
        } catch (NoUnusedKeyException e) {
            e.printStackTrace();
        }

        // change lookahead
        impl = new SingleAccountManagerImpl(10, 10);
        impl.setKeylookahead(40);

        // not exceeds the lookahead
        try {
            for(int i=0; i < 41; i++) {
                ChildNumber ret1 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
                ChildNumber ret2 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.Internal);
                assertTrue(ret1.getI() == i);
                assertTrue(ret2.getI() == i);
            }
        } catch (NoUnusedKeyException e) {
            e.printStackTrace();
        }

        //  exceeds the lookahead
        impl = new SingleAccountManagerImpl(10, 10);
        impl.setKeylookahead(40);
        try {
            for(int i=0; i < 100; i++) {
                ChildNumber ret1 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
                ChildNumber ret2 = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.Internal);
                int expected = i % 41;
                assertTrue(ret1.getI() == expected);
                assertTrue(ret2.getI() == expected);
            }
        } catch (NoUnusedKeyException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void setKeyAsUsedTest() {
        SingleAccountManagerImpl impl = new SingleAccountManagerImpl(10, 10);
        List<Integer> keyIndexs = new ArrayList<Integer>();
        keyIndexs.add(2);
        keyIndexs.add(6);
        keyIndexs.add(9);
        impl.setKeyAsUsed(keyIndexs, AuthWalletHierarchy.HierarchyAddressTypes.External);

        try {
            ChildNumber ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 0);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 1);

            // should skip 2 because it is used

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 3);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 4);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 5);

            // should skip 6 because it is used

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 7);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 8);

            // should skip 9 because it is used

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 10);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 11);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 12);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 13);

            ret = impl.getUnusedKey(AuthWalletHierarchy.HierarchyAddressTypes.External);
            assertTrue(ret.getI() == 0);
        } catch (NoUnusedKeyException e) {
            e.printStackTrace();
        }
    }
}
