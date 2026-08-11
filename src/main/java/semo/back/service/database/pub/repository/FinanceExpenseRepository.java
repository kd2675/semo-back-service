package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.FinanceExpense;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import jakarta.persistence.LockModeType;

public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, Long> {
    List<FinanceExpense> findByClubIdOrderBySpentAtDescFinanceExpenseIdDesc(Long clubId);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from FinanceExpense e
            where e.clubId = :clubId
              and e.statusCode = 'POSTED'
            """)
    BigDecimal sumAmountByClubId(Long clubId);

    Optional<FinanceExpense> findByFinanceExpenseIdAndClubId(Long financeExpenseId, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select expense
            from FinanceExpense expense
            where expense.financeExpenseId = :financeExpenseId
              and expense.clubId = :clubId
            """)
    Optional<FinanceExpense> findForUpdate(
            @Param("financeExpenseId") Long financeExpenseId,
            @Param("clubId") Long clubId
    );

    @Query("""
            select coalesce(sum(expense.amount), 0)
            from FinanceExpense expense
            where expense.financePeriodId = :financePeriodId
              and expense.statusCode = 'POSTED'
            """)
    BigDecimal sumPostedAmountByFinancePeriodId(Long financePeriodId);

    @Query("""
            select coalesce(sum(expense.amount), 0)
            from FinanceExpense expense
            where expense.financePeriodId = :financePeriodId
              and expense.categoryCode = :categoryCode
              and expense.statusCode = 'POSTED'
            """)
    BigDecimal sumPostedAmountByFinancePeriodIdAndCategoryCode(Long financePeriodId, String categoryCode);

    @Query("""
            select coalesce(sum(expense.amount), 0)
            from FinanceExpense expense
            where expense.clubId = :clubId
              and expense.spentAt >= :from
              and expense.spentAt < :toExclusive
            """)
    BigDecimal sumAmountWithinTerm(Long clubId, LocalDateTime from, LocalDateTime toExclusive);
}
