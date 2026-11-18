<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.protrack.model.Task" %>
<%@ page import="com.protrack.model.TaskStatus" %>
<%@ page import="com.protrack.model.TaskPriority" %>
<%@ page import="com.protrack.model.User" %>
<%@ page import="com.protrack.model.Role" %>
<%@ page import="com.protrack.util.DateUtil" %>
<%@ page import="com.protrack.util.ValidationUtil" %>
<%!
    // Helper method to render a modern Kanban Task Card
    private String renderKanbanCard(Task t, boolean isManagerOrAdmin, String contextPath) {
        StringBuilder sb = new StringBuilder();
        String titleEsc = ValidationUtil.escapeXml(t.getTaskTitle());
        String descEsc = t.getDescription() != null ? ValidationUtil.escapeXml(t.getDescription()) : "";
        String projEsc = t.getProjectName() != null ? ValidationUtil.escapeXml(t.getProjectName()) : "Project";
        String assignee = t.getAssignedToName() != null ? ValidationUtil.escapeXml(t.getAssignedToName()) : "Unassigned";
        String initial = t.getAssignedToName() != null && !t.getAssignedToName().isEmpty() 
                ? ValidationUtil.escapeXml(t.getAssignedToName().substring(0, 1).toUpperCase()) : "?";

        String dueClass = "";
        String dueBadge = "";
        if (t.isOverdue()) {
            dueClass = "overdue";
            dueBadge = "<i class=\"bi bi-alarm\"></i> Overdue";
        } else if (t.isDueToday()) {
            dueClass = "due-today";
            dueBadge = "<i class=\"bi bi-clock-history\"></i> Due Today";
        }

        sb.append("<div class=\"kanban-card\" draggable=\"true\" data-id=\"").append(t.getId()).append("\" ")
          .append("data-status=\"").append(t.getStatus().name()).append("\" ")
          .append("ondragstart=\"handleKanbanDragStart(event)\" ondragend=\"handleKanbanDragEnd(event)\">\n");

        // Top Row: Priority Badge & Project Tag
        sb.append("  <div class=\"kanban-card-top\">\n")
          .append("    <span class=\"badge badge-custom ").append(t.getPriority().getBadgeClass()).append("\">")
          .append(t.getPriority().getLabel()).append("</span>\n")
          .append("    <span class=\"badge bg-light text-muted border text-truncate\" style=\"max-width: 140px; font-size: 0.7rem;\">")
          .append(projEsc).append("</span>\n")
          .append("  </div>\n");

        // Middle: Title & Description
        sb.append("  <div class=\"kanban-card-title\">").append(titleEsc).append("</div>\n");
        if (!descEsc.isEmpty()) {
            sb.append("  <div class=\"kanban-card-desc\">").append(descEsc).append("</div>\n");
        }

        // Time Tracking & Productivity Progress Bar
        sb.append("  <div class=\"kanban-time-track mb-2\">\n")
          .append("    <div class=\"d-flex justify-content-between align-items-center mb-1\" style=\"font-size: 0.7rem;\">\n")
          .append("      <span class=\"text-muted\"><i class=\"bi bi-clock-history me-1\"></i>")
          .append(String.format("%.1f", t.getLoggedHours())).append(" / ").append(String.format("%.1f", t.getEstimatedHours())).append("h</span>\n");
        if (t.isOverEstimate()) {
            sb.append("      <span class=\"badge bg-danger text-white py-0 px-1\" style=\"font-size: 0.65rem;\">+")
              .append(String.format("%.1f", t.getOvertimeHours())).append("h Over</span>\n");
        } else {
            sb.append("      <span class=\"fw-bold text-dark\">").append(t.getTimeProgressPercentage()).append("%</span>\n");
        }
        sb.append("    </div>\n")
          .append("    <div class=\"progress\" style=\"height: 4px; background-color: #e9ecef; border-radius: 2px;\">\n")
          .append("      <div class=\"progress-bar ").append(t.getTimeProgressBarClass()).append("\" style=\"width: ")
          .append(t.getDisplayProgressPercentage()).append("%;\"></div>\n")
          .append("    </div>\n")
          .append("  </div>\n");

        // Bottom Row: Assignee & Due Date
        sb.append("  <div class=\"kanban-card-footer\">\n")
          .append("    <div class=\"kanban-assignee\">\n")
          .append("      <div class=\"kanban-avatar\">").append(initial).append("</div>\n")
          .append("      <span class=\"text-truncate\" style=\"max-width: 95px;\">").append(assignee).append("</span>\n")
          .append("    </div>\n");

        if (t.getDueDate() != null) {
            sb.append("    <div class=\"kanban-due-date ").append(dueClass).append("\" title=\"Due date: ").append(DateUtil.formatDisplayDate(t.getDueDate())).append("\">\n")
              .append("      <i class=\"bi bi-calendar-event\"></i>\n")
              .append("      <span>").append(DateUtil.formatDisplayDate(t.getDueDate())).append("</span>\n")
              .append("    </div>\n");
        }
        sb.append("  </div>\n");

        // Quick Actions Row
        sb.append("  <div class=\"d-flex justify-content-between align-items-center mt-2 pt-2 border-top border-light\">\n")
          .append("    <span class=\"badge badge-custom kanban-status-badge ").append(t.getStatus().getBadgeClass()).append("\" style=\"font-size: 0.7rem;\">")
          .append(t.getStatus().getLabel()).append("</span>\n")
          .append("    <div class=\"d-flex gap-1\">\n");

        if (isManagerOrAdmin) {
            sb.append("      <button type=\"button\" class=\"btn btn-sm btn-outline-custom py-0 px-1\" title=\"Log Time\" ")
              .append("data-bs-toggle=\"modal\" data-bs-target=\"#logTimeModal\" ")
              .append("data-id=\"").append(t.getId()).append("\" ")
              .append("data-title=\"").append(titleEsc).append("\" ")
              .append("data-logged=\"").append(String.format("%.1f", t.getLoggedHours())).append("\" ")
              .append("data-estimated=\"").append(String.format("%.1f", t.getEstimatedHours())).append("\">\n")
              .append("        <i class=\"bi bi-stopwatch text-primary\" style=\"font-size: 0.75rem;\"></i>\n")
              .append("      </button>\n")
              .append("      <button type=\"button\" class=\"btn btn-sm btn-outline-custom py-0 px-1\" title=\"Edit Task\" ")
              .append("data-bs-toggle=\"modal\" data-bs-target=\"#editTaskModal\" ")
              .append("data-id=\"").append(t.getId()).append("\" ")
              .append("data-title=\"").append(titleEsc).append("\" ")
              .append("data-desc=\"").append(descEsc).append("\" ")
              .append("data-project=\"").append(t.getProjectId()).append("\" ")
              .append("data-assigned=\"").append(t.getAssignedTo() != null ? t.getAssignedTo() : "").append("\" ")
              .append("data-priority=\"").append(t.getPriority().name()).append("\" ")
              .append("data-status=\"").append(t.getStatus().name()).append("\" ")
              .append("data-start=\"").append(DateUtil.formatInputDate(t.getStartDate())).append("\" ")
              .append("data-due=\"").append(DateUtil.formatInputDate(t.getDueDate())).append("\" ")
              .append("data-estimated=\"").append(t.getEstimatedHours()).append("\">\n")
              .append("        <i class=\"bi bi-pencil\" style=\"font-size: 0.75rem;\"></i>\n")
              .append("      </button>\n")
              .append("      <button type=\"button\" class=\"btn btn-sm btn-outline-custom py-0 px-1 text-danger\" title=\"Delete Task\" ")
              .append("data-bs-toggle=\"modal\" data-bs-target=\"#deleteConfirmModal\" ")
              .append("data-id=\"").append(t.getId()).append("\" ")
              .append("data-name=\"").append(titleEsc).append("\" ")
              .append("data-action=\"").append(contextPath).append("/manager/tasks\">\n")
              .append("        <i class=\"bi bi-trash3\" style=\"font-size: 0.75rem;\"></i>\n")
              .append("      </button>\n");
        } else {
            sb.append("      <button type=\"button\" class=\"btn btn-sm btn-outline-custom py-0 px-1\" title=\"Log Time\" ")
              .append("data-bs-toggle=\"modal\" data-bs-target=\"#logTimeModal\" ")
              .append("data-id=\"").append(t.getId()).append("\" ")
              .append("data-title=\"").append(titleEsc).append("\" ")
              .append("data-logged=\"").append(String.format("%.1f", t.getLoggedHours())).append("\" ")
              .append("data-estimated=\"").append(String.format("%.1f", t.getEstimatedHours())).append("\">\n")
              .append("        <i class=\"bi bi-stopwatch text-primary\" style=\"font-size: 0.75rem;\"></i>\n")
              .append("      </button>\n");
            // Team Member Quick Transition Dropdown
            sb.append("      <div class=\"dropdown\">\n")
              .append("        <button class=\"btn btn-sm btn-outline-custom py-0 px-2 dropdown-toggle\" type=\"button\" data-bs-toggle=\"dropdown\" style=\"font-size: 0.7rem;\">\n")
              .append("          Move <i class=\"bi bi-chevron-down ms-1\"></i>\n")
              .append("        </button>\n")
              .append("        <ul class=\"dropdown-menu dropdown-menu-end shadow-sm small\">\n")
              .append("          <li><a class=\"dropdown-item\" href=\"#\" onclick=\"quickTransitionTaskStatus(").append(t.getId()).append(", 'TODO'); return false;\"><i class=\"bi bi-circle me-2 text-secondary\"></i>To Do</a></li>\n")
              .append("          <li><a class=\"dropdown-item\" href=\"#\" onclick=\"quickTransitionTaskStatus(").append(t.getId()).append(", 'IN_PROGRESS'); return false;\"><i class=\"bi bi-arrow-repeat me-2 text-primary\"></i>In Progress</a></li>\n")
              .append("          <li><a class=\"dropdown-item\" href=\"#\" onclick=\"quickTransitionTaskStatus(").append(t.getId()).append(", 'REVIEW'); return false;\"><i class=\"bi bi-eye me-2 text-warning\"></i>In Review</a></li>\n")
              .append("          <li><a class=\"dropdown-item\" href=\"#\" onclick=\"quickTransitionTaskStatus(").append(t.getId()).append(", 'COMPLETED'); return false;\"><i class=\"bi bi-check2-circle me-2 text-success\"></i>Completed</a></li>\n")
              .append("        </ul>\n")
              .append("      </div>\n");
        }

        sb.append("    </div>\n")
          .append("  </div>\n");

        sb.append("</div>\n");
        return sb.toString();
    }
%>
<%
    @SuppressWarnings("unchecked")
    List<Task> allTasks = (List<Task>) request.getAttribute("tasks");
    User currentUser = (User) session.getAttribute("currentUser");
    Role userRole = currentUser != null ? currentUser.getRole() : Role.TEAM_MEMBER;
    boolean isManagerOrAdmin = (userRole == Role.ADMIN || userRole == Role.PROJECT_MANAGER);
    String contextPath = request.getContextPath();

    List<Task> todoTasks = new ArrayList<>();
    List<Task> inProgressTasks = new ArrayList<>();
    List<Task> reviewTasks = new ArrayList<>();
    List<Task> completedTasks = new ArrayList<>();

    if (allTasks != null) {
        for (Task t : allTasks) {
            if (t.getStatus() == TaskStatus.TODO) {
                todoTasks.add(t);
            } else if (t.getStatus() == TaskStatus.IN_PROGRESS) {
                inProgressTasks.add(t);
            } else if (t.getStatus() == TaskStatus.REVIEW) {
                reviewTasks.add(t);
            } else if (t.getStatus() == TaskStatus.COMPLETED) {
                completedTasks.add(t);
            }
        }
    }
%>

<div id="kanbanBoardView" class="kanban-board-container d-none">
    <!-- 1. Column: TO DO -->
    <div class="kanban-column kanban-col-todo" data-column-status="TODO">
        <div class="kanban-column-header">
            <div class="kanban-header-title">
                <span class="kanban-status-dot bg-secondary"></span>
                <span>To Do</span>
            </div>
            <span class="kanban-count-badge"><%= todoTasks.size() %></span>
        </div>
        <div class="kanban-column-body" data-status="TODO"
             ondragover="allowKanbanDrop(event)" ondragleave="handleKanbanDragLeave(event)" ondrop="handleKanbanDrop(event, 'TODO')">
            <% if (!todoTasks.isEmpty()) {
                for (Task t : todoTasks) { %>
                    <%= renderKanbanCard(t, isManagerOrAdmin, contextPath) %>
            <%  }
               } else { %>
                <div class="kanban-empty-state">
                    <i class="bi bi-inbox"></i>
                    <span>No tasks in this stage</span>
                </div>
            <% } %>
        </div>
    </div>

    <!-- 2. Column: IN PROGRESS -->
    <div class="kanban-column kanban-col-in-progress" data-column-status="IN_PROGRESS">
        <div class="kanban-column-header">
            <div class="kanban-header-title">
                <span class="kanban-status-dot bg-primary"></span>
                <span>In Progress</span>
            </div>
            <span class="kanban-count-badge"><%= inProgressTasks.size() %></span>
        </div>
        <div class="kanban-column-body" data-status="IN_PROGRESS"
             ondragover="allowKanbanDrop(event)" ondragleave="handleKanbanDragLeave(event)" ondrop="handleKanbanDrop(event, 'IN_PROGRESS')">
            <% if (!inProgressTasks.isEmpty()) {
                for (Task t : inProgressTasks) { %>
                    <%= renderKanbanCard(t, isManagerOrAdmin, contextPath) %>
            <%  }
               } else { %>
                <div class="kanban-empty-state">
                    <i class="bi bi-inbox"></i>
                    <span>No tasks in this stage</span>
                </div>
            <% } %>
        </div>
    </div>

    <!-- 3. Column: IN REVIEW -->
    <div class="kanban-column kanban-col-review" data-column-status="REVIEW">
        <div class="kanban-column-header">
            <div class="kanban-header-title">
                <span class="kanban-status-dot bg-warning"></span>
                <span>In Review</span>
            </div>
            <span class="kanban-count-badge"><%= reviewTasks.size() %></span>
        </div>
        <div class="kanban-column-body" data-status="REVIEW"
             ondragover="allowKanbanDrop(event)" ondragleave="handleKanbanDragLeave(event)" ondrop="handleKanbanDrop(event, 'REVIEW')">
            <% if (!reviewTasks.isEmpty()) {
                for (Task t : reviewTasks) { %>
                    <%= renderKanbanCard(t, isManagerOrAdmin, contextPath) %>
            <%  }
               } else { %>
                <div class="kanban-empty-state">
                    <i class="bi bi-inbox"></i>
                    <span>No tasks in this stage</span>
                </div>
            <% } %>
        </div>
    </div>

    <!-- 4. Column: COMPLETED -->
    <div class="kanban-column kanban-col-completed" data-column-status="COMPLETED">
        <div class="kanban-column-header">
            <div class="kanban-header-title">
                <span class="kanban-status-dot bg-success"></span>
                <span>Completed</span>
            </div>
            <span class="kanban-count-badge"><%= completedTasks.size() %></span>
        </div>
        <div class="kanban-column-body" data-status="COMPLETED"
             ondragover="allowKanbanDrop(event)" ondragleave="handleKanbanDragLeave(event)" ondrop="handleKanbanDrop(event, 'COMPLETED')">
            <% if (!completedTasks.isEmpty()) {
                for (Task t : completedTasks) { %>
                    <%= renderKanbanCard(t, isManagerOrAdmin, contextPath) %>
            <%  }
               } else { %>
                <div class="kanban-empty-state">
                    <i class="bi bi-inbox"></i>
                    <span>No tasks in this stage</span>
                </div>
            <% } %>
        </div>
    </div>
</div>
