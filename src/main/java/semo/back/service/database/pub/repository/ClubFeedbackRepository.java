package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubFeedback;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ClubFeedbackRepository extends JpaRepository<ClubFeedback, Long> {
    Optional<ClubFeedback> findByFeedbackIdAndClubIdAndDeletedFalse(Long feedbackId, Long clubId);

    @Query("""
            select f
            from ClubFeedback f
            where f.clubId = :clubId
              and f.deleted = false
            order by f.createDate desc, f.feedbackId desc
            """)
    List<ClubFeedback> findFeed(Long clubId);

    long countByClubIdAndSubmitterClubProfileIdAndDeletedFalseAndStatusCodeIn(
            Long clubId,
            Long submitterClubProfileId,
            Collection<String> statusCodes
    );

    long countByClubIdAndDeletedFalseAndStatusCodeIn(Long clubId, Collection<String> statusCodes);
}
