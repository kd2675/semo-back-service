package semo.back.service.database.pub.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "finance_period")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinancePeriod extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_period_id")
    private Long financePeriodId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_operating_term_id")
    private Long clubOperatingTermId;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 14, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "closed_by_club_profile_id")
    private Long closedByClubProfileId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "note", length = 1000)
    private String note;

    public boolean includes(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public void close(Long actorClubProfileId, LocalDateTime closedAt, BigDecimal closingBalance) {
        this.statusCode = "CLOSED";
        this.closedByClubProfileId = actorClubProfileId;
        this.closedAt = closedAt;
        this.closingBalance = closingBalance;
    }
}
