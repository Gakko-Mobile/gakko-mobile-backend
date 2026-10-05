package com.gakkomobile.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("classpath:firebase-service-account.json")
    private Resource serviceAccountResource;

    @PostConstruct
    public void initializeFirebase() {
        try {

            if (FirebaseApp.getApps().isEmpty() && serviceAccountResource.exists()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountResource.getInputStream()))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Successfully initialize Firebase Admin SDK");
            } else {
                log.error("File firebase-service-account.json not found");
            }
        } catch (IOException e) {

            log.error("I/O Exception while initializing Firebase SDK", e);
        }
    }
}