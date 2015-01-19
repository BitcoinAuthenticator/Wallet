package org.wallet.apps;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
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
import org.authenticator.Utils.ExchangeProvider.*;
import org.authenticator.Utils.ExchangeProvider.exceptions.ExchangeProviderNoDataException;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.walletCore.ExtendedTransactionOutput;
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
                Exchanges.getInstance().currencies.get("USD").parseAndSetPriceDataFromBlockchainInfo(response.getResponseBody());

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
        myValuesAddressCol.setCellValueFactory(new PropertyValueFactory<TableDataClass, String>("address"));
        myValuesNumberOfBitcoinsCol.setCellValueFactory(new PropertyValueFactory<TableDataClass, String>("numberOfBitcoins"));
        myValuesDiffCol.setCellValueFactory(new PropertyValueFactory<TableDataClass, String>("diff"));
    }

    @FXML
    protected void close(){
        overlayUi.done();
    }

    private void calculateEndPointPrices(int accountIdx) throws CannotGetAddressException {
        List<TransactionOutput> data = Authenticator.getWalletOperation().getUnspentOutputsForAccount(accountIdx);

        new CalculateEndPointPrices(data,
                Exchanges.getInstance().currencies.get("USD"),
                Authenticator.getWalletOperation().getTrackedWallet()) {
            @Override
            protected void onPreExecute() {
                myValuesLoadingLbl.setVisible(true);
                animateLoadingLable(myValuesLoadingLbl, "Loading Data");
            }

            @Override
            protected void onPostExecute() {
                stopLoadingAnimation();
                myValuesLoadingPane.setVisible(false);

                List<TableDataClass> data = new ArrayList<TableDataClass>();
                for (ExtendedTransactionOutput e : result) {
                    TableDataClass d = new TableDataClass(e);
                    data.add(d);
                }
                myValuesTbl.setItems(FXCollections.observableArrayList(data));

                Platform.runLater(() -> {
                    Coin totBitcoins = Coin.ZERO;
                    Currency totValue = Currency.ZERO;
                    float totGainOrLost = 0;
                    float breakEven = 0;
                    for (ExtendedTransactionOutput e : result) {
                        ExtendedTransactionOutput.ValuesExtension extension = (ExtendedTransactionOutput.ValuesExtension) e.getExtensionByName("Values Extension");
                        totBitcoins = totBitcoins.add(e.getValue());
                        totValue = totValue.add(extension.getCurrentTotalValue().getValue());
                        totGainOrLost += extension.getGainOrLoss();
                    }
                    breakEven = (totValue.getValue() / Currency.ONE.getValue() + (float) totGainOrLost * (totGainOrLost < 0 ? -1 : 1)) /
                            ((float) totBitcoins.getValue() / (float) Coin.COIN.getValue());

                    long currentUnixTime = System.currentTimeMillis() / 1000L;
                    float currentPricePoint = 0;
                    try {
                        Exchange ex = Exchanges.getInstance().currencies.get("USD");
                        currentPricePoint = ex.getExchangeRate(currentUnixTime);
                    } catch (ExchangeProviderNoDataException e) {
                        e.printStackTrace();
                    }

                    lblTotNumberOfBitcoins.setText(totBitcoins.toFriendlyString());
                    lblBitcoinPrice.setText("$" + currentPricePoint);
                    lblUSDValue.setText("$" + new DecimalFormat("#.###").format(totValue.getValue() / Currency.ONE.getValue()));
                    lblGainOrLostValue.setText("$" + new DecimalFormat("#.###").format(totGainOrLost) + " (" +
                            new DecimalFormat("#.###").format(totGainOrLost / (totValue.getValue() / Currency.ONE.getValue())) + "%)");
                    lblBreakEven.setText(" $" + breakEven);
                });

            }
        }.execute();
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

    public class TableDataClass {
        private  ExtendedTransactionOutput output;
        public TableDataClass(ExtendedTransactionOutput output) {
            this.output = output;
        }

        public String getAddress() {
            return output.getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
        }

        public String getNumberOfBitcoins() {
            return output.getValue().toFriendlyString();
        }

        public String getDiff() {
            ExtendedTransactionOutput.ValuesExtension extension =  (ExtendedTransactionOutput.ValuesExtension)output.getExtensionByName("Values Extension");
            return extension.getGainOrLossString();
        }
    }

    public class  CalculateEndPointPrices extends AsyncTask {
        public List<ExtendedTransactionOutput> result;
        private ExchangeProvider exchangeProvider;
        private Wallet wallet;

        public CalculateEndPointPrices(List<TransactionOutput> outputs, ExchangeProvider exchangeProvider, Wallet wallet) {

            this.result = new ArrayList<ExtendedTransactionOutput>();
            for(TransactionOutput o:outputs) {
                this.result.add(ExtendedTransactionOutput.fromTransactionOutput(o));
            }
            this.exchangeProvider = exchangeProvider;
            this.wallet = wallet;
        }

        @Override
        protected void onPreExecute() { }

        @Override
        protected void doInBackground() {
            for (ExtendedTransactionOutput o: result) {
                ExtendedTransactionOutput.ValuesExtension extension = new ExtendedTransactionOutput.ValuesExtension();
                extension.activate(o, exchangeProvider, wallet);
                o.registerExtension(extension);
            }
        }

        @Override
        protected void onPostExecute() { }

        @Override
        protected void progressCallback(Object... params) { }
    }

}
