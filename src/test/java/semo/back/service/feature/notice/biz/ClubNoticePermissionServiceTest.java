package semo.back.service.feature.notice.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.NoticePermissionPolicyRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.notice.vo.UpdateClubAdminNoticeSettingsRequest;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;

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
    private NoticePermissionPolicyRepository noticePermissionPolicyRepository;

    @Autowired
    private ClubNoticeRepository clubNoticeRepository;

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
        noticePermissionPolicyRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void adminNoticeSettingsExposeDefaultPolicy() {
        String ownerUserKey = "notice-policy-owner-001";
        Long clubId = createNoticeClub(ownerUserKey, "Notice Policy Club");

        var response = clubNoticePermissionService.getAdminSettings(clubId, ownerUserKey);

        assertThat(response.allowMemberCreate()).isFalse();
        assertThat(response.allowMemberUpdate()).isTrue();
        assertThat(response.allowMemberDelete()).isTrue();
    }

    @Test
    void memberCreateUpdateDeleteFollowConfiguredPolicyWhileAdminStillCanManage() {
        String ownerUserKey = "notice-policy-owner-002";
        String memberUserKey = "notice-policy-member-001";
        Long clubId = createNoticeClub(ownerUserKey, "Notice Permission Lab");
        addActiveMember(clubId, memberUserKey, "Notice Member");

        assertThatThrownBy(() -> clubNoticeService.createNotice(
                clubId,
                memberUserKey,
                noticeRequest("멤버 공지", "기본 설정에서는 작성 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("공지 작성 권한");

        clubNoticePermissionService.updateAdminSettings(
                clubId,
                ownerUserKey,
                new UpdateClubAdminNoticeSettingsRequest(true, false, false)
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

        clubNoticePermissionService.updateAdminSettings(
                clubId,
                ownerUserKey,
                new UpdateClubAdminNoticeSettingsRequest(true, true, true)
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
                new UpdateClubFeaturesRequest(List.of("NOTICE"))
        );
        return clubId;
    }

    private void addActiveMember(Long clubId, String userKey, String displayName) {
        ProfileUser profileUser = profileUserRepository.save(ProfileUser.builder()
                .userKey(userKey)
                .displayName(displayName)
                .tagline(displayName + " 소개")
                .profileColor("#135bec")
                .build());

        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
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
