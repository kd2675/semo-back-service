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
@Table(name = "tournament_match_side")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentMatchSide extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_match_side_id")
    private Long tournamentMatchSideId;

    @Column(name = "tournament_match_id", nullable = false)
    private Long tournamentMatchId;

    @Column(name = "side_no", nullable = false)
    private int sideNo;

    @Column(name = "tournament_entry_id")
    private Long tournamentEntryId;

    @Column(name = "score_summary", length = 120)
    private String scoreSummary;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus;
}
