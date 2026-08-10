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

import java.time.LocalDateTime;

@Entity
@Table(name = "todo_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoItem extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_item_id")
    private Long todoItemId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "assigned_club_profile_id")
    private Long assignedClubProfileId;

    @Column(name = "assigned_by_club_profile_id")
    private Long assignedByClubProfileId;

    @Column(name = "todo_type", nullable = false, length = 30)
    private String todoType;

    @Column(name = "assignment_mode", nullable = false, length = 30)
    private String assignmentMode;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Builder.Default
    @Column(name = "priority_code", nullable = false, length = 20)
    private String priorityCode = "NORMAL";

    @Builder.Default
    @Column(name = "recruitment_capacity", nullable = false)
    private Integer recruitmentCapacity = 1;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "work_start_at")
    private LocalDateTime workStartAt;

    @Column(name = "work_end_at")
    private LocalDateTime workEndAt;

    @Column(name = "linked_schedule_event_id")
    private Long linkedScheduleEventId;

    @Column(name = "completed_by_club_profile_id")
    private Long completedByClubProfileId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
