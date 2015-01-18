package org.authenticator.Utils.ExchangeProvider;

import com.google.common.math.LongMath;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.MonetaryFormat;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.math.LongMath.checkedMultiply;
import static com.google.common.math.LongMath.checkedPow;
import static com.google.common.math.LongMath.divide;

/**
 * Created by alonmuroch on 1/18/15.
 */
public class Currency implements Monetary, Comparable<Currency> {


    /**
     * Zero .
     */
    public static final Currency ZERO = Currency.valueOf(0);

    /**
     * One.
     */
    public static final Currency CENT = Currency.valueOf(1);

    /**
     * One Bitcoin.
     */
    public static final Currency ONE = Currency.valueOf(CENT.getValue()).multiply(100);

    /**
     * The number of cents of this monetary value.
     */
    public final long value;

    public Currency(long cents) {
        this.value = cents;
    }

    /**
     * Convert an amount expressed in the way humans are used to into satoshis.
     */
    public static Currency valueOf(final int coins, final int cents) {
        checkArgument(cents < 100);
        checkArgument(cents >= 0);
        checkArgument(coins >= 0);
        return Currency.valueOf(Currency.ONE.getValue()).multiply(coins).add(cents);
    }

    public static Currency valueOf(final long cents) {
        return new Currency(cents);
    }

    public Currency multiply(long factor) {
        return Currency.valueOf(LongMath.checkedMultiply(this.value, factor));
    }

    public Currency divide(final long divisor) {
        return Currency.valueOf(this.value / divisor);
    }

    public long divide(final Currency divisor) {
        return this.value / divisor.value;
    }

    public Currency add(long cents) {
        return Currency.valueOf(LongMath.checkedAdd(this.value, cents));
    }

    public Currency subtract(long cents) {
        return new Currency(this.value - cents);
    }

    @Override
    public int smallestUnitExponent() {
        return 3;
    }

    @Override
    public long getValue() {
        return value;
    }

    @Override
    public int signum() {
        if (this.value == 0)
            return 0;
        return this.value < 0 ? -1 : 1;
    }


    @Override
    public String toString() {
        return Long.toString(value) + (currencyCode!= null? " " + currencyCode:"") + " Cents";
    }

    public String toFriendlyString() {
        return Float.toString((float)(value / Currency.ONE.getValue())) + (currencyCode!= null? " " + currencyCode:"");
    }

    private String currencyCode;
    public void setCurrencyCode(String code) { currencyCode = code;}

    @Override
    public int compareTo(final Currency other) {
        if (this.value == other.value)
            return 0;
        return this.value > other.value ? 1 : -1;
    }
}
