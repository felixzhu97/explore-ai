import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { I18nService } from '@core/i18n';
import {
  ImageGenFormComponent,
  MediaPreviewPanelComponent,
} from '@shared/components/media-shell';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardButtonComponent } from '@/shared/components/button';
import { ImageGenerationService } from './image-generation.service';

@Component({
  selector: 'app-image-gen-tab',
  imports: [
    ImageGenFormComponent,
    MediaPreviewPanelComponent,
    ZardAlertComponent,
    ZardButtonComponent,
  ],
  templateUrl: './image.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block min-w-0 w-full max-w-full' },
})
export class ImageComponent {
  protected readonly imageGen = inject(ImageGenerationService);
  protected readonly i18n = inject(I18nService);
}
