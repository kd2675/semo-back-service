package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(
        name = "club_activity_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_club_activity_tag", columnNames = {"club_id", "tag_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubActivityTag extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_activity_tag_id")
    private Long clubActivityTagId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "tag_key", nullable = false, length = 30)
    private String tagKey;
}
