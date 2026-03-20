package semo.back.service.feature.poll.biz;

import org.junit.jupiter.api.AfterEach;
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
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.PollPermissionPolicyRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.poll.vo.UpdateClubAdminPollSettingsRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubPollPermissionServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubScheduleService clubScheduleService;

    @Autowired
    private ClubPollService clubPollService;

    @Autowired
    private ClubPollPermissionService clubPollPermissionService;

    @Autowired
    private PollPermissionPolicyRepository pollPermissionPolicyRepository;

    @Autowired
    private ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;

    @Autowired
    private ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;

    @Autowired
    private ClubScheduleVoteRepository clubScheduleVoteRepository;

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
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        pollPermissionPolicyRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @AfterEach
    void tearDown() {
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        pollPermissionPolicyRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void adminPollSettingsExposeDefaultPolicyAndHomeCreateFallback() {
        String ownerUserKey = "poll-policy-owner-001";
        String memberUserKey = "poll-policy-member-001";
        Long clubId = createPollClub(ownerUserKey, "Poll Policy Club");
        addActiveMember(clubId, memberUserKey, "Poll Member");

        var settings = clubPollPermissionService.getAdminSettings(clubId, ownerUserKey);
        var home = clubPollService.getPollHome(clubId, memberUserKey, null);

        assertThat(settings.allowMemberCreate()).isFalse();
        assertThat(settings.allowMemberUpdate()).isTrue();
        assertThat(settings.allowMemberDelete()).isTrue();
        assertThat(home.canCreate()).isFalse();
    }

    @Test
    void memberPollCreateUpdateDeleteFollowConfiguredPolicyWhileAdminStillCanManage() {
        String ownerUserKey = "poll-policy-owner-002";
        String memberUserKey = "poll-policy-member-002";
        Long clubId = createPollClub(ownerUserKey, "Poll Permission Lab");
        addActiveMember(clubId, memberUserKey, "Poll Member");

        assertThatThrownBy(() -> clubScheduleService.createScheduleVote(
                clubId,
                memberUserKey,
                voteRequest("멤버 투표", List.of("찬성", "반대"))
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("투표 작성 권한");

        clubPollPermissionService.updateAdminSettings(
                clubId,
                ownerUserKey,
                new UpdateClubAdminPollSettingsRequest(true, false, false)
        );

        var created = clubScheduleService.createScheduleVote(
                clubId,
                memberUserKey,
                voteRequest("멤버 투표", List.of("1안", "2안"))
        );

        assertThat(created.voteId()).isNotNull();

        assertThatThrownBy(() -> clubScheduleService.updateScheduleVote(
                clubId,
                created.voteId(),
                memberUserKey,
                voteRequest("수정 불가 투표", List.of("A", "B"))
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("투표 수정 권한");

        assertThatThrownBy(() -> clubScheduleService.deleteScheduleVote(
                clubId,
                created.voteId(),
                memberUserKey
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("투표 삭제 권한");

        var adminUpdated = clubScheduleService.updateScheduleVote(
                clubId,
                created.voteId(),
                ownerUserKey,
                voteRequest("관리자가 수정한 투표", List.of("점심", "저녁"))
        );

        assertThat(adminUpdated.title()).isEqualTo("관리자가 수정한 투표");

        clubPollPermissionService.updateAdminSettings(
                clubId,
                ownerUserKey,
                new UpdateClubAdminPollSettingsRequest(true, true, true)
        );

        var memberUpdated = clubScheduleService.updateScheduleVote(
                clubId,
                created.voteId(),
                memberUserKey,
                voteRequest("멤버가 수정한 투표", List.of("서울숲", "건대", "잠실"))
        );

        assertThat(memberUpdated.title()).isEqualTo("멤버가 수정한 투표");

        clubScheduleService.deleteScheduleVote(clubId, created.voteId(), memberUserKey);

        assertThat(clubScheduleVoteRepository.findByVoteIdAndClubId(created.voteId(), clubId)).isEmpty();
    }

    private Long createPollClub(String ownerUserKey, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Poll Owner",
                new CreateClubRequest(
                        clubName,
                        "투표 권한 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("POLL"))
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

    private UpsertScheduleVoteRequest voteRequest(String title, List<String> optionLabels) {
        return new UpsertScheduleVoteRequest(
                title,
                "2030-05-01",
                "2030-05-03",
                null,
                null,
                optionLabels,
                false,
                false,
                false
        );
    }
}
