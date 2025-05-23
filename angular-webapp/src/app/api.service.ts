import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment'; // Import environment

// Define interfaces for the data structures expected from the API
interface WeatherForecast {
  date: string; // Or Date if you parse it
  temperatureC: number;
  temperatureF: number; // Calculated
  summary: string;
}

interface SystemInfo {
   currentDateTime: string; // Or Date if you parse it
   isContainerized: boolean;
   hostname: string;
   systemInfo: string;
}


@Injectable({
  providedIn: 'root'
})
export class ApiService {

  // Use the apiUrl from the environment file
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getSystemInfo(): Observable<SystemInfo> {
    // Call the /api/info endpoint
    console.log(`Calling API Info endpoint at: ${this.apiUrl}/api/info`);
    return this.http.get<SystemInfo>(`${this.apiUrl}/api/info`);
  }

  getWeatherForecast(): Observable<WeatherForecast[]> {
    // Call the /api/weatherforecast endpoint
    console.log(`Calling API Weather endpoint at: ${this.apiUrl}/api/weatherforecast`);
    return this.http.get<WeatherForecast[]>(`${this.apiUrl}/api/weatherforecast`);
  }
}