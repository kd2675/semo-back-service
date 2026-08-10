package semo.back.service.feature.attachment.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;

@ExtendWith(MockitoExtension.class)
class ResourceAttachmentPolicyTest {
    @Mock
    private TodoItemRepository todoItemRepository;
    @Mock
    private FinanceRequestRepository financeRequestRepository;
    @Mock
    private ClubFeedbackRepository clubFeedbackRepository;
    @Mock
    private ClubTodoPermissionService clubTodoPermissionService;
    @Mock
    private ClubFinancePermissionService clubFinancePermissionService;

    @InjectMocks
    private ResourceAttachmentPolicy resourceAttachmentPolicy;

    @Test
    void requireCanAttach_ownFeedback_returnsPrivateVisibility() {
        ClubAccessResolver.ClubAccess access = access(1L, 11L);
        when(clubFeedbackRepository.findByFeedbackIdAndClubIdAndDeletedFalse(31L, 1L))
                .thenReturn(Optional.of(ClubFeedback.builder()
                        .feedbackId(31L)
                        .clubId(1L)
                        .submitterClubProfileId(11L)
                        .deleted(false)
                        .build()));

        String visibility = resourceAttachmentPolicy.requireCanAttach(access, "FEEDBACK", 31L);

        assertThat(visibility).isEqualTo("OWNER_AND_ADMIN");
    }

    @Test
    void requireCanView_otherMembersFinanceRequestWithoutCapability_throwsForbidden() {
        ClubAccessResolver.ClubAccess access = access(1L, 11L);
        when(financeRequestRepository.findByFinanceRequestIdAndClubId(41L, 1L))
                .thenReturn(Optional.of(FinanceRequest.builder()
                        .financeRequestId(41L)
                        .clubId(1L)
                        .requesterClubProfileId(22L)
                        .build()));
        when(clubFinancePermissionService.canViewAdminFinance(access)).thenReturn(false);

        assertThatThrownBy(() -> resourceAttachmentPolicy.requireCanView(access, "FINANCE_REQUEST", 41L))
                .isInstanceOf(SemoException.ForbiddenException.class);
    }

    private ClubAccessResolver.ClubAccess access(Long clubId, Long clubProfileId) {
        Club club = mock(Club.class);
        ClubProfile clubProfile = mock(ClubProfile.class);
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        when(access.club()).thenReturn(club);
        when(access.clubProfile()).thenReturn(clubProfile);
        when(club.getClubId()).thenReturn(clubId);
        when(clubProfile.getClubProfileId()).thenReturn(clubProfileId);
        return access;
    }
}
