package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.FinanceObligation;

import java.util.List;
import java.util.Optional;

public interface FinanceObligationRepository extends JpaRepository<FinanceObligation, Long> {
    long countByClubId(Long clubId);

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
}
