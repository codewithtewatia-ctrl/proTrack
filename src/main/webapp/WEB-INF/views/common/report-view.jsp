<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.protrack.model.Project" %>
<%@ page import="com.protrack.model.Task" %>
<%@ page import="com.protrack.model.User" %>
<%@ page import="com.protrack.util.DateUtil" %>
<%@ page import="com.protrack.util.ValidationUtil" %>
<%
    String reportType = (String) request.getAttribute("reportType");
    if (reportType == null || reportType.trim().isEmpty()) reportType = "projects";
    String reportTitle = (String) request.getAttribute("reportTitle");
    if (reportTitle == null || reportTitle.trim().isEmpty()) reportTitle = "Reports & Analytics Hub";
    Date generatedDate = (Date) request.getAttribute("generatedDate");
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null) currentUser = (User) session.getAttribute("currentUser");
    if (generatedDate == null) generatedDate = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm:ss z");

    @SuppressWarnings("unchecked")
    List<Project> projects = (List<Project>) request.getAttribute("projects");
    @SuppressWarnings("unchecked")
    List<Task> overdueTasks = (List<Task>) request.getAttribute("overdueTasks");
    @SuppressWarnings("unchecked")
    List<Task> tasks = (List<Task>) request.getAttribute("tasks");

    request.setAttribute("pageTitle", "Reports & Exports");
    request.setAttribute("pageSubtitle", "Generate, review and export audit-ready project & task reports");
    request.setAttribute("activeMenu", "reports-exports");
%>

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="../common/navbar.jsp" />

    <div class="content-body">
        <jsp:include page="../common/alerts.jsp" />

        <style>
            .report-paper {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 12px;
                padding: 35px;
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
            }
            .brand-logo {
                width: 36px;
                height: 36px;
                background: linear-gradient(135deg, #6366f1 0%, #4338ca 100%);
                border-radius: 8px;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                color: #fff;
                font-size: 1.2rem;
                margin-right: 10px;
            }
            .kpi-card {
                background-color: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                padding: 16px;
                text-align: center;
            }
            .kpi-value {
                font-size: 1.75rem;
                font-weight: 700;
                color: #0f172a;
            }
            .kpi-label {
                font-size: 0.8rem;
                font-weight: 600;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                color: #64748b;
            }
            .table-report th {
                background-color: #f1f5f9;
                color: #334155;
                font-weight: 600;
                font-size: 0.8rem;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                border-bottom: 2px solid #cbd5e1;
                padding: 10px 12px;
            }
            .table-report td {
                font-size: 0.85rem;
                vertical-align: middle;
                padding: 10px 12px;
                border-bottom: 1px solid #e2e8f0;
            }
            @media print {
                body {
                    background: #ffffff !important;
                    padding: 0 !important;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }
                .sidebar, .top-navbar, footer, .d-print-none {
                    display: none !important;
                }
                .app-wrapper, .main-wrapper, .content-body {
                    margin: 0 !important;
                    padding: 0 !important;
                    width: 100% !important;
                    background: #ffffff !important;
                }
                .report-paper {
                    box-shadow: none !important;
                    border: none !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    max-width: 100% !important;
                }
                @page {
                    size: A4 landscape;
                    margin: 10mm;
                }
            }
        </style>

        <!-- Top Controls & Export Bar (Hidden in Print) -->
        <div class="card border-0 shadow-sm mb-4 d-print-none">
            <div class="card-body py-3 px-4 d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div class="btn-group" role="group" aria-label="Report Type Selector">
                    <a href="<%= request.getContextPath() %>/export?type=projects&format=pdf" target="_self" class="btn btn-sm <%= "projects".equals(reportType) ? "btn-primary" : "btn-outline-custom" %>">
                        <i class="bi bi-folder-check me-1"></i> Project Milestones
                    </a>
                    <a href="<%= request.getContextPath() %>/export?type=overdue&format=pdf" target="_self" class="btn btn-sm <%= "overdue".equals(reportType) ? "btn-primary" : "btn-outline-custom" %>">
                        <i class="bi bi-alarm-fill text-danger me-1"></i> Overdue Tasks
                    </a>
                    <a href="<%= request.getContextPath() %>/export?type=tasks&format=pdf" target="_self" class="btn btn-sm <%= "tasks".equals(reportType) ? "btn-primary" : "btn-outline-custom" %>">
                        <i class="bi bi-check2-square text-success me-1"></i> Tasks Summary
                    </a>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <a href="<%= request.getContextPath() %>/export?type=<%= reportType %>&format=csv" target="_self" class="btn btn-sm btn-outline-success">
                        <i class="bi bi-filetype-csv me-1"></i> Download CSV
                    </a>
                    <button type="button" class="btn btn-sm btn-primary" onclick="window.print()">
                        <i class="bi bi-printer-fill me-1"></i> Print / Save as PDF
                    </button>
                </div>
            </div>
        </div>

        <!-- Printable Report Paper Document Card -->
        <div class="report-paper">
            <!-- Header -->
            <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-4">
                <div>
                    <div class="d-flex align-items-center mb-1">
                        <div class="brand-logo"><i class="bi bi-kanban-fill"></i></div>
                        <h4 class="fw-bold mb-0 text-dark">ProTrack Enterprise</h4>
                    </div>
                    <h5 class="text-primary fw-semibold mt-2 mb-1"><%= ValidationUtil.escapeXml(reportTitle) %></h5>
                    <div class="text-muted small">Generated on <%= sdf.format(generatedDate) %></div>
                </div>
                <div class="text-end">
                    <span class="badge bg-light text-dark border p-2 mb-1 d-inline-block">
                        <i class="bi bi-person-badge text-primary me-1"></i>
                        <strong><%= currentUser != null ? ValidationUtil.escapeXml(currentUser.getFullName()) : "User" %></strong>
                        (<%= currentUser != null ? currentUser.getRole().getDisplayName() : "" %>)
                    </span>
                    <div class="text-muted small">CONFIDENTIAL &bull; FOR INTERNAL USE</div>
                </div>
            </div>

            <% if ("projects".equals(reportType)) { 
                int totalProjects = (projects != null) ? projects.size() : 0;
                int completedCount = 0;
                int totalTasks = 0;
                int completedTasks = 0;
                if (projects != null) {
                    for (Project p : projects) {
                        if ("COMPLETED".equalsIgnoreCase(p.getStatus())) completedCount++;
                        totalTasks += p.getTotalTasks();
                        completedTasks += p.getCompletedTasks();
                    }
                }
                int avgProgress = (totalTasks > 0) ? (int) Math.round(((double) completedTasks / totalTasks) * 100) : 0;
            %>
                <!-- Project Milestones KPI Grid -->
                <div class="row g-3 mb-4">
                    <div class="col-6 col-md-3">
                        <div class="kpi-card">
                            <div class="kpi-value text-primary"><%= totalProjects %></div>
                            <div class="kpi-label">Total Projects</div>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="kpi-card">
                            <div class="kpi-value text-success"><%= completedCount %></div>
                            <div class="kpi-label">Completed Initiatives</div>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="kpi-card">
                            <div class="kpi-value text-info"><%= totalTasks %></div>
                            <div class="kpi-label">Total Tasks Tracked</div>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="kpi-card">
                            <div class="kpi-value text-dark"><%= avgProgress %>%</div>
                            <div class="kpi-label">Overall Completion</div>
                        </div>
                    </div>
                </div>

                <!-- Project Milestones Table -->
                <div class="table-responsive">
                    <table class="table table-report">
                        <thead>
                            <tr>
                                <th style="width: 50px;">ID</th>
                                <th>Project Title</th>
                                <th>Status</th>
                                <th>Manager</th>
                                <th>Timeline</th>
                                <th style="width: 140px;">Tasks Breakdown</th>
                                <th style="width: 140px;">Milestone Progress</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (projects != null && !projects.isEmpty()) {
                                for (Project p : projects) { %>
                                <tr>
                                    <td class="text-muted fw-bold">#<%= p.getId() %></td>
                                    <td>
                                        <div class="fw-bold text-dark"><%= ValidationUtil.escapeXml(p.getTitle()) %></div>
                                        <div class="small text-muted text-truncate" style="max-width: 300px;">
                                            <%= p.getDescription() != null ? ValidationUtil.escapeXml(p.getDescription()) : "" %>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge <%= p.getStatusBadgeClass() %>"><%= p.getStatus() %></span>
                                    </td>
                                    <td>
                                        <div class="fw-medium"><%= p.getManagerName() != null ? ValidationUtil.escapeXml(p.getManagerName()) : "Unassigned" %></div>
                                        <div class="small text-muted"><%= p.getManagerEmail() != null ? ValidationUtil.escapeXml(p.getManagerEmail()) : "" %></div>
                                    </td>
                                    <td>
                                        <div class="small"><i class="bi bi-calendar-event me-1 text-muted"></i><%= DateUtil.formatDisplayDate(p.getStartDate()) %> &rarr; <%= DateUtil.formatDisplayDate(p.getEndDate()) %></div>
                                    </td>
                                    <td>
                                        <span class="small fw-semibold text-success"><%= p.getCompletedTasks() %> Done</span> /
                                        <span class="small text-muted"><%= p.getTotalTasks() %> Total</span>
                                    </td>
                                    <td>
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="progress flex-grow-1" style="height: 8px;">
                                                <div class="progress-bar bg-primary" style="width: <%= p.getProgressPercentage() %>%;"></div>
                                            </div>
                                            <span class="small fw-bold"><%= p.getProgressPercentage() %>%</span>
                                        </div>
                                    </td>
                                </tr>
                            <%  }
                            } else { %>
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-muted">No projects found for the selected scope.</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>

            <% } else if ("overdue".equals(reportType)) { 
                int count = (overdueTasks != null) ? overdueTasks.size() : 0;
                int highPriorityCount = 0;
                long maxDays = 0;
                if (overdueTasks != null) {
                    for (Task t : overdueTasks) {
                        if (t.getPriority() != null && ("HIGH".equalsIgnoreCase(t.getPriority().name()) || "URGENT".equalsIgnoreCase(t.getPriority().name()))) {
                            highPriorityCount++;
                        }
                        long d = Math.abs(DateUtil.getDaysRemaining(t.getDueDate()));
                        if (d > maxDays) maxDays = d;
                    }
                }
            %>
                <!-- Overdue Tasks KPI Grid -->
                <div class="row g-3 mb-4">
                    <div class="col-12 col-md-4">
                        <div class="kpi-card border-danger bg-danger-subtle">
                            <div class="kpi-value text-danger"><%= count %></div>
                            <div class="kpi-label text-danger">Total Overdue Tasks</div>
                        </div>
                    </div>
                    <div class="col-12 col-md-4">
                        <div class="kpi-card border-warning">
                            <div class="kpi-value text-warning"><%= highPriorityCount %></div>
                            <div class="kpi-label">High / Urgent Priority</div>
                        </div>
                    </div>
                    <div class="col-12 col-md-4">
                        <div class="kpi-card">
                            <div class="kpi-value text-dark"><%= maxDays %> days</div>
                            <div class="kpi-label">Longest Delinquency</div>
                        </div>
                    </div>
                </div>

                <!-- Overdue Tasks Table -->
                <div class="table-responsive">
                    <table class="table table-report">
                        <thead>
                            <tr>
                                <th style="width: 50px;">ID</th>
                                <th>Task Title</th>
                                <th>Project</th>
                                <th>Priority</th>
                                <th>Assignee</th>
                                <th>Due Date</th>
                                <th>Days Overdue</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (overdueTasks != null && !overdueTasks.isEmpty()) {
                                for (Task t : overdueTasks) { 
                                    long days = Math.abs(DateUtil.getDaysRemaining(t.getDueDate()));
                                %>
                                <tr>
                                    <td class="text-muted fw-bold">#<%= t.getId() %></td>
                                    <td>
                                        <div class="fw-bold text-dark"><%= ValidationUtil.escapeXml(t.getTaskTitle()) %></div>
                                        <div class="small text-muted text-truncate" style="max-width: 280px;">
                                            <%= t.getDescription() != null ? ValidationUtil.escapeXml(t.getDescription()) : "" %>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge bg-light text-dark border"><%= ValidationUtil.escapeXml(t.getProjectName()) %></span>
                                    </td>
                                    <td>
                                        <span class="badge <%= t.getPriority() != null ? t.getPriority().getBadgeClass() : "" %>">
                                            <%= t.getPriority() != null ? t.getPriority().name() : "" %>
                                        </span>
                                    </td>
                                    <td>
                                        <div class="fw-medium"><%= t.getAssignedToName() != null ? ValidationUtil.escapeXml(t.getAssignedToName()) : "Unassigned" %></div>
                                        <div class="small text-muted"><%= t.getAssignedToEmail() != null ? ValidationUtil.escapeXml(t.getAssignedToEmail()) : "" %></div>
                                    </td>
                                    <td>
                                        <span class="fw-medium text-danger"><%= DateUtil.formatDisplayDate(t.getDueDate()) %></span>
                                    </td>
                                    <td>
                                        <span class="badge bg-danger text-white">
                                            <i class="bi bi-clock-history me-1"></i><%= days %> <%= days == 1 ? "day" : "days" %> overdue
                                        </span>
                                    </td>
                                </tr>
                            <%  }
                            } else { %>
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-success fw-medium">
                                        <i class="bi bi-check-circle-fill me-1"></i> No overdue tasks! All projects are on schedule.
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>

            <% } else { 
                int count = (tasks != null) ? tasks.size() : 0;
                int completed = 0;
                if (tasks != null) {
                    for (Task t : tasks) {
                        if (t.getStatus() != null && "COMPLETED".equalsIgnoreCase(t.getStatus().name())) completed++;
                    }
                }
            %>
                <!-- Tasks KPI Grid -->
                <div class="row g-3 mb-4">
                    <div class="col-12 col-md-4">
                        <div class="kpi-card">
                            <div class="kpi-value text-primary"><%= count %></div>
                            <div class="kpi-label">Total Tasks</div>
                        </div>
                    </div>
                    <div class="col-12 col-md-4">
                        <div class="kpi-card">
                            <div class="kpi-value text-success"><%= completed %></div>
                            <div class="kpi-label">Completed Tasks</div>
                        </div>
                    </div>
                    <div class="col-12 col-md-4">
                        <div class="kpi-card">
                            <div class="kpi-value text-dark"><%= count - completed %></div>
                            <div class="kpi-label">Pending Execution</div>
                        </div>
                    </div>
                </div>

                <!-- Tasks Table -->
                <div class="table-responsive">
                    <table class="table table-report">
                        <thead>
                            <tr>
                                <th style="width: 50px;">ID</th>
                                <th>Task Title</th>
                                <th>Project</th>
                                <th>Priority</th>
                                <th>Status</th>
                                <th>Assignee</th>
                                <th>Deadline</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (tasks != null && !tasks.isEmpty()) {
                                for (Task t : tasks) { %>
                                <tr>
                                    <td class="text-muted fw-bold">#<%= t.getId() %></td>
                                    <td>
                                        <div class="fw-bold text-dark"><%= ValidationUtil.escapeXml(t.getTaskTitle()) %></div>
                                    </td>
                                    <td>
                                        <span class="badge bg-light text-dark border"><%= ValidationUtil.escapeXml(t.getProjectName()) %></span>
                                    </td>
                                    <td>
                                        <span class="badge <%= t.getPriority() != null ? t.getPriority().getBadgeClass() : "" %>">
                                            <%= t.getPriority() != null ? t.getPriority().name() : "" %>
                                        </span>
                                    </td>
                                    <td>
                                        <span class="badge <%= t.getStatus() != null ? t.getStatus().getBadgeClass() : "" %>">
                                            <%= t.getStatus() != null ? t.getStatus().name() : "" %>
                                        </span>
                                    </td>
                                    <td><%= t.getAssignedToName() != null ? ValidationUtil.escapeXml(t.getAssignedToName()) : "Unassigned" %></td>
                                    <td><%= DateUtil.formatDisplayDate(t.getDueDate()) %></td>
                                </tr>
                            <%  }
                            } else { %>
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-muted">No tasks found.</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>

            <!-- Document Footer -->
            <div class="d-flex justify-content-between align-items-center pt-4 border-top text-muted small mt-4">
                <div>&copy; <%= new SimpleDateFormat("yyyy").format(new Date()) %> ProTrack Project Governance Platform</div>
                <div>Page 1 of 1 &bull; Document Verified</div>
            </div>
        </div>

    </div><!-- /.content-body -->

<jsp:include page="../common/footer.jsp" />
