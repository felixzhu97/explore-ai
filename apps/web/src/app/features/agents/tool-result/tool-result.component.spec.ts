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

    it('should format primitive values', () => {
      createFixture(createToolCall());
      expect(component.formatJson('string')).toContain('string');
      expect(component.formatJson(123)).toContain('123');
      expect(component.formatJson(true)).toContain('true');
    });

    it('should handle null values', () => {
      createFixture(createToolCall());
      const result = component.formatJson({ key: null });
      expect(result).toContain('null');
    });

    it('should handle undefined values', () => {
      createFixture(createToolCall());
      // JSON.stringify removes undefined values, so {key: undefined} becomes '{}'
      const result = component.formatJson({ key: undefined });
      expect(result).toBe('{}');
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

    it('should handle deep nesting', () => {
      createFixture(createToolCall());
      const obj = { a: { b: { c: { d: { e: 'deep' } } } } };
      const result = component.formatJson(obj);
      expect(result).toContain('deep');
    });

    it('should handle special characters', () => {
      createFixture(createToolCall());
      const result = component.formatJson({ text: 'Hello\nWorld' });
      expect(result).toContain('Hello');
      expect(result).toContain('World');
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

    it('should format JSON array output', () => {
      createFixture(createToolCall());
      const output = '["item1", "item2", "item3"]';
      const result = component.formatOutput(output);
      expect(result).toContain('item1');
      expect(result).toContain('item2');
    });

    it('should handle image URLs in PNG format', () => {
      createFixture(createToolCall());
      const output = 'Check this image: https://example.com/photo.png';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('https://example.com/photo.png');
    });

    it('should handle image URLs in JPG format', () => {
      createFixture(createToolCall());
      const output = 'Image: https://example.com/photo.jpg';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('https://example.com/photo.jpg');
    });

    it('should handle image URLs in JPEG format', () => {
      createFixture(createToolCall());
      const output = 'Image: https://example.com/photo.jpeg';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle image URLs in GIF format', () => {
      createFixture(createToolCall());
      const output = 'GIF: https://example.com/animated.gif';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle image URLs with query parameters', () => {
      createFixture(createToolCall());
      const output = 'Image: https://example.com/photo.png?size=large&format=high';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('size=large');
    });

    it('should handle image URLs with special characters', () => {
      createFixture(createToolCall());
      const output = 'Image: https://example.com/photo%20name.png';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle invalid JSON gracefully', () => {
      createFixture(createToolCall());
      const output = '{not valid json';
      const result = component.formatOutput(output);
      expect(result).toBe(output);
    });

    it('should handle JSON with trailing whitespace', () => {
      createFixture(createToolCall());
      const output = '  {"key": "value"}  ';
      const result = component.formatOutput(output);
      expect(result).toContain('key');
    });

    it('should handle empty string', () => {
      createFixture(createToolCall());
      const result = component.formatOutput('');
      expect(result).toBe('');
    });

    it('should handle string that looks like JSON but is invalid', () => {
      createFixture(createToolCall());
      const output = '{ key: value }'; // missing quotes
      const result = component.formatOutput(output);
      expect(result).toBe(output);
    });

    it('should include text before image URL', () => {
      createFixture(createToolCall());
      const output = 'Here is the image: https://example.com/pic.png';
      const result = component.formatOutput(output);
      expect(result).toContain('Here is the image');
      expect(result).toContain('[Image:');
    });

    it('should include text after image URL', () => {
      createFixture(createToolCall());
      const output = 'https://example.com/pic.png - look at this!';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('look at this');
    });

    it('should handle SVG image URLs', () => {
      createFixture(createToolCall());
      const output = 'SVG: https://example.com/icon.svg';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle WebP image URLs', () => {
      createFixture(createToolCall());
      const output = 'WebP: https://example.com/photo.webp';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle BMP image URLs', () => {
      createFixture(createToolCall());
      const output = 'BMP: https://example.com/photo.bmp';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
    });

    it('should handle whitespace in output', () => {
      createFixture(createToolCall());
      const output = '  \n  plain text with whitespace  \n  ';
      const result = component.formatOutput(output);
      expect(result).toBe(output);
    });

    it('should handle mixed content with multiple URLs', () => {
      createFixture(createToolCall());
      const output = 'First: https://example.com/1.png Second: https://example.com/2.png';
      const result = component.formatOutput(output);
      expect(result).toContain('[Image:');
      expect(result).toContain('First:');
      expect(result).toContain('Second:');
    });
  });

  describe('tool call status rendering', () => {
    it('should show pending status indicator', () => {
      createFixture(createToolCall({ status: 'pending' }));
      const indicator = fixture.nativeElement.querySelector('.status-indicator--pending');
      expect(indicator).toBeTruthy();
    });

    it('should show running status indicator with spinner', () => {
      createFixture(createToolCall({ status: 'running' }));
      const indicator = fixture.nativeElement.querySelector('.status-indicator--running');
      expect(indicator).toBeTruthy();
      const spinner = fixture.nativeElement.querySelector('.spinner');
      expect(spinner).toBeTruthy();
    });

    it('should show success status indicator', () => {
      createFixture(createToolCall({ status: 'success' }));
      const indicator = fixture.nativeElement.querySelector('.status-indicator--success');
      expect(indicator).toBeTruthy();
      expect(indicator?.textContent).toContain('✓');
    });

    it('should show error status indicator', () => {
      createFixture(createToolCall({ status: 'error' }));
      const indicator = fixture.nativeElement.querySelector('.status-indicator--error');
      expect(indicator).toBeTruthy();
      expect(indicator?.textContent).toContain('✗');
    });
  });

  describe('tool header styling', () => {
    it('should apply error header class for error status', () => {
      createFixture(createToolCall({ status: 'error' }));
      const header = fixture.nativeElement.querySelector('.tool-header--error');
      expect(header).toBeTruthy();
    });

    it('should apply success header class for success status', () => {
      createFixture(createToolCall({ status: 'success' }));
      const header = fixture.nativeElement.querySelector('.tool-header--success');
      expect(header).toBeTruthy();
    });

    it('should apply pending header class for pending status', () => {
      createFixture(createToolCall({ status: 'pending' }));
      const header = fixture.nativeElement.querySelector('.tool-header--pending');
      expect(header).toBeTruthy();
    });

    it('should apply running header class for running status', () => {
      createFixture(createToolCall({ status: 'running' }));
      const header = fixture.nativeElement.querySelector('.tool-header--running');
      expect(header).toBeTruthy();
    });
  });

  describe('expand/collapse functionality', () => {
    it('should not show tool body when collapsed', () => {
      createFixture(createToolCall());
      component.expanded.set(false);
      fixture.detectChanges();

      const toolBody = fixture.nativeElement.querySelector('.tool-body');
      expect(toolBody).toBeFalsy();
    });

    it('should show tool body when expanded', () => {
      createFixture(createToolCall());
      component.expanded.set(true);
      fixture.detectChanges();

      const toolBody = fixture.nativeElement.querySelector('.tool-body');
      expect(toolBody).toBeTruthy();
    });

    it('should show input section when expanded', () => {
      createFixture(createToolCall());
      component.expanded.set(true);
      fixture.detectChanges();

      const inputSection = fixture.nativeElement.querySelector('.section-label');
      expect(inputSection?.textContent).toContain('Input');
    });

    it('should show code block for input when expanded', () => {
      createFixture(createToolCall({ input: { query: 'test query' } }));
      component.expanded.set(true);
      fixture.detectChanges();

      const codeBlock = fixture.nativeElement.querySelector('.code-block');
      expect(codeBlock).toBeTruthy();
      expect(codeBlock?.textContent).toContain('query');
    });

    it('should show output section when expanded and output exists', () => {
      createFixture(createToolCall({ output: 'Tool output result' }));
      component.expanded.set(true);
      fixture.detectChanges();

      const outputSection = fixture.nativeElement.querySelectorAll('.section-label');
      const outputLabel = (Array.from(outputSection) as HTMLElement[]).find((el) =>
        el.textContent?.includes('Output')
      );
      expect(outputLabel).toBeTruthy();
    });

    it('should show empty output message when success with no output', () => {
      createFixture(createToolCall({ status: 'success' }));
      component.expanded.set(true);
      fixture.detectChanges();

      const emptyOutput = fixture.nativeElement.querySelector('.empty-output');
      expect(emptyOutput).toBeTruthy();
      expect(emptyOutput?.textContent).toContain('No output');
    });

    it('should show error text for error status output', () => {
      createFixture(createToolCall({ status: 'error', output: 'Error: Something went wrong' }));
      component.expanded.set(true);
      fixture.detectChanges();

      const errorText = fixture.nativeElement.querySelector('.error-text');
      expect(errorText).toBeTruthy();
      expect(errorText?.textContent).toContain('Error: Something went wrong');
    });
  });

  describe('expand icon', () => {
    it('should not have expanded class when collapsed', () => {
      createFixture(createToolCall());
      component.expanded.set(false);
      fixture.detectChanges();

      const expandIcon = fixture.nativeElement.querySelector('.expand-icon');
      expect(expandIcon?.classList.contains('expanded')).toBe(false);
    });

    it('should have expanded class when expanded', () => {
      createFixture(createToolCall());
      component.expanded.set(true);
      fixture.detectChanges();

      const expandIcon = fixture.nativeElement.querySelector('.expand-icon');
      expect(expandIcon?.classList.contains('expanded')).toBe(true);
    });
  });

  describe('tool name display', () => {
    it('should display tool name', () => {
      createFixture(createToolCall({ name: 'get_weather' }));
      const toolName = fixture.nativeElement.querySelector('.tool-name');
      expect(toolName?.textContent).toContain('get_weather');
    });

    it('should handle long tool names', () => {
      createFixture(
        createToolCall({ name: 'a_very_long_tool_name_that_should_be_handled_gracefully' })
      );
      const toolName = fixture.nativeElement.querySelector('.tool-name');
      expect(toolName).toBeTruthy();
    });
  });

  describe('click handler', () => {
    it('should call toggleExpanded when header is clicked', () => {
      createFixture(createToolCall());
      const spy = vi.spyOn(component, 'toggleExpanded');

      const header = fixture.nativeElement.querySelector('.tool-header');
      header?.click();

      expect(spy).toHaveBeenCalled();
    });
  });
});
