package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;

import java.util.Collection;
import java.util.List;

public interface ClubScheduleVoteOptionRepository extends JpaRepository<ClubScheduleVoteOption, Long> {
    List<ClubScheduleVoteOption> findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(Long voteId);

    List<ClubScheduleVoteOption> findByVoteIdIn(Collection<Long> voteIds);

    void deleteByVoteId(Long voteId);
}
