import {
  Component,
  input,
  output,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideImage } from '@ng-icons/lucide';
import { ZardButtonComponent } from '../ui/button';
import { ZardCardComponent } from '../ui/card';
import { ZardSkeletonComponent } from '../ui/skeleton';
import { DetectionOverlayComponent } from './detection-overlay.component';
import type { DetectionResponse } from './vision.service';
import { hasText } from '../shared/presence';

@Component({
  selector: 'app-media-upload-panel',
  imports: [
    NgIcon,
    ZardButtonComponent,
    ZardCardComponent,
    ZardSkeletonComponent,
    DetectionOverlayComponent,
  ],
  template: `
    <z-card
      class="gap-4 py-4 shadow-card [&_[data-slot=card-title]]:text-sm [&_[data-slot=card-title]]:font-medium [&_[data-slot=card-title]]:tracking-wide [&_[data-slot=card-title]]:text-muted-foreground [&_[data-slot=card-title]]:uppercase"
      [zTitle]="title()"
    >
      <div
        class="
          group relative flex min-h-64 w-full cursor-pointer flex-col
          items-stretch justify-center overflow-hidden rounded-xl border-2
          border-dashed border-input bg-muted/40
        "
        tabindex="0"
        role="button"
        [attr.aria-label]="dropText()"
        (click)="onAreaClick()"
        (keydown.enter)="onAreaClick()"
        (drop)="onDrop($event)"
        (dragover)="onDragOver($event)"
      >
        @let preview = imagePreview();
        @if (preview !== null) {
          @let overlayDetections = detections() ?? [];
          @if (showDetectionOverlay() && overlayDetections.length > 0) {
            <app-detection-overlay
              [imageSrc]="preview"
              [detections]="overlayDetections"
            />
          } @else {
            <img
              class="max-h-96 max-w-full cursor-zoom-in rounded-xl object-contain"
              alt="Preview"
              tabindex="0"
              role="button"
              [src]="preview"
              [attr.aria-label]="clickToEnlargeLabel()"
              (click)="onZoomClick($event)"
              (keydown.enter)="onZoomClick($event)"
            />
          }
          <div
            class="
              pointer-events-none absolute bottom-2 left-1/2 z-10
              -translate-x-1/2 rounded bg-black/60 px-3 py-1 text-xs
              text-white opacity-0 backdrop-blur-sm transition-opacity
              group-hover:opacity-100
            "
          >
            {{ clickToEnlargeLabel() }}
          </div>
          <button
            type="button"
            z-button
            zType="outline"
            zSize="icon"
            zShape="circle"
            class="absolute top-2 right-2 z-10 opacity-0 transition-opacity group-hover:opacity-100"
            [attr.aria-label]="clearLabel()"
            (click)="onClearClick($event)"
          >
            ×
          </button>
          @if (isLoading()) {
            <div
              class="
                absolute inset-0 z-20 flex flex-col items-center
                justify-center gap-3 rounded-xl bg-background/90 backdrop-blur-sm
              "
            >
              <z-skeleton class="size-8 rounded-full" />
            </div>
          }
        } @else {
          <div class="flex w-full flex-col items-center justify-center gap-2 px-6 py-8 text-center">
            <div
              class="
                flex size-10 items-center justify-center rounded-lg bg-muted
                text-muted-foreground
              "
            >
              <ng-icon name="lucideImage" class="size-5!" />
            </div>
            <p class="text-base leading-normal font-medium text-foreground">
              {{ dropText() }}
            </p>
            <p class="text-sm leading-relaxed text-muted-foreground">
              {{ dropHint() }}
            </p>
          </div>
        }
      </div>
      <input
        #fileInput
        type="file"
        accept="image/*"
        class="hidden"
        (change)="onFileInputChange($event)"
      />
    </z-card>
  `,
  providers: [provideIcons({ lucideImage })],
})
export class MediaUploadPanelComponent {
  readonly title = input.required<string>();
  readonly imagePreview = input<string | null>(null);
  readonly isLoading = input(false);
  readonly dropText = input.required<string>();
  readonly dropHint = input.required<string>();
  readonly clearLabel = input.required<string>();
  readonly clickToEnlargeLabel = input.required<string>();
  readonly showDetectionOverlay = input(false);
  readonly detections = input<DetectionResponse[] | undefined>(undefined);
  readonly fileSelected = output<File>();
  readonly cleared = output<void>();
  readonly zoomRequested = output<string>();

  /** Opens the file picker when no image is shown. */
  onAreaClick(): void {
    if (!hasText(this.imagePreview())) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = event => this.onFileInputChange(event);
      input.click();
    }
  }

  /** Takes the dropped file. */
  onDrop(event: DragEvent): void {
    event.preventDefault();
    const droppedFile = event.dataTransfer?.files[0];
    if (droppedFile !== undefined) {
      this.fileSelected.emit(droppedFile);
    }
  }

  /** Allows dropping files on the area. */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  /** Asks to zoom the image. */
  onZoomClick(event: Event): void {
    event.stopPropagation();
    const image = this.imagePreview();
    if (hasText(image)) {
      this.zoomRequested.emit(image);
    }
  }

  /** Asks to clear the image. */
  onClearClick(event: Event): void {
    event.stopPropagation();
    this.cleared.emit();
  }

  /** Takes the file picked in the file input. */
  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file !== undefined) {
      this.fileSelected.emit(file);
    }
    input.value = '';
  }
}
