package semo.back.service.feature.tournament.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.tournament.biz.policy.ClubTournamentPermissionService;
import semo.back.service.feature.tournament.vo.UpsertTournamentRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClubTournamentSupport {
    private static final String MATCH_FORMAT_SINGLE = "SINGLE";
    private static final String MATCH_FORMAT_DOUBLE = "DOUBLE";
    private static final String MATCH_FORMAT_TEAM = "TEAM";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);

    private final ClubTournamentPermissionService clubTournamentPermissionService;
    private final ImageFileUrlResolver imageFileUrlResolver;

    public TournamentDraft toTournamentDraft(
            UpsertTournamentRequest request,
            ClubAccessResolver.ClubAccess access
    ) {
        if (request == null) {
            throw new SemoException.ValidationException("대회 요청이 비어 있습니다.");
        }

        String matchFormat = normalizeMatchFormat(request.matchFormat());
        Integer teamMemberLimit = normalizeTeamMemberLimit(matchFormat, request.teamMemberLimit());
        LocalDateTime applicationStartAt = parseRequiredDateTime(request.applicationStartAt());
        LocalDateTime applicationEndAt = parseRequiredDateTime(request.applicationEndAt());
        LocalDate startDate = parseRequiredDate(request.startDate());
        LocalDate endDate = parseRequiredDate(request.endDate());
        if (applicationEndAt.isBefore(applicationStartAt)) {
            throw new SemoException.ValidationException("신청 종료일시는 시작일시 이후여야 합니다.");
        }
        if (endDate.isBefore(startDate)) {
            throw new SemoException.ValidationException("대회 종료일은 시작일 이후여야 합니다.");
        }
        if (applicationEndAt.toLocalDate().isAfter(endDate)) {
            throw new SemoException.ValidationException("참가 신청 종료일은 대회 종료일보다 늦을 수 없습니다.");
        }

        boolean feeRequired = request.feeRequired() != null && request.feeRequired();
        Integer feeAmount = feeRequired ? request.feeAmount() : null;
        if (feeRequired && feeAmount != null && feeAmount < 0) {
            throw new SemoException.ValidationException("참가비는 0 이상이어야 합니다.");
        }
        if (request.participantLimit() != null && request.participantLimit() < 2) {
            throw new SemoException.ValidationException("참가 인원 제한은 2 이상이어야 합니다.");
        }

        boolean pinned = request.pinned() != null && request.pinned();
        if (pinned && !clubTournamentPermissionService.canPinTournament(access)) {
            throw new SemoException.ForbiddenException("대회를 고정할 권한이 없습니다.");
        }

        return new TournamentDraft(
                request.title().trim(),
                trimToNull(request.summaryText()),
                trimToNull(request.detailText()),
                applicationStartAt,
                applicationEndAt,
                startDate,
                endDate,
                trimToNull(request.locationLabel()),
                matchFormat,
                teamMemberLimit,
                request.participantLimit(),
                feeRequired,
                feeAmount,
                normalizeCurrencyCode(request.feeCurrencyCode()),
                request.postToBoard() == null || request.postToBoard(),
                request.postToCalendar() == null || request.postToCalendar(),
                pinned
        );
    }

    public String normalizeApplicationReviewStatus(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of("APPROVED", "REJECTED").contains(normalized)) {
            throw new SemoException.ValidationException("참가 신청 상태는 APPROVED 또는 REJECTED만 가능합니다.");
        }
        return normalized;
    }

    public String normalizeTournamentApprovalStatus(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of("APPROVED", "REJECTED").contains(normalized)) {
            throw new SemoException.ValidationException("대회 검토 상태는 APPROVED 또는 REJECTED만 가능합니다.");
        }
        return normalized;
    }

    public String formatApplicationWindowLabel(TournamentRecord tournament) {
        return DATE_TIME_LABEL_FORMATTER.format(tournament.getApplicationStartAt())
                + " ~ "
                + DATE_TIME_LABEL_FORMATTER.format(tournament.getApplicationEndAt());
    }

    public String formatTournamentPeriodLabel(TournamentRecord tournament) {
        if (tournament.getStartDate().equals(tournament.getEndDate())) {
            return DATE_LABEL_FORMATTER.format(tournament.getStartDate());
        }
        return DATE_LABEL_FORMATTER.format(tournament.getStartDate())
                + " ~ "
                + DATE_LABEL_FORMATTER.format(tournament.getEndDate());
    }

    public String formatDate(LocalDate date) {
        return date == null ? null : DATE_FORMATTER.format(date);
    }

    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    public String formatDateTimeLabel(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_LABEL_FORMATTER.format(dateTime);
    }

    public String resolveDisplayName(ClubProfile profile) {
        return profile == null ? "알 수 없음" : profile.getDisplayName();
    }

    public String resolveAvatarImageUrl(ClubProfile profile) {
        return profile == null ? null : imageFileUrlResolver.resolveImageUrl(profile.getAvatarFileName());
    }

    public String resolveAvatarThumbnailUrl(ClubProfile profile) {
        return profile == null ? null : imageFileUrlResolver.resolveThumbnailUrl(profile.getAvatarFileName());
    }

    public String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequiredKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new SemoException.ValidationException("필수 값이 비어 있습니다.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMatchFormat(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of(MATCH_FORMAT_SINGLE, MATCH_FORMAT_DOUBLE, MATCH_FORMAT_TEAM).contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 경기 형식입니다.");
        }
        return normalized;
    }

    private Integer normalizeTeamMemberLimit(String matchFormat, Integer teamMemberLimit) {
        if (!MATCH_FORMAT_TEAM.equals(matchFormat)) {
            return null;
        }
        if (teamMemberLimit == null || teamMemberLimit < 3) {
            throw new SemoException.ValidationException("단체전은 팀 인원을 3명 이상으로 설정해야 합니다.");
        }
        return teamMemberLimit;
    }

    private String normalizeCurrencyCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "KRW";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate parseRequiredDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 날짜 형식입니다.");
        }
    }

    private LocalDateTime parseRequiredDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 일시 형식입니다.");
        }
    }

    public record TournamentDraft(
            String title,
            String summaryText,
            String detailText,
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            LocalDate startDate,
            LocalDate endDate,
            String locationLabel,
            String matchFormat,
            Integer teamMemberLimit,
            Integer participantLimit,
            boolean feeRequired,
            Integer feeAmount,
            String feeCurrencyCode,
            boolean postToBoard,
            boolean postToCalendar,
            boolean pinned
    ) {
    }
}
