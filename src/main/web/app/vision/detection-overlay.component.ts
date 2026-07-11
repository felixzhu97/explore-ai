import {
  Component,
  input,
  viewChild,
  effect,
  ChangeDetectionStrategy,
  ElementRef,
} from '@angular/core';
import type { Detection } from './vision.model';

@Component({
  selector: 'app-detection-overlay',
  standalone: true,
  template: `
    <div class="relative inline-block max-h-96 max-w-full">
      <img
        #previewImage
        class="max-h-96 max-w-full rounded-xl object-contain"
        [src]="imageSrc()"
        alt="Preview"
        (load)="drawOverlay()"
      />
      <canvas
        #overlayCanvas
        class="pointer-events-none absolute top-0 left-0 size-full"
      ></canvas>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetectionOverlayComponent {
  readonly imageSrc = input.required<string>();
  readonly detections = input<Detection[]>([]);

  private readonly previewImage = viewChild.required<ElementRef<HTMLImageElement>>('previewImage');
  private readonly overlayCanvas = viewChild.required<ElementRef<HTMLCanvasElement>>('overlayCanvas');

  constructor() {
    effect(() => {
      this.detections();
      this.drawOverlay();
    });
  }

  drawOverlay(): void {
    const image = this.previewImage().nativeElement;
    const canvas = this.overlayCanvas().nativeElement;
    if (!image.complete || image.clientWidth === 0) {
      return;
    }

    const scaleX = image.clientWidth / image.naturalWidth;
    const scaleY = image.clientHeight / image.naturalHeight;

    canvas.width = image.clientWidth;
    canvas.height = image.clientHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.lineWidth = 2;
    ctx.font = '12px system-ui, sans-serif';

    for (const detection of this.detections()) {
      const [x, y, width, height] = detection.bbox;
      const scaledX = x * scaleX;
      const scaledY = y * scaleY;
      const scaledWidth = width * scaleX;
      const scaledHeight = height * scaleY;

      ctx.strokeStyle = '#3b82f6';
      ctx.fillStyle = 'rgba(59, 130, 246, 0.15)';
      ctx.fillRect(scaledX, scaledY, scaledWidth, scaledHeight);
      ctx.strokeRect(scaledX, scaledY, scaledWidth, scaledHeight);

      const label = `${detection.className} ${(detection.confidence * 100).toFixed(0)}%`;
      const textWidth = ctx.measureText(label).width;
      ctx.fillStyle = '#3b82f6';
      ctx.fillRect(scaledX, Math.max(0, scaledY - 18), textWidth + 8, 18);
      ctx.fillStyle = '#ffffff';
      ctx.fillText(label, scaledX + 4, Math.max(12, scaledY - 5));
    }
  }
}
