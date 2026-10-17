package com.gakkomobile.notifications.firebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface UserDeviceTokenRepository extends JpaRepository<UserDeviceTokenEntity, UUID> {

    List<UserDeviceTokenEntity> findAllByUserId(UUID userId);

    Optional<UserDeviceTokenEntity> findByToken(String token);

    void deleteByUserIdAndToken(UUID userId, String token);
}
