package com.gakkomobile.notifications.push;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenRegisterRequestDTO;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserDeviceTokenServiceTest {

    @Mock
    private UserDeviceTokenRepository tokenRepository;

    @Mock
    private UserDeviceTokenMapper tokenMapper;

    @InjectMocks
    private UserDeviceTokenService service;


    @Test
    void registerOrUpdateToken_shouldUpdateExistingToken() {

        // given
        UserDeviceTokenRegisterRequestDTO request = UserDeviceTokenRegisterRequestDTO.builder()
                .fcmToken("token")
                .build();
        UserDeviceTokenEntity existingToken = new UserDeviceTokenEntity();
        existingToken.setUserId(uuid(1));
        when(tokenRepository.findByToken(request.fcmToken())).thenReturn(Optional.of(existingToken));

        // when
        service.registerOrUpdateToken(uuid(0), request);

        // then
        assertThat(existingToken.getUserId()).isEqualTo(uuid(0));
        verify(tokenRepository).save(existingToken);
    }

    @Test
    void registerOrUpdateToken_shouldCreateNewToken() {

        // given
        UserDeviceTokenRegisterRequestDTO request = UserDeviceTokenRegisterRequestDTO.builder()
                .fcmToken("token")
                .build();
        UserDeviceTokenEntity newToken = new UserDeviceTokenEntity();
        newToken.setUserId(uuid(0));
        when(tokenRepository.findByToken(request.fcmToken())).thenReturn(Optional.empty());
        when(tokenMapper.requestToEntity(request, uuid(0))).thenReturn(newToken);

        // when
        service.registerOrUpdateToken(uuid(0), request);

        // then
        assertThat(newToken.getUserId()).isEqualTo(uuid(0));
        verify(tokenRepository).save(newToken);
    }

    @Test
    void getUserDevices_shouldReturnListOfDevices_whenExists() {

        // given
        UserDeviceTokenEntity token1 = new UserDeviceTokenEntity();
        UserDeviceTokenEntity token2 = new UserDeviceTokenEntity();
        when(tokenRepository.findAllByUserId(uuid(0))).thenReturn(List.of(token1, token2));
        when(tokenMapper.entityToResponseDTO(any())).thenReturn(UserDeviceTokenResponseDTO.builder().build());

        // when
        List<UserDeviceTokenResponseDTO> responseDTOS = service.getUserDevices(uuid(0));

        // then
        assertThat(responseDTOS.size()).isEqualTo(2);
    }

    @Test
    void getUserDevices_shouldReturnEmptyList_whenNotExists() {

        // given
        when(tokenRepository.findAllByUserId(uuid(0))).thenReturn(List.of());

        // when
        List<UserDeviceTokenResponseDTO> responseDTOS = service.getUserDevices(uuid(0));

        // then
        assertThat(responseDTOS.size()).isEqualTo(0);
    }

    @Test
    void unregisterToken_shouldDeleteToken() {
        
        // when
        service.unregisterToken(uuid(0), "token");

        // then
        verify(tokenRepository).deleteByUserIdAndToken(uuid(0), "token");
    }
}
