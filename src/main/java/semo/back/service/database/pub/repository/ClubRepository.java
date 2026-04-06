package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.Club;

import java.util.Collection;
import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findByClubIdInAndActiveTrue(Collection<Long> clubIds);

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
                    or lower(coalesce(c.regionLabel, '')) like lower(concat('%', :query, '%'))
                  )
            order by c.createDate desc, c.clubId desc
            """)
    List<Club> searchPublicActiveClubs(String query);
}
