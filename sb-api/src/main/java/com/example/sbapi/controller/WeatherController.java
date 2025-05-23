package com.example.sbapi.controller;
import com.example.sbapi.model.SystemInfo;
import com.example.sbapi.model.WeatherForecast;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean; // Requires com.sun.management.OperatingSystemMXBean
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api") // Base path for API endpoints
@CrossOrigin(origins = "http://localhost:4200") // <-- Add this annotation
// This controller provides weather forecasts and system information
// It uses Spring's RestController to handle HTTP requests
// The @RequestMapping annotation specifies the base path for all endpoints in this controller
// The @GetMapping annotation is used to map HTTP GET requests to specific methods
// The WeatherForecast class is a simple data model representing a weather forecast
// The SystemInfo class is a simple data model representing system information
// The Random class is used to generate random numbers for the weather forecast
// The InetAddress class is used to get the hostname of the machine
// The UnknownHostException is handled to avoid crashing if the hostname cannot be determined
public class WeatherController {

    private static final String[] SUMMARIES = new String[] {
        "Freezing", "Bracing", "Chilly", "Cool", "Mild", "Warm", "Balmy", "Hot", "Sweltering", "Scorching"
    };

    @GetMapping("/weatherforecast")
    public List<WeatherForecast> getWeatherForecast() {
        Random random = new Random();
        return Arrays.stream(SUMMARIES)
                .map(summary -> new WeatherForecast(
                        LocalDate.now().plusDays(random.nextInt(5)), // Random date within next 5 days
                        random.nextInt(60) - 20, // Temperature between -20 and 39
                        summary
                ))
                .limit(5) // Return 5 forecasts
                .toList(); // Collect as a List
    }

    @GetMapping("/info")
    public SystemInfo getSystemInfo() {
         String hostname = "Unknown Host";
         try {
             hostname = InetAddress.getLocalHost().getHostName();
         } catch (UnknownHostException e) {
             // Log error, but continue
             e.printStackTrace();
         }

         // Attempt to get OS info and resources - this can be complex in containers
         String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
         String resourceNote = "Note: Resource details (RAM/CPU) can vary by environment and may require specific APIs.";

         // Basic attempt to get process CPU load (might not work in all container setups)
         double processCpuLoad = -1.0;
         try {
             OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
             if (osBean != null) {
                 processCpuLoad = osBean.getProcessCpuLoad(); // Returns a value between 0.0 and 1.0
             }
         } catch (Exception e) {
             // Ignore, not critical for demo
         }
         if (processCpuLoad >= 0) {
              resourceNote = String.format("OS: %s; Hostname: %s; Process CPU Load: %.2f%%; Note: Resource limits can vary.",
                  osInfo, hostname, processCpuLoad * 100);
         } else {
              resourceNote = String.format("OS: %s; Hostname: %s; %s", osInfo, hostname, resourceNote);
         }


        // In a container, we can check environment variables like KUBERNETES_SERVICE_HOST
        // Or rely on the fact that it's running in Docker if the Dockerfile sets an env var.
        // For simplicity, we'll rely on the hostname being the container ID/name in Docker/K8s.
        boolean isContainerized = System.getenv("DOTNET_RUNNING_IN_CONTAINER") != null // Example from original, unlikely in Spring Boot
                                  || System.getenv("KUBERNETES_SERVICE_HOST") != null // K8s specific
                                  || System.getenv("HOSTNAME") != null; // Common in containers

        return new SystemInfo(LocalDate.now(), isContainerized, hostname, resourceNote);
    }
}