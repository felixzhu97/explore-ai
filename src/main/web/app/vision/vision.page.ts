import { Component, signal, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@core/i18n';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';
import type { VisionResult } from './vision.model';

type TaskType = 'caption' | 'detect' | 'ocr';

interface TabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

@Component({
  selector: 'app-vision-page',
  imports: [CommonModule, FormsModule, SegmentedControlComponent],
  standalone: true,
  templateUrl: './vision.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VisionPageComponent {
  private readonly api = inject(ApiService);
  protected readonly i18n = inject(I18nService);

  readonly activeTask = signal<TaskType>('caption');
  readonly tabStates = signal<Record<TaskType, TabState>>({
    caption: { image: null, file: null, result: null, error: null },
    detect: { image: null, file: null, result: null, error: null },
    ocr: { image: null, file: null, result: null, error: null },
  });

  readonly isLoading = signal(false);
  readonly zoomedImage = signal<string | null>(null);

  readonly taskOptions = computed(() => {
    const t = this.i18n.t().imageUploader;
    return [
      { value: 'caption' as TaskType, label: t.caption },
      { value: 'detect' as TaskType, label: t.detect },
      { value: 'ocr' as TaskType, label: t.ocr },
    ];
  });

  readonly currentState = computed<TabState>(() => this.tabStates()[this.activeTask()]);

  setActiveTask(task: TaskType) {
    this.activeTask.set(task);
  }

  onImageAreaClick() {
    if (!this.currentState().image) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = e => this.onFileChange(e);
      input.click();
    }
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.processFile(file);
    }
    input.value = '';
  }

  processFile(file: File) {
    if (!file.type.startsWith('image/')) {
      this.updateState({ error: this.i18n.t().imageUploader.selectImageError });
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      const imageData = e.target?.result as string;
      this.updateState({ image: imageData, file, error: null, result: null });
    };
    reader.readAsDataURL(file);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const droppedFile = event.dataTransfer?.files[0];
    if (droppedFile) {
      this.processFile(droppedFile);
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  clearImage(event: Event) {
    event.stopPropagation();
    this.updateState({ image: null, file: null, error: null, result: null });
  }

  zoomImage(image: string) {
    this.zoomedImage.set(image);
  }

  closeZoom() {
    this.zoomedImage.set(null);
  }

  submit() {
    const currentFile = this.currentState().file;
    if (!currentFile) return;

    this.isLoading.set(true);
    this.updateState({ error: null, result: null });

    const task = this.activeTask();
    let request: Observable<unknown>;
    switch (task) {
      case 'caption':
        request = this.api.captionImage(currentFile);
        break;
      case 'detect':
        request = this.api.detectObjects(currentFile);
        break;
      case 'ocr':
        request = this.api.ocrImage(currentFile);
        break;
    }

    request.subscribe({
      next: (data: unknown) => {
        this.updateState({ result: data as VisionResult });
      },
      error: (err: unknown) => {
        const msg = err instanceof Error
          ? err.message
          : this.i18n.t().imageUploader.processingFailed;
        this.updateState({ error: msg });
      },
      complete: () => {
        this.isLoading.set(false);
      },
    });
  }

  private updateState(partial: Partial<TabState>) {
    this.tabStates.update(states => ({
      ...states,
      [this.activeTask()]: { ...states[this.activeTask()], ...partial },
    }));
  }
}
