package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubFeedback;

import java.util.List;
import java.util.Optional;

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
}
