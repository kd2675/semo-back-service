package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubGrowthCore;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubGrowthCoreRepository extends JpaRepository<ClubGrowthCore, Long> {
    List<ClubGrowthCore> findByClubIdIn(Collection<Long> clubIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select core from ClubGrowthCore core where core.clubId = :clubId")
    Optional<ClubGrowthCore> findForUpdate(Long clubId);

    @Query("""
            select core.clubId
            from ClubGrowthCore core
            where core.lastProjectedAt is null
               or core.lastProjectedAt < :cutoff
            order by
              case when core.lastProjectedAt is null then 0 else 1 end,
              core.lastProjectedAt,
              core.clubId
            """)
    List<Long> findStaleClubIds(LocalDateTime cutoff, Pageable pageable);
}
