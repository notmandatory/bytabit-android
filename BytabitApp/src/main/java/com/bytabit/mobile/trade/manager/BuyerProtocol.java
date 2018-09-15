package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.bitcoinj.core.Address;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class BuyerProtocol extends TradeProtocol {

    public BuyerProtocol() {
        super();
    }

    // 1.B: create trade, post created trade
    Single<Trade> createTrade(SellOffer sellOffer, BigDecimal buyBtcAmount) {

        return Single.zip(walletManager.getTradeWalletEscrowPubKey().toSingle(),
                profileManager.loadOrCreateMyProfile().map(Profile::getPubKey),
                walletManager.getTradeWalletDepositAddress().map(Address::toBase58).toSingle(),
                (buyerEscrowPubKey, buyerProfilePubKey, buyerPayoutAddress) ->
                        Trade.builder()
                                .version(null)
                                .role(Trade.Role.BUYER)
                                .status(Trade.Status.CREATED)
                                .escrowAddress(walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyerEscrowPubKey))
                                .createdTimestamp(LocalDateTime.now())
                                .sellOffer(sellOffer)
                                .buyRequest(new BuyRequest(buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress))
                                .build())
                .doOnSuccess(t -> walletManager.watchEscrowAddress(t.getEscrowAddress()).subscribe());
    }

    // 1.B: create trade, post created trade
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        // TODO handle seller canceling created trade
        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasPaymentRequest()) {
            updatedTrade = Maybe.just(trade.copyBuilder().paymentRequest(receivedTrade.getPaymentRequest()).build());
        }

        return updatedTrade;
    }

    // 2.B: buyer receives payment request, confirm funding tx
    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.just(trade);

        if (receivedTrade.hasArbitrateRequest()) {
            updatedTrade = Maybe.just(trade.copyBuilder().arbitrateRequest(receivedTrade.getArbitrateRequest()).build());
        }

        return updatedTrade;
    }

    // 3.B: buyer sends payment to seller and post payout request
    Maybe<Trade> sendPayment(Trade trade, String paymentReference) {

        // 1. create payout request with buyer payout signature

        return walletManager.getPayoutSignature(trade)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(payoutSignature -> new PayoutRequest(paymentReference, payoutSignature))
                .map(pr -> trade.copyBuilder().payoutRequest(pr).build());
    }

    Maybe<Trade> refundTrade(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> refundTxHash = walletManager.refundEscrowToSeller(trade);

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = refundTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.BUYER_SELLER_REFUND));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build());
    }
}
