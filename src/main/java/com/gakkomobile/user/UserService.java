package com.gakkomobile.user;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.user.dto.UserIdentifiersResponseDTO;
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
    private final UserMapper userMapper;

    public List<UserIdentifiersResponseDTO> searchUserIdentifiers(
            Role userRole,
            String nameIndexSearch,
            Role selectedRole,
            int pageNum,
            Integer limit
    ) {
        Pageable pageable = PageRequest.of(pageNum, limit);

        List<UserEntity> users = switch (selectedRole) {
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
                .map(userMapper::toDto)
                .toList();
    }
}
