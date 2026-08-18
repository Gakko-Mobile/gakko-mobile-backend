package com.gakkomobile.security.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "blacklisted_tokens")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlacklistedToken {
    @Id
    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;
}
