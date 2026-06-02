package com.teampulse.backend.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.workspace.dto.MeetingView;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.RiskView;
import com.teampulse.backend.workspace.dto.TaskView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskEngineTest {

    private final RiskEngine riskEngine = new RiskEngine(Clock.fixed(
            Instant.parse("2026-04-24T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    ));

    @Test
    void derivesStructuralProjectRisksWithoutScoringPeople() {
        var tasks = List.of(
                task(1, "ERD", "Lee", TaskStatus.TODO, "2026-04-20", List.of(), List.of()),
                task(2, "API", "Lee", TaskStatus.DOING, "2026-04-25", List.of("ERD"), List.of()),
                task(3, "UI", "Lee", TaskStatus.TODO, "2026-04-26", List.of(), List.of()),
                task(4, "Docs", "Park", TaskStatus.DONE, "2026-04-22", List.of(), List.of())
        );

        var risks = riskEngine.deriveRisks(tasks, List.of(), List.of(
                new MemberView(1, "Lee", TeamRole.LEADER),
                new MemberView(2, "Park", TeamRole.MEMBER)
        ));

        assertThat(risks).extracting(RiskView::id).contains(101L, 102L, 103L, 104L, 105L);
        assertThat(risks).extracting(RiskView::title)
                .contains("진행 정체", "일정 지연 위험", "병목 구간", "역할 집중", "업데이트 부족");
        assertThat(risks).extracting(RiskView::severity)
                .contains(RiskSeverity.CRITICAL, RiskSeverity.WARNING, RiskSeverity.INFO);
        assertThat(risks).extracting(RiskView::body)
                .allSatisfy(body -> assertThat(body).doesNotContain("점수", "평가", "기여도"));
        assertThat(riskById(risks, 101).affectedTaskIds()).containsExactly(1L, 3L);
        assertThat(riskById(risks, 102).affectedTaskIds()).containsExactly(1L, 2L, 3L);
        assertThat(riskById(risks, 103).affectedTaskIds()).containsExactly(2L);
        assertThat(riskById(risks, 104).affectedTaskIds()).containsExactly(1L, 2L, 3L);
        assertThat(riskById(risks, 105).affectedTaskIds()).containsExactly(1L, 2L, 3L);
        assertThat(risks).allSatisfy(risk -> assertThat(risk.suggestedActions()).isNotEmpty());
        assertThat(riskById(risks, 104).suggestedActions()).contains("작업 재할당");
    }

    @Test
    void doneTasksDoNotCreateDateRisks() {
        var risks = riskEngine.deriveRisks(List.of(
                task(1, "Completed", "Lee", TaskStatus.DONE, "2026-04-20", List.of(), List.of())
        ), List.of(), List.of(new MemberView(1, "Lee", TeamRole.LEADER)));

        assertThat(risks).isEmpty();
    }

    @Test
    void dueSoonTasksWithoutOverdueCreateWarningDeadlineRiskAndIgnoreInvalidDates() {
        var risks = riskEngine.deriveRisks(List.of(
                task(1, "Today", "Lee", TaskStatus.TODO, "2026-04-24", List.of(), List.of()),
                task(2, "Soon", "Park", TaskStatus.DOING, "2026-04-26", List.of(), List.of()),
                task(3, "Bad date", "Kim", TaskStatus.TODO, "not-a-date", List.of(), List.of())
        ), List.of(meeting()), List.of(
                new MemberView(1, "Lee", TeamRole.LEADER),
                new MemberView(2, "Park", TeamRole.MEMBER)
        ));

        assertThat(risks).extracting(RiskView::id).contains(101L, 102L);
        assertThat(riskById(risks, 102).severity()).isEqualTo(RiskSeverity.WARNING);
        assertThat(riskById(risks, 102).affectedTaskIds()).containsExactly(1L, 2L);
    }

    @Test
    void riskEngineIgnoresBlankMissingAndMalformedDatesWhenNoOtherSignalsExist() {
        var risks = riskEngine.deriveRisks(List.of(
                task(1, "No due date", null, TaskStatus.TODO, null, List.of(), List.of()),
                task(2, "Blank due date", " ", TaskStatus.DOING, " ", List.of(), List.of()),
                task(3, "Malformed due date", "", TaskStatus.TODO, "2026-99-99", List.of(), List.of())
        ), List.of(meeting()), List.of(
                new MemberView(1, "Lee", TeamRole.LEADER),
                new MemberView(2, "Park", TeamRole.MEMBER)
        ));

        assertThat(risks).isEmpty();
    }

    @Test
    void ownerConcentrationRequiresEnoughTasksAndMeaningfulRatio() {
        var lessThanThree = riskEngine.deriveRisks(List.of(
                task(1, "A", "Lee", TaskStatus.TODO, "2026-05-10", List.of(), List.of()),
                task(2, "B", "Lee", TaskStatus.DOING, "2026-05-11", List.of(), List.of())
        ), List.of(meeting()), List.of(
                new MemberView(1, "Lee", TeamRole.LEADER),
                new MemberView(2, "Park", TeamRole.MEMBER)
        ));
        var belowRatio = riskEngine.deriveRisks(List.of(
                task(1, "A", "Lee", TaskStatus.TODO, "2026-05-10", List.of(), List.of()),
                task(2, "B", "Park", TaskStatus.DOING, "2026-05-11", List.of(), List.of()),
                task(3, "C", "Kim", TaskStatus.TODO, "2026-05-12", List.of(), List.of())
        ), List.of(meeting()), List.of(
                new MemberView(1, "Lee", TeamRole.LEADER),
                new MemberView(2, "Park", TeamRole.MEMBER)
        ));

        assertThat(lessThanThree).extracting(RiskView::id).doesNotContain(104L);
        assertThat(belowRatio).extracting(RiskView::id).doesNotContain(104L);
    }

    @Test
    void nextTaskRelationshipCreatesBlockedRiskEvenWithoutBlockers() {
        var risks = riskEngine.deriveRisks(List.of(
                task(1, "Preceding", "Lee", TaskStatus.DOING, "2026-05-10", List.of(), List.of("Follow-up")),
                task(2, "Follow-up", "Park", TaskStatus.TODO, "2026-05-11", List.of(), List.of())
        ), List.of(meeting()), List.of(
                new MemberView(1, "Lee", TeamRole.LEADER),
                new MemberView(2, "Park", TeamRole.MEMBER)
        ));

        assertThat(risks).extracting(RiskView::id).contains(103L);
        assertThat(riskById(risks, 103).affectedTaskIds()).containsExactly(1L);
    }

    private TaskView task(long id, String title, String owner, TaskStatus status, String dueDate, List<String> blockers, List<String> next) {
        return new TaskView(id, title, owner, status, dueDate, "MEDIUM", blockers, next, "");
    }

    private MeetingView meeting() {
        return new MeetingView(1, "Weekly", "2026-04-24", "Sync", List.of(), List.of());
    }

    private RiskView riskById(List<RiskView> risks, long id) {
        return risks.stream()
                .filter(risk -> risk.id() == id)
                .findFirst()
                .orElseThrow();
    }
}
