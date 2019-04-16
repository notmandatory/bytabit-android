/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.mobile.common.ui;

import javafx.util.StringConverter;

import java.math.BigDecimal;

public class StringBigDecimalConverter extends StringConverter<BigDecimal> {

    @Override
    public String toString(BigDecimal object) {
        return object != null ? object.toString() : null;
    }

    @Override
    public BigDecimal fromString(String string) {
        try {
            return string != null && string.length() > 0 ? new BigDecimal(string) : null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}