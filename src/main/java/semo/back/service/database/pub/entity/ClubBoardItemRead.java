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
@Table(name = "club_board_item_read")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubBoardItemRead extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_board_item_read_id")
    private Long clubBoardItemReadId;

    @Column(name = "board_item_id", nullable = false)
    private Long boardItemId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "first_read_at", nullable = false)
    private LocalDateTime firstReadAt;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    public void markRead(LocalDateTime readAt) {
        if (this.firstReadAt == null) {
            this.firstReadAt = readAt;
        }
        this.lastReadAt = readAt;
    }
}
