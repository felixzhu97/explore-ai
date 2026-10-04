import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MediaResultPanelComponent } from './media-result-panel.component';

describe('MediaResultPanelComponent', () => {
  let fixture: ComponentFixture<MediaResultPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MediaResultPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MediaResultPanelComponent);
    fixture.componentRef.setInput('title', 'Result');
    fixture.componentRef.setInput('task', 'caption');
    fixture.componentRef.setInput('emptyLabel', 'No result yet');
    fixture.componentRef.setInput('noDetectionsLabel', 'No detections');
    fixture.componentRef.setInput('analyzeLabel', 'Analyze');
    fixture.componentRef.setInput('analyzingLabel', 'Analyzing...');
  });

  it('should show empty state when no result', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No result yet');
  });

  it('should show caption result', () => {
    fixture.componentRef.setInput('result', { caption: 'A beach scene', processingTimeMs: 50 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('A beach scene');
  });

  it('should show error alert', () => {
    fixture.componentRef.setInput('error', 'Something went wrong');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Something went wrong');
  });

  it('should disable analyze button when cannot analyze', () => {
    fixture.componentRef.setInput('canAnalyze', false);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button');
    expect(button.disabled).toBe(true);
  });
});
