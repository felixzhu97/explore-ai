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
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons.length).toBe(3);
  });

  it('should display option labels', () => {
    createFixture();
    const labels = fixture.nativeElement.querySelectorAll('button');
    expect(labels[0].textContent?.trim()).toBe('Test');
    expect(labels[1].textContent?.trim()).toBe('Demo');
    expect(labels[2].textContent?.trim()).toBe('Other');
  });

  it('should mark active option', () => {
    createFixture();
    const buttons = fixture.nativeElement.querySelectorAll('button');
    // Active button has text-text class, inactive has text-text-tertiary
    expect(buttons[0].classList).toContain('text-text');
    expect(buttons[1].classList).toContain('text-text-tertiary');
  });

  it('should emit change event when selecting option', () => {
    createFixture();
    component.changed.subscribe((value) => {
      expect(value).toBe('demo');
    });

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[1].click();
  });

  it('should not emit change when selecting already selected option', () => {
    createFixture();
    const spy = vi.fn();
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[0].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit change when selecting disabled option', () => {
    createFixture();
    const spy = vi.fn();
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[2].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should have proper ARIA attributes', () => {
    createFixture();
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons[0].getAttribute('role')).toBe('tab');
    expect(buttons[0].getAttribute('aria-selected')).toBe('true');
    expect(buttons[2].getAttribute('aria-disabled')).toBe('true');
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

      const buttons = fixture.nativeElement.querySelectorAll('button');
      // Active button has bg-surface-secondary
      expect(buttons[1].classList).toContain('bg-surface-secondary');
    });
  });

  describe('snapshot tests', () => {
    it('should match snapshot of rendered options', () => {
      createFixture();
      const buttons = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLElement>);
      const snapshot = buttons.map(btn => ({
        text: btn.textContent?.trim(),
        ariaSelected: btn.getAttribute('aria-selected'),
        ariaDisabled: btn.getAttribute('aria-disabled'),
        role: btn.getAttribute('role'),
      }));

      expect(snapshot).toEqual([
        {
          text: 'Test',
          ariaSelected: 'true',
          ariaDisabled: 'false',
          role: 'tab',
        },
        {
          text: 'Demo',
          ariaSelected: 'false',
          ariaDisabled: 'false',
          role: 'tab',
        },
        {
          text: 'Other',
          ariaSelected: 'false',
          ariaDisabled: 'true',
          role: 'tab',
        },
      ]);
    });
  });

  describe('container element', () => {
    it('should have proper role attribute on container', () => {
      createFixture();
      const container = fixture.nativeElement.querySelector('[role="tablist"]');
      expect(container).toBeTruthy();
    });
  });
});
