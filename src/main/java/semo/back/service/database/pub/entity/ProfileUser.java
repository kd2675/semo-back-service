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
import lombok.Setter;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(name = "profile_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProfileUser extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "user_key", nullable = false, unique = true, length = 64)
    private String userKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "tagline", length = 255)
    private String tagline;

    @Column(name = "profile_color", length = 20)
    private String profileColor;
}
