package org.authenticator.walletCore;

import org.authenticator.Authenticator;
import org.authenticator.Utils.ExchangeProvider.Currency;
import org.authenticator.Utils.ExchangeProvider.ExchangeProvider;
import org.authenticator.Utils.ExchangeProvider.PricePoint;
import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;
import org.bitcoinj.core.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by alonmuroch on 1/19/15.
 */
public class ExtendedTransactionOutput extends TransactionOutput {

    public static ExtendedTransactionOutput fromTransactionOutput(TransactionOutput out) {
        return new ExtendedTransactionOutput(out.getParams(),
                out.getParentTransaction(),
                out.getValue(),
                out.getScriptPubKey().getToAddress(out.getParams()));
    }

    public ExtendedTransactionOutput(NetworkParameters params, Transaction parent, byte[] payload, int offset) throws ProtocolException {
        super(params, parent, payload, offset);
        init();
    }

    public ExtendedTransactionOutput(NetworkParameters params, Transaction parent, byte[] payload, int offset, boolean parseLazy, boolean parseRetain) throws ProtocolException {
        super(params, parent, payload, offset, parseLazy, parseRetain);
        init();
    }

    public ExtendedTransactionOutput(NetworkParameters params, Transaction parent, Coin value, Address to) {
        super(params, parent, value, to);
        init();
    }

    public ExtendedTransactionOutput(NetworkParameters params, Transaction parent, Coin value, ECKey to) {
        super(params, parent, value, to);
        init();
    }

    public ExtendedTransactionOutput(NetworkParameters params, Transaction parent, Coin value, byte[] scriptBytes) {
        super(params, parent, value, scriptBytes);
        init();
    }

    private void init() {
        extensions = new ArrayList<OutputExtension>();
    }

    //######################
    //
    //  Extensions API
    //
    //######################
    List<OutputExtension> extensions;
    public void registerExtension(OutputExtension extension) {
        extensions.add(extension);
    }

    public OutputExtension getExtensionByName(String name) {
        for (OutputExtension e : extensions)
            if (e.name().equals(name))
                return e;
        return null;
    }

    public interface OutputExtension {
        public String name();
        public void activate(Object... args);
        public boolean isActive();
    }

    //######################
    //
    //  values extension
    //
    //######################
    public static class ValuesExtension implements OutputExtension {
        private boolean isActive = false;
        private EndPoint result;
        private TransactionOutput output;
        private ExchangeProvider exchangeProvider;
        private Wallet wallet;

        @Override
        public String name() {
            return "Values Extension";
        }

        @Override
        public void activate(Object... args) {
            checkArgument(args[0] instanceof TransactionOutput);
            checkArgument(args[1] instanceof ExchangeProvider);
            checkArgument(args[2] instanceof Wallet);

            output = (TransactionOutput)args[0];
            exchangeProvider = (ExchangeProvider)args[1];
            wallet = (Wallet)args[2];

            result = calculateEndPointPrices(output, exchangeProvider, wallet);

            isActive = true;
        }

        @Override
        public boolean isActive() {
            return isActive;
        }

        //#########################
        //
        //  extension specific API
        //
        //#########################
        /**
         * Will return the current value in {@link org.authenticator.Utils.ExchangeProvider.Currency Currency} of the unspent output
         *
         * @return
         */
        public Currency getCurrentTotalValue() {
            try {
                long currentUnixTime = System.currentTimeMillis() / 1000L;
                float currentPrice = exchangeProvider.getExchangeRate(currentUnixTime);
                return Currency.valueOf((long)(((float) output.getValue().longValue() / (float) Coin.COIN.longValue()) * currentPrice) * Currency.ONE.getValue());
            }
            catch (ExchangeProviderNoDataException e) {
                e.printStackTrace();
            }
            return Currency.ZERO;
        }

        /**
         * returns the difference in value between the accumulated value of the inputs
         * (at the time they were transfered to the wallet) and their current value.
         * @return
         */
        public float getGainOrLoss() {
            List<Price> finalPrices = result.getPrices();
            Currency originalAccumulatedValue = Currency.ZERO;
            for(Price p:finalPrices) {
                float bitcoins = (float)p.sathosies / (float)Coin.COIN.longValue();
                float value = bitcoins * p.price;
                long valueInCents = (long)(value * (float)Currency.ONE.getValue());
                originalAccumulatedValue = originalAccumulatedValue.add(valueInCents);
            }

            Currency currentPrice = getCurrentTotalValue();
            return currentPrice.subtract(originalAccumulatedValue.getValue()).floatFriendlyValue();
        }

        /**
         * Will return a list of a list which contains, at index:
         * <ol start="0">
         *  <li>number of satoshies</li>
         *  <li>at what price (in cents)</li>
         *  <li>unix time</li>
         * </ol>
         *
         * @return
         */
        public List<List<Number>> getOriginPricePoints() {
            List<List<Number>> ret = new ArrayList<List<Number>>();
            for(Price p: result.getPrices()) {
                List<Number> row = new ArrayList<Number>();
                row.add(p.sathosies);
                row.add(p.price);
                row.add(p.time);
                ret.add(row);
            }
            return ret;
        }

        public String getGainOrLossString() {
            return "$" + new DecimalFormat("#.###").format(getGainOrLoss());
        }

        //####################
        //
        //  Private helper classes
        //
        //####################

        private class EndPoint {
            private TransactionOutput outPoint;
            public Coin tot;
            private List<Price> prices;

            public EndPoint(TransactionOutput outPoint) {
                this.outPoint = outPoint;
                tot = outPoint.getValue();
                prices = new ArrayList<Price>();
            }

            public Long getTotalSatoshies() {
                return outPoint.getValue().longValue();
            }

            public void addPrice(Price p) {
                List<Price> toAdd = new ArrayList<Price>();
                toAdd.add(p);
                addPrices(toAdd);
            }

            public void addPrices(List<Price> p) {
                prices.addAll(p);
            }

            /**
             * Will calculate the amount of coins entered from each source respectively
             * @return
             */
            public List<Price> getPrices() {
                List<Price> ret = new ArrayList<Price>(prices);
                BigDecimal tot = new BigDecimal(0);
                for(Price p: prices)
                    tot = tot.add(new BigDecimal(p.sathosies));

                for(Price p: ret) {
                    BigDecimal fraction = new BigDecimal(p.sathosies).divide(tot, new MathContext(7, RoundingMode.HALF_UP));
                    p.sathosies = fraction.multiply(new BigDecimal(outPoint.getValue().longValue()),
                            new MathContext(7, RoundingMode.HALF_UP)).longValue();
                }

                return prices;
            }

            @Override
            public String toString() {
                String ret = "";
                List<Price> r = getPrices();
                ret += "EndPoint " + tot.toFriendlyString() + ":\n";
                for (Price p: r) {
                    ret += "   - " + Coin.valueOf(p.sathosies).toFriendlyString() + ", at $" + p.price + "\n" ;
                }
                return ret;
            }
        }

        private class Price {
            /**
             * number of satoshies
             */
            public long time;
            /**
             * at what price
             */
            public float price;
            /**
             * When
             */
            public long sathosies;

            public JSONObject toJSONObject() throws JSONException {
                JSONObject ret = new JSONObject();
                ret.put("satoshies", sathosies);
                ret.put("timeStamp", time);
                ret.put("price", price);
                return ret;
            }
        }

        private EndPoint calculateEndPointPrices(TransactionOutput output, ExchangeProvider exchangeProvider, Wallet wallet) {
            try {
                EndPoint endPoint = new EndPoint(output);

                Transaction masterParentTx = output.getParentTransaction();

                if(masterParentTx.getValueSentFromMe(wallet).signum() == 0) {
                    Price p = new Price();
                    p.sathosies = output.getValue().longValue();
                    p.time = masterParentTx.getUpdateTime().getTime() / 1000L;
                    p.price = exchangeProvider.getExchangeRate(p.time);
                    endPoint.addPrice(p);
                }
                else {
                    for(TransactionInput in: masterParentTx.getInputs()) {
                        Coin inV = in.getOutpoint().getConnectedOutput().getValue();
                        List<Price> prices = calculatePricesRecursively(in,
                                exchangeProvider,
                                inV,
                                new ArrayList<Price>(),
                                wallet);
                        endPoint.addPrices(prices);
                    }
                }

                return endPoint;
            }
            catch (ExchangeProviderNoDataException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Giving a {@link org.bitcoinj.core.TransactionInput TransactionInput}, the method will trace back the blockchain to
         * find the original transaction which transferred coins to the wallet.<br>
         * <b>Original Tx</b> - a Tx which does not have any wallet inputs, meaning, an outside source transferred coins to the wallet.<br>
         * When the method gets to said origin transaction, it will add to a list the output amount from that transaction and the
         * price in USD of 1 bitcoin at the time of the transaction.
         *
         *
         * @param masterInput
         * @param value
         * @param lst
         * @param wallet
         * @return
         */
        private List<Price> calculatePricesRecursively(TransactionInput masterInput,
                                                       ExchangeProvider exchangeProvider,
                                                       Coin value, List<Price> lst,
                                                       Wallet wallet) throws ExchangeProviderNoDataException {
            // check if this Tx even sends coins from this wallet
            Transaction masterParentTx = masterInput.getParentTransaction();
            if(masterParentTx.getValueSentFromMe(wallet).signum() == 0) {
                Price p = new Price();
                p.sathosies = value.longValue();
                p.time = masterParentTx.getUpdateTime().getTime() / 1000L;
                p.price = exchangeProvider.getExchangeRate(p.time);
                lst.add(p);
                return lst;
            }

            // If not, recursively get to the origin Tx
            TransactionOutPoint masterOutPoint =  masterInput.getOutpoint();
            TransactionOutput masterConnectedOutput = masterOutPoint.getConnectedOutput();
            Transaction masterConnectedParentTx = masterConnectedOutput.getParentTransaction();

            if(masterConnectedParentTx.getValueSentFromMe(wallet).signum() > 0)
                for(TransactionInput in: masterConnectedParentTx.getInputs()) {
                    TransactionOutPoint oPoint = in.getOutpoint();
                    TransactionOutput   oPut = oPoint.getConnectedOutput();
                    calculatePricesRecursively(in, exchangeProvider, oPut.getValue(), lst, wallet);
                }
            else {
                Price p = new Price();
                p.sathosies = value.longValue();
                p.time = masterConnectedParentTx.getUpdateTime().getTime() / 1000L;
                p.price = exchangeProvider.getExchangeRate(p.time);
                lst.add(p);
            }
            return lst;
        }
    }
}
