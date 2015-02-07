package org.authenticator.network;

import com.ning.http.client.cookie.Cookie;
import org.bouncycastle.crypto.ec.ECElGamalDecryptor;
import org.json.JSONObject;

/**
 * Created by alonmuroch on 2/7/15.
 */
public class NingCookieSerializer {
    public static byte[] serialize(Cookie cookie) {
        try {
            JSONObject j = new JSONObject();

            j.put("name", cookie.getName());
            j.put("value", cookie.getValue());
            j.put("domain", cookie.getDomain());
            j.put("rawValue", cookie.getRawValue());
            j.put("path", cookie.getPath());
            j.put("expires", cookie.getExpires());
            j.put("maxAge", cookie.getMaxAge());
            j.put("secure", cookie.isSecure());
            j.put("httpOnly", cookie.isHttpOnly());

            return j.toString().getBytes();
        }
        catch (Exception e) {
            return null;
        }
    }

    public static Cookie fromBytes(byte[] data) {
        try {
            JSONObject j = new JSONObject(new String(data));

            String name = j.getString("name");
            String value = j.getString("value");
            String domain = j.getString("domain");
            String rawValue = j.getString("rawValue");
            String path = j.getString("path");
            long expires = j.getLong("expires");
            int maxAge = j.getInt("maxAge");
            boolean secure = j.getBoolean("secure");
            boolean httpOnly = j.getBoolean("httpOnly");

            return new Cookie(name, value, domain, rawValue, path, expires, maxAge, secure, httpOnly);
        }
        catch (Exception e) {
            return null;
        }
    }
}
