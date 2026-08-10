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
@Table(name = "resource_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ResourceAttachment extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_attachment_id")
    private Long resourceAttachmentId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "uploader_club_profile_id", nullable = false)
    private Long uploaderClubProfileId;

    @Column(name = "file_name", nullable = false, unique = true, length = 500)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "visibility_scope", nullable = false, length = 30)
    private String visibilityScope;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_by_club_profile_id")
    private Long deletedByClubProfileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void markDeleted(Long deletedByClubProfileId, LocalDateTime deletedAt) {
        if (!deleted) {
            this.deleted = true;
            this.deletedByClubProfileId = deletedByClubProfileId;
            this.deletedAt = deletedAt;
        }
    }
}
