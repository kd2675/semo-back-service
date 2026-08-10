package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.Club;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findByClubIdInAndActiveTrue(Collection<Long> clubIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select club from Club club where club.clubId = :clubId")
    Optional<Club> findForUpdate(Long clubId);

    @Query("""
            select c
            from Club c
            where c.active = true
              and c.visibilityStatus = 'PUBLIC'
              and (
                    :query = ''
                    or lower(c.name) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.summary, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.activityCategory, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.affiliationType, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.regionLabel, '')) like lower(concat('%', :query, '%'))
                    or exists (
                        select 1
                        from ClubActivityTag tag
                        where tag.clubId = c.clubId
                          and lower(tag.tagKey) like lower(concat('%', :query, '%'))
                    )
                  )
            order by c.createDate desc, c.clubId desc
            """)
    List<Club> searchPublicActiveClubs(String query);
}
