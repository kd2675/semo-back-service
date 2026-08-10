package semo.back.service.database.pub.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubOperatingTerm;

public interface ClubOperatingTermRepository extends JpaRepository<ClubOperatingTerm, Long> {
    List<ClubOperatingTerm> findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(Long clubId);

    Optional<ClubOperatingTerm> findByClubOperatingTermIdAndClubId(Long clubOperatingTermId, Long clubId);

    Optional<ClubOperatingTerm> findFirstByClubIdAndStatusCodeOrderByStartDateDescClubOperatingTermIdDesc(
            Long clubId,
            String statusCode
    );

    @Query("""
            select term
            from ClubOperatingTerm term
            where term.clubId = :clubId
              and term.statusCode = 'PLANNED'
              and term.startDate >= :fromDate
            order by term.startDate asc, term.clubOperatingTermId asc
            """)
    List<ClubOperatingTerm> findPlannedFrom(Long clubId, LocalDate fromDate);

    boolean existsByClubIdAndTermNameAndStartDate(
            Long clubId,
            String termName,
            LocalDate startDate
    );

    boolean existsByClubIdAndTermNameAndStartDateAndClubOperatingTermIdNot(
            Long clubId,
            String termName,
            LocalDate startDate,
            Long clubOperatingTermId
    );
}
