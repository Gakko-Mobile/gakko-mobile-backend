package com.gakkomobile.surveys.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SurveyLifecycleSchedulerTest {

    @Mock
    private SurveyLifecycleService surveyLifecycleService;

    @InjectMocks
    private SurveyLifecycleScheduler scheduler;

    @Test
    void runLifecycle_shouldCloseExpiredSurveysAndDispatchReminders() {

        scheduler.runLifecycle();

        verify(surveyLifecycleService).closeExpiredSurveys();
        verify(surveyLifecycleService).dispatchDueReminders();
    }
}
