package com.protrack.servlet;

import com.protrack.dao.ProjectDAO;
import com.protrack.dao.TaskDAO;
import com.protrack.model.Project;
import com.protrack.model.Role;
import com.protrack.model.Task;
import com.protrack.model.User;
import com.protrack.model.UserFactory;
import com.protrack.service.ActivityService;
import com.protrack.service.ReportExportService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ExportServletTest {

    private ExportServlet servlet;
    private ProjectDAO mockProjectDAO;
    private TaskDAO mockTaskDAO;
    private ReportExportService exportService;
    private ActivityService mockActivityService;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    public void setup() {
        mockProjectDAO = mock(ProjectDAO.class);
        mockTaskDAO = mock(TaskDAO.class);
        exportService = new ReportExportService();
        mockActivityService = mock(ActivityService.class);

        servlet = new ExportServlet(mockProjectDAO, mockTaskDAO, exportService, mockActivityService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        when(request.getContextPath()).thenReturn("/protrack");
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    @Test
    @DisplayName("Verify unauthenticated user is redirected to /login")
    public void testUnauthenticatedAccessRedirectsToLogin() throws ServletException, IOException {
        when(session.getAttribute("currentUser")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/protrack/login");
    }

    @Test
    @DisplayName("Verify Team Member is rejected with 403 Forbidden")
    public void testTeamMemberAccessForbidden() throws ServletException, IOException {
        User member = UserFactory.createUser(3, "Alex Rivera", "alex.dev@protrack.com", "pass", Role.TEAM_MEMBER, "ACTIVE");
        when(session.getAttribute("currentUser")).thenReturn(member);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(request).getRequestDispatcher("/WEB-INF/views/error/403.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    @DisplayName("Verify Admin can export project milestones as CSV")
    public void testAdminExportProjectMilestonesCsv() throws ServletException, IOException {
        User admin = UserFactory.createUser(1, "David Miller", "david.admin@protrack.com", "pass", Role.ADMIN, "ACTIVE");
        when(session.getAttribute("currentUser")).thenReturn(admin);
        when(request.getParameter("type")).thenReturn("projects");
        when(request.getParameter("format")).thenReturn("csv");

        Project p = new Project(1, "Core API", "Backend services", Date.valueOf("2026-01-01"), Date.valueOf("2026-06-01"), 2, "IN_PROGRESS");
        when(mockProjectDAO.findAll()).thenReturn(List.of(p));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { baos.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        servlet.doGet(request, response);

        verify(response).setContentType("text/csv; charset=UTF-8");
        verify(response).setHeader(eq("Content-Disposition"), contains("attachment; filename=\"protrack_project_milestones_"));
        verify(mockProjectDAO).findAll();
    }

    @Test
    @DisplayName("Verify Project Manager exports overdue tasks scoped to their managed projects")
    public void testManagerExportOverdueTasksScoped() throws ServletException, IOException {
        User pm = UserFactory.createUser(2, "Sarah Jenkins", "sarah.pm@protrack.com", "pass", Role.PROJECT_MANAGER, "ACTIVE");
        when(session.getAttribute("currentUser")).thenReturn(pm);
        when(request.getParameter("type")).thenReturn("overdue");
        when(request.getParameter("format")).thenReturn("csv");

        Task t = new Task(10, "Fix Security Bug", "Urgent fix", 1, 3, null, null, null, Date.valueOf("2026-01-01"), 2);
        when(mockTaskDAO.findOverdueTasksByManager(2)).thenReturn(List.of(t));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { baos.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        servlet.doGet(request, response);

        verify(response).setContentType("text/csv; charset=UTF-8");
        verify(mockTaskDAO).findOverdueTasksByManager(2);
        verify(mockTaskDAO, never()).findOverdueTasks(null);
    }

    @Test
    @DisplayName("Verify print/PDF format forwards to printable report JSP")
    public void testPrintPdfFormatForwardsToReportView() throws ServletException, IOException {
        User pm = UserFactory.createUser(2, "Sarah Jenkins", "sarah.pm@protrack.com", "pass", Role.PROJECT_MANAGER, "ACTIVE");
        when(session.getAttribute("currentUser")).thenReturn(pm);
        when(request.getParameter("type")).thenReturn("projects");
        when(request.getParameter("format")).thenReturn("pdf");

        servlet.doGet(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/views/common/report-view.jsp");
        verify(dispatcher).forward(request, response);
    }
}
