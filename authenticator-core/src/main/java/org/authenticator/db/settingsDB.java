package org.authenticator.db;

import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoSettings;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by alonmuroch on 2/10/15.
 */
public class SettingsDB extends DBBase {

    public SettingsDB(String filePath) throws IOException {
        super(filePath);
    }

    @Override
    public byte[] dumpToByteArray() {
        return getSettingsBuilder().build().toByteArray();
    }

    @Override
    public String dumpKey() { return "SettingsDb.db.authenticator.org"; }

    @Override
    public void restoreFromBytes(byte[] data) throws IOException {
        ProtoSettings.ConfigSettings settings = ProtoSettings.ConfigSettings.parseFrom(data);
        writeSettingsFile(settings.toBuilder());
    }

    //#####################################
    //
    //				General
    //
    //#####################################


    private synchronized ProtoConfig.AuthenticatorConfiguration.Builder getConfigFileBuilder() {
        ProtoConfig.AuthenticatorConfiguration.Builder auth = ProtoConfig.AuthenticatorConfiguration.newBuilder();
        try{ auth.mergeDelimitedFrom(new FileInputStream(filePath)); }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return auth;
    }

    private synchronized ProtoSettings.ConfigSettings.Builder getSettingsBuilder(){
        return getConfigFileBuilder().getConfigSettingsBuilder();
    }

    private synchronized void writeConfigFile(ProtoConfig.AuthenticatorConfiguration.Builder auth) throws IOException{
        FileOutputStream output = new FileOutputStream(filePath);
        auth.build().writeDelimitedTo(output);
        output.close();
    }

    private synchronized void writeSettingsFile(ProtoSettings.ConfigSettings.Builder settings) throws IOException{
        ProtoConfig.AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();

        FileOutputStream output = new FileOutputStream(filePath);
        auth.setConfigSettings(settings.build());
        auth.build().writeDelimitedTo(output);
        output.close();
    }

    //#####################################
    //
    //				General
    //
    //#####################################

    public void setAccountUnit(ProtoSettings.BitcoinUnit value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setAccountUnit(value);
        writeSettingsFile(s);
    }

    public ProtoSettings.BitcoinUnit getAccountUnit() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getAccountUnit();
    }

    public void setDecimalPoint(int value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setDecimalPoints(value);
        writeSettingsFile(s);
    }

    public int getDecimalPoint() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getDecimalPoints();
    }

    public void setLocalCurrencySymbol(String value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setLocalCurrencySymbol(value);
        writeSettingsFile(s);
    }

    public String getLocalCurrencySymbol() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getLocalCurrencySymbol();
    }

    public void setLanguage(ProtoSettings.Languages value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setLanguage(value);
        writeSettingsFile(s);
    }

    public ProtoSettings.Languages getLanguage() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getLanguage();
    }

    public void setDefaultFee(long value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setDefaultFee(value);
        writeSettingsFile(s);
    }

    public long getDefaultFee() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getDefaultFee();
    }

    public void setIsUsingTOR(boolean value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setTOR(value);
        writeSettingsFile(s);
    }

    public boolean getIsUsingTOR() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getTOR();
    }

    public void setIsConnectingToLocalHost(boolean value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setConnectOnLocalHost(value);
        writeSettingsFile(s);
    }

    public boolean getIsConnectingToLocalHost() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getConnectOnLocalHost();
    }

    public void setIsConnectingToTrustedPeer(boolean value, String peerIP) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setConnectToTrustedPeer(value);
        s.setTrustedPeerIP(peerIP);
        writeSettingsFile(s);
    }

    public void setNotConnectingToTrustedPeer() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setConnectToTrustedPeer(false);
        s.setTrustedPeerIP("");
        writeSettingsFile(s);
    }

    public boolean getIsConnectingToTrustedPeer() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getConnectToTrustedPeer();
    }

    public String getTrustedPeerIP() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getTrustedPeerIP();
    }

    public void setBloomFilterFalsePositiveRate(float value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setBloomFilterFalsePositiveRate(value);
        writeSettingsFile(s);
    }

    public float getBloomFilterFalsePositiveRate() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getBloomFilterFalsePositiveRate();
    }

    public void setIsPortForwarding(boolean value) throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setPortForwarding(value);
        writeSettingsFile(s);
    }

    public boolean getIsPortForwarding() throws IOException{
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getPortForwarding();
    }

    public boolean getShouldBackupToCloud() {
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        return s.getBackupToCloud();
    }

    public void setShouldBackupToCloud(boolean value) throws IOException {
        ProtoSettings.ConfigSettings.Builder s = getSettingsBuilder();
        s.setBackupToCloud(value);
        writeSettingsFile(s);
    }
}

