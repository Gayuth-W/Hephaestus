import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalyzeResponse, LoginResponse } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private tokenValue = signal<string | null>(null);
  readonly token = this.tokenValue.asReadonly();
  readonly username = signal<string | null>(null);

  constructor(private http: HttpClient) { }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password });
  }

  setSession(res: LoginResponse): void {
    this.tokenValue.set(res.token);
    this.username.set(res.username);
  }

  logout(): void {
    this.tokenValue.set(null);
    this.username.set(null);
  }

  analyze(trace: unknown, type: string): Observable<AnalyzeResponse> {
    return this.http.post<AnalyzeResponse>('/api/traces/analyze', { type, trace }, {
      headers: new HttpHeaders({ Authorization: `Bearer ${this.tokenValue()}` })
    });
  }
}
