import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../i18n';
import { ZardButtonComponent } from '../ui/button';
import type { PipelineTemplate, PipelineTemplateDefinition } from './pipelines.service';

@Component({
  selector: 'app-pipelines-gallery',
  imports: [ZardButtonComponent],
  templateUrl: './pipelines-gallery.component.html',
  host: { class: 'absolute inset-0 z-10 overflow-y-auto px-4 py-6' },
})
export class PipelinesGalleryComponent {
  readonly i18n = inject(I18nService);

  readonly builtinTemplates = input.required<PipelineTemplateDefinition[]>();
  readonly savedTemplates = input.required<PipelineTemplate[]>();
  readonly addingTemplateId = input<string | null>(null);

  readonly addWorkflow = output<void>();
  readonly useTemplate = output<PipelineTemplateDefinition>();
  readonly editTemplate = output<PipelineTemplateDefinition>();
  readonly addTemplate = output<PipelineTemplateDefinition>();
  readonly useSavedTemplate = output<PipelineTemplate>();
  readonly editSavedTemplate = output<PipelineTemplate>();
  readonly deleteSavedTemplate = output<PipelineTemplate>();

  isSaved(template: PipelineTemplateDefinition): boolean {
    return this.savedTemplates().some(item => item.sourceTemplateId === template.id);
  }

  templateOrder(agentTypes: readonly string[]): string {
    return agentTypes.join(' → ');
  }
}
