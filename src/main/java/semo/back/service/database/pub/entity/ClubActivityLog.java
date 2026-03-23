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
@Table(name = "club_activity_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubActivityLog extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_activity_log_id")
    private Long clubActivityLogId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "actor_club_member_id")
    private Long actorClubMemberId;

    @Column(name = "actor_club_profile_id")
    private Long actorClubProfileId;

    @Column(name = "actor_display_name", nullable = false, length = 100)
    private String actorDisplayName;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "detail_text", nullable = false, length = 500)
    private String detailText;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
