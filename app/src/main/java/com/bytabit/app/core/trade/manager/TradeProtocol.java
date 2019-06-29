/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.trade.manager;


import com.bytabit.app.core.arbitrate.manager.ArbitratorManager;
import com.bytabit.app.core.common.HashUtils;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.trade.model.ArbitrateRequest;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeRequest;
import com.bytabit.app.core.wallet.manager.WalletManager;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.math.RoundingMode;

import io.reactivex.Maybe;
import lombok.NonNull;

abstract class TradeProtocol {

    WalletManager walletManager;

    ArbitratorManager arbitratorManager;

    TradeService tradeService;

    public TradeProtocol(WalletManager walletManager, ArbitratorManager arbitratorManager,
                         TradeService tradeService) {
        this.walletManager = walletManager;
        this.arbitratorManager = arbitratorManager;
        this.tradeService = tradeService;
    }

    // CREATED, ACCEPTED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING

    abstract Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade);

    Maybe<Trade> handleAccepted(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasPaymentRequest()) {
            tradeBuilder.paymentRequest(receivedTrade.getPaymentRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    Maybe<Trade> handleFunding(Trade trade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (trade.getFundingTransactionWithAmt() != null) {
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }

    abstract Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade);

    Maybe<Trade> handlePaid(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasArbitrateRequest()) {
            tradeBuilder.arbitrateRequest(receivedTrade.getArbitrateRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasPayoutCompleted()) {
            tradeBuilder.payoutCompleted(receivedTrade.getPayoutCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    Maybe<Trade> requestArbitrate(Trade trade) {

        ArbitrateRequest.Reason reason;
        if (trade.getRole().equals(Trade.Role.SELLER)) {
            reason = ArbitrateRequest.Reason.NO_PAYMENT;
        } else if (trade.getRole().equals(Trade.Role.BUYER)) {
            reason = ArbitrateRequest.Reason.NO_BTC;
        } else {
            return Maybe.error(new TradeException("Invalid role, can't request arbitrate"));
        }

        ArbitrateRequest arbitrateRequest = new ArbitrateRequest(reason);

        return Maybe.just(trade)
                .map(t -> t.copyBuilder().arbitrateRequest(arbitrateRequest).build().withStatus());
    }

    Maybe<Trade> handleCompleting(Trade trade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (trade.getPayoutTransactionWithAmt() != null) {
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }

    Maybe<Trade> handleArbitrating(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasPayoutCompleted()) {
            updatedTrade = Maybe.just(tradeBuilder.payoutCompleted(receivedTrade.getPayoutCompleted()).build());
        }

        return updatedTrade;
    }

    Maybe<Trade> handleCanceled(Trade trade, Trade receivedTrade) {

        return Maybe.empty();
    }

    // Use Hex encoded Sha256 Hash of Offer.id and TakeOfferRequest properties
    public String getId(@NonNull Offer offer, @NonNull TradeRequest tradeRequest) {
        Sha256Hash sha256Hash = sha256Hash(offer, tradeRequest);
        return Base58.encode(sha256Hash.getBytes());
    }

    public Sha256Hash sha256Hash(@NonNull Offer offer, @NonNull TradeRequest tradeRequest) {

        return HashUtils.sha256Hash(offer.getId(), tradeRequest.getTakerProfilePubKey(),
                tradeRequest.getTakerEscrowPubKey(),
                tradeRequest.getBtcAmount().setScale(8, RoundingMode.HALF_UP),
                tradeRequest.getPaymentAmount().setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP));
    }
}
