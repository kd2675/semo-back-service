package semo.back.service.feature.attendance.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubAttendanceCheckIn;
import semo.back.service.database.pub.entity.ClubAttendanceSession;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.feature.attendance.vo.AdminAttendanceMemberResponse;
import semo.back.service.feature.attendance.vo.AttendanceCheckInRequest;
import semo.back.service.feature.attendance.vo.AttendanceHistoryItemResponse;
import semo.back.service.feature.attendance.vo.AttendanceSessionResponse;
import semo.back.service.feature.attendance.vo.ClubAdminAttendanceResponse;
import semo.back.service.feature.attendance.vo.ClubAttendanceResponse;
import semo.back.service.feature.attendance.vo.CreateAttendanceSessionRequest;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAttendanceService {
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final String SESSION_OPEN = "OPEN";
    private static final String SESSION_CLOSED = "CLOSED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter DATETIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAttendanceSessionRepository clubAttendanceSessionRepository;
    private final ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;

    public ClubAttendanceResponse getAttendance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        boolean featureEnabled = requireAttendanceFeature(clubId);
        List<ClubAttendanceSession> recentSessions = clubAttendanceSessionRepository.findRecentSessions(clubId, PageRequest.of(0, 5));
        Map<Long, ClubAttendanceCheckIn> myCheckIns = clubAttendanceCheckInRepository.findByAttendanceSessionIdIn(
                        recentSessions.stream().map(ClubAttendanceSession::getAttendanceSessionId).toList()
                ).stream()
                .filter(checkIn -> checkIn.getClubProfileId().equals(access.clubProfile().getClubProfileId()))
                .collect(Collectors.toMap(ClubAttendanceCheckIn::getAttendanceSessionId, Function.identity()));

        ClubAttendanceSession currentSession = recentSessions.stream()
                .filter(session -> session.getAttendanceDate().equals(LocalDate.now()))
                .findFirst()
                .orElseGet(() -> recentSessions.stream().findFirst().orElse(null));

        AttendanceSessionResponse currentSessionResponse = currentSession == null
                ? null
                : toSessionResponse(
                        currentSession,
                        myCheckIns.get(currentSession.getAttendanceSessionId()),
                        countCheckIns(currentSession.getAttendanceSessionId()),
                        clubAccessResolver.getActiveMemberSnapshots(clubId).size()
                );

        return new ClubAttendanceResponse(
                access.club().getClubId(),
                access.club().getName(),
                featureEnabled,
                currentSessionResponse,
                recentSessions.stream()
                        .map(session -> toHistoryItem(session, myCheckIns.get(session.getAttendanceSessionId())))
                        .toList()
        );
    }

    public ClubAdminAttendanceResponse getAdminAttendance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        boolean featureEnabled = requireAttendanceFeature(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> memberSnapshots = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubAttendanceSession> recentSessions = clubAttendanceSessionRepository.findRecentSessions(clubId, PageRequest.of(0, 5));
        ClubAttendanceSession currentSession = recentSessions.stream()
                .filter(session -> session.getAttendanceDate().equals(LocalDate.now()))
                .findFirst()
                .orElseGet(() -> recentSessions.stream().findFirst().orElse(null));

        Map<Long, ClubAttendanceCheckIn> currentCheckInsByProfileId = currentSession == null
                ? Map.of()
                : clubAttendanceCheckInRepository.findByAttendanceSessionId(currentSession.getAttendanceSessionId()).stream()
                .collect(Collectors.toMap(ClubAttendanceCheckIn::getClubProfileId, Function.identity()));

        AttendanceSessionResponse currentSessionResponse = currentSession == null
                ? null
                : toSessionResponse(
                        currentSession,
                        currentCheckInsByProfileId.get(access.clubProfile().getClubProfileId()),
                        currentCheckInsByProfileId.size(),
                        memberSnapshots.size()
                );

        return new ClubAdminAttendanceResponse(
                access.club().getClubId(),
                access.club().getName(),
                featureEnabled,
                currentSessionResponse,
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
                        .map(session -> toHistoryItem(session, null))
                        .toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public AttendanceSessionResponse createAttendanceSession(Long clubId, String userKey, CreateAttendanceSessionRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireAttendanceFeature(clubId);
        LocalDate attendanceDate = request != null && request.attendanceDate() != null
                ? request.attendanceDate()
                : LocalDate.now();
        String title = StringUtils.hasText(request == null ? null : request.title())
                ? request.title().trim()
                : attendanceDate.format(DateTimeFormatter.ofPattern("M월 d일 출석체크", Locale.KOREAN));

        ClubAttendanceSession session = clubAttendanceSessionRepository.findByClubIdAndAttendanceDate(clubId, attendanceDate)
                .orElseGet(() -> clubAttendanceSessionRepository.save(ClubAttendanceSession.builder()
                        .clubId(clubId)
                        .createdByClubProfileId(access.clubProfile().getClubProfileId())
                        .title(title)
                        .attendanceDate(attendanceDate)
                        .openAt(LocalDateTime.now())
                        .closeAt(null)
                        .sessionStatus(SESSION_OPEN)
                        .build()));

        int memberCount = clubAccessResolver.getActiveMemberSnapshots(clubId).size();
        return toSessionResponse(
                session,
                clubAttendanceCheckInRepository.findByAttendanceSessionIdAndClubProfileId(session.getAttendanceSessionId(), access.clubProfile().getClubProfileId()).orElse(null),
                countCheckIns(session.getAttendanceSessionId()),
                memberCount
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public AttendanceSessionResponse closeAttendanceSession(Long clubId, Long sessionId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireAttendanceFeature(clubId);
        ClubAttendanceSession session = clubAttendanceSessionRepository.findById(sessionId)
                .filter(item -> item.getClubId().equals(clubId))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubAttendanceSession", "sessionId", sessionId));

        ClubAttendanceSession closedSession = clubAttendanceSessionRepository.save(ClubAttendanceSession.builder()
                .attendanceSessionId(session.getAttendanceSessionId())
                .clubId(session.getClubId())
                .createdByClubProfileId(session.getCreatedByClubProfileId())
                .title(session.getTitle())
                .attendanceDate(session.getAttendanceDate())
                .openAt(session.getOpenAt())
                .closeAt(LocalDateTime.now())
                .sessionStatus(SESSION_CLOSED)
                .build());

        return toSessionResponse(
                closedSession,
                clubAttendanceCheckInRepository.findByAttendanceSessionIdAndClubProfileId(closedSession.getAttendanceSessionId(), access.clubProfile().getClubProfileId()).orElse(null),
                countCheckIns(closedSession.getAttendanceSessionId()),
                clubAccessResolver.getActiveMemberSnapshots(clubId).size()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public AttendanceSessionResponse checkIn(Long clubId, String userKey, AttendanceCheckInRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireAttendanceFeature(clubId);
        if (request == null || request.sessionId() == null) {
            throw new SemoException.ValidationException("출석 세션 ID는 필수입니다.");
        }

        ClubAttendanceSession session = clubAttendanceSessionRepository.findById(request.sessionId())
                .filter(item -> item.getClubId().equals(clubId))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubAttendanceSession", "sessionId", request.sessionId()));

        if (!SESSION_OPEN.equals(session.getSessionStatus())) {
            throw new SemoException.ValidationException("현재 출석체크가 닫혀 있습니다.");
        }

        ClubAttendanceCheckIn existing = clubAttendanceCheckInRepository
                .findByAttendanceSessionIdAndClubProfileId(session.getAttendanceSessionId(), access.clubProfile().getClubProfileId())
                .orElse(null);

        ClubAttendanceCheckIn checkIn = clubAttendanceCheckInRepository.save(ClubAttendanceCheckIn.builder()
                .clubAttendanceCheckInId(existing == null ? null : existing.getClubAttendanceCheckInId())
                .attendanceSessionId(session.getAttendanceSessionId())
                .clubProfileId(access.clubProfile().getClubProfileId())
                .statusCode(CHECKED_IN)
                .checkedInAt(LocalDateTime.now())
                .note(null)
                .build());

        return toSessionResponse(
                session,
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

    private AttendanceSessionResponse toSessionResponse(
            ClubAttendanceSession session,
            ClubAttendanceCheckIn checkIn,
            int checkedInCount,
            int memberCount
    ) {
        return new AttendanceSessionResponse(
                session.getAttendanceSessionId(),
                session.getTitle(),
                session.getAttendanceDate().format(DATE_LABEL_FORMATTER),
                session.getSessionStatus(),
                formatDateTime(session.getOpenAt()),
                formatDateTime(session.getCloseAt()),
                checkIn != null,
                formatDateTime(checkIn == null ? null : checkIn.getCheckedInAt()),
                SESSION_OPEN.equals(session.getSessionStatus()) && checkIn == null,
                checkedInCount,
                memberCount
        );
    }

    private AttendanceHistoryItemResponse toHistoryItem(ClubAttendanceSession session, ClubAttendanceCheckIn checkIn) {
        return new AttendanceHistoryItemResponse(
                session.getAttendanceSessionId(),
                session.getTitle(),
                session.getAttendanceDate().format(DATE_LABEL_FORMATTER),
                session.getSessionStatus(),
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
