package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    private final BuyRequestService buyRequestService;

    private final PaymentRequestService paymentRequestService;

    private final ObservableList<Trade> tradesObservableList;

    private final Trade viewTrade;

    @Inject
    ProfileManager profileManager;

    String tradesPath = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    public TradeManager() {
        Retrofit buyRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(BuyRequest.class))
                .build();

        buyRequestService = buyRequestRetrofit.create(BuyRequestService.class);

        Retrofit paymentRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(PaymentRequest.class))
                .build();

        paymentRequestService = paymentRequestRetrofit.create(PaymentRequestService.class);

        tradesObservableList = FXCollections.observableArrayList();
        viewTrade = new Trade();
        readTrades();
    }

    public BuyRequest createBuyRequest(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                       String buyerEscrowPubKey, String buyerProfilePubKey,
                                       String buyerPayoutAddress) {

        try {
            BuyRequest newBuyRequest = new BuyRequest(sellOffer.getSellerEscrowPubKey(),
                    buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress);

            String spk = sellOffer.getSellerEscrowPubKey();
            BuyRequest createdBuyRequest = buyRequestService.createBuyRequest(spk, newBuyRequest).execute().body();
            LOG.debug("Created buyRequest: {}", createdBuyRequest);
            return createdBuyRequest;

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public PaymentRequest createPaymentRequest(Trade trade, String txHash, String paymentDetails) {
        PaymentRequest newPaymentRequest = new PaymentRequest();
        newPaymentRequest.setEscrowAddress(trade.getEscrowAddress());
        newPaymentRequest.setFundingTxHash(txHash);
        newPaymentRequest.setPaymentDetails(paymentDetails);

        String tradePath = tradesPath + trade.getEscrowAddress();
        String paymentRequestPath = tradePath + File.separator + "paymentRequest.json";
        try {
            FileWriter paymentRequestWriter = new FileWriter(paymentRequestPath);
            paymentRequestWriter.write(JSON.std.asString(newPaymentRequest));
            paymentRequestWriter.flush();

            // TODO save payment request to server
            PaymentRequest createdPaymentRequest =
                    paymentRequestService.createPaymentRequest(trade.getEscrowAddress(), newPaymentRequest).execute().body();

            LOG.debug("Created paymentRequest: {}", createdPaymentRequest);
            trade.setPaymentRequest(createdPaymentRequest);
            return createdPaymentRequest;

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public PaymentRequest readPaymentRequest(Trade trade) {

        String tradePath = tradesPath + trade.getEscrowAddress();
        String paymentRequestPath = tradePath + File.separator + "paymentRequest.json";
        try {
            PaymentRequest readPaymentRequest =
                    paymentRequestService.readPaymentRequests(trade.getEscrowAddress()).execute().body();
            LOG.debug("Read paymentRequest: {}", readPaymentRequest);

            FileWriter paymentRequestWriter = new FileWriter(paymentRequestPath);
            paymentRequestWriter.write(JSON.std.asString(readPaymentRequest));
            paymentRequestWriter.flush();

            trade.setPaymentRequest(readPaymentRequest);
            return readPaymentRequest;

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public void createTrades(String profilePubKey, List<? extends SellOffer> offers) {
        for (SellOffer sellOffer : offers) {
            try {
                if (sellOffer.getSellerProfilePubKey().equals(profilePubKey)) {
                    List<BuyRequest> buyRequests = buyRequestService.readBuyRequests(sellOffer.getSellerEscrowPubKey()).execute().body();
                    for (BuyRequest buyRequest : buyRequests) {
                        createTrade(sellOffer, buyRequest);
                    }
                }
            } catch (IOException ioe) {
                LOG.error("Error getting buy requests for sell offer {}", sellOffer.getSellerEscrowPubKey());
            }
        }
    }

    public Trade createTrade(SellOffer sellOffer, BuyRequest buyRequest) {

        try {
            String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());

            String tradePath = tradesPath + tradeEscrowAddress;
            File tradeDir = new File(tradePath);

            Trade trade = new Trade(sellOffer, buyRequest, tradeEscrowAddress);

            if (!tradeDir.exists()) {
                tradeDir.mkdirs();
                String sellOfferPath = tradePath + File.separator + "sellOffer.json";
                FileWriter sellOfferFileWriter = new FileWriter(sellOfferPath);
                sellOfferFileWriter.write(JSON.std.asString(sellOffer));
                sellOfferFileWriter.flush();
                String buyRequestPath = tradePath + File.separator + "buyRequest.json";
                FileWriter buyRequestFileWriter = new FileWriter(buyRequestPath);
                buyRequestFileWriter.write(JSON.std.asString(buyRequest));
                buyRequestFileWriter.flush();
                LOG.debug("Created new trade: {}", trade);
            }

            if (!tradesObservableList.contains(trade)) {
                tradesObservableList.add(trade);
                LOG.debug("Added trade to list: {}", trade);
            }

            return trade;

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public ObservableList<Trade> getTradesObservableList() {
        return tradesObservableList;
    }

    public Trade getViewTrade() {
        return viewTrade;
    }

    private void readTrades() {
        File tradesDir = new File(tradesPath);
        List<String> tradeIds;
        if (tradesDir.list() != null) {
            tradeIds = Arrays.asList(tradesDir.list());
        } else {
            tradeIds = new ArrayList<>();
        }
        for (String tradeId : tradeIds) {
            Trade trade = new Trade();
            trade.setEscrowAddress(tradeId);
            try {
                File sellOfferFile = new File(tradesPath + tradeId + File.separator + "sellOffer.json");
                FileReader sellOfferReader = new FileReader(sellOfferFile);
                SellOffer sellOffer = JSON.std.beanFrom(SellOffer.class, sellOfferReader);
                trade.setSellOffer(sellOffer);

                File buyRequestFile = new File(tradesPath + tradeId + File.separator + "buyRequest.json");
                FileReader buyRequestReader = new FileReader(buyRequestFile);
                BuyRequest buyRequest = JSON.std.beanFrom(BuyRequest.class, buyRequestReader);
                trade.setBuyRequest(buyRequest);

                tradesObservableList.add(trade);
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage());
            }
        }
    }


    public boolean activeSellerEscrowPubKey(String sellerEscrowPubKey) {
        for (Trade trade : tradesObservableList) {
            // TODO also check if in active status
            if (trade.getSellOffer().getSellerEscrowPubKey().equals(sellerEscrowPubKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean activeBuyerEscrowPubKey(String buyerEscrowPubKey) {
        for (Trade trade : tradesObservableList) {
            // TODO also check if in active status
            if (trade.getBuyRequest().getBuyerEscrowPubKey().equals(buyerEscrowPubKey)) {
                return true;
            }
        }
        return false;
    }

    public void removeTradeTx(TransactionWithAmt updatedTx) {

    }

    public void addTradeTx(TransactionWithAmt updatedTx) {
        BigDecimal defaultTxFee = new BigDecimal(Transaction.DEFAULT_TX_FEE.toPlainString());
        for (Trade trade : tradesObservableList) {
            if (trade.getEscrowAddress().equals(updatedTx.getOutputAddress())
                    && trade.getBuyRequest().getBtcAmount().add(defaultTxFee).equals(updatedTx.getBtcAmt())
                    && updatedTx.getConfidenceType().equals("BUILDING")
                    && trade.getPaymentRequest() == null) {

                if (trade.getSellOffer().getSellerProfilePubKey()
                        .equals(profileManager.profile().getPubKey())) {
                    // create, publish and save payment request
                    String paymentDetails = profileManager.retrievePaymentDetails(trade.getSellOffer().getCurrencyCode(), trade.getSellOffer().getPaymentMethod())
                            .get();
                    createPaymentRequest(trade, updatedTx.getHash(), paymentDetails);
                } else {
                    // read and save payment request
                    readPaymentRequest(trade);
                }
            }
        }
    }
}