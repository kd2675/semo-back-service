package semo.back.service.feature.memberdirectory.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.MemberDirectorySettingRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.memberdirectory.vo.UpdateClubAdminMemberDirectorySettingsRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubMemberDirectoryServiceTest {

    @Autowired
    private ClubMemberDirectoryService clubMemberDirectoryService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private ClubActivityLogRepository clubActivityLogRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @Autowired
    private MemberDirectorySettingRepository memberDirectorySettingRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        clubActivityLogRepository.deleteAll();
        memberDirectorySettingRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        featureCatalogRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void memberDirectoryReturnsOtherMembersWithPositionsAndRecentActivity() {
        String ownerUserKey = "directory-owner-001";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Directory Owner",
                new CreateClubRequest(
                        "Directory Club",
                        "회원 디렉터리 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("MEMBER_DIRECTORY", "ROLE_MANAGEMENT"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("directory-member-001", "한결");
        LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 2, 10, 0);
        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("ADMIN")
                .membershipStatus("ACTIVE")
                .joinedAt(joinedAt)
                .lastActivityAt(joinedAt.plusHours(3))
                .build());
        ClubProfile memberProfile = clubProfileRepository.save(ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName("한결")
                .tagline("주말마다 나오는 운영진")
                .introText(null)
                .avatarFileName(null)
                .build());

        ClubPosition position = clubPositionRepository.save(ClubPosition.builder()
                .clubId(clubId)
                .positionCode("MC")
                .displayName("총무")
                .description("운영 담당")
                .iconName("badge")
                .colorHex("#ff7a00")
                .active(true)
                .createdByClubProfileId(clubProfileRepository.findAll().getFirst().getClubProfileId())
                .build());
        clubMemberPositionRepository.save(ClubMemberPosition.builder()
                .clubMemberId(member.getClubMemberId())
                .clubPositionId(position.getClubPositionId())
                .assignedByClubProfileId(clubProfileRepository.findAll().getFirst().getClubProfileId())
                .assignedAt(joinedAt.plusHours(1))
                .build());
        clubActivityLogRepository.save(ClubActivityLog.builder()
                .clubId(clubId)
                .actorClubMemberId(member.getClubMemberId())
                .actorClubProfileId(memberProfile.getClubProfileId())
                .actorDisplayName("한결")
                .subject("공지관리")
                .detailText("공지 '주말 모임'을 생성했습니다.")
                .statusCode("SUCCESS")
                .errorMessage(null)
                .build());

        var defaultResponse = clubMemberDirectoryService.getMemberDirectory(clubId, ownerUserKey);
        assertThat(defaultResponse.settings().showRecentActivity()).isFalse();
        assertThat(defaultResponse.members()).allSatisfy(memberResponse ->
                assertThat(memberResponse.recentActivity()).isNull()
        );

        clubMemberDirectoryService.updateAdminMemberDirectory(
                clubId,
                ownerUserKey,
                new UpdateClubAdminMemberDirectorySettingsRequest(true, true, true)
        );
        var response = clubMemberDirectoryService.getMemberDirectory(clubId, ownerUserKey);

        assertThat(response.featureEnabled()).isTrue();
        assertThat(response.totalMemberCount()).isEqualTo(2);
        assertThat(response.settings().showPositions()).isTrue();
        assertThat(response.settings().showTagline()).isTrue();
        assertThat(response.settings().showRecentActivity()).isTrue();
        assertThat(response.members()).hasSize(2);
        assertThat(response.members()).anySatisfy(memberResponse -> {
            assertThat(memberResponse.displayName()).isEqualTo("한결");
            assertThat(memberResponse.roleCode()).isEqualTo("ADMIN");
            assertThat(memberResponse.roleLabel()).isEqualTo("관리자");
            assertThat(memberResponse.positions()).extracting("displayName").containsExactly("총무");
            assertThat(memberResponse.tagline()).isEqualTo("주말마다 나오는 운영진");
            assertThat(memberResponse.recentActivity()).isNotNull();
            assertThat(memberResponse.recentActivity().subject()).isEqualTo("공지관리");
            assertThat(memberResponse.recentActivity().detail()).isEqualTo("공지를 관리했습니다.");
        });
    }

    @Test
    void updateAdminMemberDirectoryPersistsVisibilitySettings() {
        String ownerUserKey = "directory-owner-002";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Directory Owner",
                new CreateClubRequest(
                        "Directory Settings Club",
                        "회원 디렉터리 설정 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("MEMBER_DIRECTORY"))
        );

        var response = clubMemberDirectoryService.updateAdminMemberDirectory(
                clubId,
                ownerUserKey,
                new UpdateClubAdminMemberDirectorySettingsRequest(false, true, false)
        );

        assertThat(response.featureEnabled()).isTrue();
        assertThat(response.totalMemberCount()).isEqualTo(1);
        assertThat(response.settings()).isEqualTo(new semo.back.service.feature.memberdirectory.vo.MemberDirectorySettingsResponse(
                false,
                true,
                false
        ));
        assertThat(memberDirectorySettingRepository.findByClubId(clubId)).isPresent();
        assertThat(memberDirectorySettingRepository.findByClubId(clubId).orElseThrow().isShowPositions()).isFalse();
        assertThat(memberDirectorySettingRepository.findByClubId(clubId).orElseThrow().isShowTagline()).isTrue();
        assertThat(memberDirectorySettingRepository.findByClubId(clubId).orElseThrow().isShowRecentActivity()).isFalse();
    }

    @Test
    void memberDirectoryMasksFieldsDisabledByAdminSettings() {
        String ownerUserKey = "directory-owner-003";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Directory Owner",
                new CreateClubRequest(
                        "Masked Directory Club",
                        "회원 디렉터리 마스킹 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("MEMBER_DIRECTORY"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("directory-member-003", "도연");
        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 4, 2, 9, 0))
                .build());
        ClubProfile memberProfile = clubProfileRepository.save(ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName("도연")
                .tagline("부산 원정 자주 갑니다")
                .introText(null)
                .avatarFileName(null)
                .build());
        clubActivityLogRepository.save(ClubActivityLog.builder()
                .clubId(clubId)
                .actorClubMemberId(member.getClubMemberId())
                .actorClubProfileId(memberProfile.getClubProfileId())
                .actorDisplayName("도연")
                .subject("출석관리")
                .detailText("출석을 완료했습니다.")
                .statusCode("SUCCESS")
                .errorMessage(null)
                .build());

        clubMemberDirectoryService.updateAdminMemberDirectory(
                clubId,
                ownerUserKey,
                new UpdateClubAdminMemberDirectorySettingsRequest(false, false, false)
        );

        var response = clubMemberDirectoryService.getMemberDirectory(clubId, ownerUserKey);

        assertThat(response.members()).hasSize(2);
        assertThat(response.members()).anySatisfy(memberResponse -> {
            assertThat(memberResponse.displayName()).isEqualTo("도연");
            assertThat(memberResponse.roleCode()).isEmpty();
            assertThat(memberResponse.roleLabel()).isEmpty();
            assertThat(memberResponse.positions()).isEmpty();
            assertThat(memberResponse.tagline()).isNull();
            assertThat(memberResponse.recentActivity()).isNull();
        });
    }

    @Test
    void memberDirectoryRequiresFeatureToBeEnabled() {
        String ownerUserKey = "directory-owner-004";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Directory Owner",
                new CreateClubRequest(
                        "Disabled Directory Club",
                        "회원 디렉터리 비활성 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        assertThatThrownBy(() -> clubMemberDirectoryService.getMemberDirectory(clubId, ownerUserKey))
                .isInstanceOf(semo.back.service.common.exception.SemoException.ForbiddenException.class)
                .hasMessageContaining("회원 디렉터리 기능이 활성화되지 않았습니다.");
    }
}
