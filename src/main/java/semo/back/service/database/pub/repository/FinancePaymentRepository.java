package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FinancePaymentRepository extends JpaRepository<FinancePayment, Long> {
    List<FinancePayment> findByClubIdAndClubProfileIdOrderByFinancePaymentIdDesc(Long clubId, Long clubProfileId);

    List<FinancePayment> findByFinanceObligationIdOrderByFinancePaymentIdDesc(Long financeObligationId);

    List<FinancePayment> findByFinanceObligationIdIn(Collection<Long> financeObligationIds);

    Optional<FinancePayment> findByFinancePaymentIdAndClubId(Long financePaymentId, Long clubId);

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
