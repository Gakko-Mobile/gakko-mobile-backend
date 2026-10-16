package com.gakkomobile.user;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.user.dto.UserIdentifiersResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void searchUserIdentifiers_WhenSearchingStudents_ShouldCallSearchByFullNameOrIndex() {
        String searchQuery = "danisa";
        int pageNum = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(pageNum, limit);

        UserEntity student = new UserEntity();
        student.setEmail("danisa@gmail.com");
        student.setFirstName("Danisa");
        student.setLastName("Novikova");
        student.setIndexNumber("s12345");

        when(userRepository.searchByFullNameOrIndex(searchQuery, pageable))
                .thenReturn(List.of(student));

        List<UserIdentifiersResponse> result = userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.STUDENT, pageNum, limit
        );

        assertThat(result).hasSize(1);
        UserIdentifiersResponse response = result.get(0);
        assertThat(response.email()).isEqualTo("danisa@gmail.com");
        assertThat(response.fullName()).isEqualTo("Novikova Danisa");
        assertThat(response.index()).isEqualTo("s12345");

        verify(userRepository).searchByFullNameOrIndex(searchQuery, pageable);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void searchUserIdentifiers_WhenSearchingLecturers_ShouldCallSearchByFullName() {
        String searchQuery = "kowalski";
        int pageNum = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(pageNum, limit);

        UserEntity lecturer = new UserEntity();
        lecturer.setEmail("jan@pjatk.edu.pl");
        lecturer.setFirstName("Jan");
        lecturer.setLastName("Kowalski");
        lecturer.setIndexNumber("emp123");

        when(userRepository.searchByFullName(searchQuery, pageable))
                .thenReturn(List.of(lecturer));

        List<UserIdentifiersResponse> result = userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.LECTURER, pageNum, limit
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Kowalski Jan");

        verify(userRepository).searchByFullName(searchQuery, pageable);
    }

    @Test
    void searchUserIdentifiers_WhenSearchingOtherRolesAsAdmin_ShouldCallSearchByFullName() {
        String searchQuery = "admin";
        int pageNum = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(pageNum, limit);

        UserEntity employee = new UserEntity();
        employee.setEmail("emp@pjatk.edu.pl");
        employee.setFirstName("Anna");
        employee.setLastName("Nowak");

        when(userRepository.searchByFullName(searchQuery, pageable))
                .thenReturn(List.of(employee));

        List<UserIdentifiersResponse> result = userService.searchUserIdentifiers(
                Role.ADMIN, searchQuery, Role.EMPLOYEE, pageNum, limit
        );

        assertThat(result).hasSize(1);
        verify(userRepository).searchByFullName(searchQuery, pageable);
    }

    @Test
    void searchUserIdentifiers_WhenSearchingOtherRolesAsNonAdmin_ShouldThrowForbidden() {
        String searchQuery = "admin";
        int pageNum = 0;
        int limit = 10;

        assertThatThrownBy(() -> userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.EMPLOYEE, pageNum, limit
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(ExceptionErrorConstants.FORBIDDEN)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verifyNoInteractions(userRepository);
    }

    @Test
    void searchUserIdentifiers_ShouldReturnEmptyList_WhenNoUsersFound() {
        String searchQuery = "unknown";
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchByFullNameOrIndex(searchQuery, pageable))
                .thenReturn(Collections.emptyList());

        List<UserIdentifiersResponse> result = userService.searchUserIdentifiers(
                Role.STUDENT, searchQuery, Role.STUDENT, 0, 10
        );

        assertThat(result).isEmpty();
        verify(userRepository).searchByFullNameOrIndex(searchQuery, pageable);
    }
}