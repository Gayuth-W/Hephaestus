import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalyzeResponse, AuthResponse, ReportSummary } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private tokenValue = signal<string | null>(null);
  readonly token = this.tokenValue.asReadonly();
  readonly email = signal<string | null>(null);

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password });
  }

  register(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', { email, password });
  }

  setSession(res: AuthResponse): void {
    this.tokenValue.set(res.token);
    this.email.set(res.email);
  }

  logout(): void {
    this.tokenValue.set(null);
    this.email.set(null);
  }

  analyze(trace: unknown, type: string): Observable<AnalyzeResponse> {
    return this.http.post<AnalyzeResponse>('/api/traces/analyze', { type, trace }, {
      headers: new HttpHeaders({ Authorization: `Bearer ${this.tokenValue()}` })
    });
  }

  history(): Observable<ReportSummary[]> {
    return this.http.get<ReportSummary[]>('/api/reports', {
      headers: new HttpHeaders({ Authorization: `Bearer ${this.tokenValue()}` })
    });
  }

  getReport(id: string): Observable<AnalyzeResponse> {
    return this.http.get<AnalyzeResponse>(`/api/reports/${id}`, {
      headers: new HttpHeaders({ Authorization: `Bearer ${this.tokenValue()}` })
    });
  }
}
