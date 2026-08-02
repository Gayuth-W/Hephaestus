import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from './core/services/api.service';
import { AnalyzerComponent } from './features/analyzer/analyzer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, AnalyzerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  username = 'admin';
  password = 'admin';
  error = signal<string | null>(null);
  loading = signal(false);

  constructor(public api: ApiService) { }

  login(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.login(this.username, this.password).subscribe({
      next: (res) => { this.api.setSession(res); this.loading.set(false); },
      error: (err) => {
        this.error.set(err?.status === 401
          ? 'Invalid credentials.'
          : 'Cannot reach the backend — is it running on :8080?');
        this.loading.set(false);
      }
    });
  }
}
