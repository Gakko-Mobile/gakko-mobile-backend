package com.gakkomobile.surveys.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SurveyLifecycleScheduler {

    private final SurveyService surveyService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    void runLifecycle() {

        surveyService.closeExpiredSurveys();
        surveyService.dispatchDueReminders();
    }
}
