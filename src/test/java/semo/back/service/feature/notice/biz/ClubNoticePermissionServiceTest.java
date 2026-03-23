package semo.back.service.feature.notice.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
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
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubNoticePermissionServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubNoticeService clubNoticeService;

    @Autowired
    private ClubNoticePermissionService clubNoticePermissionService;

    @Autowired
    private ClubNoticeRepository clubNoticeRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

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
        clubNoticeRepository.deleteAll();
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
    void adminNoticeSettingsRemovedAndRedirectedToRoleManagement() {
        String ownerUserKey = "notice-policy-owner-001";
        Long clubId = createNoticeClub(ownerUserKey, "Notice Policy Club");

        assertThatThrownBy(() -> clubNoticePermissionService.getAdminSettings(clubId, ownerUserKey))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("공지 권한 설정 페이지는 제거되었습니다.")
                .hasMessageContaining("직책관리");
    }

    @Test
    void memberCreateUpdateDeleteFollowAssignedPositionPermissions() {
        String ownerUserKey = "notice-policy-owner-002";
        String memberUserKey = "notice-policy-member-001";
        Long clubId = createNoticeClub(ownerUserKey, "Notice Permission Lab");
        ClubMember member = addActiveMember(clubId, memberUserKey, "Notice Member");

        assertThatThrownBy(() -> clubNoticeService.createNotice(
                clubId,
                memberUserKey,
                noticeRequest("멤버 공지", "기본 설정에서는 작성 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("공지 작성 권한");

        assignPositionPermissions(
                clubId,
                member,
                ClubPositionPermissionEvaluator.PERMISSION_NOTICE_CREATE
        );

        var created = clubNoticeService.createNotice(
                clubId,
                memberUserKey,
                noticeRequest("멤버 공지", "생성 허용 후 작성")
        );

        assertThat(created.noticeId()).isNotNull();

        assertThatThrownBy(() -> clubNoticeService.updateNotice(
                clubId,
                created.noticeId(),
                memberUserKey,
                noticeRequest("멤버 공지 수정", "수정은 아직 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("공지 수정 또는 삭제 권한");

        assertThatThrownBy(() -> clubNoticeService.deleteNotice(
                clubId,
                created.noticeId(),
                memberUserKey
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("공지 수정 또는 삭제 권한");

        var adminUpdated = clubNoticeService.updateNotice(
                clubId,
                created.noticeId(),
                ownerUserKey,
                noticeRequest("관리자가 수정한 공지", "관리자는 계속 수정 가능")
        );

        assertThat(adminUpdated.title()).isEqualTo("관리자가 수정한 공지");

        assignPositionPermissions(
                clubId,
                member,
                ClubPositionPermissionEvaluator.PERMISSION_NOTICE_CREATE,
                ClubPositionPermissionEvaluator.PERMISSION_NOTICE_UPDATE_SELF,
                ClubPositionPermissionEvaluator.PERMISSION_NOTICE_DELETE_SELF
        );

        var memberUpdated = clubNoticeService.updateNotice(
                clubId,
                created.noticeId(),
                memberUserKey,
                noticeRequest("멤버가 수정한 공지", "권한 허용 후 수정")
        );

        assertThat(memberUpdated.title()).isEqualTo("멤버가 수정한 공지");

        clubNoticeService.deleteNotice(clubId, created.noticeId(), memberUserKey);

        assertThat(clubNoticeRepository.findByNoticeIdAndClubIdAndDeletedFalse(created.noticeId(), clubId)).isEmpty();
    }

    private Long createNoticeClub(String ownerUserKey, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Notice Owner",
                new CreateClubRequest(
                        clubName,
                        "공지 권한 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );
        return clubId;
    }

    private ClubMember addActiveMember(Long clubId, String userKey, String displayName) {
        ProfileUser profileUser = profileUserRepository.save(ProfileUser.builder()
                .userKey(userKey)
                .displayName(displayName)
                .tagline(displayName + " 소개")
                .profileColor("#135bec")
                .build());

        return clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
    }

    private void assignPositionPermissions(Long clubId, ClubMember member, String... permissionKeys) {
        long nextCodeSuffix = clubPositionRepository.count() + 1;
        ClubPosition position = clubPositionRepository.save(ClubPosition.builder()
                .clubId(clubId)
                .positionCode("NOTICE_EDITOR_" + nextCodeSuffix)
                .displayName("공지 담당")
                .description("공지 작성 및 본인 글 관리")
                .iconName("campaign")
                .colorHex("#c76117")
                .active(true)
                .build());

        for (String permissionKey : permissionKeys) {
            clubPositionPermissionRepository.save(ClubPositionPermission.builder()
                    .clubPositionId(position.getClubPositionId())
                    .permissionKey(permissionKey)
                    .build());
        }

        clubMemberPositionRepository.save(ClubMemberPosition.builder()
                .clubMemberId(member.getClubMemberId())
                .clubPositionId(position.getClubPositionId())
                .assignedAt(LocalDateTime.now())
                .build());
    }

    private UpsertClubNoticeRequest noticeRequest(String title, String content) {
        return new UpsertClubNoticeRequest(
                title,
                content,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false
        );
    }
}
