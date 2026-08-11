package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.FinanceRequest;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface FinanceRequestRepository extends JpaRepository<FinanceRequest, Long> {
    List<FinanceRequest> findByClubIdAndRequesterClubProfileIdOrderByFinanceRequestIdDesc(Long clubId, Long requesterClubProfileId);

    List<FinanceRequest> findByClubIdOrderByFinanceRequestIdDesc(Long clubId);

    List<FinanceRequest> findByClubIdOrderByFinanceRequestIdDesc(Long clubId, Pageable pageable);

    long countByClubIdAndStatusCode(Long clubId, String statusCode);

    List<FinanceRequest> findByClubIdAndStatusCodeOrderByFinanceRequestIdDesc(
            Long clubId,
            String statusCode,
            Pageable pageable
    );

    List<FinanceRequest> findByClubIdAndStatusCodeOrderByFinanceRequestIdDesc(Long clubId, String statusCode);

    long countByClubIdAndCreateDateGreaterThanEqualAndCreateDateLessThan(
            Long clubId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime toExclusive
    );

    long countByClubIdAndRequesterClubProfileIdAndStatusCode(
            Long clubId,
            Long requesterClubProfileId,
            String statusCode
    );

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
