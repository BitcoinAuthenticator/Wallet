package org.authenticator.utilsTest.AuthenticatorBackupCloud;

import junit.framework.TestCase;
import org.authenticator.Utils.AuthenticatorBackupCloud.BABackupCloud;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.db.WalletDb;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoSettings;
import org.json.JSONException;
import org.junit.Test;
import org.mockito.Mockito;
import org.spongycastle.util.encoders.Hex;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * Created by alonmuroch on 1/31/15.
 */
public class BABackupCloudTest extends TestCase{
    @Test
    public void test() throws IOException, JSONException {
        WalletDb wmMock = Mockito.mock(WalletDb.class);
        Mockito.when(wmMock.getConfigFileBuilder()).thenReturn(generateATConfig());

        BABackupCloud cb = new BABackupCloud();

        byte[] seed 	  			= Hex.decode("55967fdf0e7fd5f0c78e849f37ed5b9fafcc94b5660486ee9ad97006b6590a4d");
        byte[] salt 	  			= Hex.decode("d9d2d4fa6780f5b36c3b740a52d0547c6d195dea");
        SecretKey sk = CryptoUtils.getBaAESKey(salt, seed);

    }

    private ProtoConfig.AuthenticatorConfiguration.Builder generateATConfig() {
        ProtoConfig.AuthenticatorConfiguration.Builder builder = ProtoConfig.AuthenticatorConfiguration.newBuilder();

        /*
         * ConfigOneNameProfile
         */
        ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile.Builder onBuilder = ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile.newBuilder();
        onBuilder.setOnename("one name");
        onBuilder.setOnenameFormatted("one name formatted");
        onBuilder.setOnenameAvatarURL("avatar URL");
        onBuilder.setOnenameAvatarFilePath("avatar file path");
        onBuilder.setBitcoinAddress("bitcoin address");
        builder.setConfigOneNameProfile(onBuilder);

        /*
         * SavedTX
         */
        ProtoConfig.AuthenticatorConfiguration.SavedTX.Builder txBuilder = ProtoConfig.AuthenticatorConfiguration.SavedTX.newBuilder();
        txBuilder.setTxid("tx id");
        txBuilder.setToFrom("to from");
        txBuilder.setDescription("description");
        builder.addConfigSavedTXData(txBuilder);

        /*
        *   ConfigSettings
        */
        ProtoSettings.ConfigSettings.Builder csBuilder = ProtoSettings.ConfigSettings.newBuilder();
        csBuilder.setLocalCurrencySymbol("USD");
        builder.setConfigSettings(csBuilder);

        return builder;
    }
}
