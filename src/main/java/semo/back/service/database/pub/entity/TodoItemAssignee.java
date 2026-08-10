package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(name = "todo_item_assignee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoItemAssignee extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_item_assignee_id")
    private Long todoItemAssigneeId;

    @Column(name = "todo_item_id", nullable = false)
    private Long todoItemId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "assigned_by_club_profile_id", nullable = false)
    private Long assignedByClubProfileId;
}
