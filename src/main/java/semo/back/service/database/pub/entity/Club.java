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
@Table(name = "club")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Club extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "summary", length = 255)
    private String summary;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "category_key", length = 40)
    private String categoryKey;

    @Column(name = "visibility_status", nullable = false, length = 20)
    private String visibilityStatus;

    @Column(name = "membership_policy", nullable = false, length = 20)
    private String membershipPolicy;

    @Column(name = "image_file_name", length = 255)
    private String imageFileName;

    @Column(name = "active", nullable = false)
    private boolean active;
}
