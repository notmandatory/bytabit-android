package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.model.Profile;
import javafx.util.StringConverter;

public class ProfileStringConverter extends StringConverter<Profile> {

    @Override
    public String toString(Profile profile) {
        return profile.getName();
    }

    @Override
    public Profile fromString(String displayName) {
//        Profile found = null;
//        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
//            if (paymentMethod.displayName().equals(displayName)) {
//                found = paymentMethod;
//                break;
//            }
//        }
        return null;
    }
}
