package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubJoinRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubJoinRequestRepository extends JpaRepository<ClubJoinRequest, Long> {
    Optional<ClubJoinRequest> findByClubIdAndProfileId(Long clubId, Long profileId);

    Optional<ClubJoinRequest> findByClubJoinRequestIdAndClubId(Long clubJoinRequestId, Long clubId);

    List<ClubJoinRequest> findByClubIdAndRequestStatusOrderByCreateDateDescClubJoinRequestIdDesc(Long clubId, String requestStatus);

    List<ClubJoinRequest> findByProfileIdAndClubIdIn(Long profileId, Collection<Long> clubIds);

    long countByClubIdAndRequestStatus(Long clubId, String requestStatus);
}
