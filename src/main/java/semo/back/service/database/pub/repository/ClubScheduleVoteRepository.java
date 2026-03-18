package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubScheduleVote;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubScheduleVoteRepository extends JpaRepository<ClubScheduleVote, Long> {
    Optional<ClubScheduleVote> findByVoteIdAndClubId(Long voteId, Long clubId);

    Optional<ClubScheduleVote> findByLinkedNoticeId(Long linkedNoticeId);

    List<ClubScheduleVote> findByLinkedNoticeIdIn(Collection<Long> linkedNoticeIds);

    @Query("""
            select v
            from ClubScheduleVote v
            where v.clubId = :clubId
              and v.sharedToSchedule = true
              and v.voteStartDate <= :monthEnd
              and v.voteEndDate >= :monthStart
            order by v.voteStartDate asc, v.voteStartTime asc, v.voteId desc
            """)
    List<ClubScheduleVote> findAllByClubIdForMonth(Long clubId, LocalDate monthStart, LocalDate monthEnd);

    @Query("""
            select v
            from ClubScheduleVote v
            where v.clubId = :clubId
              and (
                    :queryText is null
                    or lower(v.title) like lower(concat('%', :queryText, '%'))
                  )
            order by v.voteStartDate desc, v.voteStartTime desc, v.voteId desc
            """)
    List<ClubScheduleVote> findAllByClubIdForPollHome(Long clubId, String queryText);
}
