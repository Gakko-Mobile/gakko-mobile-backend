package com.gakkomobile.academicgroup;

import com.gakkomobile.groupmembership.GroupMembership;
import com.gakkomobile.studies.Studies;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "academic_groups")
@Getter
@Setter
public class AcademicGroup {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

//    @Column(name = "name", length = 50, unique = true, nullable = false)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "academic_year", length = 10, nullable = false)
    private String academicYear;

//    @Column(name = "semester", nullable = false)
//    private int semester;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMembership> memberships = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studies_id", nullable = false)
    private Studies studies;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
