package org.authenticator.Utils.AuthenticatorBackupCloud;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by alonmuroch on 2/8/15.
 */
public class WalletDataObjectTest {
    @Test
    public void parseFromTastypieTest() throws JSONException {
        JSONObject data = new JSONObject("{\"meta\": {\"limit\": 20, \"next\": null, \"offset\": 0, \"previous\": null, \"total_count\": 1},\"objects\": [{\"id\": 6, \"name\": \"Wallet\", \"resource_uri\": \"/api/Wallets/6/\", \"user\": \"/api/Users/23/\"}]}");
        WalletDataObject result = WalletDataObject.fromTastypieGetResponseJson(data);

        assertEquals(result.id, 6);
        assertEquals(result.name, "Wallet");
        assertEquals(result.resource_uri, "/api/Wallets/6/");
        assertEquals(result.user_uri, "/api/Users/23/");
    }

}
