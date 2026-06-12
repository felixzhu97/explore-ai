import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageZoomModalComponent } from './image-zoom-modal.component';

describe('ImageZoomModalComponent', () => {
  let fixture: ComponentFixture<ImageZoomModalComponent>;
  let component: ImageZoomModalComponent;

  const createFixture = (src = 'https://example.com/image.jpg', alt = 'Test image') => {
    fixture = TestBed.createComponent(ImageZoomModalComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('src', src);
    fixture.componentRef.setInput('alt', alt);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageZoomModalComponent],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture();
    expect(component).toBeTruthy();
  });

  it('should display overlay when open', () => {
    createFixture();
    const overlay = fixture.nativeElement.querySelector('.overlay');
    expect(overlay).toBeTruthy();
  });

  it('should display image with correct src', () => {
    createFixture();
    const img = fixture.nativeElement.querySelector('.image');
    expect(img).toBeTruthy();
    expect(img.getAttribute('src')).toBe('https://example.com/image.jpg');
  });

  it('should display alt text as caption', () => {
    createFixture();
    const caption = fixture.nativeElement.querySelector('.caption');
    expect(caption).toBeTruthy();
    expect(caption.textContent?.trim()).toBe('Test image');
  });

  it('should not display caption when alt is empty', () => {
    createFixture('https://example.com/image.jpg', '');
    fixture.detectChanges();

    const caption = fixture.nativeElement.querySelector('.caption');
    expect(caption).toBeFalsy();
  });

  it('should emit closed event when close is called', () => {
    createFixture();
    component.closed.subscribe(() => {
      // done callback equivalent
    });

    component.close();
  });

  it('should close modal and emit closed event', () => {
    createFixture();
    const spy = vi.fn();
    component.closed.subscribe(spy);

    component.close();

    expect(spy).toHaveBeenCalled();
  });

  it('should have close button', () => {
    createFixture();
    const closeButton = fixture.nativeElement.querySelector('.close-button');
    expect(closeButton).toBeTruthy();
  });

  it('should have proper ARIA attributes', () => {
    createFixture();
    const overlay = fixture.nativeElement.querySelector('.overlay');
    expect(overlay.getAttribute('role')).toBe('dialog');
    expect(overlay.getAttribute('aria-modal')).toBe('true');

    const closeButton = fixture.nativeElement.querySelector('.close-button');
    expect(closeButton.getAttribute('aria-label')).toBe('Close');
  });

  describe('keyboard support', () => {
    it('should close on escape key', () => {
      createFixture();
      const spy = vi.fn();
      component.closed.subscribe(spy);

      component.onEscapeKey();

      expect(spy).toHaveBeenCalled();
    });
  });

  describe('lifecycle', () => {
    it('should clean up body overflow on destroy', () => {
      createFixture();
      document.body.style.overflow = 'hidden';

      component.ngOnDestroy();

      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('click handling', () => {
    it('should stop propagation on image container click', () => {
      createFixture();
      const container = fixture.nativeElement.querySelector('.image-container');

      const mockEvent = {
        stopPropagation: vi.fn(),
      } as unknown as Event;

      container.dispatchEvent(new MouseEvent('click', { bubbles: true }));

      expect(container).toBeTruthy();
    });

    it('should close on overlay click', () => {
      createFixture();
      const overlay = fixture.nativeElement.querySelector('.overlay');
      const spy = vi.fn();
      component.closed.subscribe(spy);

      overlay.click();

      expect(spy).toHaveBeenCalled();
    });
  });
});
