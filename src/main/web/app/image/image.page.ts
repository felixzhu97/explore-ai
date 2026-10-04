import { Component, inject } from '@angular/core';
import { I18nService } from '../i18n';
import { ImageGenFormComponent } from './image-gen-form.component';
import { MediaPreviewPanelComponent } from './media-preview-panel.component';
import { ZardAlertComponent } from '../ui/alert';
import { ZardButtonComponent } from '../ui/button';
import { ImageService } from './image.service';

@Component({
  selector: 'app-image-page',
  imports: [
    ImageGenFormComponent,
    MediaPreviewPanelComponent,
    ZardAlertComponent,
    ZardButtonComponent,
  ],
  templateUrl: './image.page.html',
  host: { class: 'block min-w-0 w-full max-w-full' },
})
export class ImagePageComponent {
  protected readonly imageGen = inject(ImageService);
  protected readonly i18n = inject(I18nService);
}
