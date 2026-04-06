package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubActivityTag;

import java.util.Collection;
import java.util.List;

public interface ClubActivityTagRepository extends JpaRepository<ClubActivityTag, Long> {
    List<ClubActivityTag> findByClubId(Long clubId);

    List<ClubActivityTag> findByClubIdIn(Collection<Long> clubIds);

    void deleteByClubId(Long clubId);
}
