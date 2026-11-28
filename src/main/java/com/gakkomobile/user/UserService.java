package com.gakkomobile.user;

import static com.gakkomobile.exception.ExceptionErrorConstants.USER_NOT_FOUND;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.user.dto.UserIdentifiersResponseDTO;
import com.gakkomobile.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper userMapper;

    public List<UserIdentifiersResponseDTO> searchUserIdentifiers(Role userRole, String nameIndexSearch,
            Role selectedRole, int pageNum, Integer limit) {

        Pageable pageable = PageRequest.of(pageNum, limit);

        List<UserEntity> users = switch (selectedRole) {
            case STUDENT -> repository.searchByFullNameOrIndex(nameIndexSearch, pageable);
            case LECTURER -> repository.searchByFullName(nameIndexSearch, pageable);
            default -> {
                if (!userRole.equals(Role.ADMIN)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            ExceptionErrorConstants.FORBIDDEN
                    );
                }

                yield repository.searchByFullName(nameIndexSearch, pageable);
            }
        };

        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    public void updateAppLanguage(UUID userId, AppLanguage language) {

        UserEntity user = repository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.setAppLanguage(language);
        repository.save(user);
    }

    public Map<UUID, UserEntity> getUsersDictionaryByIds(Set<UUID> ids) {

        return repository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    public UserEntity requireUser(UUID userId) {

        return repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(USER_NOT_FOUND, userId)));
    }
}
