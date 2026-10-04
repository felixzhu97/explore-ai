import { Component, inject, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { EvalService, type EvaluationResponse } from './eval.service';
import { ZardButtonComponent } from '../ui/button';
import { I18nService } from '../i18n';
import { requiredText } from '../forms/required-text';

@Component({
  selector: 'app-eval-page',
  imports: [FormField, ZardButtonComponent],
  templateUrl: './eval.page.html',
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6' },
})
export class EvalPageComponent {
  readonly #evalService = inject(EvalService);
  protected readonly i18n = inject(I18nService);

  readonly #draft = signal({ userMessage: '', assistantResponse: '' });
  protected readonly draftForm = form(this.#draft, (path) => {
    requiredText(path.userMessage);
    requiredText(path.assistantResponse);
  });

  readonly result = signal<EvaluationResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    if (this.draftForm().invalid()) {
      return;
    }
    const userMessage = this.#draft().userMessage.trim();
    const assistantResponse = this.#draft().assistantResponse.trim();

    this.isLoading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.#evalService.evaluate({ userMessage, assistantResponse }).subscribe({
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
