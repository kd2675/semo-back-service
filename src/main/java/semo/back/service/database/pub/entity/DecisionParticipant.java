package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(
        name = "decision_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_decision_participant_profile",
                columnNames = {"decision_record_id", "club_profile_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DecisionParticipant extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_participant_id")
    private Long decisionParticipantId;

    @Column(name = "decision_record_id", nullable = false)
    private Long decisionRecordId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "participant_role", nullable = false, length = 20)
    private String participantRole;

    @Column(name = "display_name_snapshot", nullable = false, length = 100)
    private String displayNameSnapshot;
}
