package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventParticipationRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ClubScheduleCommandSupport {
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_NOT_GOING = "NOT_GOING";
    private static final String PARTICIPATION_CANCEL = "CANCEL";

    private final ClubScheduleViewSupport clubScheduleViewSupport;

    EventDraft toEventDraft(UpsertScheduleEventRequest request) {
        if (request == null) {
            throw new SemoException.ValidationException("일정 요청이 비어 있습니다.");
        }

        LocalDate startDate = clubScheduleViewSupport.parseDate(request.startDate());
        LocalDate endDate = clubScheduleViewSupport.parseOptionalDate(request.endDate());
        LocalTime startTime = clubScheduleViewSupport.parseOptionalTime(request.startTime());
        LocalTime endTime = clubScheduleViewSupport.parseOptionalTime(request.endTime());
        if (startTime == null && endTime != null) {
            throw new SemoException.ValidationException("종료 시간만 단독으로 입력할 수 없습니다.");
        }

        LocalDate resolvedEndDate = endDate == null ? startDate : endDate;
        LocalDateTime startAt = startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
        LocalDateTime endAt = endDate == null && endTime == null
                ? null
                : resolvedEndDate.atTime(endTime == null ? LocalTime.MIDNIGHT : endTime);
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new SemoException.ValidationException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }

        boolean participationEnabled = Boolean.TRUE.equals(request.participationEnabled());
        boolean feeRequired = Boolean.TRUE.equals(request.feeRequired());
        boolean feeAmountUndecided = feeRequired && Boolean.TRUE.equals(request.feeAmountUndecided());
        Integer feeAmount = feeRequired && !feeAmountUndecided ? request.feeAmount() : null;
        if (feeRequired && !feeAmountUndecided && feeAmount == null) {
            throw new SemoException.ValidationException("참가비를 입력하거나 금액 미정을 선택해야 합니다.");
        }

        return new EventDraft(
                trimRequired(request.title(), "일정 제목은 필수입니다."),
                startAt,
                endAt,
                participationEnabled ? request.attendeeLimit() : null,
                trimToNull(request.locationLabel()),
                participationEnabled ? trimToNull(request.participationConditionText()) : null,
                participationEnabled,
                feeRequired,
                feeAmount,
                feeAmountUndecided,
                feeRequired && participationEnabled && Boolean.TRUE.equals(request.feeNWaySplit())
        );
    }

    VoteDraft toVoteDraft(UpsertScheduleVoteRequest request) {
        if (request == null) {
            throw new SemoException.ValidationException("투표 요청이 비어 있습니다.");
        }

        List<String> optionLabels = request.optionLabels() == null
                ? List.of()
                : request.optionLabels().stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();
        if (optionLabels.size() < 2) {
            throw new SemoException.ValidationException("투표 항목은 최소 2개 이상이어야 합니다.");
        }
        if (optionLabels.size() != optionLabels.stream().distinct().count()) {
            throw new SemoException.ValidationException("투표 항목은 중복될 수 없습니다.");
        }

        LocalDate voteStartDate = clubScheduleViewSupport.parseDate(request.voteStartDate());
        LocalDate voteEndDate = clubScheduleViewSupport.parseDate(request.voteEndDate());
        LocalTime voteStartTime = clubScheduleViewSupport.parseOptionalTime(request.voteStartTime());
        LocalTime voteEndTime = clubScheduleViewSupport.parseOptionalTime(request.voteEndTime());
        if (voteStartTime == null && voteEndTime != null) {
            throw new SemoException.ValidationException("투표 종료 시간만 단독으로 입력할 수 없습니다.");
        }

        LocalDateTime voteStartAt = toVoteStartAt(voteStartDate, voteStartTime);
        LocalDateTime voteEndAt = toVoteEffectiveEndAt(voteEndDate, voteEndTime);
        if (voteEndAt.isBefore(voteStartAt)) {
            throw new SemoException.ValidationException("투표 종료일시는 시작일시보다 빠를 수 없습니다.");
        }

        return new VoteDraft(
                trimRequired(request.title(), "투표 제목은 필수입니다."),
                voteStartDate,
                voteEndDate,
                voteStartTime,
                voteEndTime,
                optionLabels
        );
    }

    String normalizeParticipationStatus(UpdateScheduleEventParticipationRequest request) {
        if (request == null || !StringUtils.hasText(request.participationStatus())) {
            throw new SemoException.ValidationException("참석 상태는 필수입니다.");
        }

        String normalized = request.participationStatus().trim().toUpperCase(Locale.ROOT);
        if (!PARTICIPATION_GOING.equals(normalized)
                && !PARTICIPATION_NOT_GOING.equals(normalized)
                && !PARTICIPATION_CANCEL.equals(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 참석 상태입니다.");
        }
        return normalized;
    }

    String toParticipationActivityLabel(String participationStatus) {
        return switch (participationStatus) {
            case PARTICIPATION_GOING -> "참석으로 응답했습니다";
            case PARTICIPATION_NOT_GOING -> "불참으로 응답했습니다";
            case PARTICIPATION_CANCEL -> "응답을 취소했습니다";
            default -> "응답했습니다";
        };
    }

    boolean shouldPostToBoard(Boolean postToBoard) {
        return postToBoard == null || postToBoard;
    }

    boolean shouldPin(Boolean pinned) {
        return Boolean.TRUE.equals(pinned);
    }

    boolean shouldPostToCalendar(Boolean postToCalendar) {
        return postToCalendar == null || postToCalendar;
    }

    boolean shouldPostVoteToCalendar(Boolean postToCalendar, Boolean postToSchedule) {
        if (postToCalendar != null) {
            return postToCalendar;
        }
        if (postToSchedule != null) {
            return postToSchedule;
        }
        return true;
    }

    LocalDateTime toVoteStartAt(LocalDate startDate, LocalTime startTime) {
        return startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
    }

    LocalDateTime toVoteEffectiveEndAt(LocalDate endDate, LocalTime endTime) {
        return endDate.atTime(endTime == null ? LocalTime.MAX : endTime);
    }

    private String trimRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new SemoException.ValidationException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    record EventDraft(
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer attendeeLimit,
            String locationLabel,
            String participationConditionText,
            boolean participationEnabled,
            boolean feeRequired,
            Integer feeAmount,
            boolean feeAmountUndecided,
            boolean feeNWaySplit
    ) {
    }

    record VoteDraft(
            String title,
            LocalDate voteStartDate,
            LocalDate voteEndDate,
            LocalTime voteStartTime,
            LocalTime voteEndTime,
            List<String> optionLabels
    ) {
    }
}
