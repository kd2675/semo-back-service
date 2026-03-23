package semo.back.service.feature.attendance.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubAttendanceSession;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClubAttendanceSessionWriter {
    private static final String SESSION_OPEN = "OPEN";
    private static final DateTimeFormatter TITLE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 출석체크", Locale.KOREAN);

    private final ClubAttendanceSessionRepository clubAttendanceSessionRepository;
    private final ClubAccessResolver clubAccessResolver;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAttendanceSession ensureTodaySession(Long clubId, Long fallbackCreatorClubProfileId) {
        return clubAttendanceSessionRepository.findByClubIdAndAttendanceDate(clubId, LocalDate.now())
                .orElseGet(() -> clubAttendanceSessionRepository.save(ClubAttendanceSession.builder()
                        .clubId(clubId)
                        .createdByClubProfileId(resolveAttendanceCreatorClubProfileId(clubId, fallbackCreatorClubProfileId))
                        .title(LocalDate.now().format(TITLE_FORMATTER))
                        .attendanceDate(LocalDate.now())
                        .openAt(LocalDateTime.now())
                        .closeAt(null)
                        .sessionStatus(SESSION_OPEN)
                        .build()));
    }

    private Long resolveAttendanceCreatorClubProfileId(Long clubId, Long fallbackCreatorClubProfileId) {
        return clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .filter(snapshot -> "OWNER".equals(snapshot.membership().getRoleCode()) || "ADMIN".equals(snapshot.membership().getRoleCode()))
                .sorted(Comparator
                        .comparingInt((ClubAccessResolver.ClubMemberSnapshot snapshot) -> "OWNER".equals(snapshot.membership().getRoleCode()) ? 0 : 1)
                        .thenComparing(snapshot -> snapshot.membership().getClubMemberId()))
                .map(snapshot -> snapshot.clubProfile().getClubProfileId())
                .findFirst()
                .orElseGet(() -> {
                    if (fallbackCreatorClubProfileId != null) {
                        return fallbackCreatorClubProfileId;
                    }
                    throw new SemoException.ValidationException("오늘 출석 기록 준비에 필요한 클럽 프로필을 찾을 수 없습니다.");
                });
    }
}
