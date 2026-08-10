package semo.back.service.database.pub.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.FinancePeriod;

public interface FinancePeriodRepository extends JpaRepository<FinancePeriod, Long> {
    List<FinancePeriod> findByClubIdOrderByStartDateDescFinancePeriodIdDesc(Long clubId);

    Optional<FinancePeriod> findByFinancePeriodIdAndClubId(Long financePeriodId, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select period
            from FinancePeriod period
            where period.financePeriodId = :financePeriodId
              and period.clubId = :clubId
            """)
    Optional<FinancePeriod> findForUpdate(
            @Param("financePeriodId") Long financePeriodId,
            @Param("clubId") Long clubId
    );

    @Query("""
            select period
            from FinancePeriod period
            where period.clubId = :clubId
              and period.startDate <= :date
              and period.endDate >= :date
            order by period.startDate desc, period.financePeriodId desc
            """)
    List<FinancePeriod> findContainingDate(Long clubId, LocalDate date);

    @Query("""
            select count(period)
            from FinancePeriod period
            where period.clubId = :clubId
              and period.startDate <= :endDate
              and period.endDate >= :startDate
            """)
    long countOverlapping(Long clubId, LocalDate startDate, LocalDate endDate);
}
