import { Component, OnInit } from '@angular/core';
import { ApiService } from './api.service'; // Import the service

// Define interfaces (can be in api.service.ts or a shared file)
interface WeatherForecast {
  date: string;
  temperatureC: number;
  temperatureF: number;
  summary: string;
}

interface SystemInfo {
   currentDateTime: string;
   isContainerized: boolean;
   hostname: string;
   systemInfo: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.css' // or styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'angular-webapp';
  systemInfo: SystemInfo | null = null; // To hold system info
  weatherData: WeatherForecast[] | null = null; // To hold weather data
  errorMessage: string | null = null; // To display API errors
  showWeather = false; // Flag to show weather data

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    // Fetch system info when the component initializes
    this.getSystemInformation();
  }

  getSystemInformation(): void {
     this.errorMessage = null; // Clear previous errors
     this.apiService.getSystemInfo().subscribe({
       next: (data) => {
         this.systemInfo = data;
         console.log('System Info received:', data);
       },
       error: (error) => {
         console.error('Error fetching system info:', error);
         this.errorMessage = `Error fetching system info: ${error.message || error.statusText || error}`;
       }
     });
  }

  getWeather(): void {
    this.errorMessage = null; // Clear previous errors
    this.showWeather = true; // Show the weather section
    this.weatherData = null; // Clear previous weather data

    this.apiService.getWeatherForecast().subscribe({
      next: (data) => {
        this.weatherData = data;
        console.log('Weather data received:', data);
      },
      error: (error) => {
        console.error('Error fetching weather data:', error);
        this.errorMessage = `Error fetching weather data: ${error.message || error.statusText || error}`;
        this.weatherData = []; // Set to empty array on error
      }
    });
  }

  // Optional: Method to go back to just showing info (if you add routing later)
  // For this simple app, we just fetch info again
  goBackToInfo(): void {
     this.showWeather = false;
     this.weatherData = null; // Clear weather data
     this.errorMessage = null; // Clear errors
     this.getSystemInformation(); // Re-fetch system info
  }
}