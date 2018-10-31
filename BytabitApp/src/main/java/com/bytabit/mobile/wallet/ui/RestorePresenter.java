package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.AutoCompleteTextField;
import com.gluonhq.charm.glisten.control.DatePicker;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;
import org.bitcoinj.crypto.MnemonicCode;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestorePresenter {

    @FXML
    private View restoreView;

    @FXML
    private AutoCompleteTextField<String> word1;

    @FXML
    private AutoCompleteTextField<String> word2;

    @FXML
    private AutoCompleteTextField<String> word3;

    @FXML
    private AutoCompleteTextField<String> word4;

    @FXML
    private AutoCompleteTextField<String> word5;

    @FXML
    private AutoCompleteTextField<String> word6;

    @FXML
    private AutoCompleteTextField<String> word7;

    @FXML
    private AutoCompleteTextField<String> word8;

    @FXML
    private AutoCompleteTextField<String> word9;

    @FXML
    private AutoCompleteTextField<String> word10;

    @FXML
    private AutoCompleteTextField<String> word11;

    @FXML
    private AutoCompleteTextField<String> word12;

    @FXML
    private javafx.scene.control.DatePicker datePicker;

    private List<AutoCompleteTextField<String>> words = new ArrayList<>();

    @FXML
    private DatePicker firstUsedDate;

    @FXML
    private Button restoreButton;

    @Inject
    WalletManager walletManager;

    private final int completerWaitMs = 200;

    public void initialize() {

        words.addAll(Arrays.asList(word1, word2, word3, word4, word5, word6,
                word7, word8, word9, word10, word11, word12));

        setupWordCompleters(words);

        JavaFxObservable.changesOf(restoreView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(showing -> {
                    setAppBar();
                    clearAll(words);
                });
    }

    private void setupWordCompleters(List<AutoCompleteTextField<String>> words) {

        MnemonicCode mnemonicCode = MnemonicCode.INSTANCE;
        List<String> wordList = mnemonicCode.getWordList();

        for (AutoCompleteTextField<String> word : words) {
            word.setCompleter(s -> matching(wordList, s));
            word.setCompleterWaitDuration(Duration.millis(completerWaitMs));
        }
    }

    private List<String> matching(List<String> words, String s) {
        List<String> matches = new ArrayList<>();

        for (String word : words) {
            if (word.startsWith(s)) {
                matches.add(word);
            }
        }
        return matches;
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(ae -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Restore");
    }

    private void clearAll(List<AutoCompleteTextField<String>> words) {
        for (AutoCompleteTextField<String> word : words) {
            word.setText(null);
        }
        datePicker.valueProperty().setValue(null);
    }
}