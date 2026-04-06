package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.FinanceRequest;

import java.util.List;
import java.util.Optional;

public interface FinanceRequestRepository extends JpaRepository<FinanceRequest, Long> {
    List<FinanceRequest> findByClubIdAndRequesterClubProfileIdOrderByFinanceRequestIdDesc(Long clubId, Long requesterClubProfileId);

    List<FinanceRequest> findByClubIdOrderByFinanceRequestIdDesc(Long clubId);

    long countByClubIdAndStatusCode(Long clubId, String statusCode);

    Optional<FinanceRequest> findByFinanceRequestIdAndClubId(Long financeRequestId, Long clubId);
}
