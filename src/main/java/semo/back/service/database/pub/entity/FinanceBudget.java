package semo.back.service.database.pub.entity;

import java.math.BigDecimal;

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
@Table(name = "finance_budget")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinanceBudget extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_budget_id")
    private Long financeBudgetId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "finance_period_id", nullable = false)
    private Long financePeriodId;

    @Column(name = "category_code", nullable = false, length = 40)
    private String categoryCode;

    @Column(name = "allocated_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    public void update(BigDecimal allocatedAmount, String note) {
        this.allocatedAmount = allocatedAmount;
        this.note = note;
    }
}
