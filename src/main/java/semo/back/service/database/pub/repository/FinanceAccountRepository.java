package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.FinanceAccount;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {
    List<FinanceAccount> findByClubIdOrderByActiveDescDisplayNameAscFinanceAccountIdAsc(Long clubId);

    List<FinanceAccount> findByClubIdAndActiveTrue(Long clubId);

    Optional<FinanceAccount> findByFinanceAccountIdAndClubId(Long financeAccountId, Long clubId);
}
