package com.gakkomobile.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

class NotificationTextResolverTest {

    private final NotificationTextResolver resolver = new NotificationTextResolver(messageSource());

    @Test
    void resolve_shouldReturnTheTitleForTheRequestedLanguage() {

        assertThat(resolver.resolve(NotificationMessages.MESSAGE_CREATED, Locale.forLanguageTag("pl")))
                .isEqualTo("Nowa wiadomość");
        assertThat(resolver.resolve(NotificationMessages.MESSAGE_CREATED, Locale.ENGLISH))
                .isEqualTo("New message");
        assertThat(resolver.resolve(NotificationMessages.SURVEY_CREATED, Locale.ENGLISH))
                .isEqualTo("New survey");
        assertThat(resolver.resolve(NotificationMessages.SURVEY_REMINDER, Locale.forLanguageTag("pl")))
                .isEqualTo("Przypomnienie o ankiecie");
    }

    @Test
    void resolve_shouldFallBackToTheMessageKeyAndPolish() {

        assertThat(resolver.resolve("missing.key", Locale.ENGLISH))
                .isEqualTo("missing.key");
        assertThat(resolver.resolve(null, Locale.ENGLISH))
                .isEmpty();
        assertThat(resolver.resolve(NotificationMessages.MESSAGE_CREATED, null))
                .isEqualTo("Nowa wiadomość");
    }

    private static ReloadableResourceBundleMessageSource messageSource() {

        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
