package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ResourceAttachment;

public interface ResourceAttachmentRepository extends JpaRepository<ResourceAttachment, Long> {
    List<ResourceAttachment> findByClubIdAndResourceTypeAndResourceIdAndDeletedFalseOrderByResourceAttachmentIdAsc(
            Long clubId,
            String resourceType,
            Long resourceId
    );

    long countByClubIdAndResourceTypeAndResourceIdAndDeletedFalse(
            Long clubId,
            String resourceType,
            Long resourceId
    );

    Optional<ResourceAttachment> findByResourceAttachmentIdAndClubIdAndDeletedFalse(
            Long resourceAttachmentId,
            Long clubId
    );

    Optional<ResourceAttachment> findByFileNameAndDeletedFalse(String fileName);
}
