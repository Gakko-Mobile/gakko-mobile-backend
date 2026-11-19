package com.gakkomobile.user;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.user.exception.NotALecturerException;
import com.gakkomobile.user.dto.UserIdentifiersResponseDTO;
import com.gakkomobile.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper userMapper;

    public void existsByEmail(String email) {
        if (!repository.existsByEmail(email)) {
            throw new UserNotFoundException();
        }
    }

    public Set<UserEntity> getLecturersByIds(Set<UUID> ids) {
        Map<UUID, UserEntity> usersById = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        for (UUID id : ids) {
            UserEntity user = usersById.get(id);
            if (Objects.isNull(user)) {
                throw new ResourceNotFoundException(ExceptionErrorConstants.LECTURER_NOT_FOUND);
            }
            if (user.getRole() != Role.LECTURER) {
                throw new NotALecturerException(ExceptionErrorConstants.USER_IS_NOT_A_LECTURER.formatted(id));
            }
        }

        return new HashSet<>(usersById.values());
    }

    public List<UserIdentifiersResponseDTO> searchUserIdentifiers(
            Role userRole,
            String nameIndexSearch,
            Role selectedRole,
            int pageNum,
            Integer limit
    ) {
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
}
