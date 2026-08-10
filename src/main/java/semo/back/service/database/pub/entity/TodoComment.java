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
@Table(name = "todo_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoComment extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_comment_id")
    private Long todoCommentId;

    @Column(name = "todo_item_id", nullable = false)
    private Long todoItemId;

    @Column(name = "author_club_profile_id", nullable = false)
    private Long authorClubProfileId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_by_club_profile_id")
    private Long deletedByClubProfileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete(Long actorClubProfileId, LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedByClubProfileId = actorClubProfileId;
        this.deletedAt = deletedAt;
    }
}
