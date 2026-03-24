package semo.back.service.feature.timeline.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static semo.back.service.feature.activity.biz.ClubActivityRecorder.STATUS_FAIL;
import static semo.back.service.feature.activity.biz.ClubActivityRecorder.STATUS_SUCCESS;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubTimelineServiceTest {

    @Autowired
    private ClubTimelineService clubTimelineService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubActivityLogRepository clubActivityLogRepository;

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
        clubActivityLogRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void timelineReturnsClubActivityFeedWithCursorPaging() {
        String ownerUserKey = "timeline-owner-001";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Timeline Owner",
                new CreateClubRequest(
                        "Timeline Club",
                        "타임라인 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("TIMELINE"))
        );
        clubActivityLogRepository.deleteAll();
        Long ownerClubProfileId = clubProfileRepository.findAll().stream()
                .map(ClubProfile::getClubProfileId)
                .findFirst()
                .orElseThrow();

        ClubActivityLog oldest = saveActivity(
                clubId,
                ownerClubProfileId,
                "민지",
                "출석관리",
                "2026.03.24 출석을 완료했습니다.",
                STATUS_SUCCESS,
                null
        );
        ClubActivityLog middle = saveActivity(
                clubId,
                ownerClubProfileId,
                "도윤",
                "공지관리",
                "공지 '정기 모임 안내'를 생성했습니다.",
                STATUS_SUCCESS,
                null
        );
        ClubActivityLog latest = saveActivity(
                clubId,
                ownerClubProfileId,
                "서준",
                "멤버관리",
                "신규 멤버 승인에 실패했습니다.",
                STATUS_FAIL,
                "가입 대기 상태인 멤버만 승인할 수 있습니다."
        );
        saveActivity(
                clubId,
                ownerClubProfileId + 999,
                "다른 멤버",
                "투표관리",
                "투표 '회식 일정'을 생성했습니다.",
                STATUS_SUCCESS,
                null
        );

        var firstPage = clubTimelineService.getTimeline(clubId, ownerUserKey, null, null, 2);

        assertThat(firstPage.admin()).isTrue();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.entries()).hasSize(2);
        assertThat(firstPage.entries().get(0).activityId()).isEqualTo(latest.getClubActivityLogId());
        assertThat(firstPage.entries().get(0).actorDisplayName()).isEqualTo("서준");
        assertThat(firstPage.entries().get(0).actorAvatarLabel()).isEqualTo("서");
        assertThat(firstPage.entries().get(0).subject()).isEqualTo("멤버관리");
        assertThat(firstPage.entries().get(0).detail()).isEqualTo("신규 멤버 승인에 실패했습니다.");
        assertThat(firstPage.entries().get(0).status()).isEqualTo(STATUS_FAIL);
        assertThat(firstPage.entries().get(0).createdAt()).isNotBlank();
        assertThat(firstPage.entries().get(0).createdAtLabel()).isNotBlank();
        assertThat(firstPage.entries().get(1).activityId()).isEqualTo(middle.getClubActivityLogId());

        var secondPage = clubTimelineService.getTimeline(
                clubId,
                ownerUserKey,
                firstPage.nextCursorCreatedAt(),
                firstPage.nextCursorActivityId(),
                2
        );

        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.activityId()).isEqualTo(oldest.getClubActivityLogId());
            assertThat(entry.actorDisplayName()).isEqualTo("민지");
            assertThat(entry.subject()).isEqualTo("출석관리");
            assertThat(entry.status()).isEqualTo(STATUS_SUCCESS);
        });
    }

    private ClubActivityLog saveActivity(
            Long clubId,
            Long actorClubProfileId,
            String actorDisplayName,
            String subject,
            String detailText,
            String statusCode,
            String errorMessage
    ) {
        return clubActivityLogRepository.save(ClubActivityLog.builder()
                .clubId(clubId)
                .actorClubProfileId(actorClubProfileId)
                .actorDisplayName(actorDisplayName)
                .subject(subject)
                .detailText(detailText)
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .build());
    }
}
