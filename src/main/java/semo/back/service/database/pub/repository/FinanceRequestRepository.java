package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.FinanceRequest;

import java.util.List;
import java.util.Optional;

public interface FinanceRequestRepository extends JpaRepository<FinanceRequest, Long> {
    List<FinanceRequest> findByClubIdAndRequesterClubProfileIdOrderByFinanceRequestIdDesc(Long clubId, Long requesterClubProfileId);

    List<FinanceRequest> findByClubIdOrderByFinanceRequestIdDesc(Long clubId);

    long countByClubIdAndStatusCode(Long clubId, String statusCode);

    Optional<FinanceRequest> findByFinanceRequestIdAndClubId(Long financeRequestId, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from FinanceRequest request
            where request.financeRequestId = :financeRequestId
              and request.clubId = :clubId
            """)
    Optional<FinanceRequest> findForUpdate(
            @Param("financeRequestId") Long financeRequestId,
            @Param("clubId") Long clubId
    );
}
