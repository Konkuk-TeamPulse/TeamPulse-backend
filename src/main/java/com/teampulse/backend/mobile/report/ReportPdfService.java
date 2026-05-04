package com.teampulse.backend.mobile.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.teampulse.backend.domain.risk.RiskSeverity;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.domain.team.TeamRole;
import com.teampulse.backend.mobile.dto.ActivityView;
import com.teampulse.backend.mobile.dto.MeetingActionItemView;
import com.teampulse.backend.mobile.dto.MeetingView;
import com.teampulse.backend.mobile.dto.MemberView;
import com.teampulse.backend.mobile.dto.ReportView;
import com.teampulse.backend.mobile.dto.RiskView;
import com.teampulse.backend.mobile.dto.TaskView;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReportPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final String configuredFontPath;

    public ReportPdfService(@Value("${app.report.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath == null ? "" : configuredFontPath.trim();
    }

    public byte[] render(WorkspaceState workspace, ReportView report) {
        var view = buildView(workspace, report);
        var html = html(view);

        try (var output = new ByteArrayOutputStream()) {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            findFontFile().ifPresent(font -> builder.useFont(font, "ReportSans"));
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("PDF rendering failed.", exception);
        }
    }

    private ReportPdfView buildView(WorkspaceState workspace, ReportView report) {
        var baselineDate = reportRangeEnd(report.range()).orElse(LocalDate.now());
        var tasks = workspace.tasks();
        var meetings = workspace.meetings();
        var risks = workspace.risks();
        var activities = workspace.activities();
        var members = workspace.members();

        var total = tasks.size();
        var todo = countByStatus(tasks, TaskStatus.TODO);
        var doing = countByStatus(tasks, TaskStatus.DOING);
        var done = countByStatus(tasks, TaskStatus.DONE);
        var overdue = (int) tasks.stream().filter(task -> isOverdue(task, baselineDate)).count();
        var blocked = (int) tasks.stream().filter(task -> isBlocked(task)).count();
        var dueSoon = (int) tasks.stream().filter(task -> isDueSoon(task, baselineDate)).count();
        var checkTaskRows = checkTasks(tasks, baselineDate);
        Integer completionRate = total == 0 ? null : (int) Math.round(done * 100.0 / total);

        var progress = new ProgressSummary(
                total,
                todo,
                doing,
                done,
                overdue,
                blocked,
                dueSoon,
                checkTaskRows.size(),
                completionRate,
                summaryLine1(total, done),
                summaryLine2(doing, checkTaskRows.size()),
                summaryLine3(meetings));

        return new ReportPdfView(
                new ReportMeta(
                        report.id(),
                        statusLabel(report.status()),
                        LocalDateTime.now().format(DATE_TIME_FORMAT),
                        report.range(),
                        baselineDate.toString()),
                new ProjectOverview(
                        workspace.projectId(),
                        textOrDash(workspace.team().name()),
                        textOrDash(workspace.team().courseName()),
                        textOrDash(workspace.team().description()),
                        textOrDash(workspace.team().startDate()),
                        textOrDash(workspace.team().dueDate()),
                        textOrDash(workspace.team().semester())),
                progress,
                metricCards(progress, meetings.size(), risks.size()),
                memberWorkloads(members, tasks, activities, baselineDate),
                taskRows(tasks),
                checkTaskRows,
                meetingSummaries(meetings, members),
                riskRows(risks, tasks),
                nextActions(meetings, risks, tasks, baselineDate),
                activityRows(activities));
    }

    private List<MetricCard> metricCards(ProgressSummary progress, int meetingCount, int riskCount) {
        return List.of(
                new MetricCard("전체 태스크", String.valueOf(progress.totalTasks()), "프로젝트에 등록된 태스크"),
                new MetricCard("완료 태스크", String.valueOf(progress.doneTasks()), "DONE 상태 태스크"),
                new MetricCard("진행 중", String.valueOf(progress.doingTasks()), "DOING 상태 태스크"),
                new MetricCard("진행 점검", String.valueOf(progress.checkTasks()), "마감일/의존관계 기준"),
                new MetricCard("회의", String.valueOf(meetingCount), "등록된 회의"),
                new MetricCard("리스크", String.valueOf(riskCount), "등록된 확인 필요 항목"));
    }

    private List<MemberWorkloadRow> memberWorkloads(
            List<MemberView> members,
            List<TaskView> tasks,
            List<ActivityView> activities,
            LocalDate baselineDate
    ) {
        if (members.isEmpty()) {
            return List.of();
        }
        return members.stream()
                .map(member -> {
                    var assigned = tasks.stream()
                            .filter(task -> same(task.owner(), member.name()))
                            .toList();
                    var done = countByStatus(assigned, TaskStatus.DONE);
                    var doing = countByStatus(assigned, TaskStatus.DOING);
                    var overdue = (int) assigned.stream().filter(task -> isOverdue(task, baselineDate)).count();
                    var lastActivity = activities.stream()
                            .filter(activity -> same(activity.actor(), member.name()) || same(activity.actor(), member.email()))
                            .max(Comparator.comparing(activity -> parseDateTimeOrMin(activity.at())));
                    return new MemberWorkloadRow(
                            member.id(),
                            member.name(),
                            roleLabel(member.role()),
                            assigned.size(),
                            done,
                            doing,
                            overdue,
                            lastActivity.map(ActivityView::at).orElse("-"),
                            memberComment(assigned.size(), done, doing, overdue, lastActivity.isPresent()));
                })
                .toList();
    }

    private List<TaskRow> taskRows(List<TaskView> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparing((TaskView task) -> parseDateOrMax(task.dueDate())).thenComparing(TaskView::id))
                .map(task -> new TaskRow(
                        task.id(),
                        task.title(),
                        textOrDash(task.owner()),
                        statusLabel(task.status()),
                        textOrDash(task.dueDate()),
                        task.blockers().isEmpty() ? "없음" : String.join(", ", task.blockers())))
                .toList();
    }

    private List<TaskCheckRow> checkTasks(List<TaskView> tasks, LocalDate baselineDate) {
        return tasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .filter(task -> isOverdue(task, baselineDate) || isBlocked(task) || isDueSoon(task, baselineDate))
                .sorted(Comparator.comparing((TaskView task) -> parseDateOrMax(task.dueDate())).thenComparing(TaskView::id))
                .map(task -> new TaskCheckRow(
                        task.id(),
                        task.title(),
                        textOrDash(task.owner()),
                        textOrDash(task.dueDate()),
                        statusLabel(task.status()),
                        checkReason(task, baselineDate),
                        suggestedAction(task, baselineDate)))
                .toList();
    }

    private List<MeetingSummary> meetingSummaries(List<MeetingView> meetings, List<MemberView> members) {
        var membersById = members.stream().collect(Collectors.toMap(MemberView::id, MemberView::name, (left, right) -> left));
        return meetings.stream()
                .sorted(Comparator.comparing((MeetingView meeting) -> textOrDash(meeting.time())).reversed())
                .map(meeting -> new MeetingSummary(
                        meeting.id(),
                        meeting.title(),
                        textOrDash(meeting.time()),
                        meeting.attendeeIds().stream()
                                .map(membersById::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(", ")),
                        textOrDash(meeting.agenda()),
                        textOrDash(meeting.content()),
                        cleanList(meeting.decisions()),
                        cleanList(meeting.actionItems().stream().map(MeetingActionItemView::content).toList())))
                .toList();
    }

    private List<RiskCheckRow> riskRows(List<RiskView> risks, List<TaskView> tasks) {
        var taskTitlesById = tasks.stream().collect(Collectors.toMap(TaskView::id, TaskView::title, (left, right) -> left));
        return risks.stream()
                .map(risk -> new RiskCheckRow(
                        risk.id(),
                        riskTitleLabel(risk.title()),
                        severityLabel(risk.severity()),
                        risk.body(),
                        risk.affectedTaskIds().stream()
                                .map(taskTitlesById::get)
                                .filter(Objects::nonNull)
                                .toList(),
                        cleanList(risk.suggestedActions())))
                .toList();
    }

    private List<NextActionRow> nextActions(
            List<MeetingView> meetings,
            List<RiskView> risks,
            List<TaskView> tasks,
            LocalDate baselineDate
    ) {
        var actions = new ArrayList<NextActionRow>();
        meetings.stream()
                .flatMap(meeting -> meeting.actionItems().stream())
                .filter(item -> item.content() != null && !item.content().isBlank())
                .limit(5)
                .forEach(item -> actions.add(new NextActionRow(
                        "중간",
                        item.content(),
                        "-",
                        "회의 액션 아이템",
                        textOrDash(item.dueDate()))));

        risks.stream()
                .flatMap(risk -> risk.suggestedActions().stream())
                .filter(action -> action != null && !action.isBlank())
                .limit(5)
                .forEach(action -> actions.add(new NextActionRow("중간", action, "-", "리스크 제안 액션", "-")));

        checkTasks(tasks, baselineDate).stream()
                .limit(5)
                .forEach(task -> actions.add(new NextActionRow(
                        isOverdueTaskRow(task) ? "높음" : "중간",
                        "\"" + task.title() + "\" 태스크의 실제 진행 상태를 확인합니다.",
                        task.assigneeName(),
                        checkSourceLabel(task),
                        task.dueDate())));

        return actions.stream().limit(12).toList();
    }

    private boolean isOverdueTaskRow(TaskCheckRow task) {
        return task.checkReason().contains("마감일");
    }

    private List<ActivityLogRow> activityRows(List<ActivityView> activities) {
        return activities.stream()
                .sorted(Comparator.comparing((ActivityView activity) -> parseDateTimeOrMin(activity.at())).reversed())
                .limit(20)
                .map(activity -> new ActivityLogRow(activity.at(), activity.actor(), activitySummaryLabel(activity.summary())))
                .toList();
    }

    private String html(ReportPdfView view) {
        return """
                <html xmlns="http://www.w3.org/1999/xhtml" lang="ko">
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    %s
                  </style>
                </head>
                <body>
                  %s
                </body>
                </html>
                """.formatted(css(), body(view));
    }

    private String body(ReportPdfView view) {
        var html = new StringBuilder();
        html.append("""
                <div class="cover">
                  <div class="topline">
                    <div>
                      <h1>TeamPulse Project Report</h1>
                      <p class="subtitle">프로젝트 진행 상황 리포트</p>
                    </div>
                    <div class="status">
                      <span>Report Status</span>
                      <strong>%s</strong>
                      <small>RPT-%s</small>
                    </div>
                  </div>
                  <div class="hero">
                    <div>
                      <p class="eyebrow">프로젝트명</p>
                      <h2>%s</h2>
                      <p>%s · %s</p>
                      <p>리포트 기간: %s</p>
                      <p>생성일: %s</p>
                    </div>
                    <div class="progressBox">
                      <span>태스크 완료율</span>
                      <strong>%s</strong>
                      <div class="bar"><i style="width:%s"></i></div>
                      <small>완료 %d / 전체 %d</small>
                    </div>
                  </div>
                  <div class="summary">
                    <p>%s</p>
                    <p>%s</p>
                    <p>%s</p>
                  </div>
                  <div class="metrics">
                    %s
                  </div>
                </div>
                """.formatted(
                e(view.report().statusLabel()),
                e(String.valueOf(view.report().reportId())),
                e(view.project().projectName()),
                e(view.project().subject()),
                e(view.project().semester()),
                e(view.report().range()),
                e(view.report().generatedAt()),
                view.progress().completionRate() == null ? "계산 불가" : view.progress().completionRate() + "%",
                view.progress().completionRate() == null ? "0%" : view.progress().completionRate() + "%",
                view.progress().doneTasks(),
                view.progress().totalTasks(),
                e(view.progress().summaryLine1()),
                e(view.progress().summaryLine2()),
                e(view.progress().summaryLine3()),
                metricCardsHtml(view.metricCards())));

        html.append(section("프로젝트 개요", overviewHtml(view)));
        html.append(section("전체 진행 요약", progressHtml(view)));
        html.append(section("태스크 진행 현황", taskTableHtml(view.taskRows())));
        html.append(section("팀원별 작업 현황", memberTableHtml(view.memberWorkloads())));
        html.append(section("지연 및 병목 확인 항목", checkTaskTableHtml(view.delayedOrBlockedTasks())));
        html.append(section("회의 및 의사결정 요약", meetingHtml(view.meetings())));
        html.append(section("리스크 확인 항목", riskHtml(view.risks())));
        html.append(section("다음 액션", nextActionTableHtml(view.nextActions())));
        html.append(section("활동 로그 요약", activityTableHtml(view.activityLogs())));
        return html.toString();
    }

    private String section(String title, String content) {
        return "<section><h3>" + e(title) + "</h3>" + content + "</section>";
    }

    private String overviewHtml(ReportPdfView view) {
        return """
                <div class="infoGrid">
                  <div><span>프로젝트 ID</span><strong>%d</strong></div>
                  <div><span>프로젝트명</span><strong>%s</strong></div>
                  <div><span>과목명</span><strong>%s</strong></div>
                  <div><span>학기</span><strong>%s</strong></div>
                  <div><span>기간</span><strong>%s ~ %s</strong></div>
                </div>
                <p class="note">%s</p>
                """.formatted(
                view.project().projectId(),
                e(view.project().projectName()),
                e(view.project().subject()),
                e(view.project().semester()),
                e(view.project().startDate()),
                e(view.project().endDate()),
                e("-".equals(view.project().description())
                        ? "프로젝트 설명이 등록되지 않았습니다. 프로젝트 목표나 제출 범위를 추가하면 리포트 이해도가 높아집니다."
                        : view.project().description()));
    }

    private String progressHtml(ReportPdfView view) {
        return """
                <div class="progressLayout">
                  <div class="progressBox small">
                    <span>태스크 완료율</span>
                    <strong>%s</strong>
                    <div class="bar"><i style="width:%s"></i></div>
                  </div>
                  <table>
                    <thead><tr><th>구분</th><th>값</th><th>설명</th></tr></thead>
                    <tbody>
                      <tr><td>예정</td><td>%d</td><td>TODO 상태 태스크</td></tr>
                      <tr><td>진행 중</td><td>%d</td><td>DOING 상태 태스크</td></tr>
                      <tr><td>완료</td><td>%d</td><td>DONE 상태 태스크</td></tr>
                      <tr><td>일정 확인 필요</td><td>%d</td><td>마감일 기준 추가 확인 필요</td></tr>
                      <tr><td>마감 임박</td><td>%d</td><td>기준일로부터 3일 이내 마감</td></tr>
                      <tr><td>의존관계 확인 필요</td><td>%d</td><td>차단 요소 또는 선행 작업 확인 필요</td></tr>
                      <tr><td>진행 점검 대상</td><td>%d</td><td>일정/마감/의존관계 기준 중복 제거 목록</td></tr>
                    </tbody>
                  </table>
                </div>
                """.formatted(
                view.progress().completionRate() == null ? "계산 불가" : view.progress().completionRate() + "%",
                view.progress().completionRate() == null ? "0%" : view.progress().completionRate() + "%",
                view.progress().todoTasks(),
                view.progress().doingTasks(),
                view.progress().doneTasks(),
                view.progress().overdueTasks(),
                view.progress().dueSoonTasks(),
                view.progress().blockedTasks(),
                view.progress().checkTasks());
    }

    private String metricCardsHtml(List<MetricCard> cards) {
        return cards.stream()
                .map(card -> """
                        <div class="metric">
                          <span>%s</span>
                          <strong>%s</strong>
                          <small>%s</small>
                        </div>
                        """.formatted(e(card.label()), e(card.value()), e(card.description())))
                .collect(Collectors.joining());
    }

    private String taskTableHtml(List<TaskRow> rows) {
        if (rows.isEmpty()) {
            return empty("등록된 태스크가 없어 진행률을 계산할 수 없습니다. 태스크를 등록하면 상태별 진행 현황이 표시됩니다.");
        }
        var body = rows.stream()
                .map(row -> """
                        <tr>
                          <td>%s</td><td>%s</td><td><span class="badge">%s</span></td><td>%s</td><td>%s</td>
                        </tr>
                        """.formatted(e(row.title()), e(row.assigneeName()), e(row.statusLabel()), e(row.dueDate()), e(row.dependencyLabel())))
                .collect(Collectors.joining());
        return table("태스크명", "담당자", "상태", "마감일", "의존/차단", body);
    }

    private String memberTableHtml(List<MemberWorkloadRow> rows) {
        if (rows.isEmpty()) {
            return empty("등록된 팀원 정보가 없습니다. 팀원을 추가하면 역할과 작업 현황을 리포트에서 확인할 수 있습니다.");
        }
        return "<div class=\"memberCards\">" + rows.stream()
                .map(row -> """
                        <div class="memberCard">
                          <h4>%s <span class="badge muted">%s</span></h4>
                          <div class="miniStats">
                            <span>담당 <b>%d</b></span>
                            <span>완료 <b>%d</b></span>
                            <span>진행 중 <b>%d</b></span>
                            <span>확인 필요 <b>%d</b></span>
                          </div>
                          <p><b>최근 활동</b> %s</p>
                          <p>%s</p>
                        </div>
                        """.formatted(
                        e(row.name()),
                        e(row.roleLabel()),
                        row.assignedTaskCount(),
                        row.doneTaskCount(),
                        row.doingTaskCount(),
                        row.overdueTaskCount(),
                        e(row.lastActivityAt()),
                        e(row.comment())))
                .collect(Collectors.joining()) + "</div>";
    }

    private String checkTaskTableHtml(List<TaskCheckRow> rows) {
        if (rows.isEmpty()) {
            return empty("현재 기준으로 마감일 또는 의존관계상 추가 확인이 필요한 태스크가 없습니다.");
        }
        return rows.stream()
                .map(row -> """
                        <div class="checkCard">
                          <h4>%s <span class="badge">%s</span></h4>
                          <p><b>담당자</b> %s · <b>마감일</b> %s</p>
                          <p><b>확인 사유</b> %s</p>
                          <p><b>권장 조치</b> %s</p>
                        </div>
                        """.formatted(
                        e(row.title()),
                        e(row.statusLabel()),
                        e(row.assigneeName()),
                        e(row.dueDate()),
                        e(row.checkReason()),
                        e(row.suggestedAction())))
                .collect(Collectors.joining());
    }

    private String meetingHtml(List<MeetingSummary> meetings) {
        if (meetings.isEmpty()) {
            return empty("리포트 기간 내 등록된 회의가 없습니다.");
        }
        return meetings.stream()
                .map(meeting -> """
                        <div class="card">
                          <h4>%s</h4>
                          <p><b>일시</b> %s</p>
                          <p><b>참석자</b> %s</p>
                          <p><b>안건</b> %s</p>
                          <p><b>회의 노트</b> %s</p>
                          <p><b>결정 사항</b> %s</p>
                          <p><b>액션 아이템</b> %s</p>
                        </div>
                        """.formatted(
                        e(meeting.title()),
                        e(meeting.time()),
                        e(blankTo(meeting.attendees(), "등록된 참석자 정보가 없습니다.")),
                        e(meeting.agenda()),
                        e("-".equals(meeting.content()) ? "회의 노트가 등록되지 않았습니다." : meeting.content()),
                        e(meeting.decisions().isEmpty() ? "등록된 결정 사항이 없습니다." : String.join(", ", meeting.decisions())),
                        e(meeting.actionItems().isEmpty() ? "등록된 액션 아이템이 없습니다." : String.join(", ", meeting.actionItems()))))
                .collect(Collectors.joining());
    }

    private String riskHtml(List<RiskCheckRow> risks) {
        if (risks.isEmpty()) {
            return empty("현재 등록된 확인 필요 항목이 없습니다.");
        }
        return risks.stream()
                .map(risk -> """
                        <div class="card avoid">
                          <h4><span class="badge">%s</span> %s</h4>
                          <p><b>사유</b> %s</p>
                          <p><b>관련 태스크</b> %s</p>
                          <p><b>제안 액션</b> %s</p>
                        </div>
                        """.formatted(
                        e(risk.severityLabel()),
                        e(risk.title()),
                        e(risk.reason()),
                        e(risk.relatedTaskTitles().isEmpty() ? "직접 연결된 태스크 정보가 없습니다." : String.join(", ", risk.relatedTaskTitles())),
                        e(risk.suggestedActions().isEmpty() ? "추가 확인이 필요합니다." : String.join(", ", risk.suggestedActions()))))
                .collect(Collectors.joining());
    }

    private String nextActionTableHtml(List<NextActionRow> rows) {
        if (rows.isEmpty()) {
            return empty("현재 자동으로 도출할 수 있는 다음 액션이 없습니다.");
        }
        return rows.stream()
                .map(row -> """
                        <div class="actionCard">
                          <h4><span class="badge">%s</span> %s</h4>
                          <p><b>담당자</b> %s · <b>출처</b> %s · <b>기준일</b> %s</p>
                        </div>
                        """.formatted(
                        e(row.priorityLabel()),
                        e(row.action()),
                        e(row.ownerName()),
                        e(row.sourceLabel()),
                        e(row.dueDate())))
                .collect(Collectors.joining());
    }

    private String activityTableHtml(List<ActivityLogRow> rows) {
        if (rows.isEmpty()) {
            return empty("리포트 기간 내 활동 로그가 없습니다.");
        }
        var body = rows.stream()
                .map(row -> """
                        <tr>
                          <td>%s</td><td>%s</td><td>%s</td>
                        </tr>
                        """.formatted(e(row.at()), e(row.actor()), e(row.summary())))
                .collect(Collectors.joining());
        return table("일시", "사용자", "활동", body);
    }

    private String table(String header1, String header2, String header3, String rows) {
        return table(List.of(header1, header2, header3), rows);
    }

    private String table(String header1, String header2, String header3, String header4, String header5, String rows) {
        return table(List.of(header1, header2, header3, header4, header5), rows);
    }

    private String table(String header1, String header2, String header3, String header4, String header5, String header6, String rows) {
        return table(List.of(header1, header2, header3, header4, header5, header6), rows);
    }

    private String table(String header1, String header2, String header3, String header4, String header5, String header6, String header7, String header8, String rows) {
        return table(List.of(header1, header2, header3, header4, header5, header6, header7, header8), rows);
    }

    private String table(List<String> headers, String rows) {
        return """
                <table>
                  <thead><tr>%s</tr></thead>
                  <tbody>%s</tbody>
                </table>
                """.formatted(headers.stream().map(header -> "<th>" + e(header) + "</th>").collect(Collectors.joining()), rows);
    }

    private String empty(String message) {
        return "<p class=\"empty\">" + e(message) + "</p>";
    }

    private String css() {
        return """
                @page { size: A4; margin: 18mm 16mm; @bottom-center { content: "TeamPulse Report · " counter(page); font-size: 9px; color: #7A8194; } }
                * { box-sizing: border-box; }
                body { font-family: ReportSans, 'Malgun Gothic', 'Noto Sans KR', sans-serif; color: #1F2437; font-size: 11px; line-height: 1.55; }
                h1, h2, h3, h4, p { margin: 0; }
                h1 { font-size: 28px; letter-spacing: 0; color: #151A2E; }
                h2 { font-size: 20px; margin: 4px 0 8px; }
                h3 { font-size: 17px; margin-bottom: 12px; padding-bottom: 7px; border-bottom: 2px solid #E7EAF3; color: #151A2E; }
                h4 { font-size: 13px; margin-bottom: 6px; }
                section { page-break-inside: avoid; margin-top: 22px; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; page-break-inside: auto; }
                thead { display: table-header-group; }
                tr { page-break-inside: avoid; }
                th { background: #F1F4FA; color: #30384F; text-align: left; font-weight: 700; border: 1px solid #DDE3EF; padding: 6px 7px; }
                td { border: 1px solid #E5E9F2; padding: 6px 7px; vertical-align: top; }
                .cover { page-break-after: always; }
                .topline, .hero, .progressLayout { display: table; width: 100%; }
                .topline > div, .hero > div, .progressLayout > div, .progressLayout > table { display: table-cell; vertical-align: top; }
                .subtitle { color: #626B82; margin-top: 4px; font-size: 13px; }
                .status { border: 1px solid #D9DEEA; border-radius: 10px; padding: 10px 12px; min-width: 145px; text-align: right; }
                .status span, .eyebrow, .metric span, .progressBox span, .infoGrid span { color: #6D7488; font-size: 10px; display: block; }
                .status strong { display: block; font-size: 13px; margin: 3px 0; }
                .status small, .metric small, .progressBox small { color: #7B8498; }
                .hero { margin-top: 30px; background: #F7F9FC; border: 1px solid #E3E8F1; border-radius: 14px; padding: 20px; }
                .progressBox { width: 190px; padding: 14px; border-radius: 12px; background: #FFFFFF; border: 1px solid #E1E6F0; }
                .progressBox.small { width: 210px; }
                .progressBox strong { display: block; font-size: 30px; color: #4B5FE3; margin: 8px 0; }
                .bar { height: 8px; background: #E8ECF5; border-radius: 999px; overflow: hidden; margin-bottom: 7px; }
                .bar i { display: block; height: 8px; background: #4B5FE3; border-radius: 999px; }
                .summary { margin-top: 18px; padding: 14px 16px; border-left: 4px solid #4B5FE3; background: #FAFBFE; }
                .summary p { margin: 4px 0; }
                .metrics { margin-top: 18px; }
                .metric { display: inline-block; width: 31.5%; margin: 0 1% 8px 0; vertical-align: top; border: 1px solid #E1E6F0; border-radius: 12px; padding: 12px; min-height: 74px; }
                .metric strong { display: block; font-size: 22px; margin: 5px 0; color: #151A2E; }
                .infoGrid div { display: inline-block; width: 48%; margin: 0 1% 8px 0; vertical-align: top; border: 1px solid #E4E8F1; border-radius: 9px; padding: 9px 10px; }
                .note { margin-top: 10px; padding: 10px 12px; background: #FAFBFE; border: 1px solid #E4E8F1; border-radius: 9px; }
                .badge { display: inline-block; padding: 2px 7px; border-radius: 999px; background: #EEF1FF; color: #4050C8; font-weight: 700; font-size: 10px; }
                .card { border: 1px solid #E1E6F0; border-radius: 12px; padding: 11px 12px; margin: 8px 0; page-break-inside: avoid; }
                .card p { margin: 4px 0; }
                .empty { background: #F8FAFD; color: #697286; border: 1px dashed #D9DFEB; border-radius: 10px; padding: 12px; }
                .memberCards { margin-top: 8px; }
                .memberCard { display: inline-block; width: 48%; margin: 0 1% 10px 0; vertical-align: top; border: 1px solid #E1E6F0; border-radius: 12px; padding: 11px 12px; min-height: 130px; page-break-inside: avoid; }
                .memberCard p, .checkCard p, .actionCard p { margin: 5px 0; }
                .miniStats { border-top: 1px solid #EEF1F7; border-bottom: 1px solid #EEF1F7; padding: 6px 0; margin: 7px 0; }
                .miniStats span { display: inline-block; width: 48%; color: #687084; font-size: 10px; }
                .miniStats b { color: #1F2437; }
                .checkCard, .actionCard { border: 1px solid #E1E6F0; border-radius: 12px; padding: 10px 12px; margin: 8px 0; page-break-inside: avoid; }
                .badge.muted { background: #F1F4FA; color: #5E667B; }
                """;
    }

    private Optional<File> findFontFile() {
        var candidates = new ArrayList<String>();
        if (!configuredFontPath.isBlank()) {
            candidates.add(configuredFontPath);
        }
        candidates.add("C:\\Windows\\Fonts\\malgun.ttf");
        candidates.add("C:\\Windows\\Fonts\\malgunbd.ttf");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
        return candidates.stream()
                .map(File::new)
                .filter(File::isFile)
                .findFirst();
    }

    private int countByStatus(List<TaskView> tasks, TaskStatus status) {
        return (int) tasks.stream().filter(task -> task.status() == status).count();
    }

    private boolean isOverdue(TaskView task, LocalDate baselineDate) {
        return task.status() != TaskStatus.DONE && parseDateOrMax(task.dueDate()).isBefore(baselineDate);
    }

    private boolean isDueSoon(TaskView task, LocalDate baselineDate) {
        var dueDate = parseDateOrMax(task.dueDate());
        return task.status() != TaskStatus.DONE
                && !dueDate.isBefore(baselineDate)
                && !dueDate.isAfter(baselineDate.plusDays(3));
    }

    private boolean isBlocked(TaskView task) {
        return task.status() != TaskStatus.DONE && task.blockers() != null && !task.blockers().isEmpty();
    }

    private String checkReason(TaskView task, LocalDate baselineDate) {
        if (isOverdue(task, baselineDate)) {
            return "마감일이 지났으나 완료 상태가 아닙니다.";
        }
        if (isBlocked(task)) {
            return "의존관계 또는 차단 요소 확인이 필요합니다.";
        }
        return "마감일이 임박해 진행 상태 확인이 필요합니다.";
    }

    private String suggestedAction(TaskView task, LocalDate baselineDate) {
        if (isOverdue(task, baselineDate)) {
            return "담당자와 실제 진행 상태를 확인합니다.";
        }
        if (isBlocked(task)) {
            return "선행 작업 완료 여부와 진행 순서를 확인합니다.";
        }
        return "마감 전 필요한 후속 작업을 점검합니다.";
    }

    private String memberComment(int assigned, int done, int doing, int overdue, boolean hasRecentActivity) {
        if (assigned == 0) {
            return "현재 담당 태스크가 등록되어 있지 않습니다.";
        }
        if (overdue > 0) {
            return "마감일 기준 확인이 필요한 태스크가 있습니다.";
        }
        if (done == assigned) {
            return "담당 태스크 대부분이 완료된 상태입니다.";
        }
        if (doing > 0) {
            return "진행 중인 태스크가 있어 후속 상태 업데이트가 필요합니다.";
        }
        if (!hasRecentActivity) {
            return "리포트 기간 내 활동 로그가 확인되지 않았습니다.";
        }
        return "등록된 태스크 기준으로 진행 상태를 확인할 수 있습니다.";
    }

    private String summaryLine1(int total, int done) {
        if (total == 0) {
            return "등록된 태스크가 없어 진행률을 계산할 수 없습니다.";
        }
        return "전체 " + total + "개 태스크 중 " + done + "개가 완료되었습니다.";
    }

    private String summaryLine2(int doing, int checkTasks) {
        return "현재 " + doing + "개 태스크가 진행 중이며, " + checkTasks + "개 항목은 진행 상태 확인이 필요합니다.";
    }

    private String summaryLine3(List<MeetingView> meetings) {
        var actionItemCount = meetings.stream().mapToInt(meeting -> meeting.actionItems().size()).sum();
        if (meetings.isEmpty()) {
            return "리포트 기간 내 등록된 회의가 없습니다.";
        }
        return "등록된 회의 액션 아이템 " + actionItemCount + "건을 다음 작업 목록에 반영했습니다.";
    }

    private Optional<LocalDate> reportRangeEnd(String range) {
        if (range == null || range.isBlank() || !range.contains("~")) {
            return Optional.empty();
        }
        var end = range.substring(range.indexOf('~') + 1).trim();
        try {
            return Optional.of(LocalDate.parse(end));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private LocalDate parseDateOrMax(String value) {
        if (value == null || value.isBlank()) {
            return MAX_DATE;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            return MAX_DATE;
        }
    }

    private LocalDateTime parseDateTimeOrMin(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.MIN;
        }
        try {
            return value.contains("T")
                    ? LocalDateTime.parse(value)
                    : LocalDateTime.parse(value.replace(" ", "T"));
        } catch (DateTimeParseException exception) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                return LocalDateTime.MIN;
            }
        }
    }

    private String statusLabel(TaskStatus status) {
        return switch (status) {
            case TODO -> "예정";
            case DOING -> "진행 중";
            case DONE -> "완료";
        };
    }

    private String statusLabel(String status) {
        return "READY".equalsIgnoreCase(status) ? "생성 완료" : textOrDash(status);
    }

    private String severityLabel(RiskSeverity severity) {
        return switch (severity) {
            case INFO -> "낮음";
            case WARNING -> "중간";
            case CRITICAL -> "높음";
        };
    }

    private String riskTitleLabel(String title) {
        return switch (textOrDash(title)) {
            case "진행 정체" -> "진행 정체 확인";
            case "일정 지연 위험" -> "일정 확인 필요";
            case "병목 구간" -> "병목 확인 필요";
            case "역할 편중" -> "작업 분배 확인";
            default -> textOrDash(title);
        };
    }

    private String checkSourceLabel(TaskCheckRow task) {
        if (task.checkReason().contains("마감일이 지났으나")) {
            return "마감일 기준 확인 필요";
        }
        if (task.checkReason().contains("의존관계")) {
            return "의존관계 확인 필요";
        }
        return "마감 임박 확인";
    }

    private String roleLabel(TeamRole role) {
        return role == TeamRole.LEADER ? "리더" : "팀원";
    }

    private List<String> cleanList(List<String> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String activitySummaryLabel(String summary) {
        var value = textOrDash(summary);
        if (value.equals("Report draft refreshed.")) {
            return "리포트 초안이 갱신되었습니다.";
        }
        if (value.endsWith(" meeting saved.")) {
            return "\"" + value.substring(0, value.length() - " meeting saved.".length()) + "\" 회의록이 저장되었습니다.";
        }
        if (value.startsWith("Dependency added to ") && value.endsWith(".")) {
            return "\"" + value.substring("Dependency added to ".length(), value.length() - 1) + "\" 태스크에 의존관계가 추가되었습니다.";
        }
        if (value.endsWith(" moved to DONE.")) {
            return "\"" + value.substring(0, value.length() - " moved to DONE.".length()) + "\" 태스크가 완료 상태로 변경되었습니다.";
        }
        if (value.endsWith(" moved to DOING.")) {
            return "\"" + value.substring(0, value.length() - " moved to DOING.".length()) + "\" 태스크가 진행 중 상태로 변경되었습니다.";
        }
        if (value.endsWith(" moved to TODO.")) {
            return "\"" + value.substring(0, value.length() - " moved to TODO.".length()) + "\" 태스크가 예정 상태로 변경되었습니다.";
        }
        if (value.endsWith(" created.")) {
            return "\"" + value.substring(0, value.length() - " created.".length()) + "\" 항목이 생성되었습니다.";
        }
        return value;
    }

    private String textOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String e(String value) {
        return textOrDash(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record ReportPdfView(
            ReportMeta report,
            ProjectOverview project,
            ProgressSummary progress,
            List<MetricCard> metricCards,
            List<MemberWorkloadRow> memberWorkloads,
            List<TaskRow> taskRows,
            List<TaskCheckRow> delayedOrBlockedTasks,
            List<MeetingSummary> meetings,
            List<RiskCheckRow> risks,
            List<NextActionRow> nextActions,
            List<ActivityLogRow> activityLogs
    ) {
    }

    private record ReportMeta(long reportId, String statusLabel, String generatedAt, String range, String baselineDate) {
    }

    private record ProjectOverview(long projectId, String projectName, String subject, String description, String startDate, String endDate, String semester) {
    }

    private record ProgressSummary(int totalTasks, int todoTasks, int doingTasks, int doneTasks, int overdueTasks, int blockedTasks, int dueSoonTasks, int checkTasks, Integer completionRate, String summaryLine1, String summaryLine2, String summaryLine3) {
    }

    private record MetricCard(String label, String value, String description) {
    }

    private record MemberWorkloadRow(long memberId, String name, String roleLabel, int assignedTaskCount, int doneTaskCount, int doingTaskCount, int overdueTaskCount, String lastActivityAt, String comment) {
    }

    private record TaskRow(long taskId, String title, String assigneeName, String statusLabel, String dueDate, String dependencyLabel) {
    }

    private record TaskCheckRow(long taskId, String title, String assigneeName, String dueDate, String statusLabel, String checkReason, String suggestedAction) {
    }

    private record MeetingSummary(long meetingId, String title, String time, String attendees, String agenda, String content, List<String> decisions, List<String> actionItems) {
    }

    private record RiskCheckRow(long riskId, String title, String severityLabel, String reason, List<String> relatedTaskTitles, List<String> suggestedActions) {
    }

    private record NextActionRow(String priorityLabel, String action, String ownerName, String sourceLabel, String dueDate) {
    }

    private record ActivityLogRow(String at, String actor, String summary) {
    }
}
