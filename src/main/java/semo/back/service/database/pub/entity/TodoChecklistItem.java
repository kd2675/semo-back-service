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
@Table(name = "todo_checklist_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoChecklistItem extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_checklist_item_id")
    private Long todoChecklistItemId;

    @Column(name = "todo_item_id", nullable = false)
    private Long todoItemId;

    @Column(name = "content", nullable = false, length = 300)
    private String content;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_by_club_profile_id")
    private Long completedByClubProfileId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void update(String content, boolean completed, Long actorClubProfileId, LocalDateTime changedAt) {
        this.content = content;
        if (this.completed == completed) {
            return;
        }
        this.completed = completed;
        this.completedByClubProfileId = completed ? actorClubProfileId : null;
        this.completedAt = completed ? changedAt : null;
    }
}
