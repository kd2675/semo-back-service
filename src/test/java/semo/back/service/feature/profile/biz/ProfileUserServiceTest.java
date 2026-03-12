package semo.back.service.feature.profile.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.profile.vo.ProfileSummaryResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProfileUserServiceTest {

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        profileUserRepository.deleteAll();
    }

    @Test
    void initializeProfileCreatesProfileOnlyOnce() {
        ProfileSummaryResponse first = profileUserService.initializeProfile("user-001", "Semo User");
        ProfileSummaryResponse second = profileUserService.initializeProfile("user-001", "Changed Name");

        assertThat(profileUserRepository.count()).isOne();
        assertThat(second.profileId()).isEqualTo(first.profileId());
        assertThat(second.displayName()).isEqualTo("Semo User");
        assertThat(second.tagline()).isEqualTo("새로운 모임을 시작할 준비가 된 멤버");
        assertThat(second.profileColor()).isNotBlank();
    }

    @Test
    void getProfileSummaryReturnsInitializedProfile() {
        profileUserService.initializeProfile("user-002", "Profile Owner");

        ProfileSummaryResponse summary = profileUserService.getProfileSummary("user-002");

        assertThat(summary.userKey()).isEqualTo("user-002");
        assertThat(summary.displayName()).isEqualTo("Profile Owner");
    }
}
