package com.jiralite.backend.dto;

import java.util.UUID;

public class UpdateTicketRequest {

    private String title;

    private String description;

    private String priority;

    private UUID assigneeId;
    
    // When true, assignee will be cleared even if assigneeId is null.
    private Boolean clearAssignee;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Boolean getClearAssignee() {
        return clearAssignee;
    }

    public void setClearAssignee(Boolean clearAssignee) {
        this.clearAssignee = clearAssignee;
    }
}
