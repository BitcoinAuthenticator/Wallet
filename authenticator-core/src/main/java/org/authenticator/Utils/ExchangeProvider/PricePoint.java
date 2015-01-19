package org.authenticator.Utils.ExchangeProvider;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A data class to rappresent a price point of an exchange.<br>
 * Used in history exchange data
 *
 * Created by alonmuroch on 1/19/15.
 */
public class PricePoint implements Comparable {
    private Long unixTime;
    private double price;

    public PricePoint(long x, double y) {
        unixTime = x;
        price = y;
    }

    //  API
    public Long getUnixTime() {
        return unixTime;
    }

    public double getPrice() {
        return price;
    }

    //  Private
    @Override
    public int compareTo(Object o) {
        Long t2 = ((PricePoint)o).getUnixTime();
        return this.getUnixTime().intValue() - t2.intValue();
    }
}
