package semo.back.service.feature.club.act;

import auth.common.core.context.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.vo.ClubProfileResponse;
import semo.back.service.feature.club.vo.ClubCreateResponse;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.club.vo.MyClubSummaryResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ClubControllerTest {

    @Autowired
    private ClubController clubController;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void createClubCreatesClubForAuthenticatedUser() {
        UserContext userContext = UserContext.builder()
                .userKey("user-club-002")
                .userName("Admin User")
                .role("USER")
                .build();

        ResponseDataDTO<ClubCreateResponse> response = clubController.createClub(
                new CreateClubRequest(
                        "Semo Running",
                        "러닝 크루를 위한 소개입니다.",
                        "RUNNING",
                        "PRIVATE",
                        "OPEN",
                        null
                ),
                userContext
        );

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().name()).isEqualTo("Semo Running");
        assertThat(response.getData().visibilityStatus()).isEqualTo("PRIVATE");
        assertThat(response.getData().membershipPolicy()).isEqualTo("OPEN");
        assertThat(clubRepository.count()).isOne();
        assertThat(clubMemberRepository.count()).isOne();
    }

    @Test
    void createClubRejectsAnonymousRequest() {
        assertThatThrownBy(() -> clubController.createClub(
                new CreateClubRequest("Semo Club", null, "OTHER", "PUBLIC", "APPROVAL", null),
                UserContext.builder().build()
        ))
                .isInstanceOf(SemoException.UnauthorizedException.class)
                .hasMessage("Login required");
    }

    @Test
    void getMyClubsReturnsCreatedClubForAuthenticatedUser() {
        UserContext userContext = UserContext.builder()
                .userKey("user-club-004")
                .userName("Owner User")
                .role("USER")
                .build();

        clubController.createClub(
                new CreateClubRequest("Semo Cycle", "사이클 모임입니다.", "CYCLING", "PUBLIC", "OPEN", null),
                userContext
        );

        ResponseDataDTO<List<MyClubSummaryResponse>> response = clubController.getMyClubs(userContext);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().name()).isEqualTo("Semo Cycle");
    }

    @Test
    void getMyClubReturnsClubForAuthenticatedMember() {
        UserContext userContext = UserContext.builder()
                .userKey("user-club-006")
                .userName("Owner User")
                .role("USER")
                .build();

        ResponseDataDTO<ClubCreateResponse> created = clubController.createClub(
                new CreateClubRequest("Semo Trail", "트레일 러닝", "RUNNING", "PUBLIC", "APPROVAL", null),
                userContext
        );

        ResponseDataDTO<MyClubSummaryResponse> response = clubController.getMyClub(
                created.getData().clubId(),
                userContext
        );

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().clubId()).isEqualTo(created.getData().clubId());
        assertThat(response.getData().name()).isEqualTo("Semo Trail");
    }

    @Test
    void getClubProfileReturnsAppAndClubProfileSections() {
        UserContext userContext = UserContext.builder()
                .userKey("user-club-008")
                .userName("Club Profile User")
                .role("USER")
                .build();

        ResponseDataDTO<ClubCreateResponse> created = clubController.createClub(
                new CreateClubRequest("Semo Profile Club", "프로필 분리 테스트", "OTHER", "PUBLIC", "APPROVAL", null),
                userContext
        );

        ResponseDataDTO<ClubProfileResponse> response = clubController.getClubProfile(
                created.getData().clubId(),
                userContext
        );

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().appProfile().displayName()).isEqualTo("Club Profile User");
        assertThat(response.getData().clubProfile().displayName()).isEqualTo("Club Profile User");
        assertThat(response.getData().clubProfile().roleCode()).isEqualTo("OWNER");
    }
}
