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
@Table(name = "club_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubFeedback extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "submitter_club_profile_id", nullable = false)
    private Long submitterClubProfileId;

    @Column(name = "feedback_type", nullable = false, length = 40)
    private String feedbackType;

    @Column(name = "visibility_scope", nullable = false, length = 20)
    private String visibilityScope;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "admin_answer", columnDefinition = "TEXT")
    private String adminAnswer;

    @Column(name = "answered_by_club_profile_id")
    private Long answeredByClubProfileId;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
