import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ButtonComponent, ButtonVariant, ButtonSize } from './button.component';

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<ButtonComponent>;
  let component: ButtonComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default values', () => {
    expect(component.variant()).toBe('primary');
    expect(component.size()).toBe('md');
    expect(component.loading()).toBe(false);
    expect(component.fullWidth()).toBe(false);
    expect(component.disabled()).toBe(false);
  });

  it('should emit click event', () => {
    const spy = jasmine.createSpy('clicked');
    component.clicked.subscribe(spy);

    component.handleClick(new MouseEvent('click'));

    expect(spy).toHaveBeenCalled();
  });

  it('should not emit click when disabled', () => {
    component.disabled.set(true);
    const spy = jasmine.createSpy('clicked');
    component.clicked.subscribe(spy);

    component.handleClick(new MouseEvent('click'));

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit click when loading', () => {
    component.loading.set(true);
    const spy = jasmine.createSpy('clicked');
    component.clicked.subscribe(spy);

    component.handleClick(new MouseEvent('click'));

    expect(spy).not.toHaveBeenCalled();
  });

  it('should compute button classes correctly', () => {
    expect(component.buttonClasses()).toContain('button--primary');
    expect(component.buttonClasses()).toContain('button--md');
  });

  it('should include full-width class when fullWidth is true', () => {
    component.fullWidth.set(true);
    expect(component.buttonClasses()).toContain('button--full-width');
  });

  it('should update classes when variant changes', () => {
    component.variant.set('secondary');
    expect(component.buttonClasses()).toContain('button--secondary');
  });

  it('should update classes when size changes', () => {
    component.size.set('lg');
    expect(component.buttonClasses()).toContain('button--lg');
  });

  describe('variants', () => {
    const variants: ButtonVariant[] = ['primary', 'secondary', 'ghost', 'danger'];
    variants.forEach((variant) => {
      it(`should support ${variant} variant`, () => {
        component.variant.set(variant);
        expect(component.buttonClasses()).toContain(`button--${variant}`);
      });
    });
  });

  describe('sizes', () => {
    const sizes: ButtonSize[] = ['sm', 'md', 'lg'];
    sizes.forEach((size) => {
      it(`should support ${size} size`, () => {
        component.size.set(size);
        expect(component.buttonClasses()).toContain(`button--${size}`);
      });
    });
  });
});
