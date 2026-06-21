import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToolResultComponent, ToolCall } from './tool-result.component';

describe('ToolResultComponent', () => {
  let fixture: ComponentFixture<ToolResultComponent>;
  let component: ToolResultComponent;

  const createToolCall = (overrides: Partial<ToolCall> = {}): ToolCall => ({
    id: 'tool_1',
    name: 'test_tool',
    input: { query: 'test' },
    status: 'running',
    ...overrides,
  });

  const createFixture = (toolCall: ToolCall) => {
    fixture = TestBed.createComponent(ToolResultComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('toolCall', toolCall);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolResultComponent],
    }).compileComponents();
  });

  describe('component creation', () => {
    it('should create', () => {
      createFixture(createToolCall());
      expect(component).toBeTruthy();
    });

    it('should initialize with expanded signal set to false', () => {
      createFixture(createToolCall());
      expect(component.expanded()).toBe(false);
    });
  });

  describe('toggleExpanded', () => {
    it('should toggle expanded state from false to true', () => {
      createFixture(createToolCall());
      expect(component.expanded()).toBe(false);

      component.toggleExpanded();
      expect(component.expanded()).toBe(true);
    });

    it('should toggle expanded state from true to false', () => {
      createFixture(createToolCall());
      component.expanded.set(true);

      component.toggleExpanded();
      expect(component.expanded()).toBe(false);
    });
  });

  describe('formatJson', () => {
    it('should format simple object', () => {
      createFixture(createToolCall());
      const result = component.formatJson({ key: 'value' });
      expect(result).toContain('key');
      expect(result).toContain('value');
    });

    it('should format nested object', () => {
      createFixture(createToolCall());
      const result = component.formatJson({ nested: { key: 'value' } });
      expect(result).toContain('nested');
      expect(result).toContain('key');
    });

    it('should format array', () => {
      createFixture(createToolCall());
      const result = component.formatJson(['item1', 'item2']);
      expect(result).toContain('item1');
      expect(result).toContain('item2');
    });

    it('should handle empty object', () => {
      createFixture(createToolCall());
      const result = component.formatJson({});
      expect(result).toBe('{}');
    });

    it('should handle empty array', () => {
      createFixture(createToolCall());
      const result = component.formatJson([]);
      expect(result).toBe('[]');
    });
  });

  describe('formatOutput', () => {
    it('should return plain text output as-is', () => {
      createFixture(createToolCall());
      const output = 'This is plain text';
      const result = component.formatOutput(output);
      expect(result).toBe(output);
    });

    it('should format JSON object output', () => {
      createFixture(createToolCall());
      const output = '{"key": "value", "count": 42}';
      const result = component.formatOutput(output);
      expect(result).toContain('key');
      expect(result).toContain('value');
      expect(result).toContain('42');
    });

    it('should handle image URLs', () => {
      createFixture(createToolCall());
      const output = 'Check this image: https://example.com/photo.png';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('https://example.com/photo.png');
    });

    it('should handle empty string', () => {
      createFixture(createToolCall());
      const result = component.formatOutput('');
      expect(result).toBe('');
    });
  });

  describe('tool call status rendering', () => {
    it('should show pending status indicator', () => {
      createFixture(createToolCall({ status: 'pending' }));
      const indicator = fixture.nativeElement.querySelector('.text-text-tertiary');
      expect(indicator).toBeTruthy();
    });

    it('should show running status indicator with spinner', () => {
      createFixture(createToolCall({ status: 'running' }));
      const spinner = fixture.nativeElement.querySelector('.animate-spin');
      expect(spinner).toBeTruthy();
    });

    it('should show success status indicator', () => {
      createFixture(createToolCall({ status: 'success' }));
      const successText = fixture.nativeElement.querySelector('.text-success');
      expect(successText).toBeTruthy();
      expect(successText.textContent).toContain('✓');
    });

    it('should show error status indicator', () => {
      createFixture(createToolCall({ status: 'error' }));
      const errorText = fixture.nativeElement.querySelector('.text-error');
      expect(errorText).toBeTruthy();
      expect(errorText.textContent).toContain('✗');
    });
  });

  describe('expand/collapse functionality', () => {
    it('should not show tool body when collapsed', () => {
      createFixture(createToolCall());
      component.expanded.set(false);
      fixture.detectChanges();

      const toolBody = fixture.nativeElement.querySelector('.border-t');
      expect(toolBody).toBeFalsy();
    });

    it('should show tool body when expanded', () => {
      createFixture(createToolCall());
      component.expanded.set(true);
      fixture.detectChanges();

      const toolBody = fixture.nativeElement.querySelector('.border-t');
      expect(toolBody).toBeTruthy();
    });

    it('should show input section when expanded', () => {
      createFixture(createToolCall());
      component.expanded.set(true);
      fixture.detectChanges();

      const inputSection = fixture.nativeElement.querySelector('.uppercase');
      expect(inputSection).toBeTruthy();
    });

    it('should show empty output message when success with no output', () => {
      createFixture(createToolCall({ status: 'success' }));
      component.expanded.set(true);
      fixture.detectChanges();

      const emptyOutput = fixture.nativeElement.querySelector('.italic');
      expect(emptyOutput).toBeTruthy();
    });

    it('should show error text for error status output', () => {
      createFixture(createToolCall({ status: 'error', output: 'Error: Something went wrong' }));
      component.expanded.set(true);
      fixture.detectChanges();

      const errorText = fixture.nativeElement.querySelector('.text-error');
      expect(errorText).toBeTruthy();
    });
  });

  describe('tool name display', () => {
    it('should display tool name', () => {
      createFixture(createToolCall({ name: 'get_weather' }));
      const toolName = fixture.nativeElement.querySelector('.font-medium');
      expect(toolName.textContent).toContain('get_weather');
    });
  });

  describe('click handler', () => {
    it('should call toggleExpanded when header is clicked', () => {
      createFixture(createToolCall());
      const spy = vi.spyOn(component, 'toggleExpanded');

      const header = fixture.nativeElement.querySelector('.cursor-pointer');
      header?.click();

      expect(spy).toHaveBeenCalled();
    });
  });
});
