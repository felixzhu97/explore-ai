import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EvalService } from './eval.service';
import type { EvaluationResponse } from './eval.model';
import { ZardButtonComponent } from '../shared/components/button';

@Component({
  selector: 'app-eval-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './eval.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class EvalPageComponent {
  private readonly evalService = inject(EvalService);

  readonly userMessage = signal('');
  readonly assistantResponse = signal('');
  readonly result = signal<EvaluationResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    const userMessage = this.userMessage().trim();
    const assistantResponse = this.assistantResponse().trim();
    if (!userMessage || !assistantResponse) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.evalService.evaluate({ userMessage, assistantResponse }).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Evaluation request failed');
        this.loading.set(false);
      },
    });
  }
}
