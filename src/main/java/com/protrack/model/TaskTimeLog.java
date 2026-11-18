package com.protrack.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * TaskTimeLog Entity representing an individual work log entry on a task.
 */
public class TaskTimeLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private int taskId;
    private int userId;
    private double hoursLogged;
    private Date logDate;
    private String description;

    // Display fields joined from other tables
    private String userName;
    private String userEmail;
    private String taskTitle;

    public TaskTimeLog() {
        super();
    }

    public TaskTimeLog(int id, int taskId, int userId, double hoursLogged, Date logDate, String description) {
        super(id);
        this.taskId = taskId;
        this.userId = userId;
        this.hoursLogged = hoursLogged;
        this.logDate = logDate;
        this.description = description;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getHoursLogged() {
        return hoursLogged;
    }

    public void setHoursLogged(double hoursLogged) {
        this.hoursLogged = hoursLogged;
    }

    public Date getLogDate() {
        return logDate;
    }

    public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }
}
