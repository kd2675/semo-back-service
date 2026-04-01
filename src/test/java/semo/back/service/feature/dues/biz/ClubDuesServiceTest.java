package semo.back.service.feature.dues.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.repository.ClubDuesInvoiceRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.dues.vo.IssueClubDuesInvoicesRequest;
import semo.back.service.feature.dues.vo.UpdateClubDuesPaymentStatusRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    private ClubDuesInvoiceRepository clubDuesInvoiceRepository;

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
        clubDuesInvoiceRepository.deleteAll();
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
    void memberCanSeeIssuedDuesAndAdminCanMarkItPaid() {
        Long clubId = clubService.createClub(
                "dues-owner-001",
                "Dues Owner",
                new CreateClubRequest(
                        "Dues Club",
                        "회비 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                "dues-owner-001",
                new UpdateClubFeaturesRequest(List.of("DUES"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("dues-member-001", "Dues Member");
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());

        var issued = clubDuesService.issueMonthlyInvoices(
                clubId,
                "dues-owner-001",
                new IssueClubDuesInvoicesRequest(
                        2026,
                        4,
                        new BigDecimal("5000"),
                        "2026-04-30T23:59",
                        "4월 회비",
                        null
                )
        );

        assertThat(issued.createdCount()).isEqualTo(2);

        var memberDues = clubDuesService.getDues(clubId, "dues-member-001");
        assertThat(memberDues.myInvoices()).hasSize(1);
        assertThat(memberDues.myInvoices().get(0).paymentStatus()).isEqualTo("PENDING");

        Long invoiceId = memberDues.myInvoices().get(0).invoiceId();
        var updated = clubDuesService.updatePaymentStatus(
                clubId,
                invoiceId,
                "dues-owner-001",
                new UpdateClubDuesPaymentStatusRequest("PAID", "현장 수납")
        );

        assertThat(updated.paymentStatus()).isEqualTo("PAID");
        assertThat(clubDuesInvoiceRepository.findByClubDuesInvoiceIdAndClubId(invoiceId, clubId))
                .get()
                .extracting(invoice -> invoice.getPaymentStatus())
                .isEqualTo("PAID");
    }

    @Test
    void nextInvoiceIsNullWhenMemberHasNoPendingOrOverdueInvoices() {
        Long clubId = clubService.createClub(
                "dues-owner-002",
                "Dues Owner 2",
                new CreateClubRequest(
                        "Dues Club 2",
                        "회비 next invoice 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                "dues-owner-002",
                new UpdateClubFeaturesRequest(List.of("DUES"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("dues-member-002", "Dues Member 2");
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());

        clubDuesService.issueMonthlyInvoices(
                clubId,
                "dues-owner-002",
                new IssueClubDuesInvoicesRequest(
                        2026,
                        4,
                        new BigDecimal("5000"),
                        "2026-04-30T23:59",
                        "4월 회비",
                        null
                )
        );

        Long invoiceId = clubDuesService.getDues(clubId, "dues-member-002")
                .myInvoices()
                .get(0)
                .invoiceId();

        clubDuesService.updatePaymentStatus(
                clubId,
                invoiceId,
                "dues-owner-002",
                new UpdateClubDuesPaymentStatusRequest("PAID", "완납 처리")
        );

        var memberDues = clubDuesService.getDues(clubId, "dues-member-002");

        assertThat(memberDues.pendingInvoiceCount()).isZero();
        assertThat(memberDues.overdueInvoiceCount()).isZero();
        assertThat(memberDues.nextInvoice()).isNull();
    }

    @Test
    void collectionRateExcludesWaivedInvoicesFromDenominator() {
        Long clubId = clubService.createClub(
                "dues-owner-003",
                "Dues Owner 3",
                new CreateClubRequest(
                        "Dues Club 3",
                        "회비 수금률 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                "dues-owner-003",
                new UpdateClubFeaturesRequest(List.of("DUES"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("dues-member-003", "Dues Member 3");
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());

        clubDuesService.issueMonthlyInvoices(
                clubId,
                "dues-owner-003",
                new IssueClubDuesInvoicesRequest(
                        2026,
                        4,
                        new BigDecimal("5000"),
                        "2026-04-30T23:59",
                        "4월 회비",
                        null
                )
        );

        var adminDues = clubDuesService.getAdminDues(clubId, "dues-owner-003");
        Long ownerInvoiceId = adminDues.invoices().stream()
                .filter(invoice -> "OWNER".equals(invoice.memberRoleCode()))
                .findFirst()
                .orElseThrow()
                .invoiceId();
        Long memberInvoiceId = adminDues.invoices().stream()
                .filter(invoice -> "MEMBER".equals(invoice.memberRoleCode()))
                .findFirst()
                .orElseThrow()
                .invoiceId();

        clubDuesService.updatePaymentStatus(
                clubId,
                ownerInvoiceId,
                "dues-owner-003",
                new UpdateClubDuesPaymentStatusRequest("PAID", "납부 완료")
        );
        clubDuesService.updatePaymentStatus(
                clubId,
                memberInvoiceId,
                "dues-owner-003",
                new UpdateClubDuesPaymentStatusRequest("WAIVED", "면제 처리")
        );

        var updatedAdminDues = clubDuesService.getAdminDues(clubId, "dues-owner-003");

        assertThat(updatedAdminDues.totalInvoiceCount()).isEqualTo(2);
        assertThat(updatedAdminDues.paidInvoiceCount()).isEqualTo(1);
        assertThat(updatedAdminDues.waivedInvoiceCount()).isEqualTo(1);
        assertThat(updatedAdminDues.collectionRate()).isEqualTo(100);
    }

    @Test
    void adminDuesKeepsMemberNameAndRoleForInactiveBilledMember() {
        Long clubId = clubService.createClub(
                "dues-owner-004",
                "Dues Owner 4",
                new CreateClubRequest(
                        "Dues Club 4",
                        "비활성 멤버 회비 감사 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                "dues-owner-004",
                new UpdateClubFeaturesRequest(List.of("DUES"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("dues-member-004", "Dormant Member");
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());

        clubDuesService.issueMonthlyInvoices(
                clubId,
                "dues-owner-004",
                new IssueClubDuesInvoicesRequest(
                        2026,
                        4,
                        new BigDecimal("5000"),
                        "2026-04-30T23:59",
                        "4월 회비",
                        null
                )
        );

        ClubMember dormantMember = clubMemberRepository.findByClubIdAndProfileId(clubId, memberProfileId).orElseThrow();
        dormantMember.updateMembershipStatus("DORMANT");
        clubMemberRepository.save(dormantMember);

        var adminDues = clubDuesService.getAdminDues(clubId, "dues-owner-004");
        var dormantInvoice = adminDues.invoices().stream()
                .filter(invoice -> "Dormant Member".equals(invoice.memberDisplayName()))
                .findFirst()
                .orElseThrow();

        assertThat(adminDues.activeMemberCount()).isEqualTo(1);
        assertThat(dormantInvoice.memberDisplayName()).isEqualTo("Dormant Member");
        assertThat(dormantInvoice.memberRoleCode()).isEqualTo("MEMBER");
    }
}
