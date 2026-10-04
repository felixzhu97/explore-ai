import { describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { PipelinesGalleryComponent } from './pipelines-gallery.component';
import type { PipelineTemplate, PipelineTemplateDefinition } from './pipelines.service';

const builtin: PipelineTemplateDefinition = {
  id: 'research',
  name: 'Research',
  description: 'Research flow',
  agentTypes: ['researcher', 'writer'],
  shortTopic: 'topic',
  briefPrompt: 'brief',
};

const saved: PipelineTemplate = {
  ...builtin,
  id: 'saved-1',
  sourceTemplateId: 'research',
  enabled: false,
};

describe('PipelinesGalleryComponent', () => {
  function setup(savedTemplates: PipelineTemplate[]) {
    const fixture = TestBed.createComponent(PipelinesGalleryComponent);
    fixture.componentRef.setInput('builtinTemplates', [builtin]);
    fixture.componentRef.setInput('savedTemplates', savedTemplates);
    fixture.detectChanges();
    return fixture;
  }

  it('should render the agent order of each template', () => {
    const fixture = setup([]);
    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('researcher → writer');
  });

  it('should disable adding a builtin template that is already saved', () => {
    const fixture = setup([saved]);
    const host = fixture.nativeElement as HTMLElement;
    const buttons = [...host.querySelectorAll('button')];
    const disabled = buttons.filter(button => button.disabled);
    expect(disabled).toHaveLength(1);
  });

  it('should emit the template when use is clicked', () => {
    const fixture = setup([]);
    const emitted: PipelineTemplateDefinition[] = [];
    fixture.componentInstance.useTemplate.subscribe(t => emitted.push(t));
    const host = fixture.nativeElement as HTMLElement;
    host.querySelectorAll('button')[1]?.click();
    expect(emitted).toEqual([builtin]);
  });
});
