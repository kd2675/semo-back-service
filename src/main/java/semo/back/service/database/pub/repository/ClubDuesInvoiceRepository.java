package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubDuesInvoice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubDuesInvoiceRepository extends JpaRepository<ClubDuesInvoice, Long> {
    List<ClubDuesInvoice> findByClubIdAndClubProfileIdOrderByBillingYearDescBillingMonthDescClubDuesInvoiceIdDesc(
            Long clubId,
            Long clubProfileId
    );

    List<ClubDuesInvoice> findByClubIdOrderByBillingYearDescBillingMonthDescClubDuesInvoiceIdDesc(Long clubId);

    List<ClubDuesInvoice> findByClubIdAndBillingYearAndBillingMonthAndClubProfileIdIn(
            Long clubId,
            Short billingYear,
            Byte billingMonth,
            Collection<Long> clubProfileIds
    );

    Optional<ClubDuesInvoice> findByClubDuesInvoiceIdAndClubId(Long clubDuesInvoiceId, Long clubId);
}
