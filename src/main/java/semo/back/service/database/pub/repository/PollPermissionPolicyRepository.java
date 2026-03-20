package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.PollPermissionPolicy;

import java.util.Optional;

public interface PollPermissionPolicyRepository extends JpaRepository<PollPermissionPolicy, Long> {
    Optional<PollPermissionPolicy> findByClubId(Long clubId);
}
