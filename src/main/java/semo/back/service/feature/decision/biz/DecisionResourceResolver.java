package semo.back.service.feature.decision.biz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.decision.vo.DecisionResourceOptionResponse;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DecisionResourceResolver {
    public static final String RESOURCE_SCHEDULE_EVENT = "SCHEDULE_EVENT";
    public static final String RESOURCE_TODO_ITEM = "TODO_ITEM";
    public static final String RESOURCE_FINANCE_REQUEST = "FINANCE_REQUEST";
    public static final String RESOURCE_FINANCE_OBLIGATION = "FINANCE_OBLIGATION";
    public static final String RESOURCE_TOURNAMENT = "TOURNAMENT";

    private static final int OPTION_LIMIT_PER_TYPE = 20;

    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final TodoItemRepository todoItemRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final FinanceObligationRepository financeObligationRepository;
    private final TournamentRecordRepository tournamentRecordRepository;

    public ResourceDescriptor resolve(Long clubId, String resourceType, Long resourceId) {
        String normalizedType = normalizeType(resourceType);
        if (resourceId == null || resourceId <= 0) {
            throw new SemoException.ValidationException("연결할 리소스 ID가 올바르지 않습니다.");
        }
        return switch (normalizedType) {
            case RESOURCE_SCHEDULE_EVENT -> toDescriptor(requireScheduleEvent(clubId, resourceId));
            case RESOURCE_TODO_ITEM -> toDescriptor(requireTodoItem(clubId, resourceId));
            case RESOURCE_FINANCE_REQUEST -> toDescriptor(requireFinanceRequest(clubId, resourceId));
            case RESOURCE_FINANCE_OBLIGATION -> toDescriptor(requireFinanceObligation(clubId, resourceId));
            case RESOURCE_TOURNAMENT -> toDescriptor(requireTournament(clubId, resourceId));
            default -> throw new SemoException.ValidationException("지원하지 않는 의사결정 연결 대상입니다.");
        };
    }

    public List<DecisionResourceOptionResponse> getOptions(Long clubId) {
        List<ResourceDescriptor> options = new ArrayList<>();
        PageRequest optionPage = PageRequest.of(0, OPTION_LIMIT_PER_TYPE);
        todoItemRepository.findByClubIdOrderByTodoItemIdDesc(clubId, optionPage).stream()
                .map(this::toDescriptor)
                .forEach(options::add);
        clubScheduleEventRepository.findRecentActiveEvents(clubId, optionPage).stream()
                .map(this::toDescriptor)
                .forEach(options::add);
        financeRequestRepository.findByClubIdOrderByFinanceRequestIdDesc(clubId, optionPage).stream()
                .map(this::toDescriptor)
                .forEach(options::add);
        financeObligationRepository.findAdminFeed(
                        clubId,
                        null,
                        null,
                        null,
                        optionPage
                ).stream()
                .map(this::toDescriptor)
                .forEach(options::add);
        tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(
                        clubId,
                        optionPage
                ).stream()
                .map(this::toDescriptor)
                .forEach(options::add);
        return options.stream()
                .map(item -> new DecisionResourceOptionResponse(
                        item.resourceType(),
                        item.resourceId(),
                        item.title(),
                        item.statusLabel(),
                        item.targetPath()
                ))
                .toList();
    }

    private ClubScheduleEvent requireScheduleEvent(Long clubId, Long resourceId) {
        return clubScheduleEventRepository.findByEventIdAndClubId(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubScheduleEvent",
                        "eventId",
                        resourceId
                ));
    }

    private TodoItem requireTodoItem(Long clubId, Long resourceId) {
        return todoItemRepository.findByTodoItemIdAndClubId(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TodoItem", "todoItemId", resourceId));
    }

    private FinanceRequest requireFinanceRequest(Long clubId, Long resourceId) {
        return financeRequestRepository.findByFinanceRequestIdAndClubId(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceRequest",
                        "financeRequestId",
                        resourceId
                ));
    }

    private FinanceObligation requireFinanceObligation(Long clubId, Long resourceId) {
        return financeObligationRepository.findByFinanceObligationIdAndClubId(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceObligation",
                        "financeObligationId",
                        resourceId
                ));
    }

    private TournamentRecord requireTournament(Long clubId, Long resourceId) {
        return tournamentRecordRepository.findByTournamentRecordIdAndClubIdAndDeletedFalse(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TournamentRecord",
                        "tournamentRecordId",
                        resourceId
                ));
    }

    private ResourceDescriptor toDescriptor(ClubScheduleEvent event) {
        return new ResourceDescriptor(
                RESOURCE_SCHEDULE_EVENT,
                event.getEventId(),
                event.getTitle(),
                event.getEventStatus(),
                "/clubs/%d/schedule/%d".formatted(event.getClubId(), event.getEventId())
        );
    }

    private ResourceDescriptor toDescriptor(TodoItem todo) {
        return new ResourceDescriptor(
                RESOURCE_TODO_ITEM,
                todo.getTodoItemId(),
                todo.getTitle(),
                todo.getStatusCode(),
                "/clubs/%d/more/todos".formatted(todo.getClubId())
        );
    }

    private ResourceDescriptor toDescriptor(FinanceRequest request) {
        return new ResourceDescriptor(
                RESOURCE_FINANCE_REQUEST,
                request.getFinanceRequestId(),
                request.getTitle(),
                request.getStatusCode(),
                "/clubs/%d/more/finance".formatted(request.getClubId())
        );
    }

    private ResourceDescriptor toDescriptor(FinanceObligation obligation) {
        return new ResourceDescriptor(
                RESOURCE_FINANCE_OBLIGATION,
                obligation.getFinanceObligationId(),
                obligation.getTitle(),
                obligation.getStatusCode(),
                "/clubs/%d/more/finance".formatted(obligation.getClubId())
        );
    }

    private ResourceDescriptor toDescriptor(TournamentRecord tournament) {
        return new ResourceDescriptor(
                RESOURCE_TOURNAMENT,
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                tournament.getTournamentStatus(),
                "/clubs/%d/more/tournaments/%d".formatted(
                        tournament.getClubId(),
                        tournament.getTournamentRecordId()
                )
        );
    }

    private String normalizeType(String resourceType) {
        return resourceType == null ? "" : resourceType.trim().toUpperCase(Locale.ROOT);
    }

    public record ResourceDescriptor(
            String resourceType,
            Long resourceId,
            String title,
            String statusLabel,
            String targetPath
    ) {
    }
}
