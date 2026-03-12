package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ProfileUser;

import java.util.Optional;

public interface ProfileUserRepository extends JpaRepository<ProfileUser, Long> {
    Optional<ProfileUser> findByUserKey(String userKey);
}
