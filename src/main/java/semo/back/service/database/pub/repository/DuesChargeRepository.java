package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.DuesCharge;

import java.util.List;
import java.util.Optional;

public interface DuesChargeRepository extends JpaRepository<DuesCharge, Long> {
    List<DuesCharge> findByClubIdOrderByDuesChargeIdDesc(Long clubId);

    long countByClubId(Long clubId);

    @Query("""
            select c
            from DuesCharge c
            where c.clubId = :clubId
              and (:cursorChargeId is null or c.duesChargeId < :cursorChargeId)
              and (
                    :searchText is null
                    or lower(c.title) like concat('%', :searchText, '%')
                    or lower(coalesce(c.note, '')) like concat('%', :searchText, '%')
                    or exists (
                        select 1
                        from ClubProfile issuer
                        where issuer.clubProfileId = c.issuedByClubProfileId
                          and lower(issuer.displayName) like concat('%', :searchText, '%')
                    )
                    or exists (
                        select 1
                        from DuesInvoice di, ClubProfile billed
                        where di.duesChargeId = c.duesChargeId
                          and billed.clubProfileId = di.clubProfileId
                          and lower(billed.displayName) like concat('%', :searchText, '%')
                    )
                  )
              and (
                    :chargeFilter is null
                    or (
                        :chargeFilter = 'OPEN'
                        and exists (
                            select 1
                            from DuesInvoice di
                            where di.duesChargeId = c.duesChargeId
                              and di.paymentStatus = 'PENDING'
                        )
                    )
                    or (
                        :chargeFilter = 'SETTLED'
                        and not exists (
                            select 1
                            from DuesInvoice di
                            where di.duesChargeId = c.duesChargeId
                              and di.paymentStatus = 'PENDING'
                        )
                    )
                  )
            order by c.duesChargeId desc
            """)
    List<DuesCharge> findAdminFeed(
            Long clubId,
            Long cursorChargeId,
            String searchText,
            String chargeFilter,
            Pageable pageable
    );

    Optional<DuesCharge> findByDuesChargeIdAndClubId(Long duesChargeId, Long clubId);
}
