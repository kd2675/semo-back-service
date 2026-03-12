package semo.back.service.feature.profile.act;

import auth.common.core.context.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.profile.vo.ProfileSummaryResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private ProfileController profileController;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        profileUserRepository.deleteAll();
    }

    @Test
    void initializeProfileCreatesProfileForAuthenticatedUser() {
        UserContext userContext = UserContext.builder()
                .userKey("user-003")
                .userName("Club Master")
                .role("USER")
                .build();

        ResponseDataDTO<ProfileSummaryResponse> response = profileController.initializeProfile(userContext);

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().userKey()).isEqualTo("user-003");
        assertThat(response.getData().displayName()).isEqualTo("Club Master");
        assertThat(profileUserRepository.count()).isOne();
    }

    @Test
    void initializeProfileRejectsAnonymousRequest() {
        assertThatThrownBy(() -> profileController.initializeProfile(UserContext.builder().build()))
                .isInstanceOf(SemoException.UnauthorizedException.class)
                .hasMessage("Login required");
    }
}
