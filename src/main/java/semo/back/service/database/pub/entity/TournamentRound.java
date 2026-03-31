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
@Table(name = "tournament_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentRound extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_round_id")
    private Long tournamentRoundId;

    @Column(name = "tournament_record_id", nullable = false)
    private Long tournamentRecordId;

    @Column(name = "round_key", nullable = false, length = 40)
    private String roundKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "round_type", nullable = false, length = 20)
    private String roundType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
