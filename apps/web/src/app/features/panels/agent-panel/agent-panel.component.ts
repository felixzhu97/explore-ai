import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-agent-panel',
  standalone: true,
  template: `
    <div class="panel-container">
      @if (!hideHeader()) {
        <div class="panel-header">
          <div class="panel-header__left">
            <h3 class="panel-header__title">{{ title() }}</h3>
            @if (description()) {
              <p class="panel-header__description">{{ description() }}</p>
            }
          </div>
          <div class="panel-header__right">
            <ng-content select="[slot=headerRight]"></ng-content>
          </div>
        </div>
      }
      <div class="panel-content">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [
    `
      .panel-container {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }

      .panel-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: var(--spacing-md);
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-lg);
      }

      .panel-header__left {
        display: flex;
        flex-direction: column;
      }

      .panel-header__title {
        font-size: var(--font-size-lg);
        font-weight: var(--font-weight-semibold);
        color: var(--color-text);
        margin: 0;
      }

      .panel-header__description {
        font-size: var(--font-size-sm);
        color: var(--color-text-secondary);
        margin: 0;
        margin-top: var(--spacing-xs);
      }

      .panel-header__right {
        display: flex;
        align-items: center;
      }

      .panel-content {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-lg);
        padding: var(--spacing-lg);
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgentPanelComponent {
  title = input.required<string>();
  description = input<string>('');
  hideHeader = input<boolean>(false);
}
