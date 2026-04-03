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
@Table(name = "todo_item_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoItemApplication extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_item_application_id")
    private Long todoItemApplicationId;

    @Column(name = "todo_item_id", nullable = false)
    private Long todoItemId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "application_status", nullable = false, length = 20)
    private String applicationStatus;

    @Column(name = "application_note", length = 500)
    private String applicationNote;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_by_club_profile_id")
    private Long reviewedByClubProfileId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void markApplied(String applicationNote) {
        this.applicationStatus = "APPLIED";
        this.applicationNote = applicationNote;
        this.reviewNote = null;
        this.reviewedByClubProfileId = null;
        this.reviewedAt = null;
    }

    public void review(String applicationStatus, String reviewNote, Long reviewedByClubProfileId, LocalDateTime reviewedAt) {
        this.applicationStatus = applicationStatus;
        this.reviewNote = reviewNote;
        this.reviewedByClubProfileId = reviewedByClubProfileId;
        this.reviewedAt = reviewedAt;
    }

    public void withdraw() {
        this.applicationStatus = "WITHDRAWN";
        this.reviewNote = null;
        this.reviewedByClubProfileId = null;
        this.reviewedAt = null;
    }
}
