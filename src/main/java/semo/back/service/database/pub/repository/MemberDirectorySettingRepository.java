package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.MemberDirectorySetting;

import java.util.Optional;

public interface MemberDirectorySettingRepository extends JpaRepository<MemberDirectorySetting, Long> {
    Optional<MemberDirectorySetting> findByClubId(Long clubId);
}
