package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubMember;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    Optional<ClubMember> findByClubIdAndProfileId(Long clubId, Long profileId);

    Optional<ClubMember> findByClubMemberIdAndClubId(Long clubMemberId, Long clubId);

    @Query("""
            select cm
            from ClubMember cm
            where cm.profileId = :profileId
              and cm.membershipStatus = :membershipStatus
            order by
              case when cm.lastActivityAt is null then 1 else 0 end,
              cm.lastActivityAt desc,
              cm.clubMemberId desc
            """)
    List<ClubMember> findActiveMemberships(Long profileId, String membershipStatus);

    List<ClubMember> findByClubIdAndMembershipStatusOrderByJoinedAtAscClubMemberIdAsc(Long clubId, String membershipStatus);

    List<ClubMember> findByClubIdOrderByClubMemberIdAsc(Long clubId);
}
