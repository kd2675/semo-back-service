package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.schedule.vo.ClubScheduleHomeResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleHomeService {
    private static final String FEATURE_SCHEDULE_MANAGE = "SCHEDULE_MANAGE";

    private final ClubAccessResolver clubAccessResolver;
    private final ClubScheduleService clubScheduleService;
    private final ClubFeatureService clubFeatureService;
    private final ClubScheduleEventRepository clubScheduleEventRepository;

    public ClubScheduleHomeResponse getScheduleHome(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireScheduleManageFeature(clubId);

        List<ClubScheduleEvent> events = loadHomeEvents(access, clubId);
        List<ScheduleEventSummaryResponse> eventSummaries = clubScheduleService.getEventSummariesForHome(access, events);
        LocalDate today = LocalDate.now();

        return new ClubScheduleHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                access.isAdmin(),
                events.size(),
                (int) events.stream()
                        .filter(event -> {
                            LocalDate endDate = event.getEndAt() == null
                                    ? event.getStartAt().toLocalDate()
                                    : event.getEndAt().toLocalDate();
                            return !endDate.isBefore(today);
                        })
                        .count(),
                events.size(),
                eventSummaries.stream().limit(20).toList()
        );
    }

    public void requireScheduleManageFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_SCHEDULE_MANAGE)) {
            throw new SemoException.ValidationException("일정 관리 기능이 활성화되지 않았습니다.");
        }
    }

    private List<ClubScheduleEvent> loadHomeEvents(ClubAccessResolver.ClubAccess access, Long clubId) {
        return clubScheduleEventRepository.findAllActiveEvents(clubId).stream()
                .filter(event -> access.isAdmin() || event.getAuthorClubProfileId().equals(access.clubProfile().getClubProfileId()))
                .sorted(Comparator.comparing(ClubScheduleEvent::getStartAt).reversed().thenComparing(ClubScheduleEvent::getEventId).reversed())
                .toList();
    }
}
