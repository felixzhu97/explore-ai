import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { I18nService } from '../../../../core/i18n';
import { ImageGenFormComponent } from '../components/image-gen-form.component';
import { MediaPreviewPanelComponent } from '../components/media-preview-panel.component';
import { ZardAlertComponent } from '../../../../shared/components/alert';
import { ZardButtonComponent } from '../../../../shared/components/button';
import { ImageService } from '../services/image.service';

@Component({
  selector: 'app-image-page',
  imports: [
    ImageGenFormComponent,
    MediaPreviewPanelComponent,
    ZardAlertComponent,
    ZardButtonComponent,
  ],
  templateUrl: './image.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block min-w-0 w-full max-w-full' },
})
export class ImagePageComponent {
  protected readonly imageGen = inject(ImageService);
  protected readonly i18n = inject(I18nService);
}
