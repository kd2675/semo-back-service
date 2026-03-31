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
@Table(name = "tournament_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentEntry extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_entry_id")
    private Long tournamentEntryId;

    @Column(name = "tournament_record_id", nullable = false)
    private Long tournamentRecordId;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "source_application_id")
    private Long sourceApplicationId;

    @Column(name = "entry_status", nullable = false, length = 20)
    private String entryStatus;

    @Column(name = "seed_number")
    private Integer seedNumber;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
