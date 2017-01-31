package com.bytabit.ft;

import com.bytabit.ft.nav.NavDrawer;
import com.bytabit.ft.nav.evt.NavEvent;
import com.bytabit.ft.nav.evt.QuitEvent;
import com.bytabit.ft.wallet.DepositView;
import com.bytabit.ft.wallet.WalletView;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.Swatch;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.javafx.sources.CompositeObservable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FiatTraderMobile extends MobileApplication {

    private static Logger LOG = LoggerFactory.getLogger(FiatTraderMobile.class);

    final public static String WALLET_VIEW = HOME_VIEW;
    final public static String DEPOSIT_VIEW = "Deposit";

    final public static String OFFER_VIEW = "Offers";
    final public static String ADD_OFFER_VIEW = "Add Offer";
    final public static String OFFER_DETAILS_VIEW = "Offer Details";

    final public static String TRADE_VIEW = "Trades"; //HOME_VIEW;

    final public static String PROFILE_VIEW = "Profile";
    final public static String PAYMENT_VIEW = "Payment Details";
    final public static String ADD_PAYMENT_VIEW = "Add Payment Detail";

    final public static String CONTRACT_VIEW = "Contracts";
    final public static String ADD_CONTRACT_VIEW = "Add Contract";

    final public static String MENU_LAYER = "Side Menu";

    final public static Executor EXECUTOR = Executors.newWorkStealingPool();

    private static CompositeObservable<NavEvent> navEventsComposite = new CompositeObservable<>();

    @Override
    public void init() {

//        addViewFactory(OFFER_VIEW, () -> (View) new OffersView().getView(OFFER_VIEW));
//        addViewFactory(ADD_OFFER_VIEW, () -> (View) new AddOfferView().getView(ADD_OFFER_VIEW));
//        addViewFactory(OFFER_DETAILS_VIEW, () -> (View) new OfferDetailsView().getView(OFFER_DETAILS_VIEW));

        addViewFactory(WALLET_VIEW, () -> (View) new WalletView().getView());
        addViewFactory(DEPOSIT_VIEW, () -> (View) new DepositView().getView());

//        addViewFactory(PAYMENT_VIEW, () -> new PaymentDetailsView(PAYMENT_VIEW));
//        addViewFactory(ADD_PAYMENT_VIEW, () -> new AddPaymentDetailView(ADD_PAYMENT_VIEW));

//        addViewFactory(PROFILE_VIEW, () -> new ProfileView(PROFILE_VIEW));

//        addViewFactory(CONTRACT_VIEW, () -> new ContractsView(CONTRACT_VIEW));
//        addViewFactory(ADD_CONTRACT_VIEW, () -> new AddContractView(ADD_CONTRACT_VIEW));

        addLayerFactory(MENU_LAYER, () -> new SidePopupView(new NavDrawer().getDrawer()));
    }

    @Override
    public void postInit(Scene scene) {

        Swatch.ORANGE.assignTo(scene);

        scene.getStylesheets().add(FiatTraderMobile.class.getResource("style.css").toExternalForm());
        ((Stage) scene.getWindow()).getIcons().add(new Image(FiatTraderMobile.class.getResourceAsStream("/logo.png")));
    }

    @Override
    public void stop() {
        navEventsComposite.add(Observable.just(new QuitEvent()));
        LOG.debug("Stop app");
    }

    public static CompositeObservable<NavEvent> getNavEventsComposite() {
        return navEventsComposite;
    }

    public static Observable<NavEvent> getNavEvents() {
        return navEventsComposite.toObservable();
    }
}
