import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideImage } from '@ng-icons/lucide';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { DetectionOverlayComponent } from '../../../vision/detection-overlay.component';
import type { Detection } from '../../../vision/vision.model';

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
        (click)="onAreaClick()"
        (keydown.enter)="onAreaClick()"
        (drop)="onDrop($event)"
        (dragover)="onDragOver($event)"
        tabindex="0"
        role="button"
        [attr.aria-label]="dropText()"
      >
        @if (imagePreview()) {
          @if (showDetectionOverlay() && detections()?.length) {
            <app-detection-overlay
              [imageSrc]="imagePreview()!"
              [detections]="detections()!"
            />
          } @else {
            <img
              class="max-h-96 max-w-full cursor-zoom-in rounded-xl object-contain"
              [src]="imagePreview()"
              alt="Preview"
              (click)="onZoomClick($event)"
              (keydown.enter)="onZoomClick($event)"
              tabindex="0"
              role="button"
              [attr.aria-label]="clickToEnlargeLabel()"
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
            (click)="onClearClick($event)"
            [attr.aria-label]="clearLabel()"
          >
            ×
          </button>
          @if (loading()) {
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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MediaUploadPanelComponent {
  readonly title = input.required<string>();
  readonly imagePreview = input<string | null>(null);
  readonly loading = input(false);
  readonly dropText = input.required<string>();
  readonly dropHint = input.required<string>();
  readonly clearLabel = input.required<string>();
  readonly clickToEnlargeLabel = input.required<string>();
  readonly showDetectionOverlay = input(false);
  readonly detections = input<Detection[] | undefined>(undefined);

  readonly fileSelected = output<File>();
  readonly cleared = output<void>();
  readonly zoomRequested = output<string>();

  onAreaClick(): void {
    if (!this.imagePreview()) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = event => this.onFileInputChange(event);
      input.click();
    }
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.fileSelected.emit(file);
    }
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const droppedFile = event.dataTransfer?.files[0];
    if (droppedFile) {
      this.fileSelected.emit(droppedFile);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onClearClick(event: Event): void {
    event.stopPropagation();
    this.cleared.emit();
  }

  onZoomClick(event: Event): void {
    event.stopPropagation();
    const image = this.imagePreview();
    if (image) {
      this.zoomRequested.emit(image);
    }
  }
}
