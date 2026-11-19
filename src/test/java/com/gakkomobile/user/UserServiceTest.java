package com.gakkomobile.user;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.user.dto.UserIdentifiersResponseDTO;
import com.gakkomobile.user.exception.NotALecturerException;
import com.gakkomobile.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void searchUserIdentifiers_WhenSearchingStudents_ShouldCallSearchByFullNameOrIndex() {
        // given
        String searchQuery = "danisa";
        int pageNum = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(pageNum, limit);

        UserEntity student = new UserEntity();
        student.setEmail("danisa@gmail.com");
        student.setFirstName("Danisa");
        student.setLastName("Novikova");
        student.setIndexNumber("s12345");

        UserIdentifiersResponseDTO expectedDto = new UserIdentifiersResponseDTO(
                "danisa@gmail.com", "Novikova Danisa", "s12345"
        );

        when(userRepository.searchByFullNameOrIndex(searchQuery, pageable))
                .thenReturn(List.of(student));
        when(userMapper.toDto(student)).thenReturn(expectedDto);

        // when
        List<UserIdentifiersResponseDTO> result = userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.STUDENT, pageNum, limit
        );

        // then
        assertThat(result).hasSize(1);
        UserIdentifiersResponseDTO response = result.get(0);
        assertThat(response.email()).isEqualTo("danisa@gmail.com");
        assertThat(response.fullName()).isEqualTo("Novikova Danisa");
        assertThat(response.index()).isEqualTo("s12345");

        verify(userRepository).searchByFullNameOrIndex(searchQuery, pageable);
        verify(userMapper).toDto(student);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void searchUserIdentifiers_WhenSearchingLecturers_ShouldCallSearchByFullName() {
        // given
        String searchQuery = "kowalski";
        int pageNum = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(pageNum, limit);

        UserEntity lecturer = new UserEntity();
        lecturer.setEmail("jan@pjatk.edu.pl");
        lecturer.setFirstName("Jan");
        lecturer.setLastName("Kowalski");
        lecturer.setIndexNumber("emp123");

        UserIdentifiersResponseDTO expectedDto = new UserIdentifiersResponseDTO(
                "jan@pjatk.edu.pl", "Kowalski Jan", "emp123"
        );

        when(userRepository.searchByFullName(searchQuery, pageable))
                .thenReturn(List.of(lecturer));
        when(userMapper.toDto(lecturer)).thenReturn(expectedDto);

        // when
        List<UserIdentifiersResponseDTO> result = userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.LECTURER, pageNum, limit
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Kowalski Jan");

        verify(userRepository).searchByFullName(searchQuery, pageable);
        verify(userMapper).toDto(lecturer);
    }

    @Test
    void searchUserIdentifiers_WhenSearchingOtherRolesAsAdmin_ShouldCallSearchByFullName() {
        // given
        String searchQuery = "admin";
        int pageNum = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(pageNum, limit);

        UserEntity employee = new UserEntity();
        employee.setEmail("emp@pjatk.edu.pl");
        employee.setFirstName("Anna");
        employee.setLastName("Nowak");

        UserIdentifiersResponseDTO expectedDto = new UserIdentifiersResponseDTO(
                "emp@pjatk.edu.pl", "Nowak Anna", null
        );

        when(userRepository.searchByFullName(searchQuery, pageable))
                .thenReturn(List.of(employee));
        when(userMapper.toDto(employee)).thenReturn(expectedDto);

        // when
        List<UserIdentifiersResponseDTO> result = userService.searchUserIdentifiers(
                Role.ADMIN, searchQuery, Role.EMPLOYEE, pageNum, limit
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("emp@pjatk.edu.pl");

        verify(userRepository).searchByFullName(searchQuery, pageable);
        verify(userMapper).toDto(employee);
    }

    @Test
    void searchUserIdentifiers_WhenSearchingOtherRolesAsNonAdmin_ShouldThrowForbidden() {
        // given
        String searchQuery = "admin";
        int pageNum = 0;
        int limit = 10;

        // when and then
        assertThatThrownBy(() -> userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.EMPLOYEE, pageNum, limit
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(ExceptionErrorConstants.FORBIDDEN)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verifyNoInteractions(userRepository, userMapper);
    }

    @Test
    void searchUserIdentifiers_ShouldReturnEmptyList_WhenNoUsersFound() {
        // given
        String searchQuery = "unknown";
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchByFullNameOrIndex(searchQuery, pageable))
                .thenReturn(Collections.emptyList());

        // when
        List<UserIdentifiersResponseDTO> result = userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.STUDENT, 0, 10
        );

        // then
        assertThat(result).isEmpty();
        verify(userRepository).searchByFullNameOrIndex(searchQuery, pageable);
        verifyNoInteractions(userMapper);
    }

    @Test
    void existsByEmail_ShouldNotThrow_WhenUserExists() {
        // given
        when(userRepository.existsByEmail("jan@pjatk.edu.pl")).thenReturn(true);

        // when and then
        assertThatCode(() -> userService.existsByEmail("jan@pjatk.edu.pl")).doesNotThrowAnyException();
    }

    @Test
    void existsByEmail_ShouldThrowUserNotFound_WhenUserDoesNotExist() {
        // given
        when(userRepository.existsByEmail("unknown@pjatk.edu.pl")).thenReturn(false);

        // when and then
        assertThatThrownBy(() -> userService.existsByEmail("unknown@pjatk.edu.pl"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getLecturersByIds_ShouldReturnAllLecturers() {
        // given
        UserEntity first = userWithRole(Role.LECTURER);
        UserEntity second = userWithRole(Role.LECTURER);
        Set<UUID> ids = Set.of(first.getId(), second.getId());

        when(userRepository.findAllById(ids)).thenReturn(List.of(first, second));

        // when
        Set<UserEntity> result = userService.getLecturersByIds(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void getLecturersByIds_ShouldThrowResourceNotFound_WhenAnyIdIsUnknown() {
        // given
        UserEntity known = userWithRole(Role.LECTURER);
        UUID unknownId = UUID.randomUUID();
        Set<UUID> ids = Set.of(known.getId(), unknownId);

        when(userRepository.findAllById(ids)).thenReturn(List.of(known));

        // when and then
        assertThatThrownBy(() -> userService.getLecturersByIds(ids))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ExceptionErrorConstants.LECTURER_NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "EMPLOYEE", "USER", "ADMIN"})
    void getLecturersByIds_ShouldThrowNotALecturer_WhenUserHasAnotherRole(Role role) {
        // given
        UserEntity lecturer = userWithRole(Role.LECTURER);
        UserEntity other = userWithRole(role);
        Set<UUID> ids = Set.of(lecturer.getId(), other.getId());

        when(userRepository.findAllById(ids)).thenReturn(List.of(lecturer, other));

        // when and then
        assertThatThrownBy(() -> userService.getLecturersByIds(ids))
                .isInstanceOf(NotALecturerException.class)
                .hasMessage(ExceptionErrorConstants.USER_IS_NOT_A_LECTURER.formatted(other.getId()));
    }

    private UserEntity userWithRole(Role role) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }
}