package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.NoticePermissionPolicy;

import java.util.Optional;

public interface NoticePermissionPolicyRepository extends JpaRepository<NoticePermissionPolicy, Long> {
    Optional<NoticePermissionPolicy> findByClubId(Long clubId);
}
