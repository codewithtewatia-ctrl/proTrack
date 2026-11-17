package com.protrack.service;

import com.protrack.model.Project;
import com.protrack.model.Task;
import com.protrack.util.DateUtil;

import java.util.List;

/**
 * Service generating standardized CSV reports and export summaries
 * for project milestones and overdue tasks according to RFC 4180.
 */
public class ReportExportService {

    // UTF-8 Byte Order Mark for Microsoft Excel and Google Sheets compatibility
    private static final String CSV_BOM = "\uFEFF";

    /**
     * Generates a CSV summary of Project Milestones and progress.
     */
    public String generateProjectMilestonesCsv(List<Project> projects) {
        StringBuilder sb = new StringBuilder(CSV_BOM);
        // Header
        sb.append(escapeCsv("Project ID")).append(",")
          .append(escapeCsv("Project Title")).append(",")
          .append(escapeCsv("Status")).append(",")
          .append(escapeCsv("Manager Name")).append(",")
          .append(escapeCsv("Manager Email")).append(",")
          .append(escapeCsv("Start Date")).append(",")
          .append(escapeCsv("End Date")).append(",")
          .append(escapeCsv("Total Tasks")).append(",")
          .append(escapeCsv("Completed Tasks")).append(",")
          .append(escapeCsv("Pending Tasks")).append(",")
          .append(escapeCsv("Milestone Progress (%)")).append(",")
          .append(escapeCsv("Description")).append("\r\n");

        if (projects != null) {
            for (Project p : projects) {
                sb.append(p.getId()).append(",")
                  .append(escapeCsv(p.getTitle())).append(",")
                  .append(escapeCsv(p.getStatus())).append(",")
                  .append(escapeCsv(p.getManagerName() != null ? p.getManagerName() : "Unassigned")).append(",")
                  .append(escapeCsv(p.getManagerEmail() != null ? p.getManagerEmail() : "")).append(",")
                  .append(escapeCsv(p.getStartDate() != null ? p.getStartDate().toString() : "")).append(",")
                  .append(escapeCsv(p.getEndDate() != null ? p.getEndDate().toString() : "")).append(",")
                  .append(p.getTotalTasks()).append(",")
                  .append(p.getCompletedTasks()).append(",")
                  .append(p.getPendingTasks()).append(",")
                  .append(p.getProgressPercentage()).append("%").append(",")
                  .append(escapeCsv(p.getDescription() != null ? p.getDescription() : "")).append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Generates a CSV summary of Overdue Tasks across projects.
     */
    public String generateOverdueTasksCsv(List<Task> overdueTasks) {
        StringBuilder sb = new StringBuilder(CSV_BOM);
        // Header
        sb.append(escapeCsv("Task ID")).append(",")
          .append(escapeCsv("Task Title")).append(",")
          .append(escapeCsv("Project Name")).append(",")
          .append(escapeCsv("Priority")).append(",")
          .append(escapeCsv("Status")).append(",")
          .append(escapeCsv("Assigned To")).append(",")
          .append(escapeCsv("Assignee Email")).append(",")
          .append(escapeCsv("Due Date")).append(",")
          .append(escapeCsv("Days Overdue")).append(",")
          .append(escapeCsv("Created By")).append(",")
          .append(escapeCsv("Description")).append("\r\n");

        if (overdueTasks != null) {
            for (Task t : overdueTasks) {
                long daysOverdue = Math.abs(DateUtil.getDaysRemaining(t.getDueDate()));
                sb.append(t.getId()).append(",")
                  .append(escapeCsv(t.getTaskTitle())).append(",")
                  .append(escapeCsv(t.getProjectName() != null ? t.getProjectName() : "")).append(",")
                  .append(escapeCsv(t.getPriority() != null ? t.getPriority().name() : "")).append(",")
                  .append(escapeCsv(t.getStatus() != null ? t.getStatus().name() : "")).append(",")
                  .append(escapeCsv(t.getAssignedToName() != null ? t.getAssignedToName() : "Unassigned")).append(",")
                  .append(escapeCsv(t.getAssignedToEmail() != null ? t.getAssignedToEmail() : "")).append(",")
                  .append(escapeCsv(t.getDueDate() != null ? t.getDueDate().toString() : "")).append(",")
                  .append(daysOverdue).append(",")
                  .append(escapeCsv(t.getCreatedByName() != null ? t.getCreatedByName() : "")).append(",")
                  .append(escapeCsv(t.getDescription() != null ? t.getDescription() : "")).append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Generates a detailed CSV list of Tasks.
     */
    public String generateTasksCsv(List<Task> tasks) {
        StringBuilder sb = new StringBuilder(CSV_BOM);
        // Header
        sb.append(escapeCsv("Task ID")).append(",")
          .append(escapeCsv("Task Title")).append(",")
          .append(escapeCsv("Project Name")).append(",")
          .append(escapeCsv("Priority")).append(",")
          .append(escapeCsv("Status")).append(",")
          .append(escapeCsv("Assigned To")).append(",")
          .append(escapeCsv("Start Date")).append(",")
          .append(escapeCsv("Due Date")).append(",")
          .append(escapeCsv("Deadline Status")).append(",")
          .append(escapeCsv("Created By")).append(",")
          .append(escapeCsv("Description")).append("\r\n");

        if (tasks != null) {
            for (Task t : tasks) {
                sb.append(t.getId()).append(",")
                  .append(escapeCsv(t.getTaskTitle())).append(",")
                  .append(escapeCsv(t.getProjectName() != null ? t.getProjectName() : "")).append(",")
                  .append(escapeCsv(t.getPriority() != null ? t.getPriority().name() : "")).append(",")
                  .append(escapeCsv(t.getStatus() != null ? t.getStatus().name() : "")).append(",")
                  .append(escapeCsv(t.getAssignedToName() != null ? t.getAssignedToName() : "Unassigned")).append(",")
                  .append(escapeCsv(t.getStartDate() != null ? t.getStartDate().toString() : "")).append(",")
                  .append(escapeCsv(t.getDueDate() != null ? t.getDueDate().toString() : "")).append(",")
                  .append(escapeCsv(t.getRelativeDueStatus())).append(",")
                  .append(escapeCsv(t.getCreatedByName() != null ? t.getCreatedByName() : "")).append(",")
                  .append(escapeCsv(t.getDescription() != null ? t.getDescription() : "")).append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a single CSV value according to RFC 4180 specification.
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
