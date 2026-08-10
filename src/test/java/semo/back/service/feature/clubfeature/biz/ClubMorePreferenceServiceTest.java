package semo.back.service.feature.clubfeature.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.database.pub.entity.ClubMorePreference;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubMorePreferenceRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.UpdateClubMorePreferenceRequest;

@ExtendWith(MockitoExtension.class)
class ClubMorePreferenceServiceTest {
    @Mock
    private ClubAccessResolver clubAccessResolver;
    @Mock
    private ClubFeatureService clubFeatureService;
    @Mock
    private ClubMorePreferenceRepository clubMorePreferenceRepository;

    @InjectMocks
    private ClubMorePreferenceService clubMorePreferenceService;

    @Test
    void updateFavorite_enabledFeature_persistsFavorite() {
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        ClubProfile clubProfile = mock(ClubProfile.class);
        ClubMorePreference preference = ClubMorePreference.builder()
                .clubMorePreferenceId(1L)
                .clubId(1L)
                .clubProfileId(11L)
                .featureKey("TODO")
                .favorite(false)
                .build();
        when(access.clubProfile()).thenReturn(clubProfile);
        when(clubProfile.getClubProfileId()).thenReturn(11L);
        when(clubAccessResolver.requireActiveMember(1L, "user-key")).thenReturn(access);
        when(clubFeatureService.getClubFeatures(1L, "user-key")).thenReturn(List.of(feature("TODO")));
        when(clubMorePreferenceRepository.findForUpdate(1L, 11L, "TODO")).thenReturn(Optional.of(preference));
        when(clubMorePreferenceRepository.save(preference)).thenReturn(preference);

        var response = clubMorePreferenceService.updateFavorite(
                1L,
                "todo",
                "user-key",
                new UpdateClubMorePreferenceRequest(true)
        );

        assertThat(response.favorite()).isTrue();
    }

    @Test
    void markUsed_missingPreference_createsRecentUsage() {
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        ClubProfile clubProfile = mock(ClubProfile.class);
        when(access.clubProfile()).thenReturn(clubProfile);
        when(clubProfile.getClubProfileId()).thenReturn(11L);
        when(clubAccessResolver.requireActiveMember(1L, "user-key")).thenReturn(access);
        when(clubFeatureService.getClubFeatures(1L, "user-key")).thenReturn(List.of(feature("FINANCE")));
        when(clubMorePreferenceRepository.findForUpdate(1L, 11L, "FINANCE")).thenReturn(Optional.empty());
        when(clubMorePreferenceRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = clubMorePreferenceService.markUsed(1L, "FINANCE", "user-key");

        assertThat(response.lastUsedAt()).isNotNull();
    }

    private ClubFeatureResponse feature(String featureKey) {
        return new ClubFeatureResponse(
                featureKey,
                featureKey,
                null,
                "apps",
                "USER_AND_ADMIN",
                10,
                true,
                "/user/" + featureKey,
                "/admin/" + featureKey
        );
    }
}
