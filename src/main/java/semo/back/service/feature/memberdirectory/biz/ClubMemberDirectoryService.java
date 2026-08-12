package semo.back.service.feature.memberdirectory.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.MemberDirectorySetting;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.database.pub.repository.MemberDirectorySettingRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.memberdirectory.vo.ClubAdminMemberDirectoryResponse;
import semo.back.service.feature.memberdirectory.vo.ClubMemberDirectoryResponse;
import semo.back.service.feature.memberdirectory.vo.MemberDirectoryMemberResponse;
import semo.back.service.feature.memberdirectory.vo.MemberDirectoryRecentActivityResponse;
import semo.back.service.feature.memberdirectory.vo.MemberDirectorySettingsResponse;
import semo.back.service.feature.memberdirectory.vo.UpdateClubAdminMemberDirectorySettingsRequest;
import semo.back.service.feature.position.biz.ClubPositionService;
import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberDirectoryService {
    private static final String FEATURE_MEMBER_DIRECTORY = "MEMBER_DIRECTORY";
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final MemberDirectorySettingsResponse DEFAULT_SETTINGS = new MemberDirectorySettingsResponse(true, true, false);
    private static final Map<String, String> MEMBER_VISIBLE_ACTIVITY_LABELS = Map.of(
            "공지관리", "공지를 관리했습니다.",
            "일정관리", "일정을 관리했습니다.",
            "투표관리", "투표에 참여하거나 관리했습니다.",
            "출석관리", "출석 활동을 남겼습니다.",
            "대회관리", "대회 활동을 남겼습니다.",
            "대진표관리", "대진표 활동을 남겼습니다."
    );

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubPositionService clubPositionService;
    private final ClubActivityLogRepository clubActivityLogRepository;
    private final MemberDirectorySettingRepository memberDirectorySettingRepository;
    private final ImageFileUrlResolver imageFileUrlResolver;

    public ClubMemberDirectoryResponse getMemberDirectory(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireMemberDirectoryFeature(clubId);
        MemberDirectorySettingsResponse settings = loadSettings(clubId);
        List<MemberDirectoryMemberResponse> members = loadDirectoryMembers(clubId).stream()
                .map(member -> applyVisibilitySettings(member, settings))
                .toList();
        return new ClubMemberDirectoryResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                true,
                members.size(),
                settings,
                members
        );
    }

    public ClubAdminMemberDirectoryResponse getAdminMemberDirectory(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireMemberDirectoryFeature(clubId);
        List<MemberDirectoryMemberResponse> members = loadDirectoryMembers(clubId);
        return new ClubAdminMemberDirectoryResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                true,
                members.size(),
                loadSettings(clubId),
                members
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회원디렉터리")
    public ClubAdminMemberDirectoryResponse updateAdminMemberDirectory(
            Long clubId,
            String userKey,
            UpdateClubAdminMemberDirectorySettingsRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireMemberDirectoryFeature(clubId);
        boolean showPositions = requireToggle(request == null ? null : request.showPositions(), "직책");
        boolean showTagline = requireToggle(request == null ? null : request.showTagline(), "한줄소개");
        boolean showRecentActivity = requireToggle(request == null ? null : request.showRecentActivity(), "최근 활동");

        MemberDirectorySetting current = memberDirectorySettingRepository.findByClubId(clubId).orElse(null);
        memberDirectorySettingRepository.save(MemberDirectorySetting.builder()
                .memberDirectorySettingId(current == null ? null : current.getMemberDirectorySettingId())
                .clubId(clubId)
                .showPositions(showPositions)
                .showTagline(showTagline)
                .showRecentActivity(showRecentActivity)
                .updatedByClubProfileId(access.clubProfile().getClubProfileId())
                .build());
        ClubActivityContextHolder.setDetails(
                "회원 디렉터리 노출 항목을 " + buildSettingSummary(showPositions, showTagline, showRecentActivity) + "로 저장했습니다.",
                "회원 디렉터리 노출 항목을 저장하지 못했습니다."
        );
        return getAdminMemberDirectory(clubId, userKey);
    }

    private List<MemberDirectoryMemberResponse> loadDirectoryMembers(Long clubId) {
        List<ClubAccessResolver.ClubMemberSnapshot> snapshots = clubAccessResolver.getActiveMemberSnapshots(clubId);
        if (snapshots.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ClubPositionSummaryResponse>> positionsByMemberId = clubPositionService.getAssignedPositionSummaries(
                clubId,
                snapshots.stream().map(snapshot -> snapshot.membership().getClubMemberId()).toList()
        );
        Map<Long, MemberDirectoryRecentActivityResponse> recentActivityByProfileId = loadRecentActivityByProfileId(clubId, snapshots);

        return snapshots.stream()
                .map(snapshot -> toMemberResponse(
                        snapshot,
                        positionsByMemberId.getOrDefault(snapshot.membership().getClubMemberId(), List.of()),
                        recentActivityByProfileId.get(snapshot.clubProfile().getClubProfileId())
                ))
                .sorted(directoryComparator())
                .toList();
    }

    private Map<Long, MemberDirectoryRecentActivityResponse> loadRecentActivityByProfileId(
            Long clubId,
            List<ClubAccessResolver.ClubMemberSnapshot> snapshots
    ) {
        List<Long> clubProfileIds = snapshots.stream()
                .map(snapshot -> snapshot.clubProfile().getClubProfileId())
                .distinct()
                .toList();
        if (clubProfileIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, MemberDirectoryRecentActivityResponse> recentActivityByProfileId = new LinkedHashMap<>();
        for (ClubActivityLog log : clubActivityLogRepository
                .findLatestByClubIdAndActorClubProfileIdIn(clubId, clubProfileIds)) {
            if (log.getActorClubProfileId() == null || recentActivityByProfileId.containsKey(log.getActorClubProfileId())) {
                continue;
            }
            String publicDetail = MEMBER_VISIBLE_ACTIVITY_LABELS.get(log.getSubject());
            if (publicDetail == null) {
                continue;
            }
            recentActivityByProfileId.put(log.getActorClubProfileId(), new MemberDirectoryRecentActivityResponse(
                    log.getSubject(),
                    publicDetail,
                    formatDateTimeValue(log.getCreatedAt()),
                    formatDateTimeLabel(log.getCreatedAt())
            ));
        }
        return recentActivityByProfileId;
    }

    private MemberDirectoryMemberResponse toMemberResponse(
            ClubAccessResolver.ClubMemberSnapshot snapshot,
            List<ClubPositionSummaryResponse> positions,
            MemberDirectoryRecentActivityResponse recentActivity
    ) {
        ClubMember membership = snapshot.membership();
        return new MemberDirectoryMemberResponse(
                membership.getClubMemberId(),
                snapshot.clubProfile().getClubProfileId(),
                snapshot.clubProfile().getDisplayName(),
                imageFileUrlResolver.resolveImageUrl(snapshot.clubProfile().getAvatarFileName()),
                membership.getRoleCode(),
                toRoleLabel(membership.getRoleCode()),
                positions,
                trimToNull(snapshot.clubProfile().getTagline()),
                recentActivity
        );
    }

    private MemberDirectoryMemberResponse applyVisibilitySettings(
            MemberDirectoryMemberResponse member,
            MemberDirectorySettingsResponse settings
    ) {
        return new MemberDirectoryMemberResponse(
                member.clubMemberId(),
                member.clubProfileId(),
                member.displayName(),
                member.avatarImageUrl(),
                settings.showPositions() ? member.roleCode() : "",
                settings.showPositions() ? member.roleLabel() : "",
                settings.showPositions() ? member.positions() : List.of(),
                settings.showTagline() ? member.tagline() : null,
                settings.showRecentActivity() ? member.recentActivity() : null
        );
    }

    private Comparator<MemberDirectoryMemberResponse> directoryComparator() {
        return Comparator.comparingInt((MemberDirectoryMemberResponse member) -> rolePriority(member.roleCode()))
                .thenComparing(MemberDirectoryMemberResponse::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MemberDirectoryMemberResponse::clubMemberId);
    }

    private int rolePriority(String roleCode) {
        return switch (roleCode) {
            case "OWNER" -> 0;
            case "ADMIN" -> 1;
            default -> 2;
        };
    }

    private MemberDirectorySettingsResponse loadSettings(Long clubId) {
        return memberDirectorySettingRepository.findByClubId(clubId)
                .map(setting -> new MemberDirectorySettingsResponse(
                        setting.isShowPositions(),
                        setting.isShowTagline(),
                        setting.isShowRecentActivity()
                ))
                .orElse(DEFAULT_SETTINGS);
    }

    private void requireMemberDirectoryFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_MEMBER_DIRECTORY)) {
            throw new SemoException.ForbiddenException("회원 디렉터리 기능이 활성화되지 않았습니다.");
        }
    }

    private boolean requireToggle(Boolean value, String label) {
        if (value == null) {
            throw new SemoException.ValidationException(label + " 노출 여부는 필수입니다.");
        }
        return value;
    }

    private String buildSettingSummary(boolean showPositions, boolean showTagline, boolean showRecentActivity) {
        List<String> visibleLabels = Stream.of(
                showPositions ? "직책" : null,
                showTagline ? "한줄소개" : null,
                showRecentActivity ? "최근 활동" : null
        ).filter(StringUtils::hasText).toList();
        if (visibleLabels.isEmpty()) {
            return "멤버 이름만";
        }
        return "멤버 이름과 " + String.join(", ", visibleLabels);
    }

    private String toRoleLabel(String roleCode) {
        return switch (trimToNull(roleCode)) {
            case "OWNER" -> "소유자";
            case "ADMIN" -> "관리자";
            default -> "일반 회원";
        };
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String formatDateTimeValue(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_REQUEST_FORMATTER);
    }

    private String formatDateTimeLabel(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_LABEL_FORMATTER);
    }
}
