package org.wallet.apps;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.sun.tools.javac.util.Name;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.authenticator.Authenticator;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.walletCore.exceptions.CannotGetAddressException;
import org.bitcoinj.core.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wallet.ControllerHelpers.AsyncTask;
import org.wallet.Main;
import org.wallet.utils.BaseUI;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by alonmuroch on 1/14/15.
 */
public class MyBitcoinsAppController extends BaseUI {
    public Main.OverlayUI overlayUi;

    @FXML private Pane myValuesLoadingPane;
    @FXML private ChoiceBox myValuesCmbAccounts;
    @FXML private Label myValuesLoadingLbl;
    @FXML private Label lblTotNumberOfBitcoins;
    @FXML private Label lblBitcoinPrice;
    @FXML private Label lblUSDValue;
    @FXML private Label lblGainOrLostValue;
    @FXML private Label lblBreakEven;
    @FXML private TableView myValuesTbl;
    @FXML private TableColumn myValuesAddressCol;
    @FXML private TableColumn myValuesNumberOfBitcoinsCol;
    @FXML private TableColumn myValuesDiffCol;

    private ProtoConfig.ATAccount selectedAccount;
    private PriceData priceData;
    /**
     * contains all the trasactions that spent coins to a watched wallet address with no inputs from the wallet, thus, sent from outside
     */
    private ObservableList<EndPoint> endPoints;

    public void initialize() {
        super.initialize(MyBitcoinsAppController.class);

        myValuesCmbAccounts.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String t, String t1) {
                if(t1 != null && t1.length() > 0){
                    myValuesCmbAccounts.setPrefWidth(org.wallet.utils.TextUtils.computeTextWidth(new Font("Arial", 14),t1, 0.0D)+45);
                    selectedAccount = Authenticator.getWalletOperation().getAccountByName(t1);
                    try {
                        calculateEndPointPrices(selectedAccount.getIndex());
                    } catch (CannotGetAddressException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        animateLoadingLable(myValuesLoadingLbl, "Loading Data");
        downloadPriceDataFromBlockchainInfo(new AsyncCompletionHandler<Response>(){
            @Override
            public Response onCompleted(Response response) throws Exception{
                priceData = new PriceData(response.getResponseBody());

                /*
                 *  Set account
                 */
                {
                    selectedAccount = Authenticator.getWalletOperation().getActiveAccount().getActiveAccount();
                    List<ProtoConfig.ATAccount> all = Authenticator.getWalletOperation().getAllAccounts();
                    Platform.runLater(() -> {
                    myValuesCmbAccounts.getItems().clear();
                        for(ProtoConfig.ATAccount acc:all){
                            myValuesCmbAccounts.getItems().add(acc.getAccountName());
                        }
                        myValuesCmbAccounts.setTooltip(new Tooltip("Select account"));
                        myValuesCmbAccounts.setValue(selectedAccount.getAccountName());
                        myValuesCmbAccounts.setPrefWidth(org.wallet.utils.TextUtils.computeTextWidth(new Font("Arial", 14), myValuesCmbAccounts.getValue().toString(), 0.0D) + 60);
                    });

                }

                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                t.printStackTrace();
                stopLoadingAnimation();
                Platform.runLater(() -> {
                    myValuesLoadingLbl.setText("Error !");
                });
            }
        });

        // table view
        myValuesAddressCol.setCellValueFactory(new PropertyValueFactory<EndPoint,String>("address"));
        myValuesNumberOfBitcoinsCol.setCellValueFactory(new PropertyValueFactory<EndPoint,String>("numberOfBitcoins"));
        myValuesDiffCol.setCellValueFactory(new PropertyValueFactory<EndPoint,String>("diff"));
    }

    @FXML
    protected void close(){
        overlayUi.done();
    }

    private void calculateEndPointPrices(int accountIdx) throws CannotGetAddressException {
        List<TransactionOutput> data = Authenticator.getWalletOperation().getUnspentOutputsForAccount(accountIdx);

        new CalculateEndPointPrices(Authenticator.getWalletOperation().getTrackedWallet()) {
            @Override
            protected void onPreExecute() {
                myValuesLoadingLbl.setVisible(true);
                animateLoadingLable(myValuesLoadingLbl, "Loading Data");
            }

            @Override
            protected void onPostExecute() {
                stopLoadingAnimation();
                myValuesLoadingPane.setVisible(false);

                MyBitcoinsAppController.this.endPoints = this.result;
                myValuesTbl.setItems(MyBitcoinsAppController.this.endPoints);

                Platform.runLater(() -> {
                    Coin totBitcoins = Coin.ZERO;
                    float totValue = 0;
                    float totGainOrLost = 0;
                    float breakEven = 0;
                    for (MyBitcoinsAppController.EndPoint ep : MyBitcoinsAppController.this.endPoints) {
                        totBitcoins = totBitcoins.add(ep.tot);
                        totValue += ep.getCurrentTotalPrice();
                        totGainOrLost += ep.getNumericDiff();
                    }
                    if (totGainOrLost < 0)
                        breakEven = ((float)totValue + (float)totGainOrLost * -1)/ ((float)totBitcoins.getValue() / (float)Coin.COIN.getValue());
                    else
                        breakEven = ((float)totValue - (float)totGainOrLost) / ((float)totBitcoins.getValue() / (float)Coin.COIN.getValue());

                    long currentUnixTime = System.currentTimeMillis() / 1000L;
                    PricePoint currentPricePoint = priceData.getClosestPriceToUnixTime(currentUnixTime);

                    lblTotNumberOfBitcoins.setText(totBitcoins.toFriendlyString());
                    lblBitcoinPrice.setText("$" + currentPricePoint.price);
                    lblUSDValue.setText("$" + new DecimalFormat("#.###").format(totValue));
                    lblGainOrLostValue.setText("$" + new DecimalFormat("#.###").format(totGainOrLost) + " (" +
                            new DecimalFormat("#.###").format(totGainOrLost/ totValue) + "%)");
                    lblBreakEven.setText(" $" + breakEven);
                });

                // for debugging
//                        for (MyBitcoinsAppController.EndPoint ep : MyBitcoinsAppController.this.endPoints) {
//                            System.out.println(ep.toString());
//                        }
            }
        }
        .setOutputsData(data)
        .setPriceData(priceData)
        .execute();
    }

    //####################
    //
    //  Animations
    //
    //####################

    boolean shouldStop = false;
    private void animateLoadingLable(Label lbl, final String baseMsg) {
        shouldStop = false;
        Thread t = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg);
                        });
                        Thread.sleep(500);

                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg + " .");
                        });
                        Thread.sleep(500);

                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg + " ..");
                        });
                        Thread.sleep(500);

                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg + " ...");
                        });
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.setName("Loading animaiton thread");
        t.start();
    }

    private void stopLoadingAnimation() {
        shouldStop = true;
    }

    //####################
    //
    //  Private
    //
    //####################

    private void downloadPriceDataFromBlockchainInfo(AsyncCompletionHandler<Response> listener) {
        String url = "https://blockchain.info/charts/market-price?showDataPoints=false&timespan=all&show_header=true&daysAverageString=1&scale=0&format=json&address=";
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        try {
            asyncHttpClient.prepareGet(url).execute(listener);
        } catch (IOException e) {
            e.printStackTrace();
            listener.onThrowable(e);
        }
    }

    public class PriceData {
        private List<PricePoint> data;

        public PriceData(String rawData) {
            data = parsePriceDataFromBlockchainInfo(rawData);
        }

        //####################
        //
        //  API
        //
        //####################

        /**
         * Could return null if not found. Assuming the data is in ascending unix time order with no major time gaps.
         * @param unix
         * @return
         */
        public PricePoint getClosestPriceToUnixTime(Long unix) {
            int dataGap = 60*60*24; // 86400
            // 1)
            PricePoint firstPoint = data.get(0);
            Long diff = unix - firstPoint.getUnixTime() + dataGap;
            if(diff <= 0 || diff < dataGap)
                return firstPoint;

            //2)
            diff /= dataGap;
            if(diff >= data.size() -1)
                return data.get(data.size() - 1);
            int startIdx = Math.min(diff.intValue(), data.size());
            PricePoint closest = data.get(startIdx);
            for(int i = startIdx - 1; i > 0; i--) {
                PricePoint p = data.get(i);
                Long closestDiff = closest.getUnixTime() - unix;
                Long currentDiff = p.getUnixTime() - unix;

                if(closestDiff == 0)
                    return closest;
                if(currentDiff == 0)
                    return p;

                if(currentDiff < 0 && closestDiff > 0) {
                    currentDiff = Math.abs(currentDiff);
                    if(currentDiff > closestDiff)
                        return closest;
                    else
                        return p;
                }
                closest = p;
            }

            return null;
        }


        //####################
        //
        //  Private
        //
        //####################

        private List<PricePoint> parsePriceDataFromBlockchainInfo(String s) {
            try {
                List<PricePoint> ret = new ArrayList<PricePoint>();
                JSONObject ob = new JSONObject(s);

                JSONArray arr = ob.getJSONArray("values");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.getJSONObject(i);
                    ret.add(new PricePoint(row));
                }

                Collections.sort(ret);
                return ret;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public class PricePoint implements Comparable {
        private Long unixTime;
        private double price;

        public PricePoint(JSONObject data) throws JSONException {
            unixTime = data.getLong("x");
            price = data.getDouble("y");

            if(unixTime == 1303409705)
                System.out.print("");
        }

        //####################
        //
        //  API
        //
        //####################
        public Long getUnixTime() {
            return unixTime;
        }

        public double getPrice() {
            return price;
        }

        //####################
        //
        //  Private
        //
        //####################

        @Override
        public int compareTo(Object o) {
            Long t2 = ((PricePoint)o).getUnixTime();
            return this.getUnixTime().intValue() - t2.intValue();
        }
    }

    public class EndPoint {
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
                BigDecimal fraction = new BigDecimal(p.sathosies).divide(tot, new MathContext(7, RoundingMode.CEILING));
                p.sathosies = fraction.multiply(new BigDecimal(outPoint.getValue().longValue()),
                        new MathContext(8, RoundingMode.UNNECESSARY)).longValue();
            }

            return prices;
        }

        @Override
        public String toString() {
            String ret = "";
            List<MyBitcoinsAppController.Price> r = getPrices();
            ret += "EndPoint " + tot.toFriendlyString() + ":\n";
            for (MyBitcoinsAppController.Price p: r) {
                ret += "   - " + Coin.valueOf(p.sathosies).toFriendlyString() + ", at $" + p.price + "\n" ;
            }
            return ret;
        }

        //####################################
        //
        //  methods for the table view
        //
        //####################################

        public String getAddress() {
            return outPoint.getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
        }

        public String getNumberOfBitcoins() {
            return tot.toFriendlyString();
        }

        public double getCurrentTotalPrice() {
            long currentUnixTime = System.currentTimeMillis() / 1000L;
            PricePoint currentPricePoint = priceData.getClosestPriceToUnixTime(currentUnixTime);
            return ((float)tot.longValue() / (float)Coin.COIN.longValue()) * currentPricePoint.price;
        }

        public double getNumericDiff() {
            List<Price> finalPrices = getPrices();
            double originalAccumulatedPrices = 0;
            for(Price p:finalPrices) {
                originalAccumulatedPrices += ((double)p.sathosies / (double)Coin.COIN.longValue()) * p.price;
            }

            double currentPrice = getCurrentTotalPrice();
            return currentPrice - originalAccumulatedPrices;
        }

        public String getDiff() {
            return "$" + new DecimalFormat("#.###").format(getNumericDiff());
        }

    }

    public class Price {
        public Long sathosies;
        public long time;
        public double price;
    }

    public class  CalculateEndPointPrices extends AsyncTask {
        public ObservableList<EndPoint> result;
        private List<TransactionOutput> outputsData;
        private PriceData priceData;
        private Wallet wallet;

        public CalculateEndPointPrices(Wallet wallet) {

            this.wallet = wallet;
        }

        public CalculateEndPointPrices setOutputsData(List<TransactionOutput> data) {
            outputsData = data;
            return this;
        }

        public CalculateEndPointPrices setPriceData(PriceData priceData) {
            this.priceData = priceData;
            return this;
        }

        @Override
        protected void onPreExecute() { }

        @Override
        protected void doInBackground() {
            List<EndPoint> ret = calculateEndPointPrices(outputsData, priceData, wallet);
            result = FXCollections.observableArrayList(ret);
        }

        @Override
        protected void onPostExecute() { }

        @Override
        protected void progressCallback(Object... params) { }
    }

    public List<EndPoint> calculateEndPointPrices(List<TransactionOutput> data, PriceData priceData, Wallet wallet) {
        if(data == null || priceData == null) return null;

        List<EndPoint> ret = new ArrayList<EndPoint>();
        for (TransactionOutput out: data) {
            EndPoint endPoint = new EndPoint(out);

            Transaction masterParentTx = out.getParentTransaction();

            if(masterParentTx.getValueSentFromMe(wallet).signum() == 0) {
                Price p = new Price();
                p.sathosies = out.getValue().longValue();
                p.time = masterParentTx.getUpdateTime().getTime() / 1000L;
                p.price = priceData.getClosestPriceToUnixTime(p.time).price;
                endPoint.addPrice(p);
            }
            else {
                for(TransactionInput in: masterParentTx.getInputs()) {
                    Coin inV = in.getOutpoint().getConnectedOutput().getValue();
                    List<Price> prices = calculatePricesRecursively(in, priceData, inV, new ArrayList<Price>(), wallet);
                    endPoint.addPrices(prices);
                }
            }

            ret.add(endPoint);
        }

        return ret;
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
    private List<Price> calculatePricesRecursively(TransactionInput masterInput, PriceData priceData, Coin value, List<Price> lst, Wallet wallet) {
        // check if this Tx even sends coins from this wallet
        Transaction masterParentTx = masterInput.getParentTransaction();
        if(masterParentTx.getValueSentFromMe(wallet).signum() == 0) {
            Price p = new Price();
            p.sathosies = value.longValue();
            p.time = masterParentTx.getUpdateTime().getTime() / 1000L;
            p.price = priceData.getClosestPriceToUnixTime(p.time).price;
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
                calculatePricesRecursively(in, priceData, oPut.getValue(), lst, wallet);
            }
        else {
            Price p = new Price();
            p.sathosies = value.longValue();
            p.time = masterConnectedParentTx.getUpdateTime().getTime() / 1000L;
            p.price = priceData.getClosestPriceToUnixTime(p.time).price;
            lst.add(p);
        }
        return lst;
    }

}
