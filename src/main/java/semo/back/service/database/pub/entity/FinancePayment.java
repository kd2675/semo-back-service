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
@Table(name = "finance_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinancePayment extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_payment_id")
    private Long financePaymentId;

    @Column(name = "finance_obligation_id", nullable = false)
    private Long financeObligationId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "finance_account_id")
    private Long financeAccountId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "payment_status_code", nullable = false, length = 20)
    private String paymentStatusCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_method_code", length = 20)
    private String paymentMethodCode;

    @Column(name = "note", length = 500)
    private String note;

    public void updateStatus(
            String paymentStatusCode,
            LocalDateTime paidAt,
            Long financeAccountId,
            String paymentMethodCode,
            String note
    ) {
        this.paymentStatusCode = paymentStatusCode;
        this.paidAt = paidAt;
        this.financeAccountId = financeAccountId;
        this.paymentMethodCode = paymentMethodCode;
        this.note = note;
    }
}
