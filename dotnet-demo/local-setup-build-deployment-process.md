# Local Application Setup, Build, and Deployment Process by simeptk

This document provides a detailed, step-by-step guide for setting up your local environment, building .NET 9 applications, containerizing them using Docker, and deploying them to a local Kubernetes cluster (specifically using Rancher Desktop). It combines conceptual planning with concrete command examples and outputs.

## 1. Prerequisites and Environment Check

Before we begin the build and deployment process, ensure you have the necessary tools installed and verify the state of your local environment.

### 1.1 Essential Tools

Make sure the following tools are installed on your Windows machine with WSL2:

*   **.NET 9 SDK:** The .NET Software Development Kit is required to build the applications.
*   **Visual Studio Code or Visual Studio:** A code editor or Integrated Development Environment (IDE) is necessary for writing and modifying application code and configuration files.
*   **Docker Desktop or Rancher Desktop:** This provides the Docker engine for building and running containers, and a local Kubernetes cluster. Ensure Kubernetes is enabled and running in your chosen Docker/Kubernetes tool. Rancher Desktop is used in the provided logs, so configuring it to use `containerd` as the Kubernetes runtime (which is often the default) is recommended.
*   **PowerShell:** The command-line shell used for executing the build and deployment commands.
*   **kubectl:** The Kubernetes command-line tool for interacting with the local cluster. This is typically included with Docker Desktop or Rancher Desktop when Kubernetes is enabled.
*   **curl (Optional):** A command-line tool useful for testing HTTP endpoints, like the API and web application.

### 1.2 Verifying the Local Environment

It's good practice to check the status of your Docker and Kubernetes environments before starting.

*   **Existing Docker Images:** Check the Docker images already present on your system.

    ```bash
    docker images
    ```

    ```
    REPOSITORY                                          TAG                    IMAGE ID       CREATED        SIZE
    mcr.microsoft.com/dotnet/sdk                        9.0                    a03668e9c254   4 days ago     850MB
    mcr.microsoft.com/dotnet/aspnet                     9.0                    e4a8465fadd8   4 days ago     224MB
    registry                                            3                      3dec7d02aaea   6 weeks ago    57.7MB
    rancher/klipper-lb                                  v0.4.13                f7415d0003cb   2 months ago   12.2MB
    rancher/local-path-provisioner                      v0.0.31                8309ed19e06b   3 months ago   60.4MB
    rancher/mirrored-library-traefik                    3.3.2                  88eafdd76c93   4 months ago   190MB
    rancher/klipper-helm                                v0.9.4-build20250113   cf1d4e2d0dbd   4 months ago   190MB
    rancher/mirrored-coredns-coredns                    1.12.0                 1cf5f116067c   5 months ago   70.1MB
    rancher/mirrored-metrics-server                     v0.7.2                 48d9cfaaf390   8 months ago   67.1MB
    rancher/mirrored-library-busybox                    1.36.1                 2d61ae04c2b8   2 years ago    4.27MB
    rancher/mirrored-pause                              3.6                    6270bb605e12   3 years ago    683kB
    ghcr.io/rancher-sandbox/rancher-desktop/rdx-proxy   latest                 d178408c8cf6   55 years ago   5.63MB
    ```

    This output lists various base images (like .NET SDK and ASP.NET runtime) and system images used by the local Kubernetes environment (like `rancher/`). A local Docker registry image (`registry:3`) is also shown as available.

*   **Local Kubernetes Cluster Info:** Check if your local Kubernetes cluster is running.

    ```bash
    kubectl cluster-info
    ```

    ```
    Kubernetes control plane is running at https://127.0.0.1:6443
    CoreDNS is running at https://127.0.0.1:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
    Metrics-server is running at https://127.0.0.1:6443/api/v1/namespaces/kube-system/services/https:metrics-server:https/proxy

    To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
    ```

    This confirms the Kubernetes control plane and essential services are active and accessible via `https://127.0.0.1:6443`.

*   **Kubernetes Nodes:** See the nodes participating in the cluster.

    ```bash
    kubectl get nodes -o wide
    ```

    ```
    NAME            STATUS   ROLES                  AGE   VERSION        INTERNAL-IP     EXTERNAL-IP   OS-IMAGE                           KERNEL-VERSION                       CONTAINER-RUNTIME
    wgc100fk68v44   Ready    control-plane,master   17d   v1.32.3+k3s1   192.168.127.2   <none>        Rancher Desktop WSL Distribution   5.15.167.4-microsoft-standard-WSL2   docker://26.1.5
    ```

    This shows a single node cluster (`wgc100fk68v44`) that is `Ready`.

*   **Kubernetes Namespaces:** List the existing namespaces.

    ```bash
    kubectl get namespace
    ```

    ```
    NAME              STATUS   AGE
    default           Active   17d
    kube-node-lease   Active   17d
    kube-public       Active   17d
    kube-system       Active   17d
    ```

    Standard Kubernetes namespaces are present. We will create a new namespace for our application later for better organization.

*   **System Pods:** View the pods running core Kubernetes components across all namespaces.

    ```bash
    kubectl get pods --all-namespaces -o wide
    ```

    ```
    NAMESPACE     NAME                                      READY   STATUS      RESTARTS       AGE   IP            NODE            NOMINATED NODE   READINESS GATES
    kube-system   coredns-ff8999cc5-5s7kd                   1/1     Running     13 (26m ago)   17d   10.42.0.102   wgc100fk68v44   <none>           <none>
    kube-system   helm-install-traefik-crd-bjjbf            0/1     Completed   0              17d   <none>        wgc100fk68v44   <none>           <none>
    kube-system   helm-install-traefik-sks4b                0/1     Completed   2              17d   <none>        wgc100fk68v44   <none>           <none>
    kube-system   local-path-provisioner-774c6665dc-8hfkw   1/1     Running     13 (26m ago)   17d   10.42.0.100   wgc100fk68v44   <none>           <none>
    kube-system   metrics-server-6f4c6675d5-nvzkk           1/1     Running     13 (26m ago)   17d   10.42.0.101   wgc100fk68v44   <none>           <none>
    kube-system   svclb-traefik-48fd4d45-r7pv4              2/2     Running     26 (26m ago)   17d   10.42.0.99    wgc100fk68v44   <none>           <none>
    kube-system   traefik-67bfb46dcb-khkhv                  1/1     Running     13 (26m ago)   17d   10.42.0.98    wgc100fk68v44   <none>           <none>
    ```

    This confirms the system pods are healthy and running on the single node.

## 2. Application Project Creation

We will create two .NET 9 projects: a Web API (`helloworldapikt`) and an MVC Web Application (`helloworldkt`).

### 2.1 Create the API Project (`helloworldapikt`)

1.  Open PowerShell and navigate to your desired project directory (e.g., `C:\kubernetes-demo`).
2.  Create the Web API project:

    ```powershell
    dotnet new webapi -n helloworldapikt -f net9.0
    ```

    *   `dotnet new webapi`: Initializes a new ASP.NET Core Web API project.
    *   `-n helloworldapikt`: Specifies the name of the output folder and project file.
    *   `-f net9.0`: Sets the target .NET framework version.

3.  Navigate into the newly created project folder:

    ```powershell
    cd helloworldapikt
    ```

4.  (Optional) Open the project in your code editor to inspect the default files (e.g., `Program.cs`, `WeatherForecastController.cs`, `helloworldapikt.csproj`).

    ```powershell
    code .
    ```

### 2.2 Create the MVC Web App Project (`helloworldkt`)

1.  Go back to the parent directory in PowerShell:

    ```powershell
    cd ..
    ```

2.  Create the MVC Web Application project:

    ```powershell
    dotnet new mvc -n helloworldkt -f net9.0
    ```

    *   `dotnet new mvc`: Initializes a new ASP.NET Core MVC project.
    *   `-n helloworldkt`: Specifies the name of the output folder and project file.
    *   `-f net9.0`: Sets the target .NET framework version.

3.  Navigate into the web app project folder:

    ```powershell
    cd helloworldkt
    ```

4.  (Optional) Open the project in your code editor to inspect the default files (e.g., `Program.cs`, `Controllers/HomeController.cs`, `Views/Home/Index.cshtml`, `helloworldkt.csproj`).

    ```powershell
    code .
    ```

## 3. Application Implementation and Configuration

Now we will modify the `helloworldkt` MVC application to display specific information on its home page and add functionality to call the `helloworldapikt` API.

### 3.1 Modify `helloworldkt/Controllers/HomeController.cs`

We need to add code to the `HomeController` to capture and pass the required information (Date/Time, container status, hostname, OS/resource details) to the view and implement the API call logic.

1.  Open `helloworldkt/Controllers/HomeController.cs` in your code editor.
2.  Add the necessary `using` directives at the top for networking, JSON serialization, configuration, and system information:

    ```csharp
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
    using helloworldkt.Models; // Import ErrorViewModel
    using Microsoft.AspNetCore.Mvc;
    namespace helloworldkt.Controllers;
    ```

3.  Add the `Newtonsoft.Json` NuGet package to the `helloworldkt` project from your PowerShell window (in the `helloworldkt` directory):

    ```powershell
    dotnet add package Newtonsoft.Json
    ```

4.  Modify the `HomeController` class to inject dependencies (`HttpClientFactory`, `IConfiguration`), add properties for data, update the `Index` action, and add a new `Weather` action to call the API.

    ```csharp
    // ... using directives ...

    public class HomeController : Controller
    {
        private readonly ILogger<HomeController> _logger; // Default logger
        private readonly IHttpClientFactory _httpClientFactory; // To create HttpClient instances
        private readonly IConfiguration _configuration; // To access application configuration

        // Properties to hold data that will be displayed on the Index view
        public DateTime CurrentDateTime { get; set; }
        public bool IsContainerized { get; set; }
        public string HostName { get; set; }
        public string SystemInfo { get; set; } // Combines OS and basic resource note

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
                     _logger.LogInformation($"Successfully received {weatherData.Count()} weather forecasts from {apiBaseUrl}.");
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
    ```

### 3.2 Add Configuration for API Base URL

The MVC application needs a way to know the base URL of the API. This is environment-specific. We'll use the .NET configuration system, reading from `appsettings.json` or environment variables.

1.  Open `helloworldkt/appsettings.Development.json` in your code editor.
2.  Add an `ApiBaseUrl` key within the root JSON object. This value will be used when running the application locally during development.

    ```json
    {
      "Logging": {
        "LogLevel": {
          "Default": "Information",
          "Microsoft.AspNetCore": "Warning"
        }
      },
      "ApiBaseUrl": "http://localhost:8080" // Default API URL for local development
    }
    ```

    *   For Docker Compose and Kubernetes, we will override this setting using environment variables in the respective configuration files (`docker-compose.yaml` and Kubernetes deployment YAML).

### 3.3 Modify `helloworldkt/Views/Home/Index.cshtml`

Update the home page view to display the information passed from the `HomeController`'s `Index` action and provide a link to the `Weather` action.

1.  Open `helloworldkt/Views/Home/Index.cshtml` in your code editor.
2.  Replace the default content with the following HTML structure that accesses the `ViewBag` properties:

    ```cshtml
    @* Access the data passed from the HomeController's Index action via ViewBag *@
    @{
        ViewData["Title"] = "Home Page";
    }

    <div class="text-center">
        <h1 class="display-4">Welcome</h1>
        <p>Learn about <a href="https://learn.microsoft.com/aspnet/core">building Web apps with ASP.NET Core</a>.</p>
    </div>

    <hr/> @* Horizontal rule for separation *@

    <h2>Application Information</h2>
    @* Display the application details in an unordered list *@
    <ul>
        <li>
            @* Display the current date and time, formatted *@
            <strong>Current Date & Time:</strong> @ViewBag.CurrentDateTime.ToString("yyyy-MM-dd HH:mm:ss")
        </li>
        <li>
            @* Display whether the application is running containerized *@
            <strong>Running Containerized:</strong> @(ViewBag.IsContainerized ? "True" : "False")
        </li>
        <li>
            @* Display the hostname where the application is running *@
            <strong>Hostname:</strong> @ViewBag.HostName
        </li>
        <li>
            @* Display system information (OS and resource note) *@
            <strong>System Info:</strong> @ViewBag.SystemInfo
        </li>
    </ul>

    <hr/>

    <h2>API Interaction</h2>
    <p>
        @* Create a link that, when clicked, navigates to the Weather action in the HomeController *@
        <a asp-controller="Home" asp-action="Weather">Click to See WeatherForecast</a>
    </p>

    @* Display an error message if one is present in ViewBag (e.g., if API URL is not configured) *@
    @if (!string.IsNullOrEmpty(ViewBag.ErrorMessage))
    {
        <div class="alert alert-danger mt-3" role="alert">
           @ViewBag.ErrorMessage
        </div>
    }
    ```

### 3.4 Create `helloworldkt/Views/Home/Weather.cshtml`

Create a new view file specifically for displaying the weather forecast data received from the API call.

1.  In the `helloworldkt/Views/Home` folder, create a new file named `Weather.cshtml`.
2.  Add the following content to define the structure for displaying the weather data in a table:

    ```cshtml
    @* Define the model for this view as an enumerable collection of WeatherForecast objects *@
    @model IEnumerable<helloworldkt.Controllers.WeatherForecast>
    @{
        ViewData["Title"] = "Weather Forecast"; // Set the page title
    }

    <h1>Weather Forecast</h1>

    @* Check if an error message was set in ViewBag (e.g., if the API call failed) *@
    @if (!string.IsNullOrEmpty(ViewBag.ErrorMessage))
    {
        <div class="alert alert-danger mt-3" role="alert">
           @ViewBag.ErrorMessage
        </div>
         <p>
            @* Provide a link back to the home page *@
            <a asp-controller="Home" asp-action="Index">Back to Home</a>
        </p>
    }
    @* Check if the model is null or contains no data *@
    else if (Model == null || !Model.Any())
    {
        <p>No weather data available.</p>
         <p>
            <a asp-controller="Home" asp-action="Index">Back to Home</a>
        </p>
    }
    else
    {
        <p>Data retrieved from the API:</p>

        @* Display the weather data in a table *@
        <table class="table table-striped table-bordered"> @* Using Bootstrap table classes *@
            <thead>
                <tr>
                    <th>Date</th>
                    <th>Temp. (C)</th>
                    <th>Temp. (F)</th>
                    <th>Summary</th>
                </tr>
            </thead>
            <tbody>
                @* Iterate through the collection of WeatherForecast objects *@
                @foreach (var forecast in Model)
                {
                    <tr>
                        <td>@forecast.Date.ToShortDateString()</td> @* Format the Date *@
                        <td>@forecast.TemperatureC</td>
                        <td>@forecast.TemperatureF</td>
                        <td>@forecast.Summary</td>
                    </tr>
                }
            </tbody>
            <tfoot>
                 <tr>
                    @* Footer row spanning all columns *@
                    <td colspan="4" class="text-center">End of Weather Data</td>
                </tr>
            </tfoot>
        </table>
         <p>
            <a asp-controller="Home" asp-action="Index">Back to Home</a>
        </p>
    }
    ```

## 4. Run Applications Locally (for Verification)

Before containerizing, it's helpful to verify that both applications run correctly and can communicate when executed directly on your machine.

1.  Open **two separate PowerShell windows**.
2.  **In the first PowerShell window (for the API):**
    *   Navigate to the `helloworldapikt` project directory.
    *   Run the API, explicitly setting the HTTP URL to port 8080.

        ```powershell
        cd C:\kubernetes-demo\helloworld\helloworldapikt
        dotnet run --urls "http://localhost:8080"
        ```

    *   Look for console output confirming the application is listening on `http://localhost:8080`.
    *   *(Verification):* Open a web browser and go to `http://localhost:8080/weatherforecast`. You should see JSON data representing the weather forecast.

3.  **In the second PowerShell window (for the MVC app):**
    *   Navigate to the `helloworldkt` project directory.
    *   Run the MVC app, explicitly setting the HTTP URL to port 4200.

        ```powershell
        cd C:\kubernetes-demo\helloworld\helloworldkt
        dotnet run --urls "http://localhost:4200"
        ```

    *   Look for console output confirming the application is listening on `http://localhost:4200`.
    *   *(Verification):* Open a web browser and go to `http://localhost:4200`.
        *   The home page should load, displaying the current date/time, your machine's hostname, OS info, and "Running Containerized: **False**".
        *   Click the "Click to See WeatherForecast" link. This should make an HTTP GET request from the MVC app to the API running on `http://localhost:8080`. If successful, you should see the weather data displayed in a table.
    *   *(Troubleshooting Tip):* If the API call fails, ensure the API is running in the other PowerShell window and that the `ApiBaseUrl` in `appsettings.Development.json` is correctly set to `http://localhost:8080`.

4.  **Stop the applications:** Press `Ctrl+C` in each of the PowerShell windows where the applications are running.

This confirms that your code changes and application logic work as expected before moving to containerization.

## 5. Containerization with Docker

We will now create Dockerfiles for each application to define how their container images should be built and then build those images.

### 5.1 Create Dockerfile for helloworldapikt

1.  In the `helloworldapikt` project folder (`C:\kubernetes-demo\helloworld\helloworldapikt`), create a new file named `Dockerfile` (with no file extension).
2.  Add the following content, which uses a common multi-stage build pattern for .NET applications:

    ```dockerfile
    # Use the official .NET SDK image as the build environment
    FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
    WORKDIR /app

    # Copy the project file and restore dependencies to leverage Docker's layer caching
    COPY *.csproj ./
    RUN dotnet restore

    # Copy the rest of the application code into the build environment
    COPY . .

    # Build the application in Release configuration
    RUN dotnet build -c Release --no-restore

    # Use the build environment to publish the application for deployment
    FROM build AS publish
    RUN dotnet publish -c Release -o /app/publish --no-build

    # Use the official ASP.NET Core runtime image as the final, smaller runtime environment
    FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS final
    WORKDIR /app
    COPY --from=publish /app/publish . # Copy the published application from the build stage

    # Expose the port the application (Kestrel) will listen on inside the container
    EXPOSE 80

    # Set the entry point command to run the published application DLL
    ENTRYPOINT ["dotnet", "helloworldapikt.dll"]

    # Optional Environment Variable: Configure Kestrel to listen on the exposed port
    # ENV ASPNETCORE_URLS=http://+:80 # This is often handled by default in containers, but can be explicit
    # Optional Environment Variable: Indicate to the app it's running in a container
    # ENV DOTNET_RUNNING_IN_CONTAINER=true # Used by helloworldkt to detect containerization
    ```

### 5.2 Create Dockerfile for helloworldkt

1.  In the `helloworldkt` project folder (`C:\kubernetes-demo\helloworld\helloworldkt`), create a new file named `Dockerfile`.
2.  Add similar multi-stage build content, tailored for the web app:

    ```dockerfile
    # Use the official .NET SDK image as the build environment
    FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
    WORKDIR /app

    # Copy the project file and restore dependencies
    COPY *.csproj ./
    RUN dotnet restore

    # Copy the rest of the application code
    COPY . .

    # Build the application
    RUN dotnet build -c Release --no-restore

    # Publish the application
    FROM build AS publish
    RUN dotnet publish -c Release -o /app/publish --no-build

    # Use the official ASP.NET Core runtime image as the final runtime environment
    FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS final
    WORKDIR /app
    COPY --from=publish /app/publish .

    # Expose the port the application (Kestrel) will listen on inside the container
    EXPOSE 80

    # Set the entry point command
    ENTRYPOINT ["dotnet", "helloworldkt.dll"]

    # Optional Environment Variable: Configure Kestrel
    # ENV ASPNETCORE_URLS=http://+:80
    # Optional Environment Variable: Indicate containerization
    ENV DOTNET_RUNNING_IN_CONTAINER=true # Explicitly set for the app to detect it's in a container
    # Optional Environment Variable: Configure the API URL (will be overridden by docker-compose/k8s)
    # ENV ApiBaseUrl=... # Will be set via docker-compose or K8s manifests
    ```

### 5.3 Build the Docker Images

Now, build the container images using the Dockerfiles. Run these commands from the *parent directory* (`C:\kubernetes-demo\helloworld`) so the build context (`.`) can access both project folders.

1.  Open PowerShell in the parent directory (`C:\kubernetes-demo\helloworld`).
2.  Build the API image:

    ```powershell
    docker build -t helloworldapikt -f helloworldapikt/Dockerfile .
    ```

    *   `-t helloworldapikt`: Tags the resulting image with the name `helloworldapikt`. By default, it gets the `latest` tag.
    *   `-f helloworldapikt/Dockerfile`: Specifies the path to the Dockerfile for the API.
    *   `.`: The build context, which is the current directory. This allows `COPY . .` inside the Dockerfile to copy files from the project subfolder relative to this context.

3.  Build the MVC image:

    ```powershell
    docker build -t helloworldkt -f helloworldkt/Dockerfile .
    ```

    *   `-t helloworldkt`: Tags the resulting image with the name `helloworldkt`.

4.  Verify that the new images have been created locally:

    ```powershell
    docker images
    ```

    ```
    REPOSITORY                                          TAG                    IMAGE ID       CREATED         SIZE
    helloworldkt                                        latest                 cba6b7f4273f   2 minutes ago   237MB
    helloworldapikt                                     latest                 0aeaf0b2107d   3 minutes ago   227MB
    # ... other images ...
    ```

    Your two application images should now appear in the list, usually with the `latest` tag by default.

## 6. Run Applications with Docker Compose

Docker Compose is a tool for defining and running multi-container Docker applications. You define your application's services, networks, and volumes in a `docker-compose.yaml` file, and then use a single command to build and start everything.

### 6.1 Create `docker-compose.yaml`

1.  In the parent directory (`C:\kubernetes-demo\helloworld`), create a file named `docker-compose.yaml`.
2.  Add the following content to define your two services (`api` and `webapp`), specify their builds, port mappings, environment variables, and network:

    ```yaml
    # docker-compose.yaml
    services:
      api:
        image: helloworldapikt # Use the image we built
        build:
          context: . # Build context is the parent directory
          dockerfile: helloworldapikt/Dockerfile # Path to the Dockerfile
        ports:
          - "8080:80" # Map host port 8080 to container port 80
        environment:
          - ASPNETCORE_ENVIRONMENT=Development # Set development environment
          - ASPNETCORE_URLS=http://+:80 # Configure Kestrel to listen on port 80
          - DOTNET_RUNNING_IN_CONTAINER=true # Tell the app it's in a container
        networks:
          - app-network # Connect to the defined network
        restart: always # Restart container if it stops

      webapp:
        image: helloworldkt # Use the image we built
        build:
          context: .
          dockerfile: helloworldkt/Dockerfile
        ports:
          - "4200:80" # Map host port 4200 to container port 80
        environment:
          - ASPNETCORE_ENVIRONMENT=Development
          - ASPNETCORE_URLS=http://+:80
          - DOTNET_RUNNING_IN_CONTAINER=true # Tell the app it's in a container
          # Crucial: Configure the API URL to use the Docker Compose service name
          - ApiBaseUrl=http://api:80  # 'api' is the service name, 80 is the container port
        depends_on:
          - api # Ensure the 'api' service starts before 'webapp'
        networks:
          - app-network
        restart: always

    networks:
      app-network:
        driver: bridge # Use the default bridge network driver
    ```

    *   **Explanation:**
        *   `services`: Defines the individual containers/applications (`api`, `webapp`).
        *   `build`: Specifies how to build the Docker image for the service. `context: .` means the build process starts in the directory containing `docker-compose.yaml`.
        *   `image`: Specifies the image name to use (or tag the built image with).
        *   `ports`: Maps a port on your host machine (`8080` or `4200`) to a port inside the container (`80`).
        *   `environment`: Sets environment variables inside the container. `ApiBaseUrl=http://api:80` is key here; within the Docker network created by Compose, services can reach each other using their service names (`api`) and the port they expose internally (`80`).
        *   `depends_on`: Ensures services start in the correct order.
        *   `networks`: Connects services to a shared network, enabling them to communicate using their service names.

### 6.2 Build and Run with Docker Compose

1.  Open PowerShell in the parent directory (`C:\kubernetes-demo\helloworld`).
2.  Run the following command to build (if necessary) and start the services in detached mode (`-d`):

    ```powershell
    docker compose up -d --build
    ```

    *   `up`: Starts the services defined in `docker-compose.yaml`.
    *   `-d`: Runs the containers in the background (detached mode).
    *   `--build`: Ensures that the Docker images are built using the specified Dockerfiles before starting the containers. This is important if you've made code changes since the last build.

    ```
    Compose can now delegate builds to bake for better performance.
     To do so, set COMPOSE_BAKE=true.
    [+] Building 19.6s (18/19)                                                                                          docker:default
    [+] Building 43.0s (31/31) FINISHED                                                                                 docker:default
     => [api internal] load build definition from Dockerfile                                                                      0.0s
     ... (build output) ...
     => [webapp] resolving provenance for metadata file                                                                           0.0s
    [+] Running 5/5
     ✔ api                             Built                                                                                      0.0s
     ✔ webapp                          Built                                                                                      0.0s
     ✔ Network helloworld_app-network  Created                                                                                    0.2s
     ✔ Container helloworld-api-1      Started                                                                                    0.4s
     ✔ Container helloworld-webapp-1   Started
    ```

3.  Verify the status of the services running via Docker Compose:

    ```powershell
    docker compose ps
    ```

    ```
    NAME                  IMAGE            COMMAND               SERVICE   CREATED         STATUS          PORTS
    helloworld-api-1      helloworldapikt  "dotnet helloworlda…" helloworld  5 seconds ago   Up 4 seconds    0.0.0.0:8080->80/tcp, :::8080->80/tcp
    helloworld-webapp-1   helloworldkt     "dotnet helloworldk…" webapp    5 seconds ago   Up 4 seconds    0.0.0.0:4200->80/tcp, :::4200->80/tcp
    ```

    This confirms that both `api` and `webapp` containers are running and their host ports are mapped correctly.

### 6.3 Test the Docker Compose Deployment

1.  Open a web browser and go to `http://localhost:4200`.
    *   The home page should load.
    *   Verify the Application Information:
        *   Current Date & Time: Should reflect the current time.
        *   Running Containerized: **True** (because we set the environment variable in `docker-compose.yaml`).
        *   Hostname: Should be the container ID or name (e.g., `helloworld-webapp-1`).
        *   System Info: Should show the OS details from *inside* the container (likely a Linux distribution).
    *   Click the "Click to See WeatherForecast" link.
    *   The MVC application (running in the `webapp` container) should successfully call the API (running in the `api` container) using the internal Docker network address (`http://api:80`). The weather data should be displayed in a table.

2.  *(Optional):* Directly test the API by opening a browser or using `curl` on `http://localhost:8080/weatherforecast`.

    ```powershell
    curl http://localhost:8080/weatherforecast
    ```

    ```
    [{"date":"2025-05-19","temperatureC":15,"summary":"Warm","temperatureF":58},{"date":"2025-05-20","temperatureC":25,"summary":"Chilly","temperatureF":76},{"date":"2025-05-21","temperatureC":8,"summary":"Cool","temperatureF":46},{"date":"2025-05-22","temperatureC":-1,"summary":"Hot","temperatureF":31},{"date":"2025-05-23","temperatureC":26,"summary":"Scorching","temperatureF":78}]
    ```

### 6.4 Stop the Docker Compose Deployment

1.  In the PowerShell window where you ran `docker compose up -d`, run the following command:

    ```powershell
    docker compose down
    ```

    ```
    [+] Running 3/3
     ✔ Container helloworld-webapp-1   Removed                                                                                    0.5s
     ✔ Container helloworld-api-1      Removed                                                                                    0.4s
     ✔ Network helloworld_app-network  Removed                                                                                    0.6s
    ```

    This command stops the running containers, removes the containers and the network created by Docker Compose.

This confirms your applications are correctly containerized and can communicate within a Docker Compose setup.

## 7. Prepare for Kubernetes Deployment

Deploying to Kubernetes requires defining your application components (Deployments, Services, etc.) using YAML manifest files. When deploying to a local Kubernetes cluster like the one provided by Rancher Desktop, it's common practice to use a local Docker registry to store your application images.

### 7.1 Start a Local Docker Registry

1.  Start a Docker container running the `registry:3` image. This container will act as your local registry.

    ```powershell
    docker run -d -p 5000:5000 --name local-registry registry:3
    ```

    ```
    1c2422b8c1393ca241e99c9699da62e61ac80714b6c4cf3cc787fd7218eab3bc
    ```

2.  Verify the registry is running and check its catalog (it should be empty initially).

    ```powershell
    curl http://localhost:5000/v2/_catalog
    ```

    ```
    {"repositories":[]}
    ```

### 7.2 Tag and Push Images to the Local Registry

Kubernetes needs to know where to pull the container images from. By default, it looks at Docker Hub or other configured registries. To use your local images, you tag them with the local registry's address (`localhost:5000`) and then push them to it.

1.  Open PowerShell in the parent directory (`C:\kubernetes-demo\helloworld`).
2.  Tag the API image with the local registry address and a version tag:

    ```powershell
    docker tag helloworldapikt localhost:5000/helloworldapikt:1.0.1
    ```

    *   `docker tag <source_image> <target_image>`: Creates a new tag for the source image. `helloworldapikt` is the source image (from your earlier build), `localhost:5000/helloworldapikt:1.0.1` is the target tag including the registry address and version.

3.  Tag the MVC image:

    ```powershell
    docker tag helloworldkt localhost:5000/helloworldkt:1.0.1
    ```

4.  Push the tagged API image to the local registry:

    ```powershell
    docker push localhost:5000/helloworldapikt:1.0.1
    ```

    ```
    The push refers to repository [localhost:5000/helloworldapikt]
    ab95e624ec06: Pushed
    ... (push output) ...
    1.0.1: digest: sha256:7b3e3053f8ebc231aabbd85afa52010178229817c9b36f3a26e6bfc8dc45b98e size: 2202
    ```

5.  Push the tagged MVC image:

    ```powershell
    docker push localhost:5000/helloworldkt:1.0.1
    ```

    ```
    The push refers to repository [localhost:5000/helloworldkt]
    86deca85a101: Pushed
    ... (push output) ...
    1.0.1: digest: sha256:3490637a404968ebd41bf0c976292633b4a1287a58ebaf48130fcfdec599d1c0 size: 2203
    ```

    The output shows the image layers being pushed to your local registry.

6.  Verify the images are now in the local registry's catalog:

    ```powershell
    curl http://localhost:5000/v2/_catalog
    ```

    ```
    {"repositories":["helloworldapikt","helloworldkt"]}
    ```

### 7.3 Create Kubernetes Manifest Files (YAML)

These files declare the desired state of your application components in Kubernetes. Create a new subfolder (e.g., `k8s`) in your parent project directory (`C:\kubernetes-demo\helloworld`) to keep these files organized.

1.  Create the `k8s` subfolder:

    ```powershell
    mkdir k8s
    cd k8s
    ```

2.  Create `namespace.yaml`: Defines a dedicated namespace for your application.

    ```yaml
    # k8s/namespace.yaml
    apiVersion: v1 # API version for Namespace
    kind: Namespace # Type of Kubernetes object
    metadata:
      name: helloworldapp # Name of the namespace
    ```

3.  Create `api-deployment.yaml`: Defines a Deployment for the API.

    ```yaml
    # k8s/api-deployment.yaml
    apiVersion: apps/v1 # API version for Deployment
    kind: Deployment # Type of Kubernetes object
    metadata:
      name: helloworldapi-deployment # Name of the deployment
      namespace: helloworldapp # Deploy into our dedicated namespace
      labels:
        app: helloworldapi # Label to identify the deployment
    spec:
      replicas: 1 # Desired number of pod replicas
      selector:
        matchLabels:
          app: helloworldapi # Selector to find pods belonging to this deployment
      template: # Template for the pods managed by this deployment
        metadata:
          labels:
            app: helloworldapi # Labels applied to pods
        spec:
          containers: # List of containers in the pod
            - name: helloworldapi # Name of the container
              image: localhost:5000/helloworldapikt:1.0.1 # **Image from local registry**
              ports:
                - containerPort: 80 # Port the application listens on inside the container
              env: # Environment variables for the container
                - name: ASPNETCORE_URLS
                  value: http://+:80 # Configure Kestrel
                - name: DOTNET_RUNNING_IN_CONTAINER
                  value: "true" # Indicate containerization
              imagePullPolicy: Always # **Crucial for local registry**: Ensures Kubernetes always pulls the latest image
    ```

4.  Create `webapp-deployment.yaml`: Defines a Deployment for the web app.

    ```yaml
    # k8s/webapp-deployment.yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: helloworldwebapp-deployment
      namespace: helloworldapp
      labels:
        app: helloworldwebapp
    spec:
      replicas: 1 # Start with one instance
      selector:
        matchLabels:
          app: helloworldwebapp
      template:
        metadata:
          labels:
            app: helloworldwebapp
        spec:
          containers:
            - name: helloworldwebapp
              image: localhost:5000/helloworldkt:1.0.1 # **Image from local registry**
              ports:
                - containerPort: 80
              env:
                - name: ASPNETCORE_ENVIRONMENT
                  value: Development
                - name: ASPNETCORE_URLS
                  value: http://+:80
                - name: DOTNET_RUNNING_IN_CONTAINER
                  value: "true"
                # Crucial: Configure the API URL using the Kubernetes Service name
                - name: ApiBaseUrl
                  value: http://helloworldapi-service:80 # ServiceName.Namespace (or just ServiceName in the same namespace)
              imagePullPolicy: Always # **Crucial for local registry**
    ```

    *   **API URL in K8s:** Notice how `ApiBaseUrl` is set to `http://helloworldapi-service:80`. In Kubernetes, services provide stable network identities. Within the same namespace, pods can typically reach a service by its name (`helloworldapi-service`) on the port the service exposes (`80`).

5.  Create `api-service.yaml`: Defines a Service to expose the API Deployment internally.

    ```yaml
    # k8s/api-service.yaml
    apiVersion: v1 # API version for Service
    kind: Service # Type of Kubernetes object
    metadata:
      name: helloworldapi-service # Name of the service (used in webapp's ApiBaseUrl)
      namespace: helloworldapp # Service belongs to this namespace
    spec:
      selector:
        app: helloworldapi # Selects pods with this label (our API deployment pods)
      ports:
        - protocol: TCP
          port: 80 # The port the service listens on within the cluster
          targetPort: 80 # The port the pods are listening on
      type: ClusterIP # Service is only accessible within the cluster network
    ```

    *   `ClusterIP` is the default and suitable for internal communication. We'll use `kubectl port-forward` to access it from outside the cluster for local testing.

6.  Create `webapp-service.yaml`: Defines a Service to expose the Web App Deployment internally.

    ```yaml
    # k8s/webapp-service.yaml
    apiVersion: v1
    kind: Service
    metadata:
      name: helloworldwebapp-service # Name of the service
      namespace: helloworldapp
    spec:
      selector:
        app: helloworldwebapp # Selects pods with this label (our webapp deployment pods)
      ports:
        - protocol: TCP
          port: 80 # The port the service listens on within the cluster
          targetPort: 80 # The port the pods are listening on
      type: ClusterIP # Service is only accessible within the cluster network
    ```

    *   Again, `ClusterIP` is suitable for local testing with `port-forward`.

7.  *(Optional):* Create `deploy.ps1` and `cleanup-local.ps1` scripts in the parent directory (`C:\kubernetes-demo\helloworld`) to automate the steps in sections 7.2, 8, and 9. (The contents of these scripts are not provided, but their actions are described in the following steps).

## 8. Deploy to Local Kubernetes Cluster

Now that the images are in the local registry and the Kubernetes manifests are defined, we can deploy the application to the local cluster using `kubectl`.

1.  Open PowerShell and navigate to the `k8s` directory (`C:\kubernetes-demo\helloworld\k8s`).
2.  Apply the namespace manifest:

    ```powershell
    kubectl apply -f .\namespace.yaml
    ```

    ```
    namespace/helloworldapp created
    ```

    This creates the dedicated namespace.

3.  Verify the namespace exists:

    ```powershell
    kubectl get namespace
    ```

    ```
    NAME              STATUS   AGE
    default           Active   17d
    helloworldapp     Active   33s # Age will vary
    kube-node-lease   Active   17d
    kube-public       Active   17d
    kube-system       Active   17d
    ```

4.  Apply the deployment manifests:

    ```powershell
    kubectl apply -f .\api-deployment.yaml
    ```

    ```
    deployment.apps/helloworldapi created
    ```

    ```powershell
    kubectl apply -f .\webapp-deployment.yaml
    ```

    ```
    deployment.apps/helloworldwebapp created
    ```

    This tells Kubernetes to create the Deployments, which in turn will create the specified number of pods. Kubernetes will pull the images from `localhost:5000`.

5.  Check the status of the deployments:

    ```powershell
    kubectl get deployments -n helloworldapp
    ```

    ```
    NAME               READY   UP-TO-DATE   AVAILABLE   AGE
    helloworldapi      1/1     1            1           88s # Age will vary
    helloworldwebapp   1/1     1            1           50s # Age will vary
    ```

    The `READY` column shows how many replicas are currently available and ready to serve requests (desired/current). `UP-TO-DATE` shows how many replicas match the latest deployment configuration. `AVAILABLE` shows how many replicas are available to users.

6.  Check the status of the pods:

    ```powershell
    kubectl get pods -n helloworldapp
    ```

    ```
    NAME                                READY   STATUS    RESTARTS   AGE
    helloworldapi-88bfcdb99-c8g6l       1/1     Running   0          3m40s # Age will vary
    helloworldwebapp-64d69fc995-dmgqq   1/1     Running   0          3m2s # Age will vary
    ```

    This shows the specific pods created by the deployments are running.

7.  Apply the service manifests:

    ```powershell
    kubectl apply -f .\api-service.yaml
    ```

    ```
    service/helloworldapi-service created
    ```

    ```powershell
    kubectl apply -f .\webapp-service.yaml
    ```

    ```
    service/helloworldwebapp-service created
    ```

    This creates the Service objects, making the pods discoverable within the cluster using the service names.

8.  Check the status of the services:

    ```powershell
    kubectl get services -n helloworldapp
    ```

    ```
    NAME                       TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)   AGE
    helloworldapi-service      ClusterIP   10.43.73.150   <none>        80/TCP    59s # Age will vary
    helloworldwebapp-service   ClusterIP   10.43.45.159   <none>        80/TCP    44s # Age will vary
    ```

    The services are created. `TYPE: ClusterIP` and `<none>` for `EXTERNAL-IP` mean they are not exposed outside the cluster directly.

## 9. Test the Kubernetes Deployment

Since the services are `ClusterIP`, we use `kubectl port-forward` to temporarily expose them to your local machine's network for testing.

1.  Open **two *new* PowerShell windows**. Keep the previous windows open if you want to monitor status or logs.
2.  **In the first new PowerShell window (for API Port Forward):** Forward traffic from local port 8080 to the API service's port 80.

    ```powershell
    kubectl port-forward service/helloworldapi-service 8080:80 -n helloworldapp
    ```

    This command will typically block and show output indicating the forwarding is active.

3.  **In the second new PowerShell window (for Web App Port Forward):** Forward traffic from local port 4200 to the Web App service's port 80.

    ```powershell
    kubectl port-forward service/helloworldwebapp-service 4200:80 -n helloworldapp
    ```

    This command will also block, indicating forwarding is active.

4.  **Test the applications:**
    *   Open a web browser and go to `http://localhost:4200`.
        *   The home page should load.
        *   Verify the Application Information:
            *   Current Date & Time: Should be current.
            *   Running Containerized: **True**.
            *   Hostname: Should be the *Kubernetes pod name* (e.g., `helloworldwebapp-deployment-64d69fc995-dmgqq`), not your machine name or a Docker Compose container name.
            *   System Info: Should show the OS details from *inside* the Kubernetes container.
        *   Click the "Click to See WeatherForecast" link.
        *   The MVC app (running in a K8s pod) should successfully call the API (running in another K8s pod) using the Kubernetes service name (`http://helloworldapi-service:80`). The weather data should be displayed in a table.

    *   *(Optional):* Directly test the API via its port-forward at `http://localhost:8080/weatherforecast`.

    ```powershell
    curl http://localhost:8080/weatherforecast
    ```

    ```
    [{"date":"2025-05-19","temperatureC":44,"summary":"Sweltering","temperatureF":111},{"date":"2025-05-20","temperatureC":25,"summary":"Bracing","temperatureF":76},{"date":"2025-05-21","temperatureC":-13,"summary":"Balmy","temperatureF":9},{"date":"2025-05-22","temperatureC":-14,"summary":"Hot","temperatureF":7},{"date":"2025-05-23","temperatureC":54,"summary":"Mild","temperatureF":129}]
    ```

    This confirms your applications are running and communicating correctly within the Kubernetes cluster, accessible via `port-forward`.

## 10. Scaling Deployments in Kubernetes

Kubernetes Deployments make it easy to scale your application horizontally by changing the number of desired replicas.

1.  Open a PowerShell window (not one running `port-forward`) and navigate to the `k8s` directory.
2.  Check the current number of replicas for the web app deployment:

    ```powershell
    kubectl get deployment helloworldwebapp-deployment -n helloworldapp
    ```

    ```
    NAME                        READY   UP-TO-DATE   AVAILABLE   AGE
    helloworldwebapp-deployment   1/1     1            1           ...
    ```

3.  Scale the `helloworldwebapp` deployment to 3 replicas:

    ```powershell
    kubectl scale deployment/helloworldwebapp-deployment --replicas=3 -n helloworldapp
    ```

    ```
    deployment.apps/helloworldwebapp-deployment scaled
    ```

    This command updates the desired state of the deployment to 3 replicas.

4.  Observe the pods in the namespace. You will see new pods being created and transitioning to `Running` status.

    ```powershell
    kubectl get pods -n helloworldapp
    ```

    ```
    NAME                                READY   STATUS    RESTARTS   AGE
    helloworldapi-88bfcdb99-c8g6l       1/1     Running   0          58m # Age will vary
    helloworldwebapp-64d69fc995-dmgqq   1/1     Running   0          57m # Age will vary
    helloworldwebapp-64d69fc995-lmxpz   1/1     Running   0          11m # New pod
    helloworldwebapp-64d69fc995-tl6kj   1/1     Running   0          11m # New pod
    ```

5.  Scale the `helloworldwebapp` deployment back down to 2 replicas:

    ```powershell
    kubectl scale deployment/helloworldwebapp-deployment --replicas=2 -n helloworldapp
    ```

    ```
    deployment.apps/helloworldwebapp-deployment scaled
    ```

6.  Observe the pods again. One pod will be terminated, leaving 2 replicas running.

    ```powershell
    kubectl get pods -n helloworldapp
    ```

    ```
    NAME                                READY   STATUS    RESTARTS   AGE
    helloworldapi-88bfcdb99-c8g6l       1/1     Running   0          58m # Age will vary
    helloworldwebapp-64d69fc995-dmgqq   1/1     Running   0          57m # Age will vary
    helloworldwebapp-64d69fc995-tl6kj   1/1     Running   0          11m # Age will vary
    ```

## 11. Clean Up Local Resources

To clean up all the resources created during this process (Kubernetes deployments, services, namespace, local registry container, and potentially local Docker images), you can use `kubectl` commands and Docker commands. A cleanup script is often used to automate these steps.

### 11.1 Clean Up Kubernetes Resources

1.  Open a PowerShell window (not one running `port-forward`).
2.  List the background port-forward jobs and stop/remove them:

    ```powershell
    get-job
    stop-job 11 # Use the actual Job ID
    remove-job 11
    stop-job 13 # Use the actual Job ID
    remove-job 13
    ```

3.  Verify local access is gone:

    ```powershell
    curl http://localhost:4200
    # curl: (7) Failed to connect... (Expected output)
    curl http://localhost:8080/weatherforecast
    # curl: (7) Failed to connect... (Expected output)
    ```

4.  Delete the namespace. This is often the simplest way to remove all resources within it (deployments, pods, services).

    ```powershell
    kubectl delete namespace helloworldapp
    ```

    ```
    namespace "helloworldapp" deleted
    ```

5.  *(Verification):* Check if the namespace still exists.

    ```powershell
    kubectl get namespace helloworldapp
    ```

    ```
    Error from server (NotFound): namespaces 'helloworldapp' not found
    ```

6.  *(Verification):* Check if any resources remain in the namespace (should show nothing).

    ```powershell
    kubectl get all -n helloworldapp
    ```

    ```
    No resources found in helloworldapp namespace.
    ```

### 11.2 Clean Up Docker Resources

1.  Stop and remove the local Docker registry container:

    ```powershell
    docker stop local-registry
    docker rm local-registry
    ```

    ```
    local-registry
    local-registry
    ```

2.  *(Optional):* Remove the local Docker images of your applications to free up disk space.

    ```powershell
    docker rmi localhost:5000/helloworldapikt:1.0.1 helloworldapikt
    docker rmi localhost:5000/helloworldkt:1.0.1 helloworldkt
    ```

    *(Note: You might need to remove the tagged images before the original ones if they share layers and are still referenced).*

## Conclusion

You have successfully completed the full cycle of setting up your local environment, building container images for your .NET 9 applications, running them locally with Docker Compose, and deploying and scaling them on a local Kubernetes cluster using a local Docker registry. You also cleaned up all the resources afterward. This process demonstrates key steps in a typical cloud-native application development workflow.

