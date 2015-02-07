package org.authenticator.network;

import com.ning.http.client.cookie.Cookie;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by alonmuroch on 2/7/15.
 */
public class NingCookieSerializerTest {
    @Test
    public void serializeTest() {
        String expected = "7b2270617468223a2270617468222c2265787069726573223a3130303030303030302c2272617756616c7565223a22646f6d61696e222c226d6178416765223a34352c22646f6d61696e223a2272617756616c7565222c226e616d65223a226e616d65222c22687474704f6e6c79223a747275652c22736563757265223a747275652c2276616c7565223a2276616c7565227d";

        Cookie c = new Cookie("name", "value", "domain", "rawValue", "path", 100000000, 45, true, true);
        byte[] serialized = NingCookieSerializer.serialize(c);
        String hexResult = Hex.toHexString(serialized);
        assertEquals(expected, hexResult);
    }

    @Test
    public void fromBytesTest() {
        String data = "7b2270617468223a2270617468222c2265787069726573223a3130303030303030302c2272617756616c7565223a22646f6d61696e222c226d6178416765223a34352c22646f6d61696e223a2272617756616c7565222c226e616d65223a226e616d65222c22687474704f6e6c79223a747275652c22736563757265223a747275652c2276616c7565223a2276616c7565227d";

        Cookie c = NingCookieSerializer.fromBytes(org.spongycastle.util.encoders.Hex.decode(data));
        assertEquals(c.getName(), "name");
        assertEquals(c.getValue(), "value");
        assertEquals(c.getDomain(), "domain");
        assertEquals(c.getRawValue(), "rawValue");
        assertEquals(c.getPath(), "path");
        assertEquals(c.getExpires(), 100000000);
        assertEquals(c.getMaxAge(), 45);
        assertEquals(c.isSecure(), true);
        assertEquals(c.isHttpOnly(), true);
    }
}
