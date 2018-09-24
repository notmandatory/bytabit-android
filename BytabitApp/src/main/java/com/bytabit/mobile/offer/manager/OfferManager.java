package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<SellOffer> selectedOfferSubject;

    private final Observable<SellOffer> selectedOffer;

    private final ConnectableObservable<SellOffer> lastSelectedOffer;

    private final PublishSubject<SellOffer> createdOffer;

    private final PublishSubject<SellOffer> removedOffer;

    private final SellOfferService sellOfferService;

    private Observable<List<SellOffer>> offers;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    @Inject
    TradeManager tradeManager;

    public OfferManager() {

        sellOfferService = new SellOfferService();

        selectedOfferSubject = PublishSubject.create();

        selectedOffer = selectedOfferSubject.share();

        lastSelectedOffer = selectedOffer.replay(1);

        createdOffer = PublishSubject.create();

        removedOffer = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        offers = Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> getLoadedOffers());
    }

    public Observable<List<SellOffer>> getLoadedOffers() {
        return sellOfferService.getAll().retryWhen(errors ->
                errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                .toObservable();
    }

    public Observable<List<SellOffer>> getUpdatedOffers() {
        return offers.share();
    }

    public void createOffer(CurrencyCode currencyCode, PaymentMethod paymentMethod, String arbitratorProfilePubKey,
                            BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

        Single.zip(profileManager.loadOrCreateMyProfile(), walletManager.getTradeWalletEscrowPubKey().toSingle(), (p, pk) ->
                SellOffer.builder()
                        .sellerProfilePubKey(p.getPubKey())
                        .sellerEscrowPubKey(pk)
                        .arbitratorProfilePubKey(arbitratorProfilePubKey)
                        .currencyCode(currencyCode)
                        .paymentMethod(paymentMethod)
                        .minAmount(minAmount)
                        .maxAmount(maxAmount)
                        .price(price.setScale(currencyCode.getScale(), RoundingMode.HALF_UP))
                        .build()
        )
                .flatMap(sellOfferService::put)
                .subscribe(createdOffer::onNext);
    }

    public void deleteOffer(String sellerEscrowPubKey) {
        sellOfferService.delete(sellerEscrowPubKey)
                .subscribe(removedOffer::onNext);
    }

    public void setSelectedOffer(SellOffer sellOffer) {
        selectedOfferSubject.onNext(sellOffer);
    }

    public Observable<SellOffer> getSelectedOffer() {
        return selectedOffer
                .doOnNext(sellOffer -> log.debug("Selected: {}", sellOffer));
    }

    public ConnectableObservable<SellOffer> getLastSelectedOffer() {
        return lastSelectedOffer;
    }

    public Observable<SellOffer> getCreatedOffer() {
        return createdOffer
                .doOnNext(sellOffer -> log.debug("Created: {}", sellOffer))
                .share();
    }

    public Observable<SellOffer> getRemovedOffer() {
        return removedOffer
                .doOnNext(sellOffer -> log.debug("Created: {}", sellOffer))
                .share();
    }

    public Maybe<Trade> createTrade(BigDecimal btcAmount) {
        return getLastSelectedOffer().autoConnect().firstOrError()
                .flatMapMaybe(sellOffer -> tradeManager.buyerCreateTrade(sellOffer, btcAmount));
    }
}