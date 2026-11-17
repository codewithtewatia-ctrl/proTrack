package com.protrack.servlet;

import com.protrack.dao.ProjectDAO;
import com.protrack.dao.TaskDAO;
import com.protrack.dao.impl.ProjectDAOImpl;
import com.protrack.dao.impl.TaskDAOImpl;
import com.protrack.model.Project;
import com.protrack.model.Role;
import com.protrack.model.Task;
import com.protrack.model.User;
import com.protrack.service.ActivityService;
import com.protrack.service.ReportExportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Controller handling CSV and PDF/Print reporting and data exports
 * for Project Milestones and Overdue Tasks.
 */
@WebServlet(name = "ExportServlet", urlPatterns = {"/export", "/manager/export", "/admin/export"})
public class ExportServlet extends HttpServlet {

    private final ProjectDAO projectDAO;
    private final TaskDAO taskDAO;
    private final ReportExportService reportExportService;
    private final ActivityService activityService;

    public ExportServlet() {
        this.projectDAO = new ProjectDAOImpl();
        this.taskDAO = new TaskDAOImpl();
        this.reportExportService = new ReportExportService();
        this.activityService = new ActivityService();
    }

    public ExportServlet(ProjectDAO projectDAO, TaskDAO taskDAO,
                         ReportExportService reportExportService, ActivityService activityService) {
        this.projectDAO = projectDAO;
        this.taskDAO = taskDAO;
        this.reportExportService = reportExportService;
        this.activityService = activityService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // RBAC: Strictly allow only Project Managers and Administrators
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PROJECT_MANAGER) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("errorMessage", "Access Denied: Only Project Managers and Administrators can export reports.");
            request.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(request, response);
            return;
        }

        String type = request.getParameter("type");
        if (type == null || type.trim().isEmpty()) {
            type = "projects";
        }
        type = type.trim().toLowerCase();

        String format = request.getParameter("format");
        if (format == null || format.trim().isEmpty()) {
            format = "csv";
        }
        format = format.trim().toLowerCase();

        boolean isAdmin = (currentUser.getRole() == Role.ADMIN);
        int managerId = currentUser.getId();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        if ("overdue".equals(type)) {
            // Overdue Tasks Export
            List<Task> overdueTasks;
            if (isAdmin) {
                overdueTasks = taskDAO.findOverdueTasks(null);
            } else {
                overdueTasks = taskDAO.findOverdueTasksByManager(managerId);
            }

            if ("pdf".equals(format) || "print".equals(format)) {
                request.setAttribute("reportType", "overdue");
                request.setAttribute("reportTitle", "Overdue Tasks Audit Report");
                request.setAttribute("overdueTasks", overdueTasks);
                request.setAttribute("generatedDate", new Date());
                request.setAttribute("currentUser", currentUser);
                activityService.log(currentUser.getId(), "REPORT_VIEW", "Viewed Overdue Tasks Print/PDF Report", "REPORT", null);
                request.getRequestDispatcher("/WEB-INF/views/common/report-view.jsp").forward(request, response);
            } else {
                String csv = reportExportService.generateOverdueTasksCsv(overdueTasks);
                sendCsvResponse(response, "protrack_overdue_tasks_" + timestamp + ".csv", csv);
                activityService.log(currentUser.getId(), "EXPORT", "Downloaded Overdue Tasks CSV (" + overdueTasks.size() + " tasks)", "REPORT", null);
            }

        } else if ("tasks".equals(type)) {
            // Detailed Tasks Export
            List<Task> tasks;
            String projectIdParam = request.getParameter("projectId");
            if (projectIdParam != null && !projectIdParam.isEmpty()) {
                try {
                    int pId = Integer.parseInt(projectIdParam);
                    tasks = taskDAO.findByProjectId(pId);
                } catch (NumberFormatException e) {
                    tasks = isAdmin ? taskDAO.findAll() : taskDAO.searchTasks(null, null, null, null, null, managerId);
                }
            } else {
                tasks = isAdmin ? taskDAO.findAll() : taskDAO.searchTasks(null, null, null, null, null, managerId);
            }

            if ("pdf".equals(format) || "print".equals(format)) {
                request.setAttribute("reportType", "tasks");
                request.setAttribute("reportTitle", "Task Management Summary Report");
                request.setAttribute("tasks", tasks);
                request.setAttribute("generatedDate", new Date());
                request.setAttribute("currentUser", currentUser);
                activityService.log(currentUser.getId(), "REPORT_VIEW", "Viewed Tasks Summary Print/PDF Report", "REPORT", null);
                request.getRequestDispatcher("/WEB-INF/views/common/report-view.jsp").forward(request, response);
            } else {
                String csv = reportExportService.generateTasksCsv(tasks);
                sendCsvResponse(response, "protrack_tasks_summary_" + timestamp + ".csv", csv);
                activityService.log(currentUser.getId(), "EXPORT", "Downloaded Tasks Summary CSV (" + tasks.size() + " tasks)", "REPORT", null);
            }

        } else {
            // Default: Project Milestones & Progress Summary
            List<Project> projects;
            if (isAdmin) {
                projects = projectDAO.findAll();
            } else {
                projects = projectDAO.findByManagerId(managerId);
            }

            if ("pdf".equals(format) || "print".equals(format)) {
                request.setAttribute("reportType", "projects");
                request.setAttribute("reportTitle", "Project Milestones & Progress Summary");
                request.setAttribute("projects", projects);
                request.setAttribute("generatedDate", new Date());
                request.setAttribute("currentUser", currentUser);
                activityService.log(currentUser.getId(), "REPORT_VIEW", "Viewed Project Milestones Print/PDF Report", "REPORT", null);
                request.getRequestDispatcher("/WEB-INF/views/common/report-view.jsp").forward(request, response);
            } else {
                String csv = reportExportService.generateProjectMilestonesCsv(projects);
                sendCsvResponse(response, "protrack_project_milestones_" + timestamp + ".csv", csv);
                activityService.log(currentUser.getId(), "EXPORT", "Downloaded Project Milestones CSV (" + projects.size() + " projects)", "REPORT", null);
            }
        }
    }

    private void sendCsvResponse(HttpServletResponse response, String filename, String csvContent) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);

        try (OutputStream out = response.getOutputStream()) {
            out.write(bytes);
            out.flush();
        }
    }
}
