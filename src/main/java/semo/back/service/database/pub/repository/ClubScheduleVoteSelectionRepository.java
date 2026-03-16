package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubScheduleVoteSelectionRepository extends JpaRepository<ClubScheduleVoteSelection, Long> {
    List<ClubScheduleVoteSelection> findByVoteIdIn(Collection<Long> voteIds);

    Optional<ClubScheduleVoteSelection> findByVoteIdAndClubProfileId(Long voteId, Long clubProfileId);

    void deleteByVoteId(Long voteId);
}
