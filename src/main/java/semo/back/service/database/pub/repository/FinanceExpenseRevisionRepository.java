package semo.back.service.database.pub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.FinanceExpenseRevision;

public interface FinanceExpenseRevisionRepository extends JpaRepository<FinanceExpenseRevision, Long> {
    List<FinanceExpenseRevision> findByFinanceExpenseIdOrderByFinanceExpenseRevisionIdDesc(Long financeExpenseId);
}
