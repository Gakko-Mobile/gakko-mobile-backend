package com.gakkomobile.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class NotificationTextResolver {

    private static final Locale POLISH = Locale.forLanguageTag("pl");

    private final MessageSource messageSource;

    public String resolve(String messageKey, Locale locale) {

        if (Objects.isNull(messageKey) || messageKey.isBlank()) {
            return "";
        }
        Locale resolvedLocale = Objects.isNull(locale) ? POLISH : locale;
        return messageSource.getMessage(messageKey, null, messageKey, resolvedLocale);
    }
}
