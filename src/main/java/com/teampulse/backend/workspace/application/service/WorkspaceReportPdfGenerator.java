package com.teampulse.backend.workspace.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.teampulse.backend.domain.task.TaskStatus;
import com.teampulse.backend.workspace.dto.MeetingActionItemView;
import com.teampulse.backend.workspace.dto.MeetingView;
import com.teampulse.backend.workspace.dto.ReportView;
import com.teampulse.backend.workspace.dto.RiskView;
import com.teampulse.backend.workspace.dto.TaskView;
import com.teampulse.backend.workspace.dto.WorkspaceState;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceReportPdfGenerator {

    private static final String REPORT_FONT_RESOURCE = "/fonts/malgun.ttf";
    private static final Color REPORT_INK = new Color(17, 24, 39);
    private static final Color REPORT_MUTED = new Color(107, 114, 128);
    private static final Color REPORT_LINE = new Color(226, 232, 240);
    private static final Color REPORT_PANEL = new Color(248, 250, 252);
    private static final Color REPORT_BRAND = new Color(37, 99, 235);
    private static final Color REPORT_BRAND_DARK = new Color(15, 23, 42);
    private static final Color REPORT_WARNING = new Color(180, 83, 9);
    private static final Color REPORT_DANGER = new Color(185, 28, 28);

    public byte[] generate(WorkspaceState workspace, ReportView report) throws IOException {
        try {
            var output = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 36, 36, 38, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            var baseFont = koreanBaseFont();
            var titleFont = pdfFont(baseFont, 22, Font.BOLD, Color.WHITE);
            var subtitleFont = pdfFont(baseFont, 9, Font.NORMAL, new Color(203, 213, 225));
            var sectionFont = pdfFont(baseFont, 13, Font.BOLD, REPORT_INK);
            var bodyFont = pdfFont(baseFont, 9, Font.NORMAL, REPORT_INK);
            var smallFont = pdfFont(baseFont, 8, Font.NORMAL, REPORT_MUTED);
            var labelFont = pdfFont(baseFont, 8, Font.BOLD, REPORT_MUTED);
            var valueFont = pdfFont(baseFont, 11, Font.BOLD, REPORT_INK);

            addReportHeader(document, workspace, report, titleFont, subtitleFont);
            addProjectSnapshot(document, workspace, report, labelFont, bodyFont);
            addMetricGrid(document, workspace, labelFont, valueFont, smallFont);
            addMembers(document, workspace, sectionFont, bodyFont, smallFont);
            addTaskTable(document, workspace, sectionFont, bodyFont, smallFont, labelFont);
            addMeetingSection(document, workspace, sectionFont, bodyFont, smallFont);
            addRiskSection(document, workspace, sectionFont, bodyFont, smallFont);
            addActivityTable(document, workspace, sectionFont, bodyFont, smallFont, labelFont);

            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IOException("Failed to create report PDF.", exception);
        }
    }

    private Font pdfFont(BaseFont baseFont, float size, int style, Color color) {
        return new Font(baseFont, size, style, color);
    }

    private void addReportHeader(
            Document document,
            WorkspaceState workspace,
            ReportView report,
            Font titleFont,
            Font subtitleFont
    ) throws DocumentException {
        var table = fullWidthTable(1);
        var cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(REPORT_BRAND_DARK);
        cell.setPaddingTop(22);
        cell.setPaddingRight(22);
        cell.setPaddingBottom(20);
        cell.setPaddingLeft(22);

        var title = new Paragraph("TeamPulse Report", titleFont);
        title.setSpacingAfter(8);
        cell.addElement(title);
        cell.addElement(new Paragraph(textOrDash(workspace.team().name()) + " / " + textOrDash(workspace.team().courseName()), subtitleFont));
        cell.addElement(new Paragraph("Generated " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                + " / Range " + textOrDash(report.range())
                + " / Status " + textOrDash(report.status()), subtitleFont));
        table.addCell(cell);
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addProjectSnapshot(
            Document document,
            WorkspaceState workspace,
            ReportView report,
            Font labelFont,
            Font bodyFont
    ) throws DocumentException {
        var table = fullWidthTable(4);
        table.setWidths(new float[]{1.1f, 2.2f, 1.1f, 2.2f});
        addInfoCell(table, "Project", labelFont);
        addInfoCell(table, textOrDash(workspace.team().name()), bodyFont);
        addInfoCell(table, "Subject", labelFont);
        addInfoCell(table, textOrDash(workspace.team().courseName()), bodyFont);
        addInfoCell(table, "Schedule", labelFont);
        addInfoCell(table, textOrDash(workspace.team().startDate()) + " ~ " + textOrDash(workspace.team().dueDate()), bodyFont);
        addInfoCell(table, "Report", labelFont);
        addInfoCell(table, textOrDash(report.range()), bodyFont);
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addMetricGrid(
            Document document,
            WorkspaceState workspace,
            Font labelFont,
            Font valueFont,
            Font smallFont
    ) throws DocumentException {
        var table = fullWidthTable(4);
        table.setWidths(new float[]{1, 1, 1, 1});
        addMetricCell(table, "Tasks", String.valueOf(workspace.tasks().size()),
                "TODO " + countTasksByStatus(workspace, TaskStatus.TODO)
                        + " / DOING " + countTasksByStatus(workspace, TaskStatus.DOING)
                        + " / DONE " + countTasksByStatus(workspace, TaskStatus.DONE), labelFont, valueFont, smallFont);
        addMetricCell(table, "Overdue", String.valueOf(countOverdueTasks(workspace)), "unfinished tasks past due", labelFont, valueFont, smallFont);
        addMetricCell(table, "Meetings", String.valueOf(workspace.meetings().size()), "recorded notes", labelFont, valueFont, smallFont);
        addMetricCell(table, "Risks", String.valueOf(workspace.risks().size()), "active signals", labelFont, valueFont, smallFont);
        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addMembers(Document document, WorkspaceState workspace, Font sectionFont, Font bodyFont, Font smallFont) throws DocumentException {
        addSectionTitle(document, "Members", sectionFont);
        var members = workspace.members().isEmpty()
                ? "No members recorded."
                : workspace.members().stream()
                .map(member -> member.name() + " (" + member.role() + ")")
                .collect(Collectors.joining("   "));
        var paragraph = new Paragraph(members, workspace.members().isEmpty() ? smallFont : bodyFont);
        paragraph.setSpacingAfter(12);
        document.add(paragraph);
    }

    private void addTaskTable(
            Document document,
            WorkspaceState workspace,
            Font sectionFont,
            Font bodyFont,
            Font smallFont,
            Font labelFont
    ) throws DocumentException {
        addSectionTitle(document, "Task Details", sectionFont);
        if (workspace.tasks().isEmpty()) {
            addEmptyState(document, "No tasks recorded.", smallFont);
            return;
        }

        var table = fullWidthTable(5);
        table.setWidths(new float[]{1.1f, 3.4f, 1.6f, 1.3f, 1.2f});
        addHeaderCell(table, "Status", labelFont);
        addHeaderCell(table, "Task", labelFont);
        addHeaderCell(table, "Owner", labelFont);
        addHeaderCell(table, "Due", labelFont);
        addHeaderCell(table, "Priority", labelFont);

        workspace.tasks().stream()
                .sorted(Comparator.comparing(TaskView::dueDate))
                .limit(12)
                .forEach(task -> {
                    addBodyCell(table, task.status().name(), bodyFont);
                    addBodyCell(table, taskTitle(task), bodyFont);
                    addBodyCell(table, textOrDash(task.owner()), bodyFont);
                    addBodyCell(table, textOrDash(task.dueDate()), bodyFont);
                    addBodyCell(table, textOrDash(task.priority()), bodyFont);
                });
        table.setSpacingAfter(14);
        document.add(table);
    }

    private void addMeetingSection(Document document, WorkspaceState workspace, Font sectionFont, Font bodyFont, Font smallFont) throws DocumentException {
        addSectionTitle(document, "Meeting Notes", sectionFont);
        if (workspace.meetings().isEmpty()) {
            addEmptyState(document, "No meetings recorded.", smallFont);
            return;
        }

        for (var meeting : workspace.meetings().stream()
                .sorted(Comparator.comparing(MeetingView::time).reversed())
                .limit(4)
                .toList()) {
            var card = cardTable();
            var cell = cardCell();
            cell.addElement(new Paragraph(textOrDash(meeting.time()) + "  /  " + textOrDash(meeting.title()), bodyFont));
            cell.addElement(new Paragraph("Agenda: " + textOrDash(meeting.agenda()), smallFont));
            if (!meeting.decisions().isEmpty()) {
                cell.addElement(new Paragraph("Decisions: " + String.join(", ", meeting.decisions()), smallFont));
            }
            if (!meeting.actionItems().isEmpty()) {
                cell.addElement(new Paragraph("Action items: " + meeting.actionItems().stream()
                        .map(MeetingActionItemView::content)
                        .collect(Collectors.joining(", ")), smallFont));
            }
            card.addCell(cell);
            card.setSpacingAfter(8);
            document.add(card);
        }
    }

    private void addRiskSection(Document document, WorkspaceState workspace, Font sectionFont, Font bodyFont, Font smallFont) throws DocumentException {
        addSectionTitle(document, "Risk Signals", sectionFont);
        if (workspace.risks().isEmpty()) {
            addEmptyState(document, "No active risk signals.", smallFont);
            return;
        }

        for (var risk : workspace.risks().stream().limit(5).toList()) {
            var card = cardTable();
            var cell = cardCell();
            cell.setBorderColor(riskColor(risk));
            cell.setBorderWidthLeft(4);
            cell.addElement(new Paragraph("[" + risk.severity() + "] " + textOrDash(risk.title()), bodyFont));
            cell.addElement(new Paragraph("Basis: " + textOrDash(risk.body()), smallFont));
            cell.addElement(new Paragraph("Suggested action: " + textOrDash(risk.action()), smallFont));
            card.addCell(cell);
            card.setSpacingAfter(8);
            document.add(card);
        }
    }

    private void addActivityTable(Document document, WorkspaceState workspace, Font sectionFont, Font bodyFont, Font smallFont, Font labelFont) throws DocumentException {
        addSectionTitle(document, "Recent Activity", sectionFont);
        if (workspace.activities().isEmpty()) {
            addEmptyState(document, "No activity logs recorded.", smallFont);
            return;
        }

        var table = fullWidthTable(3);
        table.setWidths(new float[]{1.5f, 1.4f, 4.1f});
        addHeaderCell(table, "Time", labelFont);
        addHeaderCell(table, "Actor", labelFont);
        addHeaderCell(table, "Summary", labelFont);
        workspace.activities().stream()
                .limit(8)
                .forEach(activity -> {
                    addBodyCell(table, textOrDash(activity.at()), smallFont);
                    addBodyCell(table, textOrDash(activity.actor()), bodyFont);
                    addBodyCell(table, textOrDash(activity.summary()), bodyFont);
                });
        document.add(table);
    }

    private PdfPTable fullWidthTable(int columns) {
        var table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        return table;
    }

    private PdfPTable cardTable() {
        return fullWidthTable(1);
    }

    private PdfPCell cardCell() {
        var cell = new PdfPCell();
        cell.setBackgroundColor(REPORT_PANEL);
        cell.setBorderColor(REPORT_LINE);
        cell.setPadding(10);
        return cell;
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        var paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(7);
        document.add(paragraph);
    }

    private void addEmptyState(Document document, String text, Font font) throws DocumentException {
        var paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(12);
        document.add(paragraph);
    }

    private void addInfoCell(PdfPTable table, String text, Font font) {
        var cell = baseCell(text, font);
        cell.setBackgroundColor(REPORT_PANEL);
        table.addCell(cell);
    }

    private void addMetricCell(PdfPTable table, String label, String value, String helper, Font labelFont, Font valueFont, Font smallFont) {
        var cell = new PdfPCell();
        cell.setBorderColor(REPORT_LINE);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(11);
        var labelParagraph = new Paragraph(label, labelFont);
        labelParagraph.setSpacingAfter(5);
        var valueParagraph = new Paragraph(value, valueFont);
        valueParagraph.setSpacingAfter(4);
        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);
        cell.addElement(new Paragraph(helper, smallFont));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        var cell = baseCell(text, font);
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        table.addCell(baseCell(text, font));
    }

    private PdfPCell baseCell(String text, Font font) {
        var cell = new PdfPCell(new Phrase(textOrDash(text), font));
        cell.setBorderColor(REPORT_LINE);
        cell.setPaddingTop(7);
        cell.setPaddingRight(8);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String taskTitle(TaskView task) {
        var details = new ArrayList<String>();
        details.add("#" + task.id() + " " + textOrDash(task.title()));
        if (!task.blockers().isEmpty()) {
            details.add("Blockers: " + String.join(", ", task.blockers()));
        }
        if (task.note() != null && !task.note().isBlank()) {
            details.add("Note: " + task.note());
        }
        return String.join("\n", details);
    }

    private Color riskColor(RiskView risk) {
        return switch (risk.severity()) {
            case CRITICAL -> REPORT_DANGER;
            case WARNING -> REPORT_WARNING;
            case INFO -> REPORT_BRAND;
        };
    }

    private BaseFont koreanBaseFont() throws IOException, DocumentException {
        try (var fontStream = WorkspaceReportPdfGenerator.class.getResourceAsStream(REPORT_FONT_RESOURCE)) {
            if (fontStream == null) {
                throw new IOException("Report font resource not found: " + REPORT_FONT_RESOURCE);
            }
            return BaseFont.createFont(
                    "malgun.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontStream.readAllBytes(),
                    null);
        }
    }

    private long countTasksByStatus(WorkspaceState workspace, TaskStatus status) {
        return workspace.tasks().stream()
                .filter(task -> task.status() == status)
                .count();
    }

    private long countOverdueTasks(WorkspaceState workspace) {
        var today = LocalDate.now();
        return workspace.tasks().stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .filter(task -> parseDateOrMax(task.dueDate()).isBefore(today))
                .count();
    }

    private String textOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private LocalDate parseDateOrMax(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.MAX;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return LocalDate.MAX;
        }
    }
}