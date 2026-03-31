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
@Table(name = "tournament_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentApplication extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_application_id")
    private Long tournamentApplicationId;

    @Column(name = "tournament_record_id", nullable = false)
    private Long tournamentRecordId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "application_status", nullable = false, length = 20)
    private String applicationStatus;

    @Column(name = "application_note", length = 500)
    private String applicationNote;

    @Column(name = "reviewed_by_club_profile_id")
    private Long reviewedByClubProfileId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void markApplied(String applicationNote) {
        this.applicationStatus = "APPLIED";
        this.applicationNote = applicationNote;
        this.reviewedByClubProfileId = null;
        this.reviewedAt = null;
    }

    public void review(String applicationStatus, Long reviewedByClubProfileId, LocalDateTime reviewedAt) {
        this.applicationStatus = applicationStatus;
        this.reviewedByClubProfileId = reviewedByClubProfileId;
        this.reviewedAt = reviewedAt;
    }
}
