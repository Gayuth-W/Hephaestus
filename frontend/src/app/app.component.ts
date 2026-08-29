import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from './core/services/api.service';
import { AnalyzerComponent } from './features/analyzer/analyzer.component';
import { AuthResponse } from './shared/models/models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, AnalyzerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  email = '';
  password = '';
  mode = signal<'signin' | 'register'>('signin');
  error = signal<string | null>(null);
  loading = signal(false);

  constructor(public api: ApiService) {}

  toggle(): void {
    this.mode.set(this.mode() === 'signin' ? 'register' : 'signin');
    this.error.set(null);
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);
    const call = this.mode() === 'signin'
      ? this.api.login(this.email, this.password)
      : this.api.register(this.email, this.password);
    call.subscribe({
      next: (res: AuthResponse) => { this.api.setSession(res); this.loading.set(false); },
      error: (err: any) => { this.error.set(this.messageFor(err)); this.loading.set(false); }
    });
  }

  private messageFor(err: { status?: number; error?: { error?: string } }): string {
    if (err?.error?.error) { return err.error.error; }
    if (err?.status === 0) { return 'Cannot reach the backend — is it running on :8080?'; }
    return this.mode() === 'signin' ? 'Sign in failed.' : 'Could not create account.';
  }
}
