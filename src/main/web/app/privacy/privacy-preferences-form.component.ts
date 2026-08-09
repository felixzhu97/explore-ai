import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { form, FormField, submit, validate, disabled } from '@angular/forms/signals';
import { ZardSwitchComponent } from '../shared/components/switch/switch.component';
import { ZardInputDirective } from '../shared/components/input';
import { ZardButtonComponent } from '../shared/components/button';
import { NotificationService } from '../core/notification.service';
import { PrivacyConsentService } from './privacy-consent.service';
import type { PrivacyPageCopy } from './privacy.page.copy';

export interface PrivacyPreferencesModel {
  analytics: boolean;
  contactEmail: string;
}

@Component({
  selector: 'app-privacy-preferences-form',
  imports: [FormField, ZardSwitchComponent, ZardInputDirective, ZardButtonComponent],
  template: `
    <section>
      <h2 class="text-xl leading-snug font-medium tracking-[-0.02em] text-[#0D0D0D]">
        {{ copy().analyticsHeading }}
      </h2>
      <p class="mt-3 text-[15px] leading-relaxed text-[#0D0D0D]/80">
        {{ copy().analyticsHelp }}
      </p>

      <div class="mt-5 flex items-center justify-between gap-4">
        <span class="text-[15px] text-[#0D0D0D]">{{ copy().analyticsLabel }}</span>
        <z-switch [formField]="preferencesForm.analytics" />
      </div>

      <div class="mt-8">
        <label
          class="block text-[15px] font-medium text-[#0D0D0D]"
          for="privacy-contact-email"
        >
          {{ copy().contactEmailLabel }}
        </label>
        <p class="mt-1 text-[13px] text-[#8F8F8F]">{{ copy().contactEmailHelp }}</p>
        <input
          id="privacy-contact-email"
          type="email"
          autocomplete="email"
          z-input
          class="mt-2 w-full"
          [formField]="preferencesForm.contactEmail"
        />
        @if (
          preferencesForm.contactEmail().invalid() && preferencesForm.contactEmail().touched()
        ) {
          @for (error of preferencesForm.contactEmail().errors(); track error.kind) {
            <p class="mt-1 text-[13px] text-red-600">{{ error.message }}</p>
          }
        }
      </div>

      <div class="mt-6 flex items-center gap-3">
        <button
          type="button"
          z-button
          zType="default"
          class="rounded-full"
          [disabled]="
            saving() || preferencesForm().invalid() || !preferencesForm().dirty()
          "
          (click)="savePreferences()"
        >
          {{ saving() ? copy().savePreferencesSaving : copy().savePreferencesButton }}
        </button>
      </div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPreferencesFormComponent implements OnInit {
  private readonly consent = inject(PrivacyConsentService);
  private readonly notify = inject(NotificationService);

  readonly copy = input.required<PrivacyPageCopy>();

  readonly saving = signal(false);

  readonly preferencesModel = signal<PrivacyPreferencesModel>({
    analytics: false,
    contactEmail: '',
  });

  readonly preferencesForm = form(this.preferencesModel, (schemaPath) => {
    validate(schemaPath.contactEmail, ({ value }) => {
      const email = value().trim();
      if (!email) {
        return undefined;
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return { kind: 'email', message: this.copy().contactEmailInvalid };
      }
      return undefined;
    });
    disabled(schemaPath.analytics, { when: () => this.saving() });
    disabled(schemaPath.contactEmail, { when: () => this.saving() });
  });

  ngOnInit(): void {
    const stored = this.consent.consent();
    this.preferencesModel.set({
      analytics: stored.analytics,
      contactEmail: stored.contactEmail,
    });
  }

  savePreferences(): void {
    submit(this.preferencesForm, async () => {
      this.saving.set(true);
      const { analytics, contactEmail } = this.preferencesModel();
      this.consent.savePreferences({ analytics, contactEmail: contactEmail.trim() });
      this.preferencesForm().reset(this.preferencesModel());
      this.notify.showSuccess(this.copy().savePreferencesSuccess);
      this.saving.set(false);
    });
  }
}
