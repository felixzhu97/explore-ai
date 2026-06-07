import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { I18nService } from '../../i18n';

@Component({
  selector: 'app-vision-panel',
  standalone: true,
  template: `
    <div class="panel">
      <div class="panel-header">
        <h1 class="panel-title">{{ i18n.t().nav.visionAI }}</h1>
        <span class="model-badge">Vision API</span>
      </div>
      <p class="panel-description">{{ i18n.t().imageUploader.uploadToAnalyze }}</p>
    </div>
  `,
  styles: [`
    .panel {
      padding: 24px 0;
    }
    .panel-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }
    .panel-title {
      font-size: 24px;
      font-weight: 600;
      color: #1d1d1f;
    }
    .model-badge {
      font-size: 12px;
      color: #86868b;
      background: rgba(0, 122, 255, 0.12);
      padding: 4px 8px;
      border-radius: 6px;
    }
    .panel-description {
      color: #86868b;
      font-size: 14px;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VisionPanel {
  readonly i18n = inject(I18nService);
}
