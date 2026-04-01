package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.DuesInvoice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DuesInvoiceRepository extends JpaRepository<DuesInvoice, Long> {
    List<DuesInvoice> findByClubIdAndClubProfileIdOrderByDuesInvoiceIdDesc(Long clubId, Long clubProfileId);

    List<DuesInvoice> findByClubIdOrderByDuesInvoiceIdDesc(Long clubId);

    List<DuesInvoice> findByDuesChargeIdOrderByDuesInvoiceIdDesc(Long duesChargeId);

    List<DuesInvoice> findByDuesChargeIdIn(Collection<Long> duesChargeIds);

    Optional<DuesInvoice> findByDuesInvoiceIdAndClubId(Long duesInvoiceId, Long clubId);
}
