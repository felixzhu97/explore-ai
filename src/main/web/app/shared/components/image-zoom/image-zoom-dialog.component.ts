import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Z_MODAL_DATA } from '@/shared/components/dialog';

export interface ImageZoomData {
  src: string;
  alt?: string;
}

@Component({
  selector: 'app-image-zoom-dialog',
  template: `
    <div class="flex flex-col items-center p-2">
      <img
        class="max-h-[85vh] max-w-full rounded-lg object-contain shadow-2xl"
        [src]="data.src"
        [alt]="data.alt || 'Zoomed image'"
      />
      @if (data.alt) {
        <p class="mt-4 text-center text-sm text-muted-foreground">{{ data.alt }}</p>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageZoomDialogComponent {
  readonly data = inject<ImageZoomData>(Z_MODAL_DATA);
}
