package org.authenticator.hierarchy;

import com.google.common.collect.ImmutableList;
import org.authenticator.hierarchy.exceptions.IncorrectPathException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.ProtoConfig;
import org.bitcoinj.core.ECKey;
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

    @Test
    public void generateAccountAddressHierarchyTest() {
        String seedHex = "6167656e742074727920646976696465207375626d697420656c65637472696320626f72696e67206d656d626572206165726f62696320726573637565206372616e652067617264656e20666174";

        ProtoConfig.ATAccount.ATAccountAddressHierarchy ret = HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                2,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        String expectedChainCode    = "48db2b0b546cb88b1fae522f45b293ede06282c8d62b544eac99985da1dec2f1";
        String expectedPubKey       = "02d9b84f5149495f1a223f103df9535c36e0e7fb462f3d3cb4570bf7b09072802a";
        assertTrue(Hex.toHexString(ret.getHierarchyChaincode().toByteArray()).equals(expectedChainCode));
        assertTrue(Hex.toHexString(ret.getHierarchyKey().toByteArray()).equals(expectedPubKey));

        ret = HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                33,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        expectedChainCode    = "cb5b6baf80ece9ff06e0243286d57f8cdf09e7d849499585136098a449999a68";
        expectedPubKey       = "03568de1b4de858d900f8a79fc7957059db5b2fc81df968670452aba22d55f09f4";
        assertTrue(Hex.toHexString(ret.getHierarchyChaincode().toByteArray()).equals(expectedChainCode));
        assertTrue(Hex.toHexString(ret.getHierarchyKey().toByteArray()).equals(expectedPubKey));

        ret = HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                146,
                AuthWalletHierarchy.HierarchyAddressTypes.External,
                AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        expectedChainCode    = "b31fbd276344a92a987922747188c0385db91bb84b8a5f093c3e7e5dab3849f9";
        expectedPubKey       = "034816b20a58328f325be5ea29be6dc44869c058e4eff98a735d00fe001e587f23";
        assertTrue(Hex.toHexString(ret.getHierarchyChaincode().toByteArray()).equals(expectedChainCode));
        assertTrue(Hex.toHexString(ret.getHierarchyKey().toByteArray()).equals(expectedPubKey));

        // Integer.MAX_VALUE + 1000 = 2147484647, should throw IllegalArgumentException
        boolean didCatch = true;
        try {
            HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
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

    @Test
    public void getPrivKeyFromAccountTest() {
        String seedHex = "6167656e742074727920646976696465207375626d697420656c65637472696320626f72696e67206d656d626572206165726f62696320726573637565206372616e652067617264656e20666174";

        // check simple derivation
        try {
            DeterministicKey ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    1,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            String expected = "8f15e252dee73985bb7942b4560b5a0102f84e187d3223750b873831fa142cae";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    100,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "72ed06cb78fd731e1567b4dcb1c0bf0330470f3c902a7ba056e7a6782e9d8d54";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    56,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "e76f0f947cc612afaa041c8011279edd8c74842892cfafb544cddb7140eb9330";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    23,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "9d1e8d8c059191965ccb7ccfb4578597f24cbe5a09d5fd92c40528593bd20578";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                   Integer.MAX_VALUE,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "ccd0ff11f0bfaae5b6f4fe88f6e403329b7879fcf5016cc66551ae4d7d19866d";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            /*
                Different account
             */
            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    13,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "01013974a8b576ae35d28640f75a4a87c49178cb04290311756be8dd5286fc6f";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    89,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "afd2941a46354cc4d25aadcb1fa6de3f3671facd8607559b0ff09d747b17c332";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

            ret = HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    556,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
            expected = "44eaf0f1b69ac6bf7ed33bde9559cf08a60c5b2ede91ff186b905600f24a9448";
            assertTrue(Hex.toHexString(ret.getPrivKeyBytes()).equals(expected));

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // check throws IllegalArgumentException
        boolean didThrow = false;
        try {
            HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    556,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE + 100,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            didThrow = true;
        }
        assertTrue(didThrow);

        // check throws IllegalArgumentException
        didThrow = false;
        try {
            HierarchyUtils.getPrivKeyFromAccount(Hex.decode(seedHex),
                    Integer.MAX_VALUE + 100,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    556,
                    AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            didThrow = true;
        }
        assertTrue(didThrow);
    }

    @Test
    public void getPubKeyFromAccountTest() {
        String seedHex = "6167656e742074727920646976696465207375626d697420656c65637472696320626f72696e67206d656d626572206165726f62696320726573637565206372616e652067617264656e20666174";

        // check simple derivation
        try {
            DeterministicKey ret = HierarchyUtils.getPubKeyFromAccount(1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    1,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            1,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            String expected = "03741e074d40be508445ebe18c6fd8bf9b51bcb733cf030baf3f4531b559cc2b65";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            ret = HierarchyUtils.getPubKeyFromAccount(1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    100,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            1,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "02897d73292f7ad759beeaf5344758bc0236e635e6f6af404cdbff707f05e5d6c6";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            ret = HierarchyUtils.getPubKeyFromAccount(1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    56,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            1,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "02fd5cbb7872d3d3aef0717728cdebf6ffafca22c83f3b4dad9d791411d3618161";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            ret = HierarchyUtils.getPubKeyFromAccount(1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    23,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            1,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "02c16d451937d89e23a308dc283793b4444f2b63629dbc7c012c192fd828c26106";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            ret = HierarchyUtils.getPubKeyFromAccount(1,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            1,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "03f2ae51f1736013479d0aa347f9e72f7f7e9dd13606a336d29c68e29656cd69ff";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            /*
                Different account
             */
            ret = HierarchyUtils.getPubKeyFromAccount(13,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            13,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "023eb677483f7c1868b0f046dd9acdfe458225cab92b95b65d567e423ca5a49799";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            ret = HierarchyUtils.getPubKeyFromAccount(89,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            89,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "020a94c6a94e01f23bb1302d00e4fe230db0a8b3e7414e7619e036609505c61e40";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

            ret = HierarchyUtils.getPubKeyFromAccount(556,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            556,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
            expected = "03e31cc9a4298a44da8673cb0d43fa5c9fed72fef5c25bfe4378896ccf83a9da96";
            assertTrue(Hex.toHexString(ret.getPubKey()).equals(expected));

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // check throws IllegalArgumentException
        boolean didThrow = false;
        try {
            HierarchyUtils.getPubKeyFromAccount(556,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    Integer.MAX_VALUE + 100,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            556,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            didThrow = true;
        }
        assertTrue(didThrow);

        // check throws IllegalArgumentException
        didThrow = false;
        try {
            HierarchyUtils.getPubKeyFromAccount(Integer.MAX_VALUE + 100,
                    AuthWalletHierarchy.HierarchyAddressTypes.External,
                    556,
                    HierarchyUtils.generateAccountAddressHierarchy(Hex.decode(seedHex),
                            556,
                            AuthWalletHierarchy.HierarchyAddressTypes.External,
                            AuthWalletHierarchy.HierarchyCoinTypes.CoinBitcoin));
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            didThrow = true;
        }
        assertTrue(didThrow);
    }
}
