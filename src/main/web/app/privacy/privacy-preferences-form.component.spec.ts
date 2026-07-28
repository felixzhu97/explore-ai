import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PrivacyPreferencesFormComponent } from './privacy-preferences-form.component';
import { PrivacyConsentService } from './privacy-consent.service';
import { NotificationService } from '../core/notification.service';
import { PRIVACY_PAGE_COPY } from './privacy.page.copy';
import { writePrivacyPreferences } from './privacy-consent.storage';

describe('PrivacyPreferencesFormComponent', () => {
  let fixture: ComponentFixture<PrivacyPreferencesFormComponent>;
  let consent: PrivacyConsentService;
  let notifySuccess: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    localStorage.clear();
    notifySuccess = vi.fn();

    await TestBed.configureTestingModule({
      imports: [PrivacyPreferencesFormComponent],
      providers: [
        PrivacyConsentService,
        {
          provide: NotificationService,
          useValue: { showSuccess: notifySuccess, showError: vi.fn() },
        },
      ],
    }).compileComponents();

    consent = TestBed.inject(PrivacyConsentService);
    fixture = TestBed.createComponent(PrivacyPreferencesFormComponent);
    fixture.componentRef.setInput('copy', PRIVACY_PAGE_COPY.en);
    fixture.detectChanges();
  });

  it('should_hydrateForm_when_consentStored', () => {
    const stored = writePrivacyPreferences({
      analytics: true,
      contactEmail: 'user@example.com',
    });
    consent.consent.set(stored);

    fixture = TestBed.createComponent(PrivacyPreferencesFormComponent);
    fixture.componentRef.setInput('copy', PRIVACY_PAGE_COPY.en);
    fixture.detectChanges();

    expect(fixture.componentInstance.preferencesModel()).toEqual({
      analytics: true,
      contactEmail: 'user@example.com',
    });
  });

  it('should_markInvalid_when_contactEmailMalformed', () => {
    fixture.componentInstance.preferencesModel.update(model => ({
      ...model,
      contactEmail: 'not-an-email',
    }));
    fixture.detectChanges();

    const emailField = fixture.componentInstance.preferencesForm.contactEmail();
    emailField.markAsTouched();

    expect(emailField.invalid()).toBe(true);
    expect(emailField.errors()[0]?.kind).toBe('email');
  });

  it('should_persistPreferences_when_saveSubmitted', async () => {
    fixture.componentInstance.preferencesModel.set({
      analytics: false,
      contactEmail: 'privacy@example.com',
    });
    fixture.componentInstance.preferencesForm().markAsDirty();
    fixture.detectChanges();

    fixture.componentInstance.savePreferences();
    await vi.waitFor(() => expect(notifySuccess).toHaveBeenCalledOnce());

    expect(consent.consent()).toMatchObject({
      analytics: false,
      contactEmail: 'privacy@example.com',
      decided: true,
    });
  });
});
