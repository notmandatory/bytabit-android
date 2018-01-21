package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.PaymentsResult.PaymentDetailsResult;
import com.bytabit.mobile.profile.action.PaymentDetailsAction;
import com.bytabit.mobile.profile.event.PaymentDetailsEvent;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PaymentsPresenter {

    private static Logger LOG = LoggerFactory.getLogger(PaymentsPresenter.class);

    @Inject
    private ProfileManager profileManager;

    @FXML
    private View paymentsView;

    @FXML
    private CharmListView<PaymentDetails, String> paymentDetailsListView;

    private FloatingActionButton addPaymentDetailsButton = new FloatingActionButton();

    public void initialize() {
        LOG.debug("initialize payment details list presenter");

        // setup view components

        paymentDetailsListView.setCellFactory((view) -> new CharmListCell<PaymentDetails>() {
            @Override
            public void updateItem(PaymentDetails paymentDetails, boolean empty) {
                super.updateItem(paymentDetails, empty);
                if (paymentDetails != null && !empty) {
                    ListTile tile = new ListTile();
                    String currencyCodeMethod = String.format("%s via %s",
                            paymentDetails.getCurrencyCode().name(),
                            paymentDetails.getPaymentMethod().displayName());
                    String details = String.format("%s", paymentDetails.getPaymentDetails());
                    tile.textProperty().addAll(currencyCodeMethod, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        paymentDetailsListView.setComparator((s1, s2) -> s2.getCurrencyCode().compareTo(s1.getCurrencyCode()));

        addPaymentDetailsButton.setText(MaterialDesignIcon.ADD.text);

        paymentsView.getLayers().add(addPaymentDetailsButton.getLayer());

        // setup event observables

        Observable<PaymentDetailsEvent> viewShowingEvents = JavaFxObservable.changesOf(paymentsView.showingProperty())
                .map(showing -> showing.getNewVal() ? PaymentDetailsEvent.listViewShowing() : PaymentDetailsEvent.listViewNotShowing());

        Observable<PaymentDetailsEvent> listItemChangedEvents = JavaFxObservable.changesOf(paymentDetailsListView.selectedItemProperty())
                .map(Change::getNewVal).map(PaymentDetailsEvent::listItemChanged);

        Observable<PaymentDetailsEvent> addButtonEvents = Observable.create(source ->
                addPaymentDetailsButton.setOnAction(source::onNext))
                .map(actionEvent -> PaymentDetailsEvent.listAddButtonPressed());

        Observable<PaymentDetailsEvent> paymentDetailsEvents = Observable.merge(viewShowingEvents,
                listItemChangedEvents, addButtonEvents).publish().refCount();

        // transform events to actions

        Observable<PaymentDetailsAction> paymentDetailsActions = paymentDetailsEvents.map(event -> {

            switch (event.getType()) {
                case LIST_VIEW_SHOWING:
                    return PaymentDetailsAction.load();
//                case LIST_VIEW_NOT_SHOWING:
//                    break;
//                case LIST_ITEM_CHANGED:
//                    break;
//                case LIST_ADD_BUTTON_PRESSED:
//                    return PaymentDetailsAction.add();
//                    break;
//                case DETAILS_VIEW_SHOWING:
//                    break;
//                case DETAILS_VIEW_NOT_SHOWING:
//                    break;
//                case DETAILS_ADD_BUTTON_PRESSED:
//                    break;
                default:
                    throw new RuntimeException("Unexpected PaymentDetailsEvent.Type");
            }
        });

        paymentDetailsActions.

                // transform actions to results

                Observable<PaymentDetailsResult> paymentDetailsResults = paymentDetailsActions
                .compose(profileManager.paymentDetailsActionTransformer());

        // handle events

        paymentDetailsEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case LIST_VIEW_SHOWING:
                            setAppBar();
                            break;
                        case LIST_VIEW_NOT_SHOWING:
                            break;
                        case LIST_ITEM_CHANGED:
                            break;
                        case LIST_ADD_BUTTON_PRESSED:
                            MobileApplication.getInstance().switchView(BytabitMobile.ADD_PAYMENT_VIEW);
                            break;
                        case DETAILS_VIEW_SHOWING:
                            break;
                        case DETAILS_VIEW_NOT_SHOWING:
                            break;
                        case DETAILS_ADD_BUTTON_PRESSED:
                            break;
                    }
                });

        // handle results

        paymentDetailsResults.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(result -> {
                    switch (result.getType()) {
                        case PENDING:
                            //paymentsView.setDisable(true);
                            break;
                        case ADDED:
                        case LOADED:
                        case UPDATED:
                            paymentsView.setDisable(false);
                            paymentDetailsListView.itemsProperty().add(result.getData());
                            break;
                        case ERROR:
                            break;
                    }
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> {
            MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER);
        }));
        appBar.setTitleText("Payment Details");
    }
}
