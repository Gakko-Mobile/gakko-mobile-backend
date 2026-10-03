package com.gakkomobile.user;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.user.dto.UserIdentifiersResponse;
import com.gakkomobile.user.entity.Role;
import com.gakkomobile.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserIdentifiersResponse> searchUserIdentifiers(
            Role userRole,
            String nameIndexSearch,
            Role selectedRole,
            int pageNum,
            Integer limit
    ) {
        Pageable pageable = PageRequest.of(pageNum, limit);

        List<User> users = switch (selectedRole) {
            case STUDENT -> userRepository.searchByFullNameOrIndex(nameIndexSearch, pageable);
            case LECTURER -> userRepository.searchByFullName(nameIndexSearch, pageable);
            default -> {
                if (!userRole.equals(Role.ADMIN)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            ExceptionErrorConstants.FORBIDDEN
                    );
                }

                yield userRepository.searchByFullName(nameIndexSearch, pageable);
            }
        };

        return users.stream()
                .map(u -> new UserIdentifiersResponse(
                        u.getEmail(),
                        String.format("%s %s", u.getLastName(), u.getFirstName()),
                        u.getIndexNumber()
                )).toList();
    }
}
