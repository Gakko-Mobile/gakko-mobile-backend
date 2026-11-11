package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
class SurveyAudienceMatcher {

    boolean matches(SurveyEntity survey, Set<String> groupTokens, Set<String> subjectTokens) {

        if (survey.getTargetType() == AudienceTargetType.ALL || survey.getTargetType() == AudienceTargetType.VIA_LINK) {
            return true;
        }
        if (Objects.isNull(survey.getTargetIds())) {
            return false;
        }
        Set<String> audienceTokens = survey.getTargetType() == AudienceTargetType.GROUPS ? groupTokens : subjectTokens;
        for (String targetId : survey.getTargetIds()) {
            if (audienceTokens.contains(targetId)) {
                return true;
            }
        }
        return false;
    }
}
