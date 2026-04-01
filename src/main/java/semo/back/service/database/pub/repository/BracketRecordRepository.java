package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.BracketRecord;

import java.util.List;
import java.util.Optional;

public interface BracketRecordRepository extends JpaRepository<BracketRecord, Long> {
    Optional<BracketRecord> findByBracketRecordIdAndClubIdAndDeletedFalse(Long bracketRecordId, Long clubId);

    List<BracketRecord> findByClubIdAndDeletedFalseOrderByCreateDateDescBracketRecordIdDesc(Long clubId);
}
