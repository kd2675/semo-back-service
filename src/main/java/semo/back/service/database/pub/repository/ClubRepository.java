package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.Club;

import java.util.Collection;
import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findByClubIdInAndActiveTrue(Collection<Long> clubIds);
}
