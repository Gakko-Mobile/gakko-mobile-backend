package com.gakkomobile.surveys.service;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyCompletionEntity;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyReminderDispatchEntity;
import com.gakkomobile.surveys.domain.SurveyReminderDispatchKey;
import com.gakkomobile.surveys.domain.SurveyResponseEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.surveys.dto.SurveyResultDTO;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
import com.gakkomobile.surveys.exception.SurveyConflictException;
import com.gakkomobile.surveys.exception.SurveyNotFoundException;
import com.gakkomobile.surveys.exception.SurveyResultsHiddenException;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private static final BigDecimal NANOS_PER_DAY = BigDecimal.valueOf(86_400_000_000_000L);

    private final SurveyRepository surveyRepository;
    private final SurveyCompletionRepository surveyCompletionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyReminderDispatchRepository surveyReminderDispatchRepository;
    private final UserRepository userRepository;
    private final SurveyMapper surveyMapper;
    private final SurveyConfigurationValidator surveyConfigurationValidator;
    private final SurveyAnswerValidator surveyAnswerValidator;
    private final SurveyPresentation surveyPresentation;
    private final SurveyNotificationPublisher surveyNotificationPublisher;
    private final SurveyAnalyticsAggregator surveyAnalyticsAggregator;
    private final DatabaseStudentAudienceLookup audienceLookup;

    @Transactional
    public AdminSurveyDetailsDTO createDraft(CreateSurveyCommand command, UUID creatorIdentifier) {

        SurveyConfiguration configuration = surveyMapper.toConfiguration(command);
        surveyConfigurationValidator.validate(configuration);
        UserEntity creator = userRepository.findById(creatorIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User with id [%s] not found", creatorIdentifier)));

        SurveyEntity survey = new SurveyEntity();
        survey.setCreator(creator);
        survey.setStatus(SurveyStatus.DRAFT);
        applyConfiguration(survey, configuration);
        return surveyPresentation.toAdminDetails(surveyRepository.save(survey));
    }

    @Transactional
    public AdminSurveyDetailsDTO updateSurvey(UUID surveyIdentifier, UpdateSurveyCommand command) {

        SurveyEntity survey = findSurvey(surveyIdentifier);
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new SurveyConflictException("Only draft surveys can be updated");
        }
        SurveyConfiguration configuration = surveyMapper.toConfiguration(command);
        surveyConfigurationValidator.validate(configuration);
        applyConfiguration(survey, configuration);
        return surveyPresentation.toAdminDetails(surveyRepository.save(survey));
    }

    @Transactional
    public AdminSurveyDetailsDTO publishSurvey(UUID surveyIdentifier) {

        SurveyEntity survey = findSurvey(surveyIdentifier);
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new SurveyConflictException("Only draft surveys can be published");
        }
        surveyConfigurationValidator.validate(configurationFrom(survey));
        survey.setStatus(SurveyStatus.COLLECTING);
        SurveyEntity publishedSurvey = surveyRepository.save(survey);
        surveyNotificationPublisher.publishOpened(publishedSurvey);
        return surveyPresentation.toAdminDetails(publishedSurvey);
    }

    @Transactional
    public AdminSurveyDetailsDTO closeSurvey(UUID surveyIdentifier) {

        SurveyEntity survey = findSurvey(surveyIdentifier);
        if (survey.getStatus() != SurveyStatus.COLLECTING) {
            throw new SurveyConflictException("Only collecting surveys can be closed");
        }
        survey.setStatus(SurveyStatus.CLOSED);
        return surveyPresentation.toAdminDetails(surveyRepository.save(survey));
    }

    @Transactional
    public void softDeleteSurvey(UUID surveyIdentifier) {

        SurveyEntity survey = findSurvey(surveyIdentifier);
        survey.setDeletedAt(OffsetDateTime.now());
        surveyRepository.save(survey);
    }

    @Transactional(readOnly = true)
    public AdminSurveyDetailsDTO getAdminSurvey(UUID surveyIdentifier) {

        return surveyPresentation.toAdminDetails(findSurvey(surveyIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<AdminSurveyDetailsDTO> getAdminSurveys(Pageable pageable) {

        return surveyRepository.findAll(pageable).map(surveyPresentation::toAdminDetails);
    }

    @Transactional(readOnly = true)
    public SurveyAnalyticsDTO getAggregatedResults(UUID surveyIdentifier) {

        return surveyAnalyticsAggregator.aggregateForStaff(findSurvey(surveyIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<SurveyResponseDTO> getPendingSurveys(UUID userIdentifier, Pageable pageable) {

        OffsetDateTime currentTime = OffsetDateTime.now();
        Set<UUID> completedSurveyIdentifiers = new HashSet<>(
                surveyCompletionRepository.findSurveyIdentifiersByUserIdentifier(userIdentifier));
        Set<String> groupTokens = audienceLookup.findGroupTokens(userIdentifier);
        Set<String> subjectTokens = audienceLookup.findSubjectTokens(userIdentifier);

        List<SurveyResponseDTO> pendingSurveys = new ArrayList<>();
        for (SurveyEntity survey : surveyRepository.findOpenSurveys(SurveyStatus.COLLECTING, currentTime)) {
            if (survey.getTargetType() == AudienceTargetType.VIA_LINK) {
                continue;
            }
            if (completedSurveyIdentifiers.contains(survey.getId())) {
                continue;
            }
            if (!matchesAudience(survey, groupTokens, subjectTokens)) {
                continue;
            }
            pendingSurveys.add(surveyPresentation.toStudentResponse(survey, userIdentifier, false, false));
        }
        return toPage(pendingSurveys, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SurveyResponseDTO> getCompletedSurveys(UUID userIdentifier, Pageable pageable) {

        List<UUID> completedSurveyIdentifiers = surveyCompletionRepository.findSurveyIdentifiersByUserIdentifier(userIdentifier);
        List<SurveyEntity> surveys = new ArrayList<>(surveyRepository.findAllById(completedSurveyIdentifiers));
        surveys.sort(Comparator.comparing(SurveyEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<SurveyResponseDTO> completedSurveys = new ArrayList<>();
        for (SurveyEntity survey : surveys) {
            completedSurveys.add(surveyPresentation.toStudentResponse(survey, userIdentifier, true, false));
        }
        return toPage(completedSurveys, pageable);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDTO getSurveyForStudent(UUID surveyIdentifier, UUID userIdentifier) {

        SurveyEntity survey = findSurvey(surveyIdentifier);
        boolean completed = surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier);
        if (!completed && !matchesAudience(
                survey,
                audienceLookup.findGroupTokens(userIdentifier),
                audienceLookup.findSubjectTokens(userIdentifier)
        )) {
            throw new SurveyNotFoundException(surveyIdentifier);
        }
        return surveyPresentation.toStudentResponse(survey, userIdentifier, completed, true);
    }

    @Transactional
    public void submitResponse(UUID surveyIdentifier, UUID userIdentifier, SubmitSurveyRequestDTO request) {

        SurveyEntity survey = surveyRepository.findByIdForUpdate(surveyIdentifier)
                .orElseThrow(() -> new SurveyNotFoundException(surveyIdentifier));
        ensureAcceptingResponses(survey);
        if (!matchesAudience(
                survey,
                audienceLookup.findGroupTokens(userIdentifier),
                audienceLookup.findSubjectTokens(userIdentifier)
        )) {
            throw new SurveyNotFoundException(surveyIdentifier);
        }

        Optional<SurveyCompletionEntity> existingCompletion = surveyCompletionRepository
                .findBySurveyIdAndUserId(survey.getId(), userIdentifier);
        if (existingCompletion.isPresent() && !survey.isAllowResubmission()) {
            throw new SurveyConflictException("Survey response was already submitted");
        }

        Map<String, Object> answers = surveyAnswerValidator.toAnswerMap(request.answers());
        surveyAnswerValidator.validate(survey.getQuestions(), answers);
        OffsetDateTime currentTime = OffsetDateTime.now();
        saveCompletion(survey, userIdentifier, existingCompletion, currentTime);
        saveResponse(survey, userIdentifier, answers, currentTime);
        closeWhenResponseLimitReached(survey);
    }

    @Transactional(readOnly = true)
    public SurveyResultDTO getPublicResults(UUID surveyIdentifier, UUID userIdentifier) {

        SurveyEntity survey = findSurvey(surveyIdentifier);
        if (!survey.isShowResultsToStudents()) {
            throw new SurveyResultsHiddenException("Survey results are hidden");
        }
        if (!surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)) {
            throw new SurveyResultsHiddenException("Survey results are available after submission");
        }
        return surveyAnalyticsAggregator.aggregateForStudents(survey);
    }

    @Transactional
    void closeExpiredSurveys() {

        surveyRepository.closeExpired(SurveyStatus.CLOSED, SurveyStatus.COLLECTING, OffsetDateTime.now());
    }

    @Transactional
    void dispatchDueReminders() {

        OffsetDateTime currentTime = OffsetDateTime.now();
        List<SurveyEntity> surveys = surveyRepository.findByStatusAndClosesAtIsNotNull(SurveyStatus.COLLECTING);
        for (SurveyEntity survey : surveys) {
            dispatchSurveyReminders(survey, currentTime);
        }
    }

    private void dispatchSurveyReminders(SurveyEntity survey, OffsetDateTime currentTime) {

        for (BigDecimal intervalDays : reminderIntervals(survey)) {
            if (surveyReminderDispatchRepository.alreadyDispatched(survey.getId(), intervalDays)) {
                continue;
            }
            if (currentTime.isBefore(reminderMoment(survey.getClosesAt(), intervalDays))) {
                continue;
            }
            List<UUID> recipientIdentifiers = audienceLookup.findUsersWithoutCompletion(
                    survey.getId(),
                    survey.getTargetType(),
                    survey.getTargetIds()
            );
            SurveyReminderDispatchEntity dispatch = new SurveyReminderDispatchEntity();
            dispatch.setId(new SurveyReminderDispatchKey(survey.getId(), intervalDays));
            surveyReminderDispatchRepository.save(dispatch);
            surveyNotificationPublisher.publishReminders(survey, recipientIdentifiers);
        }
    }

    private void saveCompletion(
            SurveyEntity survey,
            UUID userIdentifier,
            Optional<SurveyCompletionEntity> existingCompletion,
            OffsetDateTime currentTime
    ) {

        if (existingCompletion.isPresent()) {
            existingCompletion.get().setCompletedAt(currentTime);
            return;
        }
        SurveyCompletionEntity completion = new SurveyCompletionEntity();
        completion.setSurveyId(survey.getId());
        completion.setUserId(userIdentifier);
        completion.setCompletedAt(currentTime);
        try {
            surveyCompletionRepository.saveAndFlush(completion);
        } catch (DataIntegrityViolationException exception) {
            if (surveyCompletionRepository.existsBySurveyIdAndUserId(survey.getId(), userIdentifier)) {
                throw new SurveyConflictException("Survey response was already submitted");
            }
            throw exception;
        }
    }

    private void saveResponse(
            SurveyEntity survey,
            UUID userIdentifier,
            Map<String, Object> answers,
            OffsetDateTime currentTime
    ) {

        if (survey.isAnonymous()) {
            SurveyResponseEntity response = new SurveyResponseEntity();
            response.setSurveyId(survey.getId());
            response.setUserId(null);
            response.setAnswers(answers);
            response.setSubmittedAt(currentTime);
            surveyResponseRepository.save(response);
            return;
        }

        Optional<SurveyResponseEntity> existingResponse = surveyResponseRepository
                .findBySurveyIdAndUserId(survey.getId(), userIdentifier);
        if (existingResponse.isPresent()) {
            SurveyResponseEntity response = existingResponse.get();
            response.setAnswers(answers);
            response.setSubmittedAt(currentTime);
            return;
        }

        SurveyResponseEntity response = new SurveyResponseEntity();
        response.setSurveyId(survey.getId());
        response.setUserId(userIdentifier);
        response.setAnswers(answers);
        response.setSubmittedAt(currentTime);
        surveyResponseRepository.save(response);
    }

    private void closeWhenResponseLimitReached(SurveyEntity survey) {

        if (Objects.isNull(survey.getResponseLimit())) {
            return;
        }
        long responseCount = surveyResponseRepository.countBySurveyId(survey.getId());
        if (responseCount >= survey.getResponseLimit()) {
            survey.setStatus(SurveyStatus.CLOSED);
        }
    }

    private void ensureAcceptingResponses(SurveyEntity survey) {

        OffsetDateTime currentTime = OffsetDateTime.now();
        if (survey.getStatus() != SurveyStatus.COLLECTING) {
            throw new SurveyConflictException("Survey is not accepting responses");
        }
        if (!Objects.isNull(survey.getOpensAt()) && survey.getOpensAt().isAfter(currentTime)) {
            throw new SurveyConflictException("Survey is not open yet");
        }
        if (!Objects.isNull(survey.getClosesAt()) && !survey.getClosesAt().isAfter(currentTime)) {
            throw new SurveyConflictException("Survey is closed");
        }
    }

    private boolean matchesAudience(SurveyEntity survey, Set<String> groupTokens, Set<String> subjectTokens) {

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

    private void applyConfiguration(SurveyEntity survey, SurveyConfiguration configuration) {

        survey.setTitle(configuration.title().trim());
        survey.setDescription(configuration.description());
        survey.setQuestions(new ArrayList<>(configuration.questions()));
        survey.setTargetType(configuration.targetType());
        survey.setTargetIds(new ArrayList<>(configuration.targetIds()));
        survey.setAnonymous(configuration.anonymous());
        survey.setAllowResubmission(configuration.allowResubmission());
        survey.setRequireLogin(configuration.requireLogin());
        survey.setShowResultsToStudents(configuration.showResultsToStudents());
        survey.setRandomizeQuestionOrder(configuration.randomizeQuestionOrder());
        survey.setResponseLimit(configuration.responseLimit());
        survey.setConfirmationMessage(configuration.confirmationMessage());
        survey.setOpensAt(configuration.opensAt());
        survey.setClosesAt(configuration.closesAt());
        survey.setReminderIntervals(new ArrayList<>(configuration.reminderIntervals()));
    }

    private SurveyConfiguration configurationFrom(SurveyEntity survey) {

        return new SurveyConfiguration(
                survey.getTitle(),
                survey.getDescription(),
                survey.getQuestions(),
                survey.getTargetType(),
                survey.getTargetIds(),
                survey.isAnonymous(),
                survey.isAllowResubmission(),
                survey.isRequireLogin(),
                survey.isShowResultsToStudents(),
                survey.isRandomizeQuestionOrder(),
                survey.getResponseLimit(),
                survey.getConfirmationMessage(),
                survey.getOpensAt(),
                survey.getClosesAt(),
                survey.getReminderIntervals()
        );
    }

    private SurveyEntity findSurvey(UUID surveyIdentifier) {

        return surveyRepository.findById(surveyIdentifier)
                .orElseThrow(() -> new SurveyNotFoundException(surveyIdentifier));
    }

    private List<BigDecimal> reminderIntervals(SurveyEntity survey) {

        if (Objects.isNull(survey.getReminderIntervals()) || survey.getReminderIntervals().isEmpty()) {
            return List.of();
        }
        List<BigDecimal> intervals = new ArrayList<>();
        for (Double reminderInterval : survey.getReminderIntervals()) {
            if (Objects.isNull(reminderInterval) || reminderInterval <= 0) {
                continue;
            }
            intervals.add(BigDecimal.valueOf(reminderInterval).setScale(4, RoundingMode.HALF_UP));
        }
        return intervals;
    }

    private OffsetDateTime reminderMoment(OffsetDateTime closesAt, BigDecimal intervalDays) {

        BigDecimal nanosBeforeClose = intervalDays.multiply(NANOS_PER_DAY);
        return closesAt.minusNanos(nanosBeforeClose.longValue());
    }

    private Page<SurveyResponseDTO> toPage(List<SurveyResponseDTO> surveys, Pageable pageable) {

        int start = (int) Math.min(pageable.getOffset(), surveys.size());
        int end = Math.min(start + pageable.getPageSize(), surveys.size());
        List<SurveyResponseDTO> content = new ArrayList<>(surveys.subList(start, end));
        return new PageImpl<>(content, pageable, surveys.size());
    }
}
