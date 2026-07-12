import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideImage } from '@ng-icons/lucide';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';

@Component({
  selector: 'app-media-preview-panel',
  imports: [NgIcon, ZardCardComponent, ZardSkeletonComponent],
  template: `
    <z-card class="flex min-h-[300px] flex-col gap-3 py-4 shadow-card">
      <div
        class="
          relative flex min-h-64 w-full flex-1 items-stretch justify-center
          overflow-hidden rounded-xl border-2 border-dashed border-input
          bg-muted/40
        "
      >
        @if (loading()) {
          <div
            class="
              absolute inset-0 z-10 flex flex-col items-center justify-center
              gap-3 rounded-xl bg-background/90 backdrop-blur-sm
            "
          >
            <z-skeleton class="size-11 rounded-full" />
            <span class="text-sm font-medium text-muted-foreground">
              {{ loadingLabel() }}
            </span>
          </div>
        }
        @if (imageSrc()) {
          <img
            class="
              max-h-96 max-w-full animate-fade-in cursor-zoom-in rounded-xl
              object-contain transition-transform hover:scale-[1.02]
            "
            [src]="imageSrc()"
            [alt]="imageAlt()"
            (click)="zoomRequested.emit(imageSrc()!)"
            (keydown.enter)="zoomRequested.emit(imageSrc()!)"
            tabindex="0"
            role="button"
            [attr.aria-label]="zoomLabel()"
          />
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
              {{ emptyState() }}
            </p>
          </div>
        }
      </div>
    </z-card>
  `,
  providers: [provideIcons({ lucideImage })],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MediaPreviewPanelComponent {
  readonly imageSrc = input<string | null>(null);
  readonly loading = input(false);
  readonly loadingLabel = input('');
  readonly emptyState = input.required<string>();
  readonly imageAlt = input('Generated image');
  readonly zoomLabel = input('Zoom image');

  readonly zoomRequested = output<string>();
}
