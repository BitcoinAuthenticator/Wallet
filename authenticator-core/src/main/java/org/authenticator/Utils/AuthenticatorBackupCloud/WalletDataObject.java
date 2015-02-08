package org.authenticator.Utils.AuthenticatorBackupCloud;

import com.google.common.base.Preconditions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * A data object containing wallet data downloaded from the backup cloud.<br>
 * Will parse the data from a standard django tastypie GET response, e.g.,<br>
 *     {"meta": {"limit": 20, "next": null, "offset": 0, "previous": null, "total_count": 1},
 *     "objects": [{"id": 6, "name": "Wallet", "resource_uri": "/api/Wallets/6/", "user": "/api/Users/23/"}]}
 *
 * Created by alonmuroch on 2/8/15.
 */
class WalletDataObject implements Serializable {
    int id;
    String name;
    String resource_uri;
    String user_uri;

    public WalletDataObject(int id, String name, String resource_uri, String user_uri) {
        this.id = id;
        this.name = name;
        this.resource_uri = resource_uri;
        this.user_uri = user_uri;
    }

    public static WalletDataObject fromTastypieGetResponseJson(JSONObject data) throws JSONException {
        JSONArray objects = data.getJSONArray("objects");
        Preconditions.checkState(objects.length() == 1);

        JSONObject object = objects.getJSONObject(0);
        int id = object.getInt("id");
        String name = object.getString("name");
        String resource_uri = object.getString("resource_uri");
        String user_uri = object.getString("user");
        return new WalletDataObject(id, name, resource_uri, user_uri);
    }
}
