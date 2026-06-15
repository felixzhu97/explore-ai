import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { McpToolsTabComponent } from './mcp-tools-tab.component';
import { McpService } from '@core/services/mcp.service';
import { NotificationService } from '@core/services/notification.service';
import { CardComponent } from '@shared/components/ui/card/card.component';

describe('McpToolsTabComponent', () => {
  let component: McpToolsTabComponent;
  let fixture: ComponentFixture<McpToolsTabComponent>;
  let mockMcpService: any;
  let mockNotification: any;

  beforeEach(async () => {
    mockMcpService = {
      listTools: vi.fn(),
      invokeTool: vi.fn(),
    };

    mockNotification = {
      showError: vi.fn(),
      showSuccess: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [McpToolsTabComponent, CardComponent],
      providers: [
        { provide: McpService, useValue: mockMcpService },
        { provide: NotificationService, useValue: mockNotification },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(McpToolsTabComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tools on init', () => {
    mockMcpService.listTools.mockReturnValue({
      subscribe: (handlers: any) => {
        handlers.next([]);
      },
    });

    component.ngOnInit();

    expect(mockMcpService.listTools).toHaveBeenCalled();
  });

  it('should open tool modal', () => {
    const tool = {
      name: 'test_tool',
      description: 'Test tool',
      inputSchema: {},
    };

    component.openToolModal(tool);

    expect(component.selectedTool()).toEqual(tool);
  });

  it('should close modal', () => {
    component.closeModal();

    expect(component.selectedTool()).toBeNull();
  });

  it('should get tool params from schema', () => {
    const tool = {
      name: 'test_tool',
      description: 'Test tool',
      inputSchema: {
        properties: {
          query: { type: 'string', description: 'Search query' },
          limit: { type: 'number' },
        },
        required: ['query'],
      },
    };

    const params = component.getToolParams(tool);

    expect(params.length).toBe(2);
    expect(params.find((p) => p.name === 'query')?.required).toBe(true);
    expect(params.find((p) => p.name === 'limit')?.required).toBe(false);
  });
});
