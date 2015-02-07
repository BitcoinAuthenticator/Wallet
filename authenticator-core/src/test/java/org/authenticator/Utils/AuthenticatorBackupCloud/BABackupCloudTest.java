package org.authenticator.Utils.AuthenticatorBackupCloud;

import com.google.protobuf.ByteString;
import com.ning.http.client.cookie.Cookie;
import junit.framework.TestCase;
import org.authenticator.Utils.AuthenticatorBackupCloud.exceptions.CannotRestoreFromBackupException;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.db.DBBase;
import org.authenticator.db.WalletDB;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoSettings;
import org.json.JSONException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.spongycastle.util.encoders.Hex;
import static org.junit.Assert.*;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InvalidObjectException;

/**
 * Created by alonmuroch on 1/31/15.
 */
public class BABackupCloudTest {
    @Test
    public void encryptDumpTest() throws IOException, JSONException, CryptoUtils.CouldNotEncryptPayload {
        BABackupCloud cb = new BABackupCloud();

        byte[] seed 	  			= Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
        byte[] salt 	  			= Hex.decode("d9d2d4fa6780f5b36c3b740a52d0547c6d195dea");
        SecretKey sk = CryptoUtils.getBaAESKey(salt, seed);

        DBBase b1 = new DBBase() {
            @Override
            public String dumpKey() { return "b1.AuthenticatorBackupCloud.Utils.authenticator.org"; }

            @Override
            public byte[] dumpToByteArray() {
                return "b1 dump".getBytes();
            }
        };
        DBBase b2 = new DBBase() {
            @Override
            public String dumpKey() { return "b2.AuthenticatorBackupCloud.Utils.authenticator.org"; }

            @Override
            public byte[] dumpToByteArray() {
                return "b2 dump".getBytes();
            }
        };
        DBBase[] dbs = new DBBase[]{ b1, b2 };

        String expected = "dc834f9dd77b9b1155087a2100c308bfa49b125d8bdb80246ed1ecf5c42dab3672b35942c3d8812a86c2d676bf47dc03aea72909d3468dc8e94fefb5e5786cb4739e14874008b26c96a2b4325da88f1ce256a8be982ca236697c2b8036c0083df1eb71249c64d50aba1a1085329731077016e925c33596700e9d29b8ed39ef993cb3b728d6351abea8a3f6100b35d99a1695c2b13a490b123972a7467d3d29cce256a8be982ca236697c2b8036c0083d8b8f34488e67633b5b40b373f345526601b85171edad9582c894ee5c3cde4ab85431b52eb1537bb236f101a75837b5ce2daeff3477d66530342b5af92cd650c43bade9a508e2bcaba992fb4638b65a4d6304b67a0d7059982cfa327428c083bf69baac65614c61ad075db50be3e5f3b37dcf0d23c60e26afabc6a64c68b530549a7489328d357ea49172df77fc7d591f";
        byte[] resBytes = cb.getEncryptedDump(sk, dbs);
        String resHex = Hex.toHexString(resBytes);
        assertEquals(expected, resHex);
    }

    @Test
    public void decryptDumpTest() throws CannotRestoreFromBackupException, IOException {
        String encryptedDumpHex = "dc834f9dd77b9b1155087a2100c308bfa49b125d8bdb80246ed1ecf5c42dab3672b35942c3d8812a86c2d676bf47dc03aea72909d3468dc8e94fefb5e5786cb4739e14874008b26c96a2b4325da88f1ce256a8be982ca236697c2b8036c0083df1eb71249c64d50aba1a1085329731077016e925c33596700e9d29b8ed39ef993cb3b728d6351abea8a3f6100b35d99a1695c2b13a490b123972a7467d3d29cce256a8be982ca236697c2b8036c0083d8b8f34488e67633b5b40b373f345526601b85171edad9582c894ee5c3cde4ab85431b52eb1537bb236f101a75837b5ce2daeff3477d66530342b5af92cd650c43bade9a508e2bcaba992fb4638b65a4d6304b67a0d7059982cfa327428c083bf69baac65614c61ad075db50be3e5f3b37dcf0d23c60e26afabc6a64c68b530549a7489328d357ea49172df77fc7d591f";

        byte[] seed 	  			= Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
        byte[] salt 	  			= Hex.decode("d9d2d4fa6780f5b36c3b740a52d0547c6d195dea");
        SecretKey sk = CryptoUtils.getBaAESKey(salt, seed);

        DBBase b1 = Mockito.spy(new DBBase(){
            @Override
            public String dumpKey() { return "b1.AuthenticatorBackupCloud.Utils.authenticator.org"; }

            @Override
            public void restoreFromBytes(byte[] data) throws IOException { }
            });
        DBBase b2 = Mockito.spy(new DBBase(){
            @Override
            public String dumpKey() { return "b2.AuthenticatorBackupCloud.Utils.authenticator.org"; }

            @Override
            public void restoreFromBytes(byte[] data) throws IOException { }
        });
        DBBase[] dbs = new DBBase[]{ b1, b2 };

        BABackupCloud cb = new BABackupCloud();
        byte[] encryptedPayload = Hex.decode(encryptedDumpHex);
        cb.restoreFromEncryptedDump(sk, encryptedPayload, dbs);



        ArgumentCaptor<byte[]> arg = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(b1, Mockito.atLeastOnce()).restoreFromBytes(arg.capture());
        byte[] res = arg.getValue();
        assertEquals(Hex.toHexString(res), Hex.toHexString("b1 dump".getBytes()));

        ArgumentCaptor<byte[]> arg2 = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(b2, Mockito.atLeastOnce()).restoreFromBytes(arg2.capture());
        res = arg2.getValue();
        assertEquals(Hex.toHexString(res), Hex.toHexString("b2 dump".getBytes()));
    }

    @Test
    public void serializedTest() throws InvalidObjectException {
        Cookie c1 = new Cookie("name1", "value1", "domain1", "rawValue1", "path1", 100000000, 45, true, true);
        Cookie c2 = new Cookie("name2", "value2", "domain2", "rawValue2", "path2", 100000000, 45, true, true);

        BABackupCloud cb = new BABackupCloud();
        cb.addCookie(c1); cb.addCookie(c2);

        String expected = "7b22636f6f6b696573223a5b2237623232373036313734363832323361323237303631373436383331323232633232363537383730363937323635373332323361333133303330333033303330333033303330326332323732363137373536363136633735363532323361323236343666366436313639366533313232326332323664363137383431363736353232336133343335326332323634366636643631363936653232336132323732363137373536363136633735363533313232326332323665363136643635323233613232366536313664363533313232326332323638373437343730346636653663373932323361373437323735363532633232373336353633373537323635323233613734373237353635326332323736363136633735363532323361323237363631366337353635333132323764222c2237623232373036313734363832323361323237303631373436383332323232633232363537383730363937323635373332323361333133303330333033303330333033303330326332323732363137373536363136633735363532323361323236343666366436313639366533323232326332323664363137383431363736353232336133343335326332323634366636643631363936653232336132323732363137373536363136633735363533323232326332323665363136643635323233613232366536313664363533323232326332323638373437343730346636653663373932323361373437323735363532633232373336353633373537323635323233613734373237353635326332323736363136633735363532323361323237363631366337353635333232323764225d7d";
        byte[] resBytes = cb.serialize();
        String resHex = Hex.toHexString(resBytes);
        assertEquals(expected, resHex);

    }

    @Test
    public void deserializedTest() throws InvalidObjectException {
        byte[] serialized = Hex.decode("7b22636f6f6b696573223a5b2237623232373036313734363832323361323237303631373436383331323232633232363537383730363937323635373332323361333133303330333033303330333033303330326332323732363137373536363136633735363532323361323236343666366436313639366533313232326332323664363137383431363736353232336133343335326332323634366636643631363936653232336132323732363137373536363136633735363533313232326332323665363136643635323233613232366536313664363533313232326332323638373437343730346636653663373932323361373437323735363532633232373336353633373537323635323233613734373237353635326332323736363136633735363532323361323237363631366337353635333132323764222c2237623232373036313734363832323361323237303631373436383332323232633232363537383730363937323635373332323361333133303330333033303330333033303330326332323732363137373536363136633735363532323361323236343666366436313639366533323232326332323664363137383431363736353232336133343335326332323634366636643631363936653232336132323732363137373536363136633735363533323232326332323665363136643635323233613232366536313664363533323232326332323638373437343730346636653663373932323361373437323735363532633232373336353633373537323635323233613734373237353635326332323736363136633735363532323361323237363631366337353635333232323764225d7d");

        ProtoConfig.Extenstion.Builder ex = ProtoConfig.Extenstion.newBuilder();
        ex.setExtentionID("313230363934363034");
        ex.setDescription("Cloud backup");
        ex.setData(ByteString.copyFrom(serialized));

        BABackupCloud cb = new BABackupCloud();
        cb.deserialize(ex.build());

        assertEquals(cb.getID(), "313230363934363034");
        assertTrue(cb.getCookies().size() == 2);
    }
}
