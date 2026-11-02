package com.gakkomobile.surveys.service;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
import com.gakkomobile.surveys.exception.SurveyConflictException;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyAdministrationServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SurveyMapper surveyMapper;
    @Mock
    private SurveyConfigurationValidator surveyConfigurationValidator;
    @Mock
    private SurveyPresentation surveyPresentation;
    @Mock
    private SurveyNotificationPublisher surveyNotificationPublisher;
    @Mock
    private SurveyLookup surveyLookup;

    @InjectMocks
    private SurveyAdministrationService service;

    @Test
    void createDraft_shouldRejectAnUnknownCreator() {

        UUID creatorIdentifier = UUID.randomUUID();
        when(surveyMapper.toConfiguration(nullable(com.gakkomobile.surveys.dto.CreateSurveyCommand.class)))
                .thenReturn(configuration());
        when(userRepository.findById(creatorIdentifier)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDraft(null, creatorIdentifier))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(surveyRepository, never()).save(any());
    }

    @Test
    void createDraft_shouldStoreADraftFromTheMappedConfiguration() {

        UUID creatorIdentifier = UUID.randomUUID();
        UserEntity creator = new UserEntity();
        creator.setId(creatorIdentifier);
        SurveyConfiguration configuration = configuration();
        AdminSurveyDetailsDTO details = AdminSurveyDetailsDTO.builder().title("Feedback").build();
        when(surveyMapper.toConfiguration(nullable(com.gakkomobile.surveys.dto.CreateSurveyCommand.class))).thenReturn(configuration);
        when(userRepository.findById(creatorIdentifier)).thenReturn(Optional.of(creator));
        when(surveyRepository.save(any(SurveyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(surveyPresentation.toAdminDetails(any(SurveyEntity.class))).thenReturn(details);

        AdminSurveyDetailsDTO created = service.createDraft(null, creatorIdentifier);

        ArgumentCaptor<SurveyEntity> surveyCaptor = ArgumentCaptor.forClass(SurveyEntity.class);
        verify(surveyConfigurationValidator).validate(configuration);
        verify(surveyMapper).applyConfiguration(surveyCaptor.capture(), org.mockito.ArgumentMatchers.eq(configuration));
        assertThat(surveyCaptor.getValue().getStatus()).isEqualTo(SurveyStatus.DRAFT);
        assertThat(surveyCaptor.getValue().getCreator()).isEqualTo(creator);
        assertThat(created).isEqualTo(details);
    }

    @Test
    void updateSurvey_shouldRejectSurveysThatAreNoLongerDrafts() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        survey.setStatus(SurveyStatus.COLLECTING);
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);

        assertThatThrownBy(() -> service.updateSurvey(surveyIdentifier, (UpdateSurveyCommand) null))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Only draft surveys can be updated");
    }

    @Test
    void publishSurvey_shouldStartCollectionAndNotifyTheAudience() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        survey.setId(surveyIdentifier);
        survey.setStatus(SurveyStatus.DRAFT);
        SurveyConfiguration configuration = configuration();
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);
        when(surveyMapper.toConfiguration(survey)).thenReturn(configuration);
        when(surveyRepository.save(survey)).thenReturn(survey);
        when(surveyPresentation.toAdminDetails(survey)).thenReturn(AdminSurveyDetailsDTO.builder().build());

        service.publishSurvey(surveyIdentifier);

        assertThat(survey.getStatus()).isEqualTo(SurveyStatus.COLLECTING);
        verify(surveyConfigurationValidator).validate(configuration);
        verify(surveyNotificationPublisher).publishOpened(survey);
    }

    @Test
    void publishSurvey_shouldRejectASurveyThatIsAlreadyCollecting() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        survey.setStatus(SurveyStatus.COLLECTING);
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);

        assertThatThrownBy(() -> service.publishSurvey(surveyIdentifier))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Only draft surveys can be published");
    }

    @Test
    void closeSurvey_shouldRejectADraft() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        survey.setStatus(SurveyStatus.DRAFT);
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);

        assertThatThrownBy(() -> service.closeSurvey(surveyIdentifier))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Only collecting surveys can be closed");
    }

    @Test
    void softDeleteSurvey_shouldMarkTheSurveyDeletedWithoutRemovingIt() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);

        service.softDeleteSurvey(surveyIdentifier);

        assertThat(survey.getDeletedAt()).isNotNull();
        verify(surveyRepository).save(survey);
    }

    private SurveyConfiguration configuration() {

        return new SurveyConfiguration(
                "Feedback",
                null,
                java.util.List.of(),
                com.gakkomobile.surveys.domain.AudienceTargetType.ALL,
                java.util.List.of(),
                true,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                java.util.List.of()
        );
    }
}
