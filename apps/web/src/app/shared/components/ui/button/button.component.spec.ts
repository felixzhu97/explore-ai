import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ButtonComponent } from './button.component';

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<ButtonComponent>;
  let component: ButtonComponent;

  const createFixture = () => {
    fixture = TestBed.createComponent(ButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonComponent],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture();
    expect(component).toBeTruthy();
  });

  it('should have default values', () => {
    createFixture();
    expect(component.variant()).toBe('primary');
    expect(component.size()).toBe('md');
    expect(component.loading()).toBe(false);
    expect(component.fullWidth()).toBe(false);
    expect(component.disabled()).toBe(false);
  });

  it('should emit click event', () => {
    createFixture();
    const spy = vi.fn();
    component.clicked.subscribe(spy);

    component.handleClick(new MouseEvent('click'));

    expect(spy).toHaveBeenCalled();
  });

  it('should not emit click when disabled', () => {
    createFixture();
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const spy = vi.fn();
    component.clicked.subscribe(spy);

    component.handleClick(new MouseEvent('click'));

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit click when loading', () => {
    createFixture();
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const spy = vi.fn();
    component.clicked.subscribe(spy);

    component.handleClick(new MouseEvent('click'));

    expect(spy).not.toHaveBeenCalled();
  });

  it('should render button with default classes', () => {
    createFixture();
    const button = fixture.nativeElement.querySelector('button');
    expect(button.classList).toContain('inline-flex');
    expect(button.classList).toContain('items-center');
  });

  it('should include w-full class when fullWidth is true', () => {
    createFixture();
    fixture.componentRef.setInput('fullWidth', true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button');
    expect(button.classList).toContain('w-full');
  });

  describe('variants', () => {
    it('should apply primary variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'primary');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('bg-primary');
      expect(button.classList).toContain('text-white');
    });

    it('should apply secondary variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'secondary');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('bg-surface');
      expect(button.classList).toContain('text-primary');
    });

    it('should apply ghost variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'ghost');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('bg-transparent');
      expect(button.classList).toContain('text-primary');
    });

    it('should apply danger variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'danger');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('bg-error');
      expect(button.classList).toContain('text-white');
    });
  });

  describe('sizes', () => {
    it('should apply sm size styles', () => {
      createFixture();
      fixture.componentRef.setInput('size', 'sm');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('px-3');
      expect(button.classList).toContain('py-1.5');
      expect(button.classList).toContain('text-xs');
    });

    it('should apply md size styles', () => {
      createFixture();
      fixture.componentRef.setInput('size', 'md');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('text-sm');
    });

    it('should apply lg size styles', () => {
      createFixture();
      fixture.componentRef.setInput('size', 'lg');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button');
      expect(button.classList).toContain('text-base');
      expect(button.classList).toContain('px-6');
    });
  });
});
