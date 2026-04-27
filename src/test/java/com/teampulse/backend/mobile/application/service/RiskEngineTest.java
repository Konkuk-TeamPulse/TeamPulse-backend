package com.teampulse.backend.mobile.application.service;


import com.teampulse.backend.mobile.dto.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
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
    void derivesOverdueDueSoonBlockedAndConcentrationRisks() {
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
        assertThat(risks).extracting(RiskView::severity).contains(RiskSeverity.CRITICAL, RiskSeverity.WARNING, RiskSeverity.INFO);
    }

    @Test
    void doneTasksDoNotCreateDateRisks() {
        var risks = riskEngine.deriveRisks(List.of(
                task(1, "Completed", "Lee", TaskStatus.DONE, "2026-04-20", List.of(), List.of())
        ), List.of(), List.of(new MemberView(1, "Lee", TeamRole.LEADER)));

        assertThat(risks).isEmpty();
    }

    private TaskView task(long id, String title, String owner, TaskStatus status, String dueDate, List<String> blockers, List<String> next) {
        return new TaskView(id, title, owner, status, dueDate, "MEDIUM", blockers, next, "");
    }
}
