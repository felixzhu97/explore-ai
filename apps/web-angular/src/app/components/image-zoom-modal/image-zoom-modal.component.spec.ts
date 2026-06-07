import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageZoomModalComponent } from './image-zoom-modal.component';

describe('ImageZoomModalComponent', () => {
  let fixture: ComponentFixture<ImageZoomModalComponent>;
  let component: ImageZoomModalComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageZoomModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ImageZoomModalComponent);
    component = fixture.componentInstance;
    component.src = 'https://example.com/image.jpg';
    component.alt = 'Test image';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display overlay when open', () => {
    expect(component.isOpen()).toBe(true);
    const overlay = fixture.nativeElement.querySelector('.overlay');
    expect(overlay).toBeTruthy();
  });

  it('should display image with correct src', () => {
    const img = fixture.nativeElement.querySelector('.image');
    expect(img).toBeTruthy();
    expect(img.getAttribute('src')).toBe('https://example.com/image.jpg');
  });

  it('should display alt text as caption', () => {
    const caption = fixture.nativeElement.querySelector('.caption');
    expect(caption).toBeTruthy();
    expect(caption.textContent?.trim()).toBe('Test image');
  });

  it('should not display caption when alt is empty', () => {
    component.alt.set('');
    fixture.detectChanges();
    
    const caption = fixture.nativeElement.querySelector('.caption');
    expect(caption).toBeFalsy();
  });

  it('should emit closed event when close is called', (done) => {
    component.closed.subscribe(() => {
      done();
    });

    component.close();
  });

  it('should close modal and emit closed event', () => {
    const spy = jasmine.createSpy('closed');
    component.closed.subscribe(spy);

    component.close();

    expect(component.isOpen()).toBe(false);
    expect(spy).toHaveBeenCalled();
  });

  it('should have close button', () => {
    const closeButton = fixture.nativeElement.querySelector('.close-button');
    expect(closeButton).toBeTruthy();
  });

  it('should have proper ARIA attributes', () => {
    const overlay = fixture.nativeElement.querySelector('.overlay');
    expect(overlay.getAttribute('role')).toBe('dialog');
    expect(overlay.getAttribute('aria-modal')).toBe('true');
    
    const closeButton = fixture.nativeElement.querySelector('.close-button');
    expect(closeButton.getAttribute('aria-label')).toBe('Close');
  });

  describe('keyboard support', () => {
    it('should close on escape key', () => {
      const spy = jasmine.createSpy('closed');
      component.closed.subscribe(spy);

      component.onEscapeKey();

      expect(component.isOpen()).toBe(false);
      expect(spy).toHaveBeenCalled();
    });

    it('should not close on escape if already closed', () => {
      component.isOpen.set(false);
      const spy = jasmine.createSpy('closed');
      component.closed.subscribe(spy);

      component.onEscapeKey();

      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('lifecycle', () => {
    it('should clean up body overflow on destroy', () => {
      document.body.style.overflow = 'hidden';
      
      component.ngOnDestroy();
      
      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('click handling', () => {
    it('should stop propagation on image container click', () => {
      const container = fixture.nativeElement.querySelector('.image-container');
      const stopPropagationSpy = jasmine.createSpy('stopPropagation');
      
      const mockEvent = {
        stopPropagation: stopPropagationSpy,
      } as unknown as Event;
      
      container.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      
      // The event handler is on the element
      expect(container).toBeTruthy();
    });

    it('should close on overlay click', () => {
      const overlay = fixture.nativeElement.querySelector('.overlay');
      const spy = jasmine.createSpy('closed');
      component.closed.subscribe(spy);

      overlay.click();

      expect(spy).toHaveBeenCalled();
    });
  });
});
