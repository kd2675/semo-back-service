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
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_expense")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinanceExpense extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_expense_id")
    private Long financeExpenseId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "entered_by_club_profile_id", nullable = false)
    private Long enteredByClubProfileId;

    @Column(name = "source_finance_request_id", unique = true)
    private Long sourceFinanceRequestId;

    @Column(name = "expense_type_code", nullable = false, length = 30)
    private String expenseTypeCode;

    @Column(name = "category_code", nullable = false, length = 40)
    private String categoryCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "spent_at", nullable = false)
    private LocalDateTime spentAt;

    @Column(name = "related_event_name", length = 120)
    private String relatedEventName;

    @Column(name = "note", length = 1000)
    private String note;
}
