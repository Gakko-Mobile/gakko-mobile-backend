package com.gakkomobile.notifications.push;

import com.gakkomobile.notifications.push.dto.UserDeviceTokenRegisterRequestDTO;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeviceTokenService {

    private final UserDeviceTokenRepository tokenRepository;
    private final UserDeviceTokenMapper tokenMapper;

    @Transactional
    public void registerOrUpdateToken(UUID userId, UserDeviceTokenRegisterRequestDTO request) {

        tokenRepository.findByToken(request.fcmToken())
                .ifPresentOrElse(
                        existingToken -> updateExistingToken(userId, existingToken),
                        () -> registerToken(userId, request)
                );
    }

    private void updateExistingToken(UUID userId, UserDeviceTokenEntity existingToken) {

        log.info("Updating existing FCM token for user: {}", userId);
        existingToken.setUserId(userId);
        tokenRepository.save(existingToken);
    }

    private void registerToken(UUID userId, UserDeviceTokenRegisterRequestDTO request) {

        log.info("Registering new FCM token for user: {}", userId);
        UserDeviceTokenEntity userDeviceTokenEntity = tokenMapper.requestToEntity(request, userId);
        tokenRepository.save(userDeviceTokenEntity);
    }

    public List<UserDeviceTokenResponseDTO> getUserDevices(UUID userId) {

        return tokenRepository.findAllByUserId(userId).stream()
                .map(tokenMapper::entityToResponseDTO)
                .toList();
    }

    @Transactional
    public void unregisterToken(UUID userId, String fcmToken) {

        log.info("Unregistering FCM token for user: {}", userId);
        tokenRepository.deleteByUserIdAndToken(userId, fcmToken);
    }
}
