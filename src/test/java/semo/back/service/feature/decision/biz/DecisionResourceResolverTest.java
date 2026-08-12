package semo.back.service.feature.decision.biz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionResourceResolverTest {
    @Mock private ClubScheduleEventRepository clubScheduleEventRepository;
    @Mock private TodoItemRepository todoItemRepository;
    @Mock private FinanceRequestRepository financeRequestRepository;
    @Mock private FinanceObligationRepository financeObligationRepository;
    @Mock private TournamentRecordRepository tournamentRecordRepository;
    @Mock private ClubFeatureService clubFeatureService;

    @InjectMocks
    private DecisionResourceResolver decisionResourceResolver;

    @Test
    void getOptions_onlyTodoEnabled_returnsOnlyTodoResources() {
        when(clubFeatureService.isFeatureEnabled(1L, "TODO")).thenReturn(true);
        when(todoItemRepository.findByClubIdOrderByTodoItemIdDesc(any(), any())).thenReturn(List.of(
                TodoItem.builder()
                        .todoItemId(11L)
                        .clubId(1L)
                        .title("독립 업무")
                        .statusCode("OPEN")
                        .build()
        ));

        var options = decisionResourceResolver.getOptions(1L);

        assertThat(options).singleElement()
                .satisfies(option -> {
                    assertThat(option.resourceType()).isEqualTo("TODO_ITEM");
                    assertThat(option.resourceId()).isEqualTo(11L);
                });
        verify(clubScheduleEventRepository, never()).findRecentActiveEvents(any(), any());
        verify(financeRequestRepository, never()).findByClubIdOrderByFinanceRequestIdDesc(any(), any());
        verify(financeObligationRepository, never()).findAdminFeed(any(), any(), any(), any(), any());
        verify(tournamentRecordRepository, never())
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(any(), any());
    }

    @Test
    void resolve_disabledSourceFeature_rejectsNewLinkWithoutRepositoryLookup() {
        when(clubFeatureService.isFeatureEnabled(1L, "FINANCE")).thenReturn(false);

        assertThatThrownBy(() -> decisionResourceResolver.resolve(1L, "FINANCE_REQUEST", 22L))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("비활성화된 기능");
        verify(financeRequestRepository, never()).findByFinanceRequestIdAndClubId(any(), any());
    }
}
