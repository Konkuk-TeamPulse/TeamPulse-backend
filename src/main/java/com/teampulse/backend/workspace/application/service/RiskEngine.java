package com.teampulse.backend.workspace.application.service;

import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.workspace.dto.MeetingView;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.RiskView;
import com.teampulse.backend.workspace.dto.TaskView;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RiskEngine {

    private static final int DUE_SOON_DAYS = 2;

    private final Clock clock;

    public RiskEngine() {
        this(Clock.systemDefaultZone());
    }

    RiskEngine(Clock clock) {
        this.clock = clock;
    }

    public List<RiskView> deriveRisks(List<TaskView> tasks, List<MeetingView> meetings, List<MemberView> members) {
        var today = LocalDate.now(clock);
        var openTasks = tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .toList();
        var risks = new ArrayList<RiskView>();

        addNotStartedNearDueRisk(risks, openTasks, today);
        addDeadlineRisk(risks, openTasks, today);
        addBlockedRisk(risks, openTasks);
        addOwnerConcentrationRisk(risks, openTasks);
        addMissingUpdateRisk(risks, openTasks, meetings, members);

        return List.copyOf(risks);
    }

    private void addNotStartedNearDueRisk(List<RiskView> risks, List<TaskView> openTasks, LocalDate today) {
        var notStartedNearDue = openTasks.stream()
                .filter(task -> task.status() == TaskStatus.TODO)
                .filter(task -> isDueWithin(task, today, DUE_SOON_DAYS))
                .toList();
        if (notStartedNearDue.isEmpty()) {
            return;
        }
        risks.add(new RiskView(
                101,
                RiskSeverity.WARNING,
                "진행 정체",
                notStartedNearDue.size() + "개 작업이 아직 시작되지 않았고 마감이 임박했거나 지났습니다.",
                "오늘 착수할 작업과 대기할 작업을 분리하고 우선순위를 다시 정하세요.",
                taskIds(notStartedNearDue),
                List.of("오늘 착수할 작업 선별", "우선순위 재정렬", "마감 임박 작업 일정 재조정")));
    }

    private void addDeadlineRisk(List<RiskView> risks, List<TaskView> openTasks, LocalDate today) {
        var overdue = openTasks.stream()
                .filter(task -> dueDate(task) != null)
                .filter(task -> dueDate(task).isBefore(today))
                .toList();
        var dueSoon = openTasks.stream()
                .filter(task -> isDueWithin(task, today, DUE_SOON_DAYS))
                .filter(task -> !dueDate(task).isBefore(today))
                .toList();
        if (overdue.isEmpty() && dueSoon.isEmpty()) {
            return;
        }
        risks.add(new RiskView(
                102,
                overdue.isEmpty() ? RiskSeverity.WARNING : RiskSeverity.CRITICAL,
                "일정 지연 위험",
                overdue.size() + "개 작업이 지연되었고 " + dueSoon.size() + "개 작업이 곧 마감됩니다.",
                "범위를 줄이거나 가장 오래 지연된 작업부터 완료하세요.",
                taskIds(overdue, dueSoon),
                List.of("범위 축소 검토", "마감일 재조정", "작업 재할당")));
    }

    private void addBlockedRisk(List<RiskView> risks, List<TaskView> openTasks) {
        var blocked = openTasks.stream()
                .filter(task -> !task.blockers().isEmpty() || !task.next().isEmpty())
                .toList();
        if (blocked.isEmpty()) {
            return;
        }
        risks.add(new RiskView(
                103,
                RiskSeverity.WARNING,
                "병목 구간",
                blocked.size() + "개 작업이 선행 작업이나 후속 의존관계에 묶여 있습니다.",
                "다른 작업을 미루기 전에 선행 작업과 의존관계를 먼저 정리하세요.",
                taskIds(blocked),
                List.of("선행 작업 우선 처리", "의존관계 재검토", "작업 분할")));
    }

    private void addOwnerConcentrationRisk(List<RiskView> risks, List<TaskView> openTasks) {
        var concentration = ownerConcentration(openTasks);
        if (concentration == null || openTasks.size() < 3 || concentration.count() / (double) openTasks.size() < 0.4) {
            return;
        }
        var concentratedTasks = openTasks.stream()
                .filter(task -> task.owner().equalsIgnoreCase(concentration.owner()))
                .toList();
        risks.add(new RiskView(
                104,
                RiskSeverity.WARNING,
                "역할 집중",
                "열린 작업의 " + Math.round(concentration.count() * 100.0 / openTasks.size()) + "%가 한 담당자에게 집중되어 있습니다.",
                "담당자를 평가하지 말고 작업을 나누거나 일부 역할을 재배치하세요.",
                taskIds(concentratedTasks),
                List.of("작업 재할당", "큰 작업 분할", "담당자 과부하 일정 조정")));
    }

    private void addMissingUpdateRisk(
            List<RiskView> risks,
            List<TaskView> openTasks,
            List<MeetingView> meetings,
            List<MemberView> members
    ) {
        if (openTasks.isEmpty() || !meetings.isEmpty() || members.size() <= 1) {
            return;
        }
        risks.add(new RiskView(
                105,
                RiskSeverity.INFO,
                "업데이트 부족",
                "진행 중인 작업은 있지만 공유된 회의록이나 의사결정 기록이 없습니다.",
                "동기화 회의를 기록하고 결정 사항과 다음 액션을 남기세요.",
                taskIds(openTasks),
                List.of("동기화 회의 등록", "결정 사항 기록", "다음 액션 정리")));
    }

    private boolean isDueWithin(TaskView task, LocalDate today, int days) {
        var dueDate = dueDate(task);
        return dueDate != null && !dueDate.isAfter(today.plusDays(days));
    }

    private LocalDate dueDate(TaskView task) {
        return parseDate(task.dueDate());
    }

    private List<Long> taskIds(List<TaskView> tasks) {
        return tasks.stream()
                .map(TaskView::id)
                .distinct()
                .toList();
    }

    private List<Long> taskIds(List<TaskView> first, List<TaskView> second) {
        var ids = new ArrayList<Long>();
        ids.addAll(taskIds(first));
        ids.addAll(taskIds(second));
        return ids.stream()
                .distinct()
                .toList();
    }

    private OwnerLoad ownerConcentration(List<TaskView> tasks) {
        return tasks.stream()
                .filter(task -> task.owner() != null && !task.owner().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(TaskView::owner, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(entry -> entry.getValue()))
                .map(entry -> new OwnerLoad(entry.getKey(), entry.getValue()))
                .orElse(null);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private record OwnerLoad(String owner, long count) {
    }
}
