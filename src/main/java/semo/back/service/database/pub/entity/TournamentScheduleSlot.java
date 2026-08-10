package semo.back.service.database.pub.entity;

import java.time.LocalDateTime;

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
@Table(name = "tournament_schedule_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentScheduleSlot extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_schedule_slot_id")
    private Long tournamentScheduleSlotId;

    @Column(name = "tournament_record_id", nullable = false)
    private Long tournamentRecordId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "court_label", length = 100)
    private String courtLabel;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    public void update(String title, String courtLabel, LocalDateTime startAt, LocalDateTime endAt, String note) {
        this.title = title;
        this.courtLabel = courtLabel;
        this.startAt = startAt;
        this.endAt = endAt;
        this.note = note;
    }
}
