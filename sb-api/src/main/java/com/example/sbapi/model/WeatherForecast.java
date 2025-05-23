package com.example.sbapi.model;

import java.time.LocalDate;

public class WeatherForecast {
    private LocalDate date;
    private int temperatureC;
    private String summary;
    private int temperatureF; // Calculated property

    public WeatherForecast(LocalDate date, int temperatureC, String summary) {
        this.date = date;
        this.temperatureC = temperatureC;
        this.summary = summary;
        this.temperatureF = 32 + (int)(temperatureC / 0.5556);
    }

    // Getters (required for JSON serialization)
    public LocalDate getDate() { return date; }
    public int getTemperatureC() { return temperatureC; }
    public String getSummary() { return summary; }
    public int getTemperatureF() { return temperatureF; }

    // Setters (optional, but good practice if needed)
    public void setDate(LocalDate date) { this.date = date; }
    public void setTemperatureC(int temperatureC) { this.temperatureC = temperatureC; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setTemperatureF(int temperatureF) { this.temperatureF = temperatureF; } // Setter for calculated field might not be needed
}