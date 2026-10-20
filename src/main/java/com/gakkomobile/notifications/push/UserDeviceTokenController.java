package com.gakkomobile.notifications.push;

import com.gakkomobile.notifications.push.dto.UserDeviceTokenRegisterRequestDTO;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenResponseDTO;
import com.gakkomobile.user.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/tokens")
@RequiredArgsConstructor
public class UserDeviceTokenController {

    private final UserDeviceTokenService tokenService;


    @PostMapping
    public ResponseEntity<Void> registerToken(@AuthenticationPrincipal UserEntity currentUser,
                                              @Valid @RequestBody UserDeviceTokenRegisterRequestDTO request) {

        tokenService.registerOrUpdateToken(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<UserDeviceTokenResponseDTO>> getMyDevices(@AuthenticationPrincipal UserEntity currentUser) {

        List<UserDeviceTokenResponseDTO> devices = tokenService.getUserDevices(currentUser.getId());
        return ResponseEntity.ok(devices);
    }


    @DeleteMapping("/{fcmToken}")
    public ResponseEntity<Void> unregisterToken(@AuthenticationPrincipal UserEntity currentUser, @PathVariable String fcmToken) {

        tokenService.unregisterToken(currentUser.getId(), fcmToken);
        return ResponseEntity.noContent().build();
    }

}
