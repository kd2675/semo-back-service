package semo.back.service.feature.growth.biz;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubGrowthCore;
import semo.back.service.database.pub.repository.ClubGrowthCoreRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClubGrowthCoreProjectionServiceTest {
    @Autowired
    private ClubGrowthCoreProjectionService clubGrowthCoreProjectionService;

    @Autowired
    private ClubGrowthCoreQueryService clubGrowthCoreQueryService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubGrowthCoreRepository clubGrowthCoreRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @AfterEach
    void tearDown() {
        clubGrowthCoreRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void refresh_newClub_projectsCanonicalEmptyEvidence() {
        Long clubId = clubService.createClub(
                "growth-owner-001",
                "Growth Owner",
                new CreateClubRequest("Growth Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();

        clubGrowthCoreProjectionService.refresh(clubId);

        assertThat(clubGrowthCoreQueryService.get(clubId))
                .extracting(
                        response -> response.tierCode(),
                        response -> response.togetherProgress(),
                        response -> response.operationsProgress(),
                        response -> response.continuityProgress(),
                        response -> response.lastProjectedAt() != null
                )
                .containsExactly("RAW", 0, 0, 0, true);
    }

    @Test
    void refresh_inactiveClub_marksProjectionComplete() {
        Club club = clubRepository.save(Club.builder()
                .name("Inactive Growth Club")
                .visibilityStatus("PRIVATE")
                .membershipPolicy("APPROVAL")
                .regionScope("NATIONWIDE")
                .regionLabel("전국")
                .active(false)
                .build());
        clubGrowthCoreRepository.save(ClubGrowthCore.initial(
                club.getClubId(),
                ClubGrowthCorePolicy.POLICY_VERSION
        ));

        clubGrowthCoreProjectionService.refresh(club.getClubId());

        assertThat(clubGrowthCoreRepository.findById(club.getClubId()))
                .get()
                .extracting(core -> core.getLastProjectedAt() != null)
                .isEqualTo(true);
    }
}
