import {
  Component,
  input,
  viewChild,
  effect,
  type ElementRef,
} from '@angular/core';
import type { DetectionResponse } from './vision.service';

@Component({
  selector: 'app-detection-overlay',
  template: `
    <div class="relative inline-block max-h-96 max-w-full">
      <img
        previewImage
        class="max-h-96 max-w-full rounded-xl object-contain"
        alt="Preview"
        [src]="imageSrc()"
        (load)="drawOverlay()"
      />
      <canvas
        overlayCanvas
        class="pointer-events-none absolute top-0 left-0 size-full"
      ></canvas>
    </div>
  `,
})
export class DetectionOverlayComponent {
  readonly imageSrc = input.required<string>();
  readonly detections = input<DetectionResponse[]>([]);

  protected readonly previewImage = viewChild.required<ElementRef<HTMLImageElement>>('previewImage');
  protected readonly overlayCanvas = viewChild.required<ElementRef<HTMLCanvasElement>>('overlayCanvas');

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

    const context = canvas.getContext('2d');
    if (context === null) {
      return;
    }

    context.clearRect(0, 0, canvas.width, canvas.height);
    context.lineWidth = 2;
    context.font = '12px system-ui, sans-serif';

    for (const detection of this.detections()) {
      const [x = 0, y = 0, width = 0, height = 0] = detection.bbox;
      const scaledX = x * scaleX;
      const scaledY = y * scaleY;
      const scaledWidth = width * scaleX;
      const scaledHeight = height * scaleY;

      context.strokeStyle = '#3b82f6';
      context.fillStyle = 'rgba(59, 130, 246, 0.15)';
      context.fillRect(scaledX, scaledY, scaledWidth, scaledHeight);
      context.strokeRect(scaledX, scaledY, scaledWidth, scaledHeight);

      const label = `${detection.className} ${(detection.confidence * 100).toFixed(0)}%`;
      const textWidth = context.measureText(label).width;
      context.fillStyle = '#3b82f6';
      context.fillRect(scaledX, Math.max(0, scaledY - 18), textWidth + 8, 18);
      context.fillStyle = '#ffffff';
      context.fillText(label, scaledX + 4, Math.max(12, scaledY - 5));
    }
  }
}
