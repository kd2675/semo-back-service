package semo.back.service.feature.tournament.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.TournamentApplication;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.entity.TournamentRosterMember;
import semo.back.service.database.pub.entity.TournamentScheduleSlot;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRosterMemberRepository;
import semo.back.service.database.pub.repository.TournamentScheduleSlotRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.tournament.biz.TournamentFinanceLinkService;
import semo.back.service.feature.tournament.biz.policy.ClubTournamentPermissionService;
import semo.back.service.feature.tournament.biz.policy.ClubTournamentStatusPolicy;
import semo.back.service.feature.tournament.vo.TournamentApplicationSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import semo.back.service.feature.tournament.vo.TournamentParticipantSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentRosterMemberResponse;
import semo.back.service.feature.tournament.vo.TournamentRosterOptionResponse;
import semo.back.service.feature.tournament.vo.TournamentScheduleSlotResponse;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentUpsertResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClubTournamentViewAssembler {
    private static final String APPLICATION_APPROVED = "APPROVED";

    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final TournamentRosterMemberRepository tournamentRosterMemberRepository;
    private final TournamentScheduleSlotRepository tournamentScheduleSlotRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubTournamentPermissionService clubTournamentPermissionService;
    private final ClubTournamentStatusPolicy clubTournamentStatusPolicy;
    private final ClubTournamentSupport clubTournamentSupport;
    private final TournamentFinanceLinkService tournamentFinanceLinkService;

    public TournamentDetailResponse toDetail(
            ClubAccessResolver.ClubAccess access,
            TournamentRecord tournament
    ) {
        List<TournamentApplication> applications = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(
                        tournament.getTournamentRecordId()
                );
        List<Long> applicationIds = applications.stream()
                .map(TournamentApplication::getTournamentApplicationId)
                .toList();
        List<TournamentRosterMember> rosterMembers = applicationIds.isEmpty()
                ? List.of()
                : tournamentRosterMemberRepository
                        .findByTournamentApplicationIdInOrderBySortOrderAscTournamentRosterMemberIdAsc(applicationIds);
        Map<Long, List<TournamentRosterMember>> rosterByApplicationId = rosterMembers.stream()
                .collect(Collectors.groupingBy(
                        TournamentRosterMember::getTournamentApplicationId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Set<Long> clubProfileIds = new HashSet<>();
        clubProfileIds.add(tournament.getAuthorClubProfileId());
        if (tournament.getReviewedByClubProfileId() != null) {
            clubProfileIds.add(tournament.getReviewedByClubProfileId());
        }
        applications.forEach(application -> {
            clubProfileIds.add(application.getClubProfileId());
            if (application.getReviewedByClubProfileId() != null) {
                clubProfileIds.add(application.getReviewedByClubProfileId());
            }
        });
        rosterMembers.forEach(member -> clubProfileIds.add(member.getClubProfileId()));
        Map<Long, ClubProfile> profileById = loadClubProfiles(clubProfileIds);

        ClubTournamentPermissionService.TournamentActionPermission actionPermission =
                clubTournamentPermissionService.getActionPermission(access, tournament.getAuthorClubProfileId());
        TournamentViewerState viewerState = resolveViewerState(access, tournament, applications);
        boolean canReviewTournament = clubTournamentPermissionService.canReviewTournament(access);
        boolean canManageApplications = canManageApplications(access, tournament);
        List<TournamentApplication> approvedApplications = applications.stream()
                .filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus()))
                .toList();
        List<TournamentScheduleSlotResponse> scheduleSlots = tournamentScheduleSlotRepository
                .findByTournamentRecordIdOrderByStartAtAscTournamentScheduleSlotIdAsc(
                        tournament.getTournamentRecordId()
                ).stream()
                .map(this::toScheduleSlotResponse)
                .toList();
        List<TournamentRosterOptionResponse> availableRosterMembers = loadAvailableRosterMembers(
                access.club().getClubId()
        );
        int activeApplicantCount = (int) applications.stream()
                .filter(application -> clubTournamentStatusPolicy.isActiveApplicationStatus(
                        application.getApplicationStatus()
                ))
                .count();

        return new TournamentDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                access.clubProfile().getClubProfileId(),
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                tournament.getSummaryText(),
                tournament.getDetailText(),
                tournament.getApprovalStatus(),
                clubTournamentStatusPolicy.resolveStatus(tournament),
                clubTournamentSupport.resolveDisplayName(profileById.get(tournament.getAuthorClubProfileId())),
                clubTournamentSupport.resolveAvatarImageUrl(profileById.get(tournament.getAuthorClubProfileId())),
                clubTournamentSupport.resolveAvatarThumbnailUrl(profileById.get(tournament.getAuthorClubProfileId())),
                clubTournamentSupport.resolveDisplayName(profileById.get(tournament.getReviewedByClubProfileId())),
                clubTournamentSupport.formatDateTimeLabel(tournament.getReviewedAt()),
                tournament.getRejectionReason(),
                clubTournamentSupport.formatDateTime(tournament.getApplicationStartAt()),
                clubTournamentSupport.formatDateTime(tournament.getApplicationEndAt()),
                clubTournamentSupport.formatApplicationWindowLabel(tournament),
                clubTournamentSupport.formatDate(tournament.getStartDate()),
                clubTournamentSupport.formatDate(tournament.getEndDate()),
                clubTournamentSupport.formatTournamentPeriodLabel(tournament),
                tournament.getLocationLabel(),
                tournament.getMatchFormat(),
                tournament.getTeamMemberLimit(),
                tournament.getParticipantLimit(),
                tournament.isFeeRequired(),
                tournament.getFeeAmount(),
                tournament.getFeeCurrencyCode(),
                tournamentFinanceLinkService.isFinanceIntegrationEnabled(tournament.getClubId()),
                tournament.isSharedToBoard(),
                tournament.isSharedToCalendar(),
                tournament.isPinned(),
                clubTournamentSupport.formatDateTimeLabel(tournament.getCancelledAt()),
                tournament.getCancelReason(),
                activeApplicantCount,
                approvedApplications.size(),
                approvedApplications.size(),
                clubTournamentStatusPolicy.isApplicationOpen(tournament),
                !viewerState.applied() && clubTournamentStatusPolicy.isApplicationOpen(tournament),
                viewerState.applied(),
                viewerState.applicationStatus(),
                viewerState.participating(),
                canReviewTournament,
                actionPermission.canEdit() && clubTournamentStatusPolicy.canModify(tournament),
                actionPermission.canCancel() && clubTournamentStatusPolicy.canModify(tournament),
                clubTournamentPermissionService.canDeleteTournament(access),
                canManageApplications,
                (canManageApplications ? applications : List.<TournamentApplication>of()).stream()
                        .map(application -> toApplicationSummary(
                                access,
                                application,
                                profileById.get(application.getClubProfileId()),
                                canManageApplications,
                                toRosterResponses(
                                        rosterByApplicationId.getOrDefault(
                                                application.getTournamentApplicationId(),
                                                List.of()
                                        ),
                                        profileById
                                )
                        ))
                        .toList(),
                approvedApplications.stream()
                        .map(application -> toParticipantSummary(
                                application,
                                profileById.get(application.getClubProfileId()),
                                toRosterResponses(
                                        rosterByApplicationId.getOrDefault(
                                                application.getTournamentApplicationId(),
                                                List.of()
                                        ),
                                        profileById
                                )
                        ))
                        .toList(),
                scheduleSlots,
                availableRosterMembers
        );
    }

    public List<TournamentSummaryResponse> toSummaries(
            ClubAccessResolver.ClubAccess access,
            List<TournamentRecord> tournaments
    ) {
        List<TournamentRecord> visibleTournaments = tournaments.stream()
                .filter(tournament -> clubTournamentStatusPolicy.canView(access, tournament))
                .toList();
        List<TournamentApplication> applications = visibleTournaments.isEmpty()
                ? List.of()
                : tournamentApplicationRepository.findByTournamentRecordIdIn(
                        visibleTournaments.stream().map(TournamentRecord::getTournamentRecordId).toList()
                );
        Map<Long, List<TournamentApplication>> applicationsByTournamentId = applications.stream()
                .collect(Collectors.groupingBy(TournamentApplication::getTournamentRecordId));
        Set<Long> profileIds = new HashSet<>();
        visibleTournaments.forEach(tournament -> {
            profileIds.add(tournament.getAuthorClubProfileId());
            if (tournament.getReviewedByClubProfileId() != null) {
                profileIds.add(tournament.getReviewedByClubProfileId());
            }
        });
        applications.forEach(application -> profileIds.add(application.getClubProfileId()));
        Map<Long, ClubProfile> profileById = loadClubProfiles(profileIds);

        return visibleTournaments.stream()
                .map(tournament -> {
                    List<TournamentApplication> tournamentApplications = applicationsByTournamentId.getOrDefault(
                            tournament.getTournamentRecordId(),
                            List.of()
                    );
                    ClubTournamentPermissionService.TournamentActionPermission permission =
                            clubTournamentPermissionService.getActionPermission(
                                    access,
                                    tournament.getAuthorClubProfileId()
                            );
                    TournamentViewerState viewerState = resolveViewerState(
                            access,
                            tournament,
                            tournamentApplications
                    );
                    int approvedApplicationCount = (int) tournamentApplications.stream()
                            .filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus()))
                            .count();
                    return new TournamentSummaryResponse(
                            tournament.getTournamentRecordId(),
                            tournament.getTitle(),
                            tournament.getSummaryText(),
                            tournament.getApprovalStatus(),
                            clubTournamentStatusPolicy.resolveStatus(tournament),
                            clubTournamentSupport.resolveDisplayName(
                                    profileById.get(tournament.getAuthorClubProfileId())
                            ),
                            clubTournamentSupport.resolveAvatarImageUrl(
                                    profileById.get(tournament.getAuthorClubProfileId())
                            ),
                            clubTournamentSupport.resolveAvatarThumbnailUrl(
                                    profileById.get(tournament.getAuthorClubProfileId())
                            ),
                            clubTournamentSupport.formatApplicationWindowLabel(tournament),
                            clubTournamentSupport.formatTournamentPeriodLabel(tournament),
                            clubTournamentSupport.formatDate(tournament.getStartDate()),
                            clubTournamentSupport.formatDate(tournament.getEndDate()),
                            tournament.getLocationLabel(),
                            tournament.getMatchFormat(),
                            tournament.getTeamMemberLimit(),
                            tournament.getParticipantLimit(),
                            approvedApplicationCount,
                            approvedApplicationCount,
                            tournament.isFeeRequired(),
                            tournament.getFeeAmount(),
                            tournament.getFeeCurrencyCode(),
                            tournament.isSharedToBoard(),
                            tournament.isSharedToCalendar(),
                            tournament.isPinned(),
                            viewerState.mine(),
                            viewerState.participating(),
                            permission.canEdit() && clubTournamentStatusPolicy.canModify(tournament),
                            permission.canCancel() && clubTournamentStatusPolicy.canModify(tournament),
                            clubTournamentPermissionService.canDeleteTournament(access)
                    );
                })
                .toList();
    }

    public TournamentUpsertResponse toUpsertResponse(TournamentRecord tournament) {
        return new TournamentUpsertResponse(
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                clubTournamentSupport.formatDate(tournament.getStartDate()),
                clubTournamentSupport.formatDate(tournament.getEndDate()),
                tournament.getApprovalStatus(),
                clubTournamentStatusPolicy.resolveStatus(tournament)
        );
    }

    private TournamentApplicationSummaryResponse toApplicationSummary(
            ClubAccessResolver.ClubAccess access,
            TournamentApplication application,
            ClubProfile profile,
            boolean canManageApplications,
            List<TournamentRosterMemberResponse> rosterMembers
    ) {
        boolean mine = access.clubProfile().getClubProfileId().equals(application.getClubProfileId());
        FinancePayment payment = tournamentFinanceLinkService.findPayment(application.getFinancePaymentId());
        return new TournamentApplicationSummaryResponse(
                application.getTournamentApplicationId(),
                application.getClubProfileId(),
                clubTournamentSupport.resolveDisplayName(profile),
                clubTournamentSupport.resolveAvatarImageUrl(profile),
                clubTournamentSupport.resolveAvatarThumbnailUrl(profile),
                application.getApplicationStatus(),
                application.getApplicationNote(),
                application.getTeamName(),
                application.getWaitlistPosition(),
                application.getFinancePaymentId(),
                payment == null ? null : payment.getPaymentStatusCode(),
                resolvePaymentStatusLabel(payment),
                clubTournamentSupport.formatDateTimeLabel(application.getCheckedInAt()),
                application.getPlacement(),
                application.getResultNote(),
                rosterMembers,
                clubTournamentSupport.formatDateTimeLabel(application.getCreateDate()),
                mine,
                canManageApplications,
                mine && clubTournamentStatusPolicy.isActiveApplicationStatus(application.getApplicationStatus())
        );
    }

    private TournamentParticipantSummaryResponse toParticipantSummary(
            TournamentApplication application,
            ClubProfile profile,
            List<TournamentRosterMemberResponse> rosterMembers
    ) {
        FinancePayment payment = tournamentFinanceLinkService.findPayment(application.getFinancePaymentId());
        return new TournamentParticipantSummaryResponse(
                application.getClubProfileId(),
                clubTournamentSupport.resolveDisplayName(profile),
                clubTournamentSupport.resolveAvatarImageUrl(profile),
                clubTournamentSupport.resolveAvatarThumbnailUrl(profile),
                clubTournamentSupport.formatDateTimeLabel(application.getReviewedAt()),
                application.getTeamName(),
                application.getFinancePaymentId(),
                payment == null ? null : payment.getPaymentStatusCode(),
                resolvePaymentStatusLabel(payment),
                clubTournamentSupport.formatDateTimeLabel(application.getCheckedInAt()),
                application.getPlacement(),
                application.getResultNote(),
                rosterMembers
        );
    }

    private TournamentViewerState resolveViewerState(
            ClubAccessResolver.ClubAccess access,
            TournamentRecord tournament,
            List<TournamentApplication> applications
    ) {
        Long viewerClubProfileId = access.clubProfile().getClubProfileId();
        TournamentApplication myApplication = applications.stream()
                .filter(application -> application.getClubProfileId().equals(viewerClubProfileId))
                .findFirst()
                .orElse(null);
        boolean participating = myApplication != null
                && APPLICATION_APPROVED.equals(myApplication.getApplicationStatus());
        boolean applied = myApplication != null
                && clubTournamentStatusPolicy.isActiveApplicationStatus(myApplication.getApplicationStatus());
        return new TournamentViewerState(
                access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId()),
                applied,
                myApplication == null ? null : myApplication.getApplicationStatus(),
                participating
        );
    }

    private List<TournamentRosterMemberResponse> toRosterResponses(
            List<TournamentRosterMember> rosterMembers,
            Map<Long, ClubProfile> profileById
    ) {
        return rosterMembers.stream()
                .map(member -> {
                    ClubProfile profile = profileById.get(member.getClubProfileId());
                    return new TournamentRosterMemberResponse(
                            member.getClubProfileId(),
                            clubTournamentSupport.resolveDisplayName(profile),
                            clubTournamentSupport.resolveAvatarImageUrl(profile),
                            clubTournamentSupport.resolveAvatarThumbnailUrl(profile),
                            member.getRosterRoleCode()
                    );
                })
                .toList();
    }

    private List<TournamentRosterOptionResponse> loadAvailableRosterMembers(Long clubId) {
        return clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .map(snapshot -> new TournamentRosterOptionResponse(
                        snapshot.clubProfile().getClubProfileId(),
                        snapshot.clubProfile().getDisplayName(),
                        clubTournamentSupport.resolveAvatarImageUrl(snapshot.clubProfile()),
                        clubTournamentSupport.resolveAvatarThumbnailUrl(snapshot.clubProfile())
                ))
                .toList();
    }

    private TournamentScheduleSlotResponse toScheduleSlotResponse(TournamentScheduleSlot slot) {
        return new TournamentScheduleSlotResponse(
                slot.getTournamentScheduleSlotId(),
                slot.getTitle(),
                slot.getCourtLabel(),
                clubTournamentSupport.formatDateTime(slot.getStartAt()),
                clubTournamentSupport.formatDateTimeLabel(slot.getStartAt()),
                clubTournamentSupport.formatDateTime(slot.getEndAt()),
                clubTournamentSupport.formatDateTimeLabel(slot.getEndAt()),
                slot.getNote()
        );
    }

    private String resolvePaymentStatusLabel(FinancePayment payment) {
        if (payment == null) {
            return null;
        }
        return switch (payment.getPaymentStatusCode()) {
            case "PENDING" -> "납부 대기";
            case "PAID" -> "납부 완료";
            case "WAIVED" -> "면제";
            default -> payment.getPaymentStatusCode();
        };
    }

    private Map<Long, ClubProfile> loadClubProfiles(Collection<Long> clubProfileIds) {
        if (clubProfileIds == null || clubProfileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ClubProfile> result = new HashMap<>();
        clubProfileRepository.findAllById(clubProfileIds)
                .forEach(profile -> result.put(profile.getClubProfileId(), profile));
        return result;
    }

    private boolean canManageApplications(
            ClubAccessResolver.ClubAccess access,
            TournamentRecord tournament
    ) {
        return access.isAdmin()
                || access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId());
    }

    private record TournamentViewerState(
            boolean mine,
            boolean applied,
            String applicationStatus,
            boolean participating
    ) {
    }
}
