package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.subject.SubjectEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "group_classes")
@Getter
@Setter
public class GroupClassEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "academic_groups_group_classes",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<AcademicGroupEntity> groups;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Column(name = "start_timestamp", nullable = false)
    private OffsetDateTime startTimestamp;

    @Column(name = "end_timestamp", nullable = false)
    private OffsetDateTime endTimestamp;

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
