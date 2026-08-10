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
        name = "decision_resource_link",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_decision_resource_link",
                columnNames = {"decision_record_id", "relation_type", "resource_type", "resource_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DecisionResourceLink extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_resource_link_id")
    private Long decisionResourceLinkId;

    @Column(name = "decision_record_id", nullable = false)
    private Long decisionRecordId;

    @Column(name = "relation_type", nullable = false, length = 20)
    private String relationType;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_title_snapshot", nullable = false, length = 200)
    private String resourceTitleSnapshot;

    @Column(name = "resource_path_snapshot", nullable = false, length = 500)
    private String resourcePathSnapshot;
}
