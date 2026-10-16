package com.gakkomobile.event;

import com.gakkomobile.building.Building;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organizator", length = 100, nullable = false)
    private String organizator;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "start_timestamp", nullable = false)
    private OffsetDateTime startTimestamp;

    @Column(name = "end_timestamp", nullable = false)
    private OffsetDateTime endTimestamp;

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
