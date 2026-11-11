package com.gakkomobile.surveys.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SurveyLifecycleScheduler {

    private final SurveyLifecycleService surveyLifecycleService;

    @Scheduled(
            initialDelayString = "${survey.lifecycle.initial-delay}",
            fixedDelayString = "${survey.lifecycle.fixed-delay}"
    )
    void runLifecycle() {

        surveyLifecycleService.closeExpiredSurveys();
        surveyLifecycleService.dispatchDueReminders();
    }
}
