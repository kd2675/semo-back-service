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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_obligation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinanceObligation extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_obligation_id")
    private Long financeObligationId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "created_by_club_profile_id")
    private Long createdByClubProfileId;

    @Column(name = "finance_period_id")
    private Long financePeriodId;

    @Column(name = "finance_account_id")
    private Long financeAccountId;

    @Column(name = "linked_schedule_event_id")
    private Long linkedScheduleEventId;

    @Column(name = "obligation_type_code", nullable = false, length = 30)
    private String obligationTypeCode;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "target_scope_code", nullable = false, length = 30)
    private String targetScopeCode;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "note", length = 500)
    private String note;

    @Builder.Default
    @Column(name = "recurrence_frequency", nullable = false, length = 20)
    private String recurrenceFrequency = "NONE";

    @Builder.Default
    @Column(name = "recurrence_interval", nullable = false)
    private int recurrenceInterval = 1;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "recurrence_source_finance_obligation_id", unique = true)
    private Long recurrenceSourceFinanceObligationId;

    public void updateStatus(String statusCode) {
        this.statusCode = statusCode;
    }
}
