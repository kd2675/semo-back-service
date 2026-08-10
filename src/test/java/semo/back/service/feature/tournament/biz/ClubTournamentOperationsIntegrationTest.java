package semo.back.service.feature.tournament.biz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.database.pub.repository.TournamentRosterMemberRepository;
import semo.back.service.database.pub.repository.TournamentScheduleSlotRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;
import semo.back.service.feature.tournament.vo.ReviewTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.ReviewTournamentRecordRequest;
import semo.back.service.feature.tournament.vo.SubmitTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.UpdateTournamentApplicationOperationsRequest;
import semo.back.service.feature.tournament.vo.UpsertTournamentRequest;
import semo.back.service.feature.tournament.vo.UpsertTournamentScheduleSlotRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubTournamentOperationsIntegrationTest {
    @Autowired
    private ClubTournamentService clubTournamentService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private ClubAccessResolver clubAccessResolver;

    @Autowired
    private TournamentRosterMemberRepository tournamentRosterMemberRepository;

    @Autowired
    private TournamentScheduleSlotRepository tournamentScheduleSlotRepository;

    @Autowired
    private TournamentApplicationRepository tournamentApplicationRepository;

    @Autowired
    private TournamentRecordRepository tournamentRecordRepository;

    @Autowired
    private FinancePaymentRepository financePaymentRepository;

    @Autowired
    private FinanceObligationRepository financeObligationRepository;

    @Autowired
    private ClubNotificationRepository clubNotificationRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        clubNotificationRepository.deleteAll();
        financePaymentRepository.deleteAll();
        financeObligationRepository.findAll().stream()
                .sorted(Comparator.comparingLong(obligation -> -obligation.getFinanceObligationId()))
                .forEach(financeObligationRepository::delete);
        tournamentRosterMemberRepository.deleteAll();
        tournamentScheduleSlotRepository.deleteAll();
        tournamentApplicationRepository.deleteAll();
        tournamentRecordRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void applyToTournament_fullCapacity_waitlistsAndPromotesOldestAfterCancellation() {
        TestClub club = createClub("tournament-owner-001", "대회장", "대기열 테스트", false);
        addActiveMember(club.clubId(), "tournament-member-001", "첫 번째");
        addActiveMember(club.clubId(), "tournament-member-002", "두 번째");
        addActiveMember(club.clubId(), "tournament-member-003", "세 번째");
        Long tournamentId = createApprovedTournament(club, "SINGLE", null, 2, false);

        clubTournamentService.applyToTournament(
                club.clubId(), tournamentId, "tournament-member-001", new SubmitTournamentApplicationRequest(null)
        );
        clubTournamentService.applyToTournament(
                club.clubId(), tournamentId, "tournament-member-002", new SubmitTournamentApplicationRequest(null)
        );
        var waitlistedDetail = clubTournamentService.applyToTournament(
                club.clubId(), tournamentId, "tournament-member-003", new SubmitTournamentApplicationRequest(null)
        );

        assertThat(waitlistedDetail.myApplicationStatus()).isEqualTo("WAITLISTED");
        assertThat(tournamentApplicationRepository
                .findByTournamentRecordIdAndApplicationStatusOrderByCreateDateAscTournamentApplicationIdAsc(
                        tournamentId,
                        "WAITLISTED"
                ))
                .singleElement()
                .returns(1, application -> application.getWaitlistPosition());

        clubTournamentService.cancelMyApplication(club.clubId(), tournamentId, "tournament-member-001");

        assertThat(tournamentApplicationRepository
                .findByTournamentRecordIdAndApplicationStatusOrderByCreateDateAscTournamentApplicationIdAsc(
                        tournamentId,
                        "APPLIED"
                ))
                .extracting(application -> application.getClubProfileId())
                .containsExactlyInAnyOrder(
                        clubProfileId(club.clubId(), "tournament-member-002"),
                        clubProfileId(club.clubId(), "tournament-member-003")
                );
        assertThat(clubNotificationRepository.findAll())
                .anySatisfy(notification -> assertThat(notification.getNotificationType())
                        .isEqualTo("TOURNAMENT_WAITLIST_PROMOTED"));
    }

    @Test
    void applyToTournament_teamRoster_persistsRosterAndRejectsDuplicateMemberAcrossTeams() {
        TestClub club = createClub("tournament-owner-002", "대회장", "팀 테스트", false);
        addActiveMember(club.clubId(), "tournament-captain-001", "주장 A");
        addActiveMember(club.clubId(), "tournament-player-001", "선수 A1");
        addActiveMember(club.clubId(), "tournament-player-002", "선수 A2");
        addActiveMember(club.clubId(), "tournament-captain-002", "주장 B");
        Long tournamentId = createApprovedTournament(club, "TEAM", 4, 4, false);

        Long playerOneProfileId = clubProfileId(club.clubId(), "tournament-player-001");
        Long playerTwoProfileId = clubProfileId(club.clubId(), "tournament-player-002");
        var detail = clubTournamentService.applyToTournament(
                club.clubId(),
                tournamentId,
                "tournament-captain-001",
                new SubmitTournamentApplicationRequest(
                        "첫 번째 팀",
                        "블루 팀",
                        List.of(playerOneProfileId, playerTwoProfileId)
                )
        );

        assertThat(detail.myApplicationStatus()).isEqualTo("APPLIED");
        assertThat(tournamentRosterMemberRepository.findAll())
                .hasSize(3)
                .extracting(member -> member.getRosterRoleCode())
                .containsExactly("CAPTAIN", "MEMBER", "MEMBER");

        assertThatThrownBy(() -> clubTournamentService.applyToTournament(
                club.clubId(),
                tournamentId,
                "tournament-captain-002",
                new SubmitTournamentApplicationRequest(
                        null,
                        "레드 팀",
                        List.of(playerOneProfileId, club.ownerClubProfileId())
                )
        ))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("이미 다른 참가 신청에 포함된 멤버");
    }

    @Test
    void approvedPaidApplication_linksFinanceAndSupportsScheduleCheckInAndResult() {
        TestClub club = createClub("tournament-owner-003", "대회장", "운영 테스트", true);
        addActiveMember(club.clubId(), "tournament-player-003", "결승 선수");
        Long tournamentId = createApprovedTournament(club, "SINGLE", null, 8, true);
        clubTournamentService.applyToTournament(
                club.clubId(), tournamentId, "tournament-player-003", new SubmitTournamentApplicationRequest("우승 도전")
        );
        Long applicationId = tournamentApplicationRepository
                .findByTournamentRecordIdAndClubProfileId(
                        tournamentId,
                        clubProfileId(club.clubId(), "tournament-player-003")
                )
                .orElseThrow()
                .getTournamentApplicationId();

        var approved = clubTournamentService.reviewApplication(
                club.clubId(),
                tournamentId,
                applicationId,
                club.ownerUserKey(),
                new ReviewTournamentApplicationRequest("APPROVED", "참가 확정")
        );

        assertThat(approved.applications())
                .singleElement()
                .satisfies(application -> {
                    assertThat(application.financePaymentId()).isNotNull();
                    assertThat(application.feePaymentStatusCode()).isEqualTo("PENDING");
                });
        FinancePayment payment = financePaymentRepository.findAll().getFirst();
        assertThat(payment.getPaymentStatusCode()).isEqualTo("PENDING");
        assertThat(financeObligationRepository.findAll())
                .singleElement()
                .returns(applicationId, obligation -> obligation.getSourceTournamentApplicationId());

        LocalDate startDate = LocalDate.now().plusDays(2);
        clubTournamentService.createScheduleSlot(
                club.clubId(),
                tournamentId,
                club.ownerUserKey(),
                new UpsertTournamentScheduleSlotRequest(
                        "결승",
                        "센터 코트",
                        startDate.atTime(14, 0).toString(),
                        startDate.atTime(15, 0).toString(),
                        "경기 20분 전 집결"
                )
        );
        var operated = clubTournamentService.updateApplicationOperations(
                club.clubId(),
                tournamentId,
                applicationId,
                club.ownerUserKey(),
                new UpdateTournamentApplicationOperationsRequest(true, 1, "결승 우승")
        );

        assertThat(operated.scheduleSlots())
                .singleElement()
                .returns("센터 코트", slot -> slot.courtLabel());
        assertThat(operated.participants())
                .singleElement()
                .satisfies(participant -> {
                    assertThat(participant.checkedInAtLabel()).isNotBlank();
                    assertThat(participant.placement()).isEqualTo(1);
                    assertThat(participant.resultNote()).isEqualTo("결승 우승");
                });

        clubTournamentService.cancelMyApplication(club.clubId(), tournamentId, "tournament-player-003");
        assertThat(financePaymentRepository.findById(payment.getFinancePaymentId()).orElseThrow().getPaymentStatusCode())
                .isEqualTo("WAIVED");
    }

    private TestClub createClub(
            String ownerUserKey,
            String ownerDisplayName,
            String clubName,
            boolean financeEnabled
    ) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                ownerDisplayName,
                new CreateClubRequest(clubName, "대회 테스트", "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(financeEnabled
                        ? List.of("TOURNAMENT_RECORD", "FINANCE")
                        : List.of("TOURNAMENT_RECORD"))
        );
        Long ownerClubProfileId = clubAccessResolver.requireActiveMember(clubId, ownerUserKey)
                .clubProfile()
                .getClubProfileId();
        return new TestClub(clubId, ownerUserKey, ownerClubProfileId);
    }

    private void addActiveMember(Long clubId, String userKey, String displayName) {
        Long profileId = profileUserService.resolveProfileId(userKey, displayName);
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
        clubAccessResolver.requireActiveMember(clubId, userKey);
    }

    private Long clubProfileId(Long clubId, String userKey) {
        return clubAccessResolver.requireActiveMember(clubId, userKey).clubProfile().getClubProfileId();
    }

    private Long createApprovedTournament(
            TestClub club,
            String matchFormat,
            Integer teamMemberLimit,
            Integer participantLimit,
            boolean feeRequired
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate startDate = LocalDate.now().plusDays(2);
        var created = clubTournamentService.createTournament(
                club.clubId(),
                club.ownerUserKey(),
                new UpsertTournamentRequest(
                        "통합 운영 대회",
                        "운영 흐름 검증",
                        "팀과 참가비, 현장 운영을 검증합니다.",
                        now.minusHours(1).withSecond(0).withNano(0).toString(),
                        now.plusDays(1).withSecond(0).withNano(0).toString(),
                        startDate.toString(),
                        startDate.plusDays(1).toString(),
                        "테스트 경기장",
                        matchFormat,
                        teamMemberLimit,
                        participantLimit,
                        feeRequired,
                        feeRequired ? 20000 : null,
                        "KRW",
                        false,
                        false,
                        false
                )
        );
        clubTournamentService.reviewTournament(
                club.clubId(),
                created.tournamentRecordId(),
                club.ownerUserKey(),
                new ReviewTournamentRecordRequest("APPROVED", null)
        );
        return created.tournamentRecordId();
    }

    private record TestClub(Long clubId, String ownerUserKey, Long ownerClubProfileId) {
    }
}
