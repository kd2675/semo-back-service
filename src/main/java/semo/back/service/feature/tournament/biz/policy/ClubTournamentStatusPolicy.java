package semo.back.service.feature.tournament.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClubTournamentStatusPolicy {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_APPLICATION_OPEN = "APPLICATION_OPEN";
    public static final String STATUS_ENTRY_CONFIRMED = "ENTRY_CONFIRMED";
    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPLICATION_APPLIED = "APPLIED";
    private static final String APPLICATION_APPROVED = "APPROVED";
    private static final String APPLICATION_WAITLISTED = "WAITLISTED";

    private final ClubTournamentPermissionService clubTournamentPermissionService;

    public String resolveStatus(TournamentRecord tournament) {
        if (!isApproved(tournament) && tournament.getCancelledAt() == null) {
            return STATUS_DRAFT;
        }
        return resolveStatus(
                tournament.getApplicationStartAt(),
                tournament.getApplicationEndAt(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getCancelledAt()
        );
    }

    public String resolveStatus(
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime cancelledAt
    ) {
        if (cancelledAt != null) {
            return STATUS_CANCELLED;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        if (today.isAfter(endDate)) {
            return STATUS_COMPLETED;
        }
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            return STATUS_ONGOING;
        }
        if (!now.isBefore(applicationStartAt) && !now.isAfter(applicationEndAt)) {
            return STATUS_APPLICATION_OPEN;
        }
        if (today.isBefore(startDate)) {
            return STATUS_ENTRY_CONFIRMED;
        }
        return STATUS_DRAFT;
    }

    public boolean isApplicationOpen(TournamentRecord tournament) {
        return isApproved(tournament) && STATUS_APPLICATION_OPEN.equals(resolveStatus(tournament));
    }

    public boolean isArchived(String status) {
        return STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    public boolean canModify(TournamentRecord tournament) {
        String status = resolveStatus(tournament);
        return !STATUS_COMPLETED.equals(status) && !STATUS_CANCELLED.equals(status);
    }

    public boolean isApproved(TournamentRecord tournament) {
        return tournament != null && APPROVAL_APPROVED.equals(tournament.getApprovalStatus());
    }

    public boolean canView(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        return isApproved(tournament)
                || access.isAdmin()
                || clubTournamentPermissionService.canReviewTournament(access)
                || clubTournamentPermissionService.canDeleteTournament(access)
                || access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId());
    }

    public boolean isActiveApplicationStatus(String applicationStatus) {
        return APPLICATION_APPLIED.equals(applicationStatus)
                || APPLICATION_APPROVED.equals(applicationStatus)
                || APPLICATION_WAITLISTED.equals(applicationStatus);
    }
}
