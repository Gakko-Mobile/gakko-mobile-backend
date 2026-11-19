package com.gakkomobile.groupclass;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "class_slots")
@Getter
@Setter
public class ClassSlotEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_class_id", nullable = false)
    private GroupClassEntity groupClass;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Column(name = "ms_teams_code", length = 100)
    private String msTeamsCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (Objects.isNull(this.id)) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
