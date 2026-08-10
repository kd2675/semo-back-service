package semo.back.service.feature.clubfeature.biz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.repository.BracketRecordRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubHandoverNoteRepository;
import semo.back.service.database.pub.repository.ClubTermCarryoverItemRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemApplicationRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMoreWorkQueueService {
    private static final Set<String> FEEDBACK_IN_PROGRESS_STATUSES = Set.of("RECEIVED", "IN_REVIEW");

    private final TodoItemRepository todoItemRepository;
    private final TodoItemApplicationRepository todoItemApplicationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final ClubFeedbackRepository clubFeedbackRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final ClubHandoverNoteRepository clubHandoverNoteRepository;
    private final ClubTermCarryoverItemRepository clubTermCarryoverItemRepository;
    private final TournamentRecordRepository tournamentRecordRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final BracketRecordRepository bracketRecordRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final DecisionRecordRepository decisionRecordRepository;

    public Map<String, FeatureQueueCounts> getQueueCounts(
            Long clubId,
            Long clubProfileId,
            List<ClubFeatureResponse> features,
            Set<String> adminToolFeatureKeys
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        Map<String, FeatureQueueCounts> counts = new LinkedHashMap<>();

        for (ClubFeatureResponse feature : features) {
            if (!feature.enabled()) {
                continue;
            }
            boolean adminAccessible = adminToolFeatureKeys.contains(feature.featureKey());
            counts.put(feature.featureKey(), switch (feature.featureKey()) {
                case "TODO" -> todoCounts(clubId, clubProfileId, now, adminAccessible);
                case "FINANCE" -> financeCounts(clubId, clubProfileId, now, adminAccessible);
                case "FEEDBACK" -> feedbackCounts(clubId, clubProfileId, adminAccessible);
                case "JOIN_REQUEST" -> adminOnlyCounts(
                        adminAccessible
                                ? clubJoinRequestRepository.countByClubIdAndRequestStatus(clubId, "PENDING")
                                : 0
                );
                case "TOURNAMENT_RECORD" -> tournamentCounts(clubId, clubProfileId, adminAccessible);
                case "BRACKET" -> bracketCounts(clubId, clubProfileId, adminAccessible);
                case "SCHEDULE_MANAGE" -> userOnlyCounts(
                        clubScheduleEventRepository.countPendingParticipationResponses(
                                clubId,
                                clubProfileId,
                                today.atStartOfDay()
                        )
                );
                case "POLL" -> userOnlyCounts(
                        clubScheduleVoteRepository.countPendingSelections(
                                clubId,
                                clubProfileId,
                                today,
                                now.toLocalTime()
                        )
                );
                case "ATTENDANCE" -> adminOnlyCounts(
                        adminAccessible
                                ? clubEventParticipantRepository.countStartedEventAttendancePending(clubId, now)
                                : 0
                );
                case "HANDOVER" -> adminOnlyCounts(
                        adminAccessible
                                ? clubHandoverNoteRepository.countByClubIdAndDeletedFalseAndStatusCodeIn(
                                            clubId,
                                            List.of("DRAFT", "READY")
                                    ) + clubTermCarryoverItemRepository.countByClubIdAndStatusCode(clubId, "OPEN")
                                : 0
                );
                case "DECISION_LOG" -> decisionCounts(clubId, today, adminAccessible);
                default -> FeatureQueueCounts.empty();
            });
        }
        return Map.copyOf(counts);
    }

    private FeatureQueueCounts todoCounts(
            Long clubId,
            Long clubProfileId,
            LocalDateTime now,
            boolean adminAccessible
    ) {
        long adminPendingCount = 0;
        long adminOverdueCount = 0;
        if (adminAccessible) {
            adminPendingCount = todoItemRepository.countActiveForAdmin(clubId)
                    + todoItemApplicationRepository.countPendingApplicationsForClub(clubId);
            adminOverdueCount = todoItemRepository.countOverdueForAdmin(clubId, now);
        }
        return FeatureQueueCounts.of(
                todoItemRepository.countActiveAssigned(clubId, clubProfileId),
                todoItemRepository.countOverdueAssigned(clubId, clubProfileId, now),
                adminPendingCount,
                adminOverdueCount
        );
    }

    private FeatureQueueCounts financeCounts(
            Long clubId,
            Long clubProfileId,
            LocalDateTime now,
            boolean adminAccessible
    ) {
        long adminPendingCount = 0;
        long adminOverdueCount = 0;
        if (adminAccessible) {
            ClubAdminFinanceSummaryAggregate summary = financePaymentRepository.summarizeAdminFinance(clubId, now);
            adminPendingCount = summary.pendingPaymentCount()
                    + financeRequestRepository.countByClubIdAndStatusCode(clubId, "PENDING");
            adminOverdueCount = summary.overduePaymentCount();
        }
        long userPendingCount = financePaymentRepository.countPendingForMember(clubId, clubProfileId)
                + financeRequestRepository.countByClubIdAndRequesterClubProfileIdAndStatusCode(
                        clubId,
                        clubProfileId,
                        "PENDING"
                );
        return FeatureQueueCounts.of(
                userPendingCount,
                financePaymentRepository.countOverdueForMember(clubId, clubProfileId, now),
                adminPendingCount,
                adminOverdueCount
        );
    }

    private FeatureQueueCounts feedbackCounts(Long clubId, Long clubProfileId, boolean adminAccessible) {
        long adminPendingCount = adminAccessible
                ? clubFeedbackRepository.countByClubIdAndDeletedFalseAndStatusCodeIn(
                        clubId,
                        FEEDBACK_IN_PROGRESS_STATUSES
                )
                : 0;
        return FeatureQueueCounts.of(
                clubFeedbackRepository.countByClubIdAndSubmitterClubProfileIdAndDeletedFalseAndStatusCodeIn(
                        clubId,
                        clubProfileId,
                        FEEDBACK_IN_PROGRESS_STATUSES
                ),
                0,
                adminPendingCount,
                0
        );
    }

    private FeatureQueueCounts tournamentCounts(Long clubId, Long clubProfileId, boolean adminAccessible) {
        long adminPendingCount = adminAccessible
                ? tournamentRecordRepository.countByClubIdAndDeletedFalseAndApprovalStatus(clubId, "PENDING")
                        + tournamentApplicationRepository.countPendingApplicationsForClub(clubId)
                : 0;
        return FeatureQueueCounts.of(
                tournamentApplicationRepository.countPendingApplicationsForMember(clubId, clubProfileId),
                0,
                adminPendingCount,
                0
        );
    }

    private FeatureQueueCounts bracketCounts(Long clubId, Long clubProfileId, boolean adminAccessible) {
        long adminPendingCount = adminAccessible
                ? bracketRecordRepository.countByClubIdAndDeletedFalseAndApprovalStatus(clubId, "PENDING")
                : 0;
        return FeatureQueueCounts.of(
                bracketRecordRepository.countByClubIdAndAuthorClubProfileIdAndDeletedFalseAndApprovalStatus(
                        clubId,
                        clubProfileId,
                        "PENDING"
                ),
                0,
                adminPendingCount,
                0
        );
    }

    private FeatureQueueCounts decisionCounts(Long clubId, LocalDate today, boolean adminAccessible) {
        if (!adminAccessible) {
            return FeatureQueueCounts.empty();
        }
        long reviewDueCount = decisionRecordRepository.countReviewDue(clubId, today);
        return FeatureQueueCounts.of(
                0,
                0,
                decisionRecordRepository.countByClubIdAndDeletedFalseAndStatusCode(clubId, "DRAFT")
                        + reviewDueCount,
                reviewDueCount
        );
    }

    private FeatureQueueCounts userOnlyCounts(long userPendingCount) {
        return FeatureQueueCounts.of(userPendingCount, 0, 0, 0);
    }

    private FeatureQueueCounts adminOnlyCounts(long adminPendingCount) {
        return FeatureQueueCounts.of(0, 0, adminPendingCount, 0);
    }

    public record FeatureQueueCounts(
            int userPendingCount,
            int userOverdueCount,
            int adminPendingCount,
            int adminOverdueCount
    ) {
        public static FeatureQueueCounts empty() {
            return new FeatureQueueCounts(0, 0, 0, 0);
        }

        public static FeatureQueueCounts of(
                long userPendingCount,
                long userOverdueCount,
                long adminPendingCount,
                long adminOverdueCount
        ) {
            return new FeatureQueueCounts(
                    safeCount(userPendingCount),
                    safeCount(userOverdueCount),
                    safeCount(adminPendingCount),
                    safeCount(adminOverdueCount)
            );
        }

        private static int safeCount(long count) {
            if (count <= 0) {
                return 0;
            }
            return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        }
    }
}
