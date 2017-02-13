package com.bytabit.ft.profile;

import com.bytabit.ft.profile.model.CurrencyCode;
import com.bytabit.ft.profile.model.PaymentDetails;
import com.bytabit.ft.profile.model.PaymentMethod;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_NAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";
    private String PROFILE_PAYMENTDTLS = "profile.paymentDtls";

    private Optional<String> retrieve(String key) {
        return Services.get(SettingsService.class).map(s -> s.retrieve(key));
    }

    private void store(String key, String value) {
        Services.get(SettingsService.class).ifPresent(s -> s.store(key, value));
    }

    private void remove(String key) {
        Services.get(SettingsService.class).ifPresent(s -> s.remove(key));
    }

    public Optional<String> getPubKey() {
        return retrieve(PROFILE_PUBKEY);
    }

    public void setPubKey(String pubKey) {
        store(PROFILE_PUBKEY, pubKey);
    }

    public Optional<String> getName() {
        return retrieve(PROFILE_NAME);
    }

    public void setName(String pubKey) {
        store(PROFILE_NAME, pubKey);
    }

    public Optional<String> getPhoneNum() {
        return retrieve(PROFILE_PHONENUM);
    }

    public void setPhoneNum(String pubKey) {
        store(PROFILE_PHONENUM, pubKey);
    }

    public Optional<String> getPaymentDetails(CurrencyCode currencyCode,
                                              PaymentMethod paymentMethod) {

        return retrieve(paymentDetailsKey(currencyCode, paymentMethod));
    }

    public void setPaymentDetails(CurrencyCode currencyCode,
                                  PaymentMethod paymentMethod,
                                  String paymentDetails) {

        String key = paymentDetailsKey(currencyCode, paymentMethod);
        //retrieve(key).ifPresent(pd -> remove(key));
        store(paymentDetailsKey(currencyCode, paymentMethod), paymentDetails);
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENTDTLS, currencyCode.name(),
                paymentMethod.displayName());
    }

    public List<PaymentDetails> getPaymentDetails() {
        List<PaymentDetails> paymentDetails = new ArrayList<PaymentDetails>();
        for (CurrencyCode c : CurrencyCode.values()) {
            for (PaymentMethod p : c.paymentMethods()) {
                getPaymentDetails(c, p).ifPresent(pd -> {
                    paymentDetails.add(new PaymentDetails(c, p, pd));
                });
            }
        }
        return paymentDetails;
    }
}
