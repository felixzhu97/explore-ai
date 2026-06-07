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
});
