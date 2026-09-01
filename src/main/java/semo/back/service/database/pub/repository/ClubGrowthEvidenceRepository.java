package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.Club;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClubGrowthEvidenceRepository extends Repository<Club, Long> {
    @Query(value = """
            select
              (select count(*)
                 from club_event_participant participant
                 join club_schedule_event event on event.event_id = participant.event_id
                where event.club_id = club.club_id
                  and participant.attendance_status in ('PRESENT', 'LATE')) as attendanceCount,
              (select count(*)
                 from club_schedule_vote_selection selection
                 join club_schedule_vote vote on vote.vote_id = selection.vote_id
                where vote.club_id = club.club_id) as voteSelectionCount,
              (select count(*)
                 from todo_item todo
                where todo.club_id = club.club_id
                  and todo.status_code = 'COMPLETED'
                  and todo.completed_at is not null) as completedTodoCount,
              (select count(*)
                 from club_feedback feedback
                where feedback.club_id = club.club_id
                  and feedback.deleted = false) as submittedFeedbackCount,
              (select count(*)
                 from club_board_item_read item_read
                 join club_board_item item on item.board_item_id = item_read.board_item_id
                where item.club_id = club.club_id) as boardReadCount,
              (select count(*)
                 from club_notice notice
                where notice.club_id = club.club_id
                  and notice.deleted = false
                  and notice.published_at is not null) as publishedNoticeCount,
              (select count(*)
                 from club_feedback feedback
                where feedback.club_id = club.club_id
                  and feedback.deleted = false
                  and feedback.answered_at is not null) as answeredFeedbackCount,
              (select count(*)
                 from club_schedule_vote vote
                where vote.club_id = club.club_id
                  and vote.closed_at is not null
                  and exists (
                        select 1
                          from club_schedule_vote_selection selection
                         where selection.vote_id = vote.vote_id
                  )) as closedVoteCount,
              (select count(*)
                 from club_schedule_event event
                where event.club_id = club.club_id
                  and event.event_status <> 'CANCELLED'
                  and exists (
                        select 1
                          from club_event_participant participant
                         where participant.event_id = event.event_id
                           and participant.attendance_status is not null
                  )) as attendanceEventCount,
              (select count(*)
                 from decision_record decision_record
                where decision_record.club_id = club.club_id
                  and decision_record.deleted = false
                  and decision_record.confirmed_at is not null) as confirmedDecisionCount,
              (select count(*)
                 from decision_resource_link resource_link
                 join decision_record decision_record
                   on decision_record.decision_record_id = resource_link.decision_record_id
                where decision_record.club_id = club.club_id
                  and decision_record.deleted = false
                  and decision_record.confirmed_at is not null) as decisionResourceLinkCount,
              (select count(*)
                 from todo_item todo
                where todo.club_id = club.club_id
                  and (todo.linked_schedule_event_id is not null
                       or todo.linked_decision_record_id is not null)) as linkedTodoCount,
              (select count(*)
                 from todo_item todo
                where todo.club_id = club.club_id
                  and todo.recurrence_source_todo_item_id is not null) as recurringTodoCount,
              (select count(*)
                 from club_handover_note handover
                where handover.club_id = club.club_id
                  and handover.deleted = false
                  and handover.status_code = 'ACKNOWLEDGED'
                  and handover.acknowledged_at is not null) as acknowledgedHandoverCount,
              (select count(*)
                 from club_term_carryover_item carryover
                where carryover.club_id = club.club_id
                  and carryover.status_code = 'RESOLVED'
                  and carryover.resolved_at is not null) as resolvedCarryoverCount,
              (select count(*)
                 from club_operating_term operating_term
                where operating_term.club_id = club.club_id
                  and operating_term.status_code = 'CLOSED'
                  and operating_term.closed_at is not null) as closedTermCount,
              (select count(*)
                 from club_activity_log activity
                where activity.club_id = club.club_id
                  and activity.status_code = 'SUCCESS'
                  and activity.create_date >= :recentCutoff) as recentActivityCount
            from club club
            where club.club_id = :clubId
            """, nativeQuery = true)
    Optional<ClubGrowthEvidenceRow> findEvidence(
            @Param("clubId") Long clubId,
            @Param("recentCutoff") LocalDateTime recentCutoff
    );
}
