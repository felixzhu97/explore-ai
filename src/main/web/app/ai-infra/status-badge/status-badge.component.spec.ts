import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent, BadgeStatus } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;
  let component: StatusBadgeComponent;

  const createFixture = (status: BadgeStatus = 'online', showDot = true, label = '') => {
    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('status', status);
    fixture.componentRef.setInput('showDot', showDot);
    if (label) {
      fixture.componentRef.setInput('label', label);
    }
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture();
    expect(component).toBeTruthy();
  });

  it('should display correct label for each status', () => {
    const statuses: BadgeStatus[] = ['online', 'offline', 'busy', 'error', 'pending'];
    const expectedLabels = ['Online', 'Offline', 'Busy', 'Error', 'Pending'];

    statuses.forEach((status, index) => {
      createFixture(status);

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.textContent?.trim()).toBe(expectedLabels[index]);
    });
  });

  it('should apply correct CSS class for each status', () => {
    const statuses: BadgeStatus[] = ['online', 'offline', 'busy', 'error', 'pending'];
    const expectedClasses: Record<BadgeStatus, string> = {
      online: 'bg-success-light',
      offline: 'bg-black/6',
      busy: 'bg-warning-light',
      error: 'bg-error-light',
      pending: 'bg-primary-light',
    };

    statuses.forEach((status) => {
      createFixture(status);

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.classList).toContain(expectedClasses[status]);
    });
  });

  it('should show dot by default', () => {
    createFixture('online', true);

    const dot = fixture.nativeElement.querySelector('.rounded-full');
    expect(dot).toBeTruthy();
  });

  it('should hide dot when showDot is false', () => {
    createFixture('online', false);

    // When showDot is false, only the badge span exists without dot span
    const allSpans = fixture.nativeElement.querySelectorAll('span');
    expect(allSpans.length).toBe(1);
  });

  it('should show dot when showDot is true', () => {
    createFixture('offline', true);

    const dot = fixture.nativeElement.querySelector('.rounded-full');
    expect(dot).toBeTruthy();
  });

  it('should render custom label when provided', () => {
    createFixture('online', true, 'Custom Label');

    const badge = fixture.nativeElement.querySelector('span');
    expect(badge.textContent?.trim()).toBe('Custom Label');
  });

  it('should prefer custom label over default status label', () => {
    createFixture('busy', true, 'Processing');

    const badge = fixture.nativeElement.querySelector('span');
    expect(badge.textContent?.trim()).toBe('Processing');
  });

  it('should use status label when no custom label provided', () => {
    createFixture('error', true, '');

    const badge = fixture.nativeElement.querySelector('span');
    expect(badge.textContent?.trim()).toBe('Error');
  });

  describe('status colors', () => {
    it('should have online status with success color class', () => {
      createFixture('online');

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.classList).toContain('bg-success-light');
    });

    it('should have offline status with muted color class', () => {
      createFixture('offline');

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.classList).toContain('bg-black/6');
    });

    it('should have busy status with warning color class', () => {
      createFixture('busy');

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.classList).toContain('bg-warning-light');
    });

    it('should have error status with error color class', () => {
      createFixture('error');

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.classList).toContain('bg-error-light');
    });

    it('should have pending status with primary color class', () => {
      createFixture('pending');

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge.classList).toContain('bg-primary-light');
    });
  });

  describe('accessibility', () => {
    it('should render badge element', () => {
      createFixture('online');

      const badge = fixture.nativeElement.querySelector('span');
      expect(badge).toBeTruthy();
    });
  });
});
