package semo.back.service.feature.attendance.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubAttendanceCheckIn;
import semo.back.service.database.pub.entity.ClubAttendanceSession;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.attendance.vo.AdminAttendanceMemberResponse;
import semo.back.service.feature.attendance.vo.AttendanceDailyLogResponse;
import semo.back.service.feature.attendance.vo.AttendanceTodayResponse;
import semo.back.service.feature.attendance.vo.ClubAdminAttendanceResponse;
import semo.back.service.feature.attendance.vo.ClubAttendanceResponse;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAttendanceService {
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter DATETIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAttendanceSessionRepository clubAttendanceSessionRepository;
    private final ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubAttendanceSessionWriter clubAttendanceSessionWriter;

    public ClubAttendanceResponse getAttendance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        boolean featureEnabled = requireAttendanceFeature(clubId);
        Optional<ClubAttendanceSession> todaySession = clubAttendanceSessionRepository.findByClubIdAndAttendanceDate(clubId, LocalDate.now());
        List<ClubAttendanceSession> recentSessions = clubAttendanceSessionRepository.findRecentSessions(clubId, PageRequest.of(0, 5));
        Map<Long, ClubAttendanceCheckIn> myCheckIns = clubAttendanceCheckInRepository.findByAttendanceSessionIdIn(
                        recentSessions.stream().map(ClubAttendanceSession::getAttendanceSessionId).toList()
                ).stream()
                .filter(checkIn -> checkIn.getClubProfileId().equals(access.clubProfile().getClubProfileId()))
                .collect(Collectors.toMap(ClubAttendanceCheckIn::getAttendanceSessionId, Function.identity()));
        ClubAttendanceCheckIn todayCheckIn = todaySession
                .map(session -> myCheckIns.get(session.getAttendanceSessionId()))
                .orElse(null);
        int memberCount = clubAccessResolver.getActiveMemberSnapshots(clubId).size();

        return new ClubAttendanceResponse(
                access.club().getClubId(),
                access.club().getName(),
                featureEnabled,
                toTodayAttendanceResponse(
                        todayCheckIn,
                        todaySession.map(session -> countCheckIns(session.getAttendanceSessionId())).orElse(0),
                        memberCount
                ),
                recentSessions.stream()
                        .map(session -> toDailyLogResponse(
                                session,
                                myCheckIns.get(session.getAttendanceSessionId()),
                                countCheckIns(session.getAttendanceSessionId()),
                                memberCount
                        ))
                        .toList()
        );
    }

    public ClubAdminAttendanceResponse getAdminAttendance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        boolean featureEnabled = requireAttendanceFeature(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> memberSnapshots = clubAccessResolver.getActiveMemberSnapshots(clubId);
        Optional<ClubAttendanceSession> todaySession = clubAttendanceSessionRepository.findByClubIdAndAttendanceDate(clubId, LocalDate.now());
        List<ClubAttendanceSession> recentSessions = clubAttendanceSessionRepository.findRecentSessions(clubId, PageRequest.of(0, 5));
        Map<Long, ClubAttendanceCheckIn> currentCheckInsByProfileId = todaySession
                .map(session -> clubAttendanceCheckInRepository.findByAttendanceSessionId(session.getAttendanceSessionId()).stream()
                        .collect(Collectors.toMap(ClubAttendanceCheckIn::getClubProfileId, Function.identity())))
                .orElseGet(Map::of);

        return new ClubAdminAttendanceResponse(
                access.club().getClubId(),
                access.club().getName(),
                featureEnabled,
                toTodayAttendanceResponse(
                        currentCheckInsByProfileId.get(access.clubProfile().getClubProfileId()),
                        currentCheckInsByProfileId.size(),
                        memberSnapshots.size()
                ),
                memberSnapshots.stream()
                        .map(snapshot -> {
                            ClubAttendanceCheckIn checkIn = currentCheckInsByProfileId.get(snapshot.clubProfile().getClubProfileId());
                            return new AdminAttendanceMemberResponse(
                                    snapshot.clubProfile().getClubProfileId(),
                                    snapshot.clubProfile().getDisplayName(),
                                    snapshot.membership().getRoleCode(),
                                    checkIn != null,
                                    formatDateTime(checkIn == null ? null : checkIn.getCheckedInAt())
                            );
                        })
                        .sorted(Comparator.comparing(AdminAttendanceMemberResponse::displayName))
                        .toList(),
                recentSessions.stream()
                        .map(session -> toDailyLogResponse(
                                session,
                                null,
                                countCheckIns(session.getAttendanceSessionId()),
                                memberSnapshots.size()
                        ))
                        .toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "출석관리")
    public AttendanceTodayResponse checkIn(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireAttendanceFeature(clubId);
        ClubAttendanceSession session = clubAttendanceSessionWriter.ensureTodaySession(clubId, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                session.getAttendanceDate().format(DATE_LABEL_FORMATTER) + " 출석을 완료했습니다.",
                "출석 체크에 실패했습니다."
        );

        ClubAttendanceCheckIn existing = clubAttendanceCheckInRepository
                .findByAttendanceSessionIdAndClubProfileId(session.getAttendanceSessionId(), access.clubProfile().getClubProfileId())
                .orElse(null);

        if (existing != null) {
            return toTodayAttendanceResponse(
                    existing,
                    countCheckIns(session.getAttendanceSessionId()),
                    clubAccessResolver.getActiveMemberSnapshots(clubId).size()
            );
        }

        ClubAttendanceCheckIn checkIn = clubAttendanceCheckInRepository.save(ClubAttendanceCheckIn.builder()
                .attendanceSessionId(session.getAttendanceSessionId())
                .clubProfileId(access.clubProfile().getClubProfileId())
                .statusCode(CHECKED_IN)
                .checkedInAt(LocalDateTime.now())
                .note(null)
                .build());

        return toTodayAttendanceResponse(
                checkIn,
                countCheckIns(session.getAttendanceSessionId()),
                clubAccessResolver.getActiveMemberSnapshots(clubId).size()
        );
    }

    private boolean requireAttendanceFeature(Long clubId) {
        boolean enabled = clubFeatureService.isFeatureEnabled(clubId, FEATURE_ATTENDANCE);
        if (!enabled) {
            throw new SemoException.ForbiddenException("Attendance feature is not enabled");
        }
        return true;
    }

    private int countCheckIns(Long sessionId) {
        return clubAttendanceCheckInRepository.findByAttendanceSessionId(sessionId).size();
    }

    private AttendanceTodayResponse toTodayAttendanceResponse(
            ClubAttendanceCheckIn checkIn,
            int checkedInCount,
            int memberCount
    ) {
        return new AttendanceTodayResponse(
                LocalDate.now().format(DATE_LABEL_FORMATTER),
                checkIn != null,
                formatDateTime(checkIn == null ? null : checkIn.getCheckedInAt()),
                checkIn == null,
                checkedInCount,
                memberCount
        );
    }

    private AttendanceDailyLogResponse toDailyLogResponse(
            ClubAttendanceSession session,
            ClubAttendanceCheckIn checkIn,
            int checkedInCount,
            int memberCount
    ) {
        return new AttendanceDailyLogResponse(
                session.getAttendanceDate().format(DATE_LABEL_FORMATTER),
                checkedInCount,
                memberCount,
                checkIn != null,
                formatDateTime(checkIn == null ? null : checkIn.getCheckedInAt())
        );
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATETIME_LABEL_FORMATTER);
    }
}
