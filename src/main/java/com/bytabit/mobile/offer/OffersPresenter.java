package com.bytabit.mobile.offer;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.TradeManager;
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

public class OffersPresenter {

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeManager tradeManager;

    @FXML
    private View offersView;

    @FXML
    private CharmListView<SellOffer, String> offersListView;

    private FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {

        // make sure profile initialized, do this here because this is HOME view
        if (profileManager.getPubKeyProperty() == null) {
            MobileApplication.getInstance().switchView(BytabitMobile.PROFILE_VIEW);
        }

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

        //offersListView.itemsProperty().addAll(offerManager.get());
        offersListView.itemsProperty().setValue(offerManager.getSellOffersObservableList());
        offersListView.selectedItemProperty().addListener((obs, oldValue, selectedSellOffer) -> {
            if (selectedSellOffer != null) {
                String sellerEscrowPubKey = selectedSellOffer.getSellerEscrowPubKey();
                offersListView.selectedItemProperty().setValue(null);

                if (tradeManager.activeSellerEscrowPubKey(sellerEscrowPubKey)) {
                    // TODO go to active trade details view for this sell offer
                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_VIEW);
                } else {
                    offerManager.getSelectedSellOfferProperty().setValue(selectedSellOffer);
                    offerManager.getSellerEscrowPubKeyProperty().setValue(sellerEscrowPubKey);
                    offerManager.getSellerProfilePubKeyProperty().setValue(selectedSellOffer.getSellerProfilePubKey());
                    offerManager.getArbitratorProfilePubKeyProperty().setValue(selectedSellOffer.getArbitratorProfilePubKey());
                    offerManager.getCurrencyCodeProperty().setValue(selectedSellOffer.getCurrencyCode());
                    offerManager.getPaymentMethodProperty().setValue(selectedSellOffer.getPaymentMethod());
                    offerManager.getMinAmountProperty().setValue(selectedSellOffer.getMinAmount());
                    offerManager.getMaxAmountProperty().setValue(selectedSellOffer.getMaxAmount());
                    offerManager.getPriceProperty().setValue(selectedSellOffer.getPrice());
                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
                }
            }
        });

        offerManager.readOffers();
    }

}