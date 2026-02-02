package com.jiralite.backend.dto;

public class DashboardMetricsResponse {
    private long activeProjects;
    private long myTickets;
    private long members;

    public DashboardMetricsResponse() {
    }

    public DashboardMetricsResponse(long activeProjects, long myTickets, long members) {
        this.activeProjects = activeProjects;
        this.myTickets = myTickets;
        this.members = members;
    }

    public long getActiveProjects() {
        return activeProjects;
    }

    public void setActiveProjects(long activeProjects) {
        this.activeProjects = activeProjects;
    }

    public long getMyTickets() {
        return myTickets;
    }

    public void setMyTickets(long myTickets) {
        this.myTickets = myTickets;
    }

    public long getMembers() {
        return members;
    }

    public void setMembers(long members) {
        this.members = members;
    }
}
