package com.bytabit.mobile.trade;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.offer.OfferManager;
import com.bytabit.mobile.offer.model.SellOffer;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class TradesPresenter {

    @Inject
    OfferManager offerManager;

    @FXML
    private View offersView;

    @FXML
    private CharmListView<SellOffer, String> offersListView;

    private FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {
        offersListView.setCellFactory((view) -> new CharmListCell<SellOffer>() {
            @Override
            public void updateItem(SellOffer o, boolean empty) {
                super.updateItem(o, empty);
                if (o != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s %s per BTC via %s", o.getPrice().toPlainString(), o.getCurrencyCode().toString(), o.getPaymentMethod().displayName());
                    String details = String.format("%s to %s %s",
                            o.getMinAmount(), o.getMaxAmount(), o.getCurrencyCode());
                    tile.textProperty().addAll(amount, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });
        //offersListView.setComparator((s1, s2) -> -1 * Integer.compare(s2.getDepth(), s1.getDepth()));

        offersView.getLayers().add(addOfferButton.getLayer());
        addOfferButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW));

        offersView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Offers");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));
            }

        });

        //offersListView.itemsProperty().addAll(offerManager.read());
        offersListView.itemsProperty().setValue(offerManager.getOffersObservableList());
        offersListView.selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            SellOffer viewOffer = offerManager.getViewOffer();
            viewOffer.setSellerEscrowPubKey(newValue.getSellerEscrowPubKey());
            viewOffer.setSellerProfilePubKey(newValue.getSellerProfilePubKey());
            viewOffer.setCurrencyCode(newValue.getCurrencyCode());
            viewOffer.setPaymentMethod(newValue.getPaymentMethod());
            viewOffer.setMinAmount(newValue.getMinAmount());
            viewOffer.setMaxAmount(newValue.getMaxAmount());
            viewOffer.setPrice(newValue.getPrice());
            MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
        });
        offerManager.readOffers();
    }

}