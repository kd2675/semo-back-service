package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.FinanceObligation;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface FinanceObligationRepository extends JpaRepository<FinanceObligation, Long> {
    long countByClubId(Long clubId);

    List<FinanceObligation> findByClubIdOrderByFinanceObligationIdAsc(Long clubId);

    long countByFinancePeriodIdAndStatusCode(Long financePeriodId, String statusCode);

    @Query("""
            select o
            from FinanceObligation o
            where o.clubId = :clubId
              and (:cursorObligationId is null or o.financeObligationId < :cursorObligationId)
              and (
                    :searchText is null
                    or lower(o.title) like concat('%', :searchText, '%')
                    or lower(coalesce(o.note, '')) like concat('%', :searchText, '%')
                    or exists (
                        select 1
                        from ClubProfile issuer
                        where issuer.clubProfileId = o.createdByClubProfileId
                          and lower(issuer.displayName) like concat('%', :searchText, '%')
                    )
                    or exists (
                        select 1
                        from FinancePayment p, ClubProfile billed
                        where p.financeObligationId = o.financeObligationId
                          and billed.clubProfileId = p.clubProfileId
                          and lower(billed.displayName) like concat('%', :searchText, '%')
                    )
                  )
              and (
                    :obligationFilter is null
                    or (:obligationFilter = 'OPEN' and o.statusCode = 'OPEN')
                    or (:obligationFilter = 'SETTLED' and o.statusCode = 'CLOSED')
                  )
            order by o.financeObligationId desc
            """)
    List<FinanceObligation> findAdminFeed(
            Long clubId,
            Long cursorObligationId,
            String searchText,
            String obligationFilter,
            Pageable pageable
    );

    Optional<FinanceObligation> findByFinanceObligationIdAndClubId(Long financeObligationId, Long clubId);

    Optional<FinanceObligation> findByRecurrenceSourceFinanceObligationId(Long recurrenceSourceFinanceObligationId);

    Optional<FinanceObligation> findBySourceTournamentApplicationId(Long sourceTournamentApplicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select obligation
            from FinanceObligation obligation
            where obligation.financeObligationId = :financeObligationId
              and obligation.clubId = :clubId
            """)
    Optional<FinanceObligation> findForUpdate(
            @Param("financeObligationId") Long financeObligationId,
            @Param("clubId") Long clubId
    );
}
