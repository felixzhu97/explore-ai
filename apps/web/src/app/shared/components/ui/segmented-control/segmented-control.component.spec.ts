import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SegmentedControlComponent, SegmentedControlOption } from './segmented-control.component';

describe('SegmentedControlComponent', () => {
  let fixture: ComponentFixture<SegmentedControlComponent<string>>;
  let component: SegmentedControlComponent<string>;

  const mockOptions: SegmentedControlOption<string>[] = [
    { value: 'test', label: 'Test' },
    { value: 'demo', label: 'Demo' },
    { value: 'other', label: 'Other', disabled: true },
  ];

  const createFixture = () => {
    fixture = TestBed.createComponent(SegmentedControlComponent);
    component = fixture.componentInstance as any;
    fixture.componentRef.setInput('options', mockOptions);
    fixture.componentRef.setInput('value', 'test');
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SegmentedControlComponent],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture();
    expect(component).toBeTruthy();
  });

  it('should render all options', () => {
    createFixture();
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons.length).toBe(3);
  });

  it('should display option labels', () => {
    createFixture();
    const labels = fixture.nativeElement.querySelectorAll('.option');
    expect(labels[0].textContent?.trim()).toBe('Test');
    expect(labels[1].textContent?.trim()).toBe('Demo');
    expect(labels[2].textContent?.trim()).toBe('Other');
  });

  it('should mark active option', () => {
    createFixture();
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons[0].classList).toContain('option--active');
    expect(buttons[1].classList).not.toContain('option--active');
  });

  it('should emit change event when selecting option', () => {
    createFixture();
    component.changed.subscribe((value) => {
      expect(value).toBe('demo');
    });

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    buttons[1].click();
  });

  it('should not emit change when selecting already selected option', () => {
    createFixture();
    const spy = vi.fn();
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    buttons[0].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit change when selecting disabled option', () => {
    createFixture();
    const spy = vi.fn();
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    buttons[2].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit change when selecting disabled option even with click', () => {
    createFixture();
    const spy = vi.fn();
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    (buttons[2] as HTMLButtonElement).disabled = true;
    buttons[2].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should have proper ARIA attributes', () => {
    createFixture();
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons[0].getAttribute('role')).toBe('tab');
    expect(buttons[0].getAttribute('aria-selected')).toBe('true');
    expect(buttons[2].getAttribute('aria-disabled')).toBe('true');
  });

  it('should set data-active attribute correctly', () => {
    createFixture();
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons[0].getAttribute('data-active')).toBe('true');
    expect(buttons[1].getAttribute('data-active')).toBe('false');
  });

  describe('generic type support', () => {
    it('should work with string union type', () => {
      createFixture();
      const options: SegmentedControlOption<'a' | 'b' | 'c'>[] = [
        { value: 'a', label: 'A' },
        { value: 'b', label: 'B' },
        { value: 'c', label: 'C' },
      ];

      fixture.componentRef.setInput('options', options);
      fixture.componentRef.setInput('value', 'b');
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons[1].classList).toContain('option--active');
    });
  });

  describe('snapshot tests', () => {
    it('should match snapshot of rendered options', () => {
      createFixture();
      const buttons = fixture.nativeElement.querySelectorAll('.option') as NodeListOf<HTMLElement>;
      const snapshot = Array.from(buttons).map((btn: HTMLElement) => ({
        text: btn.textContent?.trim(),
        isActive: btn.classList.contains('option--active'),
        isDisabled: btn.classList.contains('option--disabled'),
        ariaSelected: btn.getAttribute('aria-selected'),
        ariaDisabled: btn.getAttribute('aria-disabled'),
        role: btn.getAttribute('role'),
      }));

      expect(snapshot).toEqual([
        {
          text: 'Test',
          isActive: true,
          isDisabled: false,
          ariaSelected: 'true',
          ariaDisabled: 'false',
          role: 'tab',
        },
        {
          text: 'Demo',
          isActive: false,
          isDisabled: false,
          ariaSelected: 'false',
          ariaDisabled: 'false',
          role: 'tab',
        },
        {
          text: 'Other',
          isActive: false,
          isDisabled: true,
          ariaSelected: 'false',
          ariaDisabled: 'true',
          role: 'tab',
        },
      ]);
    });

    it('should render all options with correct structure', () => {
      createFixture();
      const buttons = fixture.nativeElement.querySelectorAll('.option') as NodeListOf<HTMLElement>;

      buttons.forEach((btn: HTMLElement) => {
        expect(btn.tagName).toBe('BUTTON');
        expect(btn.getAttribute('type')).toBe('button');
        expect(btn.getAttribute('role')).toBe('tab');
      });
    });
  });

  describe('option rendering', () => {
    it('should update UI when options change', () => {
      createFixture();
      const newOptions: SegmentedControlOption<'test' | 'demo' | 'other'>[] = [
        { value: 'test', label: 'Test Updated' },
        { value: 'demo', label: 'Demo Updated' },
      ];

      fixture.componentRef.setInput('options', newOptions);
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons.length).toBe(2);
      expect(buttons[0].textContent?.trim()).toBe('Test Updated');
      expect(buttons[1].textContent?.trim()).toBe('Demo Updated');
    });

    it('should handle dynamic value changes', () => {
      createFixture();
      fixture.componentRef.setInput('value', 'demo');
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons[1].classList).toContain('option--active');
      expect(buttons[0].classList).not.toContain('option--active');
    });
  });

  describe('selectOption method', () => {
    it('should call selectOption directly', () => {
      createFixture();
      const selectOptionSpy = vi.spyOn(component, 'selectOption');

      const buttons = fixture.nativeElement.querySelectorAll('.option');
      buttons[1].click();

      expect(selectOptionSpy).toHaveBeenCalledWith(mockOptions[1]);
    });

    it('should not select option with undefined value', () => {
      createFixture();
      const newOptions: SegmentedControlOption<string>[] = [{ value: 'test', label: 'Test' }];

      fixture.componentRef.setInput('options', newOptions);
      fixture.componentRef.setInput('value', 'nonexistent' as any);
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons[0].classList).not.toContain('option--active');
    });
  });

  describe('container element', () => {
    it('should have proper role attribute on container', () => {
      createFixture();
      const container = fixture.nativeElement.querySelector('.container');
      expect(container.getAttribute('role')).toBe('tablist');
    });
  });
});
