package com.protrack.service;

import com.protrack.model.Project;
import com.protrack.model.Task;
import com.protrack.model.TaskPriority;
import com.protrack.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportExportServiceTest {

    private ReportExportService exportService;

    @BeforeEach
    public void setup() {
        exportService = new ReportExportService();
    }

    @Test
    @DisplayName("Verify Project Milestones CSV generation and RFC-4180 header/data formatting")
    public void testGenerateProjectMilestonesCsv() {
        Project p1 = new Project(1, "E-Commerce Platform", "Revamp checkout, payments & cart",
                Date.valueOf("2026-01-01"), Date.valueOf("2026-06-30"), 2, "IN_PROGRESS");
        p1.setManagerName("Sarah Jenkins");
        p1.setManagerEmail("sarah.pm@protrack.com");
        p1.setTotalTasks(10);
        p1.setCompletedTasks(7);
        p1.setPendingTasks(3);

        String csv = exportService.generateProjectMilestonesCsv(List.of(p1));
        assertNotNull(csv);
        assertTrue(csv.startsWith("\uFEFF")); // UTF-8 BOM
        assertTrue(csv.contains("Project ID"));
        assertTrue(csv.contains("Milestone Progress (%)"));
        assertTrue(csv.contains("\"E-Commerce Platform\""));
        assertTrue(csv.contains("\"Sarah Jenkins\""));
        assertTrue(csv.contains("70%"));
        assertTrue(csv.contains("\"Revamp checkout, payments & cart\""));
    }

    @Test
    @DisplayName("Verify Overdue Tasks CSV generation with delinquency days calculation")
    public void testGenerateOverdueTasksCsv() {
        Task t1 = new Task(101, "Integrate Stripe Gateway", "Handle webhooks", 1, 3,
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS, Date.valueOf("2026-01-01"), Date.valueOf("2026-01-15"), 2);
        t1.setProjectName("E-Commerce Platform");
        t1.setAssignedToName("Alex Rivera");
        t1.setAssignedToEmail("alex.dev@protrack.com");
        t1.setCreatedByName("Sarah Jenkins");

        String csv = exportService.generateOverdueTasksCsv(List.of(t1));
        assertNotNull(csv);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Task ID"));
        assertTrue(csv.contains("Days Overdue"));
        assertTrue(csv.contains("\"Integrate Stripe Gateway\""));
        assertTrue(csv.contains("\"Alex Rivera\""));
        assertTrue(csv.contains("\"E-Commerce Platform\""));
    }

    @Test
    @DisplayName("Verify CSV special characters escaping (quotes, commas, linebreaks)")
    public void testEscapeCsv() {
        assertEquals("\"\"", ReportExportService.escapeCsv(null));
        assertEquals("\"Simple Text\"", ReportExportService.escapeCsv("Simple Text"));
        assertEquals("\"Text with, comma\"", ReportExportService.escapeCsv("Text with, comma"));
        assertEquals("\"Text with \"\"quotes\"\"\"", ReportExportService.escapeCsv("Text with \"quotes\""));
        assertEquals("\"Line 1\nLine 2\"", ReportExportService.escapeCsv("Line 1\nLine 2"));
    }
}
