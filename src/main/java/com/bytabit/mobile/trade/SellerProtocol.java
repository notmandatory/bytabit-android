package com.bytabit.mobile.trade;

import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_PAYMENT;
import static com.bytabit.mobile.trade.model.Trade.Status.PAID;

public class SellerProtocol extends TradeProtocol {

    @Inject
    private ProfileManager profileManager;

    public SellerProtocol() {
        super(LoggerFactory.getLogger(SellerProtocol.class));
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    public Trade handleCreated(Trade createdTrade) {

        Trade fundedTrade = null;

        // watch trade escrow address
        walletManager.createEscrowWallet(createdTrade.getEscrowAddress());

        // fund escrow and create paymentRequest
        PaymentRequest paymentRequest = fundEscrow(createdTrade);

        if (paymentRequest != null) {

            // 4. put payment request
            try {
                fundedTrade = Trade.builder()
                        .escrowAddress(createdTrade.getEscrowAddress())
                        .sellOffer(createdTrade.getSellOffer())
                        .buyRequest(createdTrade.getBuyRequest())
                        .paymentRequest(paymentRequest)
                        .build();

                tradeService.put(fundedTrade.getEscrowAddress(), fundedTrade).execute();

            } catch (IOException e) {
                log.error("Unable to PUT funded trade.", e);
                // TODO retry putting payment request
            }
        } else {
            log.error("Unable to fund trade.");
        }

        return fundedTrade;
    }

    // 2.S: seller fund escrow and post payment request
    private PaymentRequest fundEscrow(Trade trade) {

        // TODO verify escrow not yet funded ?
        try {
            // 1. fund escrow
            Transaction fundingTx = walletManager.fundEscrow(trade.getEscrowAddress(),
                    trade.getBtcAmount());

            // 2. create refund tx address and signature

            Address refundTxAddress = walletManager.getDepositAddress();
            String refundTxSignature = walletManager.getRefundSignature(trade, fundingTx, refundTxAddress);

            // 3. create payment request
            String paymentDetails = profileManager.retrievePaymentDetails(
                    trade.getCurrencyCode(),
                    trade.getPaymentMethod()).get();

            return PaymentRequest.builder()
                    .fundingTxHash(fundingTx.getHashAsString())
                    .paymentDetails(paymentDetails)
                    .refundAddress(refundTxAddress.toBase58())
                    .refundTxSignature(refundTxSignature)
                    .build();

        } catch (InsufficientMoneyException e) {
            log.error("Insufficient BTC to fund trade escrow.");
            // TODO let user know not enough BTC in wallet
            return null;
        }
    }

    @Override
    public Trade handleFunded(Trade createdTrade, Trade fundedTrade) {
        return null;
    }

    // 3.S: seller payout escrow to buyer and write payout details
    public Trade confirmPaymentReceived(Trade paidTrade) {

        Trade completedTrade = null;

        if (paidTrade.getStatus().equals(PAID)) {

            // 1. sign and broadcast payout tx
            try {
                String payoutTxHash = walletManager.payoutEscrowToBuyer(paidTrade);

                // 2. confirm payout tx and create payout completed
                PayoutCompleted payoutCompleted = PayoutCompleted.builder()
                        .payoutTxHash(payoutTxHash)
                        .reason(PayoutCompleted.Reason.SELLER_BUYER_PAYOUT)
                        .build();

                // 5. post payout completed
                try {

                    completedTrade = Trade.builder()
                            .escrowAddress(paidTrade.getEscrowAddress())
                            .sellOffer(paidTrade.getSellOffer())
                            .buyRequest(paidTrade.getBuyRequest())
                            .paymentRequest(paidTrade.getPaymentRequest())
                            .payoutRequest(paidTrade.getPayoutRequest())
                            .payoutCompleted(payoutCompleted)
                            .build();

                    tradeService.put(completedTrade.getEscrowAddress(), completedTrade).execute();

                } catch (IOException e) {
                    log.error("Can't post payout completed to server.");
                }

            } catch (InsufficientMoneyException e) {
                // TODO notify user
                log.error("Insufficient funds to payout escrow to buyer.");
            }
        }

        return completedTrade;
    }

    public void requestArbitrate(Trade currentTrade) {
        super.requestArbitrate(currentTrade, NO_PAYMENT);
    }
}