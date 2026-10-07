import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../i18n';
import { ZardButtonComponent } from '../ui/button';
import type { PipelineTemplate, BuiltinPipelineTemplateResponse } from './pipelines.service';

@Component({
  selector: 'app-pipelines-gallery',
  imports: [ZardButtonComponent],
  templateUrl: './pipelines-gallery.component.html',
  host: { class: 'absolute inset-0 z-10 overflow-y-auto px-4 py-6' },
})
export class PipelinesGalleryComponent {
  readonly i18n = inject(I18nService);

  readonly builtinTemplates = input.required<BuiltinPipelineTemplateResponse[]>();
  readonly savedTemplates = input.required<PipelineTemplate[]>();
  readonly addingTemplateId = input<string | null>(null);
  readonly addWorkflow = output<void>();
  readonly useTemplate = output<BuiltinPipelineTemplateResponse>();
  readonly editTemplate = output<BuiltinPipelineTemplateResponse>();
  readonly addTemplate = output<BuiltinPipelineTemplateResponse>();
  readonly useSavedTemplate = output<PipelineTemplate>();
  readonly editSavedTemplate = output<PipelineTemplate>();
  readonly deleteSavedTemplate = output<PipelineTemplate>();

  /** Joins the agent types into an arrow chain. */
  getTemplateOrder(agentTypes: readonly string[]): string {
    return agentTypes.join(' → ');
  }

  /** Tells whether the template is already in the library. */
  isSaved(template: BuiltinPipelineTemplateResponse): boolean {
    return this.savedTemplates().some(item => item.sourceTemplateId === template.id);
  }
}
