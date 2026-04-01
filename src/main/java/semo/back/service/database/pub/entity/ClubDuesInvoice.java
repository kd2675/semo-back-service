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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import semo.back.service.common.jpa.CommonDateEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "club_dues_invoice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubDuesInvoice extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_dues_invoice_id")
    private Long clubDuesInvoiceId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "billing_year", nullable = false, columnDefinition = "SMALLINT")
    private Short billingYear;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "billing_month", nullable = false, columnDefinition = "TINYINT")
    private Byte billingMonth;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "note", length = 500)
    private String note;
}
