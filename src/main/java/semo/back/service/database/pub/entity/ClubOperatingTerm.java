package semo.back.service.database.pub.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(
        name = "club_operating_term",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_club_operating_term_name_start",
                        columnNames = {"club_id", "term_name", "start_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubOperatingTerm extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_operating_term_id")
    private Long clubOperatingTermId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "term_name", nullable = false, length = 100)
    private String termName;

    @Column(name = "term_type", nullable = false, length = 20)
    private String termType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "activated_by_club_profile_id")
    private Long activatedByClubProfileId;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "closed_by_club_profile_id")
    private Long closedByClubProfileId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public void update(String termName, String termType, LocalDate startDate, LocalDate endDate, String description) {
        this.termName = termName;
        this.termType = termType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public void activate(Long actorClubProfileId, LocalDateTime activatedAt) {
        this.statusCode = "ACTIVE";
        this.activatedByClubProfileId = actorClubProfileId;
        this.activatedAt = activatedAt;
        this.closedByClubProfileId = null;
        this.closedAt = null;
    }

    public void close(Long actorClubProfileId, LocalDateTime closedAt) {
        this.statusCode = "CLOSED";
        this.closedByClubProfileId = actorClubProfileId;
        this.closedAt = closedAt;
    }
}
