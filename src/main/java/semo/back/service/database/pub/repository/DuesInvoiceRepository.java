package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.DuesInvoice;
import semo.back.service.feature.dues.vo.ClubAdminDuesSummaryAggregate;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DuesInvoiceRepository extends JpaRepository<DuesInvoice, Long> {
    List<DuesInvoice> findByClubIdAndClubProfileIdOrderByDuesInvoiceIdDesc(Long clubId, Long clubProfileId);

    List<DuesInvoice> findByClubIdOrderByDuesInvoiceIdDesc(Long clubId);

    List<DuesInvoice> findByDuesChargeIdOrderByDuesInvoiceIdDesc(Long duesChargeId);

    List<DuesInvoice> findByDuesChargeIdIn(Collection<Long> duesChargeIds);

    Optional<DuesInvoice> findByDuesInvoiceIdAndClubId(Long duesInvoiceId, Long clubId);

    @Query("""
            select new semo.back.service.feature.dues.vo.ClubAdminDuesSummaryAggregate(
                count(di),
                coalesce(sum(case when di.paymentStatus = 'PENDING' then 1 else 0 end), 0),
                coalesce(sum(case when di.paymentStatus = 'PAID' then 1 else 0 end), 0),
                coalesce(sum(case when di.paymentStatus = 'WAIVED' then 1 else 0 end), 0),
                coalesce(sum(case when di.paymentStatus = 'PENDING' and dc.dueAt is not null and dc.dueAt < :now then 1 else 0 end), 0)
            )
            from DuesInvoice di, DuesCharge dc
            where di.clubId = :clubId
              and dc.duesChargeId = di.duesChargeId
            """)
    ClubAdminDuesSummaryAggregate summarizeAdminDues(Long clubId, LocalDateTime now);
}
