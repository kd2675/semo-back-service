package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.FinanceBudget;

public interface FinanceBudgetRepository extends JpaRepository<FinanceBudget, Long> {
    List<FinanceBudget> findByFinancePeriodIdOrderByCategoryCodeAscFinanceBudgetIdAsc(Long financePeriodId);

    Optional<FinanceBudget> findByFinancePeriodIdAndCategoryCode(Long financePeriodId, String categoryCode);
}
