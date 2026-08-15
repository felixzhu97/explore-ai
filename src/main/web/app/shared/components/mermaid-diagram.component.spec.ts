import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ZardDialogService } from './dialog';
import { MermaidDiagramComponent } from './mermaid-diagram.component';
import { MermaidDiagramZoomDialogComponent } from './mermaid-diagram-zoom-dialog.component';

const renderMock = vi.fn();

vi.mock('mermaid', () => ({
  default: {
    initialize: vi.fn(),
    render: (...args: unknown[]) => renderMock(...args),
  },
}));

describe('MermaidDiagramComponent', () => {
  let fixture: ComponentFixture<MermaidDiagramComponent>;
  let dialogCreate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    renderMock.mockReset();
    dialogCreate = vi.fn();
    await TestBed.configureTestingModule({
      imports: [MermaidDiagramComponent],
      providers: [{ provide: ZardDialogService, useValue: { create: dialogCreate } }],
    }).compileComponents();

    fixture = TestBed.createComponent(MermaidDiagramComponent);
  });

  it('should render sanitized svg when mermaid succeeds', async () => {
    renderMock.mockResolvedValue({
      svg: '<svg xmlns="http://www.w3.org/2000/svg"><text>ok</text></svg>',
    });

    fixture.componentRef.setInput('source', 'sequenceDiagram\n  A->>B: hi');
    fixture.componentRef.setInput('diagramId', 'mermaid-test');
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(fixture.nativeElement.querySelector('svg')).toBeTruthy();
    });
    expect(fixture.nativeElement.textContent).not.toContain('could not be drawn');
  });

  it('should show source and calm message when mermaid fails', async () => {
    renderMock.mockRejectedValue(new Error('parse error'));

    fixture.componentRef.setInput('source', 'not a valid diagram');
    fixture.componentRef.setInput('diagramId', 'mermaid-bad');
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(fixture.componentInstance.error()).toBe(true);
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('not a valid diagram');
    expect(text.toLowerCase()).toMatch(/could not|无法|描け|impossible|no se pudo/i);
  });

  it('should open fullscreen dialog when diagram is activated', async () => {
    const svg = '<svg xmlns="http://www.w3.org/2000/svg"><text>full</text></svg>';
    renderMock.mockResolvedValue({ svg });

    fixture.componentRef.setInput('source', 'flowchart TD\n  A-->B');
    fixture.componentRef.setInput('diagramId', 'mermaid-zoom');
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(fixture.componentInstance.svgMarkup()).toContain('<svg');
    });

    fixture.componentInstance.openFullscreen();

    expect(dialogCreate).toHaveBeenCalledWith(
      expect.objectContaining({
        zContent: MermaidDiagramZoomDialogComponent,
        zData: expect.objectContaining({
          svgMarkup: expect.stringContaining('<svg'),
        }),
        zHideFooter: true,
        zMaskClosable: true,
      }),
    );
  });
});
