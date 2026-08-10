package semo.back.service.feature.feedback.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
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
import semo.back.service.feature.feedback.vo.CreateClubFeedbackRequest;
import semo.back.service.feature.feedback.vo.UpdateClubAdminFeedbackRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubFeedbackServiceTest {

    @Autowired
    private ClubFeedbackService clubFeedbackService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private ClubFeedbackRepository clubFeedbackRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubNotificationRepository clubNotificationRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        clubNotificationRepository.deleteAll();
        clubFeedbackRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
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
    void feedbackHomeShowsOwnPrivateAndPublicItemsWithAnonymousMasking() {
        String ownerUserKey = "feedback-owner-001";
        Long clubId = createFeedbackEnabledClub(ownerUserKey, "Feedback Club");
        ClubProfile ownerProfile = clubProfileRepository.findAll().getFirst();
        ClubProfile otherMemberProfile = createActiveMember(clubId, "feedback-member-001", "민서");

        clubFeedbackRepository.saveAll(List.of(
                semo.back.service.database.pub.entity.ClubFeedback.builder()
                        .clubId(clubId)
                        .submitterClubProfileId(otherMemberProfile.getClubProfileId())
                        .feedbackType("SUGGESTION")
                        .visibilityScope("PUBLIC")
                        .statusCode("ANSWERED")
                        .anonymous(true)
                        .title("동선 개선")
                        .content("입장 동선이 조금 복잡합니다.")
                        .adminAnswer("다음 모임부터 안내선을 추가하겠습니다.")
                        .answeredByClubProfileId(ownerProfile.getClubProfileId())
                        .answeredAt(LocalDateTime.of(2026, 4, 2, 12, 0))
                        .deleted(false)
                        .build(),
                semo.back.service.database.pub.entity.ClubFeedback.builder()
                        .clubId(clubId)
                        .submitterClubProfileId(ownerProfile.getClubProfileId())
                        .feedbackType("INCONVENIENCE")
                        .visibilityScope("PRIVATE")
                        .statusCode("RECEIVED")
                        .anonymous(false)
                        .title("사물함 문의")
                        .content("사물함 사용 규칙이 궁금합니다.")
                        .adminAnswer(null)
                        .answeredByClubProfileId(null)
                        .answeredAt(null)
                        .deleted(false)
                        .build()
        ));

        var response = clubFeedbackService.getFeedbackHome(clubId, ownerUserKey);

        assertThat(response.featureEnabled()).isTrue();
        assertThat(response.totalVisibleCount()).isEqualTo(2);
        assertThat(response.mySubmissionCount()).isEqualTo(1);
        assertThat(response.publicVisibleCount()).isEqualTo(1);
        assertThat(response.answeredCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("동선 개선");
            assertThat(item.anonymous()).isTrue();
            assertThat(item.authorDisplayName()).isEqualTo("익명");
            assertThat(item.answered()).isTrue();
        });
        assertThat(response.items()).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("사물함 문의");
            assertThat(item.mine()).isTrue();
            assertThat(item.authorDisplayName()).isEqualTo(ownerProfile.getDisplayName());
        });
    }

    @Test
    void privateFeedbackDetailRejectsOtherMembers() {
        String ownerUserKey = "feedback-owner-002";
        Long clubId = createFeedbackEnabledClub(ownerUserKey, "Feedback Private Club");
        ClubProfile ownerProfile = clubProfileRepository.findAll().getFirst();
        createActiveMember(clubId, "feedback-member-002", "서윤");

        Long feedbackId = clubFeedbackRepository.save(semo.back.service.database.pub.entity.ClubFeedback.builder()
                .clubId(clubId)
                .submitterClubProfileId(ownerProfile.getClubProfileId())
                .feedbackType("SUGGESTION")
                .visibilityScope("PRIVATE")
                .statusCode("RECEIVED")
                .anonymous(false)
                .title("내부 메모")
                .content("운영진에게만 전달합니다.")
                .adminAnswer(null)
                .answeredByClubProfileId(null)
                .answeredAt(null)
                .deleted(false)
                .build()).getFeedbackId();

        assertThatThrownBy(() -> clubFeedbackService.getFeedbackDetail(clubId, feedbackId, "feedback-member-002"))
                .isInstanceOf(SemoException.ForbiddenException.class);
    }

    @Test
    void adminUpdateKeepsPrivateFeedbackPrivateAndMasksAnonymousAuthor() {
        String ownerUserKey = "feedback-owner-003";
        Long clubId = createFeedbackEnabledClub(ownerUserKey, "Feedback Admin Club");
        createActiveMember(clubId, "feedback-member-003", "지후");

        Long feedbackId = clubFeedbackService.createFeedback(
                clubId,
                "feedback-member-003",
                new CreateClubFeedbackRequest(
                        "IMPROVEMENT_REQUEST",
                        "체육관 조명 개선",
                        "조명이 조금 어둡습니다.",
                        true
                )
        ).feedbackId();

        var updated = clubFeedbackService.updateAdminFeedback(
                clubId,
                feedbackId,
                ownerUserKey,
                new UpdateClubAdminFeedbackRequest(
                        "INCONVENIENCE",
                        "ANSWERED",
                        "PRIVATE",
                        "조명 교체 일정을 잡겠습니다."
                )
        );

        assertThat(updated.feedbackType()).isEqualTo("INCONVENIENCE");
        assertThat(updated.statusCode()).isEqualTo("ANSWERED");
        assertThat(updated.visibilityScope()).isEqualTo("PRIVATE");
        assertThat(updated.adminAnswer()).isEqualTo("조명 교체 일정을 잡겠습니다.");
        assertThat(updated.anonymous()).isTrue();
        assertThat(updated.authorDisplayName()).isEqualTo("익명");
        assertThat(updated.answeredByDisplayName()).isNotBlank();

        assertThat(clubNotificationRepository.findAll())
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getNotificationType()).isEqualTo("FEEDBACK_STATUS");
                    assertThat(notification.getTitle()).isEqualTo("피드백 답변이 도착했습니다");
                    assertThat(notification.getResourceId()).isEqualTo(feedbackId);
                    assertThat(notification.getTargetPath()).isEqualTo("/clubs/" + clubId + "/more/feedback");
                });

        var memberView = clubFeedbackService.getFeedbackDetail(clubId, feedbackId, "feedback-member-003");
        assertThat(memberView.authorDisplayName()).isEqualTo("익명");
        assertThat(memberView.adminAnswer()).isEqualTo("조명 교체 일정을 잡겠습니다.");

        assertThatThrownBy(() -> clubFeedbackService.updateAdminFeedback(
                clubId,
                feedbackId,
                ownerUserKey,
                new UpdateClubAdminFeedbackRequest(
                        "INCONVENIENCE",
                        "ANSWERED",
                        "PUBLIC",
                        "공개 전환을 시도합니다."
                )
        ))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("제출자의 동의");
    }

    private Long createFeedbackEnabledClub(String ownerUserKey, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Feedback Owner",
                new CreateClubRequest(
                        clubName,
                        "피드백 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("FEEDBACK"))
        );
        return clubId;
    }

    private ClubProfile createActiveMember(Long clubId, String userKey, String displayName) {
        Long profileId = profileUserService.resolveProfileId(userKey, displayName);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 2, 10, 0);
        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(joinedAt)
                .lastActivityAt(joinedAt.plusHours(1))
                .build());
        return clubProfileRepository.save(ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName(displayName)
                .tagline(null)
                .introText(null)
                .avatarFileName(null)
                .build());
    }
}
