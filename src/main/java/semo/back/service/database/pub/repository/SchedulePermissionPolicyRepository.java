package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.SchedulePermissionPolicy;

import java.util.Optional;

public interface SchedulePermissionPolicyRepository extends JpaRepository<SchedulePermissionPolicy, Long> {
    Optional<SchedulePermissionPolicy> findByClubId(Long clubId);
}
