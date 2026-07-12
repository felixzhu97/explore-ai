import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import type { VisionResult } from '../../../vision/vision.model';
import type { VisionTaskType } from '../../../vision/vision.service';

@Component({
  selector: 'app-media-result-panel',
  imports: [
    ZardAlertComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
    ZardCardComponent,
  ],
  template: `
    <z-card
      class="flex h-full flex-col gap-4 py-4 shadow-card [&_[data-slot=card-title]]:text-sm [&_[data-slot=card-title]]:font-medium [&_[data-slot=card-title]]:tracking-wide [&_[data-slot=card-title]]:text-muted-foreground [&_[data-slot=card-title]]:uppercase"
      [zTitle]="title()"
    >
      @if (result()) {
        @if (task() === 'caption') {
          <p class="text-lg text-foreground italic">"{{ result()!.caption }}"</p>
        }
        @if (task() === 'detect') {
          @if (result()!.detections?.length) {
            <div class="flex flex-col gap-2">
              @for (det of result()!.detections; track det.className) {
                <div
                  class="
                    flex items-center justify-between rounded-lg border
                    border-input bg-muted/40 p-3
                  "
                >
                  <span class="font-medium text-foreground">{{ det.className }}</span>
                  <z-badge zType="secondary">
                    {{ (det.confidence * 100).toFixed(0) }}%
                  </z-badge>
                </div>
              }
            </div>
          } @else {
            <div class="flex flex-1 items-center justify-center text-sm text-muted-foreground">
              {{ noDetectionsLabel() }}
            </div>
          }
        }
        @if (task() === 'ocr') {
          <pre
            class="
              m-0 flex-1 overflow-x-auto rounded-lg bg-muted/40 p-3 font-mono
              text-sm break-all whitespace-pre-wrap text-foreground
            "
          >{{ result()!.fullText }}</pre>
        }
        @if (processingTimeLabel()) {
          <p class="mt-3 text-xs text-muted-foreground">{{ processingTimeLabel() }}</p>
        }
      } @else {
        <div class="flex flex-1 items-center justify-center text-sm text-muted-foreground">
          {{ emptyLabel() }}
        </div>
      }

      @if (error()) {
        <z-alert zType="destructive" class="mt-4" [zDescription]="error()!" />
      }

      <div card-footer class="mt-auto w-full border-t border-black/8 pt-4">
        <button
          type="button"
          z-button
          zFull
          [zDisabled]="!canAnalyze() || loading()"
          (click)="analyzeRequested.emit()"
        >
          @if (loading()) {
            {{ analyzingLabel() }}
          } @else {
            {{ analyzeLabel() }}
          }
        </button>
      </div>
    </z-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MediaResultPanelComponent {
  readonly title = input.required<string>();
  readonly task = input.required<VisionTaskType>();
  readonly result = input<VisionResult | null>(null);
  readonly error = input<string | null>(null);
  readonly loading = input(false);
  readonly canAnalyze = input(false);
  readonly emptyLabel = input.required<string>();
  readonly noDetectionsLabel = input.required<string>();
  readonly analyzeLabel = input.required<string>();
  readonly analyzingLabel = input.required<string>();
  readonly processingTimeLabel = input<string | null>(null);

  readonly analyzeRequested = output<void>();
}
