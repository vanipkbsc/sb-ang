using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json; // Requires NuGet package
using Microsoft.Extensions.Configuration;
using System.Net;
using System;
using System.Runtime.InteropServices;
using System.Collections.Generic; // For IEnumerable
using System.Linq; // For .Any()
using System.Diagnostics; // For Activity
using Microsoft.AspNetCore.Mvc;
using helloworldweb.Models;

namespace helloworldweb.Controllers;

public class HomeController : Controller
{
    private readonly ILogger<HomeController> _logger; // Default logger
    private readonly IHttpClientFactory _httpClientFactory; // To create HttpClient instances
    private readonly IConfiguration _configuration; // To access application configuration

    // Properties to hold data that will be displayed on the Index view
    public DateTime CurrentDateTime { get; set; }
    public bool IsContainerized { get; set; }
    public string? HostName { get; set; }
    public string? SystemInfo { get; set; } // Combines OS and basic resource note

    public HomeController(ILogger<HomeController> logger, IHttpClientFactory httpClientFactory, IConfiguration configuration)
    {
        _logger = logger;
        _httpClientFactory = httpClientFactory;
        _configuration = configuration;
    }

    public IActionResult Index()
    {
        // Gather information to display on the home page:

        // 1. Display Current Date & Time
        CurrentDateTime = DateTime.Now;

        // 2. Display Containerized Status
        // We check for a specific environment variable set during containerization.
        IsContainerized = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("DOTNET_RUNNING_IN_CONTAINER"));

        // 3. Display Hostname
        // Dns.GetHostName() returns the name of the machine or container the app is running on.
        HostName = Dns.GetHostName();

        // 4. Display OS versions and a note about resource details
        // RuntimeInformation.OSDescription provides the operating system information.
        // Getting precise resource limits (RAM/CPU cores) from within a generic .NET app
        // that works consistently across local, Docker, K8s without specific libraries
        // or container APIs (like K8s Downward API) is complex. We'll display OS version
        // and a simple note about resource complexity.
        SystemInfo = $"OS: {RuntimeInformation.OSDescription}; Note: Resource details (RAM/CPU) can vary by environment and may require specific APIs.";


        // Pass data to the view using ViewBag. A ViewModel could also be used for larger applications.
        ViewBag.CurrentDateTime = CurrentDateTime;
        ViewBag.IsContainerized = IsContainerized;
        ViewBag.HostName = HostName;
        ViewBag.SystemInfo = SystemInfo;

        return View(); // Render the Index.cshtml view
    }

    // Action to call the helloworldapikt API and display results
    public async Task<IActionResult> Weather()
    {
        // Get the API base URL from application configuration.
        // This URL will be different depending on the environment (local, docker-compose, k8s).
        var apiBaseUrl = _configuration["ApiBaseUrl"];

        if (string.IsNullOrEmpty(apiBaseUrl))
        {
            // Log an error if the API URL is not configured.
            _logger.LogError("API Base URL is not configured in app settings.");
            ViewBag.ErrorMessage = "API Base URL is not configured.";
            return View(new List<WeatherForecast>()); // Return an empty list and display error
        }

        _logger.LogInformation($"Attempting to call API at: {apiBaseUrl}/weatherforecast");

        // Create an HttpClient instance using the factory.
        var client = _httpClientFactory.CreateClient();

        try
        {
            // Make the GET request to the API's weatherforecast endpoint.
            var response = await client.GetAsync($"{apiBaseUrl}/weatherforecast");

            if (response.IsSuccessStatusCode)
            {
                // Read and deserialize the JSON response into a list of WeatherForecast objects.
                var jsonString = await response.Content.ReadAsStringAsync();
                var weatherData = JsonConvert.DeserializeObject<IEnumerable<WeatherForecast>>(jsonString);
                int count = weatherData?.Count() ?? 0;
                _logger.LogInformation($"Successfully received {count} weather forecasts from {apiBaseUrl}.");
                return View(weatherData); // Pass the weather data to the Weather view
            }
            else
            {
                // Log and display error if the API returns a non-success status code.
                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError($"API call failed to {apiBaseUrl}/weatherforecast with status code {response.StatusCode}: {errorContent}");
                ViewBag.ErrorMessage = $"Error calling API: {response.StatusCode} - {errorContent}";
                return View(new List<WeatherForecast>()); // Return empty list on API error
            }
        }
        catch (Exception ex)
        {
            // Log and display error if an exception occurs during the API call (e.g., network issue).
            _logger.LogError(ex, $"Exception occurred while calling API at {apiBaseUrl}/weatherforecast.");
            ViewBag.ErrorMessage = $"Exception calling API: {ex.Message}";
            return View(new List<WeatherForecast>()); // Return empty list on exception
        }
    }


    // Keep the Privacy and Error actions as they are
    public IActionResult Privacy()
    {
        return View();
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
    }
}

// Define a simple model for the WeatherForecast data structure returned by the API.
// This must match the JSON structure from the API response.
public class WeatherForecast
{
    public DateOnly Date { get; set; }
    public int TemperatureC { get; set; }
    public int TemperatureF => 32 + (int)(TemperatureC / 0.5556); // Calculated property
    public string? Summary { get; set; }
}