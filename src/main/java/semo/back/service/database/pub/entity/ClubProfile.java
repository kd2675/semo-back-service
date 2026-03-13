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
@Table(name = "club_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubProfile extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_profile_id")
    private Long clubProfileId;

    @Column(name = "club_member_id", nullable = false, unique = true)
    private Long clubMemberId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "tagline", length = 255)
    private String tagline;

    @Column(name = "intro_text", length = 1000)
    private String introText;

    @Column(name = "avatar_file_name", length = 255)
    private String avatarFileName;
}
