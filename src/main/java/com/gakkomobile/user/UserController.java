package com.gakkomobile.user;

import com.gakkomobile.user.dto.UserIdentifiersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/identifiers")
    public ResponseEntity<List<UserIdentifiersResponse>> searchUserIdentifiers(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "name-or-index-search") String nameIndexSearch,
            @RequestParam(name = "role") Role selectedRole,
            @RequestParam(name = "page", defaultValue = "0") Integer pageNum,
            @RequestParam(name = "limit", defaultValue = "5") Integer limit
    ) {
        List<UserIdentifiersResponse> responseBody = userService.searchUserIdentifiers(
                user.getRole(),
                nameIndexSearch,
                selectedRole,
                pageNum,
                limit
        );

        return ResponseEntity.ok(responseBody);
    }
}
