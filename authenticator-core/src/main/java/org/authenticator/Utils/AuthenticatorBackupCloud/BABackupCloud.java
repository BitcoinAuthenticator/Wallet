package org.authenticator.Utils.AuthenticatorBackupCloud;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import org.authenticator.Authenticator;
import org.authenticator.Utils.AuthenticatorBackupCloud.exceptions.CannotRestoreFromBackupException;
import org.authenticator.Utils.CryptoUtils;
import org.authenticator.Utils.EncodingUtils;
import org.authenticator.db.DBBase;
import org.authenticator.network.NingCookieSerializer;
import org.authenticator.protobuf.ConfigExtension;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.walletCore.exceptions.CannotWriteToConfigurationFileException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import javax.crypto.SecretKey;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a communicator class with the backup cloud service for restoring the wallet's metadata.<br>
 *
 * <b>Problem:</b> bitcoin authenticator uses an bip44 like hierarchy of accounts to handle its keys.<br>
 * An account can be a standard Pay-To_PubHash account where redeeming coins require only a single key<br>
 * pair or a paired account which is a Pay-To-Hash account where redeeming coins require both the wallet’s<br>
 * key pair and a second device's (the authenticator) key pair, other account types could be created in the future.<br>
 * Such a scheme creates a problem when the wallet needs to be restored only from its seed,<br>
 * the user needs to remember the precise index of the different accounts and the exact paired devices<br>
 * (account-authenticator pair).<br><br>
 *
 * <b>Non scalable work around:</b> the user is required to remember the indexes and reconstruct
 *all accounts when restoring from seed.<br><br>
 *
 * <b>Cloud Solution:</b> an anonymous cloud back-up scheme that stores all the necessary wallet’s meta data,<br>
 * encrypted, automatically. The solution works as follows:<br>
 * <ol>
 *     <li>the user logs in to the services using a user name and a password</li>
 *     <li>A new wallet is being created for the user (a user can have several wallets associated with its user ID)</li>
 *      <li>AES encryption key generated using {@link org.authenticator.Utils.CryptoUtils#encryptHexPayloadWithBaAESKey encryptHexPayloadWithBaAESKey}</li>
 *      <li>A dump of all the accounts + pairing data is serialised to a byte array (the payload) + checksum</li>
 *      <li>AES encryption on (payload | checksum), denominated encrypted_payload</li>
 *      <li>the encrypted_payload is posted to the cloud service</li>
 *      <li>every change in the account hierarchy will trigger an update which will run through stages 4-7.</li>
 * </ol>
 * <br>
 * When the user restores his wallet he will be asked to login using his user name and password. Once the user is verified, an http get request with the wallet’s id will provide all the necessary data to completely restore the wallet’s hierarchy without the need of re-pairing any of the accounts.
 *
 * Created by alonmuroch on 1/8/15.
 */
public class BABackupCloud implements ConfigExtension {
    Logger LOG = LoggerFactory.getLogger(BABackupCloud.class);

    final private String CLOUD_URL_API  = "http://127.0.0.1:8000/api/";
    final private String LOGIN          = "Users/login/";
    final private String LOGOUT         = "Users/logout/";
    final private String REGISTER       = "Users/";

    private List<Cookie> cookies;
    private WalletDataObject walletDataObject;
    private boolean isLoggedIn = false;

    private String extensionID;

    public BABackupCloud() {
        cookies = new ArrayList<>();
        isLoggedIn = false;
        extensionID = Hex.toHexString(new Integer(this.hashCode()).toString().getBytes());
    }

    private static BABackupCloud instance;
    public static BABackupCloud getInstance() {
        if(instance == null)
            instance = new BABackupCloud();
        return instance;
    }

    /**
     * Get wallet dbs byte dump, encrypt it for sending to server.
     *
     * @param secretKey
     * @param dbs
     * @return
     * @throws JSONException
     * @throws CryptoUtils.CouldNotEncryptPayload
     */
    byte[] getEncryptedDump(SecretKey secretKey, DBBase[] dbs) throws JSONException, CryptoUtils.CouldNotEncryptPayload {
        JSONObject obj = new JSONObject();
        for(DBBase db: dbs)
            obj.put(db.dumpKey(), Hex.toHexString(db.dumpToByteArray()));

        byte[] payloadHex = Hex.toHexString(obj.toString().getBytes()).getBytes();
        byte[] encryptedPayload = CryptoUtils.encryptPayloadWithChecksum(payloadHex, secretKey);
        return encryptedPayload;
    }

    /**
     * Giving a {@link org.authenticator.db.DBBase DbBase} dumps from server, restore wallet dbs.
     *
     * @param secretKey
     * @param encryptedPayload
     * @param dbs
     * @throws CannotRestoreFromBackupException
     */
    void restoreFromEncryptedDump(SecretKey secretKey, byte[] encryptedPayload, DBBase[] dbs) throws CannotRestoreFromBackupException {
        try {
            byte[] dataHex = CryptoUtils.decryptPayloadWithChecksum(encryptedPayload, secretKey);
            byte[] data = Hex.decode(dataHex);
            JSONObject obj = new JSONObject(new String(data));
            for(DBBase db: dbs) {
                String k = db.dumpKey();
                if(obj.has(k)) {
                    byte[] dump = Hex.decode(obj.getString(k));
                    db.restoreFromBytes(dump);
                }
            }
        }
        catch (Exception e) {
            throw new CannotRestoreFromBackupException(e.getMessage());
        }
    }

    //############################
    //
    //      API
    //
    //############################

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void addCookie(Cookie cookie) {
        if(cookies == null)
            cookies = new ArrayList<>();
        cookies.add(cookie);
    }

    public WalletDataObject getWalletDataObject() { return this.walletDataObject; }

    public void loginToCloud(byte[] userName, byte[] password, BABackupCloudListener listener) throws JSONException, IOException {
        LOG.info("Logging to cloud backup server ...");
        JSONObject object = new JSONObject();
        object.put("username", new String(userName));
        object.put("password", new String(password));
        EncodingUtils.postToURL(CLOUD_URL_API + LOGIN, null, object, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                if(response.getStatusCode() == 200) {
                    cookies = response.getCookies();
                    isLoggedIn = true;

                    persist();
                    LOG.info("Logged to cloud backup server");

                    if(listener != null)
                        listener.onSuccess();
                    return null;
                }

                LOG.info("Failed to log to cloud backup server");
                if(listener != null)
                    listener.onFailed("Could not log in to cloud, " + response.getStatusCode());
                return null;
            }
        });
    }

    public void logoutFromCloud(BABackupCloudListener listener) throws JSONException, IOException {
        LOG.info("Logging out from cloud backup server ...");
        EncodingUtils.postToURL(CLOUD_URL_API + LOGOUT, cookies, null, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                if(response.getStatusCode() == 200) {
                    cookies = null;
                    isLoggedIn = false;

                    persist();
                    LOG.info("Logged out from cloud backup server");

                    if(listener != null)
                        listener.onSuccess();
                    return null;
                }

                LOG.info("Failed to log out from cloud backup server");
                if(listener != null)
                    listener.onFailed("Could not log in to cloud, " + response.getStatusCode());
                return null;
            }
        });
    }

    public void registerToCloud(byte[] userName, byte[] password, BABackupCloudListener listener) throws JSONException, IOException {
        LOG.info("Registering new user to cloud backup server ...");
        JSONObject object = new JSONObject();
        object.put("username", new String(userName));
        object.put("password", new String(password));
        EncodingUtils.postToURL(CLOUD_URL_API + REGISTER, null, object, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                if(response.getStatusCode() == 201) {
                    cookies = response.getCookies();
                    isLoggedIn = true;

                    persist();
                    LOG.info("registering new user to cloud backup server");

                    if(listener != null)
                        listener.onSuccess();
                    return null;
                }

                LOG.info("Failed to registering new user to cloud backup server");
                if(listener != null)
                    listener.onFailed("Could not log in to cloud, " + response.getStatusCode());
                return null;
            }
        });
    }

    public void postBackupToCloud(SecretKey secretKey, DBBase[] dbs, BABackupCloudListener listener) throws JSONException, IOException, CryptoUtils.CouldNotEncryptPayload {
//        LOG.info("Posting backup dump to cloud backup server ...");
//
//        byte[] payload = getEncryptedDump(secretKey, dbs);
//
//        JSONObject object = new JSONObject();
//        object.put("username", new String(userName));
//        object.put("password", new String(password));
//        EncodingUtils.postToURL(CLOUD_URL_API + REGISTER, null, object, new AsyncCompletionHandler<Response>() {
//            @Override
//            public Response onCompleted(Response response) throws Exception {
//                if(response.getStatusCode() == 201) {
//                    cookies = response.getCookies();
//                    isLoggedIn = true;
//
//                    persist();
//                    LOG.info("registering new user to cloud backup server");
//
//                    if(listener != null)
//                        listener.onSuccess();
//                    return null;
//                }
//
//                LOG.info("Failed to registering new user to cloud backup server");
//                if(listener != null)
//                    listener.onFailed("Could not log in to cloud, " + response.getStatusCode());
//                return null;
//            }
//        });
    }


    //#############################
    //
    //  Config extension methods
    //
    //#############################

    @Override
    public String getID() {
        return extensionID;
    }

    @Override
    public String getDescription() {
        return "Cloud backup";
    }

    @Override
    public byte[] serialize() throws InvalidObjectException {
        try {
            JSONObject j = new JSONObject();

            // cookies
            JSONArray cookies = new JSONArray();
            if(BABackupCloud.this.cookies != null) {
                for(Cookie c: BABackupCloud.this.cookies) {
                    byte[] s = NingCookieSerializer.serialize(c);
                    cookies.put(Hex.toHexString(s));
                }
                j.put("cookies", cookies);
            }

            // wallet data object
            if(getWalletDataObject() != null) {
                ByteArrayOutputStream ba = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(ba);
                oos.writeObject(getWalletDataObject());
                oos.close();
                ba.close();
                byte[] walletDataObjectBytes = ba.toByteArray();
                j.put("walletDataObject", Hex.toHexString(walletDataObjectBytes));
            }


            return j.toString().getBytes();
        }
        catch (Exception e) {
            throw new InvalidObjectException(e.getMessage());
        }
    }

    @Override
    public void deserialize(ProtoConfig.Extenstion extenstion) throws InvalidObjectException {
        try {
            extensionID = extenstion.getExtentionID();

            JSONObject j = new JSONObject(new String(extenstion.getData().toByteArray()));

            // cookies
            BABackupCloud.this.cookies = new ArrayList<>();
            if(j.has("cookies")) {
                JSONArray cookies = j.getJSONArray("cookies");
                for(int i=0; i< cookies.length(); i++) {
                    byte[] cookieBytes = Hex.decode(cookies.getString(i));
                    Cookie c = NingCookieSerializer.fromBytes(cookieBytes);
                    if(c != null)
                        BABackupCloud.this.addCookie(c);
                }
            }

            // wallet data object
            if(j.has("walletDataObject")) {
                String dataString = j.getString("walletDataObject");
                byte[] decoded = Hex.decode(dataString);
                ByteArrayInputStream ba = new ByteArrayInputStream(decoded);
                ObjectInputStream oos = new ObjectInputStream(ba);
                walletDataObject = (WalletDataObject)oos.readObject();
                oos.close();
                ba.close();
            }
        }
        catch (Exception e) {
            throw new InvalidObjectException(e.getMessage());
        }
    }

    private void persist() throws CannotWriteToConfigurationFileException {
        Authenticator.getWalletOperation().updateExtension(this);
    }

    public interface BABackupCloudListener {
        public void onSuccess();
        public void onFailed(String reason);
    }
}
