package com.example.sbapi.model;

import java.time.LocalDate;

public class SystemInfo {
    private LocalDate currentDateTime;
    private boolean isContainerized;
    private String hostname;
    private String systemInfo;

    public SystemInfo(LocalDate currentDateTime, boolean isContainerized, String hostname, String systemInfo) {
        this.currentDateTime = currentDateTime;
        this.isContainerized = isContainerized;
        this.hostname = hostname;
        this.systemInfo = systemInfo;
    }

    // Getters
    public LocalDate getCurrentDateTime() { return currentDateTime; }
    public boolean isContainerized() { return isContainerized; }
    public String getHostname() { return hostname; }
    public String getSystemInfo() { return systemInfo; }

    // Setters (optional)
    public void setCurrentDateTime(LocalDate currentDateTime) { this.currentDateTime = currentDateTime; }
    public void setContainerized(boolean containerized) { isContainerized = containerized; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setSystemInfo(String systemInfo) { this.systemInfo = systemInfo; }
}