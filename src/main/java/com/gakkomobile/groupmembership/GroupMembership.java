package com.gakkomobile.groupmembership;

import com.gakkomobile.academicgroup.entity.AcademicGroup;
import com.gakkomobile.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_groups")
@Getter
@Setter
@NoArgsConstructor
public class GroupMembership {

    @EmbeddedId
    private GroupMembershipId id = new GroupMembershipId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private AcademicGroup group;

    @Column(name = "is_itn", nullable = false)
    private boolean isItn = false;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    public GroupMembership(User user, AcademicGroup group, boolean isItn) {
        this.user = user;
        this.group = group;
        this.isItn = isItn;
    }
}
