package semo.back.service.feature.schedule.biz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.schedule.biz.policy.ClubSchedulePermissionService;
import semo.back.service.feature.schedule.biz.support.ClubScheduleViewSupport;
import semo.back.service.feature.schedule.vo.ClubScheduleAttendanceSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleAttendanceEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventAttendanceMemberResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventAttendanceResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventAttendanceSummaryResponse;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventAttendanceRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleAttendanceService {
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final String FEATURE_SCHEDULE = "SCHEDULE_MANAGE";
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String ATTENDANCE_PRESENT = "PRESENT";
    private static final String ATTENDANCE_LATE = "LATE";
    private static final String ATTENDANCE_ABSENT = "ABSENT";
    private static final String ATTENDANCE_EXCUSED = "EXCUSED";
    private static final String ATTENDANCE_UNMARKED = "UNMARKED";
    private static final Set<String> SUPPORTED_ATTENDANCE_STATUSES = Set.of(
            ATTENDANCE_PRESENT,
            ATTENDANCE_LATE,
            ATTENDANCE_ABSENT,
            ATTENDANCE_EXCUSED,
            ATTENDANCE_UNMARKED
    );
    private static final DateTimeFormatter CHECKED_IN_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubSchedulePermissionService clubSchedulePermissionService;
    private final ClubScheduleViewSupport clubScheduleViewSupport;

    public ClubScheduleAttendanceSummaryResponse getAttendanceSummary(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireAttendanceFeatures(clubId);
        List<ClubScheduleEvent> attendanceEvents = clubScheduleEventRepository.findAllActiveEvents(clubId).stream()
                .filter(ClubScheduleEvent::isParticipationEnabled)
                .toList();
        Map<Long, List<ClubEventParticipant>> participantsByEventId = loadParticipantsByEventId(attendanceEvents);
        LocalDate today = LocalDate.now();

        ScheduleAttendanceEventSummaryResponse nextEvent = attendanceEvents.stream()
                .filter(event -> !resolveEndDate(event).isBefore(today))
                .min(Comparator.comparing(ClubScheduleEvent::getStartAt)
                        .thenComparing(ClubScheduleEvent::getEventId))
                .map(event -> toEventSummary(
                        event,
                        participantsByEventId.getOrDefault(event.getEventId(), List.of()),
                        access.clubProfile().getClubProfileId()
                ))
                .orElse(null);

        List<ScheduleAttendanceEventSummaryResponse> recentEvents = attendanceEvents.stream()
                .filter(event -> resolveEndDate(event).isBefore(today))
                .sorted(Comparator.comparing(ClubScheduleEvent::getStartAt)
                        .thenComparing(ClubScheduleEvent::getEventId)
                        .reversed())
                .limit(3)
                .map(event -> toEventSummary(
                        event,
                        participantsByEventId.getOrDefault(event.getEventId(), List.of()),
                        access.clubProfile().getClubProfileId()
                ))
                .toList();

        return new ClubScheduleAttendanceSummaryResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                nextEvent,
                recentEvents
        );
    }

    public ScheduleEventAttendanceResponse getEventAttendance(Long clubId, Long eventId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireAttendanceFeatures(clubId);
        requireAttendanceManagementPermission(access);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        return buildEventAttendanceResponse(access, event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정출석")
    public ScheduleEventAttendanceResponse updateEventAttendance(
            Long clubId,
            Long eventId,
            Long targetClubProfileId,
            String userKey,
            UpdateScheduleEventAttendanceRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireAttendanceFeatures(clubId);
        requireAttendanceManagementPermission(access);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        String attendanceStatus = normalizeAttendanceStatus(request);
        String note = normalizeNote(request == null ? null : request.note());
        ClubAccessResolver.ClubMemberSnapshot target = clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .filter(snapshot -> snapshot.clubProfile().getClubProfileId().equals(targetClubProfileId))
                .findFirst()
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubProfile",
                        "clubProfileId",
                        targetClubProfileId
                ));
        ClubEventParticipant current = clubEventParticipantRepository
                .findForUpdateByEventIdAndClubProfileId(eventId, targetClubProfileId)
                .orElse(null);

        if (ATTENDANCE_UNMARKED.equals(attendanceStatus)) {
            if (current == null) {
                throw new SemoException.ValidationException("해제할 일정 출석 기록이 없습니다.");
            }
            ClubActivityContextHolder.setDetails(
                    "일정 '" + event.getTitle() + "'의 " + target.clubProfile().getDisplayName() + " 출석 표시를 해제했습니다.",
                    "일정 출석 표시 해제에 실패했습니다."
            );
            saveAttendance(current, current.getParticipationStatus(), null, null, null, null);
            return buildEventAttendanceResponse(access, event);
        }

        boolean arrived = ATTENDANCE_PRESENT.equals(attendanceStatus) || ATTENDANCE_LATE.equals(attendanceStatus);
        if (current == null && !arrived) {
            throw new SemoException.ValidationException("참석 예정인 멤버만 결석 또는 사유 인정으로 처리할 수 있습니다.");
        }
        if (current != null && !PARTICIPATION_GOING.equals(current.getParticipationStatus()) && !arrived) {
            throw new SemoException.ValidationException("참석 예정인 멤버만 결석 또는 사유 인정으로 처리할 수 있습니다.");
        }

        LocalDateTime checkedInAt = resolveCheckedInAt(arrived, current);
        ClubActivityContextHolder.setDetails(
                "일정 '" + event.getTitle() + "'의 " + target.clubProfile().getDisplayName()
                        + " 출석 상태를 " + attendanceStatusLabel(attendanceStatus) + "으로 변경했습니다.",
                "일정 출석 상태 변경에 실패했습니다."
        );
        saveAttendance(
                current,
                PARTICIPATION_GOING,
                attendanceStatus,
                checkedInAt,
                access.clubProfile().getClubProfileId(),
                note,
                eventId,
                targetClubProfileId
        );
        return buildEventAttendanceResponse(access, event);
    }

    private void saveAttendance(
            ClubEventParticipant current,
            String participationStatus,
            String attendanceStatus,
            LocalDateTime checkedInAt,
            Long verifiedByClubProfileId,
            String note
    ) {
        saveAttendance(
                current,
                participationStatus,
                attendanceStatus,
                checkedInAt,
                verifiedByClubProfileId,
                note,
                current.getEventId(),
                current.getClubProfileId()
        );
    }

    private void saveAttendance(
            ClubEventParticipant current,
            String participationStatus,
            String attendanceStatus,
            LocalDateTime checkedInAt,
            Long verifiedByClubProfileId,
            String note,
            Long eventId,
            Long clubProfileId
    ) {
        clubEventParticipantRepository.save(ClubEventParticipant.builder()
                .clubEventParticipantId(current == null ? null : current.getClubEventParticipantId())
                .eventId(eventId)
                .clubProfileId(clubProfileId)
                .participationStatus(participationStatus)
                .checkedInAt(checkedInAt)
                .attendanceStatus(attendanceStatus)
                .verifiedByClubProfileId(verifiedByClubProfileId)
                .attendanceNote(note)
                .build());
    }

    private ScheduleEventAttendanceResponse buildEventAttendanceResponse(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleEvent event
    ) {
        Map<Long, ClubEventParticipant> participantByProfileId = clubEventParticipantRepository
                .findByEventIdIn(List.of(event.getEventId())).stream()
                .collect(Collectors.toMap(ClubEventParticipant::getClubProfileId, Function.identity()));
        List<ScheduleEventAttendanceMemberResponse> members = clubAccessResolver.getActiveMemberSnapshots(
                        access.club().getClubId()
                ).stream()
                .map(snapshot -> toAttendanceMemberResponse(
                        snapshot,
                        participantByProfileId.get(snapshot.clubProfile().getClubProfileId())
                ))
                .sorted(Comparator
                        .comparingInt((ScheduleEventAttendanceMemberResponse member) -> participationSortOrder(
                                member.participationStatus()
                        ))
                        .thenComparing(ScheduleEventAttendanceMemberResponse::displayName))
                .toList();
        ScheduleEventAttendanceSummaryResponse summary = toAttendanceSummary(
                participantByProfileId.values().stream().toList()
        );

        return new ScheduleEventAttendanceResponse(
                access.club().getClubId(),
                access.club().getName(),
                event.getEventId(),
                event.getTitle(),
                clubScheduleViewSupport.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleViewSupport.formatTimeLabel(event.getStartAt(), event.getEndAt()),
                true,
                summary,
                members
        );
    }

    private ScheduleEventAttendanceMemberResponse toAttendanceMemberResponse(
            ClubAccessResolver.ClubMemberSnapshot snapshot,
            ClubEventParticipant participant
    ) {
        return new ScheduleEventAttendanceMemberResponse(
                snapshot.clubProfile().getClubProfileId(),
                snapshot.clubProfile().getDisplayName(),
                snapshot.membership().getRoleCode(),
                participant == null ? null : participant.getParticipationStatus(),
                participant == null ? null : participant.getAttendanceStatus(),
                formatCheckedInAt(participant == null ? null : participant.getCheckedInAt()),
                participant == null ? null : participant.getAttendanceNote()
        );
    }

    private ScheduleAttendanceEventSummaryResponse toEventSummary(
            ClubScheduleEvent event,
            List<ClubEventParticipant> participants,
            Long viewerClubProfileId
    ) {
        ClubEventParticipant mine = participants.stream()
                .filter(participant -> participant.getClubProfileId().equals(viewerClubProfileId))
                .findFirst()
                .orElse(null);
        int goingCount = (int) participants.stream()
                .filter(participant -> PARTICIPATION_GOING.equals(participant.getParticipationStatus()))
                .count();
        int attendedCount = (int) participants.stream()
                .filter(participant -> ATTENDANCE_PRESENT.equals(participant.getAttendanceStatus())
                        || ATTENDANCE_LATE.equals(participant.getAttendanceStatus()))
                .count();

        return new ScheduleAttendanceEventSummaryResponse(
                event.getEventId(),
                event.getTitle(),
                clubScheduleViewSupport.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleViewSupport.formatTimeLabel(event.getStartAt(), event.getEndAt()),
                mine == null ? null : mine.getParticipationStatus(),
                mine == null ? null : mine.getAttendanceStatus(),
                formatCheckedInAt(mine == null ? null : mine.getCheckedInAt()),
                goingCount,
                attendedCount
        );
    }

    private ScheduleEventAttendanceSummaryResponse toAttendanceSummary(List<ClubEventParticipant> participants) {
        int goingCount = 0;
        int presentCount = 0;
        int lateCount = 0;
        int absentCount = 0;
        int excusedCount = 0;
        int unmarkedCount = 0;
        for (ClubEventParticipant participant : participants) {
            if (PARTICIPATION_GOING.equals(participant.getParticipationStatus())) {
                goingCount++;
                if (participant.getAttendanceStatus() == null) {
                    unmarkedCount++;
                }
            }
            if (participant.getAttendanceStatus() == null) {
                continue;
            }
            switch (participant.getAttendanceStatus()) {
                case ATTENDANCE_PRESENT -> presentCount++;
                case ATTENDANCE_LATE -> lateCount++;
                case ATTENDANCE_ABSENT -> absentCount++;
                case ATTENDANCE_EXCUSED -> excusedCount++;
                default -> {
                }
            }
        }
        return new ScheduleEventAttendanceSummaryResponse(
                goingCount,
                presentCount,
                lateCount,
                absentCount,
                excusedCount,
                unmarkedCount
        );
    }

    private Map<Long, List<ClubEventParticipant>> loadParticipantsByEventId(List<ClubScheduleEvent> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        return clubEventParticipantRepository.findByEventIdIn(
                        events.stream().map(ClubScheduleEvent::getEventId).toList()
                ).stream()
                .collect(Collectors.groupingBy(ClubEventParticipant::getEventId));
    }

    private ClubScheduleEvent getEvent(Long clubId, Long eventId) {
        return clubScheduleEventRepository.findByEventIdAndClubId(eventId, clubId)
                .filter(ClubScheduleEvent::isParticipationEnabled)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubScheduleEvent",
                        "eventId",
                        eventId
                ));
    }

    private String normalizeAttendanceStatus(UpdateScheduleEventAttendanceRequest request) {
        if (request == null || !StringUtils.hasText(request.attendanceStatus())) {
            throw new SemoException.ValidationException("실제 출석 상태는 필수입니다.");
        }
        String normalized = request.attendanceStatus().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ATTENDANCE_STATUSES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 실제 출석 상태입니다.");
        }
        return normalized;
    }

    private String normalizeNote(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        String normalized = note.trim();
        if (normalized.length() > 500) {
            throw new SemoException.ValidationException("출석 메모는 500자 이하여야 합니다.");
        }
        return normalized;
    }

    private void requireAttendanceFeatures(Long clubId) {
        clubFeatureService.requireFeatureEnabled(clubId, FEATURE_SCHEDULE, "일정");
        clubFeatureService.requireFeatureEnabled(clubId, FEATURE_ATTENDANCE, "일정 출석");
    }

    private void requireAttendanceManagementPermission(ClubAccessResolver.ClubAccess access) {
        if (!clubSchedulePermissionService.canManageAttendance(access)) {
            throw new SemoException.ForbiddenException("일정 출석 관리 권한이 필요합니다.");
        }
    }

    private LocalDate resolveEndDate(ClubScheduleEvent event) {
        return event.getEndAt() == null
                ? event.getStartAt().toLocalDate()
                : event.getEndAt().toLocalDate();
    }

    private int participationSortOrder(String participationStatus) {
        if (PARTICIPATION_GOING.equals(participationStatus)) {
            return 0;
        }
        if ("NOT_GOING".equals(participationStatus)) {
            return 1;
        }
        if ("CANCELED".equals(participationStatus)) {
            return 2;
        }
        return 3;
    }

    private String formatCheckedInAt(LocalDateTime checkedInAt) {
        return checkedInAt == null ? null : checkedInAt.format(CHECKED_IN_AT_FORMATTER);
    }

    private String attendanceStatusLabel(String attendanceStatus) {
        return switch (attendanceStatus) {
            case ATTENDANCE_PRESENT -> "출석";
            case ATTENDANCE_LATE -> "지각";
            case ATTENDANCE_ABSENT -> "결석";
            case ATTENDANCE_EXCUSED -> "사유 인정";
            default -> attendanceStatus;
        };
    }

    private LocalDateTime resolveCheckedInAt(boolean arrived, ClubEventParticipant current) {
        if (!arrived) {
            return null;
        }
        if (current != null && current.getCheckedInAt() != null) {
            return current.getCheckedInAt();
        }
        return LocalDateTime.now();
    }
}
