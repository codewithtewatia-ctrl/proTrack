package com.protrack.dao.impl;

import com.protrack.dao.TaskDAO;
import com.protrack.exception.DatabaseOperationException;
import com.protrack.model.Task;
import com.protrack.model.TaskPriority;
import com.protrack.model.TaskStatus;
import com.protrack.model.TaskTimeLog;
import com.protrack.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of TaskDAO with advanced filtering and deadline metrics.
 */
public class TaskDAOImpl implements TaskDAO {

    private static final String BASE_SELECT =
            "SELECT t.*, p.title AS project_name, " +
            "       u.full_name AS assigned_to_name, u.email AS assigned_to_email, " +
            "       c.full_name AS created_by_name " +
            "FROM tasks t " +
            "JOIN projects p ON t.project_id = p.project_id " +
            "LEFT JOIN users u ON t.assigned_to = u.user_id " +
            "LEFT JOIN users c ON t.created_by = c.user_id ";

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("task_id"));
        task.setTaskTitle(rs.getString("task_title"));
        task.setDescription(rs.getString("description"));
        task.setProjectId(rs.getInt("project_id"));

        int assigned = rs.getInt("assigned_to");
        if (rs.wasNull()) {
            task.setAssignedTo(null);
        } else {
            task.setAssignedTo(assigned);
        }

        task.setPriority(TaskPriority.fromString(rs.getString("priority")));
        task.setStatus(TaskStatus.fromString(rs.getString("status")));
        task.setStartDate(rs.getDate("start_date"));
        task.setDueDate(rs.getDate("due_date"));

        int creator = rs.getInt("created_by");
        if (rs.wasNull()) {
            task.setCreatedBy(null);
        } else {
            task.setCreatedBy(creator);
        }

        task.setCreatedAt(rs.getTimestamp("created_at"));
        task.setUpdatedAt(rs.getTimestamp("updated_at"));

        task.setProjectName(rs.getString("project_name"));
        task.setAssignedToName(rs.getString("assigned_to_name"));
        task.setAssignedToEmail(rs.getString("assigned_to_email"));
        task.setCreatedByName(rs.getString("created_by_name"));

        try {
            task.setEstimatedHours(rs.getDouble("estimated_hours"));
            task.setLoggedHours(rs.getDouble("logged_hours"));
        } catch (SQLException ignored) {}

        return task;
    }

    @Override
    public Optional<Task> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE t.task_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find task by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findAll() {
        List<Task> list = new ArrayList<>();
        String sql = BASE_SELECT + " ORDER BY t.due_date ASC, t.priority DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch all tasks", e);
        }
        return list;
    }

    @Override
    public List<Task> findByProjectId(int projectId) {
        List<Task> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE t.project_id = ? ORDER BY t.due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find tasks for project: " + projectId, e);
        }
        return list;
    }

    @Override
    public List<Task> findByAssignedTo(int userId) {
        List<Task> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE t.assigned_to = ? ORDER BY t.due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find tasks for user: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Task> findByAssignedToAndFilters(int userId, String status, String priority) {
        List<Task> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE t.assigned_to = ? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND t.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append(" AND t.priority = ? ");
            params.add(priority.trim().toUpperCase());
        }

        sql.append(" ORDER BY t.due_date ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to filter tasks for user: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Task> searchTasks(String query, Integer projectId, Integer assignedTo, TaskStatus status, TaskPriority priority) {
        return searchTasks(query, projectId, assignedTo, status, priority, null);
    }

    @Override
    public List<Task> searchTasks(String query, Integer projectId, Integer assignedTo, TaskStatus status, TaskPriority priority, Integer managerId) {
        List<Task> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.task_title) LIKE ? OR LOWER(t.description) LIKE ?) ");
            String wild = "%" + query.trim().toLowerCase() + "%";
            params.add(wild);
            params.add(wild);
        }

        if (projectId != null && projectId > 0) {
            sql.append(" AND t.project_id = ? ");
            params.add(projectId);
        }

        if (assignedTo != null && assignedTo > 0) {
            sql.append(" AND t.assigned_to = ? ");
            params.add(assignedTo);
        }

        if (status != null) {
            sql.append(" AND t.status = ? ");
            params.add(status.name());
        }

        if (priority != null) {
            sql.append(" AND t.priority = ? ");
            params.add(priority.name());
        }

        if (managerId != null && managerId > 0) {
            sql.append(" AND p.manager_id = ? ");
            params.add(managerId);
        }

        sql.append(" ORDER BY t.due_date ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to search tasks", e);
        }
        return list;
    }

    @Override
    public Task save(Task task) {
        String sql = "INSERT INTO tasks (task_title, description, project_id, assigned_to, priority, status, start_date, due_date, created_by, estimated_hours, logged_hours) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTaskTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getProjectId());

            if (task.getAssignedTo() != null && task.getAssignedTo() > 0) {
                ps.setInt(4, task.getAssignedTo());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            ps.setString(5, task.getPriority() != null ? task.getPriority().name() : TaskPriority.MEDIUM.name());
            ps.setString(6, task.getStatus() != null ? task.getStatus().name() : TaskStatus.TODO.name());
            ps.setDate(7, task.getStartDate());
            ps.setDate(8, task.getDueDate());

            if (task.getCreatedBy() != null && task.getCreatedBy() > 0) {
                ps.setInt(9, task.getCreatedBy());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.setDouble(10, task.getEstimatedHours());
            ps.setDouble(11, task.getLoggedHours());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        task.setId(rs.getInt(1));
                    }
                }
            }
            return task;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to insert task: " + task.getTaskTitle(), e);
        }
    }

    @Override
    public boolean update(Task task) {
        String sql = "UPDATE tasks SET task_title = ?, description = ?, project_id = ?, assigned_to = ?, " +
                     "priority = ?, status = ?, start_date = ?, due_date = ?, estimated_hours = ? WHERE task_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getProjectId());

            if (task.getAssignedTo() != null && task.getAssignedTo() > 0) {
                ps.setInt(4, task.getAssignedTo());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            ps.setString(5, task.getPriority() != null ? task.getPriority().name() : TaskPriority.MEDIUM.name());
            ps.setString(6, task.getStatus() != null ? task.getStatus().name() : TaskStatus.TODO.name());
            ps.setDate(7, task.getStartDate());
            ps.setDate(8, task.getDueDate());
            ps.setDouble(9, task.getEstimatedHours());
            ps.setInt(10, task.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update task: " + task.getId(), e);
        }
    }

    @Override
    public boolean updateStatus(int taskId, TaskStatus newStatus) {
        String sql = "UPDATE tasks SET status = ? WHERE task_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update status for task: " + taskId, e);
        }
    }

    @Override
    public boolean updatePriority(int taskId, TaskPriority newPriority) {
        String sql = "UPDATE tasks SET priority = ? WHERE task_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPriority.name());
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update priority for task: " + taskId, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete task: " + id, e);
        }
    }

    @Override
    public int countTotalTasks() {
        String sql = "SELECT COUNT(*) FROM tasks";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count total tasks", e);
        }
        return 0;
    }

    @Override
    public int countTasksByStatus(TaskStatus status) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count tasks by status: " + status, e);
        }
        return 0;
    }

    @Override
    public int countTasksByPriority(TaskPriority priority) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE priority = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count tasks by priority: " + priority, e);
        }
        return 0;
    }

    @Override
    public int countOverdueTasks() {
        String sql = "SELECT COUNT(*) FROM tasks WHERE status != 'COMPLETED' AND due_date < CURRENT_DATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count overdue tasks", e);
        }
        return 0;
    }

    @Override
    public int countTasksForMember(int userId, TaskStatus status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM tasks WHERE assigned_to = ?");
        if (status != null) {
            sql.append(" AND status = ?");
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            if (status != null) {
                ps.setString(2, status.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count tasks for member: " + userId, e);
        }
        return 0;
    }

    @Override
    public int countOverdueTasksForMember(int userId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE assigned_to = ? AND status != 'COMPLETED' AND due_date < CURRENT_DATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count overdue tasks for member: " + userId, e);
        }
        return 0;
    }

    @Override
    public List<Task> findOverdueTasks(Integer userId) {
        List<Task> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append(" WHERE t.status != 'COMPLETED' AND t.due_date < CURRENT_DATE()");
        if (userId != null && userId > 0) {
            sql.append(" AND t.assigned_to = ?");
        }
        sql.append(" ORDER BY t.due_date ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (userId != null && userId > 0) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch overdue tasks", e);
        }
        return list;
    }

    @Override
    public List<Task> findOverdueTasksByManager(int managerId) {
        List<Task> list = new ArrayList<>();
        String sql = BASE_SELECT +
                " WHERE t.status != 'COMPLETED' AND t.due_date < CURRENT_DATE() AND p.manager_id = ? " +
                " ORDER BY t.due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch overdue tasks for manager: " + managerId, e);
        }
        return list;
    }

    @Override
    public List<Task> findTasksDueToday(Integer userId) {
        List<Task> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append(" WHERE t.status != 'COMPLETED' AND t.due_date = CURRENT_DATE()");
        if (userId != null && userId > 0) {
            sql.append(" AND t.assigned_to = ?");
        }
        sql.append(" ORDER BY t.priority DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (userId != null && userId > 0) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch tasks due today", e);
        }
        return list;
    }

    @Override
    public Map<TaskStatus, Integer> getStatusDistributionByProject(int projectId) {
        Map<TaskStatus, Integer> map = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            map.put(s, 0);
        }
        String sql = "SELECT status, COUNT(*) AS cnt FROM tasks WHERE project_id = ? GROUP BY status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskStatus status = TaskStatus.fromString(rs.getString("status"));
                    map.put(status, rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to get status distribution for project: " + projectId, e);
        }
        return map;
    }

    @Override
    public Map<TaskPriority, Integer> getPriorityDistribution() {
        Map<TaskPriority, Integer> map = new EnumMap<>(TaskPriority.class);
        for (TaskPriority p : TaskPriority.values()) {
            map.put(p, 0);
        }
        String sql = "SELECT priority, COUNT(*) AS cnt FROM tasks GROUP BY priority";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TaskPriority priority = TaskPriority.fromString(rs.getString("priority"));
                map.put(priority, rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to get priority distribution", e);
        }
        return map;
    }

    @Override
    public boolean logTime(int taskId, int userId, double hours, java.sql.Date logDate, String description) {
        String insertSql = "INSERT INTO task_time_logs (task_id, user_id, hours_logged, log_date, description) VALUES (?, ?, ?, ?, ?)";
        String updateTaskSql = "UPDATE tasks SET logged_hours = (SELECT COALESCE(SUM(hours_logged), 0) FROM task_time_logs WHERE task_id = ?) WHERE task_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, taskId);
                    psInsert.setInt(2, userId);
                    psInsert.setDouble(3, hours);
                    psInsert.setDate(4, logDate);
                    psInsert.setString(5, description);
                    psInsert.executeUpdate();
                }
                try (PreparedStatement psUpdate = conn.prepareStatement(updateTaskSql)) {
                    psUpdate.setInt(1, taskId);
                    psUpdate.setInt(2, taskId);
                    psUpdate.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to log time for task: " + taskId, e);
        }
    }

    @Override
    public List<TaskTimeLog> findTimeLogsByTaskId(int taskId) {
        List<TaskTimeLog> logs = new ArrayList<>();
        String sql = "SELECT l.*, u.full_name AS user_name, u.email AS user_email, t.task_title " +
                     "FROM task_time_logs l " +
                     "JOIN users u ON l.user_id = u.user_id " +
                     "JOIN tasks t ON l.task_id = t.task_id " +
                     "WHERE l.task_id = ? ORDER BY l.log_date DESC, l.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskTimeLog log = new TaskTimeLog();
                    log.setId(rs.getInt("log_id"));
                    log.setTaskId(rs.getInt("task_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setHoursLogged(rs.getDouble("hours_logged"));
                    log.setLogDate(rs.getDate("log_date"));
                    log.setDescription(rs.getString("description"));
                    log.setCreatedAt(rs.getTimestamp("created_at"));
                    log.setUserName(rs.getString("user_name"));
                    log.setUserEmail(rs.getString("user_email"));
                    log.setTaskTitle(rs.getString("task_title"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch time logs for task: " + taskId, e);
        }
        return logs;
    }

    @Override
    public List<TaskTimeLog> findTimeLogsByProjectId(int projectId) {
        List<TaskTimeLog> logs = new ArrayList<>();
        String sql = "SELECT l.*, u.full_name AS user_name, u.email AS user_email, t.task_title " +
                     "FROM task_time_logs l " +
                     "JOIN users u ON l.user_id = u.user_id " +
                     "JOIN tasks t ON l.task_id = t.task_id " +
                     "WHERE t.project_id = ? ORDER BY l.log_date DESC, l.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskTimeLog log = new TaskTimeLog();
                    log.setId(rs.getInt("log_id"));
                    log.setTaskId(rs.getInt("task_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setHoursLogged(rs.getDouble("hours_logged"));
                    log.setLogDate(rs.getDate("log_date"));
                    log.setDescription(rs.getString("description"));
                    log.setCreatedAt(rs.getTimestamp("created_at"));
                    log.setUserName(rs.getString("user_name"));
                    log.setUserEmail(rs.getString("user_email"));
                    log.setTaskTitle(rs.getString("task_title"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch time logs for project: " + projectId, e);
        }
        return logs;
    }

    @Override
    public double getTotalLoggedHoursForProject(int projectId) {
        String sql = "SELECT COALESCE(SUM(logged_hours), 0) FROM tasks WHERE project_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to calculate total logged hours for project: " + projectId, e);
        }
        return 0.0;
    }

    @Override
    public double getTotalEstimatedHoursForProject(int projectId) {
        String sql = "SELECT COALESCE(SUM(estimated_hours), 0) FROM tasks WHERE project_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to calculate total estimated hours for project: " + projectId, e);
        }
        return 0.0;
    }
}
