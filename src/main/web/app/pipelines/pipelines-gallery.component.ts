import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../i18n';
import { ZardButtonComponent } from '../ui/button';
import type { PipelineTemplate, PipelineTemplateDefinitionResponse } from './pipelines.service';

@Component({
  selector: 'app-pipelines-gallery',
  imports: [ZardButtonComponent],
  templateUrl: './pipelines-gallery.component.html',
  host: { class: 'absolute inset-0 z-10 overflow-y-auto px-4 py-6' },
})
export class PipelinesGalleryComponent {
  readonly i18n = inject(I18nService);

  readonly builtinTemplates = input.required<PipelineTemplateDefinitionResponse[]>();
  readonly savedTemplates = input.required<PipelineTemplate[]>();
  readonly addingTemplateId = input<string | null>(null);
  readonly addWorkflow = output<void>();
  readonly useTemplate = output<PipelineTemplateDefinitionResponse>();
  readonly editTemplate = output<PipelineTemplateDefinitionResponse>();
  readonly addTemplate = output<PipelineTemplateDefinitionResponse>();
  readonly useSavedTemplate = output<PipelineTemplate>();
  readonly editSavedTemplate = output<PipelineTemplate>();
  readonly deleteSavedTemplate = output<PipelineTemplate>();

  getTemplateOrder(agentTypes: readonly string[]): string {
    return agentTypes.join(' → ');
  }

  isSaved(template: PipelineTemplateDefinitionResponse): boolean {
    return this.savedTemplates().some(item => item.sourceTemplateId === template.id);
  }
}
