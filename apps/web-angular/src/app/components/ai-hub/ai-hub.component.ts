import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { I18nService } from '../../i18n';

@Component({
  selector: 'app-ai-hub',
  standalone: true,
  template: `
    <div class="panel">
      <div class="panel-header">
        <h1 class="panel-title">{{ i18n.t().aiHub.title }}</h1>
        <span class="model-badge">{{ i18n.t().aiHub.modelBadge }}</span>
      </div>
      <div class="status">
        <span class="status-dot"></span>
        <span class="status-text">{{ i18n.t().aiHub.statusText }}</span>
      </div>
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
    .status {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #34c759;
    }
    .status-text {
      font-size: 14px;
      color: #34c759;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiHub {
  readonly i18n = inject(I18nService);
}
