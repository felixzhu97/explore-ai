import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent, BadgeStatus } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;
  let component: StatusBadgeComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display correct label for each status', () => {
    const statuses: BadgeStatus[] = ['online', 'offline', 'busy', 'error', 'pending'];
    const expectedLabels = ['Online', 'Offline', 'Busy', 'Error', 'Pending'];

    statuses.forEach((status, index) => {
      component.status.set(status);
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge.textContent?.trim()).toBe(expectedLabels[index]);
    });
  });

  it('should apply correct CSS class for each status', () => {
    const statuses: BadgeStatus[] = ['online', 'offline', 'busy', 'error', 'pending'];

    statuses.forEach((status) => {
      component.status.set(status);
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge).toHaveClass(`badge--${status}`);
    });
  });

  it('should show dot by default', () => {
    component.status.set('online');
    fixture.detectChanges();
    
    const dot = fixture.nativeElement.querySelector('.badge__dot');
    expect(dot).toBeTruthy();
  });

  it('should hide dot when showDot is false', () => {
    component.status.set('online');
    component.showDot.set(false);
    fixture.detectChanges();
    
    const dot = fixture.nativeElement.querySelector('.badge__dot');
    expect(dot).toBeFalsy();
  });

  it('should show dot when showDot is true', () => {
    component.status.set('offline');
    component.showDot.set(true);
    fixture.detectChanges();
    
    const dot = fixture.nativeElement.querySelector('.badge__dot');
    expect(dot).toBeTruthy();
  });

  it('should render custom label when provided', () => {
    component.status.set('online');
    component.label.set('Custom Label');
    fixture.detectChanges();
    
    const badge = fixture.nativeElement.querySelector('.badge');
    expect(badge.textContent?.trim()).toBe('Custom Label');
  });

  it('should prefer custom label over default status label', () => {
    component.status.set('busy');
    component.label.set('Processing');
    fixture.detectChanges();
    
    const badge = fixture.nativeElement.querySelector('.badge');
    expect(badge.textContent?.trim()).toBe('Processing');
    expect(badge.textContent).not.toContain('Busy');
  });

  it('should use status label when no custom label provided', () => {
    component.status.set('error');
    component.label.set('');
    fixture.detectChanges();
    
    const badge = fixture.nativeElement.querySelector('.badge');
    expect(badge.textContent?.trim()).toBe('Error');
  });

  describe('status colors', () => {
    it('should have online status with success color class', () => {
      component.status.set('online');
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge).toHaveClass('badge--online');
    });

    it('should have offline status with muted color class', () => {
      component.status.set('offline');
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge).toHaveClass('badge--offline');
    });

    it('should have busy status with warning color class', () => {
      component.status.set('busy');
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge).toHaveClass('badge--busy');
    });

    it('should have error status with error color class', () => {
      component.status.set('error');
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge).toHaveClass('badge--error');
    });

    it('should have pending status with primary color class', () => {
      component.status.set('pending');
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge).toHaveClass('badge--pending');
    });
  });

  describe('accessibility', () => {
    it('should have proper role attribute', () => {
      component.status.set('online');
      fixture.detectChanges();
      
      const badge = fixture.nativeElement.querySelector('.badge');
      expect(badge.getAttribute('role')).toBe('status');
    });
  });
});
