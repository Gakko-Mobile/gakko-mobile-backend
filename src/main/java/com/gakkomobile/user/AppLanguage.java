package com.gakkomobile.user;

import java.util.Locale;

public enum AppLanguage {
    PL,
    EN;

    public Locale toLocale() {

        return this == EN ? Locale.ENGLISH : Locale.forLanguageTag("pl");
    }
}
