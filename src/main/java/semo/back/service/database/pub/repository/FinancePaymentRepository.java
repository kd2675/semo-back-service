package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface FinancePaymentRepository extends JpaRepository<FinancePayment, Long> {
    List<FinancePayment> findByClubIdAndClubProfileIdOrderByFinancePaymentIdDesc(Long clubId, Long clubProfileId);

    List<FinancePayment> findByClubIdOrderByFinancePaymentIdAsc(Long clubId);

    List<FinancePayment> findByFinanceObligationIdOrderByFinancePaymentIdDesc(Long financeObligationId);

    List<FinancePayment> findByFinanceObligationIdIn(Collection<Long> financeObligationIds);

    Optional<FinancePayment> findByFinancePaymentIdAndClubId(Long financePaymentId, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from FinancePayment payment
            where payment.financePaymentId = :financePaymentId
              and payment.clubId = :clubId
            """)
    Optional<FinancePayment> findForUpdate(
            @Param("financePaymentId") Long financePaymentId,
            @Param("clubId") Long clubId
    );

    @Query("""
            select coalesce(sum(payment.amount), 0)
            from FinancePayment payment, FinanceObligation obligation
            where payment.financeObligationId = obligation.financeObligationId
              and obligation.financePeriodId = :financePeriodId
              and payment.paymentStatusCode = 'PAID'
            """)
    java.math.BigDecimal sumPaidAmountByFinancePeriodId(Long financePeriodId);

    @Query("""
            select new semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate(
                count(p),
                coalesce(sum(case when p.paymentStatusCode = 'PENDING' then 1 else 0 end), 0),
                coalesce(sum(case when p.paymentStatusCode = 'PAID' then 1 else 0 end), 0),
                coalesce(sum(case when p.paymentStatusCode = 'WAIVED' then 1 else 0 end), 0),
                coalesce(sum(case when p.paymentStatusCode = 'PENDING' and o.dueAt is not null and o.dueAt < :now then 1 else 0 end), 0),
                coalesce(sum(p.amount), 0),
                coalesce(sum(case when p.paymentStatusCode = 'PAID' then p.amount else 0 end), 0),
                coalesce(sum(case when p.paymentStatusCode = 'PENDING' then p.amount else 0 end), 0),
                coalesce(sum(case when p.paymentStatusCode = 'WAIVED' then p.amount else 0 end), 0)
            )
            from FinancePayment p, FinanceObligation o
            where p.clubId = :clubId
              and o.financeObligationId = p.financeObligationId
            """)
    ClubAdminFinanceSummaryAggregate summarizeAdminFinance(Long clubId, LocalDateTime now);

    @Query("""
            select count(p)
            from FinancePayment p
            where p.clubId = :clubId
              and p.clubProfileId = :clubProfileId
              and p.paymentStatusCode = 'PENDING'
            """)
    long countPendingForMember(Long clubId, Long clubProfileId);

    @Query("""
            select count(p)
            from FinancePayment p, FinanceObligation o
            where p.financeObligationId = o.financeObligationId
              and p.clubId = :clubId
              and p.clubProfileId = :clubProfileId
              and p.paymentStatusCode = 'PENDING'
              and o.dueAt is not null
              and o.dueAt < :now
            """)
    long countOverdueForMember(Long clubId, Long clubProfileId, LocalDateTime now);
}
