package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(name = "bracket_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BracketParticipant extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bracket_participant_id")
    private Long bracketParticipantId;

    @Column(name = "bracket_record_id", nullable = false)
    private Long bracketRecordId;

    @Column(name = "seed_number", nullable = false)
    private int seedNumber;

    @Column(name = "club_profile_id")
    private Long clubProfileId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "participant_role", nullable = false, length = 20)
    private String participantRole;

    @Column(name = "entry_source_type", nullable = false, length = 20)
    private String entrySourceType;

    @Column(name = "source_tournament_application_id")
    private Long sourceTournamentApplicationId;

    @Column(name = "guest_entry", nullable = false)
    private boolean guestEntry;
}
