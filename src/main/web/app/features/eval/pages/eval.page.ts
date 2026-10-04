import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EvalService, type EvaluationResponse } from '../services/eval.service';
import { ZardButtonComponent } from '../../../shared/components/button';
import { I18nService } from '../../../core/i18n';

@Component({
  selector: 'app-eval-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './eval.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class EvalPageComponent {
  private readonly evalService = inject(EvalService);
  protected readonly i18n = inject(I18nService);

  readonly userMessage = signal('');
  readonly assistantResponse = signal('');
  readonly result = signal<EvaluationResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    const userMessage = this.userMessage().trim();
    const assistantResponse = this.assistantResponse().trim();
    if (!userMessage || !assistantResponse) {
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.evalService.evaluate({ userMessage, assistantResponse }).subscribe({
      next: (response) => {
        this.result.set(response);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().eval.errors.requestFailed);
        this.isLoading.set(false);
      },
    });
  }
}
