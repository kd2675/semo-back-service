package semo.back.service.feature.dues.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.DuesChargeRepository;
import semo.back.service.database.pub.repository.DuesInvoiceRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.dues.vo.CreateClubDuesChargeRequest;
import semo.back.service.feature.dues.vo.UpdateClubDuesPaymentStatusRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubDuesServiceTest {

    @Autowired
    private ClubDuesService clubDuesService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private DuesInvoiceRepository duesInvoiceRepository;

    @Autowired
    private DuesChargeRepository duesChargeRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        duesInvoiceRepository.deleteAll();
        duesChargeRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void memberCanSeeIssuedCustomChargeAndAdminCanMarkItPaid() {
        Long clubId = createEnabledClub("dues-owner-001", "Dues Owner", "Dues Club");
        addActiveMember(clubId, "dues-member-001", "Dues Member");

        var created = clubDuesService.createCharge(
                clubId,
                "dues-owner-001",
                new CreateClubDuesChargeRequest(
                        "봄 대회 참가비",
                        new BigDecimal("10000"),
                        "2026-04-30T23:59",
                        "현장 집결 전 납부",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        assertThat(created.createdCount()).isEqualTo(2);
        assertThat(created.title()).isEqualTo("봄 대회 참가비");

        var memberDues = clubDuesService.getDues(clubId, "dues-member-001");
        assertThat(memberDues.openCharges()).hasSize(1);
        assertThat(memberDues.nextPayableCharge()).isNotNull();
        assertThat(memberDues.nextPayableCharge().title()).isEqualTo("봄 대회 참가비");

        Long invoiceId = memberDues.nextPayableCharge().invoice().invoiceId();
        var updated = clubDuesService.updatePaymentStatus(
                clubId,
                invoiceId,
                "dues-owner-001",
                new UpdateClubDuesPaymentStatusRequest("PAID", "현장 수납")
        );

        assertThat(updated.paymentStatus()).isEqualTo("PAID");
        assertThat(duesInvoiceRepository.findByDuesInvoiceIdAndClubId(invoiceId, clubId))
                .get()
                .extracting(invoice -> invoice.getPaymentStatus())
                .isEqualTo("PAID");
    }

    @Test
    void selectedMembersOnlyReceiveSelectedCharge() {
        Long clubId = createEnabledClub("dues-owner-002", "Dues Owner 2", "Dues Club 2");
        addActiveMember(clubId, "dues-member-002a", "Selected Member");
        addActiveMember(clubId, "dues-member-002b", "Excluded Member");

        var adminBeforeIssue = clubDuesService.getAdminDues(clubId, "dues-owner-002");
        Long selectedClubProfileId = adminBeforeIssue.availableMembers().stream()
                .filter(member -> "Selected Member".equals(member.memberDisplayName()))
                .findFirst()
                .orElseThrow()
                .clubProfileId();

        var created = clubDuesService.createCharge(
                clubId,
                "dues-owner-002",
                new CreateClubDuesChargeRequest(
                        "신규 유니폼비",
                        new BigDecimal("35000"),
                        null,
                        "선택 멤버만 발행",
                        "SELECTED_MEMBERS",
                        List.of(selectedClubProfileId)
                )
        );

        assertThat(created.createdCount()).isEqualTo(1);

        var adminDues = clubDuesService.getAdminDues(clubId, "dues-owner-002");
        assertThat(adminDues.totalChargeCount()).isEqualTo(1);
        assertThat(adminDues.totalInvoiceCount()).isEqualTo(1);
        assertThat(adminDues.charges()).singleElement().satisfies(charge -> {
            assertThat(charge.targetScope()).isEqualTo("SELECTED_MEMBERS");
            assertThat(charge.invoices()).singleElement().satisfies(invoice -> {
                assertThat(invoice.memberDisplayName()).isEqualTo("Selected Member");
            });
        });
    }

    @Test
    void nextPayableChargeIsNullWhenMemberHasNoPendingOrOverdueInvoices() {
        Long clubId = createEnabledClub("dues-owner-003", "Dues Owner 3", "Dues Club 3");
        addActiveMember(clubId, "dues-member-003", "Dues Member 3");

        clubDuesService.createCharge(
                clubId,
                "dues-owner-003",
                new CreateClubDuesChargeRequest(
                        "2026년 4월 정기회비",
                        new BigDecimal("5000"),
                        "2026-04-30T23:59",
                        "4월분",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        Long invoiceId = clubDuesService.getDues(clubId, "dues-member-003")
                .nextPayableCharge()
                .invoice()
                .invoiceId();

        clubDuesService.updatePaymentStatus(
                clubId,
                invoiceId,
                "dues-owner-003",
                new UpdateClubDuesPaymentStatusRequest("PAID", "완납 처리")
        );

        var memberDues = clubDuesService.getDues(clubId, "dues-member-003");

        assertThat(memberDues.pendingInvoiceCount()).isZero();
        assertThat(memberDues.overdueInvoiceCount()).isZero();
        assertThat(memberDues.nextPayableCharge()).isNull();
        assertThat(memberDues.openCharges()).isEmpty();
    }

    @Test
    void collectionRateExcludesWaivedInvoicesFromDenominator() {
        Long clubId = createEnabledClub("dues-owner-004", "Dues Owner 4", "Dues Club 4");
        addActiveMember(clubId, "dues-member-004", "Dues Member 4");

        clubDuesService.createCharge(
                clubId,
                "dues-owner-004",
                new CreateClubDuesChargeRequest(
                        "봄 시즌 회비",
                        new BigDecimal("5000"),
                        "2026-04-30T23:59",
                        "봄 시즌",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        var charge = clubDuesService.getAdminDues(clubId, "dues-owner-004").charges().getFirst();
        Long ownerInvoiceId = charge.invoices().stream()
                .filter(invoice -> "OWNER".equals(invoice.memberRoleCode()))
                .findFirst()
                .orElseThrow()
                .invoiceId();
        Long memberInvoiceId = charge.invoices().stream()
                .filter(invoice -> "MEMBER".equals(invoice.memberRoleCode()))
                .findFirst()
                .orElseThrow()
                .invoiceId();

        clubDuesService.updatePaymentStatus(
                clubId,
                ownerInvoiceId,
                "dues-owner-004",
                new UpdateClubDuesPaymentStatusRequest("PAID", "납부 완료")
        );
        clubDuesService.updatePaymentStatus(
                clubId,
                memberInvoiceId,
                "dues-owner-004",
                new UpdateClubDuesPaymentStatusRequest("WAIVED", "면제 처리")
        );

        var updatedAdminDues = clubDuesService.getAdminDues(clubId, "dues-owner-004");

        assertThat(updatedAdminDues.totalInvoiceCount()).isEqualTo(2);
        assertThat(updatedAdminDues.paidInvoiceCount()).isEqualTo(1);
        assertThat(updatedAdminDues.waivedInvoiceCount()).isEqualTo(1);
        assertThat(updatedAdminDues.collectionRate()).isEqualTo(100);
        assertThat(updatedAdminDues.charges()).singleElement().satisfies(updatedCharge ->
                assertThat(updatedCharge.collectionRate()).isEqualTo(100)
        );
    }

    @Test
    void adminDuesKeepsMemberNameAndRoleForInactiveBilledMember() {
        Long clubId = createEnabledClub("dues-owner-005", "Dues Owner 5", "Dues Club 5");
        Long memberProfileId = addActiveMember(clubId, "dues-member-005", "Dormant Member");

        clubDuesService.createCharge(
                clubId,
                "dues-owner-005",
                new CreateClubDuesChargeRequest(
                        "가입비",
                        new BigDecimal("30000"),
                        null,
                        "신규 가입비",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        ClubMember dormantMember = clubMemberRepository.findByClubIdAndProfileId(clubId, memberProfileId).orElseThrow();
        dormantMember.updateMembershipStatus("DORMANT");
        clubMemberRepository.save(dormantMember);

        var adminDues = clubDuesService.getAdminDues(clubId, "dues-owner-005");
        var dormantInvoice = adminDues.charges().stream()
                .flatMap(charge -> charge.invoices().stream())
                .filter(invoice -> "Dormant Member".equals(invoice.memberDisplayName()))
                .findFirst()
                .orElseThrow();

        assertThat(adminDues.activeMemberCount()).isEqualTo(1);
        assertThat(dormantInvoice.memberDisplayName()).isEqualTo("Dormant Member");
        assertThat(dormantInvoice.memberRoleCode()).isEqualTo("MEMBER");
    }

    @Test
    void pendingOnlyChargeCanBeDeleted() {
        Long clubId = createEnabledClub("dues-owner-006", "Dues Owner 6", "Dues Club 6");
        addActiveMember(clubId, "dues-member-006", "Dues Member 6");

        var created = clubDuesService.createCharge(
                clubId,
                "dues-owner-006",
                new CreateClubDuesChargeRequest(
                        "삭제 가능한 회비",
                        new BigDecimal("12000"),
                        null,
                        "아직 미처리",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        assertThat(clubDuesService.getAdminDues(clubId, "dues-owner-006").charges())
                .singleElement()
                .satisfies(charge -> assertThat(charge.canDelete()).isTrue());

        clubDuesService.deleteCharge(clubId, created.chargeId(), "dues-owner-006");

        var adminDues = clubDuesService.getAdminDues(clubId, "dues-owner-006");
        assertThat(adminDues.totalChargeCount()).isZero();
        assertThat(adminDues.totalInvoiceCount()).isZero();
        assertThat(duesChargeRepository.findByDuesChargeIdAndClubId(created.chargeId(), clubId)).isEmpty();
    }

    @Test
    void processedChargeCannotBeDeleted() {
        Long clubId = createEnabledClub("dues-owner-007", "Dues Owner 7", "Dues Club 7");
        addActiveMember(clubId, "dues-member-007", "Dues Member 7");

        var created = clubDuesService.createCharge(
                clubId,
                "dues-owner-007",
                new CreateClubDuesChargeRequest(
                        "삭제 불가 회비",
                        new BigDecimal("15000"),
                        null,
                        "처리됨",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        Long invoiceId = clubDuesService.getAdminDues(clubId, "dues-owner-007").charges().getFirst().invoices().getFirst().invoiceId();
        clubDuesService.updatePaymentStatus(
                clubId,
                invoiceId,
                "dues-owner-007",
                new UpdateClubDuesPaymentStatusRequest("PAID", "납부 완료")
        );

        var updatedCharge = clubDuesService.getAdminDues(clubId, "dues-owner-007").charges().getFirst();
        assertThat(updatedCharge.canDelete()).isFalse();

        assertThatThrownBy(() -> clubDuesService.deleteCharge(clubId, created.chargeId(), "dues-owner-007"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("아직 아무도 처리하지 않은 회비 항목만 삭제할 수 있습니다.");
    }

    private Long createEnabledClub(String ownerUserKey, String ownerDisplayName, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                ownerDisplayName,
                new CreateClubRequest(
                        clubName,
                        "회비 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("DUES"))
        );
        return clubId;
    }

    private Long addActiveMember(Long clubId, String userKey, String displayName) {
        Long profileId = profileUserService.resolveProfileId(userKey, displayName);
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
        return profileId;
    }
}
