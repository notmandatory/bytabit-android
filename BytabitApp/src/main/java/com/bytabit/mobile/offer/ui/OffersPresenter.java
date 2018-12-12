package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.Offer;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class OffersPresenter {

    @Inject
    OfferManager offerManager;

    @FXML
    View offersView;

    @FXML
    CharmListView<Offer, String> offersListView;

    FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {

        // setup view components

        addOfferButton.showOn(offersView);

        offersListView.setCellFactory(view -> new CharmListCell<Offer>() {
            @Override
            public void updateItem(Offer o, boolean empty) {
                super.updateItem(o, empty);
                if (o != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s @ %s %s per BTC", o.getOfferType().toString(), o.getPrice().toPlainString(), o.getCurrencyCode().toString());
                    String details = String.format("%s to %s %s via %s", o.getMinAmount(), o.getMaxAmount(), o.getCurrencyCode(), o.getPaymentMethod().displayName());
                    tile.textProperty().addAll(amount, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        // setup event observables

        JavaFxObservable.changesOf(offersView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    setAppBar();
                    clearSelection();
                });

        Observable.create(source ->
                addOfferButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a ->
                        MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW)
                );

        JavaFxObservable.changesOf(offersListView.selectedItemProperty())
                .map(Change::getNewVal)
                .subscribe(offer -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
                    offerManager.setSelectedOffer(offer);
                });

        Observable.concat(offerManager.getLoadedOffers(), offerManager.getUpdatedOffers())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(offers ->
                        offersListView.itemsProperty().setAll(offers)
                );

        offerManager.getCreatedOffer()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sellOffer ->
                        offersListView.itemsProperty().add(sellOffer)
                );

        offerManager.getRemovedOffer()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(offer ->
                        offersListView.itemsProperty().remove(offer)
                );
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Offers");
    }

    private void clearSelection() {
        offersListView.selectedItemProperty().setValue(null);
    }
}