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
@Table(name = "club_join_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubJoinRequest extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_join_request_id")
    private Long clubJoinRequestId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "request_message", length = 500)
    private String requestMessage;

    @Column(name = "request_status", nullable = false, length = 20)
    private String requestStatus;

    @Column(name = "reviewed_by_profile_id")
    private Long reviewedByProfileId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void resubmit(String requestMessage) {
        this.requestMessage = requestMessage;
        this.requestStatus = "PENDING";
        this.reviewedByProfileId = null;
        this.reviewedAt = null;
    }

    public void approve(Long reviewerProfileId, LocalDateTime reviewedAt) {
        this.requestStatus = "APPROVED";
        this.reviewedByProfileId = reviewerProfileId;
        this.reviewedAt = reviewedAt;
    }

    public void reject(Long reviewerProfileId, LocalDateTime reviewedAt) {
        this.requestStatus = "REJECTED";
        this.reviewedByProfileId = reviewerProfileId;
        this.reviewedAt = reviewedAt;
    }

    public void cancel(LocalDateTime reviewedAt) {
        this.requestStatus = "CANCELED";
        this.reviewedByProfileId = null;
        this.reviewedAt = reviewedAt;
    }
}
