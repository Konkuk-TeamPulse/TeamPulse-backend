package com.teampulse.backend.mobile.application.service;

import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.mobile.dto.MeetingView;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.RiskView;
import com.teampulse.backend.mobile.dto.TaskView;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RiskEngine {

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

        var overdue = openTasks.stream()
                .filter(task -> parseDate(task.dueDate()) != null)
                .filter(task -> parseDate(task.dueDate()).isBefore(today))
                .toList();
        if (!overdue.isEmpty()) {
            risks.add(new RiskView(
                    101,
                    RiskSeverity.CRITICAL,
                    "Overdue tasks",
                    overdue.size() + " open task(s) are past their due date.",
                    "Reduce scope or complete the oldest delayed task first."));
        }

        var dueSoon = openTasks.stream()
                .filter(task -> parseDate(task.dueDate()) != null)
                .filter(task -> {
                    var dueDate = parseDate(task.dueDate());
                    return !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(2));
                })
                .toList();
        if (!dueSoon.isEmpty()) {
            risks.add(new RiskView(
                    102,
                    RiskSeverity.WARNING,
                    "Tasks due soon",
                    dueSoon.size() + " open task(s) are due within two days.",
                    "Separate work that must be handled today from work that can wait."));
        }

        var blocked = openTasks.stream()
                .filter(task -> !task.blockers().isEmpty() || !task.next().isEmpty())
                .toList();
        if (!blocked.isEmpty()) {
            risks.add(new RiskView(
                    103,
                    RiskSeverity.WARNING,
                    "Blocked work",
                    blocked.size() + " task(s) have blockers or follow-up work.",
                    "Resolve prerequisite work before adding more dependent tasks."));
        }

        var concentration = ownerConcentration(openTasks);
        if (concentration != null && openTasks.size() >= 3 && concentration.count() / (double) openTasks.size() >= 0.4) {
            risks.add(new RiskView(
                    104,
                    RiskSeverity.WARNING,
                    "Owner load concentration",
                    concentration.owner() + " owns " + Math.round(concentration.count() * 100.0 / openTasks.size()) + "% of open tasks.",
                    "Reassign part of the workload or split large tasks."));
        }

        if (!openTasks.isEmpty() && meetings.isEmpty() && members.size() > 1) {
            risks.add(new RiskView(
                    105,
                    RiskSeverity.INFO,
                    "Missing meeting log",
                    "Tasks exist, but no meeting has been recorded yet.",
                    "Record a sync meeting and save key decisions."));
        }

        return List.copyOf(risks);
    }

    private OwnerLoad ownerConcentration(List<TaskView> tasks) {
        return tasks.stream()
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
