package semo.back.service.database.pub.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.DecisionParticipant;

public interface DecisionParticipantRepository extends JpaRepository<DecisionParticipant, Long> {
    List<DecisionParticipant> findByDecisionRecordIdInOrderByDecisionParticipantIdAsc(
            Collection<Long> decisionRecordIds
    );

    void deleteByDecisionRecordId(Long decisionRecordId);
}
