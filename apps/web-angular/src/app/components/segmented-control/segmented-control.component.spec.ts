import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SegmentedControlComponent, SegmentedControlOption } from './segmented-control.component';

describe('SegmentedControlComponent', () => {
  let fixture: ComponentFixture<SegmentedControlComponent>;
  let component: SegmentedControlComponent<'test' | 'demo' | 'other'>;

  const mockOptions: SegmentedControlOption<'test' | 'demo' | 'other'>[] = [
    { value: 'test', label: 'Test' },
    { value: 'demo', label: 'Demo' },
    { value: 'other', label: 'Other', disabled: true },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SegmentedControlComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SegmentedControlComponent);
    component = fixture.componentInstance;
    component.options = mockOptions;
    component.value = 'test';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render all options', () => {
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons.length).toBe(3);
  });

  it('should display option labels', () => {
    const labels = fixture.nativeElement.querySelectorAll('.option');
    expect(labels[0].textContent?.trim()).toBe('Test');
    expect(labels[1].textContent?.trim()).toBe('Demo');
    expect(labels[2].textContent?.trim()).toBe('Other');
  });

  it('should mark active option', () => {
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons[0]).toHaveClass('option--active');
    expect(buttons[1]).not.toHaveClass('option--active');
  });

  it('should emit change event when selecting option', (done) => {
    component.changed.subscribe((value) => {
      expect(value).toBe('demo');
      done();
    });

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    buttons[1].click();
  });

  it('should not emit change when selecting already selected option', () => {
    const spy = jasmine.createSpy('changed');
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    buttons[0].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit change when selecting disabled option', () => {
    const spy = jasmine.createSpy('changed');
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    buttons[2].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit change when selecting disabled option even with click', () => {
    const spy = jasmine.createSpy('changed');
    component.changed.subscribe(spy);

    const buttons = fixture.nativeElement.querySelectorAll('.option');
    (buttons[2] as HTMLButtonElement).disabled = true;
    buttons[2].click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should have proper ARIA attributes', () => {
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons[0].getAttribute('role')).toBe('tab');
    expect(buttons[0].getAttribute('aria-selected')).toBe('true');
    expect(buttons[2].getAttribute('aria-disabled')).toBe('true');
  });

  it('should set data-active attribute correctly', () => {
    const buttons = fixture.nativeElement.querySelectorAll('.option');
    expect(buttons[0].getAttribute('data-active')).toBe('true');
    expect(buttons[1].getAttribute('data-active')).toBe('false');
  });

  describe('generic type support', () => {
    it('should work with string union type', () => {
      const options: SegmentedControlOption<'a' | 'b' | 'c'>[] = [
        { value: 'a', label: 'A' },
        { value: 'b', label: 'B' },
        { value: 'c', label: 'C' },
      ];
      
      component.options.set(options);
      component.value.set('b');
      fixture.detectChanges();
      
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons[1]).toHaveClass('option--active');
    });
  });

  describe('snapshot tests', () => {
    it('should match snapshot of rendered options', () => {
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      const snapshot = Array.from(buttons).map(btn => ({
        text: btn.textContent?.trim(),
        isActive: btn.classList.contains('option--active'),
        isDisabled: btn.classList.contains('option--disabled'),
        ariaSelected: btn.getAttribute('aria-selected'),
        ariaDisabled: btn.getAttribute('aria-disabled'),
        role: btn.getAttribute('role'),
      }));
      
      expect(snapshot).toEqual([
        { text: 'Test', isActive: true, isDisabled: false, ariaSelected: 'true', ariaDisabled: null, role: 'tab' },
        { text: 'Demo', isActive: false, isDisabled: false, ariaSelected: 'false', ariaDisabled: null, role: 'tab' },
        { text: 'Other', isActive: false, isDisabled: true, ariaSelected: 'false', ariaDisabled: 'true', role: 'tab' },
      ]);
    });

    it('should render all options with correct structure', () => {
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      
      buttons.forEach((btn, index) => {
        expect(btn.tagName).toBe('BUTTON');
        expect(btn.getAttribute('type')).toBe('button');
        expect(btn.getAttribute('role')).toBe('tab');
      });
    });
  });

  describe('option rendering', () => {
    it('should update UI when options change', () => {
      const newOptions: SegmentedControlOption<'test' | 'demo' | 'other'>[] = [
        { value: 'test', label: 'Test Updated' },
        { value: 'demo', label: 'Demo Updated' },
      ];
      
      component.options.set(newOptions);
      fixture.detectChanges();
      
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons.length).toBe(2);
      expect(buttons[0].textContent?.trim()).toBe('Test Updated');
      expect(buttons[1].textContent?.trim()).toBe('Demo Updated');
    });

    it('should handle dynamic value changes', () => {
      component.value.set('demo');
      fixture.detectChanges();
      
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons[1]).toHaveClass('option--active');
      expect(buttons[0]).not.toHaveClass('option--active');
    });
  });

  describe('selectOption method', () => {
    it('should call selectOption directly', () => {
      const selectOptionSpy = spyOn(component, 'selectOption');
      
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      buttons[1].click();
      
      expect(selectOptionSpy).toHaveBeenCalledWith(mockOptions[1]);
    });

    it('should not select option with undefined value', () => {
      const newOptions: SegmentedControlOption<string>[] = [
        { value: 'test', label: 'Test' },
      ];
      
      component.options.set(newOptions);
      component.value.set('nonexistent' as any);
      fixture.detectChanges();
      
      const buttons = fixture.nativeElement.querySelectorAll('.option');
      expect(buttons[0]).not.toHaveClass('option--active');
    });
  });

  describe('container element', () => {
    it('should have proper role attribute on container', () => {
      const container = fixture.nativeElement.querySelector('.container');
      expect(container.getAttribute('role')).toBe('tablist');
    });
  });
});
