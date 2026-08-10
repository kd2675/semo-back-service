package semo.back.service.database.pub.entity;

import java.math.BigDecimal;
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
@Table(name = "finance_expense_revision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinanceExpenseRevision extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_expense_revision_id")
    private Long financeExpenseRevisionId;

    @Column(name = "finance_expense_id", nullable = false)
    private Long financeExpenseId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "revised_by_club_profile_id", nullable = false)
    private Long revisedByClubProfileId;

    @Column(name = "revision_type_code", nullable = false, length = 20)
    private String revisionTypeCode;

    @Column(name = "previous_title", nullable = false, length = 200)
    private String previousTitle;

    @Column(name = "next_title", length = 200)
    private String nextTitle;

    @Column(name = "previous_category_code", nullable = false, length = 40)
    private String previousCategoryCode;

    @Column(name = "next_category_code", length = 40)
    private String nextCategoryCode;

    @Column(name = "previous_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal previousAmount;

    @Column(name = "next_amount", precision = 14, scale = 2)
    private BigDecimal nextAmount;

    @Column(name = "previous_spent_at", nullable = false)
    private LocalDateTime previousSpentAt;

    @Column(name = "next_spent_at")
    private LocalDateTime nextSpentAt;

    @Column(name = "previous_schedule_event_id")
    private Long previousScheduleEventId;

    @Column(name = "next_schedule_event_id")
    private Long nextScheduleEventId;

    @Column(name = "previous_finance_account_id")
    private Long previousFinanceAccountId;

    @Column(name = "next_finance_account_id")
    private Long nextFinanceAccountId;

    @Column(name = "previous_finance_period_id")
    private Long previousFinancePeriodId;

    @Column(name = "next_finance_period_id")
    private Long nextFinancePeriodId;

    @Column(name = "previous_note", length = 1000)
    private String previousNote;

    @Column(name = "next_note", length = 1000)
    private String nextNote;

    @Column(name = "previous_status_code", nullable = false, length = 20)
    private String previousStatusCode;

    @Column(name = "next_status_code", nullable = false, length = 20)
    private String nextStatusCode;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;
}
