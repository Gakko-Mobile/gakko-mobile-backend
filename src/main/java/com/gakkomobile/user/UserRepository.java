package com.gakkomobile.user;

import com.gakkomobile.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByIndexNumber(String index);

    boolean existsByEmail(String email);

    boolean existsByPesel(String pesel);

    @Query("""
        SELECT u FROM User u
        WHERE (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
           OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    List<User> searchByFullName(@Param("name") String name, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :nameOrIndex, '%'))
           OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :nameOrIndex, '%'))
           OR LOWER(u.indexNumber) LIKE LOWER(CONCAT('%', :nameOrIndex, '%'))
    """)
    List<User> searchByFullNameOrIndex(@Param("nameOrIndex") String nameOrIndex, Pageable pageable);
}
