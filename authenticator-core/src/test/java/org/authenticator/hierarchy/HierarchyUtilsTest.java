package org.authenticator.hierarchy;

import com.google.common.collect.ImmutableList;
import org.authenticator.hierarchy.exceptions.IncorrectPathException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by alonmuroch on 1/3/15.
 */
public class HierarchyUtilsTest {
    @Test
    public void getKeyIndexFromPathTest() {
        // correct
        try {
            ChildNumber ret = HierarchyUtils.getKeyIndexFromPath(ImmutableList.of(new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(32)), true);
            assertTrue(ret.getI() == 32);
        } catch (IncorrectPathException e) {
            e.printStackTrace();
        }

        // throws IncorrectPathException
        boolean didThrow = false;
        try {
            ChildNumber ret = HierarchyUtils.getKeyIndexFromPath(ImmutableList.of(new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(32)), true);
            assertTrue(ret.getI() == 32);
        } catch (Exception e) {
            assertTrue(e instanceof IncorrectPathException);
            didThrow = true;
        }
        assertTrue(didThrow);

        // not full path
        try {
            ChildNumber ret = HierarchyUtils.getKeyIndexFromPath(ImmutableList.of(new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(32)), false);
            assertTrue(ret.getI() == 32);
        } catch (IncorrectPathException e) {
            e.printStackTrace();
        }

        try {
            ChildNumber ret = HierarchyUtils.getKeyIndexFromPath(ImmutableList.of(new ChildNumber(1),
                    new ChildNumber(1),
                    new ChildNumber(32)), false);
            assertTrue(ret.getI() == 32);
        } catch (IncorrectPathException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void generatePathUntilAccountsAddressTest() {
        String seedHex = "6167656e742074727920646976696465207375626d697420656c65637472696320626f72696e67206d656d626572206165726f62696320726573637565206372616e652067617264656e20666174";

        // idx 2
        DeterministicKey ret = HierarchyUtils.generatePathUntilAccountsAddress(Hex.decode(seedHex),
                2,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals("e2a6a3c4e44da695307172692e3e1be7a48e2bd3f8e1508cc4c9e96bf3807a8b"));

        // idx 33
        ret = HierarchyUtils.generatePathUntilAccountsAddress(Hex.decode(seedHex),
                33,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals("42467f8081e6823842ff4df8b08cd8cc284eebbb3b02233d5c06221ffbb2e088"));

        // idx 146
        ret = HierarchyUtils.generatePathUntilAccountsAddress(Hex.decode(seedHex),
                146,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals("61fcc4f97bd12ef36e2ebf528ce2621969731b73b6a5462d0752cd9ffd498430"));

        // Integer.MAX_VALUE = 2147483647
        ret = HierarchyUtils.generatePathUntilAccountsAddress(Hex.decode(seedHex),
                Integer.MAX_VALUE,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals("4ef3b2b257fb6276016b257c740facea1fba2cf1de0415f00d312c2c082fd47b"));

        // Integer.MAX_VALUE + 1000 = 2147484647, should throw IllegalArgumentException
        boolean didCatch = true;
        try {
            ret = HierarchyUtils.generatePathUntilAccountsAddress(Hex.decode(seedHex),
                    Integer.MAX_VALUE + 1000,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        }
        catch(Exception e) {
            didCatch = true;
            assertTrue(e instanceof IllegalArgumentException);
        }
        assertTrue(didCatch);
    }
}
