package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.DuesCharge;

import java.util.List;
import java.util.Optional;

public interface DuesChargeRepository extends JpaRepository<DuesCharge, Long> {
    List<DuesCharge> findByClubIdOrderByDuesChargeIdDesc(Long clubId);

    Optional<DuesCharge> findByDuesChargeIdAndClubId(Long duesChargeId, Long clubId);
}
