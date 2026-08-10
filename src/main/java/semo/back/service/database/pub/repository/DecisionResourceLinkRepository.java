package semo.back.service.database.pub.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.DecisionResourceLink;

public interface DecisionResourceLinkRepository extends JpaRepository<DecisionResourceLink, Long> {
    List<DecisionResourceLink> findByDecisionRecordIdInOrderByDecisionResourceLinkIdAsc(
            Collection<Long> decisionRecordIds
    );

    void deleteByDecisionRecordId(Long decisionRecordId);
}
