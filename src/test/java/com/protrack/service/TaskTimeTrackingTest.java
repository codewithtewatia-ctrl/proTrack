package com.protrack.service;

import com.protrack.dao.ActivityDAO;
import com.protrack.dao.ProjectDAO;
import com.protrack.dao.TaskDAO;
import com.protrack.dao.UserDAO;
import com.protrack.exception.AppException;
import com.protrack.exception.UnauthorizedAccessException;
import com.protrack.model.Project;
import com.protrack.model.Role;
import com.protrack.model.Task;
import com.protrack.model.TaskPriority;
import com.protrack.model.TaskStatus;
import com.protrack.model.User;
import com.protrack.model.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TaskTimeTrackingTest {

    private TaskDAO mockTaskDAO;
    private ActivityDAO mockActivityDAO;
    private UserDAO mockUserDAO;
    private ProjectDAO mockProjectDAO;
    private TaskService taskService;

    @BeforeEach
    public void setup() {
        mockTaskDAO = mock(TaskDAO.class);
        mockActivityDAO = mock(ActivityDAO.class);
        mockUserDAO = mock(UserDAO.class);
        mockProjectDAO = mock(ProjectDAO.class);
        taskService = new TaskService(mockTaskDAO, mockActivityDAO, mockUserDAO, mockProjectDAO);
    }

    @Test
    @DisplayName("Task entity calculates time progress, remaining hours, and overtime accurately")
    public void testTaskTimeProgressCalculations() {
        Task task = new Task();
        task.setEstimatedHours(40.0);
        task.setLoggedHours(20.0);

        assertEquals(50, task.getTimeProgressPercentage());
        assertEquals(50, task.getDisplayProgressPercentage());
        assertFalse(task.isOverEstimate());
        assertEquals(0.0, task.getOvertimeHours());
        assertEquals(20.0, task.getRemainingHours());
        assertEquals("bg-primary", task.getTimeProgressBarClass());

        // Warning state >= 85%
        task.setLoggedHours(36.0); // 90%
        assertEquals(90, task.getTimeProgressPercentage());
        assertFalse(task.isOverEstimate());
        assertEquals("bg-warning", task.getTimeProgressBarClass());

        // Overtime state > 100%
        task.setLoggedHours(48.0); // 120%
        assertEquals(120, task.getTimeProgressPercentage());
        assertEquals(100, task.getDisplayProgressPercentage());
        assertTrue(task.isOverEstimate());
        assertEquals(8.0, task.getOvertimeHours());
        assertEquals(0.0, task.getRemainingHours());
        assertEquals("bg-danger", task.getTimeProgressBarClass());

        // Zero estimate edge case
        Task zeroEstTask = new Task();
        zeroEstTask.setEstimatedHours(0.0);
        zeroEstTask.setLoggedHours(0.0);
        assertEquals(0, zeroEstTask.getTimeProgressPercentage());
        assertFalse(zeroEstTask.isOverEstimate());
        assertEquals("bg-primary", zeroEstTask.getTimeProgressBarClass());
    }

    @Test
    @DisplayName("Project entity aggregates estimated and logged hours with progress bar color")
    public void testProjectTimeProgressCalculations() {
        Project project = new Project();
        project.setTotalEstimatedHours(100.0);
        project.setTotalLoggedHours(75.0);

        assertEquals(75, project.getTimeProgressPercentage());
        assertEquals(75, project.getDisplayTimeProgressPercentage());
        assertFalse(project.isOverEstimate());
        assertEquals(0.0, project.getOvertimeHours());
        assertEquals("bg-primary", project.getTimeProgressBarClass());

        // Over estimate
        project.setTotalLoggedHours(115.5);
        assertEquals(116, project.getTimeProgressPercentage());
        assertEquals(100, project.getDisplayTimeProgressPercentage());
        assertTrue(project.isOverEstimate());
        assertEquals(15.5, project.getOvertimeHours());
        assertEquals("bg-danger", project.getTimeProgressBarClass());
    }

    @Test
    @DisplayName("Team member can log time on a task assigned directly to them")
    public void testTeamMemberCanLogTimeOnAssignedTask() throws AppException {
        User member = UserFactory.createUser(4, "Alex Rivera", "alex@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE");

        Task task = new Task();
        task.setId(10);
        task.setTaskTitle("Implement Payment Webhook");
        task.setProjectId(1);
        task.setAssignedTo(4); // Assigned to Alex
        task.setEstimatedHours(20.0);
        task.setLoggedHours(5.0);

        when(mockTaskDAO.findById(10)).thenReturn(Optional.of(task));
        when(mockTaskDAO.logTime(eq(10), eq(4), eq(3.5), any(Date.class), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> {
            taskService.logTimeWithRoleCheck(10, 3.5, new Date(System.currentTimeMillis()), "Developed webhook signature verifier", member);
        });

        verify(mockTaskDAO, times(1)).logTime(eq(10), eq(4), eq(3.5), any(Date.class), eq("Developed webhook signature verifier"));
        verify(mockActivityDAO, times(1)).log(eq(4), eq("TASK_TIME_LOG"), contains("3.50 hrs"), eq("TASK"), eq(10));
    }

    @Test
    @DisplayName("Team member cannot log time on a task assigned to someone else")
    public void testTeamMemberCannotLogTimeOnUnassignedTask() {
        User member = UserFactory.createUser(4, "Alex Rivera", "alex@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE");

        Task task = new Task();
        task.setId(12);
        task.setTaskTitle("Security Audit");
        task.setProjectId(1);
        task.setAssignedTo(5); // Assigned to Emma Watson

        when(mockTaskDAO.findById(12)).thenReturn(Optional.of(task));

        UnauthorizedAccessException ex = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.logTimeWithRoleCheck(12, 4.0, new Date(System.currentTimeMillis()), "Ran pen test", member);
        });

        assertTrue(ex.getMessage().contains("Team members can only log time on tasks assigned directly to them"));
        verify(mockTaskDAO, never()).logTime(anyInt(), anyInt(), anyDouble(), any(), any());
    }

    @Test
    @DisplayName("Project Manager can log time on tasks within projects they manage")
    public void testProjectManagerCanLogTimeOnManagedProject() throws AppException {
        User manager = UserFactory.createUser(2, "Sarah Jenkins", "sarah@protrack.com", "hash", Role.PROJECT_MANAGER, "ACTIVE");

        Task task = new Task();
        task.setId(20);
        task.setTaskTitle("Client Review Meeting Preparation");
        task.setProjectId(1);
        task.setAssignedTo(4);

        Project project = new Project();
        project.setId(1);
        project.setManagerId(2); // Managed by Sarah

        when(mockTaskDAO.findById(20)).thenReturn(Optional.of(task));
        when(mockProjectDAO.findById(1)).thenReturn(Optional.of(project));
        when(mockTaskDAO.logTime(eq(20), eq(2), eq(2.0), any(Date.class), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> {
            taskService.logTimeWithRoleCheck(20, 2.0, new Date(System.currentTimeMillis()), "Sprint demo prep", manager);
        });

        verify(mockTaskDAO, times(1)).logTime(eq(20), eq(2), eq(2.0), any(Date.class), eq("Sprint demo prep"));
    }

    @Test
    @DisplayName("Project Manager cannot log time on tasks in projects managed by other managers")
    public void testProjectManagerCannotLogTimeOnOtherProject() {
        User manager = UserFactory.createUser(2, "Sarah Jenkins", "sarah@protrack.com", "hash", Role.PROJECT_MANAGER, "ACTIVE");

        Task task = new Task();
        task.setId(30);
        task.setTaskTitle("Mobile Core Architecture");
        task.setProjectId(2);

        Project project = new Project();
        project.setId(2);
        project.setManagerId(3); // Managed by David Miller

        when(mockTaskDAO.findById(30)).thenReturn(Optional.of(task));
        when(mockProjectDAO.findById(2)).thenReturn(Optional.of(project));

        UnauthorizedAccessException ex = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.logTimeWithRoleCheck(30, 2.5, new Date(System.currentTimeMillis()), "Review", manager);
        });

        assertTrue(ex.getMessage().contains("You can only log time for tasks in projects you manage"));
        verify(mockTaskDAO, never()).logTime(anyInt(), anyInt(), anyDouble(), any(), any());
    }

    @Test
    @DisplayName("Admin can log time on any task across the system")
    public void testAdminCanLogTimeOnAnyTask() throws AppException {
        User admin = UserFactory.createUser(1, "Super Admin", "admin@protrack.com", "hash", Role.ADMIN, "ACTIVE");

        Task task = new Task();
        task.setId(40);
        task.setTaskTitle("Emergency Hotfix");
        task.setProjectId(2);
        task.setAssignedTo(null);

        when(mockTaskDAO.findById(40)).thenReturn(Optional.of(task));
        when(mockTaskDAO.logTime(eq(40), eq(1), eq(1.5), any(Date.class), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> {
            taskService.logTimeWithRoleCheck(40, 1.5, new Date(System.currentTimeMillis()), "Hotfix deployment", admin);
        });

        verify(mockTaskDAO, times(1)).logTime(eq(40), eq(1), eq(1.5), any(Date.class), eq("Hotfix deployment"));
    }

    @Test
    @DisplayName("Validation fails when logging invalid hours (<= 0 or > 24)")
    public void testInvalidHoursValidation() {
        User user = UserFactory.createUser(1, "Admin", "admin@protrack.com", "hash", Role.ADMIN, "ACTIVE");

        assertThrows(AppException.class, () -> {
            taskService.logTimeWithRoleCheck(1, 0.0, new Date(System.currentTimeMillis()), "Zero hours", user);
        });

        assertThrows(AppException.class, () -> {
            taskService.logTimeWithRoleCheck(1, -2.5, new Date(System.currentTimeMillis()), "Negative hours", user);
        });

        assertThrows(AppException.class, () -> {
            taskService.logTimeWithRoleCheck(1, 25.0, new Date(System.currentTimeMillis()), "Exceeds daily limit", user);
        });
    }

    @Test
    @DisplayName("Task creation sets estimated hours properly")
    public void testCreateTaskWithEstimatedHours() throws AppException {
        Date start = Date.valueOf("2026-03-01");
        Date due = Date.valueOf("2026-03-15");

        when(mockUserDAO.findById(4)).thenReturn(Optional.of(
                UserFactory.createUser(4, "Alex", "alex@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE")
        ));
        when(mockProjectDAO.isMemberAssigned(1, 4)).thenReturn(true);
        when(mockTaskDAO.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(101);
            return t;
        });

        Task created = taskService.createTask("API Integration", "Stripe API", 1, 4,
                TaskPriority.HIGH, TaskStatus.TODO, start, due, 32.5, 2);

        assertNotNull(created);
        assertEquals(32.5, created.getEstimatedHours());
        assertEquals(0.0, created.getLoggedHours());
    }

    @Test
    @DisplayName("Task update updates estimated hours properly")
    public void testUpdateTaskWithEstimatedHours() throws AppException {
        Date due = Date.valueOf("2026-03-20");

        Task existing = new Task();
        existing.setId(50);
        existing.setTaskTitle("Initial Task");
        existing.setProjectId(1);
        existing.setEstimatedHours(10.0);

        when(mockTaskDAO.findById(50)).thenReturn(Optional.of(existing));
        when(mockTaskDAO.update(any(Task.class))).thenReturn(true);

        taskService.updateTask(50, "Updated Task Title", "New description", 1, null,
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, null, due, 28.0, 2);

        assertEquals(28.0, existing.getEstimatedHours());
        verify(mockTaskDAO, times(1)).update(existing);
    }
}
