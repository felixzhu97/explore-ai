import { Component, inject, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../i18n';
import { ZardButtonComponent } from '../ui/button';

@Component({
  selector: 'app-pipelines-toolbar',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './pipelines-toolbar.component.html',
  host: { class: 'flex shrink-0 flex-col gap-2 border-b border-black/8 bg-white px-3 py-2' },
})
export class PipelinesToolbarComponent {
  readonly i18n = inject(I18nService);

  /** Edit mode when true, use (run) mode otherwise. */
  readonly editing = input.required<boolean>();
  readonly templateName = input('');
  readonly isSaving = input(false);
  readonly task = model('');

  readonly back = output<void>();
  readonly addAgent = output<void>();
  readonly useGraph = output<void>();
  readonly editGraph = output<void>();
  readonly run = output<void>();
}
