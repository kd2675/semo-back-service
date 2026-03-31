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

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentMatch extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_match_id")
    private Long tournamentMatchId;

    @Column(name = "tournament_record_id", nullable = false)
    private Long tournamentRecordId;

    @Column(name = "tournament_round_id", nullable = false)
    private Long tournamentRoundId;

    @Column(name = "match_status", nullable = false, length = 20)
    private String matchStatus;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "winner_entry_id")
    private Long winnerEntryId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
