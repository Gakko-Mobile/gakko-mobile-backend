package com.gakkomobile.messages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
class MessageClockConfiguration {

    @Bean
    Clock clock() {

        return Clock.system(ZoneId.of("Europe/Warsaw"));
    }
}
