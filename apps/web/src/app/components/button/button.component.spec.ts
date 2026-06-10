import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ButtonComponent, ButtonVariant, ButtonSize } from './button.component';

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
    expect(button.classList).toContain('button');
    expect(button.classList).toContain('button--primary');
    expect(button.classList).toContain('button--md');
  });

  it('should include full-width class when fullWidth is true', () => {
    createFixture();
    fixture.componentRef.setInput('fullWidth', true);
    fixture.detectChanges();
    
    const button = fixture.nativeElement.querySelector('button');
    expect(button.classList).toContain('button--full-width');
  });

  it('should update classes when variant changes', () => {
    createFixture();
    fixture.componentRef.setInput('variant', 'secondary');
    fixture.detectChanges();
    
    const button = fixture.nativeElement.querySelector('button');
    expect(button.classList).toContain('button--secondary');
  });

  it('should update classes when size changes', () => {
    createFixture();
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();
    
    const button = fixture.nativeElement.querySelector('button');
    expect(button.classList).toContain('button--lg');
  });

  describe('variants', () => {
    const variants: ButtonVariant[] = ['primary', 'secondary', 'ghost', 'danger'];
    variants.forEach((variant) => {
      it(`should support ${variant} variant`, () => {
        createFixture();
        fixture.componentRef.setInput('variant', variant);
        fixture.detectChanges();
        
        const button = fixture.nativeElement.querySelector('button');
        expect(button.classList).toContain(`button--${variant}`);
      });
    });
  });

  describe('sizes', () => {
    const sizes: ButtonSize[] = ['sm', 'md', 'lg'];
    sizes.forEach((size) => {
      it(`should support ${size} size`, () => {
        createFixture();
        fixture.componentRef.setInput('size', size);
        fixture.detectChanges();
        
        const button = fixture.nativeElement.querySelector('button');
        expect(button.classList).toContain(`button--${size}`);
      });
    });
  });
});
