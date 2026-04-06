package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.FinanceExpense;

import java.math.BigDecimal;
import java.util.List;

public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, Long> {
    List<FinanceExpense> findByClubIdOrderBySpentAtDescFinanceExpenseIdDesc(Long clubId);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from FinanceExpense e
            where e.clubId = :clubId
            """)
    BigDecimal sumAmountByClubId(Long clubId);
}
