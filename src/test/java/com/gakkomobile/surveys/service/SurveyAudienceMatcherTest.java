package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SurveyAudienceMatcherTest {

    private final SurveyAudienceMatcher matcher = new SurveyAudienceMatcher();

    @Test
    void matches_shouldAcceptEveryoneForAllAndLinkAudiences() {

        assertThat(matcher.matches(survey(AudienceTargetType.ALL, null), Set.of(), Set.of())).isTrue();
        assertThat(matcher.matches(survey(AudienceTargetType.VIA_LINK, List.of()), Set.of(), Set.of())).isTrue();
    }

    @Test
    void matches_shouldUseGroupTokensForGroupsAndSubjectTokensForCourses() {

        SurveyEntity groups = survey(AudienceTargetType.GROUPS, List.of("CS-1"));
        SurveyEntity course = survey(AudienceTargetType.COURSE, List.of("MATH"));
        SurveyEntity missingTargets = survey(AudienceTargetType.GROUPS, null);

        assertThat(matcher.matches(groups, Set.of("CS-1"), Set.of())).isTrue();
        assertThat(matcher.matches(groups, Set.of("OTHER"), Set.of("CS-1"))).isFalse();
        assertThat(matcher.matches(course, Set.of("MATH"), Set.of("OTHER"))).isFalse();
        assertThat(matcher.matches(course, Set.of(), Set.of("MATH"))).isTrue();
        assertThat(matcher.matches(missingTargets, Set.of("CS-1"), Set.of())).isFalse();
    }

    private SurveyEntity survey(AudienceTargetType targetType, List<String> targetIds) {

        SurveyEntity survey = new SurveyEntity();
        survey.setTargetType(targetType);
        survey.setTargetIds(targetIds);
        return survey;
    }
}
